package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PayPalConfiguration;
import com.braintreepayments.api.models.PayPalPaymentResource;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.BrowserSwitchAdapter;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchActivity;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;
import com.paypal.android.sdk.onetouch.core.PerformRequestStatus;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.Result;
import com.paypal.android.sdk.onetouch.core.ResultType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class PayPal {

    public static final String SCOPE_FUTURE_PAYMENTS =
            "https://uri.paypal.com/services/payments/futurepayments";
    public static final String SCOPE_EMAIL = "email";
    public static final String SCOPE_ADDRESS = "address";

    public static final String PAYLOAD_KEY_CLIENT_TOKEN = "client_token";

    private static final String TAG = PayPal.class.getSimpleName();

    protected static boolean sEnableSignatureVerification = true;
    private static Request sPendingRequest;
    private static PerformRequestStatus sPendingRequestStatus;

    private PayPal() {
        throw new IllegalStateException("Non-instantiable class.");
    }

    /**
     * Used to launch an AuthorizationRequest (Future Payments) which will result in an App or
     * Browser switch. The user will then be prompted to verify their account and then return
     * automatically. A nonce will be created if authorization was successful.
     */
    protected static PerformRequestStatus launchPayPal(final Activity activity, final int requestCode,
            Configuration configuration, ClientToken clientToken, final List<String> additionalScopes) throws ConfigurationException {
        sPendingRequest = buildPayPalAuthorizationConfiguration(activity, configuration,
                clientToken);

        if (additionalScopes != null) {
            for (String scope : additionalScopes) {
                ((AuthorizationRequest)sPendingRequest).withScopeValue(scope);
            }
        }

        sPendingRequestStatus =  PayPalOneTouchCore.performRequest(activity,
                sPendingRequest,
                requestCode,
                sEnableSignatureVerification,
                new BrowserSwitchAdapter() {
                    @Override
                    public void handleBrowserSwitchIntent(Intent intent) {

                        activity.startActivityForResult(
                                new Intent(activity, BraintreeBrowserSwitchActivity.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                        .putExtra(
                                                BraintreeBrowserSwitchActivity.EXTRA_INTENT,
                                                intent), requestCode);

                    }
                });
        return sPendingRequestStatus;
    }

    /**
     * Used to launch a Checkout Request (Single Payment) which will result in an App or Browser
     * switch. The user will then be prompted to verify their account and then return automatically.
     * A nonce will be created if authorization was successful.
     */
    protected static PerformRequestStatus checkoutWithPayPal(PayPalPaymentResource payPalPaymentResource,
            final Activity activity, final int requestCode,
            com.braintreepayments.api.models.Configuration configuration) throws ConfigurationException {

        sPendingRequest =
                buildPayPalCheckoutConfiguration(payPalPaymentResource.getRedirectUrl(), activity,
                        configuration);

        sPendingRequestStatus = PayPalOneTouchCore.performRequest(activity,
                sPendingRequest,
                requestCode,
                sEnableSignatureVerification,
                new BrowserSwitchAdapter() {
                    @Override
                    public void handleBrowserSwitchIntent(Intent intent) {

                        activity.startActivityForResult(

                                new Intent(activity, BraintreeBrowserSwitchActivity.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                        .putExtra(
                                                BraintreeBrowserSwitchActivity.EXTRA_INTENT,
                                                intent), requestCode);

                    }
                });
        return sPendingRequestStatus;
    }

    /**
     * Used to create a {@link com.braintreepayments.api.models.PayPalAccountBuilder} from an
     * context response. Not necessary to use, {@link com.braintreepayments.api.Braintree#finishPayWithPayPal(android.app.Activity,
     * int, android.content.Intent)} does this for you.
     *
     * @param context Activity that received the result.
     * @param resultCode Result code returned in result.
     * @param intent {@link Intent} returned in result.
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder} or null if resultCode
     * is not both {@link android.app.Activity#RESULT_OK} and a valid response from PayPal
     */
    protected static PayPalAccountBuilder getBuilderFromActivity(Context context, int resultCode,
            Intent intent) throws ConfigurationException {

        if (resultCode != Activity.RESULT_OK || context == null) {
            throw new ConfigurationException("Cannot return PayPalAccountBuilder with invalid context or resultCode");
        }

        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .correlationId(sPendingRequestStatus.getClientMetadataId());

        Result result = getResultFromActivity(context, resultCode, intent);
        if (intent.hasExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT)) {
            paypalAccountBuilder.source("paypal-app");
        } else {
            paypalAccountBuilder.source("paypal-browser");
        }

        ResultType resultType = result.getResultType();
        switch (resultType) {
            case Error:
                return null;
            case Cancel:
                return null;
            case Success:
                JSONObject response = result.getResponse();

                JSONObject user = response.optJSONObject("user");
                if (user != null) {
                    paypalAccountBuilder.email(user.optString(
                            "display_string"));
                }

                // Modify payload in 'mock' mode to scope the response
                try {
                    if (response.optJSONObject("client").optString("environment").equalsIgnoreCase(AuthorizationRequest.ENVIRONMENT_MOCK)
                            && response.optJSONObject("response").optString("code") != null
                            && !isCheckoutRequest()) {
                        response.put("response", new JSONObject()
                                .put("code", "fake-code:" + ((AuthorizationRequest)
                                        sPendingRequest).getScopeString()));
                    }
                } catch (JSONException ignored) {}


                paypalAccountBuilder.OtcResponse(response);

                break;
        }

        return paypalAccountBuilder;
    }

    /**
     * Used to retrieve the Result to send the correct analytics events
     *
     * @param context Activity that received the result.
     * @param resultCode Result code returned in result.
     * @param intent {@link Intent} returned in result.
     * @return the {@link Result} of the OTC app/browser switch
     * @throws ConfigurationException
     */
    protected static Result getResultFromActivity(Context context, int resultCode,
            Intent intent) throws ConfigurationException {

        if (resultCode != Activity.RESULT_OK || context == null) {
            throw new ConfigurationException("Cannot return PayPalAccountBuilder with invalid context or resultCode");
        }

        Result result;
        if (intent.hasExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT)) {
            result = intent.getParcelableExtra(
                    PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT);
        } else {
            result = PayPalOneTouchCore.handleBrowserResponse(context, intent.getData(),
                    sPendingRequest);
        }

        return result;
    }

    /**
     * Check if the current/last request was a CheckoutRequest
     */
    public static Boolean isCheckoutRequest() {
        return sPendingRequest != null && sPendingRequest instanceof CheckoutRequest;
    }

    /**
     * Check if the intent is indeed a PayPal intent
     */
    public static boolean isPayPalIntent(Intent intent) {
        return intent.hasExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT);
    }

    /**
     * Set properties specific to an CheckoutRequest
     */
    protected static CheckoutRequest buildPayPalCheckoutConfiguration(String approvalUrl,
            Context context,
            Configuration configuration) throws ConfigurationException {

        validatePayPalConfiguration(configuration);

        String pairingId = null;
        if (approvalUrl != null) {
            pairingId = Uri.parse(approvalUrl).getQueryParameter("token");
        }
        return populateCommonData(new CheckoutRequest(), context, configuration)
                .pairingId(pairingId)
                .approvalURL(approvalUrl);
    }

    /**
     * Set properties specific to an AuthorizationRequest
     */
    protected static AuthorizationRequest buildPayPalAuthorizationConfiguration(Context context,
            Configuration configuration, ClientToken clientToken) throws ConfigurationException {
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        validatePayPalConfiguration(configuration);

        return populateCommonData(new AuthorizationRequest(context), context, configuration)
                .privacyUrl(payPalConfiguration.getPrivacyUrl())
                .userAgreementUrl(payPalConfiguration.getUserAgreementUrl())
                .withScopeValue(SCOPE_FUTURE_PAYMENTS)
                .withScopeValue(SCOPE_EMAIL)
                .withAdditionalPayloadAttribute(PAYLOAD_KEY_CLIENT_TOKEN,
                        clientToken.getOriginalValue());
    }

    /**
     * Set properties of a Request that are common to both CheckoutRequest and AuthorizationRequest
     */
    protected static <T extends Request> T populateCommonData(T request, Context context,
            Configuration configuration) {
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();
        String environment;
        if (payPalConfiguration.getEnvironment().equals("live")) {
            environment = AuthorizationRequest.ENVIRONMENT_LIVE;
        } else if (payPalConfiguration.getEnvironment().equals("offline")) {
            environment = AuthorizationRequest.ENVIRONMENT_MOCK;
        } else {
            environment = payPalConfiguration.getEnvironment();
        }
        String clientId = payPalConfiguration.getClientId();
        if (clientId == null && environment == AuthorizationRequest.ENVIRONMENT_MOCK) {
            clientId = "FAKE-PAYPAL-CLIENT-ID";
        }
        request.environment(environment);
        request.clientId(clientId);
        request.cancelUrl(context.getPackageName() + ".braintree", BraintreeBrowserSwitchActivity.X_CANCEL_HOST);
        request.successUrl(context.getPackageName() + ".braintree", BraintreeBrowserSwitchActivity.X_SUCCESS_HOST);
        return request;
    }

    /**
     * Throws a {@link ConfigurationException} when the config is invalid
     * @param configuration
     * @throws ConfigurationException
     */
    public static void validatePayPalConfiguration(Configuration configuration) throws ConfigurationException {
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        if (!configuration.isPayPalEnabled() ||
                payPalConfiguration.getEnvironment() == null ||
                payPalConfiguration.getPrivacyUrl() == null ||
                payPalConfiguration.getUserAgreementUrl() == null) {
            throw new ConfigurationException("PayPal is disabled or configuration is invalid");
        }
    }
}
