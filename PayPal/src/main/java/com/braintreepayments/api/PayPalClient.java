package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.paypal.checkout.PayPalCheckout;
import com.paypal.checkout.approve.Approval;
import com.paypal.checkout.approve.OnApprove;
import com.paypal.checkout.config.CheckoutConfig;
import com.paypal.checkout.config.Environment;
import com.paypal.checkout.createorder.CreateOrder;
import com.paypal.checkout.createorder.CreateOrderActions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Used to tokenize PayPal accounts. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/paypal/overview/android/">documentation</a>
 */
public class PayPalClient {

    private final BraintreeClient braintreeClient;
    private final TokenizationClient tokenizationClient;

    private final PayPalInternalClient internalPayPalClient;

    public PayPalClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new TokenizationClient(braintreeClient), new PayPalInternalClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalClient(BraintreeClient braintreeClient, TokenizationClient tokenizationClient, PayPalInternalClient internalPayPalClient) {
        this.braintreeClient = braintreeClient;
        this.tokenizationClient = tokenizationClient;
        this.internalPayPalClient = internalPayPalClient;
    }

    private static boolean payPalConfigInvalid(Configuration configuration) {
        return (configuration == null || !configuration.isPayPalEnabled());
    }

    private boolean browserSwitchNotPossible(FragmentActivity activity) {
        return !braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.PAYPAL);
    }

    private static Exception createPayPalError() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.");
    }

    private static Exception createBrowserSwitchError() {
        return new BraintreeException("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration");
    }

    /**
     * Tokenize a PayPal account for vault or checkout.
     *
     * @param activity      Android FragmentActivity
     * @param payPalRequest a {@link PayPalRequest} used to customize the request.
     * @param callback      {@link PayPalFlowStartedCallback}
     */
    public void tokenizePayPalAccount(final FragmentActivity activity, final PayPalRequest payPalRequest, final PayPalFlowStartedCallback callback) {
        if (payPalRequest instanceof PayPalCheckoutRequest) {
            sendCheckoutRequest(activity, (PayPalCheckoutRequest) payPalRequest, callback);
        } else if (payPalRequest instanceof PayPalVaultRequest) {
            sendVaultRequest(activity, (PayPalVaultRequest) payPalRequest, callback);
        }
    }

    /**
     * @param activity              Android FragmentActivity
     * @param payPalCheckoutRequest a {@link PayPalCheckoutRequest} used to customize the request.
     * @param callback              {@link PayPalFlowStartedCallback}
     * @deprecated Use {@link PayPalClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest, PayPalFlowStartedCallback)} instead.
     * Starts the One-Time Payment (Checkout) flow for PayPal.
     */
    @Deprecated
    public void requestOneTimePayment(final FragmentActivity activity, final PayPalCheckoutRequest payPalCheckoutRequest, final PayPalFlowStartedCallback callback) {
        tokenizePayPalAccount(activity, payPalCheckoutRequest, callback);
    }

    /**
     * @param activity           Android FragmentActivity
     * @param payPalVaultRequest a {@link PayPalVaultRequest} used to customize the request.
     * @param callback           {@link PayPalFlowStartedCallback}
     * @deprecated Use {@link PayPalClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest, PayPalFlowStartedCallback)} instead.
     * Starts the Billing Agreement (Vault) flow for PayPal.
     */
    @Deprecated
    public void requestBillingAgreement(final FragmentActivity activity, final PayPalVaultRequest payPalVaultRequest, final PayPalFlowStartedCallback callback) {
        tokenizePayPalAccount(activity, payPalVaultRequest, callback);
    }

    private void sendCheckoutRequest(final FragmentActivity activity, final PayPalCheckoutRequest payPalCheckoutRequest, final PayPalFlowStartedCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.single-payment.selected");
        if (payPalCheckoutRequest.getShouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal.single-payment.paylater.offered");
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (payPalConfigInvalid(configuration)) {
                    Exception configInvalidError = createPayPalError();
                    callback.onResult(configInvalidError);
                    return;
                }

                if (browserSwitchNotPossible(activity)) {
                    braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                    Exception manifestInvalidError = createBrowserSwitchError();
                    callback.onResult(manifestInvalidError);
                    return;
                }
                String clientId = configuration.getPayPalClientId() != null ? configuration.getPayPalClientId() : "Client ID Is Null";
                String redirectUrl = getPayPalReturnUrl(activity);
                Environment environment = Environment.SANDBOX;
                if ("production".equalsIgnoreCase(configuration.getEnvironment())) {
                    environment = Environment.LIVE;
                }

                CheckoutConfig checkoutConfig = new CheckoutConfig(
                    activity.getApplication(),
                    clientId,
                    environment,
                    redirectUrl
                );
                PayPalCheckout.setConfig(checkoutConfig);
                sendPayPalRequest(activity, payPalCheckoutRequest, callback);
            }
        });

    }

    private String getPayPalReturnUrl(Context context) {
        if (context != null) {
            return String.format("%s://paypalpay", context.getPackageName().toLowerCase(Locale.ROOT));
        }
        return null;
    }

    private void sendVaultRequest(final FragmentActivity activity, final PayPalVaultRequest payPalVaultRequest, final PayPalFlowStartedCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.selected");
        if (payPalVaultRequest.getShouldOfferCredit()) {
            braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (payPalConfigInvalid(configuration)) {
                    Exception configInvalidError = createPayPalError();
                    callback.onResult(configInvalidError);
                    return;
                }

                if (browserSwitchNotPossible(activity)) {
                    braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                    Exception manifestInvalidError = createBrowserSwitchError();
                    callback.onResult(manifestInvalidError);
                    return;
                }

                sendPayPalRequest(activity, payPalVaultRequest, callback);
            }
        });
    }

    private void sendPayPalRequest(final FragmentActivity activity, final PayPalRequest payPalRequest, final PayPalFlowStartedCallback callback) {
        internalPayPalClient.sendRequest(activity, payPalRequest, new PayPalInternalClientCallback() {
            @Override
            public void onResult(PayPalResponse payPalResponse, Exception error) {
                if (payPalResponse != null) {
                    String analyticsPrefix = getAnalyticsEventPrefix(payPalRequest);
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.started", analyticsPrefix));

                    try {
                        startBrowserSwitch(activity, payPalResponse);
                        callback.onResult(null);
                    } catch (JSONException | BrowserSwitchException exception) {
                        callback.onResult(exception);
                    }
                } else {
                    callback.onResult(error);
                }
            }
        });
    }

    private void startBrowserSwitch(final FragmentActivity activity, final PayPalResponse payPalResponse) throws JSONException, BrowserSwitchException {
        JSONObject metadata = new JSONObject();
        metadata.put("approval-url", payPalResponse.getApprovalUrl());
        metadata.put("success-url", payPalResponse.getSuccessUrl());

        String paymentType = payPalResponse.isBillingAgreement()
            ? "billing-agreement" : "single-payment";

        metadata.put("payment-type", paymentType);
        metadata.put("client-metadata-id", payPalResponse.getClientMetadataId());
        metadata.put("merchant-account-id", payPalResponse.getMerchantAccountId());
        metadata.put("source", "paypal-browser");
        metadata.put("intent", payPalResponse.getIntent());

        final BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
            .requestCode(BraintreeRequestCodes.PAYPAL)
            .url(Uri.parse(payPalResponse.getApprovalUrl()))
            .returnUrlScheme(braintreeClient.getReturnUrlScheme())
            .metadata(metadata);
        if (payPalResponse.isBillingAgreement()) {
            braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
        } else {
            PayPalCheckout.start(
                new CreateOrder() {
                    @Override
                    public void create(@NonNull CreateOrderActions createOrderActions) {
                        Context appContext = activity.getApplicationContext();
                        Uri browserSwitchUrl = browserSwitchOptions.getUrl();
                        int requestCode = browserSwitchOptions.getRequestCode();
                        String returnUrlScheme = browserSwitchOptions.getReturnUrlScheme();
                        JSONObject metadata = browserSwitchOptions.getMetadata();
                        BrowserSwitchRequest request =
                            new BrowserSwitchRequest(requestCode, browserSwitchUrl, metadata, returnUrlScheme);
                        BrowserSwitchPersistentStore persistentStore = BrowserSwitchPersistentStore.getInstance();
                        persistentStore.putActiveRequest(request, appContext);
                        createOrderActions.set(payPalResponse.getPairingId());
                    }
                },
                new OnApprove() {
                    @Override
                    public void onApprove(@NonNull Approval approval) {
                        Intent currentIntent = activity.getIntent();
                        String data = String.format(
                            "%s://onetouch/v1/success?token=%s",
                            braintreeClient.getReturnUrlScheme(),
                            approval.getData().getOrderId()
                        );
                        currentIntent.setData(Uri.parse(data));
                        activity.setIntent(currentIntent);
                    }
                }
            );
        }
    }

    private static String getAnalyticsEventPrefix(PayPalRequest request) {
        return request instanceof PayPalVaultRequest ? "paypal.billing-agreement" : "paypal.single-payment";
    }

    /**
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback            {@link PayPalBrowserSwitchResultCallback}
     */
    public void onBrowserSwitchResult(BrowserSwitchResult browserSwitchResult, final PayPalBrowserSwitchResultCallback callback) {
        if (browserSwitchResult == null) {
            callback.onResult(null, new BraintreeException("BrowserSwitchResult cannot be null"));
            return;
        }
        JSONObject metadata = browserSwitchResult.getRequestMetadata();
        String clientMetadataId = Json.optString(metadata, "client-metadata-id", null);
        String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);
        String payPalIntent = Json.optString(metadata, "intent", null);
        String approvalUrl = Json.optString(metadata, "approval-url", null);
        String successUrl = Json.optString(metadata, "success-url", null);
        String paymentType = Json.optString(metadata, "payment-type", "unknown");

        boolean isBillingAgreement = paymentType.equalsIgnoreCase("billing-agreement");
        String tokenKey = isBillingAgreement ? "ba_token" : "token";
        String analyticsPrefix = isBillingAgreement ? "paypal.billing-agreement" : "paypal.single-payment";

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                callback.onResult(null, new BraintreeException("User Canceled PayPal"));
                braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.canceled", analyticsPrefix));
                break;
            case BrowserSwitchStatus.SUCCESS:
                try {
                    Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                    if (deepLinkUri != null) {
                        JSONObject urlResponseData = parseUrlResponseData(deepLinkUri, successUrl, approvalUrl, tokenKey);
                        PayPalAccount payPalAccount = new PayPalAccount();
                        payPalAccount.setClientMetadataId(clientMetadataId);
                        payPalAccount.setIntent(payPalIntent);
                        payPalAccount.setSource("paypal-browser");
                        payPalAccount.setUrlResponseData(urlResponseData);

                        if (merchantAccountId != null) {
                            payPalAccount.setMerchantAccountId(merchantAccountId);
                        }

                        if (payPalIntent != null) {
                            payPalAccount.setIntent(payPalIntent);
                        }

                        tokenizationClient.tokenize(payPalAccount, new TokenizeCallback() {
                            @Override
                            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                                if (tokenizationResponse != null) {
                                    try {
                                        PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJSON(tokenizationResponse);
                                        if (payPalAccountNonce.getCreditFinancing() != null) {
                                            braintreeClient.sendAnalyticsEvent("paypal.credit.accepted");
                                        }
                                        callback.onResult(payPalAccountNonce, null);

                                    } catch (JSONException e) {
                                        callback.onResult(null, e);
                                    }
                                } else {
                                    callback.onResult(null, exception);
                                }
                            }
                        });

                        braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.succeeded", analyticsPrefix));
                    } else {
                        callback.onResult(null, new BraintreeException("Unknown error"));
                    }
                } catch (JSONException | PayPalBrowserSwitchException e) {
                    callback.onResult(null, e);
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.failed", analyticsPrefix));
                }
                break;
        }
    }

    private JSONObject parseUrlResponseData(Uri uri, String successUrl, String approvalUrl, String tokenKey) throws JSONException, PayPalBrowserSwitchException {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(successUrl).getLastPathSegment().equals(status)) {
            throw new PayPalBrowserSwitchException("User cancelled.");
        }

        String requestXoToken = Uri.parse(approvalUrl).getQueryParameter(tokenKey);
        String responseXoToken = uri.getQueryParameter(tokenKey);
        if (responseXoToken != null && TextUtils.equals(requestXoToken, responseXoToken)) {
            JSONObject client = new JSONObject();
            client.put("environment", null);

            JSONObject urlResponseData = new JSONObject();
            urlResponseData.put("client", client);

            JSONObject response = new JSONObject();
            response.put("webURL", uri.toString());
            urlResponseData.put("response", response);

            urlResponseData.put("response_type", "web");

            return urlResponseData;
        } else {
            throw new PayPalBrowserSwitchException("The response contained inconsistent data.");
        }
    }
}
