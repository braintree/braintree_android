package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.BrowserSwitchException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.AppHelper;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PayPalConfiguration;
import com.braintreepayments.api.models.PayPalPaymentResource;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.BillingAgreementRequest;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.Result;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;
import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;
import com.paypal.android.sdk.onetouch.core.sdk.PayPalScope;
import com.paypal.android.sdk.onetouch.core.sdk.PendingRequest;

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
    public static final String SCOPE_FUTURE_PAYMENTS = PayPalScope.FUTURE_PAYMENTS.getScopeUri();

    /**
     * PayPal Scope for email. Always enabled for the future payments flow.
     */
    public static final String SCOPE_EMAIL = PayPalScope.EMAIL.getScopeUri();

    /**
     * PayPal Scope for obtaining the accounts address. Optional, can be specified in the optional
     * scopes when using {@link #authorizeAccount(BraintreeFragment, List)}.
     */
    public static final String SCOPE_ADDRESS = PayPalScope.ADDRESS.getScopeUri();

    protected static boolean sFuturePaymentsOverride = false;

    private static final String SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement";
    private static final String CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource";
    private static final String NO_SHIPPING_KEY = "no_shipping";
    private static final String ADDRESS_OVERRIDE_KEY = "address_override";
    private static final String LOCALE_CODE_KEY = "locale_code";
    private static final String DESCRIPTION_KEY = "description";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorization_fingerprint";
    private static final String TOKENIZATION_KEY = "client_key";
    private static final String RETURN_URL_KEY = "return_url";
    private static final String CANCEL_URL_KEY = "cancel_url";
    private static final String EXPERIENCE_PROFILE_KEY = "experience_profile";
    private static final String AMOUNT_KEY = "amount";
    private static final String CURRENCY_ISO_CODE_KEY = "currency_iso_code";
    private static final String PAYLOAD_CLIENT_TOKEN_KEY = "client_token";

    private static Request sRequest;

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
    public static void authorizeAccount(final BraintreeFragment fragment, final List<String> additionalScopes) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.isPayPalEnabled()) {
                    fragment.postCallback(new BraintreeException("PayPal is not enabled"));
                    return;
                }

                if (!isManifestValid(fragment.getApplicationContext())) {
                    fragment.postCallback(new BraintreeException("BraintreeBrowserSwitchActivity missing or " +
                            " incorrectly configured in AndroidManifest.xml. See " +
                            "https://developers.braintreepayments.com/guides/client-sdk/android/v2#browser-switch " +
                            "for the correct configuration"));
                    return;
                }

                if (configuration.getPayPal().shouldUseBillingAgreement() && !sFuturePaymentsOverride) {
                    requestBillingAgreement(fragment, new PayPalRequest());
                    return;
                }

                fragment.sendAnalyticsEvent("paypal.future-payments.selected");

                sRequest = getAuthorizationRequest(fragment.getApplicationContext(),
                        fragment.getConfiguration().getPayPal(), fragment.getAuthorization().toString());

                if (additionalScopes != null) {
                    for (String scope : additionalScopes) {
                        ((AuthorizationRequest) sRequest).withScopeValue(scope);
                    }
                }

                startPayPal(fragment, PayPalOneTouchCore.getStartIntent(fragment.getApplicationContext(), sRequest));
            }
        });
    }

    /**
     * Starts the Billing Agreement flow for PayPal. This will launch the PayPal app if installed or
     * fall back to a browser switch.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param request A {@link PayPalRequest} used to customize the request.
     */
    public static void requestBillingAgreement(BraintreeFragment fragment, PayPalRequest request) {
        if (request.getAmount() == null) {
            fragment.sendAnalyticsEvent("paypal.billing-agreement.selected");
            requestOneTimePayment(fragment, request, true);
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
     * @param request A {@link PayPalRequest} used to customize the request. An amount MUST be
     *                 specified.
     */
    public static void requestOneTimePayment(BraintreeFragment fragment, PayPalRequest request) {
        if (request.getAmount() != null) {
            fragment.sendAnalyticsEvent("paypal.one-time-payment.selected");
            requestOneTimePayment(fragment, request, false);
        } else {
            fragment.postCallback(new BraintreeException("An amount must be specified for the Single Payment flow."));
        }
    }

    /**
     * Starts the Checkout With PayPal flow. This will launch the PayPal app if installed or switch
     * to the browser for user authorization.
     * <p>
     * This requires that the merchant uses a {@link com.braintreepayments.api.models.ClientToken}
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param request A {@link PayPalRequest} used to customize the request.
     * @param isBillingAgreement A boolean. If true, this will use the Billing Agreement. Otherwise,
     *        PayPal will perform a Single Payment.
     */
    private static void requestOneTimePayment(final BraintreeFragment fragment, final PayPalRequest request,
            final boolean isBillingAgreement) {
        final HttpResponseCallback callback = new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                final PayPalPaymentResource paypalPaymentResource;
                try {
                    paypalPaymentResource = PayPalPaymentResource.fromJson(responseBody);
                } catch (JSONException e) {
                    fragment.postCallback(e);
                    return;
                }

                if (isBillingAgreement) {
                    sRequest = getBillingAgreementRequest(paypalPaymentResource.getRedirectUrl(),
                            fragment.getApplicationContext(), fragment.getConfiguration().getPayPal());
                } else {
                    sRequest = getCheckoutRequest(paypalPaymentResource.getRedirectUrl(),
                            fragment.getApplicationContext(), fragment.getConfiguration().getPayPal());
                }

                startPayPal(fragment, PayPalOneTouchCore.getStartIntent(fragment.getApplicationContext(), sRequest));
            }

            @Override
            public void failure(Exception e) {
                fragment.postCallback(e);
            }
        };

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.isPayPalEnabled()) {
                    fragment.postCallback(new BraintreeException("PayPal is not enabled"));
                    return;
                }

                if (!isManifestValid(fragment.getApplicationContext())) {
                    fragment.postCallback(new BraintreeException("BraintreeBrowserSwitchActivity missing or " +
                            " incorrectly configured in AndroidManifest.xml. See " +
                            "https://developers.braintreepayments.com/guides/client-sdk/android/v2#browser-switch " +
                            "for the correct configuration"));
                    return;
                }

                try {
                    createPaymentResource(fragment, request, isBillingAgreement, callback);
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
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param request A {@link PayPalRequest} used to customize the request.
     * @param isBillingAgreement A boolean. If true, this will use the Billing Agreement. Otherwise,
     *        PayPal will perform a Single Payment.
     * @param callback A callback on the http request.
     */
    private static void createPaymentResource(BraintreeFragment fragment, PayPalRequest request,
            boolean isBillingAgreement, HttpResponseCallback callback)
            throws JSONException, ErrorWithResponse, BraintreeException {
        CheckoutRequest checkoutRequest = getCheckoutRequest(null, fragment.getApplicationContext(),
                fragment.getConfiguration().getPayPal());
        String currencyCode = request.getCurrencyCode();
        if (currencyCode == null) {
            currencyCode = fragment.getConfiguration().getPayPal().getCurrencyIsoCode();
        }

        JSONObject parameters = new JSONObject()
                .put(RETURN_URL_KEY, checkoutRequest.getSuccessUrl())
                .put(CANCEL_URL_KEY, checkoutRequest.getCancelUrl());

        if (fragment.getAuthorization() instanceof ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY,
                    ((ClientToken) fragment.getAuthorization()).getAuthorizationFingerprint());
        } else {
            parameters.put(TOKENIZATION_KEY, fragment.getAuthorization().toString());
        }

        if (!isBillingAgreement) {
            parameters.put(AMOUNT_KEY, request.getAmount())
                    .put(CURRENCY_ISO_CODE_KEY, currencyCode);
        } else {
            if (!TextUtils.isEmpty(request.getBillingAgreementDescription())) {
                parameters.put(DESCRIPTION_KEY, request.getBillingAgreementDescription());
            }
        }

        JSONObject experienceProfile = new JSONObject();
        experienceProfile.put(NO_SHIPPING_KEY, !request.isShippingAddressRequired());

        if (request.getLocaleCode() != null) {
            experienceProfile.put(LOCALE_CODE_KEY, request.getLocaleCode());
        }

        if (request.getShippingAddressOverride() != null && !request.getShippingAddressOverride().isEmpty()) {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, true);
            PostalAddress shippingAddress = request.getShippingAddressOverride();
            parameters.put(PostalAddress.LINE_1_KEY, shippingAddress.getStreetAddress());
            parameters.put(PostalAddress.LINE_2_KEY, shippingAddress.getExtendedAddress());
            parameters.put(PostalAddress.LOCALITY_KEY, shippingAddress.getLocality());
            parameters.put(PostalAddress.REGION_KEY, shippingAddress.getRegion());
            parameters.put(PostalAddress.POSTAL_CODE_UNDERSCORE_KEY, shippingAddress.getPostalCode());
            parameters.put(PostalAddress.COUNTRY_CODE_UNDERSCORE_KEY, shippingAddress.getCountryCodeAlpha2());
            parameters.put(PostalAddress.RECIPIENT_NAME_UNDERSCORE_KEY, shippingAddress.getRecipientName());
        } else {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, false);
        }

        parameters.put(EXPERIENCE_PROFILE_KEY, experienceProfile);

        String apiUrl = isBillingAgreement ? SETUP_BILLING_AGREEMENT_ENDPOINT : CREATE_SINGLE_PAYMENT_ENDPOINT;
        String versionedPath = "/v1/" + apiUrl;
        fragment.getHttpClient().post(versionedPath, parameters.toString(), callback);
    }

    private static void startPayPal(BraintreeFragment fragment, PendingRequest pendingRequest) {
        if (pendingRequest.isSuccess() && pendingRequest.getRequestTarget() == RequestTarget.wallet) {
            sendAnalyticsForPayPal(fragment, true, RequestTarget.wallet);

            fragment.startActivityForResult(pendingRequest.getIntent(), PAYPAL_REQUEST_CODE);
        } else if (pendingRequest.isSuccess() && pendingRequest.getRequestTarget() == RequestTarget.browser) {
            sendAnalyticsForPayPal(fragment, true, RequestTarget.browser);

            Intent intent = pendingRequest.getIntent()
                    .putExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH, true);
            fragment.startActivity(intent);
        } else {
            sendAnalyticsForPayPal(fragment, false, null);
        }
    }

    private static void sendAnalyticsForPayPal(BraintreeFragment fragment, boolean success, RequestTarget target) {
        String eventFragment = "";
        if (isCheckoutRequest()) {
            if (!success) {
                eventFragment = "paypal-single-payment.initiate.failed";
            } else if (target == RequestTarget.browser) {
                eventFragment = "paypal-single-payment.webswitch.initiate.started";
            } else if (target == RequestTarget.wallet) {
                eventFragment = "paypal-single-payment.appswitch.initiate.started";
            }
        } else {
            if (!success) {
                eventFragment = "paypal-future-payments.initiate.failed";
            } else if (target == RequestTarget.browser) {
                eventFragment = "paypal-future-payments.webswitch.initiate.started";
            } else if (target == RequestTarget.wallet) {
                eventFragment = "paypal-future-payments.appswitch.initiate.started";
            }
        }

        fragment.sendAnalyticsEvent(eventFragment);
    }

    /**
     * The result from PayPal's request.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param data Data associated with the result.
     */
    protected static void onActivityResult(final BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            boolean isAppSwitch = isAppSwitch(data);
            Result result = PayPalOneTouchCore.parseResponse(fragment.getApplicationContext(), sRequest, data);
            switch (result.getResultType()) {
                case Error:
                    fragment.postCallback(new BrowserSwitchException(result.getError().getMessage()));
                    sendAnalyticsEventForSwitchResult(fragment, isAppSwitch, "failed");
                    break;
                case Cancel:
                    fragment.postCancelCallback(PAYPAL_REQUEST_CODE);
                    sendAnalyticsEventForSwitchResult(fragment, isAppSwitch, "canceled");
                    break;
                case Success:
                    onSuccess(fragment, data, result);
                    sendAnalyticsEventForSwitchResult(fragment, isAppSwitch, "succeeded");
                    break;
            }
        } else {
            String type;
            if (sRequest != null) {
                type = sRequest.getClass().getSimpleName().toLowerCase();
            } else {
                type = "unknown";
            }
            fragment.sendAnalyticsEvent("paypal." + type + ".canceled");

            fragment.postCancelCallback(PAYPAL_REQUEST_CODE);
        }
    }

    private static void onSuccess(final BraintreeFragment fragment, Intent data, Result result) {
        TokenizationClient.tokenize(fragment, parseResponse(result, data), new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                fragment.postCallback(paymentMethodNonce);
            }

            @Override
            public void failure(Exception exception) {
                fragment.postCallback(exception);
            }
        });
    }

    /**
     * Parse the PayPal response URL using OneTouchCore.
     *
     * @param result Context that received the result.
     * @param intent The {@link Intent} returned in result.
     * @return A {@link PayPalAccountBuilder} or null if the intent is invalid.
     */
    private static PayPalAccountBuilder parseResponse(Result result, Intent intent) {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .clientMetadataId(sRequest.getClientMetadataId());

        if (isAppSwitch(intent)) {
            paypalAccountBuilder.source("paypal-app");
        } else {
            paypalAccountBuilder.source("paypal-browser");
        }

        JSONObject payload = result.getResponse();
        // Modify payload in 'mock' mode to scope the response
        try {
            JSONObject clientJson = payload.getJSONObject("client");
            JSONObject response = payload.getJSONObject("response");
            if (EnvironmentManager.MOCK.equalsIgnoreCase(clientJson.getString("client"))
                    && response.getString("code") != null && !isCheckoutRequest()) {
                payload.put("response", new JSONObject().put("code",
                        "fake-code:" + ((AuthorizationRequest) sRequest).getScopeString()));
            }
        } catch (JSONException ignored) {}

        paypalAccountBuilder.oneTouchCoreData(payload);

        return paypalAccountBuilder;
    }

    /**
     * Send analytics for PayPal app switch result.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param isAppSwitch True if the request switched to the PayPal app. False if browser switch.
     * @param eventFragment A {@link String} describing the result.
     */
    private static void sendAnalyticsEventForSwitchResult(BraintreeFragment fragment, boolean isAppSwitch,
            String eventFragment) {
        String authorizationType = isCheckoutRequest() ? "paypal-single-payment" : "paypal-future-payments";
        String switchType = isAppSwitch ? "appswitch" : "webswitch";
        String event = String.format("%s.%s.%s", authorizationType, switchType, eventFragment);
        fragment.sendAnalyticsEvent(event);
    }

    @VisibleForTesting
    static CheckoutRequest getCheckoutRequest(String redirectUrl, Context context, PayPalConfiguration configuration) {
        CheckoutRequest request = populateRequestData(new CheckoutRequest(), context, configuration)
                .approvalURL(redirectUrl);

        if (redirectUrl!= null) {
            request.pairingId(Uri.parse(redirectUrl).getQueryParameter("token"));
        }

        return request;
    }

    @VisibleForTesting
    static BillingAgreementRequest getBillingAgreementRequest(String redirectUrl, Context context,
            PayPalConfiguration configuration) {
        BillingAgreementRequest request = populateRequestData(new BillingAgreementRequest(), context, configuration)
                .approvalURL(redirectUrl);

        if (redirectUrl != null) {
            request.pairingId(Uri.parse(redirectUrl).getQueryParameter("ba_token"));
        }

        return request;
    }

    @VisibleForTesting
    static AuthorizationRequest getAuthorizationRequest(Context context, PayPalConfiguration configuration,
            String tokenizationKey) {
        return populateRequestData(new AuthorizationRequest(context), context, configuration)
                .privacyUrl(configuration.getPrivacyUrl())
                .userAgreementUrl(configuration.getUserAgreementUrl())
                .withScopeValue(SCOPE_FUTURE_PAYMENTS)
                .withScopeValue(SCOPE_EMAIL)
                .withAdditionalPayloadAttribute(PAYLOAD_CLIENT_TOKEN_KEY, tokenizationKey);
    }

    private static <T extends Request> T populateRequestData(T request, Context context,
            PayPalConfiguration configuration) {
        String environment;
        switch (configuration.getEnvironment()) {
            case "live":
                environment = EnvironmentManager.LIVE;
                break;
            case "offline":
                environment = EnvironmentManager.MOCK;
                break;
            default:
                environment = configuration.getEnvironment();
                break;
        }

        String clientId = configuration.getClientId();
        if (clientId == null && EnvironmentManager.MOCK.equals(environment)) {
            clientId = "FAKE-PAYPAL-CLIENT-ID";
        }

        request.environment(environment)
                .clientId(clientId)
                .cancelUrl(context.getPackageName() + ".braintree", "cancel")
                .successUrl(context.getPackageName() + ".braintree", "success");

        return request;
    }

    /**
     * Check if the current/last request was a CheckoutRequest
     */
    private static boolean isCheckoutRequest() {
        return sRequest instanceof CheckoutRequest;
    }

    private static boolean isAppSwitch(Intent data) {
        return data.getData() == null;
    }

    private static boolean isManifestValid(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse(context.getPackageName() + ".braintree://"))
                .addCategory(Intent.CATEGORY_DEFAULT)
                .addCategory(Intent.CATEGORY_BROWSABLE);
        ActivityInfo activityInfo = ManifestValidator.getActivityInfo(context, BraintreeBrowserSwitchActivity.class);
        return (activityInfo != null && activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK &&
                AppHelper.isIntentAvailable(context, intent));
    }
}
