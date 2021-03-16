package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class PayPalInternalClient {

    private static final String NO_SHIPPING_KEY = "no_shipping";
    private static final String ADDRESS_OVERRIDE_KEY = "address_override";
    private static final String LOCALE_CODE_KEY = "locale_code";
    private static final String DESCRIPTION_KEY = "description";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorization_fingerprint";
    private static final String TOKENIZATION_KEY = "client_key";
    private static final String RETURN_URL_KEY = "return_url";
    private static final String OFFER_CREDIT_KEY = "offer_paypal_credit";
    private static final String OFFER_PAY_LATER_KEY = "offer_pay_later";
    private static final String CANCEL_URL_KEY = "cancel_url";
    private static final String EXPERIENCE_PROFILE_KEY = "experience_profile";
    private static final String AMOUNT_KEY = "amount";
    private static final String CURRENCY_ISO_CODE_KEY = "currency_iso_code";
    private static final String INTENT_KEY = "intent";
    private static final String LANDING_PAGE_TYPE_KEY = "landing_page_type";
    private static final String DISPLAY_NAME_KEY = "brand_name";
    private static final String SHIPPING_ADDRESS_KEY = "shipping_address";
    private static final String MERCHANT_ACCOUNT_ID = "merchant_account_id";
    private static final String LINE_ITEMS_KEY = "line_items";

    private static final String CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource";
    private static final String SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement";

    private static final String USER_ACTION_KEY = "useraction";

    private final String cancelUrl;
    private final String successUrl;

    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;

    PayPalInternalClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalDataCollector());
    }

    @VisibleForTesting
    PayPalInternalClient(BraintreeClient braintreeClient, PayPalDataCollector payPalDataCollector) {
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;

        this.cancelUrl = String.format("%s://onetouch/v1/cancel", braintreeClient.getReturnUrlScheme());
        this.successUrl = String.format("%s://onetouch/v1/success", braintreeClient.getReturnUrlScheme());
    }

    void sendRequest(final Context context, final PayPalRequest payPalRequest, final PayPalInternalClientCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration == null) {
                    callback.onResult(null, error);
                    return;
                }
                try {
                    final boolean isBillingAgreement = payPalRequest instanceof PayPalVaultRequest;
                    String endpoint = isBillingAgreement
                            ? SETUP_BILLING_AGREEMENT_ENDPOINT : CREATE_SINGLE_PAYMENT_ENDPOINT;
                    String url = String.format("/v1/%s", endpoint);

                    String requestBody = payPalRequest.createRequestBody(configuration, braintreeClient.getAuthorization(), successUrl, cancelUrl);

                    braintreeClient.sendPOST(url, requestBody, new HttpResponseCallback() {
                        @Override
                        public void success(String responseBody) {
                            try {
                                PayPalResponse payPalResponse = new PayPalResponse(payPalRequest)
                                        .successUrl(successUrl);

                                PayPalPaymentResource paypalPaymentResource = PayPalPaymentResource.fromJson(responseBody);
                                String redirectUrl = paypalPaymentResource.getRedirectUrl();
                                if (redirectUrl != null) {
                                    Uri parsedRedirectUri = Uri.parse(redirectUrl);

                                    String pairingIdKey = isBillingAgreement ? "ba_token" : "token";
                                    String pairingId = parsedRedirectUri.getQueryParameter(pairingIdKey);

                                    if (pairingId != null) {
                                        payPalResponse
                                                .pairingId(pairingId)
                                                .clientMetadataId(payPalDataCollector.getClientMetadataId(context));
                                    }

                                    String approvalUrl = parsedRedirectUri
                                            .buildUpon()
                                            .appendQueryParameter(USER_ACTION_KEY, payPalRequest.getUserAction())
                                            .toString();
                                    payPalResponse.approvalUrl(approvalUrl);
                                }
                                callback.onResult(payPalResponse, null);

                            } catch (JSONException exception) {
                                callback.onResult(null, exception);
                            }
                        }

                        @Override
                        public void failure(Exception exception) {
                            callback.onResult(null, exception);
                        }
                    });
                } catch (JSONException exception) {
                    callback.onResult(null, exception);
                }

            }
        });
    }
}
