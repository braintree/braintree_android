package com.braintreepayments.api;

import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PayPalTwoFactorAuthCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalTwoFactorAuthRequest;
import com.braintreepayments.api.models.PayPalTwoFactorAuthResponse;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.PayPal.isManifestValid;

/**
 * Used to verify payments using two-factor authentication.
 */
public class PayPalTwoFactorAuth {

    public static final String SUCCESS_PATH = "success";
    public static final String CANCEL_PATH = "cancel";

    private static final String PAYPAL_ACCOUNT_PATH = "payment_methods/paypal_accounts";
    private static final String CREATE_SINGLE_PAYMENT_PATH = "paypal_hermes/create_payment_resource";

    /**
     * Perform a lookup using the nonce provided to the {@link PayPalTwoFactorAuthRequest} to determine
     * if two factor authentication is required prior to a transaction attempt.
     *
     * @param fragment A {@link BraintreeFragment} used to perform two factor lookup.
     * @param request A {@link PayPalTwoFactorAuthRequest} describing the two factor authentication lookup.
     * @param callback A {@link PayPalTwoFactorAuthCallback} notify the caller when the lookup is complete.
     */
    public static void performTwoFactorLookup(final BraintreeFragment fragment,
                                              final PayPalTwoFactorAuthRequest request,
                                              final PayPalTwoFactorAuthCallback callback) {

        fragment.sendAnalyticsEvent("paypal-two-factor.perform-two-factor-lookup.started");

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.isPayPalEnabled()) {
                    fragment.postCallback(new BraintreeException("PayPal is not enabled. " +
                            "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                            "for more information."));
                    return;
                }

                if (!isManifestValid(fragment)) {
                    fragment.postCallback(new BraintreeException("BraintreeBrowserSwitchActivity missing, " +
                            "incorrectly configured in AndroidManifest.xml or another app defines the same browser " +
                            "switch url as this app. See " +
                            "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                            "for the correct configuration"));
                    return;
                }

                paypalHermesLookup(fragment, request, callback);
            }
        });
    }

