package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

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
import java.util.List;
import java.util.Set;

public class PayPal {

    private static final String OFFLINE = "offline";

    protected static boolean sEnableSignatureVerification = true;

    private PayPal() {
        throw new IllegalStateException("Non-instantiable class.");
    }

    protected static void startPaypal(Context context, com.braintreepayments.api.models.PayPalConfiguration configuration) {
        stopPaypalService(context);
        context.startService(buildPayPalServiceIntent(context, configuration));
    }

    protected static void launchPayPal(Activity activity, int requestCode, com.braintreepayments.api.models.PayPalConfiguration configuration,
        List<String> additionalScopes) {
        Class klass;
        if (PayPalTouch.available(activity.getBaseContext(), sEnableSignatureVerification) &&
                !configuration.getEnvironment().equals(OFFLINE) &&
                !configuration.isTouchDisabled()) {
            klass = PayPalTouchActivity.class;
        } else {
            klass = PayPalProfileSharingActivity.class;
        }

        Set<String> scopes = new HashSet<String>(
                Arrays.asList(
                        PayPalOAuthScopes.PAYPAL_SCOPE_EMAIL,
                        PayPalOAuthScopes.PAYPAL_SCOPE_FUTURE_PAYMENTS)
        );
        if (additionalScopes != null) {
            scopes.addAll(additionalScopes);
        }
        Intent intent = new Intent(activity, klass)
            .putExtra(PayPalTouchActivity.EXTRA_REQUESTED_SCOPES, new PayPalOAuthScopes(scopes))
            .putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, buildPayPalConfiguration(configuration));
        activity.startActivityForResult(intent, requestCode);
    }

    protected static void stopPaypalService(Context context) {
        context.stopService(new Intent(context, PayPalService.class));
    }

    /**
     * Used to create a {@link PayPalAccountBuilder} from an activity response. Not necessary to use,
     * {@link com.braintreepayments.api.Braintree#finishPayWithPayPal(android.app.Activity, int, android.content.Intent)}
     * does this for you.
     *
     * @param activity Activity that received the result.
     * @param resultCode Result code returned in result.
     * @param data {@link Intent} returned in result.
     * @return {@link PayPalAccountBuilder} or null if resultCode is not
     * {@link android.app.Activity#RESULT_OK} or
     * {@link com.paypal.android.sdk.payments.PayPalProfileSharingActivity#RESULT_EXTRAS_INVALID}
     * @throws ConfigurationException if resultCode is {@link com.paypal.android.sdk.payments.PayPalProfileSharingActivity#RESULT_EXTRAS_INVALID}
     */
    @Nullable
    public static PayPalAccountBuilder getBuilderFromActivity(Activity activity, int resultCode, Intent data) throws ConfigurationException {
        if (resultCode == Activity.RESULT_OK) {
            PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder();
            if (activity != null) {
                paypalAccountBuilder.correlationId(PayPalConfiguration.getClientMetadataId(activity));
            }

            PayPalTouchConfirmation paypalTouchConfirmation = data.getParcelableExtra(
                    PayPalTouchActivity.EXTRA_LOGIN_CONFIRMATION);
            if (paypalTouchConfirmation != null) {
                JSONObject paypalTouchResponse;
                try {
                    paypalTouchResponse = paypalTouchConfirmation
                            .toJSONObject()
                            .getJSONObject("response");
                } catch (JSONException ignored) {
                    return null;
                }

                paypalAccountBuilder
                        .consentCode(paypalTouchResponse.optString("authorization_code"))
                        .source("paypal-app");
            } else {
                PayPalAuthorization authorization = data.getParcelableExtra(
                        PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION);
                paypalAccountBuilder.consentCode(authorization.getAuthorizationCode())
                        .source("paypal-sdk");
            }

            return paypalAccountBuilder;
        } else if (resultCode == PayPalProfileSharingActivity.RESULT_EXTRAS_INVALID) {
            throw new ConfigurationException("Result extras were invalid");
        }

        return null;
    }

    /**
     * Checks whether or not an {@link android.content.Intent} was generated by a Pay with PayPal flow.
     * Used to differentiate {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * responses for PayPal and other payment methods.
     *
     * @param data {@link android.content.Intent} to be checked
     * @return {@code true} if the {@link android.content.Intent} contains PayPal data,
     *         {@code false} if the {@link android.content.Intent} does not contain PayPal data.
     */
    public static boolean isPayPalIntent(Intent data) {
        return (data.hasExtra(PayPalTouchActivity.EXTRA_LOGIN_CONFIRMATION) ||
                data.hasExtra(PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION));
    }

    protected static PayPalConfiguration buildPayPalConfiguration(com.braintreepayments.api.models.PayPalConfiguration configuration) {
        PayPalConfiguration paypalConfiguration = new PayPalConfiguration();

        if (configuration.getEnvironment().equals("live")) {
            paypalConfiguration.environment(PayPalConfiguration.ENVIRONMENT_PRODUCTION);
        } else if (configuration.getEnvironment().equals("offline")) {
            paypalConfiguration.environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK);
        } else {
            paypalConfiguration.environment(configuration.getEnvironment());
        }

        return paypalConfiguration
                .clientId(configuration.getClientId())
                .merchantName(configuration.getDisplayName())
                .merchantUserAgreementUri(Uri.parse(configuration.getUserAgreementUrl()))
                .merchantPrivacyPolicyUri(Uri.parse(configuration.getPrivacyUrl()));
    }

    protected static Intent buildPayPalServiceIntent(Context context, com.braintreepayments.api.models.PayPalConfiguration configuration) {
        Intent intent = new Intent(context, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, buildPayPalConfiguration(configuration));
        intent.putExtra("com.paypal.android.sdk.enableAuthenticatorSecurity", sEnableSignatureVerification);

        if(configuration.getEnvironment().equals("custom")) {
            intent.putExtra("com.paypal.android.sdk.baseEnvironmentUrl", configuration.getDirectBaseUrl());
            intent.putExtra("com.paypal.android.sdk.enableStageSsl", false);
        }

        return intent;
    }
}
