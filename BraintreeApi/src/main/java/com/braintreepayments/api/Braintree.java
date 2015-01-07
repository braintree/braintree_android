package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.braintreepayments.api.data.BraintreeEnvironment;
import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Braintree {

    protected static final Map<String, Braintree> sInstances = new HashMap<String, Braintree>();
    protected static final String INTEGRATION_DROPIN = "dropin";

    /**
     * Base interface for all event listeners. Only concrete classes that implement this interface
     * (either directly or indirectly) can be registered with
     * {@link #addListener(com.braintreepayments.api.Braintree.Listener)}.
     */
    private static interface Listener {}

    /**
     * onPaymentMethodsUpdate will be called with a list of {@link com.braintreepayments.api.models.PaymentMethod}s
     * as a callback when {@link Braintree#getPaymentMethods()} is called
     */
    public static interface PaymentMethodsUpdatedListener extends Listener {
        void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods);
    }

    /**
     * onPaymentMethodCreated will be called with a {@link com.braintreepayments.api.models.PaymentMethod}
     * as a callback when
     * {@link Braintree#create(com.braintreepayments.api.models.PaymentMethod.Builder)}
     * is called
     */
    public static interface PaymentMethodCreatedListener extends Listener {
        void onPaymentMethodCreated(PaymentMethod paymentMethod);
    }

    /**
     * onPaymentMethodNonce will be called as a callback with a nonce when
     * {@link Braintree#create(com.braintreepayments.api.models.PaymentMethod.Builder)}
     * or {@link Braintree#tokenize(com.braintreepayments.api.models.PaymentMethod.Builder)}
     * is called
     */
    public static interface PaymentMethodNonceListener extends Listener {
        void onPaymentMethodNonce(String paymentMethodNonce);
    }

    /**
     * onUnrecoverableError will be called where there is an exception that cannot be handled.
     * onRecoverableError will be called on data validation errors
     */
    public static interface ErrorListener extends Listener {
        void onUnrecoverableError(Throwable throwable);
        void onRecoverableError(ErrorWithResponse error);
    }

    private final ExecutorService mExecutorService;
    private final BraintreeApi mBraintreeApi;
    private String mIntegrationType;

    /**
     * {@link Handler} to deliver events to listeners; events are always delivered on the main thread.
     */
    private final Handler mListenerHandler = new Handler(Looper.getMainLooper());

    private final List<ListenerCallback> mCallbackQueue = Utils.newLinkedList();
    private boolean mListenersLocked = false;

    private final Set<PaymentMethodsUpdatedListener> mUpdatedListeners = Utils.newHashSet();
    private final Set<PaymentMethodCreatedListener> mCreatedListeners = Utils.newHashSet();
    private final Set<PaymentMethodNonceListener> mNonceListeners = Utils.newHashSet();
    private final Set<ErrorListener> mErrorListeners = Utils.newHashSet();

    private List<PaymentMethod> mCachedPaymentMethods;

    /**
     * Obtain an instance of {@link Braintree}. If multiple calls are made with the same {@code
     * clientToken}, you may get the same instance returned.
     */
    public static Braintree getInstance(Context context, String clientToken) {
        if (sInstances.containsKey(clientToken)) {
            return sInstances.get(clientToken);
        } else {
            return new Braintree(clientToken,
                    new BraintreeApi(context.getApplicationContext(), clientToken));
        }
    }

    protected Braintree(String clientToken, BraintreeApi braintreeApi) {
        mBraintreeApi = braintreeApi;
        mExecutorService = Executors.newSingleThreadExecutor();
        mIntegrationType = "custom";
        sInstances.put(clientToken, this);
    }

    protected String analyticsPrefix() {
        return mIntegrationType + ".android";
    }

    protected String getIntegrationType() {
        return mIntegrationType;
    }

    /**
     * Sets integration method to Drop-In.
     * Used internally, not necessary to call directly.
     */
    public void setIntegrationDropin() {
        mIntegrationType = INTEGRATION_DROPIN;
    }

    /**
     * Resets saved state used to persist across {@link Activity} lifecycle.
     * In the normal course of operation this method is not necessary, but is useful for
     * test suites.
     */
    public static void reset() {
        sInstances.clear();
    }

    /**
     * Checks if PayPal is enabled and supported.
     * @return {@code true} if PayPal is enabled and supported, {@code false} otherwise.
     */
    public boolean isPayPalEnabled() {
        return mBraintreeApi.isPayPalEnabled();
    }

    /**
     * @return If Venmo app switch is supported and enabled in the current environment
     */
    public boolean isVenmoEnabled() {
        return mBraintreeApi.isVenmoEnabled();
    }

    /**
     * Checks if cvv is required when add a new card
     * @return {@code true} if cvv is required to add a new card, {@code false} otherwise.
     */
    public boolean isCvvChallenegePresent() {
        return mBraintreeApi.isCvvChallengePresent();
    }

    /**
     * Checks if postal code is required to add a new card
     * @return {@code true} if postal code is required to add a new card {@code false} otherwise.
     */
    public boolean isPostalCodeChallengePresent() {
        return mBraintreeApi.isPostalCodeChallengePresent();
    }

    /**
     * Starts the Pay With PayPal flow. This will launch a new activity for the PayPal mobile SDK.
     * @param activity the {@link android.app.Activity} to receive the {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *   when payWithPayPal finishes.
     * @param requestCode the request code associated with this start request. Will be returned
     * in {@code onActivityResult}.
     */
    public void startPayWithPayPal(Activity activity, int requestCode) {
        sendAnalyticsEvent("add-paypal.start");
        mBraintreeApi.startPayWithPayPal(activity, requestCode);
    }

    /**
     * Start the Pay With Venmo flow. This will app switch to the Venmo app.
     * @param activity The {@link android.app.Activity} to receive {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * when {@link #startPayWithVenmo(android.app.Activity, int)} finishes.
     * @param requestCode The request code associated with this start request. Will be returned in
     * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     */
    public void startPayWithVenmo(Activity activity, int requestCode) {
        try {
            mBraintreeApi.startPayWithVenmo(activity, requestCode);
            sendAnalyticsEvent("add-venmo.start");
        } catch (AppSwitchNotAvailableException e) {
            sendAnalyticsEvent("add-venmo.unavailable");
            postUnrecoverableErrorToListeners(e);
        }
    }

    /**
     * Adds a listener. Listeners must be removed when they are no longer necessary (such as in
     * {@link android.app.Activity#onDestroy()}) to avoid memory leaks.
     *
     * @param listener the listener to add.
     */
    public synchronized <T extends Listener> void addListener(final T listener) {
        if (listener instanceof PaymentMethodsUpdatedListener) {
            mUpdatedListeners.add((PaymentMethodsUpdatedListener) listener);
        }

        if (listener instanceof PaymentMethodCreatedListener) {
            mCreatedListeners.add((PaymentMethodCreatedListener) listener);
        }

        if (listener instanceof PaymentMethodNonceListener) {
            mNonceListeners.add((PaymentMethodNonceListener) listener);
        }

        if (listener instanceof ErrorListener) {
            mErrorListeners.add((ErrorListener) listener);
        }
    }

    /**
     * Removes a previously added listener.
     *
     * @param listener the listener to remove.
     */
    public synchronized <T extends Listener> void removeListener(T listener) {
        if (listener instanceof PaymentMethodsUpdatedListener) {
            mUpdatedListeners.remove(listener);
        }

        if (listener instanceof PaymentMethodCreatedListener) {
            mCreatedListeners.remove(listener);
        }

        if (listener instanceof PaymentMethodNonceListener) {
            mNonceListeners.remove(listener);
        }

        if (listener instanceof ErrorListener) {
            mErrorListeners.remove(listener);
        }
    }

    /**
     * Retrieves the current list of {@link com.braintreepayments.api.models.PaymentMethod} for this device and client token.
     *
     * When finished, the {@link java.util.List} of {@link com.braintreepayments.api.models.PaymentMethod}s
     * will be sent to {@link Braintree.PaymentMethodsUpdatedListener#onPaymentMethodsUpdated(java.util.List)}.
     *
     * If a network or server error occurs, {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)}
     * will be called with the {@link com.braintreepayments.api.exceptions.BraintreeException} that occurred.
     */
    public synchronized void getPaymentMethods() {
        getPaymentMethodsHelper();
    }

    /**
     * Helper method to {@link #getPaymentMethods()} to make execution synchronous in testing.
     */
    protected synchronized Future<?> getPaymentMethodsHelper() {
        return mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    List<PaymentMethod> paymentMethods = mBraintreeApi.getPaymentMethods();
                    mCachedPaymentMethods = paymentMethods;
                    postPaymentMethodsToListeners(paymentMethods);
                } catch (BraintreeException e) {
                    postUnrecoverableErrorToListeners(e);
                } catch (ErrorWithResponse e) {
                    postRecoverableErrorToListeners(e);
                }
            }
        });
    }

    /**
     * Handles response from PayPal and returns a PayPalAccountBuilder which must be then passed to
     * {@link #create(com.braintreepayments.api.models.PaymentMethod.Builder)}. {@link #finishPayWithPayPal(android.app.Activity, int, android.content.Intent)}
     * will call this and {@link #create(com.braintreepayments.api.models.PaymentMethod.Builder)} for you
     * and may be a better option.
     *
     * Sends a {@link com.braintreepayments.api.exceptions.ConfigurationException} to
     * {@link com.braintreepayments.api.Braintree.ErrorListener#onUnrecoverableError(Throwable)}
     * if PayPal credentials from the Braintree control panel are incorrect.
     *
     * @param activity The activity that received the result.
     * @param resultCode The result code provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param data The {@link android.content.Intent} provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder} ready to be sent to
     * {@link #create(com.braintreepayments.api.models.PaymentMethod.Builder)} or null if a
     * {@link com.braintreepayments.api.models.PayPalAccountBuilder} could not be created
     */
    public PayPalAccountBuilder handlePayPalResponse(Activity activity, int resultCode, Intent data) {
        try {
            return mBraintreeApi.handlePayPalResponse(activity, resultCode, data);
        } catch (ConfigurationException e) {
            postUnrecoverableErrorToListeners(e);
        }

        return null;
    }

    /**
     * @deprecated
     *
     * This method should *not* be used, it does not include a Application Correlation ID.
     * PayPal uses the Application Correlation ID to verify that the payment is originating from
     * a valid, user-consented device+application. This helps reduce fraud and decrease declines.
     * PayPal does not provide any loss protection for transactions that do not correctly supply
     * an Application Correlation ID.
     *
     * Method to finish Pay With PayPal flow. Create a {@link com.braintreepayments.api.models.PayPalAccount}.
     *
     * The {@link com.braintreepayments.api.models.PayPalAccount} will be sent to
     * {@link Braintree.PaymentMethodCreatedListener#onPaymentMethodCreated(com.braintreepayments.api.models.PaymentMethod)}
     * and the nonce will be sent to
     * {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * If an error occurs, the exception that occurred will be sent to
     * {@link Braintree.ErrorListener#onRecoverableError(com.braintreepayments.api.exceptions.ErrorWithResponse)} or
     * {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)} as appropriate.
     *
     * @param resultCode Result code from the Pay With PayPal flow.
     * @param data Intent returned from Pay With PayPal flow.
     */
    public synchronized void finishPayWithPayPal(int resultCode, Intent data) {
        try {
            PayPalAccountBuilder payPalAccountBuilder = mBraintreeApi.handlePayPalResponse(null, resultCode, data);
            if (payPalAccountBuilder != null) {
                create(payPalAccountBuilder);
            }
        } catch (ConfigurationException e) {
            postUnrecoverableErrorToListeners(e);
        }
    }

    /**
     * Method to finish Pay With PayPal flow. Create a {@link com.braintreepayments.api.models.PayPalAccount}.
     *
     * The {@link com.braintreepayments.api.models.PayPalAccount} will be sent to
     * {@link Braintree.PaymentMethodCreatedListener#onPaymentMethodCreated(com.braintreepayments.api.models.PaymentMethod)}
     * and the nonce will be sent to
     * {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * If an error occurs, the exception that occurred will be sent to
     * {@link Braintree.ErrorListener#onRecoverableError(com.braintreepayments.api.exceptions.ErrorWithResponse)} or
     * {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)} as appropriate.
     *
     * @param resultCode Result code from the Pay With PayPal flow.
     * @param data Intent returned from Pay With PayPal flow.
     */
    public synchronized void finishPayWithPayPal(Activity activity, int resultCode, Intent data) {
        try {
            PayPalAccountBuilder payPalAccountBuilder = mBraintreeApi.handlePayPalResponse(activity,
                    resultCode, data);
            if (payPalAccountBuilder != null) {
                create(payPalAccountBuilder);
            }
        } catch (ConfigurationException e) {
            postUnrecoverableErrorToListeners(e);
        }
    }

    /**
     * Method to finish Pay With Venmo flow. Create a {@link com.braintreepayments.api.models.PaymentMethod}.
     *
     * The {@link com.braintreepayments.api.models.PaymentMethod} will be sent to
     * {@link Braintree.PaymentMethodCreatedListener#onPaymentMethodCreated(com.braintreepayments.api.models.PaymentMethod)}
     * and the nonce will be sent to
     * {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * If an error occurs, the exception that occurred will be sent to
     * {@link Braintree.ErrorListener#onRecoverableError(com.braintreepayments.api.exceptions.ErrorWithResponse)} or
     * {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)} as appropriate.
     *
     * @param resultCode Result code from the Pay With Venmo flow.
     * @param data Intent returned from Pay With Venmo flow.
     */
    public synchronized void finishPayWithVenmo(int resultCode, Intent data) {
        final String nonce = mBraintreeApi.finishPayWithVenmo(resultCode, data);
        if (!TextUtils.isEmpty(nonce)) {
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        PaymentMethod paymentMethod = mBraintreeApi.getPaymentMethod(nonce);
                        paymentMethod.setSource(VenmoAppSwitch.VENMO_SOURCE);
                        addPaymentMethodToCache(paymentMethod);
                        postCreatedMethodToListeners(paymentMethod);
                        postCreatedNonceToListeners(nonce);
                        sendAnalyticsEvent("venmo-app.success");
                    } catch (BraintreeException e) {
                        postUnrecoverableErrorToListeners(e);
                    } catch (JSONException e) {
                        postUnrecoverableErrorToListeners(e);
                    } catch (ErrorWithResponse errorWithResponse) {
                        postRecoverableErrorToListeners(errorWithResponse);
                    }
                }
            });
        } else {
            sendAnalyticsEvent("venmo-app.fail");
        }
    }

    /**
     * Create a {@link com.braintreepayments.api.models.PaymentMethod} in the Braintree Gateway.
     *
     * On completion, returns the {@link com.braintreepayments.api.models.PaymentMethod} to
     * {@link Braintree.PaymentMethodCreatedListener#onPaymentMethodCreated(com.braintreepayments.api.models.PaymentMethod)} and nonce to
     * {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * If creation fails validation, {@link Braintree.ErrorListener#onRecoverableError(com.braintreepayments.api.exceptions.ErrorWithResponse)}
     * will be called with the resulting {@link com.braintreepayments.api.exceptions.ErrorWithResponse}.
     *
     * If an error not due to validation (server error, network issue, etc.) occurs,
     * {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)} will be called
     * with the {@link com.braintreepayments.api.exceptions.BraintreeException} that occurred.
     *
     * @param paymentMethodBuilder {@link com.braintreepayments.api.models.PaymentMethod.Builder} for the
     * {@link com.braintreepayments.api.models.PaymentMethod} to be created.
     * @param <T> {@link com.braintreepayments.api.models.PaymentMethod} or a subclass.
     * @see #tokenize(com.braintreepayments.api.models.PaymentMethod.Builder)
     */
    public synchronized <T extends PaymentMethod> void create(
            PaymentMethod.Builder<T> paymentMethodBuilder) {
        createHelper(paymentMethodBuilder);
    }

    /**
     * Helper method to {@link #create(PaymentMethod.Builder)} to make execution synchronous in
     * testing.
     */
    protected synchronized <T extends PaymentMethod> Future<?> createHelper(
            final PaymentMethod.Builder<T> paymentMethodBuilder) {
        return mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    PaymentMethod createdPaymentMethod = mBraintreeApi.create(paymentMethodBuilder);
                    addPaymentMethodToCache(createdPaymentMethod);

                    postCreatedMethodToListeners(createdPaymentMethod);
                    postCreatedNonceToListeners(createdPaymentMethod.getNonce());
                } catch (BraintreeException e) {
                    postUnrecoverableErrorToListeners(e);
                } catch (ErrorWithResponse e) {
                    postRecoverableErrorToListeners(e);
                }
            }
        });
    }

    /**
     * Tokenizes a {@link com.braintreepayments.api.models.PaymentMethod} and returns a nonce in
     * {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * Tokenization functions like creating a {@link com.braintreepayments.api.models.PaymentMethod}, but
     * defers validation until a server library attempts to use the {@link com.braintreepayments.api.models.PaymentMethod}.
     * Use {@link #tokenize(com.braintreepayments.api.models.PaymentMethod.Builder)} to handle validation errors
     * on the server instead of on device.
     *
     * If a network or server error occurs, {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)}
     * will be called with the {@link com.braintreepayments.api.exceptions.BraintreeException} that occurred.
     *
     * @param paymentMethodBuilder {@link com.braintreepayments.api.models.PaymentMethod.Builder} for the
     * {@link com.braintreepayments.api.models.PaymentMethod} to be created.
     * @see #create(com.braintreepayments.api.models.PaymentMethod.Builder)
     */
    public synchronized <T extends PaymentMethod> void tokenize(
            PaymentMethod.Builder<T> paymentMethodBuilder) {
        tokenizeHelper(paymentMethodBuilder);
    }

    /**
     * Helper method to {@link #tokenize(PaymentMethod.Builder)} to make execution synchronous in
     * testing.
     */
    protected synchronized <T extends PaymentMethod> Future<?> tokenizeHelper(
            final PaymentMethod.Builder<T> paymentMethodBuilder) {
        return mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String nonce = mBraintreeApi.tokenize(paymentMethodBuilder);
                    postCreatedNonceToListeners(nonce);
                } catch (BraintreeException e) {
                    postUnrecoverableErrorToListeners(e);
                } catch (ErrorWithResponse e) {
                    postRecoverableErrorToListeners(e);
                }
            }
        });
    }

    /**
     * @deprecated Use {@link #sendAnalyticsEvent(String)} instead.
     * Sends analytics event to send to the Braintree analytics service. Used internally and by Drop-In.
     * @param event Name of event to be sent.
     * @param integrationType The type of integration used. Should be "custom" for those directly
     * using {@link Braintree} or {@link BraintreeApi} without Drop-In
     */
    public synchronized void sendAnalyticsEvent(String event, String integrationType) {
        sendAnalyticsEventHelper(event, integrationType);
    }

    public synchronized void sendAnalyticsEvent(String eventFragment) {
        sendAnalyticsEventHelper(analyticsPrefix() + "." + eventFragment, getIntegrationType());
    }

    /**
     * Helper method to {@link #sendAnalyticsEvent(String, String)} to make execution synchronous in testing.
     */
    protected synchronized Future<?> sendAnalyticsEventHelper(final String event, final String integrationType) {
        return mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                mBraintreeApi.sendAnalyticsEvent(event, integrationType);
            }
        });
    }

    /**
     * Collect device information for fraud identification purposes.
     *
     * @param activity The currently visible activity.
     * @param environment The Braintree environment to use.
     * @return device_id String to send to Braintree.
     * @see com.braintreepayments.api.data.BraintreeData
     */
    public String collectDeviceData(Activity activity, BraintreeEnvironment environment) {
        return mBraintreeApi.collectDeviceData(activity, environment);
    }

    /**
     * Collect device information for fraud identification purposes. This should be used in conjunction
     * with a non-aggregate fraud id.
     *
     * @param activity The currently visible activity.
     * @param merchantId The fraud merchant id from Braintree.
     * @param collectorUrl The fraud collector url from Braintree.
     * @return device_id String to send to Braintree.
     * @see com.braintreepayments.api.data.BraintreeData
     */
    public String collectDeviceData(Activity activity, String merchantId, String collectorUrl) {
        return mBraintreeApi.collectDeviceData(activity, merchantId, collectorUrl);
    }

    private void addPaymentMethodToCache(PaymentMethod paymentMethod) {
        if (mCachedPaymentMethods == null) {
            mCachedPaymentMethods = new ArrayList<PaymentMethod>();
        }
        mCachedPaymentMethods.add(0, paymentMethod);
    }

    private synchronized void postPaymentMethodsToListeners(List<PaymentMethod> paymentMethods) {
        final List<PaymentMethod> paymentMethodsSafe = Collections.unmodifiableList(paymentMethods);
        postOrQueueCallback(new ListenerCallback() {
            @Override
            public void execute() {
                for (final PaymentMethodsUpdatedListener listener : mUpdatedListeners) {
                    mListenerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onPaymentMethodsUpdated(paymentMethodsSafe);
                        }
                    });
                }
            }

            @Override
            public boolean hasListeners() {
                return !mUpdatedListeners.isEmpty();
            }
        });
    }

    private synchronized void postCreatedMethodToListeners(final PaymentMethod paymentMethod) {
        postOrQueueCallback(new ListenerCallback() {
            @Override
            public void execute() {
                for (final PaymentMethodCreatedListener listener : mCreatedListeners) {
                    mListenerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onPaymentMethodCreated(paymentMethod);
                        }
                    });
                }
            }

            @Override
            public boolean hasListeners() {
                return !mCreatedListeners.isEmpty();
            }
        });
    }

    private synchronized void postCreatedNonceToListeners(final String nonce) {
        postOrQueueCallback(new ListenerCallback() {
            @Override
            public void execute() {
                for (final PaymentMethodNonceListener listener : mNonceListeners) {
                    mListenerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onPaymentMethodNonce(nonce);
                        }
                    });
                }
            }

            @Override
            public boolean hasListeners() {
                return !mNonceListeners.isEmpty();
            }
        });
    }

    protected synchronized void postUnrecoverableErrorToListeners(final Throwable throwable) {
        postOrQueueCallback(new ListenerCallback() {
            @Override
            public void execute() {
                for (final ErrorListener listener : mErrorListeners) {
                    mListenerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onUnrecoverableError(throwable);
                        }
                    });
                }
            }

            @Override
            public boolean hasListeners() {
                return !mErrorListeners.isEmpty();
            }
        });
    }

    private synchronized void postRecoverableErrorToListeners(final ErrorWithResponse error) {
        postOrQueueCallback(new ListenerCallback() {
            @Override
            public void execute() {
                for (final ErrorListener listener : mErrorListeners) {
                    mListenerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onRecoverableError(error);
                        }
                    });
                }
            }

            @Override
            public boolean hasListeners() {
                return !mErrorListeners.isEmpty();
            }
        });
    }

    protected void postOrQueueCallback(ListenerCallback callback) {
        if (mListenersLocked || !callback.hasListeners()) {
            mCallbackQueue.add(callback);
        } else {
            callback.execute();
        }
    }

    /**
     * Returns whether or not this client has any cached cards. This is <strong>not</strong> the
     * same as {@code getCachedPaymentMethods() == 0}. If this instance has never attempted to
     * retrieve the payment methods, this will return {@code false}
     */
    public synchronized boolean hasCachedCards() {
        return mCachedPaymentMethods != null;
    }

    /**
     * @return Unmodifiable list of previously retrieved {@link com.braintreepayments.api.models.PaymentMethod}.
     * If no attempts have been made, an empty list is returned.
     */
    public synchronized List<PaymentMethod> getCachedPaymentMethods() {
        if (mCachedPaymentMethods == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(mCachedPaymentMethods);
    }

    /**
     * There may be an instances where it is preferable to delay all events posted to
     * listeners, such as when an {@link android.app.Activity} is recreating itself due to a
     * configuration change. To avoid the event being posted in between when the first activity
     * is destroyed and the second activity registering itself as a listener,
     * call {@link #lockListeners()} during {@link android.app.Activity#onSaveInstanceState(android.os.Bundle)}
     * and {@link #unlockListeners()} in {@link android.app.Activity#onCreate(android.os.Bundle)}
     * (or wherever you add a listener).
     * @see #unlockListeners()
     */
    public synchronized void lockListeners() {
        mListenersLocked = true;
    }

    /**
     * Restore control flow to locked listeners. If the listeners have not been locked yet,
     * this acts as a noop.
     * @see #lockListeners()
     */
    public synchronized void unlockListeners() {
        mListenersLocked = false;
        List<ListenerCallback> callbackQueue = new ArrayList<ListenerCallback>();
        callbackQueue.addAll(mCallbackQueue);
        for (ListenerCallback callback : callbackQueue) {
            if (callback.hasListeners()) {
                callback.execute();
                mCallbackQueue.remove(callback);
            }
        }
    }

    protected static interface ListenerCallback {
        void execute();
        boolean hasListeners();
    }

}
