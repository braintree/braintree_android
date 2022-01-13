package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to tokenize PayPal accounts. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/paypal/overview/android/">documentation</a>
 */
public class PayPalClient {

    private final BraintreeClient braintreeClient;
    private final ApiClient apiClient;

    private final PayPalInternalClient internalPayPalClient;

    private PayPalListener payPalListener;
    private PayPalBrowserSwitchObserver payPalBrowserSwitchObserver;

    public PayPalClient(Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(fragment.getLifecycle(), braintreeClient, new ApiClient(braintreeClient), new PayPalInternalClient(braintreeClient));
    }

    public PayPalClient(FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {
        this(activity.getLifecycle(), braintreeClient, new ApiClient(braintreeClient), new PayPalInternalClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalClient(Lifecycle lifecycle, BraintreeClient braintreeClient, ApiClient apiClient, PayPalInternalClient internalPayPalClient) {
        this.braintreeClient = braintreeClient;
        this.apiClient = apiClient;
        this.internalPayPalClient = internalPayPalClient;

        this.payPalBrowserSwitchObserver = new PayPalBrowserSwitchObserver(this);
        lifecycle.addObserver(payPalBrowserSwitchObserver);
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
     *  @param activity      Android FragmentActivity
     * @param payPalRequest a {@link PayPalRequest} used to customize the request.
     */
    public void tokenizePayPalAccount(@NonNull final FragmentActivity activity, @NonNull final PayPalRequest payPalRequest) {
        if (payPalRequest instanceof PayPalCheckoutRequest) {
            sendCheckoutRequest(activity, (PayPalCheckoutRequest) payPalRequest);
        } else if (payPalRequest instanceof PayPalVaultRequest) {
            sendVaultRequest(activity, (PayPalVaultRequest) payPalRequest);
        }
    }

    /**
     * @param activity              Android FragmentActivity
     * @param payPalCheckoutRequest a {@link PayPalCheckoutRequest} used to customize the request.
     * @param callback              {@link PayPalFlowStartedCallback}
     * @deprecated Use {@link PayPalClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest)} instead.
     * Starts the One-Time Payment (Checkout) flow for PayPal.
     */
    @Deprecated
    public void requestOneTimePayment(@NonNull final FragmentActivity activity, @NonNull final PayPalCheckoutRequest payPalCheckoutRequest, @NonNull final PayPalFlowStartedCallback callback) {
        tokenizePayPalAccount(activity, payPalCheckoutRequest);
    }

    /**
     * @param activity           Android FragmentActivity
     * @param payPalVaultRequest a {@link PayPalVaultRequest} used to customize the request.
     * @param callback           {@link PayPalFlowStartedCallback}
     * @deprecated Use {@link PayPalClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest)} instead.
     * Starts the Billing Agreement (Vault) flow for PayPal.
     */
    @Deprecated
    public void requestBillingAgreement(@NonNull final FragmentActivity activity, @NonNull final PayPalVaultRequest payPalVaultRequest, @NonNull final PayPalFlowStartedCallback callback) {
        tokenizePayPalAccount(activity, payPalVaultRequest);
    }

    private void sendCheckoutRequest(final FragmentActivity activity, final PayPalCheckoutRequest payPalCheckoutRequest) {
        braintreeClient.sendAnalyticsEvent("paypal.single-payment.selected");
        if (payPalCheckoutRequest.getShouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal.single-payment.paylater.offered");
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (payPalConfigInvalid(configuration)) {
                    Exception configInvalidError = createPayPalError();
                    notifyTokenizeError(configInvalidError);
                    return;
                }

                if (browserSwitchNotPossible(activity)) {
                    braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                    Exception manifestInvalidError = createBrowserSwitchError();
                    notifyTokenizeError(manifestInvalidError);
                    return;
                }
                sendPayPalRequest(activity, payPalCheckoutRequest);
            }
        });

    }

    private void sendVaultRequest(final FragmentActivity activity, final PayPalVaultRequest payPalVaultRequest) {
        braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.selected");
        if (payPalVaultRequest.getShouldOfferCredit()) {
            braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (payPalConfigInvalid(configuration)) {
                    Exception configInvalidError = createPayPalError();
                    notifyTokenizeError(configInvalidError);
                    return;
                }

                if (browserSwitchNotPossible(activity)) {
                    braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                    Exception manifestInvalidError = createBrowserSwitchError();
                    notifyTokenizeError(manifestInvalidError);
                    return;
                }

                sendPayPalRequest(activity, payPalVaultRequest);
            }
        });
    }

