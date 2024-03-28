package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to integrate with local payments.
 */
public class LocalPaymentClient {

    static final String LOCAL_PAYMENT_CANCEL = "local-payment-cancel";
    static final String LOCAL_PAYMENT_SUCCESS = "local-payment-success";

    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;
    private final LocalPaymentApi localPaymentApi;
    private LocalPaymentListener listener;

    /**
     * Used for linking events from the client to server side request
     * In the Local Payment flow this will be a Payment Token/Order ID
     */
    private String payPalContextId = null;

    private boolean hasUserLocationConsent;

    @VisibleForTesting
    BrowserSwitchResult pendingBrowserSwitchResult;

    /**
     * Create a new instance of {@link LocalPaymentClient} from within an Activity using a {@link BraintreeClient}.
     *
     * @param activity        a {@link FragmentActivity}
     * @param braintreeClient a {@link BraintreeClient}
     */
    public LocalPaymentClient(@NonNull FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {
        this(activity, activity.getLifecycle(), braintreeClient, new PayPalDataCollector(braintreeClient), new LocalPaymentApi(braintreeClient));
    }

    /**
     * Create a new instance of {@link LocalPaymentClient} from within a Fragment using a {@link BraintreeClient}.
     *
     * @param fragment        a {@link Fragment
     * @param braintreeClient a {@link BraintreeClient}
     */
    public LocalPaymentClient(@NonNull Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(fragment.getActivity(), fragment.getLifecycle(), braintreeClient, new PayPalDataCollector(braintreeClient), new LocalPaymentApi(braintreeClient));
    }

    /**
     * Create a new instance of {@link LocalPaymentClient} using a {@link BraintreeClient}.
     * <p>
     * Use this constructor with the manual browser switch integration pattern.
     *
     * @param braintreeClient a {@link BraintreeClient}
     */
    public LocalPaymentClient(@NonNull BraintreeClient braintreeClient) {
        this(null, null, braintreeClient, new PayPalDataCollector(braintreeClient), new LocalPaymentApi(braintreeClient));
    }

    @VisibleForTesting
    LocalPaymentClient(FragmentActivity activity, Lifecycle lifecycle, @NonNull BraintreeClient braintreeClient, @NonNull PayPalDataCollector payPalDataCollector, @NonNull LocalPaymentApi localPaymentApi) {
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;
        this.localPaymentApi = localPaymentApi;
        if (activity != null && lifecycle != null) {
            LocalPaymentLifecycleObserver observer = new LocalPaymentLifecycleObserver(this);
            lifecycle.addObserver(observer);
        }
    }

    /**
     * Add a {@link LocalPaymentListener} to your client to receive results or errors from the Local Payment flow.
     * This method must be invoked on a {@link LocalPaymentClient(Fragment, BraintreeClient)} or
     * {@link LocalPaymentClient(FragmentActivity, BraintreeClient)} in order to receive results.
     *
     * @param listener a {@link LocalPaymentListener}
     */
    public void setListener(LocalPaymentListener listener) {
        this.listener = listener;
        if (pendingBrowserSwitchResult != null) {
            deliverBrowserSwitchResultToListener(braintreeClient.getApplicationContext(), pendingBrowserSwitchResult);
        }
    }

    /**
     * Prepares the payment flow for a specific type of local payment.
     *
     * @param request  {@link LocalPaymentRequest} with the payment details.
     * @param callback {@link LocalPaymentStartCallback}
     */
    public void startPayment(@NonNull final LocalPaymentRequest request, @NonNull final LocalPaymentStartCallback callback) {
        Exception exception = null;

        //noinspection ConstantConditions
        if (callback == null) {
            throw new RuntimeException("A LocalPaymentCallback is required.");
        }

        //noinspection ConstantConditions
        if (request == null) {
            exception = new BraintreeException("A LocalPaymentRequest is required.");
        } else if (request.getPaymentType() == null || request.getAmount() == null) {
            exception = new BraintreeException(
                    "LocalPaymentRequest is invalid, paymentType and amount are required.");
        }

        if (exception != null) {
            callback.onResult(null, exception);
        } else {
            braintreeClient.getConfiguration(new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                    if (configuration != null) {
                        if (!configuration.isPayPalEnabled()) {
                            callback.onResult(null, new ConfigurationException("Local payments are not enabled for this merchant."));
                            return;
                        }

                        sendAnalyticsEvent(request.getPaymentType(), "local-payment.start-payment.selected");

                        localPaymentApi.createPaymentMethod(request, new LocalPaymentStartCallback() {
                            @Override
                            public void onResult(@Nullable LocalPaymentResult localPaymentResult, @Nullable Exception error) {
                                if (localPaymentResult != null) {
                                    String pairingId = localPaymentResult.getPaymentId();
                                    if (pairingId != null && !pairingId.isEmpty()) {
                                        payPalContextId = pairingId;
                                    }
                                    hasUserLocationConsent = request.hasUserLocationConsent();
                                    sendAnalyticsEvent(request.getPaymentType(), "local-payment.create.succeeded");
                                } else if (error != null) {
                                    sendAnalyticsEvent(request.getPaymentType(), "local-payment.webswitch.initiate.failed");
                                }
                                callback.onResult(localPaymentResult, error);
                            }
                        });
                    } else {
                        callback.onResult(null, error);
                    }
                }
            });
        }
    }

    /**
     * Initiates the browser switch for a payment flow by opening a browser where the customer can authenticate with their bank.
     * <p>
     * Errors encountered during the approval will be returned to the {@link LocalPaymentListener}.
     *
     * @param activity           Android FragmentActivity
     * @param localPaymentResult {@link LocalPaymentRequest} which has already been sent to {@link #startPayment(LocalPaymentRequest, LocalPaymentStartCallback)}
     *                           and now has an approvalUrl and paymentId.
     */
    public void approveLocalPayment(@NonNull FragmentActivity activity, @NonNull LocalPaymentResult localPaymentResult) {
        try {
            approvePayment(activity, localPaymentResult);
        } catch (Exception error) {
            listener.onLocalPaymentFailure(error);
        }
    }

    /**
     * Initiates the browser switch for a payment flow by opening a browser where the customer can authenticate with their bank.
     * <p>
     * Deprecated. Use {@link LocalPaymentClient#approveLocalPayment(FragmentActivity, LocalPaymentResult)}.
     *
     * @param activity           Android FragmentActivity
     * @param localPaymentResult {@link LocalPaymentRequest} which has already been sent to {@link #startPayment(LocalPaymentRequest, LocalPaymentStartCallback)}
     *                           and now has an approvalUrl and paymentId.
     */
    @Deprecated
    public void approvePayment(@NonNull FragmentActivity activity, @NonNull LocalPaymentResult localPaymentResult) throws JSONException, BrowserSwitchException {
        //noinspection ConstantConditions
        if (activity == null) {
            throw new RuntimeException("A FragmentActivity is required.");
        }

        //noinspection ConstantConditions
        if (localPaymentResult == null) {
            throw new RuntimeException("A LocalPaymentTransaction is required.");
        }

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.LOCAL_PAYMENT)
                .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                .launchAsNewTask(braintreeClient.launchesBrowserSwitchAsNewTask())
                .url(Uri.parse(localPaymentResult.getApprovalUrl()));

        String paymentType = localPaymentResult.getRequest().getPaymentType();

        browserSwitchOptions.metadata(new JSONObject()
                .put("merchant-account-id", localPaymentResult.getRequest().getMerchantAccountId())
                .put("payment-type", localPaymentResult.getRequest().getPaymentType()));

        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
        sendAnalyticsEvent(paymentType, "local-payment.webswitch.initiate.succeeded");
    }

    void onBrowserSwitchResult(@NonNull Context context, @NonNull BrowserSwitchResult browserSwitchResult) {
        this.pendingBrowserSwitchResult = browserSwitchResult;
        if (listener != null) {
            // NEXT_MAJOR_VERSION: determine if browser switch logic can be further decoupled
            // from the client to allow more flexibility to merchants who rely heavily on view model.
            deliverBrowserSwitchResultToListener(context, pendingBrowserSwitchResult);
        }
    }

    private void deliverBrowserSwitchResultToListener(Context context, final BrowserSwitchResult browserSwitchResult) {
        onBrowserSwitchResult(context, browserSwitchResult, new LocalPaymentBrowserSwitchResultCallback() {
            @Override
            public void onResult(@Nullable LocalPaymentNonce localPaymentNonce, @Nullable Exception error) {
                if (localPaymentNonce != null) {
                    listener.onLocalPaymentSuccess(localPaymentNonce);
                } else if (error != null) {
                    listener.onLocalPaymentFailure(error);
                }
            }
        });

        this.pendingBrowserSwitchResult = null;
    }

    /**
     * After calling {@link LocalPaymentClient#startPayment(LocalPaymentRequest, LocalPaymentStartCallback)},
     * call this method in your Activity or Fragment's onResume() method to see if a response
     * was provided through deep linking.
     * <p>
     * If a BrowserSwitchResult exists, call {@link LocalPaymentClient#onBrowserSwitchResult(Context, BrowserSwitchResult, LocalPaymentBrowserSwitchResultCallback)},
     * to allow the SDK to continue tokenization of the PayPalAccount.
     * <p>
     * Make sure to call {@link LocalPaymentClient#clearActiveBrowserSwitchRequests(Context)} after
     * successfully parsing a BrowserSwitchResult to guard against multiple invocations of browser
     * switch event handling.
     *
     * @param context The context used to check for pending browser switch requests
     * @param intent  The intent containing a potential deep link response. May be null.
     * @return {@link BrowserSwitchResult} when a result has been parsed successfully from a deep link; null when an input Intent is null
     */
    @Nullable
    public BrowserSwitchResult parseBrowserSwitchResult(@NonNull Context context, @Nullable Intent intent) {
        int requestCode = BraintreeRequestCodes.LOCAL_PAYMENT;
        return braintreeClient.parseBrowserSwitchResult(context, requestCode, intent);
    }

    /**
     * Make sure to call this method after {@link LocalPaymentClient#parseBrowserSwitchResult(Context, Intent)}
     * parses a {@link BrowserSwitchResult} successfully to prevent multiple invocations of browser
     * switch event handling logic.
     *
     * @param context The context used to clear pending browser switch requests
     */
    public void clearActiveBrowserSwitchRequests(@NonNull Context context) {
        braintreeClient.clearActiveBrowserSwitchRequests(context);
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
     * @param context             Android Context
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback            {@link LocalPaymentBrowserSwitchResultCallback}
     */
    public void onBrowserSwitchResult(@NonNull final Context context, @NonNull BrowserSwitchResult browserSwitchResult, @NonNull final LocalPaymentBrowserSwitchResultCallback callback) {
        //noinspection ConstantConditions
        if (browserSwitchResult == null) {
            callback.onResult(null, new BraintreeException("BrowserSwitchResult cannot be null"));
            return;
        }
        JSONObject metadata = browserSwitchResult.getRequestMetadata();

        final String paymentType = Json.optString(metadata, "payment-type", null);
        final String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                sendAnalyticsEvent(paymentType, "local-payment.webswitch.canceled");
                callback.onResult(null, new UserCanceledException("User canceled Local Payment."));
                return;
            case BrowserSwitchStatus.SUCCESS:
                Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUri == null) {
                    sendAnalyticsEvent(paymentType, "local-payment.webswitch-response.invalid");
                    callback.onResult(null, new BraintreeException("LocalPayment encountered an error, " +
                            "return URL is invalid."));
                    return;
                }

                final String responseString = deepLinkUri.toString();
                if (responseString.toLowerCase().contains(LOCAL_PAYMENT_CANCEL.toLowerCase())) {
                    sendAnalyticsEvent(paymentType, "local-payment.webswitch.canceled");
                    callback.onResult(null, new UserCanceledException("User canceled Local Payment."));
                    return;
                }
                braintreeClient.getConfiguration(new ConfigurationCallback() {
                    @Override
                    public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                        if (configuration != null) {
                            localPaymentApi.tokenize(merchantAccountId, responseString, payPalDataCollector.getClientMetadataId(context, configuration, hasUserLocationConsent),
                                new LocalPaymentBrowserSwitchResultCallback() {
                                    @Override
                                    public void onResult(@Nullable LocalPaymentNonce localPaymentNonce, @Nullable Exception error) {
                                        if (localPaymentNonce != null) {
                                            sendAnalyticsEvent(paymentType, "local-payment.tokenize.succeeded");
                                        } else if (error != null) {
                                            sendAnalyticsEvent(paymentType, "local-payment.tokenize.failed");
                                        }
                                        callback.onResult(localPaymentNonce, error);
                                    }
                                });
                        } else if (error != null) {
                            callback.onResult(null, error);
                        }
                    }
                });
        }
    }

    private void sendAnalyticsEvent(String paymentType, String eventSuffix) {
        String eventPrefix = (paymentType == null) ? "unknown" : paymentType;
        braintreeClient.sendAnalyticsEvent(String.format("%s.%s", eventPrefix, eventSuffix), payPalContextId);
    }
}
