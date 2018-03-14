package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Base64;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.BrowserSwitchException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PayPalApprovalCallback;
import com.braintreepayments.api.interfaces.PayPalApprovalHandler;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PayPalAccountNonce;
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
 * Used to create and tokenize PayPal accounts. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/paypal/overview/android/v2">documentation</a>
 */
public class PayPal {

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

    private static final String REQUEST_KEY = "com.braintreepayments.api.PayPal.REQUEST_KEY";
    private static final String REQUEST_TYPE_KEY = "com.braintreepayments.api.PayPal.REQUEST_TYPE_KEY";
    private static final String PAYPAL_REQUEST_KEY = "com.braintreepayments.api.PayPal.PAYPAL_REQUEST_KEY";

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
    private static final String OFFER_CREDIT_KEY = "offer_paypal_credit";
    private static final String CANCEL_URL_KEY = "cancel_url";
    private static final String EXPERIENCE_PROFILE_KEY = "experience_profile";
    private static final String AMOUNT_KEY = "amount";
    private static final String CURRENCY_ISO_CODE_KEY = "currency_iso_code";
    private static final String PAYLOAD_CLIENT_TOKEN_KEY = "client_token";
    private static final String INTENT_KEY = "intent";
    private static final String LANDING_PAGE_TYPE_KEY = "landing_page_type";
    private static final String USER_ACTION_KEY = "useraction";
    private static final String DISPLAY_NAME_KEY = "brand_name";

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

                if (!isManifestValid(fragment)) {
                    fragment.sendAnalyticsEvent("paypal.invalid-manifest");
                    fragment.postCallback(new BraintreeException("BraintreeBrowserSwitchActivity missing, " +
                            "incorrectly configured in AndroidManifest.xml or another app defines the same browser " +
                            "switch url as this app. See " +
                            "https://developers.braintreepayments.com/guides/client-sdk/android/v2#browser-switch " +
                            "for the correct configuration"));
                    return;
                }

                if (configuration.getPayPal().shouldUseBillingAgreement() && !sFuturePaymentsOverride) {
                    requestBillingAgreement(fragment, new PayPalRequest());
                    return;
                }

                fragment.sendAnalyticsEvent("paypal.future-payments.selected");

                AuthorizationRequest request = getAuthorizationRequest(fragment);

                if (additionalScopes != null) {
                    for (String scope : additionalScopes) {
                        request.withScopeValue(scope);
                    }
                }

