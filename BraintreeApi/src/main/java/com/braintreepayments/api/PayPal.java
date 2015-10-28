package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodResponseCallback;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PayPalConfiguration;
import com.braintreepayments.api.models.PayPalPaymentResource;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PayPalRequestBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.PostalAddress;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.BillingAgreementRequest;
import com.paypal.android.sdk.onetouch.core.BrowserSwitchAdapter;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchActivity;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;
import com.paypal.android.sdk.onetouch.core.PerformRequestStatus;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.RequestTarget;
import com.paypal.android.sdk.onetouch.core.Result;
import com.paypal.android.sdk.onetouch.core.ResultType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Class containing PayPal specific logic.
 */
public class PayPal {

    public static final int PAYPAL_REQUEST_CODE = 13591;

    /**
     * PayPal Scope for Future Payments. Always enabled for the future payments flow.
     */
    public static final String SCOPE_FUTURE_PAYMENTS =
            "https://uri.paypal.com/services/payments/futurepayments";

    /**
     * PayPal Scope for email. Always enabled for the future payments flow.
     */
    public static final String SCOPE_EMAIL = "email";

    /**
     * PayPal Scope for obtaining the accounts address. Optional, can be specified in the optional
     * scopes when using {@link #authorizeAccount(BraintreeFragment, List)}.
     */
    public static final String SCOPE_ADDRESS = "address";

    protected static boolean sEnableSignatureVerification = true;

    private static final String SETUP_BILLING_AGREEMENT_ENDPOINT =
            "paypal_hermes/setup_billing_agreement";
    private static final String CREATE_SINGLE_PAYMENT_ENDPOINT =
            "paypal_hermes/create_payment_resource";
    private static final String NO_SHIPPING_KEY = "no_shipping";
    private static final String ADDRESS_OVERRIDE_KEY = "address_override";
    private static final String LOCALE_CODE_KEY = "locale_code";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorization_fingerprint";
    private static final String TOKENIZATION_KEY = "client_key"; // TODO: change when Gateway accepts tokenization key
    private static final String RETURN_URL_KEY = "return_url";
    private static final String CANCEL_URL_KEY = "cancel_url";
    private static final String EXPERIENCE_PROFILE_KEY = "experience_profile";
    private static final String AMOUNT_KEY = "amount";
    private static final String CURRENCY_ISO_CODE_KEY = "currency_iso_code";
    private static final String PAYLOAD_CLIENT_TOKEN_KEY = "client_token";

    private static Request sPendingRequest;
    private static PerformRequestStatus sPendingRequestStatus;

    /**
     * Starts the Pay With PayPal flow. This will launch the PayPal app if installed or switch to
     * the browser for user authorization. The Billing Agreement flow will be used if enabled,
     * otherwise the Future Payment flow will be used.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     */
    public static void authorizeAccount(BraintreeFragment fragment) {
        authorizeAccount(fragment, null);
    }

