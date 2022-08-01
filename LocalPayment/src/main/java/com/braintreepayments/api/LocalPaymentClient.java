package com.braintreepayments.api;

import android.content.Context;
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

    @VisibleForTesting
    BrowserSwitchResult pendingBrowserSwitchResult;

    /**
     * Create a new instance of {@link LocalPaymentClient} from within an Activity using a {@link BraintreeClient}.
     *
     * @param activity a {@link FragmentActivity}
     * @param braintreeClient a {@link BraintreeClient}
     */
    public LocalPaymentClient(@NonNull FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {
        this(activity, activity.getLifecycle(), braintreeClient, new PayPalDataCollector(braintreeClient), new LocalPaymentApi(braintreeClient));
    }

    /**
     * Create a new instance of {@link LocalPaymentClient} from within a Fragment using a {@link BraintreeClient}.
     *
     * @param fragment a {@link Fragment
     * @param braintreeClient a {@link BraintreeClient}
     */
    public LocalPaymentClient(@NonNull Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(fragment.getActivity(), fragment.getLifecycle(), braintreeClient, new PayPalDataCollector(braintreeClient), new LocalPaymentApi(braintreeClient));
    }

    /**
     * Create a new instance of {@link LocalPaymentClient} using a {@link BraintreeClient}.
     *
     * Deprecated. Use {@link LocalPaymentClient(Fragment, BraintreeClient)} or
     * {@link LocalPaymentClient(FragmentActivity, BraintreeClient)}.
     *
     * @param braintreeClient a {@link BraintreeClient}
     */
    @Deprecated
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
     *
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
     *
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
                .url(Uri.parse(localPaymentResult.getApprovalUrl()));

        String paymentType = localPaymentResult.getRequest().getPaymentType();

        browserSwitchOptions.metadata(new JSONObject()
                .put("merchant-account-id", localPaymentResult.getRequest().getMerchantAccountId())
                .put("payment-type", localPaymentResult.getRequest().getPaymentType()));

        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
        sendAnalyticsEvent(paymentType, "local-payment.webswitch.initiate.succeeded");
    }

    void onBrowserSwitchResult(FragmentActivity activity) {
        this.pendingBrowserSwitchResult = braintreeClient.deliverBrowserSwitchResult(activity);

        if (pendingBrowserSwitchResult != null && listener != null) {
            deliverBrowserSwitchResultToListener(activity, pendingBrowserSwitchResult);
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

    BrowserSwitchResult getBrowserSwitchResult(FragmentActivity activity) {
        return braintreeClient.getBrowserSwitchResult(activity);
    }

    /**
     * Deprecated. Use {@link LocalPaymentListener} to handle results.
     *
     * @param context             Android Context
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback            {@link LocalPaymentBrowserSwitchResultCallback}
     */
    @Deprecated
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
                            localPaymentApi.tokenize(merchantAccountId, responseString, payPalDataCollector.getClientMetadataId(context, configuration, null), new LocalPaymentBrowserSwitchResultCallback() {
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
        braintreeClient.sendAnalyticsEvent(String.format("%s.%s", eventPrefix, eventSuffix));
    }
}