    private void sendPayPalRequest(final FragmentActivity activity, final PayPalRequest payPalRequest) {
        internalPayPalClient.sendRequest(activity, payPalRequest, new PayPalInternalClientCallback() {
            @Override
            public void onResult(PayPalResponse payPalResponse, Exception error) {
                if (payPalResponse != null) {
                    String analyticsPrefix = getAnalyticsEventPrefix(payPalRequest);
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.started", analyticsPrefix));

                    try {
                        startBrowserSwitch(activity, payPalResponse);
                        // TODO: consider notifying PayPalListener that browser switch has started
                    } catch (JSONException | BrowserSwitchException exception) {
                        notifyTokenizeError(exception);
                    }
                } else {
                    notifyTokenizeError(error);
                }
            }
        });
    }

    private void startBrowserSwitch(FragmentActivity activity, PayPalResponse payPalResponse) throws JSONException, BrowserSwitchException {
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

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.PAYPAL)
                .url(Uri.parse(payPalResponse.getApprovalUrl()))
                .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                .metadata(metadata);
        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
    }

    private static String getAnalyticsEventPrefix(PayPalRequest request) {
        return request instanceof PayPalVaultRequest ? "paypal.billing-agreement" : "paypal.single-payment";
    }

    /**
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     */
    public void onBrowserSwitchResult(@NonNull BrowserSwitchResult browserSwitchResult) {
        //noinspection ConstantConditions
        if (browserSwitchResult == null) {
            notifyTokenizeError(new BraintreeException("BrowserSwitchResult cannot be null"));
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
                notifyTokenizeError(new UserCanceledException("User canceled PayPal."));
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
                        payPalAccount.setPaymentType(paymentType);

                        if (merchantAccountId != null) {
                            payPalAccount.setMerchantAccountId(merchantAccountId);
                        }

                        if (payPalIntent != null) {
                            payPalAccount.setIntent(payPalIntent);
                        }

                        apiClient.tokenizeREST(payPalAccount, new TokenizeCallback() {
                            @Override
                            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                                if (tokenizationResponse != null) {
                                    try {
                                        PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJSON(tokenizationResponse);
                                        if (payPalAccountNonce.getCreditFinancing() != null) {
                                            braintreeClient.sendAnalyticsEvent("paypal.credit.accepted");
                                        }
                                        notifyTokenizeSuccess(payPalAccountNonce);

                                    } catch (JSONException e) {
                                        notifyTokenizeError(e);
                                    }
                                } else {
                                    notifyTokenizeError(exception);
                                }
                            }
                        });

                        braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.succeeded", analyticsPrefix));
                    } else {
                        notifyTokenizeError(new BraintreeException("Unknown error"));
                    }
                } catch (UserCanceledException e) {
                    notifyTokenizeError(e);
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.canceled", analyticsPrefix));
                } catch (JSONException | PayPalBrowserSwitchException e) {
                    notifyTokenizeError(e);
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.failed", analyticsPrefix));
                }
                break;
        }
    }

    private JSONObject parseUrlResponseData(Uri uri, String successUrl, String approvalUrl, String tokenKey) throws JSONException, UserCanceledException, PayPalBrowserSwitchException {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(successUrl).getLastPathSegment().equals(status)) {
            throw new UserCanceledException("User canceled PayPal.");
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

    public void deliverBrowserSwitchResult(FragmentActivity activity) {
        BrowserSwitchResult pendingResult = braintreeClient.getBrowserSwitchResult(activity);
        if (pendingResult != null && pendingResult.getRequestCode() == BraintreeRequestCodes.PAYPAL) {
            BrowserSwitchResult browserSwitchResult =
                    braintreeClient.deliverBrowserSwitchResult(activity);
            onBrowserSwitchResult(browserSwitchResult);
        }
    }

    public void setPayPalListener(PayPalListener payPalListener) {
        this.payPalListener = payPalListener;
    }

    private void notifyTokenizeError(Exception error) {
        if (payPalListener != null) {
            payPalListener.onPayPalTokenizeError(error);
        }
        // TODO: consider caching result when listener is null and re-notify when listener is registered
    }

    private void notifyTokenizeSuccess(PayPalAccountNonce payPalAccountNonce) {
        if (payPalListener != null) {
            payPalListener.onPayPalTokenizeSuccess(payPalAccountNonce);
        }
        // TODO: consider caching result when listener is null and re-notify when listener is registered
    }
}