    private static void paypalHermesLookup(final BraintreeFragment fragment,
                                           final PayPalTwoFactorAuthRequest request,
                                           final PayPalTwoFactorAuthCallback callback) {

        String versionedPath = "/v1/" + CREATE_SINGLE_PAYMENT_PATH;
        String requestJson = request.toJson(fragment.getAuthorization().getBearer(), fragment.getReturnUrlScheme());

        fragment.getHttpClient().post(versionedPath, requestJson, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {

                try {
                    PayPalTwoFactorAuthResponse response = PayPalTwoFactorAuthResponse
                            .fromJson(responseBody, fragment.getAuthorization().getBearer());
                    fragment.sendAnalyticsEvent("paypal-two-factor.paypal-hermes-lookup.succeeded");
                    fetchPayPalAccount(fragment, response, callback);
                } catch (JSONException e) {
                    fragment.sendAnalyticsEvent("paypal-two-factor.paypal-hermes-lookup.failed");
                    callback.onLookupFailure(e);
                }
            }

            @Override
            public void failure(Exception e) {
                fragment.sendAnalyticsEvent("paypal-two-factor.paypal-hermes-lookup.failed");
                callback.onLookupFailure(e);
            }
        });
    }

    private static void fetchPayPalAccount(final BraintreeFragment fragment,
                                           final PayPalTwoFactorAuthResponse response,
                                           final PayPalTwoFactorAuthCallback callback) {
        String versionedPath = "/v1/" + PAYPAL_ACCOUNT_PATH;
        String request = response.toJson(PayPalDataCollector.getClientMetadataId(fragment.getApplicationContext()));

        fragment.getHttpClient().post(versionedPath, request, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {

                try {
                    String authenticateUrl = response.getAuthenticateUrl();

                    JSONObject payPalAccountResponse = new JSONObject(responseBody);
                    payPalAccountResponse
                            .getJSONArray("paypalAccounts")
                            .getJSONObject(0)
                            .put("authenticateUrl", authenticateUrl);

                    PayPalAccountNonce payPalAccountNonce =
                            PayPalAccountNonce.fromJson(payPalAccountResponse.toString());

                    fragment.sendAnalyticsEvent("paypal-two-factor.fetch-paypal-account.succeeded");
                    callback.onLookupResult(payPalAccountNonce);
                } catch (JSONException e) {
                    fragment.sendAnalyticsEvent("paypal-two-factor.fetch-paypal-account.failed");
                    fragment.postCallback(e);
                }
            }

            @Override
            public void failure(Exception e) {
                fragment.sendAnalyticsEvent("paypal-two-factor.fetch-paypal-account.failed");
                fragment.postCallback(e);
            }
        });
    }

    /**
     * Perform a two factor authentication.  Will return a nonce to your
     * {@link com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener}.
     *
     * @param fragment A {@link BraintreeFragment} used to continue two factor authentication.
     * @param paymentMethodNonce A {@link PaymentMethodNonce} returned from
     * {@link #performTwoFactorLookup(BraintreeFragment, PayPalTwoFactorAuthRequest, PayPalTwoFactorAuthCallback)}.
     *
     */
    public static void continueTwoFactorAuthentication(BraintreeFragment fragment, PaymentMethodNonce paymentMethodNonce) {
        fragment.sendAnalyticsEvent("paypal-two-factor.continue-two-factor-authentication.started");
        PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce)paymentMethodNonce;
        PayPalTwoFactorAuthSharedPreferences.persistPayPalAccountNonce(fragment, payPalAccountNonce);

        String authenticateUrl = payPalAccountNonce.getAuthenticateUrl();
        if (authenticateUrl == null) {
            // no further auth required; callback with PayPal account nonce immediately
            fragment.sendAnalyticsEvent("paypal-two-factor.continue-two-factor-authentication.no-two-factor-required");
            fragment.postCallback(payPalAccountNonce);
        } else {
            fragment.sendAnalyticsEvent("paypal-two-factor.browser-switch.started");
            fragment.browserSwitch(BraintreeRequestCodes.PAYPAL_TWO_FACTOR_AUTH, authenticateUrl);
        }
    }

    protected static void onActivityResult(final BraintreeFragment fragment, int resultCode, Intent data) {
        PayPalAccountNonce payPalAccountNonce = PayPalTwoFactorAuthSharedPreferences.getPersistedPayPalAccountNonce(fragment);

        if (resultCode == AppCompatActivity.RESULT_OK && data != null && payPalAccountNonce != null) {
            String host = null;
            Uri intentData = data.getData();
            if (intentData != null) {
                host = intentData.getHost();
            }

            if (host != null) {
                switch (host) {
                    case SUCCESS_PATH:
                        fragment.sendAnalyticsEvent("paypal-two-factor.browser-switch.succeeded");
                        fragment.postCallback(payPalAccountNonce);
                        break;
                    case CANCEL_PATH:
                        fragment.sendAnalyticsEvent("paypal-two-factor.browser-switch.canceled");
                        fragment.postCancelCallback(BraintreeRequestCodes.PAYPAL_TWO_FACTOR_AUTH);
                        break;
                    default:
                        fragment.sendAnalyticsEvent("paypal-two-factor.browser-switch.failed");
                        fragment.postCallback(new BraintreeException("Host path unknown: " + host));
                        break;
                }
            } else {
                fragment.sendAnalyticsEvent("paypal-two-factor.browser-switch.failed");
                fragment.postCallback(new BraintreeException("Host missing from browser switch response."));
            }
        } else {
            fragment.sendAnalyticsEvent("paypal-two-factor.browser-switch.canceled");

            fragment.postCancelCallback(BraintreeRequestCodes.PAYPAL_TWO_FACTOR_AUTH);
        }
    }
}
