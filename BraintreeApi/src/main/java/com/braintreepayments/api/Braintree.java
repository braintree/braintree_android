package com.braintreepayments.api;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.braintreepayments.api.annotations.Beta;
import com.braintreepayments.api.data.BraintreeEnvironment;
import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.models.AndroidPayCard;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PayPalPaymentResource;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.threedsecure.ThreeDSecureWebViewActivity;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.WalletConstants;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchActivity;
import com.paypal.android.sdk.onetouch.core.PerformRequestStatus;
import com.paypal.android.sdk.onetouch.core.RequestTarget;
import com.paypal.android.sdk.onetouch.core.Result;
import com.paypal.android.sdk.onetouch.core.ResultType;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Braintree {

    private static final String KEY_CLIENT_TOKEN = "com.braintreepayments.api.KEY_CLIENT_TOKEN";
    private static final String KEY_CONFIGURATION =  "com.braintreepayments.api.KEY_CONFIGURATION";

    protected static final Map<String, Braintree> sInstances = new HashMap<String, Braintree>();
    protected static final String INTEGRATION_DROPIN = "dropin";

    private BroadcastReceiver mBraintreeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int browserResultCode = intent.getIntExtra(BraintreeBrowserSwitchActivity.EXTRA_RESULT_CODE, Activity.RESULT_OK);
            Intent browserIntent = intent.getParcelableExtra(BraintreeBrowserSwitchActivity.EXTRA_INTENT);
            finishPayWithPayPal(mCurrentPayPalActivity, browserResultCode, browserIntent);
        }
    };
    private Activity mCurrentPayPalActivity;

    /**
     * Base interface for all event listeners. Only concrete classes that implement this interface
     * (either directly or indirectly) can be registered with
     * {@link #addListener(com.braintreepayments.api.Braintree.Listener)}.
     */
    private static interface Listener {}

    /**
     * Interface that defines the response for
     * {@link com.braintreepayments.api.Braintree#setup(android.content.Context, String, com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener)}
     */
    public static interface BraintreeSetupFinishedListener {
        /**
         * @param setupSuccessful {@code true} if setup was successful, {@code false} otherwise.
         * @param braintree the {@link com.braintreepayments.api.Braintree} instance or {@code null}
         *        if setup failed.
         * @param errorMessage the message if setupSuccessful is {@code false} or {@code null}
         *        otherwise.
         * @param exception the exception that occurred if setupSuccessful is {@code false} or {@code null}
         *        otherwise.
         */
        void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree, String errorMessage, Exception exception);
    }

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
    private String mClientTokenKey;

    /**
     * {@link Handler} to deliver events to listeners; events are always delivered on the main thread.
     */
    private final Handler mListenerHandler = new Handler(Looper.getMainLooper());

    private final List<ListenerCallback> mCallbackQueue = new LinkedList<ListenerCallback>();
    private boolean mListenersLocked = false;

    private final Set<PaymentMethodsUpdatedListener> mUpdatedListeners = new HashSet<PaymentMethodsUpdatedListener>();
    private final Set<PaymentMethodCreatedListener> mCreatedListeners = new HashSet<PaymentMethodCreatedListener>();
    private final Set<PaymentMethodNonceListener> mNonceListeners = new HashSet<PaymentMethodNonceListener>();
    private final Set<ErrorListener> mErrorListeners = new HashSet<ErrorListener>();

    private List<PaymentMethod> mCachedPaymentMethods;

    /**
     * @deprecated Use the asynchronous
     * {@link com.braintreepayments.api.Braintree#setup(android.content.Context, String, com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener)}
     * instead.
     *
     * Obtain an instance of {@link Braintree}. If multiple calls are made with the same {@code
     * clientToken}, you may get the same instance returned.
     *
     * @param context
     * @param clientToken A client token obtained from a Braintree server SDK.
     * @return {@link com.braintreepayments.api.Braintree} instance. Repeated called to
     *         {@link #getInstance(android.content.Context, String)} with the same {@code clientToken}
     *         may return the same {@link com.braintreepayments.api.Braintree} instance.
     */
    @Deprecated
    public static Braintree getInstance(Context context, String clientToken) {
        if (sInstances.containsKey(clientToken)) {
            return sInstances.get(clientToken);
        } else {
            return new Braintree(clientToken,
                    new BraintreeApi(context.getApplicationContext(), clientToken));
        }
    }

    /**
     * Called to begin the setup of {@link Braintree}. Once setup is complete the supplied
     * {@link com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener} will receive
     * a call to {@link com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener#onBraintreeSetupFinished(boolean, Braintree, String, Exception)}
     * with an instance of a {@link com.braintreepayments.api.Braintree} or an error.
     *
     * @param context
     * @param clientToken The client token obtained from a Braintree server SDK.
     * @param listener The listener to notify when setup is complete, or fails.
     */
    public static void setup(Context context, String clientToken, BraintreeSetupFinishedListener listener) {
        setupHelper(context, clientToken, listener);
    }

    /**
     * Helper method to
     * {@link #setup(android.content.Context, String, com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener)}
     * to make execution synchronous in testing.
     */
    protected static Future<?> setupHelper(final Context context, final String clientToken, final BraintreeSetupFinishedListener listener) {
        return Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                Braintree braintree = null;
                Exception exception = null;
                String errorMessage = null;
                try {
                    if (sInstances.containsKey(clientToken)) {
                        braintree = sInstances.get(clientToken);
                    } else {
                        braintree = new Braintree(context, clientToken);
                    }

                    if (!braintree.isSetup()) {
                        braintree.setup();
                    }
                } catch (Exception e) {
                    exception = e;
                    errorMessage = e.getMessage();
                }

                final Braintree finalBraintree = braintree;
                final String finalErrorMessage = errorMessage;
                final Exception finalException = exception;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalBraintree != null && finalBraintree.isSetup()) {
                            listener.onBraintreeSetupFinished(true, finalBraintree, null, null);
                        } else {
                            listener.onBraintreeSetupFinished(false, null, finalErrorMessage,
                                    finalException);
                        }
                    }
                });
            }
        });
    }

    protected Braintree(String clientToken, BraintreeApi braintreeApi) {
        mBraintreeApi = braintreeApi;
        mExecutorService = Executors.newSingleThreadExecutor();
        mIntegrationType = "custom";
        mClientTokenKey = clientToken;
        sInstances.put(mClientTokenKey, this);
    }

    protected Braintree(Context context, String clientToken) {
        mBraintreeApi = new BraintreeApi(context.getApplicationContext(),
                ClientToken.fromString(clientToken));
        mExecutorService = Executors.newSingleThreadExecutor();
        mIntegrationType = "custom";
        mClientTokenKey = clientToken;
        sInstances.put(mClientTokenKey, this);
    }

    private boolean isSetup() {
        return mBraintreeApi.isSetup();
    }

    private void setup() throws ErrorWithResponse, BraintreeException {
        mBraintreeApi.setup();
    }

    protected String analyticsPrefix() {
        return mIntegrationType + ".android";
    }

    protected String getIntegrationType() {
        return mIntegrationType;
    }

    /**
     * Saves the current state of {@link Braintree} to the provided {@link Bundle}. Call
     * This method from your {@link Activity}'s {@link Activity#onSaveInstanceState(Bundle)} method.
     *
     * @param outState The bundle supplied to {@link Activity#onSaveInstanceState(Bundle)}
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_CLIENT_TOKEN, mClientTokenKey);
        outState.putString(KEY_CONFIGURATION, mBraintreeApi.getConfigurationString());
    }

    /**
     * Restores the state of {@link Braintree} and returns an instance of {@link Braintree}.
     *
     * @param context
     * @param savedInstanceState The {@link Bundle} supplied to {@link Activity#onCreate(Bundle)} or
     *                           {@link Activity#onRestoreInstanceState(Bundle)}
     * @return The restored instance of {@link Braintree} or {@code null} if it could not be restored.
     */
    public static Braintree restoreSavedInstanceState(Context context, Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return null;
        }

        String clientToken = savedInstanceState.getString(KEY_CLIENT_TOKEN);
        String configuration = savedInstanceState.getString(KEY_CONFIGURATION);
        Braintree braintree = sInstances.get(clientToken);
        if (braintree != null && braintree.isSetup()) {
            return braintree;
        } else if (!TextUtils.isEmpty(clientToken) && !TextUtils.isEmpty(configuration)) {
            return new Braintree(clientToken,
                    new BraintreeApi(context.getApplicationContext(), clientToken, configuration));
        } else {
            return null;
        }
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
     * @return If 3D Secure is supported and enabled for the current merchant account
     */
    @Beta
    public boolean isThreeDSecureEnabled() {
        return mBraintreeApi.isThreeDSecureEnabled();
    }

    /**
     * @return If Android Pay is supported and enabled in the current environment.
     */
    @Beta
    public boolean isAndroidPayEnabled() {
        return mBraintreeApi.isAndroidPayEnabled();
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
     * To be called in {@link Activity#onResume()} each time the {@link Activity} is resumed.
     * Handles adding listeners if the {@link Activity} implements listeners as well as unlocking
     * the listeners (see {@link Braintree#unlockListeners()}.
     *
     * @param activity The {@link Activity} that is being resumed.
     */
    public synchronized void onResume(Activity activity) {
        if (activity instanceof Listener) {
            addListener((Listener) activity);
        }
        unlockListeners();
    }

    /**
     * To be called in {@link Activity#onPause()} each time the {@link Activity} is paused. Handles
     * locking (see {@link Braintree#lockListeners()}) and removing listeners if the {@link Activity}
     * implements the listeners. Also handles disconnecting the
     * {@link com.google.android.gms.common.api.GoogleApiClient} if {@link Braintree} has an active
     * connection to it.
     *
     * @param activity The {@link Activity} that is being paused.
     */
    public synchronized void onPause(Activity activity) {
        lockListeners();
        if (activity instanceof Listener) {
            removeListener((Listener) activity);
        }
        mBraintreeApi.disconnectGoogleApiClient();
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
     * Starts the Pay With PayPal flow. This will launch the PayPal app if installed or switch to
     * the browser for user authorization.
     *
     * @param activity the {@link android.app.Activity} to receive the {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *                 when payWithPayPal finishes.
     * @param requestCode the request code associated with this start request. Will be returned
     *                    in {@code onActivityResult}.
     */
    public void startPayWithPayPal(Activity activity, int requestCode) {
        startPayWithPayPal(activity, requestCode, null);
    }

    /**
     * Starts the Pay With PayPal flow with additional scopes. This will launch the PayPal app if installed or switch to
     * the browser for user authorization.
     *
     * @param activity the {@link android.app.Activity} to receive the {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *                 when payWithPayPal finishes.
     * @param requestCode the request code associated with this start request. Will be returned
     *                    in {@code onActivityResult}.
     * @param additionalScopes A {@link java.util.List} of additional scopes.
     *                         Ex: PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS. Acceptable scopes are
     *                         defined in {@link com.braintreepayments.api.PayPal}.
     */
    public void startPayWithPayPal(final Activity activity, final int requestCode, final List<String> additionalScopes) {
        mCurrentPayPalActivity = activity;
        if (activity != null) {
            BraintreeBroadcastManager.getInstance(activity)
                    .registerReceiver(mBraintreeBroadcastReceiver, new IntentFilter(
                            BraintreeBrowserSwitchActivity.LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED));
        }
        try {
            PerformRequestStatus requestStatus = mBraintreeApi.startPayWithPayPal(activity, requestCode, additionalScopes);
            sendAnalyticsForPayPalPerformRequestStatus(requestStatus, false);
        } catch (BraintreeException e) {
            postUnrecoverableErrorToListeners(e);
        }
    }

    /**
     * Starts the Checkout With PayPal flow. This will launch the PayPal app if installed or switch to
     * the browser for user authorization.
     *
     * @param activity the {@link android.app.Activity} to receive the {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *   when payWithPayPal finishes.
     * @param requestCode the request code associated with this start request. Will be returned
     * in {@code onActivityResult}.
     * @param checkout the {@link com.braintreepayments.api.PayPalCheckout} object used to create
     * a payment which the user will then be asked to authorize. Must contain a valid amount.
     */
    public void startCheckoutWithPayPal(final Activity activity, final int requestCode, final PayPalCheckout checkout) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                mCurrentPayPalActivity = activity;
                BraintreeBroadcastManager.getInstance(activity)
                        .registerReceiver(mBraintreeBroadcastReceiver, new IntentFilter(
                                BraintreeBrowserSwitchActivity.LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED));

                try {

                    final PayPalPaymentResource payPalPaymentResource = mBraintreeApi.createPayPalPaymentResource(
                            checkout, activity);

                    if (payPalPaymentResource != null) {
                        //OTC browser/app switching requires that it be run on the main thread
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                try {
                                    PerformRequestStatus requestStatus = mBraintreeApi.startCheckoutWithPayPal(activity,
                                            requestCode, payPalPaymentResource);
                                    sendAnalyticsForPayPalPerformRequestStatus(requestStatus, true);
                                } catch (ConfigurationException e) {
                                    sendAnalyticsForPayPalPerformRequestStatus(null, true);
                                    postUnrecoverableErrorToListeners(e);
                                }
                            }
                        });
                    }

                } catch (JSONException | BraintreeException e) {
                    postUnrecoverableErrorToListeners(e);
                } catch (ErrorWithResponse errorWithResponse) {
                    postRecoverableErrorToListeners(errorWithResponse);
                }
            }
        });
    }

    /**
     * Send analytics for PayPal app switching
     * @param requestStatus the {@link PerformRequestStatus} returned by PayPal OTC
     * @param isCheckout is this a single payment request
     */
    protected void sendAnalyticsForPayPalPerformRequestStatus(PerformRequestStatus requestStatus, Boolean isCheckout) {
        if (isCheckout) {
            if (requestStatus == null) {
                sendAnalyticsEvent("paypal-single-payment.none.initiate.failed");
            } else {
                if (requestStatus.isSuccess() && requestStatus.getTarget() == RequestTarget.browser) {
                    sendAnalyticsEvent("paypal-single-payment.webswitch.initiate.started");
                } else if (!requestStatus.isSuccess() && requestStatus.getTarget() == RequestTarget.browser) {
                    sendAnalyticsEvent("paypal-single-payment.webswitch.initiate.failed");
                } else if (requestStatus.isSuccess() && requestStatus.getTarget() == RequestTarget.wallet) {
                    sendAnalyticsEvent("paypal-single-payment.appswitch.initiate.started");
                } else if (!requestStatus.isSuccess() && requestStatus.getTarget() == RequestTarget.wallet) {
                    sendAnalyticsEvent("paypal-single-payment.appswitch.initiate.failed");
                }
            }
        } else {
            if (requestStatus == null) {
                sendAnalyticsEvent("paypal-future-payments.none.initiate.failed");
            } else {
                if (requestStatus.isSuccess() && requestStatus.getTarget() == RequestTarget.browser) {
                    sendAnalyticsEvent("paypal-future-payments.webswitch.initiate.started");
                } else if (!requestStatus.isSuccess() && requestStatus.getTarget() == RequestTarget.browser) {
                    sendAnalyticsEvent("paypal-future-payments.webswitch.initiate.failed");
                } else if (requestStatus.isSuccess() && requestStatus.getTarget() == RequestTarget.wallet) {
                    sendAnalyticsEvent("paypal-future-payments.appswitch.initiate.started");
                } else if (!requestStatus.isSuccess() && requestStatus.getTarget() == RequestTarget.wallet) {
                    sendAnalyticsEvent("paypal-future-payments.appswitch.initiate.failed");
                }
            }
        }
    }

    /**
     * @deprecated Use {@link com.braintreepayments.api.Braintree#finishPayWithPayPal(android.app.Activity, int, android.content.Intent)}
     * instead.
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
    @Deprecated
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
        if (activity != null) {
            BraintreeBroadcastManager.getInstance(activity).unregisterReceiver(
                    mBraintreeBroadcastReceiver);
        }

        try {

            Result result = PayPal.getResultFromActivity(activity, resultCode, data);
            Boolean isAppSwitch = data.hasExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT);
            Boolean isCheckout = PayPal.isCheckoutRequest();

            ResultType resultType = result.getResultType();
            switch (resultType) {
                case Error:
                    if (isAppSwitch && isCheckout) {
                        sendAnalyticsEvent("paypal-single-payment.appswitch.failed");
                    } else if (isAppSwitch && !isCheckout) {
                        sendAnalyticsEvent("paypal-future-payments.appswitch.failed");
                    } else if (!isAppSwitch && isCheckout) {
                        sendAnalyticsEvent("paypal-single-payment.webswitch.failed");
                    } else if (!isAppSwitch && !isCheckout) {
                        sendAnalyticsEvent("paypal-future-payments.webswitch.failed");
                    }
                    break;
                case Cancel:
                    if (isAppSwitch && isCheckout) {
                        if (result.getError() == null) {
                            sendAnalyticsEvent("paypal-single-payment.appswitch.canceled");
                        } else {
                            sendAnalyticsEvent("paypal-single-payment.appswitch.canceled-with-error");
                        }
                    } else if (isAppSwitch && !isCheckout) {
                        if (result.getError() == null) {
                            sendAnalyticsEvent("paypal-future-payments.appswitch.canceled");
                        } else {
                            sendAnalyticsEvent("paypal-future-payments.appswitch.canceled-with-error");
                        }
                    } else if (!isAppSwitch && isCheckout) {
                        if (result.getError() == null) {
                            sendAnalyticsEvent("paypal-single-payment.webswitch.canceled");
                        } else {
                            sendAnalyticsEvent("paypal-single-payment.webswitch.canceled-with-error");
                        }
                    } else if (!isAppSwitch && !isCheckout) {
                        if (result.getError() == null) {
                            sendAnalyticsEvent("paypal-future-payments.webswitch.canceled");
                        } else {
                            sendAnalyticsEvent("paypal-future-payments.webswitch.canceled-with-error");
                        }

                    }
                    break;
                case Success:
                    if (isAppSwitch && isCheckout) {
                        sendAnalyticsEvent("paypal-single-payment.appswitch.succeeded");
                    } else if (isAppSwitch && !isCheckout) {
                        sendAnalyticsEvent("paypal-future-payments.appswitch.succeeded");
                    } else if (!isAppSwitch && isCheckout) {
                        sendAnalyticsEvent("paypal-single-payment.webswitch.succeeded");
                    } else if (!isAppSwitch && !isCheckout) {
                        sendAnalyticsEvent("paypal-future-payments.webswitch.succeeded");
                    }
                    PayPalAccountBuilder payPalAccountBuilder = mBraintreeApi.handlePayPalResponse(activity,
                            resultCode, data);
                    if (payPalAccountBuilder != null) {
                        create(payPalAccountBuilder);
                    }
                    break;
            }

        } catch (ConfigurationException e) {
            postUnrecoverableErrorToListeners(e);
        }
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
     * Get Braintree specific tokenization parameters for Android Pay. Useful for existing Google Wallet
     * or Android Pay integrations, or when full control over the {@link com.google.android.gms.wallet.MaskedWalletRequest}
     * and {@link com.google.android.gms.wallet.FullWalletRequest} is required.
     *
     * These parameters should be supplied to the
     * {@link com.google.android.gms.wallet.MaskedWalletRequest} via
     * {@link com.google.android.gms.wallet.MaskedWalletRequest.Builder#setPaymentMethodTokenizationParameters(PaymentMethodTokenizationParameters)}.
     *
     * @return the {@link PaymentMethodTokenizationParameters}
     */
    @Beta
    public PaymentMethodTokenizationParameters getAndroidPayTokenizationParameters() {
        return mBraintreeApi.getAndroidPayTokenizationParameters();
    }

    /**
     * Get the Google transaction id from an Android Pay request. If the request is a masked or full
     * wallet request indicated by the presence of {@link WalletConstants#EXTRA_MASKED_WALLET} or
     * {@link WalletConstants#EXTRA_FULL_WALLET} in the intent, the transaction id will be returned,
     * otherwise {@code null} is returned.
     *
     * @param data The {@link Intent} to parse the transaction id from.
     * @return The {@link String} transaction id or {@code null}.
     */
    @Beta
    public String getAndroidPayGoogleTransactionId(Intent data) {
        if (AndroidPay.isMaskedWalletResponse(data)) {
            return ((MaskedWallet) data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET))
                    .getGoogleTransactionId();
        } else if (AndroidPay.isFullWalletResponse(data)) {
            return ((FullWallet) data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET))
                    .getGoogleTransactionId();
        } else {
            return null;
        }
    }

    /**
     * Launch an Android Pay masked wallet request. This method will show the payment instrument
     * chooser to the user.
     *
     * @param activity The current {@link Activity}.
     * @param requestCode The requestCode for this request.
     * @param cart The cart representation with price and optionally items.
     */
    @Beta
    public synchronized void performAndroidPayMaskedWalletRequest(Activity activity, int requestCode, Cart cart) {
        performAndroidPayMaskedWalletRequest(activity, requestCode, cart, false, false, false);
    }

    /**
     * Launch an Android Pay masked wallet request. This method will show the payment instrument
     * chooser to the user.
     *
     * @param activity The current {@link Activity}.
     * @param requestCode The requestCode for this request.
     * @param cart The cart representation with price and optionally items.
     * @param isBillingAgreement {@code true} if this request is for a billing agreement, {@code false} otherwise.
     * @param shippingAddressRequired {@code true} if this request requires a shipping address, {@code false} otherwise.
     * @param phoneNumberRequired {@code true} if this request requires a phone number, {@code false} otherwise.
     */
    @Beta
    public synchronized void performAndroidPayMaskedWalletRequest(final Activity activity,
            final int requestCode, final Cart cart, final boolean isBillingAgreement, final boolean shippingAddressRequired,
            final boolean phoneNumberRequired) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    mBraintreeApi.performAndroidPayMaskedWalletRequest(activity, requestCode, cart,
                            isBillingAgreement, shippingAddressRequired, phoneNumberRequired);
                } catch (InvalidArgumentException e) {
                    postUnrecoverableErrorToListeners(e);
                } catch (UnexpectedException e) {
                    postUnrecoverableErrorToListeners(e);
                }
            }
        });
    }

    /**
     * Perform a change masked wallet request and allow the user to change the payment instrument they
     * have selected.
     *
     * @param activity The current {@link Activity}.
     * @param requestCode The requestCode for this request.
     * @param googleTransactionId The transaction id of the {@link MaskedWallet} to change.
     */
    @Beta
    public synchronized void performAndroidPayChangeMaskedWalletRequest(final Activity activity,
            final int requestCode, final String googleTransactionId) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    mBraintreeApi.performAndroidPayChangeMaskedWalletRequest(activity, requestCode,
                            googleTransactionId);
                } catch (UnexpectedException e) {
                    postUnrecoverableErrorToListeners(e);
                }
            }
        });
    }

    /**
     * Perform a full wallet request. This can only be done after a masked wallet request has been
     * made.
     *
     * @param activity The current {@link Activity}
     * @param requestCode The requestCode for this request.
     * @param cart The cart representation with the price and optionally items.
     * @param googleTransactionId The transaction id from the {@link MaskedWallet}.
     */
    @Beta
    public synchronized void performAndroidPayFullWalletRequest(final Activity activity,
            final int requestCode, final Cart cart, final String googleTransactionId) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    mBraintreeApi.performAndroidPayFullWalletRequest(activity, requestCode, cart,
                            googleTransactionId);
                } catch (UnexpectedException e) {
                    postUnrecoverableErrorToListeners(e);
                }
            }
        });
    }

    /**
     * Parse a payment method nonce from a {@link FullWallet} response.
     *
     * @param resultCode The resultCode of the request.
     * @param data The {@link Intent} containing the {@link FullWallet}.
     */
    @Beta
    public synchronized void getNonceFromAndroidPayFullWalletResponse(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                mBraintreeApi.disconnectGoogleApiClient();

                AndroidPayCard androidPayCard =
                        mBraintreeApi.getNonceFromAndroidPayFullWalletResponse(data);
                if (androidPayCard != null) {
                    addPaymentMethodToCache(androidPayCard);
                    postCreatedMethodToListeners(androidPayCard);
                    postCreatedNonceToListeners(androidPayCard.getNonce());
                }
            } catch (JSONException e) {
                postUnrecoverableErrorToListeners(e);
            }
        }
    }

    /**
     * 3D Secure is a protocol that enables cardholders and issuers to add a layer of security
     * to e-commerce transactions via password entry at checkout.
     *
     * One of the primary reasons to use 3D Secure is to benefit from a shift in liability from the
     * merchant to the issuer, which may result in interchange savings. Please read our online
     * documentation (<a href="https://developers.braintreepayments.com">https://developers.braintreepayments.com</a>)
     * for a full explanation of 3D Secure.
     *
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when generating a client token
     * (See <a href="https://developers.braintreepayments.com/android/sdk/overview/generate-client-token">https://developers.braintreepayments.com/android/sdk/overview/generate-client-token</a>).
     *
     * During lookup the original payment method nonce is consumed and a new one is returned,
     * which points to the original payment method, as well as the 3D Secure verification.
     * Transactions created with this nonce will be 3D Secure, and benefit from the appropriate
     * liability shift if authentication is successful or fail with a 3D Secure failure.
     *
     * @param activity The {@link android.app.Activity} to receive {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *                 when {@link #startThreeDSecureVerification(android.app.Activity, int, String, String)} finishes.
     * @param requestCode The request code associated with this start request.
     *                    Will be returned in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param cardBuilder The cardBuilder created from raw details. Will be tokenized before
     *                    the 3D Secure verification if performed.
     * @param amount The amount of the transaction in the current merchant account's currency
     */
    @Beta
    public synchronized void startThreeDSecureVerification(final Activity activity,
            final int requestCode, final CardBuilder cardBuilder, final String amount) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String nonce = mBraintreeApi.tokenize(cardBuilder);
                    startThreeDSecureVerification(activity, requestCode, nonce, amount);
                } catch (BraintreeException e) {
                    postUnrecoverableErrorToListeners(e);
                } catch (ErrorWithResponse errorWithResponse) {
                    postRecoverableErrorToListeners(errorWithResponse);
                } catch (JSONException e) {
                    postUnrecoverableErrorToListeners(e);
                }
            }
        });
    }

    /**
     * 3D Secure is a protocol that enables cardholders and issuers to add a layer of security
     * to e-commerce transactions via password entry at checkout.
     *
     * One of the primary reasons to use 3D Secure is to benefit from a shift in liability from the
     * merchant to the issuer, which may result in interchange savings. Please read our online
     * documentation (<a href="https://developers.braintreepayments.com">https://developers.braintreepayments.com</a>)
     * for a full explanation of 3D Secure.
     *
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when generating a client token
     * (See <a href="https://developers.braintreepayments.com/android/sdk/overview/generate-client-token">https://developers.braintreepayments.com/android/sdk/overview/generate-client-token</a>).
     *
     * During lookup the original payment method nonce is consumed and a new one is returned,
     * which points to the original payment method, as well as the 3D Secure verification.
     * Transactions created with this nonce will be 3D Secure, and benefit from the appropriate
     * liability shift if authentication is successful or fail with a 3D Secure failure.
     *
     * @param activity The {@link android.app.Activity} to receive {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *                 when {@link #startThreeDSecureVerification(android.app.Activity, int, String, String)} finishes.
     * @param requestCode The request code associated with this start request.
     *                    Will be returned in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param nonce The nonce that represents a card to perform a 3D Secure verification against
     * @param amount The amount of the transaction in the current merchant account's currency
     */
    @Beta
    public synchronized void startThreeDSecureVerification(final Activity activity,
            final int requestCode, final String nonce, final String amount) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ThreeDSecureLookup threeDSecureLookup = mBraintreeApi.threeDSecureLookup(nonce,
                            amount);
                    if (threeDSecureLookup.getAcsUrl() != null) {
                        Intent intent = new Intent(activity, ThreeDSecureWebViewActivity.class)
                                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_LOOKUP, threeDSecureLookup);
                        activity.startActivityForResult(intent, requestCode);
                    } else {
                        postCreatedMethodToListeners(threeDSecureLookup.getCard());
                        postCreatedNonceToListeners(threeDSecureLookup.getCard().getNonce());
                    }
                } catch (BraintreeException e) {
                    postUnrecoverableErrorToListeners(e);
                } catch (JSONException e) {
                    postUnrecoverableErrorToListeners(e);
                } catch (ErrorWithResponse errorWithResponse) {
                    postRecoverableErrorToListeners(errorWithResponse);
                }
            }
        });
    }

    /**
     * Method to finish a 3D Secure verification. Results in a new payment method nonce that points
     * to the original payment method, as well as the 3D Secure verification.
     *
     * The {@link com.braintreepayments.api.models.Card} will be sent to
     * {@link Braintree.PaymentMethodCreatedListener#onPaymentMethodCreated(com.braintreepayments.api.models.PaymentMethod)}
     * and the nonce will be sent to
     * {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * If an error occurs, the exception that occurred will be sent to
     * {@link Braintree.ErrorListener#onRecoverableError(com.braintreepayments.api.exceptions.ErrorWithResponse)}.
     *
     * <b>Note:</b> If resultCode is not {@link android.app.Activity#RESULT_OK} no listeners will be called.
     *
     * @param resultCode The result code provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param data The {@link android.content.Intent} provided in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *
     * @see #startThreeDSecureVerification(android.app.Activity, int, String, String)
     */
    @Beta
    public synchronized void finishThreeDSecureVerification(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            ThreeDSecureAuthenticationResponse authenticationResponse =
                    data.getParcelableExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);
            if (authenticationResponse.isSuccess()) {
                postCreatedMethodToListeners(authenticationResponse.getCard());
                postCreatedNonceToListeners(authenticationResponse.getCard().getNonce());
            } else if (authenticationResponse.getException() != null) {
                postUnrecoverableErrorToListeners(new BraintreeException(authenticationResponse.getException()));
            } else {
                postRecoverableErrorToListeners(new ErrorWithResponse(422, authenticationResponse.getErrors()));
            }
        }
    }

    /**
     * Handles processing of any Braintree associated activity results and posts
     * {@link PaymentMethod}s or nonces to listeners as appropriate.
     *
     * @param activity The activity that received the {@link Activity#onActivityResult(int, int, Intent)}
     * @param requestCode The requestCode received in {@link Activity#onActivityResult(int, int, Intent)}
     * @param responseCode The responseCode received in {@link Activity#onActivityResult(int, int, Intent)}
     * @param data The {@link Intent} received in {@link Activity#onActivityResult(int, int, Intent)}
     */
    public void onActivityResult(Activity activity, int requestCode, int responseCode, Intent data) {
        if (responseCode == Activity.RESULT_OK && data != null) {
            if (PayPal.isPayPalIntent(data)) {
                finishPayWithPayPal(activity, responseCode, data);
            } else if (AndroidPay.isMaskedWalletResponse(data)) {
                performAndroidPayFullWalletRequest(activity, requestCode, null, getAndroidPayGoogleTransactionId(data));
            } else if (AndroidPay.isFullWalletResponse(data)) {
                getNonceFromAndroidPayFullWalletResponse(responseCode, data);
            } else if (VenmoAppSwitch.isVenmoAppSwitchResponse(data)) {
                finishPayWithVenmo(responseCode, data);
            } else if (ThreeDSecureAuthenticationResponse.isThreeDSecureAuthenticationResponse(data)) {
                finishThreeDSecureVerification(responseCode, data);
            }
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
     * Helper method to {@link #create(PaymentMethod.Builder)} to make execution synchronous.
     */
    protected synchronized <T extends PaymentMethod> Future<?> createHelper(
            final PaymentMethod.Builder<T> paymentMethodBuilder) {
        return mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    PaymentMethod createdPaymentMethod = mBraintreeApi.create(paymentMethodBuilder);
                    addPaymentMethodToCache(createdPaymentMethod);
                    if (paymentMethodBuilder.getClass() == PayPalAccountBuilder.class) {
                        if (PayPal.isCheckoutRequest()) {
                            sendAnalyticsEvent("paypal-single-payment.tokenize.succeeded");
                        } else {
                            sendAnalyticsEvent("paypal-future-payments.tokenize.succeeded");
                        }
                    }
                    postCreatedMethodToListeners(createdPaymentMethod);
                    postCreatedNonceToListeners(createdPaymentMethod.getNonce());
                } catch (BraintreeException e) {
                    postUnrecoverableErrorToListeners(e);
                } catch (ErrorWithResponse e) {
                    if (paymentMethodBuilder.getClass() == PayPalAccountBuilder.class) {
                        if (PayPal.isCheckoutRequest()) {
                            sendAnalyticsEvent("paypal-single-payment.tokenize.failed");
                        } else {
                            sendAnalyticsEvent("paypal-future-payments.tokenize.failed");
                        }
                    }
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
                } catch (JSONException e) {
                    postUnrecoverableErrorToListeners(e);
                }
            }
        });
    }

    /**
     * @deprecated Use {@link #sendAnalyticsEvent(String)} instead.
     *
     * Sends analytics event to send to the Braintree analytics service. Used internally and by Drop-In.
     * @param event Name of event to be sent.
     * @param integrationType The type of integration used. Should be "custom" for those directly
     *                        using {@link Braintree} or {@link BraintreeApi} without Drop-In
     */
    @Deprecated
    public synchronized void sendAnalyticsEvent(String event, String integrationType) {
        sendAnalyticsEventHelper(event, integrationType);
    }

    /**
     * Sends analytics event to the Braintree analytics service.
     *
     * @param eventFragment Event to be sent.
     */
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
