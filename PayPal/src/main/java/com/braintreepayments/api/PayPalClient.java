package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
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
 * <a href="https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4">documentation</a>
 */
public class PayPalClient {

    private final BraintreeClient braintreeClient;
    private final PayPalInternalClient internalPayPalClient;

    private PayPalListener listener;

    /**
     * Used for linking events from the client to server side request
     * In the PayPal flow this will be either an EC token or a Billing Agreement token
     */
    private String payPalContextId = null;

    /**
     * True if `tokenize()` was called with a Vault request object type
     */
    private Boolean isVaultRequest = false;

    @VisibleForTesting
    BrowserSwitchResult pendingBrowserSwitchResult;

    /**
     * Create a new instance of {@link PayPalClient} from within an Activity using a {@link BraintreeClient}.
     *
     * @param activity        a {@link FragmentActivity}
     * @param braintreeClient a {@link BraintreeClient}
     */
    public PayPalClient(@NonNull FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {
        this(activity, activity.getLifecycle(), braintreeClient, new PayPalInternalClient(braintreeClient));
    }

    /**
     * Create a new instance of {@link PayPalClient} from within a Fragment using a {@link BraintreeClient}.
     *
     * @param fragment        a {@link Fragment
     * @param braintreeClient a {@link BraintreeClient}
     */
    public PayPalClient(@NonNull Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(fragment.getActivity(), fragment.getLifecycle(), braintreeClient, new PayPalInternalClient(braintreeClient));
    }

    /**
     * Create a new instance of {@link PayPalClient} using a {@link BraintreeClient}.
     * <p>
     * Use this constructor with the manual browser switch integration pattern.
     *
     * @param braintreeClient a {@link BraintreeClient}
     */
    public PayPalClient(@NonNull BraintreeClient braintreeClient) {
        this(null, null, braintreeClient, new PayPalInternalClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalClient(FragmentActivity activity, Lifecycle lifecycle, BraintreeClient braintreeClient, PayPalInternalClient internalPayPalClient) {
        this.braintreeClient = braintreeClient;
        this.internalPayPalClient = internalPayPalClient;
        if (activity != null && lifecycle != null) {
            PayPalLifecycleObserver observer = new PayPalLifecycleObserver(this);
            lifecycle.addObserver(observer);
        }
    }

    /**
     * Add a {@link PayPalListener} to your client to receive results or errors from the PayPal flow.
     * This method must be invoked on a {@link PayPalClient(Fragment, BraintreeClient)} or
     * {@link PayPalClient(FragmentActivity, BraintreeClient)} in order to receive results.
     *
     * @param listener a {@link PayPalListener}
     */
    public void setListener(PayPalListener listener) {
        this.listener = listener;
        if (pendingBrowserSwitchResult != null) {
            deliverBrowserSwitchResultToListener(pendingBrowserSwitchResult);
        }
    }

    private static boolean payPalConfigInvalid(Configuration configuration) {
        return (configuration == null || !configuration.isPayPalEnabled());
    }

    /**
     * After calling {@link PayPalClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest)},
     * call this method in your Activity or Fragment's onResume() method to see if a response
     * was provided through deep linking.
     *
     * If a BrowserSwitchResult exists, call {@link PayPalClient#onBrowserSwitchResult(BrowserSwitchResult, PayPalBrowserSwitchResultCallback)}
     * to allow the SDK to continue tokenization of the PayPalAccount.
     *
     * Make sure to call {@link PayPalClient#clearActiveBrowserSwitchRequests(Context)} after
     * successfully parsing a BrowserSwitchResult to guard against multiple invocations of browser
     * switch event handling.
     *
     * @param context The context used to check for pending browser switch requests
     * @param intent The intent containing a potential deep link response. May be null.
     * @return {@link BrowserSwitchResult} when a result has been parsed successfully from a deep link; null when an input Intent is null
     */
    @Nullable
    public BrowserSwitchResult parseBrowserSwitchResult(@NonNull Context context, @Nullable Intent intent) {
        int requestCode = BraintreeRequestCodes.PAYPAL;
        return braintreeClient.parseBrowserSwitchResult(context, requestCode, intent);
    }

    /**
     * Make sure to call this method after {@link PayPalClient#parseBrowserSwitchResult(Context, Intent)}
     * parses a {@link BrowserSwitchResult} successfully to prevent multiple invocations of browser
     * switch event handling logic.
     *
     * @param context The context used to clear pending browser switch requests
     */
    public void clearActiveBrowserSwitchRequests(@NonNull Context context) {
        braintreeClient.clearActiveBrowserSwitchRequests(context);
    }

    private void assertCanPerformBrowserSwitch(FragmentActivity activity) throws BrowserSwitchException {
        braintreeClient.assertCanPerformBrowserSwitch(activity, BraintreeRequestCodes.PAYPAL);
    }

    private static Exception createPayPalError() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4 " +
                "for more information.");
    }

    private static Exception createBrowserSwitchError(BrowserSwitchException exception) {
        return new BraintreeException("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#browser-switch-setup " +
                "for the correct configuration: " + exception.getMessage());
    }

    /**
     * Tokenize a PayPal account for vault or checkout.
     * <p>
     * This method must be invoked on a {@link PayPalClient(Fragment, BraintreeClient)} or
     * {@link PayPalClient(FragmentActivity, BraintreeClient)} in order to receive results.
     *
     * @param activity      Android FragmentActivity
     * @param payPalRequest a {@link PayPalRequest} used to customize the request.
     */
    public void tokenizePayPalAccount(@NonNull final FragmentActivity activity, @NonNull final PayPalRequest payPalRequest) {
        tokenizePayPalAccount(activity, payPalRequest, new PayPalFlowStartedCallback() {
            @Override
            public void onResult(@Nullable Exception error) {
                if (error != null && listener != null) {
                    listener.onPayPalFailure(error);
                }
            }
        });
    }

    /**
     * Tokenize a PayPal account for vault or checkout.
     * <p>
     * Deprecated. Use {@link PayPalClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest)}
     *
     * @param activity      Android FragmentActivity
     * @param payPalRequest a {@link PayPalRequest} used to customize the request.
     * @param callback      {@link PayPalFlowStartedCallback}
     */
    @Deprecated
    public void tokenizePayPalAccount(@NonNull final FragmentActivity activity, @NonNull final PayPalRequest payPalRequest, @NonNull final PayPalFlowStartedCallback callback) {
        if (payPalRequest instanceof PayPalCheckoutRequest) {
            isVaultRequest = false;
            sendCheckoutRequest(activity, (PayPalCheckoutRequest) payPalRequest, callback);
        } else if (payPalRequest instanceof PayPalVaultRequest) {
            isVaultRequest = true;
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
    public void requestOneTimePayment(@NonNull final FragmentActivity activity, @NonNull final PayPalCheckoutRequest payPalCheckoutRequest, @NonNull final PayPalFlowStartedCallback callback) {
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
    public void requestBillingAgreement(@NonNull final FragmentActivity activity, @NonNull final PayPalVaultRequest payPalVaultRequest, @NonNull final PayPalFlowStartedCallback callback) {
        tokenizePayPalAccount(activity, payPalVaultRequest, callback);
    }

    private void sendCheckoutRequest(final FragmentActivity activity, final PayPalCheckoutRequest payPalCheckoutRequest, final PayPalFlowStartedCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.single-payment.selected", payPalContextId, null, isVaultRequest);
        if (payPalCheckoutRequest.getShouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal.single-payment.paylater.offered", payPalContextId, null, isVaultRequest);
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (error != null) {
                    callback.onResult(error);
                    return;
                }

                if (payPalConfigInvalid(configuration)) {
                    Exception configInvalidError = createPayPalError();
                    callback.onResult(configInvalidError);
                    return;
                }

                try {
                    assertCanPerformBrowserSwitch(activity);
                } catch (BrowserSwitchException browserSwitchException) {
                    braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest", payPalContextId, null, isVaultRequest);
                    Exception manifestInvalidError =
                            createBrowserSwitchError(browserSwitchException);
                    callback.onResult(manifestInvalidError);
                    return;
                }
                sendPayPalRequest(activity, payPalCheckoutRequest, callback);
            }
        });

    }

    private void sendVaultRequest(final FragmentActivity activity, final PayPalVaultRequest payPalVaultRequest, final PayPalFlowStartedCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.selected", payPalContextId, null, isVaultRequest);
        if (payPalVaultRequest.getShouldOfferCredit()) {
            braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.credit.offered", payPalContextId, null, isVaultRequest);
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (error != null) {
                    callback.onResult(error);
                    return;
                }

                if (payPalConfigInvalid(configuration)) {
                    Exception configInvalidError = createPayPalError();
                    callback.onResult(configInvalidError);
                    return;
                }

                try {
                    assertCanPerformBrowserSwitch(activity);
                } catch (BrowserSwitchException browserSwitchException) {
                    braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest", payPalContextId, null, isVaultRequest);
                    Exception manifestInvalidError =
                            createBrowserSwitchError(browserSwitchException);
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
            public void onResult(@Nullable PayPalResponse payPalResponse, @Nullable Exception error) {
                if (payPalResponse != null) {
                    String analyticsPrefix = getAnalyticsEventPrefix(payPalRequest);
                    payPalContextId = payPalResponse.getPairingId();
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.started", analyticsPrefix), payPalContextId, null, isVaultRequest);

                    try {
                        startBrowserSwitch(activity, payPalResponse, payPalRequest.isAppLinkEnabled());
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

    private void startBrowserSwitch(
            FragmentActivity activity,
            PayPalResponse payPalResponse,
            boolean isAppLinkEnabled
    ) throws JSONException, BrowserSwitchException {
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
                .launchAsNewTask(braintreeClient.launchesBrowserSwitchAsNewTask())
                .metadata(metadata);
        if (isAppLinkEnabled) {
            browserSwitchOptions.appLinkUri(braintreeClient.getAppLinkReturnUri());
        }
        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
    }

    private static String getAnalyticsEventPrefix(PayPalRequest request) {
        return request instanceof PayPalVaultRequest ? "paypal.billing-agreement" : "paypal.single-payment";
    }

    void onBrowserSwitchResult(@NonNull BrowserSwitchResult browserSwitchResult) {
        this.pendingBrowserSwitchResult = browserSwitchResult;
        if (listener != null) {
            // NEXT_MAJOR_VERSION: determine if browser switch logic can be further decoupled
            // from the client to allow more flexibility to merchants who rely heavily on view model.
            deliverBrowserSwitchResultToListener(pendingBrowserSwitchResult);
        }
    }

    private void deliverBrowserSwitchResultToListener(final BrowserSwitchResult browserSwitchResult) {
        onBrowserSwitchResult(browserSwitchResult, new PayPalBrowserSwitchResultCallback() {
            @Override
            public void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error) {
                if (payPalAccountNonce != null && listener != null) {
                    listener.onPayPalSuccess(payPalAccountNonce);
                } else if (error != null && listener != null) {
                    listener.onPayPalFailure(error);
                }
            }
        });
        this.pendingBrowserSwitchResult = null;
    }

    // NEXT_MAJOR_VERSION: duplication here could be a sign that we need to decouple browser switching
    // logic into another component that also gives merchants more flexibility when using view models
    BrowserSwitchResult getBrowserSwitchResult(FragmentActivity activity) {
        return braintreeClient.getBrowserSwitchResult(activity);
    }

    BrowserSwitchResult deliverBrowserSwitchResult(FragmentActivity activity) {
        return braintreeClient.deliverBrowserSwitchResult(activity);
    }

    BrowserSwitchResult getBrowserSwitchResultFromNewTask(FragmentActivity activity) {
        return braintreeClient.getBrowserSwitchResultFromNewTask(activity);
    }

    BrowserSwitchResult deliverBrowserSwitchResultFromNewTask(FragmentActivity activity) {
        return braintreeClient.deliverBrowserSwitchResultFromNewTask(activity);
    }

    /**
     * Use this method with the manual browser switch integration pattern.
     *
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback            {@link PayPalBrowserSwitchResultCallback}
     */
    public void onBrowserSwitchResult(@NonNull BrowserSwitchResult browserSwitchResult, @NonNull final PayPalBrowserSwitchResultCallback callback) {
        //noinspection ConstantConditions
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

        if (approvalUrl != null) {
            String pairingId = Uri.parse(approvalUrl).getQueryParameter(tokenKey);
            if (pairingId != null && !pairingId.isEmpty()) {
                payPalContextId = pairingId;
            }
        }

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                callback.onResult(null, new UserCanceledException("User canceled PayPal."));
                braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.canceled", analyticsPrefix), payPalContextId, null, isVaultRequest);
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

                        internalPayPalClient.tokenize(payPalAccount, new PayPalBrowserSwitchResultCallback() {
                            @Override
                            public void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error) {
                                if (payPalAccountNonce != null && payPalAccountNonce.getCreditFinancing() != null) {
                                    braintreeClient.sendAnalyticsEvent("paypal.credit.accepted", payPalContextId, null, isVaultRequest);
                                }
                                callback.onResult(payPalAccountNonce, error);
                            }
                        });

                        braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.succeeded", analyticsPrefix), payPalContextId, null, isVaultRequest);
                    } else {
                        callback.onResult(null, new BraintreeException("Unknown error"));
                    }
                } catch (UserCanceledException e) {
                    callback.onResult(null, e);
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.canceled", analyticsPrefix), payPalContextId, null, isVaultRequest);
                } catch (JSONException | PayPalBrowserSwitchException e) {
                    callback.onResult(null, e);
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.failed", analyticsPrefix), payPalContextId, null, isVaultRequest);
                }
                break;
        }
    }

    private JSONObject parseUrlResponseData(Uri uri, String successUrl, String approvalUrl, String tokenKey) throws JSONException, UserCanceledException, PayPalBrowserSwitchException {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(successUrl).getLastPathSegment().equals(status)) {
            throw new UserCanceledException("User canceled PayPal.", true);
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
