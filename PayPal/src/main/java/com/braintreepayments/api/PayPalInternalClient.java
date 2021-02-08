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

    void sendRequest(final Context context, final PayPalRequest payPalRequest, final boolean isBillingAgreement, final PayPalInternalClientCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration == null) {
                    callback.onResult(null, error);
                    return;
                }
                try {
                    String endpoint = isBillingAgreement
                            ? SETUP_BILLING_AGREEMENT_ENDPOINT : CREATE_SINGLE_PAYMENT_ENDPOINT;
                    String url = String.format("/v1/%s", endpoint);

                    String requestBody = createRequestBody(payPalRequest, isBillingAgreement, configuration);

                    braintreeClient.sendPOST(url, requestBody, new HttpResponseCallback() {
                        @Override
                        public void success(String responseBody) {
                            try {
                                PayPalResponse payPalResponse = new PayPalResponse()
                                        .intent(payPalRequest.getIntent())
                                        .isBillingAgreement(isBillingAgreement)
                                        .merchantAccountId(payPalRequest.getMerchantAccountId())
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

    private String createRequestBody(PayPalRequest payPalRequest, boolean isBillingAgreement, Configuration configuration) throws JSONException {

        JSONObject parameters = new JSONObject()
                .put(RETURN_URL_KEY, successUrl)
                .put(CANCEL_URL_KEY, cancelUrl)
                .put(OFFER_CREDIT_KEY, payPalRequest.shouldOfferCredit());

        Authorization authorization = braintreeClient.getAuthorization();
        if (authorization instanceof ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer());
        } else {
            parameters.put(TOKENIZATION_KEY, authorization.getBearer());
        }

        if (isBillingAgreement) {
            String billingAgreementDescription = payPalRequest.getBillingAgreementDescription();
            if (!TextUtils.isEmpty(billingAgreementDescription)) {
                parameters.put(DESCRIPTION_KEY, billingAgreementDescription);
            }
        } else {
            String currencyCode = payPalRequest.getCurrencyCode();
            if (currencyCode == null) {
                currencyCode = configuration.getPayPal().getCurrencyIsoCode();
            }

            parameters
                    .put(AMOUNT_KEY, payPalRequest.getAmount())
                    .put(CURRENCY_ISO_CODE_KEY, currencyCode)
                    .put(INTENT_KEY, payPalRequest.getIntent());

            if (!payPalRequest.getLineItems().isEmpty()) {
                JSONArray lineItems = new JSONArray();
                for (PayPalLineItem lineItem : payPalRequest.getLineItems()) {
                    lineItems.put(lineItem.toJson());
                }
                parameters.put(LINE_ITEMS_KEY, lineItems);
            }
        }

        JSONObject experienceProfile = new JSONObject();
        experienceProfile.put(NO_SHIPPING_KEY, !payPalRequest.isShippingAddressRequired());
        experienceProfile.put(LANDING_PAGE_TYPE_KEY, payPalRequest.getLandingPageType());
        String displayName = payPalRequest.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = configuration.getPayPal().getDisplayName();
        }
        experienceProfile.put(DISPLAY_NAME_KEY, displayName);

        if (payPalRequest.getLocaleCode() != null) {
            experienceProfile.put(LOCALE_CODE_KEY, payPalRequest.getLocaleCode());
        }

        if (payPalRequest.getShippingAddressOverride() != null) {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, !payPalRequest.isShippingAddressEditable());

            JSONObject shippingAddressJson;
            if (isBillingAgreement) {
                shippingAddressJson = new JSONObject();
                parameters.put(SHIPPING_ADDRESS_KEY, shippingAddressJson);
            } else {
                shippingAddressJson = parameters;
            }

            PostalAddress shippingAddress = payPalRequest.getShippingAddressOverride();
            shippingAddressJson.put(PostalAddressParser.LINE_1_KEY, shippingAddress.getStreetAddress());
            shippingAddressJson.put(PostalAddressParser.LINE_2_KEY, shippingAddress.getExtendedAddress());
            shippingAddressJson.put(PostalAddressParser.LOCALITY_KEY, shippingAddress.getLocality());
            shippingAddressJson.put(PostalAddressParser.REGION_KEY, shippingAddress.getRegion());
            shippingAddressJson.put(PostalAddressParser.POSTAL_CODE_UNDERSCORE_KEY, shippingAddress.getPostalCode());
            shippingAddressJson.put(PostalAddressParser.COUNTRY_CODE_UNDERSCORE_KEY, shippingAddress.getCountryCodeAlpha2());
            shippingAddressJson.put(PostalAddressParser.RECIPIENT_NAME_UNDERSCORE_KEY, shippingAddress.getRecipientName());
        } else {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, false);
        }

        if (payPalRequest.getMerchantAccountId() != null) {
            parameters.put(MERCHANT_ACCOUNT_ID, payPalRequest.getMerchantAccountId());
        }

        parameters.put(EXPERIENCE_PROFILE_KEY, experienceProfile);
        return parameters.toString();
    }
}
