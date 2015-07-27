package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Base64;

import com.braintreepayments.api.annotations.Beta;
import com.braintreepayments.api.data.BraintreeData;
import com.braintreepayments.api.data.BraintreeEnvironment;
import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AnalyticsRequest;
import com.braintreepayments.api.models.AndroidPayCard;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.threedsecure.ThreeDSecureWebViewActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.WalletConstants;
import com.paypal.android.sdk.payments.PayPalConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Braintree {

    private static final String KEY_CLIENT_TOKEN = "com.braintreepayments.api.KEY_CLIENT_TOKEN";
    private static final String KEY_CONFIGURATION =  "com.braintreepayments.api.KEY_CONFIGURATION";

    private static final String PAYMENT_METHOD_ENDPOINT = "payment_methods";

    protected static final Map<String, Braintree> sInstances = new HashMap<String, Braintree>();
    protected static final String INTEGRATION_DROPIN = "dropin";

    /**
     * Base interface for all event listeners. Only concrete classes that implement this interface
     * (either directly or indirectly) can be registered with
     * {@link #addListener(com.braintreepayments.api.Braintree.Listener)}.
     */
    private interface Listener {}

    /**
     * Interface that defines the response for
     * {@link com.braintreepayments.api.Braintree#setup(android.content.Context, String, com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener)}
     */
    public interface BraintreeSetupFinishedListener {
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
    public interface PaymentMethodsUpdatedListener extends Listener {
        void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods);
    }

    /**
     * onPaymentMethodCreated will be called with a {@link com.braintreepayments.api.models.PaymentMethod}
     * as a callback when
     * {@link Braintree#create(com.braintreepayments.api.models.PaymentMethodBuilder)} is called
     */
    public interface PaymentMethodCreatedListener extends Listener {
        void onPaymentMethodCreated(PaymentMethod paymentMethod);
    }

    /**
     * onPaymentMethodNonce will be called as a callback with a nonce when
     * {@link Braintree#create(com.braintreepayments.api.models.PaymentMethodBuilder)}
     * or {@link Braintree#tokenize(com.braintreepayments.api.models.PaymentMethodBuilder)}
     * is called
     */
    public interface PaymentMethodNonceListener extends Listener {
        void onPaymentMethodNonce(String paymentMethodNonce);
    }

    /**
     * onUnrecoverableError will be called where there is an exception that cannot be handled.
     * onRecoverableError will be called on data validation errors
     */
    public interface ErrorListener extends Listener {
        void onUnrecoverableError(Throwable throwable);
        void onRecoverableError(ErrorWithResponse error);
    }

    private String mIntegrationType;
    private String mClientTokenKey;

    private final List<ListenerCallback> mCallbackQueue = new LinkedList<ListenerCallback>();
    private boolean mListenersLocked = false;

    private final Set<PaymentMethodsUpdatedListener> mUpdatedListeners = new HashSet<PaymentMethodsUpdatedListener>();
    private final Set<PaymentMethodCreatedListener> mCreatedListeners = new HashSet<PaymentMethodCreatedListener>();
    private final Set<PaymentMethodNonceListener> mNonceListeners = new HashSet<PaymentMethodNonceListener>();
    private final Set<ErrorListener> mErrorListeners = new HashSet<ErrorListener>();

    private List<PaymentMethod> mCachedPaymentMethods;

    private Context mContext;
    private ClientToken mClientToken;
    private Configuration mConfiguration;
    @VisibleForTesting
    protected BraintreeHttpClient mHttpClient;

    private Object mBraintreeData;
    private AndroidPay mAndroidPay;

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
        try {
            Braintree braintree;
            if (sInstances.containsKey(clientToken)) {
                braintree = sInstances.get(clientToken);
            } else {
                braintree = new Braintree(context, clientToken);
            }

            if (!braintree.isSetup()) {
                braintree.setup(listener);
            } else {
                listener.onBraintreeSetupFinished(true, braintree, null, null);
            }
        } catch (JSONException e) {
            listener.onBraintreeSetupFinished(false, null, e.getMessage(), e);
        }
    }

    protected Braintree(Context context, String clientTokenString) throws JSONException {
        this(context, clientTokenString, null);
    }

    protected Braintree(Context context, String clientTokenString, String configurationString)
            throws JSONException {
        Pattern pattern = Pattern.compile(
                "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)");
        if (pattern.matcher(clientTokenString).matches()) {
            clientTokenString = new String(Base64.decode(clientTokenString, Base64.DEFAULT));
        }

        mContext = context.getApplicationContext();
        mClientToken = ClientToken.fromString(clientTokenString);
        mHttpClient = new BraintreeHttpClient(mClientToken.getAuthorizationFingerprint());

        if (configurationString != null) {
            mConfiguration = Configuration.fromJson(configurationString);
            mHttpClient.setBaseUrl(mConfiguration.getClientApiUrl());
        }

        mIntegrationType = "custom";
        mClientTokenKey = clientTokenString;
        sInstances.put(mClientTokenKey, this);
    }

    private boolean isSetup() {
        return mConfiguration != null;
    }

    private void setup(final BraintreeSetupFinishedListener listener) {
        String configUrl = Uri.parse(mClientToken.getConfigUrl())
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();

        mHttpClient.get(configUrl, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    mConfiguration = Configuration.fromJson(responseBody);
                    mHttpClient.setBaseUrl(mConfiguration.getClientApiUrl());

                    listener.onBraintreeSetupFinished(true, Braintree.this, null, null);
                } catch (JSONException e) {
                    listener.onBraintreeSetupFinished(false, null, e.getMessage(), e);
                }
            }

            @Override
            public void failure(Exception exception) {
                listener.onBraintreeSetupFinished(false, null, exception.getMessage(), exception);
            }
        });
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
        outState.putString(KEY_CONFIGURATION, mConfiguration.toJson());
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
            try {
                return new Braintree(context.getApplicationContext(), clientToken, configuration);
            } catch (JSONException e) {
                return null;
            }
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
     * @return {@code true} if PayPal is enabled and supported in the current environment,
     *         {@code false} otherwise.
     */
    public boolean isPayPalEnabled() {
        return mConfiguration.isPayPalEnabled();
    }

    /**
     * @return {@code true} if Venmo app switch is supported and enabled in the current environment,
     *         {@code false} otherwise.
     */
    public boolean isVenmoEnabled() {
        return Venmo.isAvailable(mContext, mConfiguration);
    }

    /**
     * @return {@code true} if 3D Secure is supported and enabled for the current merchant account,
     *         {@code false} otherwise.
     */
    @Beta
    public boolean isThreeDSecureEnabled() {
        return mConfiguration.isThreeDSecureEnabled();
    }

    /**
     * @return {@code true} if Android Pay is supported and enabled in the current environment,
     *         {@code false} otherwise.
     */
    @Beta
    public boolean isAndroidPayEnabled() {
        try {
            return (mConfiguration.getAndroidPay().isEnabled() &&
                    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS);
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * @return {@code true} if cvv is required to add a new card, {@code false} otherwise.
     */
    public boolean isCvvChallenegePresent() {
        return mConfiguration.isCvvChallengePresent();
    }

    /**
     * @return {@code true} if postal code is required to add a new card {@code false} otherwise.
     */
    public boolean isPostalCodeChallengePresent() {
        return mConfiguration.isPostalCodeChallengePresent();
    }

    /**
     * Adds a listener. Listeners must be removed when they are no longer necessary (such as in
     * {@link android.app.Activity#onDestroy()}) to avoid memory leaks.
     *
     * @param listener the listener to add.
     */
    public <T extends Listener> void addListener(final T listener) {
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
    public <T extends Listener> void removeListener(T listener) {
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
    public void onResume(Activity activity) {
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
    public void onPause(Activity activity) {
        lockListeners();
        if (activity instanceof Listener) {
            removeListener((Listener) activity);
        }

        if (mAndroidPay != null) {
            mAndroidPay.disconnect();
        }
    }

    /**
     * Retrieves the current list of {@link PaymentMethod} for this device and client token.
     *
     * When finished, the {@link java.util.List} of {@link PaymentMethod}s will be sent to
     * {@link Braintree.PaymentMethodsUpdatedListener#onPaymentMethodsUpdated(java.util.List)}.
     *
     * If a network or server error occurs, {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)}
     * will be called with the {@link com.braintreepayments.api.exceptions.BraintreeException} that occurred.
     */
    public void getPaymentMethods() {
        mHttpClient.get(versionedPath(PAYMENT_METHOD_ENDPOINT),
                new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        try {
                            List<PaymentMethod> paymentMethods =
                                    PaymentMethod.parsePaymentMethods(responseBody);
                            mCachedPaymentMethods = paymentMethods;
                            postPaymentMethodsToListeners(paymentMethods);
                        } catch (JSONException e) {
                            postExceptionToListeners(e);
                        }
                    }

                    @Override
                    public void failure(Exception exception) {
                        postExceptionToListeners(exception);
                    }
                });
    }

    /**
     * Starts the Pay With PayPal flow. This will launch a new activity for the PayPal mobile SDK.
     *
     * @param activity the {@link Activity} to receive the {@link Activity#onActivityResult(int, int, Intent)}
     *                 when pay with PayPal finishes.
     * @param requestCode the request code associated with this start request. Will be returned
     *                  {@link Activity#onActivityResult(int, int, Intent)}
     */
    public void startPayWithPayPal(Activity activity, int requestCode) {
        startPayWithPayPal(activity, requestCode, null);
    }

    /**
     * Starts the Pay With PayPal flow. This will launch a new activity for the PayPal mobile SDK.
     *
     * @param activity the {@link Activity} to receive the {@link Activity#onActivityResult(int, int, Intent)}
     *                 when pay with PayPal finishes.
     * @param requestCode the request code associated with this start request. Will be returned
     *                 {@link Activity#onActivityResult(int, int, Intent)}
     * @param additionalScopes A {@link List} of additional scopes.
     *                         Ex: PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS. Acceptable scopes are
     *                         defined in {@link com.paypal.android.sdk.payments.PayPalOAuthScopes}.
     */
    public void startPayWithPayPal(Activity activity, int requestCode, List<String> additionalScopes) {
        sendAnalyticsEvent("add-paypal.start");
        PayPal.startPaypalService(mContext, mConfiguration.getPayPal());
        activity.startActivityForResult(
                PayPal.getLaunchIntent(mContext, mConfiguration.getPayPal(), additionalScopes),
                requestCode);
    }

    /**
     * Method to finish Pay With PayPal flow. Create a {@link com.braintreepayments.api.models.PayPalAccount}.
     *
     * The {@link com.braintreepayments.api.models.PayPalAccount} will be sent to
     * {@link Braintree.PaymentMethodCreatedListener#onPaymentMethodCreated(PaymentMethod)}
     * and the nonce will be sent to
     * {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * If an error occurs, the exception that occurred will be sent to
     * {@link Braintree.ErrorListener#onRecoverableError(com.braintreepayments.api.exceptions.ErrorWithResponse)} or
     * {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)} as appropriate.
     *
     * @param activity the calling {@link Activity}.
     * @param resultCode Result code from the Pay With PayPal flow.
     * @param data {@link Intent} returned from Pay With PayPal flow.
     */
    public void finishPayWithPayPal(Activity activity, int resultCode, Intent data) {
        try {
            PayPal.stopPaypalService(mContext);
            PayPalAccountBuilder payPalAccountBuilder = PayPal.getBuilderFromActivity(activity, resultCode, data);
            if (payPalAccountBuilder != null) {
                create(payPalAccountBuilder);
            }
        } catch (ConfigurationException e) {
            postExceptionToListeners(e);
        }
    }

    /**
     * Start the Pay With Venmo flow. This will app switch to the Venmo app.
     *
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to
     * {@link com.braintreepayments.api.Braintree.ErrorListener#onUnrecoverableError(Throwable)}.
     *
     * @param activity The {@link android.app.Activity} to receive {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     *        when {@link #startPayWithVenmo(android.app.Activity, int)} finishes.
     * @param requestCode The request code associated with this start request. Will be returned in
     *                    {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     */
    public void startPayWithVenmo(Activity activity, int requestCode) {
        sendAnalyticsEvent("add-venmo.start");
        if (isVenmoEnabled()) {
            activity.startActivityForResult(Venmo.getLaunchIntent(mConfiguration), requestCode);
        } else {
            sendAnalyticsEvent("add-venmo.unavailable");
            postExceptionToListeners(new AppSwitchNotAvailableException("Venmo is not available"));
        }
    }

    /**
     * Method to finish the Pay With Venmo flow. Create a {@link PaymentMethod}.
     *
     * The {@link PaymentMethod} will be sent to
     * {@link Braintree.PaymentMethodCreatedListener#onPaymentMethodCreated(PaymentMethod)}}
     * and the nonce will be sent to
     * {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * If an error occurs, the exception that occurred will be sent to
     * {@link Braintree.ErrorListener#onRecoverableError(ErrorWithResponse)} or
     * {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)} as appropriate.
     *
     * @param resultCode Result code from the Pay With Venmo flow.
     * @param data {@link Intent} returned from Pay With Venmo flow in {@link Activity#onActivityResult(int, int, Intent)}
     */
    public void finishPayWithVenmo(int resultCode, Intent data) {
        final String nonce = Venmo.handleAppSwitchResponse(data);

        if (TextUtils.isEmpty(nonce)) {
            sendAnalyticsEvent("venmo-app.fail");
            return;
        }

        mHttpClient.get(versionedPath(PAYMENT_METHOD_ENDPOINT + "/" + nonce),
                new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        try {
                            List<PaymentMethod> paymentMethodsList =
                                    PaymentMethod.parsePaymentMethods(responseBody);
                            if (paymentMethodsList.size() == 1) {
                                sendAnalyticsEvent("venmo-app.success");

                                PaymentMethod paymentMethod = paymentMethodsList.get(0);
                                addPaymentMethodToCache(paymentMethod);
                                postCreatedMethodToListeners(paymentMethod);
                                postCreatedNonceToListeners(nonce);
                            } else {
                                failure(new ServerException(
                                        "Unexpected payment method response format."));
                            }
                        } catch (JSONException e) {
                            failure(e);
                        }
                    }

                    @Override
                    public void failure(Exception exception) {
                        sendAnalyticsEvent("venmo-app.fail");
                        postExceptionToListeners(exception);
                    }
                });
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
        if (mAndroidPay == null) {
            mAndroidPay = new AndroidPay(mConfiguration);
        }

        return mAndroidPay.getTokenizationParameters();
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
    public void performAndroidPayMaskedWalletRequest(Activity activity, int requestCode, Cart cart) {
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
    public void performAndroidPayMaskedWalletRequest(final Activity activity,
            final int requestCode, final Cart cart, final boolean isBillingAgreement, final boolean shippingAddressRequired,
            final boolean phoneNumberRequired) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isBillingAgreement && cart != null) {
                        throw new InvalidArgumentException(
                                "The cart must be null when isBillingAgreement is true");
                    } else if (!isBillingAgreement && cart == null) {
                        throw new InvalidArgumentException(
                                "Cart cannot be null unless isBillingAgreement is true");
                    }

                    if (mAndroidPay == null) {
                        mAndroidPay = new AndroidPay(mConfiguration);
                    }

                    mAndroidPay.setCart(cart);
                    mAndroidPay
                            .performMaskedWalletRequest(activity, requestCode, isBillingAgreement,
                                    shippingAddressRequired, phoneNumberRequired);
                } catch (InvalidArgumentException | UnexpectedException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            postExceptionToListeners(e);
                        }
                    });
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
    public void performAndroidPayChangeMaskedWalletRequest(final Activity activity,
            final int requestCode, final String googleTransactionId) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAndroidPay == null) {
                        mAndroidPay = new AndroidPay(mConfiguration);
                    }

                    mAndroidPay.performChangeMaskedWalletRequest(activity, requestCode,
                            googleTransactionId);
                } catch (final UnexpectedException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            postExceptionToListeners(e);
                        }
                    });
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
    public void performAndroidPayFullWalletRequest(final Activity activity,
            final int requestCode, final Cart cart, final String googleTransactionId) {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAndroidPay == null) {
                        mAndroidPay = new AndroidPay(mConfiguration);
                    }

                    if (cart != null) {
                        mAndroidPay.setCart(cart);
                    }

                    mAndroidPay
                            .performFullWalletRequest(activity, requestCode, googleTransactionId);
                } catch (final UnexpectedException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            postExceptionToListeners(e);
                        }
                    });
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
    public void getNonceFromAndroidPayFullWalletResponse(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                if (mAndroidPay != null) {
                    mAndroidPay.disconnect();
                }

                if (AndroidPay.isFullWalletResponse(data)) {
                    FullWallet fullWallet = data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET);
                    AndroidPayCard androidPayCard = AndroidPayCard.fromJson(
                            fullWallet.getPaymentMethodToken().getToken());

                    if (androidPayCard != null) {
                        addPaymentMethodToCache(androidPayCard);
                        postCreatedMethodToListeners(androidPayCard);
                        postCreatedNonceToListeners(androidPayCard.getNonce());
                    }
                }
            } catch (JSONException e) {
                postExceptionToListeners(e);
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
     * @param activity The {@link Activity} to receive {@link Activity#onActivityResult(int, int, Intent)}
     *                 when {@link #startThreeDSecureVerification(Activity, int, String, String)} finishes.
     * @param requestCode The request code associated with this start request,
     *                    returned in {@link Activity#onActivityResult(int, int, Intent)}.
     * @param cardBuilder The cardBuilder created from raw details. Will be tokenized before
     *                    the 3D Secure verification if performed.
     * @param amount The amount of the transaction in the current merchant account's currency
     */
    @Beta
    public void startThreeDSecureVerification(final Activity activity,
            final int requestCode, final CardBuilder cardBuilder, final String amount) {
        tokenize(cardBuilder, new PaymentMethodResponseCallback() {
            @Override
            public void success(PaymentMethod paymentMethod) {
                startThreeDSecureVerification(activity, requestCode, paymentMethod.getNonce(),
                        amount);
            }

            @Override
            public void failure(Exception exception) {
                postExceptionToListeners(exception);
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
     * @param activity The {@link Activity} to receive {@link Activity#onActivityResult(int, int, Intent)}
     *                 when {@link #startThreeDSecureVerification(Activity, int, String, String)} finishes.
     * @param requestCode The request code associated with this start request,
     *                    returned in {@link Activity#onActivityResult(int, int, Intent)}.
     * @param nonce The nonce that represents a card to perform a 3D Secure verification against.
     * @param amount The amount of the transaction in the current merchant account's currency.
     */
    @Beta
    public void startThreeDSecureVerification(final Activity activity, final int requestCode,
            final String nonce, final String amount) {
        try {
            JSONObject params = new JSONObject()
                    .put("merchantAccountId", mConfiguration.getMerchantAccountId())
                    .put("amount", amount);

            mHttpClient.post(
                    versionedPath(PAYMENT_METHOD_ENDPOINT + "/" + nonce + "/three_d_secure/lookup"),
                    params.toString(),
                    new HttpResponseCallback() {
                        @Override
                        public void success(String responseBody) {
                            try {
                                ThreeDSecureLookup threeDSecureLookup =
                                        ThreeDSecureLookup.fromJson(responseBody);
                                if (threeDSecureLookup.getAcsUrl() != null) {
                                    Intent intent =
                                            new Intent(activity, ThreeDSecureWebViewActivity.class)
                                                    .putExtra(
                                                            ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_LOOKUP,
                                                            threeDSecureLookup);
                                    activity.startActivityForResult(intent, requestCode);
                                } else {
                                    postCreatedMethodToListeners(threeDSecureLookup.getCard());
                                    postCreatedNonceToListeners(
                                            threeDSecureLookup.getCard().getNonce());
                                }
                            } catch (JSONException e) {
                                postExceptionToListeners(e);
                            }
                        }

                        @Override
                        public void failure(Exception exception) {
                            postExceptionToListeners(exception);
                        }
                    });
        } catch (JSONException e) {
            postExceptionToListeners(e);
        }
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
    public void finishThreeDSecureVerification(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            ThreeDSecureAuthenticationResponse authenticationResponse =
                    data.getParcelableExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT);
            if (authenticationResponse.isSuccess()) {
                postCreatedMethodToListeners(authenticationResponse.getCard());
                postCreatedNonceToListeners(authenticationResponse.getCard().getNonce());
            } else if (authenticationResponse.getException() != null) {
                postExceptionToListeners(new BraintreeException(authenticationResponse.getException()));
            } else {
                postExceptionToListeners(
                        new ErrorWithResponse(422, authenticationResponse.getErrors()));
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
                performAndroidPayFullWalletRequest(activity, requestCode, null,
                        getAndroidPayGoogleTransactionId(data));
            } else if (AndroidPay.isFullWalletResponse(data)) {
                getNonceFromAndroidPayFullWalletResponse(responseCode, data);
            } else if (Venmo.isVenmoAppSwitchResponse(data)) {
                finishPayWithVenmo(responseCode, data);
            } else if (ThreeDSecureAuthenticationResponse.isThreeDSecureAuthenticationResponse(data)) {
                finishThreeDSecureVerification(responseCode, data);
            }
        }
    }

    /**
     * Create a {@link com.braintreepayments.api.models.PaymentMethod} in the Braintree Gateway.
     *
     * On completion, returns the {@link PaymentMethod} to
     * {@link Braintree.PaymentMethodCreatedListener#onPaymentMethodCreated(PaymentMethod)} and
     * nonce to {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * If creation fails validation, {@link Braintree.ErrorListener#onRecoverableError(ErrorWithResponse)}
     * will be called with the resulting {@link ErrorWithResponse}.
     *
     * If an error not due to validation (server error, network issue, etc.) occurs,
     * {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)} will be called
     * with the {@link com.braintreepayments.api.exceptions.BraintreeException} that occurred.
     *
     * @param paymentMethodBuilder {@link PaymentMethodBuilder} for the {@link PaymentMethod}
     *        to be created.
     */
    public void create(final PaymentMethodBuilder paymentMethodBuilder) {
        create(paymentMethodBuilder, null);
    }

    private void create(final PaymentMethodBuilder paymentMethodBuilder, final PaymentMethodResponseCallback callback) {
        mHttpClient.post(versionedPath(paymentMethodBuilder.getApiPath()),
                paymentMethodBuilder.build(), new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        try {
                            PaymentMethod paymentMethod =
                                    PaymentMethod.parsePaymentMethod(responseBody,
                                            paymentMethodBuilder.getResponsePaymentMethodType());
                            postPaymentMethod(paymentMethod, callback);
                        } catch (JSONException e) {
                            postExceptionToListeners(e);
                        }
                    }

                    @Override
                    public void failure(Exception exception) {
                        postExceptionToListeners(exception);
                    }
                });
    }

    /**
     * Tokenizes a {@link PaymentMethod} and returns a nonce in
     * {@link Braintree.PaymentMethodNonceListener#onPaymentMethodNonce(String)}.
     *
     * Tokenization functions like creating a {@link PaymentMethod}, but defers validation until a
     * server library attempts to use the {@link PaymentMethod}. Use
     * {@link #tokenize(PaymentMethodBuilder)} to handle validation errors on the server instead of
     * on device.
     *
     * If a network or server error occurs, {@link Braintree.ErrorListener#onUnrecoverableError(Throwable)}
     * will be called with the {@link BraintreeException} that occurred.
     *
     * @param paymentMethodBuilder {@link PaymentMethodBuilder} for the {@link PaymentMethod}
     *        to be created.
     */
    public void tokenize(PaymentMethodBuilder paymentMethodBuilder) {
        tokenize(paymentMethodBuilder, null);
    }

    private void tokenize(PaymentMethodBuilder paymentMethodBuilder, PaymentMethodResponseCallback callback) {
        paymentMethodBuilder.validate(false);
        create(paymentMethodBuilder, callback);
    }

    /**
     * Sends analytics event to the Braintree analytics service.
     *
     * @param eventFragment Event to be sent.
     */
    public void sendAnalyticsEvent(String eventFragment) {
        if (mConfiguration.isAnalyticsEnabled()) {
            mHttpClient.post(mConfiguration.getAnalytics().getUrl(),
                    AnalyticsRequest.newRequest(mContext, analyticsPrefix() + "." + eventFragment,
                            getIntegrationType()),
                    null);
        }
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
        return collectDeviceData(activity, environment.getMerchantId(),
                environment.getCollectorUrl());
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
        String deviceData;
        try {
            mBraintreeData = new BraintreeData(activity, merchantId, collectorUrl);
            deviceData = ((BraintreeData) mBraintreeData).collectDeviceData();
        } catch (NoClassDefFoundError e) {
            deviceData = "{\"correlation_id\":\"" + PayPalConfiguration
                    .getClientMetadataId(activity) + "\"}";
        }

        return deviceData;
    }

    private void addPaymentMethodToCache(PaymentMethod paymentMethod) {
        if (mCachedPaymentMethods == null) {
            mCachedPaymentMethods = new ArrayList<PaymentMethod>();
        }
        mCachedPaymentMethods.add(0, paymentMethod);
    }

    private void postPaymentMethod(PaymentMethod paymentMethod, PaymentMethodResponseCallback callback) {
        if (callback != null) {
            callback.success(paymentMethod);
        } else {
            addPaymentMethodToCache(paymentMethod);

            postCreatedMethodToListeners(paymentMethod);
            postCreatedNonceToListeners(paymentMethod.getNonce());
        }
    }

    private void postPaymentMethodsToListeners(List<PaymentMethod> paymentMethods) {
        final List<PaymentMethod> paymentMethodsSafe = Collections.unmodifiableList(paymentMethods);
        postOrQueueCallback(new ListenerCallback() {
            @Override
            public void execute() {
                for (final PaymentMethodsUpdatedListener listener : mUpdatedListeners) {
                    listener.onPaymentMethodsUpdated(paymentMethodsSafe);
                }
            }

            @Override
            public boolean hasListeners() {
                return !mUpdatedListeners.isEmpty();
            }
        });
    }

    private void postCreatedMethodToListeners(final PaymentMethod paymentMethod) {
        postOrQueueCallback(new ListenerCallback() {
            @Override
            public void execute() {
                for (final PaymentMethodCreatedListener listener : mCreatedListeners) {
                    listener.onPaymentMethodCreated(paymentMethod);
                }
            }

            @Override
            public boolean hasListeners() {
                return !mCreatedListeners.isEmpty();
            }
        });
    }

    private void postCreatedNonceToListeners(final String nonce) {
        postOrQueueCallback(new ListenerCallback() {
            @Override
            public void execute() {
                for (final PaymentMethodNonceListener listener : mNonceListeners) {
                    listener.onPaymentMethodNonce(nonce);
                }
            }

            @Override
            public boolean hasListeners() {
                return !mNonceListeners.isEmpty();
            }
        });
    }

    protected void postExceptionToListeners(final Exception exception) {
        if (exception instanceof ErrorWithResponse) {
            postOrQueueCallback(new ListenerCallback() {
                @Override
                public void execute() {
                    for (final ErrorListener listener : mErrorListeners) {
                        listener.onRecoverableError((ErrorWithResponse) exception);
                    }
                }

                @Override
                public boolean hasListeners() {
                    return !mErrorListeners.isEmpty();
                }
            });
        } else {
            postOrQueueCallback(new ListenerCallback() {
                @Override
                public void execute() {
                    for (final ErrorListener listener : mErrorListeners) {
                        listener.onUnrecoverableError(exception);
                    }
                }

                @Override
                public boolean hasListeners() {
                    return !mErrorListeners.isEmpty();
                }
            });
        }
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
    public boolean hasCachedCards() {
        return mCachedPaymentMethods != null;
    }

    /**
     * @return Unmodifiable list of previously retrieved {@link com.braintreepayments.api.models.PaymentMethod}.
     * If no attempts have been made, an empty list is returned.
     */
    public List<PaymentMethod> getCachedPaymentMethods() {
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
    public void lockListeners() {
        mListenersLocked = true;
    }

    /**
     * Restore control flow to locked listeners. If the listeners have not been locked yet,
     * this acts as a noop.
     * @see #lockListeners()
     */
    public void unlockListeners() {
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

    protected interface ListenerCallback {
        void execute();
        boolean hasListeners();
    }

    private String versionedPath(String path) {
        return "/v1/" + path;
    }
}
