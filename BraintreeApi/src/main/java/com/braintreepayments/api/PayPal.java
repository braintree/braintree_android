package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodResponseCallback;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;
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

/**
 * Class containing PayPal specific logic.
 */
public class PayPal {

    public static final int PAYPAL_AUTHORIZATION_REQUEST_CODE = 13591;

    private static final String OFFLINE = "offline";

    protected static boolean sEnableSignatureVerification = true;

    /**
     * Starts the Pay With PayPal flow. This will launch a new activity in the PayPal mobile SDK,
     * or if available start the PayPal Wallet app.
     *
     * @param fragment {@link BraintreeFragment}
     */
    public static void authorizeAccount(BraintreeFragment fragment) {
        authorizeAccount(fragment, null);
    }

    /**
     * Starts the Pay With PayPal flow. This will launch a new activity in the PayPal mobile SDK,
     * or if available start the PayPal Wallet app.
     *
     * @param fragment {@link BraintreeFragment}
     * @param additionalScopes A {@link List} of additional scopes.
     *                         Ex: PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS. Acceptable scopes are
     *                         defined in {@link com.paypal.android.sdk.payments.PayPalOAuthScopes}.
     */
    public static void authorizeAccount(final BraintreeFragment fragment, final List<String> additionalScopes) {
        fragment.sendAnalyticsEvent("paypal.selected");

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched() {
                com.braintreepayments.api.models.PayPalConfiguration configuration =
                        fragment.getConfiguration().getPayPal();

                startPaypalService(fragment.getContext(), configuration);

                Class klass;
                if (PayPalTouch.available(fragment.getContext(), sEnableSignatureVerification) &&
                        !configuration.getEnvironment().equals(OFFLINE) &&
                        !configuration.isTouchDisabled()) {
                    klass = PayPalTouchActivity.class;

                    fragment.sendAnalyticsEvent("paypal.app-switch.started");
                } else {
                    klass = PayPalProfileSharingActivity.class;
                }

                Set<String> scopes = new HashSet<>(
                        Arrays.asList(PayPalOAuthScopes.PAYPAL_SCOPE_EMAIL,
                                PayPalOAuthScopes.PAYPAL_SCOPE_FUTURE_PAYMENTS));
                if (additionalScopes != null) {
                    scopes.addAll(additionalScopes);
                }

                Intent intent = new Intent(fragment.getContext(), klass)
                        .putExtra(PayPalTouchActivity.EXTRA_REQUESTED_SCOPES,
                                new PayPalOAuthScopes(scopes))
                        .putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,
                                buildPayPalConfiguration(configuration));

                fragment.startActivityForResult(intent, PAYPAL_AUTHORIZATION_REQUEST_CODE);
            }
        });
    }

    protected static void onActivityResult(final BraintreeFragment fragment, int resultCode, Intent data) {
        PayPal.stopPaypalService(fragment.getContext());

        if (resultCode == Activity.RESULT_OK) {
            fragment.sendAnalyticsEvent("paypal.app-switch.authorized");

            PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder();
            paypalAccountBuilder.correlationId(PayPalConfiguration.getClientMetadataId(fragment.getContext()));

            PayPalTouchConfirmation paypalTouchConfirmation = data.getParcelableExtra(
                    PayPalTouchActivity.EXTRA_LOGIN_CONFIRMATION);
            if (paypalTouchConfirmation != null) {
                JSONObject paypalTouchResponse;
                try {
                    paypalTouchResponse = paypalTouchConfirmation
                            .toJSONObject()
                            .getJSONObject("response");
                    paypalAccountBuilder
                            .consentCode(paypalTouchResponse.optString("authorization_code"))
                            .source("paypal-app");

                    fragment.sendAnalyticsEvent("paypal.app-switch.authorized");
                } catch (JSONException e) {
                    fragment.postCallback(e);
                    return;
                }
            } else {
                PayPalAuthorization authorization = data.getParcelableExtra(
                        PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION);
                paypalAccountBuilder.consentCode(authorization.getAuthorizationCode())
                        .source("paypal-sdk");
            }

            PaymentMethodTokenization.tokenize(fragment, paypalAccountBuilder,
                    new PaymentMethodResponseCallback() {
                        @Override
                        public void success(PaymentMethod paymentMethod) {
                            fragment.postCallback(paymentMethod);
                            fragment.sendAnalyticsEvent("paypal.nonce-received");
                        }

                        @Override
                        public void failure(Exception exception) {
                            fragment.postCallback(exception);
                        }
                    });
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // TODO: send analytics event for browser or app-switch cancel
        } else if (resultCode == PayPalProfileSharingActivity.RESULT_EXTRAS_INVALID) {
            fragment.postCallback(new ConfigurationException("PayPal result extras were invalid"));
        }
    }

    @VisibleForTesting
    protected static PayPalConfiguration buildPayPalConfiguration(com.braintreepayments.api.models.PayPalConfiguration configuration) {
        PayPalConfiguration paypalConfiguration = new PayPalConfiguration();
        switch (configuration.getEnvironment()) {
            case "live":
                paypalConfiguration.environment(PayPalConfiguration.ENVIRONMENT_PRODUCTION);
                break;
            case "offline":
                paypalConfiguration.environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK);
                break;
            default:
                paypalConfiguration.environment(configuration.getEnvironment());
                break;
        }

        return paypalConfiguration
                .clientId(configuration.getClientId())
                .merchantName(configuration.getDisplayName())
                .merchantUserAgreementUri(Uri.parse(configuration.getUserAgreementUrl()))
                .merchantPrivacyPolicyUri(Uri.parse(configuration.getPrivacyUrl()));
    }

    @VisibleForTesting
    protected static void startPaypalService(Context context,
            com.braintreepayments.api.models.PayPalConfiguration configuration) {
        stopPaypalService(context);
        context.startService(buildPayPalServiceIntent(context, configuration));
    }

    private static void stopPaypalService(Context context) {
        context.stopService(new Intent(context, PayPalService.class));
    }

    @VisibleForTesting
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
