package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.ClientToken.PayPal;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.paypal.android.sdk.payments.PayPalAuthorization;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalOAuthScopes;
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PayPalTouch;
import com.paypal.android.sdk.payments.PayPalTouchActivity;
import com.paypal.android.sdk.payments.PayPalTouchConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PayPalHelper {

    private static final String OFFLINE = "offline";

    protected static boolean sEnableSignatureVerification = true;

    private PayPalHelper() {
        throw new IllegalStateException("Non-instantiable class.");
    }

    protected static void startPaypal(Context context, ClientToken clientToken) {
        stopPaypalService(context);
        context.startService(buildPayPalServiceIntent(context, clientToken));
    }

    protected static void launchPayPal(Activity activity, int requestCode, ClientToken clientToken) {
        Class klass;
        if (PayPalTouch.available(activity.getBaseContext(), sEnableSignatureVerification) &&
                !clientToken.getPayPal().getEnvironment().equals(OFFLINE) &&
                !clientToken.getPayPal().getTouchDisabled()) {
            klass = PayPalTouchActivity.class;
        } else {
            klass = PayPalProfileSharingActivity.class;
        }

        Intent intent = new Intent(activity, klass);
        Set<String> scopes = new HashSet<String>(
                Arrays.asList(
                    PayPalOAuthScopes.PAYPAL_SCOPE_EMAIL,
                    PayPalOAuthScopes.PAYPAL_SCOPE_PROFILE,
                    PayPalOAuthScopes.PAYPAL_SCOPE_FUTURE_PAYMENTS)
        );
        intent.putExtra(PayPalTouchActivity.EXTRA_REQUESTED_SCOPES, new PayPalOAuthScopes(scopes));
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, buildPayPalConfiguration(
                clientToken));
        activity.startActivityForResult(intent, requestCode);
    }

    protected static void stopPaypalService(Context context) {
        context.stopService(new Intent(context, PayPalService.class));
    }

    /**
     * Used to create a {@link com.braintreepayments.api.models.PayPalAccountBuilder} from an activity
     * response. Not necessary to use, {@link com.braintreepayments.api.Braintree#finishPayWithPayPal(android.app.Activity, int, android.content.Intent)}
     * does this for you.
     * @param activity Activity that received the result.
     * @param resultCode Result code returned in result.
     * @param data {@link Intent} returned in result.
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder} or null if
     * resultCode is not {@link android.app.Activity#RESULT_OK} or {@link com.paypal.android.sdk.payments.PayPalProfileSharingActivity#RESULT_EXTRAS_INVALID}
     * @throws ConfigurationException if resultCode is {@link com.paypal.android.sdk.payments.PayPalProfileSharingActivity#RESULT_EXTRAS_INVALID}
     */
    public static PayPalAccountBuilder getBuilderFromActivity(Activity activity, int resultCode, Intent data) throws ConfigurationException {
        if (resultCode == Activity.RESULT_OK) {
            PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder();
            if (activity != null) {
                paypalAccountBuilder.correlationId(PayPalConfiguration.getClientMetadataId(activity));
            }

            PayPalTouchConfirmation paypalTouchConfirmation = data.getParcelableExtra(
                    PayPalTouchActivity.EXTRA_LOGIN_CONFIRMATION);
            if (paypalTouchConfirmation != null) {
                JSONObject paypalTouchResponse = paypalTouchConfirmation
                        .getPayPalTouchResponseBundle().toJSONObject();
                paypalAccountBuilder
                        .authorizationCode(paypalTouchResponse.optString("authorization_code"))
                        .source("paypal-app");
                paypalAccountBuilder.email(paypalTouchResponse.optString("email"));
            } else {
                PayPalAuthorization authorization = data.getParcelableExtra(
                        PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION);
                paypalAccountBuilder.authorizationCode(authorization.getAuthorizationCode())
                        .source("paypal-sdk");
                try {
                    String email = authorization.toJSONObject()
                            .getJSONObject("user")
                            .getString("display_string");
                    paypalAccountBuilder.email(email);
                } catch (JSONException e) {
                    // If email was not included, don't set it
                }
            }

            return paypalAccountBuilder;
        } else if (resultCode == PayPalProfileSharingActivity.RESULT_EXTRAS_INVALID) {
            throw new ConfigurationException();
        }

        return null;
    }

    /**
     * Checks whether or not an {@link android.content.Intent} was generated by a Pay with PayPal flow.
     * Used to differentiate {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * responses for PayPal and other payment methods.
     * @param data {@link android.content.Intent} to be checked
     * @return {@code true} if the {@link android.content.Intent} contains PayPal data,
     *         {@code false} if the {@link android.content.Intent} does not contain PayPal data.
     */
    public static boolean isPayPalIntent(Intent data) {
        return (data.getParcelableExtra(PayPalTouchActivity.EXTRA_LOGIN_CONFIRMATION) != null ||
                data.getParcelableExtra(PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION) != null);
    }

    protected static PayPalConfiguration buildPayPalConfiguration(ClientToken clientToken) {
        PayPalConfiguration paypalConfiguration = new PayPalConfiguration();

        PayPal paypal = clientToken.getPayPal();

        if (paypal.getEnvironment().equals("live")) {
            paypalConfiguration.environment(PayPalConfiguration.ENVIRONMENT_PRODUCTION);
        } else if (paypal.getEnvironment().equals("offline")) {
            paypalConfiguration.environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK);
        } else {
            paypalConfiguration.environment(paypal.getEnvironment());
        }

        return paypalConfiguration
                .clientId(paypal.getClientId())
                .merchantName(paypal.getDisplayName())
                .merchantUserAgreementUri(Uri.parse(paypal.getUserAgreementUrl()))
                .merchantPrivacyPolicyUri(Uri.parse(paypal.getPrivacyUrl()));
    }

    protected static Intent buildPayPalServiceIntent(Context context, ClientToken clientToken) {
        Intent intent = new Intent(context, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, buildPayPalConfiguration(
                clientToken));
        intent.putExtra("com.paypal.android.sdk.enableAuthenticatorSecurity", sEnableSignatureVerification);

        if(clientToken.getPayPal().getEnvironment().equals("custom")) {
            intent.putExtra("com.paypal.android.sdk.baseEnvironmentUrl",
                    clientToken.getPayPal().getDirectBaseUrl());
            intent.putExtra("com.paypal.android.sdk.enableStageSsl", false);
        }

        return intent;
    }
}