    /**
     * Starts the Pay With PayPal flow with additional scopes. This will launch the PayPal app if
     * installed or switch to the browser for user authorization. The Billing Agreement flow will be
     * used if enabled, otherwise the Future Payment flow will be used.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param additionalScopes A {@link java.util.List} of additional scopes. Ex: {@link
     * #SCOPE_ADDRESS}. Acceptable scopes are defined in {@link com.braintreepayments.api.PayPal}.
     */
    public static void authorizeAccount(final BraintreeFragment fragment,
            final List<String> additionalScopes) {
        fragment.sendAnalyticsEvent("paypal.selected");

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (configuration.getPayPal().shouldUseBillingAgreement()) {
                    requestBillingAgreement(fragment, new PayPalRequest());
                    return;
                }

                try {
                    sPendingRequest =
                            buildAuthorizationConfiguration(fragment.getApplicationContext(),
                                    fragment.getConfiguration(),
                                    fragment.getAuthorization().toString());

                    if (additionalScopes != null) {
                        for (String scope : additionalScopes) {
                            ((AuthorizationRequest) sPendingRequest).withScopeValue(scope);
                        }
                    }

                    sPendingRequestStatus =
                            PayPalOneTouchCore.performRequest(fragment.getActivity(),
                                    sPendingRequest, PAYPAL_REQUEST_CODE,
                                    sEnableSignatureVerification,
                                    getBrowserSwitchAdapter(fragment,
                                            PAYPAL_REQUEST_CODE));

                    sendAnalyticsForPayPalPerformRequestStatus(fragment, sPendingRequestStatus);
                } catch (BraintreeException error) {
                    fragment.postCallback(error);
                }
            }
        });
    }

    /**
     * Starts the Billing Agreement flow for PayPal. This will launch the PayPal app if installed or
     * fall back to a browser switch.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     */
    public static void requestBillingAgreement(BraintreeFragment fragment, PayPalRequest checkout) {
        if (checkout.getAmount() == null) {
            requestOneTimePayment(fragment, checkout, true);
        } else {
            fragment.postCallback(new BraintreeException(
                    "There must be no amount specified for the Billing Agreement flow"));
        }
    }

    /**
     * Starts the Single Payment flow for PayPal. This will launch the PayPal app if installed or
     * fall back to a browser switch.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param checkout A {@link PayPalRequest} used to customize the request. An amount MUST be
     *                 specified.
     */
    public static void requestOneTimePayment(BraintreeFragment fragment, PayPalRequest checkout) {
        if (checkout.getAmount() != null) {
            requestOneTimePayment(fragment, checkout, false);
        } else {
            fragment.postCallback(new BraintreeException(
                    "An amount MUST be specified for the Single Payment flow."));
        }
    }

    /**
     * Starts the Checkout With PayPal flow. This will launch the PayPal app if installed or switch
     * to the browser for user authorization.
     * <p>
     * This requires that the merchant uses a {@link com.braintreepayments.api.models.ClientToken}
     *
     * @param fragment           A {@link BraintreeFragment} used to process the request.
     * @param checkout           A {@link PayPalRequest} used to customize the request.
     * @param isBillingAgreement A boolean. If true, this will use the Billing Agreement. Otherwise,
     * PayPal will perform a Single Payment.
     */
    private static void requestOneTimePayment(final BraintreeFragment fragment,
            final PayPalRequest checkout, final boolean isBillingAgreement) {
        final HttpResponseCallback callback = new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                final PayPalPaymentResource paypalPaymentResource;
                try {
                    paypalPaymentResource = PayPalPaymentResource.fromJson(responseBody);
                } catch (JSONException e) {
                    // If the PayPalPaymentResource is unable to be loaded, return.
                    fragment.postCallback(e);
                    Log.d("PayPal", "Unable to parse URL for PayPal app switch.");
                    return;
                }
                try {
                    if (isBillingAgreement) {
                        sPendingRequest = buildBillingAgreementConfiguration(
                                paypalPaymentResource.getRedirectUrl(),
                                fragment.getApplicationContext(),
                                fragment.getConfiguration());
                    } else {
                        sPendingRequest = buildCheckoutConfiguration(
                                paypalPaymentResource.getRedirectUrl(),
                                fragment);
                    }

                    sPendingRequestStatus =
                            PayPalOneTouchCore.performRequest(fragment.getActivity(),
                                    sPendingRequest, PAYPAL_REQUEST_CODE,
                                    sEnableSignatureVerification,
                                    getBrowserSwitchAdapter(fragment, PAYPAL_REQUEST_CODE));

                    sendAnalyticsForPayPalPerformRequestStatus(fragment, sPendingRequestStatus);
                } catch (ConfigurationException e) {
                    fragment.postCallback(e);
                }
            }

            @Override
            public void failure(Exception e) {
                fragment.postCallback(e);
                ErrorWithResponse ex = (ErrorWithResponse) e;
                try {
                    JSONObject response = new JSONObject(ex.getErrorResponse());
                    String debugId = response.getJSONObject("error").getString("debugId");
                    Log.d("PayPal",
                            "Unable to generate URL for PayPal app switch. DebugId: " + debugId);
                } catch (JSONException jsonException) {
                    Log.d("PayPal", "Unable to generate URL for PayPal app switch.");
                }
            }
        };

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                try {
                    createPaymentResource(fragment, checkout, isBillingAgreement, callback);
                } catch (JSONException | ErrorWithResponse | BraintreeException ex) {
                    fragment.postCallback(ex);
                }
            }
        });
    }

    /**
     * Create a PayPalPaymentResource on behalf of the merchant. To be used in the PayPal Checkout
     * flows for Single Payment and Billing Agreement.
     *
     * @param fragment           A {@link BraintreeFragment} used to process the request.
     * @param checkout           A {@link PayPalRequest} used to customize the request.
     * @param isBillingAgreement A boolean. If true, this will use the Billing Agreement. Otherwise,
     *        PayPal will perform a Single Payment.
     * @param callback A callback on the http request.
     */
    private static void createPaymentResource(BraintreeFragment fragment,
            PayPalRequest checkout, boolean isBillingAgreement, HttpResponseCallback callback)
            throws JSONException, ErrorWithResponse, BraintreeException {
        CheckoutRequest request = buildCheckoutConfiguration(null, fragment);
        String currencyCode = checkout.getCurrencyCode();
        if (currencyCode == null) {
            currencyCode = fragment.getConfiguration().getPayPal().getCurrencyIsoCode();
        }

        JSONObject experienceProfile = new JSONObject();
        experienceProfile.put(NO_SHIPPING_KEY, !checkout.isShippingAddressRequired());

        if (checkout.getLocaleCode() != null) {
            experienceProfile.put(LOCALE_CODE_KEY, checkout.getLocaleCode());
        }

        JSONObject parameters = new JSONObject()
                .put(RETURN_URL_KEY, request.getSuccessUrl())
                .put(CANCEL_URL_KEY, request.getCancelUrl());

        if (fragment.getAuthorization() instanceof ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY,
                    ((ClientToken) fragment.getAuthorization()).getAuthorizationFingerprint());
        } else {
            parameters.put(TOKENIZATION_KEY, fragment.getAuthorization().toString());
        }

        if (!isBillingAgreement) {
            parameters.put(AMOUNT_KEY, checkout.getAmount())
                    .put(CURRENCY_ISO_CODE_KEY, currencyCode);
        }

        if (checkout.getShippingAddressOverride() != null && !checkout.getShippingAddressOverride().isEmpty()) {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, true);
            PostalAddress shippingAddress = checkout.getShippingAddressOverride();
            parameters.put(PostalAddress.LINE_1_KEY, shippingAddress.getStreetAddress());
            parameters.put(PostalAddress.LINE_2_KEY, shippingAddress.getExtendedAddress());
            parameters.put(PostalAddress.LOCALITY_KEY, shippingAddress.getLocality());
            parameters.put(PostalAddress.REGION_KEY, shippingAddress.getRegion());
            parameters
                    .put(PostalAddress.POSTAL_CODE_UNDERSCORE_KEY, shippingAddress.getPostalCode());
            parameters.put(PostalAddress.COUNTRY_CODE_UNDERSCORE_KEY,
                    shippingAddress.getCountryCodeAlpha2());
            parameters.put(PostalAddress.RECIPIENT_NAME_UNDERSCORE_KEY,
                    shippingAddress.getRecipientName());
        } else {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, false);
        }

        parameters.put(EXPERIENCE_PROFILE_KEY, experienceProfile);

        String apiUrl = isBillingAgreement ? SETUP_BILLING_AGREEMENT_ENDPOINT :
                CREATE_SINGLE_PAYMENT_ENDPOINT;
        String versionedPath = "/v1/" + apiUrl;
        fragment.getHttpClient().post(
                versionedPath,
                parameters.toString(),
                callback
        );
    }

    private static void onCancel(final BraintreeFragment fragment, Result result,
            boolean isAppSwitch) {
        if (result != null && result.getError() == null) {
            sendAnalyticsEventForSwitchResult(fragment, isAppSwitch, "canceled");
        } else if (result.getError() != null) {
            sendAnalyticsEventForSwitchResult(fragment, isAppSwitch, "canceled-with-error");
        }
    }

    private static void onSuccess(final BraintreeFragment fragment, Intent data, Result result)
            throws InvalidArgumentException {
        sendAnalyticsEventForSwitchResult(fragment, isAppSwitch(data), "succeeded");

        TokenizationClient.tokenize(fragment, parseResponse(result, data),
                new PaymentMethodResponseCallback() {
                    @Override
                    public void success(PaymentMethod paymentMethod) {
                        fragment.postCallback(paymentMethod);
                    }

                    @Override
                    public void failure(Exception exception) {
                        fragment.postCallback(exception);
                    }
                });
    }

    /**
     * The result from PayPal's request.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param resultCode The result code from the request.
     * @param data Data associated with the result.
     */
    protected static void onActivityResult(final BraintreeFragment fragment, int resultCode,
            Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                boolean isAppSwitch = isAppSwitch(data);
                Result result = getResultFromIntent(fragment.getActivity(), data);
                ResultType resultType = result.getResultType();
                switch (resultType) {
                    case Error:
                        sendAnalyticsEventForSwitchResult(fragment, isAppSwitch, "failed");
                        break;
                    case Cancel:
                        onCancel(fragment, result, isAppSwitch);
                        break;
                    case Success:
                        onSuccess(fragment, data, result);
                        break;
                }
            } catch (InvalidArgumentException error) {
                fragment.postCallback(error);
            }
        }
    }

    /**
     * Parse the PayPal response URL using OneTouchCore.
     *
     * @param result Context that received the result.
     * @param intent The {@link Intent} returned in result.
     * @return A {@link PayPalAccountBuilder} or null if the intent is invalid.
     */
    private static PayPalAccountBuilder parseResponse(Result result, Intent intent) throws
            InvalidArgumentException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .clientMetadataId(sPendingRequestStatus.getClientMetadataId());

        if (intent.hasExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT)) {
            paypalAccountBuilder.source("paypal-app");
        } else {
            paypalAccountBuilder.source("paypal-browser");
        }

        JSONObject payload = result.getResponse();
        // Modify payload in 'mock' mode to scope the response
        try {
            JSONObject clientJson = payload.getJSONObject("client");
            JSONObject response = payload.getJSONObject("response");
            if (AuthorizationRequest.ENVIRONMENT_MOCK
                    .equalsIgnoreCase(clientJson.getString("client"))
                    && response.getString("code") != null
                    && !isCheckoutRequest()) {
                payload.put("response", new JSONObject().put("code",
                        "fake-code:" + ((AuthorizationRequest) sPendingRequest).getScopeString()));
            }
        } catch (JSONException ignored) {
        }

        paypalAccountBuilder.oneTouchCoreData(payload);

        return paypalAccountBuilder;
    }

    /**
     * Retrieve the {@link Result} associated with the {@link Request}.
     *
     * @param context Context that received the result.
     * @param intent The {@link Intent} returned in result.
     * @return the {@link Result} of the OTC app/browser switch.
     */
    private static Result getResultFromIntent(Context context, Intent intent) {
        Result result;
        if (intent.hasExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT)) {
            result = intent.getParcelableExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT);
        } else {
            result = PayPalOneTouchCore
                    .handleBrowserResponse(context, intent.getData(), sPendingRequest);
        }

        return result;
    }

    /**
     * Check if the current/last request was a CheckoutRequest
     */
    private static boolean isCheckoutRequest() {
        return sPendingRequest != null && sPendingRequest instanceof CheckoutRequest;
    }

    private static boolean isAppSwitch(Intent data) {
        return data.hasExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT);
    }

    /**
     * Set properties specific to an CheckoutRequest
     */
    private static CheckoutRequest buildCheckoutConfiguration(String approvalUrl,
            BraintreeFragment fragment)
            throws ConfigurationException {
        String pairingId = null;
        if (approvalUrl != null) {
            pairingId = Uri.parse(approvalUrl).getQueryParameter("token");
        }

        PayPalRequestBuilder requestBuilder = new PayPalRequestBuilder();
        CheckoutRequest request =
                requestBuilder.createCheckoutRequest(fragment.getApplicationContext(),
                        fragment.getConfiguration());
        request.pairingId(pairingId)
                .approvalURL(approvalUrl);

        return request;
    }

    /**
     * Set properties specific to an BillingAgreement
     */
    private static BillingAgreementRequest buildBillingAgreementConfiguration(String approvalUrl,
            Context context,
            Configuration configuration) throws ConfigurationException {
        String pairingId = null;
        if (approvalUrl != null) {
            pairingId = Uri.parse(approvalUrl).getQueryParameter("ba_token");
        }

        PayPalRequestBuilder requestBuilder = new PayPalRequestBuilder();
        BillingAgreementRequest request = requestBuilder.createBillingAgreementRequest(context,
                configuration);
        request.pairingId(pairingId)
                .approvalURL(approvalUrl);

        return request;
    }

    /**
     * Set properties specific to an AuthorizationRequest
     */
    private static AuthorizationRequest buildAuthorizationConfiguration(Context context,
            Configuration configuration, String tokenizationKey) throws ConfigurationException {
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        PayPalRequestBuilder requestBuilder = new PayPalRequestBuilder();
        AuthorizationRequest request = requestBuilder.createAuthorizationRequest(context,
                configuration);
        request.privacyUrl(payPalConfiguration.getPrivacyUrl())
                .userAgreementUrl(payPalConfiguration.getUserAgreementUrl())
                .withScopeValue(SCOPE_FUTURE_PAYMENTS)
                .withScopeValue(SCOPE_EMAIL)
                .withAdditionalPayloadAttribute(PAYLOAD_CLIENT_TOKEN_KEY,
                        tokenizationKey);
        return request;
    }

    /**
     * Send analytics for PayPal app switching.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param requestStatus the {@link PerformRequestStatus} returned by PayPal OTC
     */
    private static void sendAnalyticsForPayPalPerformRequestStatus(BraintreeFragment fragment,
            PerformRequestStatus requestStatus) {
        String eventFragment = "";
        if (isCheckoutRequest()) {
            if (requestStatus == null) {
                eventFragment = "paypal-single-payment.none.initiate.failed";
            } else {
                if (requestStatus.isSuccess() &&
                        requestStatus.getTarget() == RequestTarget.browser) {
                    eventFragment = "paypal-single-payment.webswitch.initiate.started";
                } else if (!requestStatus.isSuccess() &&
                        requestStatus.getTarget() == RequestTarget.browser) {
                    eventFragment = "paypal-single-payment.webswitch.initiate.failed";
                } else if (requestStatus.isSuccess() &&
                        requestStatus.getTarget() == RequestTarget.wallet) {
                    eventFragment = "paypal-single-payment.appswitch.initiate.started";
                } else if (!requestStatus.isSuccess() &&
                        requestStatus.getTarget() == RequestTarget.wallet) {
                    eventFragment = "paypal-single-payment.appswitch.initiate.failed";
                }
            }
        } else {
            if (requestStatus == null) {
                eventFragment = "paypal-future-payments.none.initiate.failed";
            } else {
                if (requestStatus.isSuccess() &&
                        requestStatus.getTarget() == RequestTarget.browser) {
                    eventFragment = "paypal-future-payments.webswitch.initiate.started";
                } else if (!requestStatus.isSuccess() &&
                        requestStatus.getTarget() == RequestTarget.browser) {
                    eventFragment = "paypal-future-payments.webswitch.initiate.failed";
                } else if (requestStatus.isSuccess() &&
                        requestStatus.getTarget() == RequestTarget.wallet) {
                    eventFragment = "paypal-future-payments.appswitch.initiate.started";
                } else if (!requestStatus.isSuccess() &&
                        requestStatus.getTarget() == RequestTarget.wallet) {
                    eventFragment = "paypal-future-payments.appswitch.initiate.failed";
                }
            }
        }
        AnalyticsManager.sendRequest(fragment, "custom", eventFragment);
    }

    /**
     * Send analytics for PayPal app switch result.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param isAppSwitch True if the request switched to the PayPal app. False if browser switch.
     * @param eventFragment A {@link String} describing the result.
     */
    private static void sendAnalyticsEventForSwitchResult(BraintreeFragment fragment,
            boolean isAppSwitch, String eventFragment) {
        String authorizationType =
                isCheckoutRequest() ? "paypal-single-payment" : "paypal-future-payments";
        String switchType = isAppSwitch ? "appswitch" : "webswitch";
        String event = String.format("%s.%s.%s", authorizationType, switchType, eventFragment);
        AnalyticsManager.sendRequest(fragment, "custom", event);
    }

    /**
     * Creates a {@link BrowserSwitchAdapter} for a given requestCode
     *
     * @param fragment the {@link BraintreeFragment} responsible for starting the activity
     * @param requestCode the requestCode to use when kinking off the activity
     * @return a {@link BrowserSwitchAdapter}
     */
    private static BrowserSwitchAdapter getBrowserSwitchAdapter(final BraintreeFragment fragment,
            final int requestCode) {
        return new BrowserSwitchAdapter() {
            @Override
            public void handleBrowserSwitchIntent(Intent browserSwitchIntent) {
                browserSwitchIntent.putExtra(BraintreeBrowserSwitchActivity.EXTRA_REQUEST_CODE, requestCode);
                fragment.startActivity(browserSwitchIntent);
            }
        };
    }
}