                startPayPal(fragment, request, null);
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
        requestBillingAgreement(fragment, request, null);
    }

    /**
     * Starts the Billing Agreement flow for PayPal with custom PayPal approval handler.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param request A {@link PayPalRequest} used to customize the request.
     * @param handler A {@link PayPalApprovalHandler} for custom approval handling.
     */
    public static void requestBillingAgreement(BraintreeFragment fragment, PayPalRequest request,
            PayPalApprovalHandler handler) {
        if (request.getAmount() == null) {
            fragment.sendAnalyticsEvent("paypal.billing-agreement.selected");
            if (request.shouldOfferCredit()) {
                fragment.sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
            }

            requestOneTimePayment(fragment, request, true, handler);
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
     * @param request A {@link PayPalRequest} used to customize the request. An amount MUST be specified.
     */
    public static void requestOneTimePayment(BraintreeFragment fragment, PayPalRequest request) {
        requestOneTimePayment(fragment, request, null);
    }

    /**
     * Starts the Single Payment flow for PayPal with custom PayPal approval handler.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param request A {@link PayPalRequest} used to customize the request. An amount MUST be specified.
     * @param handler A {@link PayPalApprovalHandler} for custom approval handling.
     */
    public static void requestOneTimePayment(BraintreeFragment fragment, PayPalRequest request,
            PayPalApprovalHandler handler) {
        if (request.getAmount() != null) {
            fragment.sendAnalyticsEvent("paypal.one-time-payment.selected");
            if (request.shouldOfferCredit()) {
                fragment.sendAnalyticsEvent("paypal.single-payment.credit.offered");
            }

            requestOneTimePayment(fragment, request, false, handler);
        } else {
            fragment.postCallback(new BraintreeException("An amount must be specified for the Single Payment flow."));
        }
    }

    private static void requestOneTimePayment(final BraintreeFragment fragment, final PayPalRequest paypalRequest,
            final boolean isBillingAgreement, final PayPalApprovalHandler handler) {
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

                String redirectUrl = Uri.parse(paypalPaymentResource.getRedirectUrl())
                        .buildUpon()
                        .appendQueryParameter(USER_ACTION_KEY, paypalRequest.getUserAction())
                        .toString();

                Request request;
                if (isBillingAgreement) {
                    request = getBillingAgreementRequest(fragment, redirectUrl);
                } else {
                    request = getCheckoutRequest(fragment, redirectUrl);
                }

                startPayPal(fragment, request, handler);
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

                if (!isManifestValid(fragment)) {
                    fragment.sendAnalyticsEvent("paypal.invalid-manifest");
                    fragment.postCallback(new BraintreeException("BraintreeBrowserSwitchActivity missing, " +
                            "incorrectly configured in AndroidManifest.xml or another app defines the same browser " +
                            "switch url as this app. See " +
                            "https://developers.braintreepayments.com/guides/client-sdk/android/v2#browser-switch " +
                            "for the correct configuration"));
                    return;
                }

                try {
                    persistPayPalRequest(fragment.getApplicationContext(), paypalRequest);
                    createPaymentResource(fragment, paypalRequest, isBillingAgreement, callback);
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
        String currencyCode = request.getCurrencyCode();
        if (currencyCode == null) {
            currencyCode = fragment.getConfiguration().getPayPal().getCurrencyIsoCode();
        }

        CheckoutRequest checkoutRequest = getCheckoutRequest(fragment, null);
        JSONObject parameters = new JSONObject()
                .put(RETURN_URL_KEY, checkoutRequest.getSuccessUrl())
                .put(CANCEL_URL_KEY, checkoutRequest.getCancelUrl())
                .put(OFFER_CREDIT_KEY, request.shouldOfferCredit());

        if (fragment.getAuthorization() instanceof ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY, fragment.getAuthorization().getBearer());
        } else {
            parameters.put(TOKENIZATION_KEY, fragment.getAuthorization().getBearer());
        }

        if (!isBillingAgreement) {
            parameters.put(AMOUNT_KEY, request.getAmount())
                    .put(CURRENCY_ISO_CODE_KEY, currencyCode)
                    .put(INTENT_KEY, request.getIntent());
        } else {
            if (!TextUtils.isEmpty(request.getBillingAgreementDescription())) {
                parameters.put(DESCRIPTION_KEY, request.getBillingAgreementDescription());
            }
        }

        JSONObject experienceProfile = new JSONObject();
        experienceProfile.put(NO_SHIPPING_KEY, !request.isShippingAddressRequired());
        experienceProfile.put(LANDING_PAGE_TYPE_KEY, request.getLandingPageType());
        String displayName = request.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = fragment.getConfiguration().getPayPal().getDisplayName();
        }
        experienceProfile.put(DISPLAY_NAME_KEY, displayName);

        if (request.getLocaleCode() != null) {
            experienceProfile.put(LOCALE_CODE_KEY, request.getLocaleCode());
        }

        if (request.getShippingAddressOverride() != null) {
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

    private static void startPayPal(final BraintreeFragment fragment, Request request, PayPalApprovalHandler handler) {
        persistRequest(fragment.getApplicationContext(), request);
        PayPalApprovalCallback callback = null;

        if (handler == null) {
            handler = getDefaultApprovalHandler(fragment);
        } else {
            callback = new PayPalApprovalCallback() {
                @Override
                public void onComplete(Intent data) {
                    PayPal.onActivityResult(fragment, Activity.RESULT_OK, data);
                }

                @Override
                public void onCancel() {
                    fragment.postCancelCallback(BraintreeRequestCodes.PAYPAL);
                }
            };
        }
        handler.handleApproval(request, callback);
    }

    private static PayPalApprovalHandler getDefaultApprovalHandler(final BraintreeFragment fragment) {
        return new PayPalApprovalHandler() {
            @Override
            public void handleApproval(Request request, PayPalApprovalCallback paypalApprovalCallback) {
                PendingRequest pendingRequest =
                        PayPalOneTouchCore.getStartIntent(fragment.getApplicationContext(), request);
                if (pendingRequest.isSuccess() && pendingRequest.getRequestTarget() == RequestTarget.wallet) {
                    sendAnalyticsForPayPal(fragment, request, true, RequestTarget.wallet);

                    fragment.startActivityForResult(pendingRequest.getIntent(), BraintreeRequestCodes.PAYPAL);
                } else if (pendingRequest.isSuccess() && pendingRequest.getRequestTarget() == RequestTarget.browser) {
                    sendAnalyticsForPayPal(fragment, request, true, RequestTarget.browser);

                    fragment.browserSwitch(BraintreeRequestCodes.PAYPAL, pendingRequest.getIntent());
                } else {
                    sendAnalyticsForPayPal(fragment, request, false, null);
                }
            }
        };
    }

    private static void sendAnalyticsForPayPal(BraintreeFragment fragment, Request request, boolean success,
            RequestTarget target) {
        String eventFragment;
        String authorizationType = authorizationTypeForRequest(request);
        String switchType = target == RequestTarget.wallet ? "appswitch" : "webswitch";
        if (success) {
            eventFragment = String.format("%s.%s.started", authorizationType, switchType);
        } else {
            eventFragment = String.format("%s.initiate.failed", authorizationType);
        }
        /**
         * Possible values:
         * paypal-billing-agreement.webswitch.started
         * paypal-billing-agreement.appswitch.started
         * paypal-billing-agreement.initiate.failed
         * paypal-single-payment.webswitch.started
         * paypal-single-payment.appswitch.started
         * paypal-single-payment.initiate.failed
         * paypal-future-payments.webswitch.started
         * paypal-future-payments.appswitch.started
         * paypal-future-payments.initiate.failed
         */
        fragment.sendAnalyticsEvent(eventFragment);
    }

    /**
     * The result from PayPal's request.
     *
     * @param fragment A {@link BraintreeFragment} used to process the request.
     * @param data Data associated with the result.
     */
    protected static void onActivityResult(final BraintreeFragment fragment, int resultCode, Intent data) {
        Request request = getPersistedRequest(fragment.getApplicationContext());
        if (resultCode == Activity.RESULT_OK && data != null && request != null) {
            boolean isAppSwitch = isAppSwitch(data);
            Result result = PayPalOneTouchCore.parseResponse(fragment.getApplicationContext(), request, data);
            switch (result.getResultType()) {
                case Error:
                    fragment.postCallback(new BrowserSwitchException(result.getError().getMessage()));
                    sendAnalyticsEventForSwitchResult(fragment, request, isAppSwitch, "failed");
                    break;
                case Cancel:
                    sendAnalyticsEventForSwitchResult(fragment, request, isAppSwitch, "canceled");
                    fragment.postCancelCallback(BraintreeRequestCodes.PAYPAL);
                    break;
                case Success:
                    onSuccess(fragment, data, request, result);
                    sendAnalyticsEventForSwitchResult(fragment, request, isAppSwitch, "succeeded");
                    break;
            }
        } else {
            String type;
            if (request != null) {
                type = request.getClass().getSimpleName().toLowerCase();
            } else {
                type = "unknown";
            }
            fragment.sendAnalyticsEvent("paypal." + type + ".canceled");

            if (resultCode != Activity.RESULT_CANCELED) {
                fragment.postCancelCallback(BraintreeRequestCodes.PAYPAL);
            }
        }
    }

    private static void onSuccess(final BraintreeFragment fragment, Intent data, Request request, Result result) {
        PayPalRequest paypalRequest = getPersistedPayPalRequest(fragment.getApplicationContext());
        TokenizationClient.tokenize(fragment, parseResponse(paypalRequest, request, result, data), new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                if (paymentMethodNonce instanceof PayPalAccountNonce &&
                        ((PayPalAccountNonce) paymentMethodNonce).getCreditFinancing() != null) {
                    fragment.sendAnalyticsEvent("paypal.credit.accepted");
                }

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
     * @param paypalRequest Original {@link PayPalRequest} that started this flow.
     * @param result Context that received the result.
     * @param intent The {@link Intent} returned in result.
     * @return A {@link PayPalAccountBuilder} or null if the intent is invalid.
     */
    private static PayPalAccountBuilder parseResponse(PayPalRequest paypalRequest, Request request, Result result,
            Intent intent) {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .clientMetadataId(request.getClientMetadataId());

        if (request instanceof CheckoutRequest && paypalRequest != null) {
            paypalAccountBuilder.intent(paypalRequest.getIntent());
        }

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
                    && response.getString("code") != null && !(request instanceof CheckoutRequest)) {
                payload.put("response", new JSONObject().put("code",
                        "fake-code:" + ((AuthorizationRequest) request).getScopeString()));
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
    private static void sendAnalyticsEventForSwitchResult(BraintreeFragment fragment, Request request,
            boolean isAppSwitch, String eventFragment) {
        String authorizationType = authorizationTypeForRequest(request);
        String switchType = isAppSwitch ? "appswitch" : "webswitch";
        String event = String.format("%s.%s.%s", authorizationType, switchType, eventFragment);
        fragment.sendAnalyticsEvent(event);
    }

    @VisibleForTesting
    static CheckoutRequest getCheckoutRequest(BraintreeFragment fragment, String redirectUrl) {
        CheckoutRequest request = populateRequestData(fragment, new CheckoutRequest())
                .approvalURL(redirectUrl);

        if (redirectUrl != null) {
            request.pairingId(fragment.getApplicationContext(), Uri.parse(redirectUrl).getQueryParameter("token"));
        }

        return request;
    }

    @VisibleForTesting
    static BillingAgreementRequest getBillingAgreementRequest(BraintreeFragment fragment, String redirectUrl) {
        BillingAgreementRequest request = populateRequestData(fragment, new BillingAgreementRequest())
                .approvalURL(redirectUrl);

        if (redirectUrl != null) {
            request.pairingId(fragment.getApplicationContext(), Uri.parse(redirectUrl).getQueryParameter("ba_token"));
        }

        return request;
    }

    @VisibleForTesting
    static AuthorizationRequest getAuthorizationRequest(BraintreeFragment fragment) {
        return populateRequestData(fragment, new AuthorizationRequest(fragment.getApplicationContext()))
                .privacyUrl(fragment.getConfiguration().getPayPal().getPrivacyUrl())
                .userAgreementUrl(fragment.getConfiguration().getPayPal().getUserAgreementUrl())
                .withScopeValue(SCOPE_FUTURE_PAYMENTS)
                .withScopeValue(SCOPE_EMAIL)
                .withAdditionalPayloadAttribute(PAYLOAD_CLIENT_TOKEN_KEY, fragment.getAuthorization().toString());
    }

    private static <T extends Request> T populateRequestData(BraintreeFragment fragment, T request) {
        PayPalConfiguration paypalConfiguration = fragment.getConfiguration().getPayPal();
        String environment;
        switch (paypalConfiguration.getEnvironment()) {
            case "live":
                environment = EnvironmentManager.LIVE;
                break;
            case "offline":
                environment = EnvironmentManager.MOCK;
                break;
            default:
                environment = paypalConfiguration.getEnvironment();
                break;
        }

        String clientId = paypalConfiguration.getClientId();
        if (clientId == null && EnvironmentManager.MOCK.equals(environment)) {
            clientId = "FAKE-PAYPAL-CLIENT-ID";
        }

        request.environment(environment)
                .clientId(clientId)
                .cancelUrl(fragment.getReturnUrlScheme(), "cancel")
                .successUrl(fragment.getReturnUrlScheme(), "success");

        return request;
    }

    private static boolean isAppSwitch(Intent data) {
        return data.getData() == null;
    }

    private static void persistPayPalRequest(Context context, PayPalRequest paypalRequest) {
        Parcel parcel = Parcel.obtain();
        paypalRequest.writeToParcel(parcel, 0);
        BraintreeSharedPreferences.getSharedPreferences(context).edit()
                .putString(PAYPAL_REQUEST_KEY, Base64.encodeToString(parcel.marshall(), 0))
                .apply();
    }

    private static void persistRequest(Context context, Request request) {
        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        BraintreeSharedPreferences.getSharedPreferences(context).edit()
                .putString(REQUEST_KEY, Base64.encodeToString(parcel.marshall(), 0))
                .putString(REQUEST_TYPE_KEY, request.getClass().getSimpleName())
                .apply();
    }

    @Nullable
    private static PayPalRequest getPersistedPayPalRequest(Context context) {
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(context);

        try {
            byte[] requestBytes = Base64.decode(prefs.getString(PAYPAL_REQUEST_KEY, ""), 0);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(requestBytes, 0, requestBytes.length);
            parcel.setDataPosition(0);
            return PayPalRequest.CREATOR.createFromParcel(parcel);
        } catch (Exception ignored) {
        } finally {
            prefs.edit()
                    .remove(PAYPAL_REQUEST_KEY)
                    .apply();
        }

        return null;
    }

    @Nullable
    private static Request getPersistedRequest(Context context) {
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(context);

        try {
            byte[] requestBytes = Base64.decode(prefs.getString(REQUEST_KEY, ""), 0);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(requestBytes, 0, requestBytes.length);
            parcel.setDataPosition(0);

            String type = prefs.getString(REQUEST_TYPE_KEY, "");
            if (AuthorizationRequest.class.getSimpleName().equals(type)) {
                return AuthorizationRequest.CREATOR.createFromParcel(parcel);
            } else if (BillingAgreementRequest.class.getSimpleName().equals(type)) {
                return BillingAgreementRequest.CREATOR.createFromParcel(parcel);
            } else if (CheckoutRequest.class.getSimpleName().equals(type)) {
                return CheckoutRequest.CREATOR.createFromParcel(parcel);
            }
        } catch (Exception ignored) {
        } finally {
            prefs.edit()
                    .remove(REQUEST_KEY)
                    .remove(REQUEST_TYPE_KEY)
                    .apply();
        }

        return null;
    }

    private static boolean isManifestValid(BraintreeFragment fragment) {
        return ManifestValidator.isUrlSchemeDeclaredInAndroidManifest(fragment.getApplicationContext(),
                fragment.getReturnUrlScheme(), BraintreeBrowserSwitchActivity.class);
    }

    private static String authorizationTypeForRequest(Request request) {
        if (request instanceof BillingAgreementRequest) {
            return "paypal-billing-agreement";
        } else if (request instanceof CheckoutRequest) {
            return "paypal-single-payment";
        }
        return "paypal-future-payments";
    }
}
