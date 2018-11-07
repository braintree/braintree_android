package com.braintreepayments.api;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.GoogleApiClientException;
import com.braintreepayments.api.exceptions.GoogleApiClientException.ErrorType;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.AmericanExpressListener;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeListener;
import com.braintreepayments.api.interfaces.BraintreePaymentResultListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceDeletedListener;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.interfaces.QueuedCallback;
import com.braintreepayments.api.interfaces.UnionPayListener;
import com.braintreepayments.api.internal.AnalyticsDatabase;
import com.braintreepayments.api.internal.AnalyticsEvent;
import com.braintreepayments.api.internal.AnalyticsIntentService;
import com.braintreepayments.api.internal.AnalyticsSender;
import com.braintreepayments.api.internal.BraintreeApiHttpClient;
import com.braintreepayments.api.internal.BraintreeGraphQLHttpClient;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.internal.IntegrationType;
import com.braintreepayments.api.internal.UUIDHelper;
import com.braintreepayments.api.models.AmericanExpressRewardsBalance;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreePaymentResult;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.TokenizationKey;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.browserswitch.BrowserSwitchFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

/**
 * Core Braintree class that handles network requests and managing callbacks.
 */
public class BraintreeFragment extends BrowserSwitchFragment {

    public static final String TAG = "com.braintreepayments.api.BraintreeFragment";

    private static final String EXTRA_AUTHORIZATION_TOKEN = "com.braintreepayments.api.EXTRA_AUTHORIZATION_TOKEN";
    private static final String EXTRA_INTEGRATION_TYPE = "com.braintreepayments.api.EXTRA_INTEGRATION_TYPE";
    private static final String EXTRA_SESSION_ID = "com.braintreepayments.api.EXTRA_SESSION_ID";

    @VisibleForTesting
    static final String EXTRA_CONFIGURATION = "com.braintreepayments.api.EXTRA_CONFIGURATION";
    @VisibleForTesting
    static final String EXTRA_CACHED_PAYMENT_METHOD_NONCES = "com.braintreepayments.api.EXTRA_CACHED_PAYMENT_METHOD_NONCES";
    @VisibleForTesting
    static final String EXTRA_FETCHED_PAYMENT_METHOD_NONCES = "com.braintreepayments.api.EXTRA_FETCHED_PAYMENT_METHOD_NONCES";

    @VisibleForTesting
    protected BraintreeHttpClient mHttpClient;
    @VisibleForTesting
    protected BraintreeApiHttpClient mBraintreeApiClient;
    @VisibleForTesting
    protected BraintreeGraphQLHttpClient mGraphQLHttpClient;
    @VisibleForTesting
    protected GoogleApiClient mGoogleApiClient;

    private CrashReporter mCrashReporter;
    private Authorization mAuthorization;
    private Configuration mConfiguration;
    private final Queue<QueuedCallback> mCallbackQueue = new ArrayDeque<>();
    private final List<PaymentMethodNonce> mCachedPaymentMethodNonces = new ArrayList<>();
    private boolean mHasFetchedPaymentMethodNonces = false;
    private boolean mNewActivityNeedsConfiguration;
    private int mConfigurationRequestAttempts = 0;
    private String mIntegrationType;
    private String mSessionId;
    private AnalyticsDatabase mAnalyticsDatabase;

    private ConfigurationListener mConfigurationListener;
    private BraintreeResponseListener<Exception> mConfigurationErrorListener;
    private BraintreeCancelListener mCancelListener;
    private PaymentMethodNoncesUpdatedListener mPaymentMethodNoncesUpdatedListener;
    private PaymentMethodNonceCreatedListener mPaymentMethodNonceCreatedListener;
    private PaymentMethodNonceDeletedListener mPaymentMethodNonceDeletedListener;
    private BraintreeErrorListener mErrorListener;
    private BraintreePaymentResultListener mBraintreePaymentResultListener;
    private UnionPayListener mUnionPayListener;
    private AmericanExpressListener mAmericanExpressListener;

    public BraintreeFragment() {}

    /**
     * Create a new instance of {@link BraintreeFragment} using the client token and add it to the
     * {@link AppCompatActivity}'s {@link FragmentManager}.
     *
     * @param activity The {@link AppCompatActivity} to add the {@link BraintreeFragment} to.
     * @param authorization The tokenization key or client token to use.
     * @return {@link BraintreeFragment}
     * @throws InvalidArgumentException If the tokenization key or client token is not valid or cannot be
     *         parsed.
     */
    public static BraintreeFragment newInstance(AppCompatActivity activity, String authorization)
            throws InvalidArgumentException {
        if (activity == null) {
            throw new InvalidArgumentException("Activity is null");
        }

        FragmentManager fm = activity.getSupportFragmentManager();
        BraintreeFragment braintreeFragment = (BraintreeFragment) fm.findFragmentByTag(TAG);
        if (braintreeFragment == null) {
            braintreeFragment = new BraintreeFragment();
            Bundle bundle = new Bundle();

            try {
                Authorization auth = Authorization.fromString(authorization);
                bundle.putParcelable(EXTRA_AUTHORIZATION_TOKEN, auth);
            } catch (InvalidArgumentException e) {
                throw new InvalidArgumentException("Tokenization Key or client token was invalid.");
            }

            bundle.putString(EXTRA_SESSION_ID, UUIDHelper.getFormattedUUID());
            bundle.putString(EXTRA_INTEGRATION_TYPE, IntegrationType.get(activity));
            braintreeFragment.setArguments(bundle);

            try {
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    try {
                        fm.beginTransaction().add(braintreeFragment, TAG).commitNow();
                    } catch (IllegalStateException | NullPointerException e) {
                        fm.beginTransaction().add(braintreeFragment, TAG).commit();
                        try {
                            fm.executePendingTransactions();
                        } catch (IllegalStateException ignored) {}
                    }
                } else {
                    fm.beginTransaction().add(braintreeFragment, TAG).commit();
                    try {
                        fm.executePendingTransactions();
                    } catch (IllegalStateException ignored) {}
                }
            } catch (IllegalStateException e) {
                throw new InvalidArgumentException(e.getMessage());
            }
        }

        braintreeFragment.mContext = activity.getApplicationContext();

        return braintreeFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (mContext == null) {
            mContext = getActivity().getApplicationContext();
        }

        mNewActivityNeedsConfiguration = false;
        mCrashReporter = CrashReporter.setup(this);
        mSessionId = getArguments().getString(EXTRA_SESSION_ID);
        mIntegrationType = getArguments().getString(EXTRA_INTEGRATION_TYPE);
        mAuthorization = getArguments().getParcelable(EXTRA_AUTHORIZATION_TOKEN);
        mAnalyticsDatabase = AnalyticsDatabase.getInstance(getApplicationContext());

        if (mHttpClient == null) {
            mHttpClient = new BraintreeHttpClient(mAuthorization);
        }

        if (savedInstanceState != null) {
            List<PaymentMethodNonce> paymentMethodNonces =
                    savedInstanceState.getParcelableArrayList(EXTRA_CACHED_PAYMENT_METHOD_NONCES);
            if (paymentMethodNonces != null) {
                mCachedPaymentMethodNonces.addAll(paymentMethodNonces);
            }

            mHasFetchedPaymentMethodNonces = savedInstanceState.getBoolean(EXTRA_FETCHED_PAYMENT_METHOD_NONCES);
            try {
                setConfiguration(Configuration.fromJson(savedInstanceState.getString(EXTRA_CONFIGURATION)));
            } catch (JSONException ignored) {}
        } else {
            if (mAuthorization instanceof TokenizationKey) {
                sendAnalyticsEvent("started.client-key");
            } else {
                sendAnalyticsEvent("started.client-token");
            }
        }

        fetchConfiguration();
    }

    @TargetApi(VERSION_CODES.M)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttach(getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mNewActivityNeedsConfiguration = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof BraintreeListener) {
            addListener((BraintreeListener) getActivity());

            if (mNewActivityNeedsConfiguration && getConfiguration() != null) {
                mNewActivityNeedsConfiguration = false;
                postConfigurationCallback();
            }
        }

        flushCallbacks();

        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected() &&
                !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (getActivity() instanceof BraintreeListener) {
            removeListener((BraintreeListener) getActivity());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(EXTRA_CACHED_PAYMENT_METHOD_NONCES,
                (ArrayList<? extends Parcelable>) mCachedPaymentMethodNonces);
        outState.putBoolean(EXTRA_FETCHED_PAYMENT_METHOD_NONCES, mHasFetchedPaymentMethodNonces);

        if (mConfiguration != null) {
            outState.putString(EXTRA_CONFIGURATION, mConfiguration.toJson());
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        flushAnalyticsEvents();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCrashReporter.tearDown();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (!isAdded()) {
            postCallback(new BraintreeException("BraintreeFragment is not attached to an Activity. Please ensure it " +
                    "is attached and try again."));
        } else {
            super.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public String getReturnUrlScheme() {
        return getApplicationContext().getPackageName().toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree";
    }

    @Override
    public void onBrowserSwitchResult(int requestCode, BrowserSwitchResult browserSwitchResult, @Nullable Uri uri) {
        String type = "";
        switch (requestCode) {
            case BraintreeRequestCodes.PAYPAL:
                type = "paypal";
                break;
            case BraintreeRequestCodes.THREE_D_SECURE:
                type =  "three-d-secure";
                break;
            case BraintreeRequestCodes.LOCAL_PAYMENT:
                type = "local-payment";
                break;
        }

        int resultCode = AppCompatActivity.RESULT_FIRST_USER;
        if (browserSwitchResult == BrowserSwitchResult.OK) {
            resultCode = AppCompatActivity.RESULT_OK;
            sendAnalyticsEvent(type + ".browser-switch.succeeded");
        } else if (browserSwitchResult == BrowserSwitchResult.CANCELED) {
            resultCode = AppCompatActivity.RESULT_CANCELED;
            sendAnalyticsEvent(type + ".browser-switch.canceled");
        } else if (browserSwitchResult == BrowserSwitchResult.ERROR) {
            if (browserSwitchResult.getErrorMessage().startsWith("No installed activities")) {
                sendAnalyticsEvent(type + ".browser-switch.failed.no-browser-installed");
            } else {
                sendAnalyticsEvent(type + ".browser-switch.failed.not-setup");
            }
        }

        onActivityResult(requestCode, resultCode, new Intent().setData(uri));
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BraintreeRequestCodes.PAYPAL:
                PayPal.onActivityResult(this, resultCode, data);
                break;
            case BraintreeRequestCodes.THREE_D_SECURE:
                ThreeDSecure.onActivityResult(this, resultCode, data);
                break;
            case BraintreeRequestCodes.VENMO:
                Venmo.onActivityResult(this, resultCode, data);
                break;
            case BraintreeRequestCodes.VISA_CHECKOUT:
                VisaCheckoutFacade.onActivityResult(this, resultCode, data);
                break;
            case BraintreeRequestCodes.LOCAL_PAYMENT:
                LocalPayment.onActivityResult(this, resultCode, data);
                break;
            case BraintreeRequestCodes.GOOGLE_PAYMENT:
                GooglePayment.onActivityResult(this, resultCode, data);
                break;
        }

        if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            postCancelCallback(requestCode);
        }
    }

    /**
     * Adds a listener.
     *
     * @param listener the listener to add.
     */
    public <T extends BraintreeListener> void addListener(T listener) {
        if (listener instanceof ConfigurationListener) {
            mConfigurationListener = (ConfigurationListener) listener;
        }

        if (listener instanceof BraintreeCancelListener) {
            mCancelListener = (BraintreeCancelListener) listener;
        }

        if (listener instanceof PaymentMethodNoncesUpdatedListener) {
            mPaymentMethodNoncesUpdatedListener = (PaymentMethodNoncesUpdatedListener) listener;
        }

        if (listener instanceof PaymentMethodNonceCreatedListener) {
            mPaymentMethodNonceCreatedListener = (PaymentMethodNonceCreatedListener) listener;
        }

        if (listener instanceof PaymentMethodNonceDeletedListener) {
            mPaymentMethodNonceDeletedListener = (PaymentMethodNonceDeletedListener) listener;
        }

        if (listener instanceof BraintreePaymentResultListener) {
            mBraintreePaymentResultListener = (BraintreePaymentResultListener) listener;
        }

        if (listener instanceof BraintreeErrorListener) {
            mErrorListener = (BraintreeErrorListener) listener;
        }

        if (listener instanceof UnionPayListener) {
            mUnionPayListener = (UnionPayListener) listener;
        }

        if (listener instanceof AmericanExpressListener) {
            mAmericanExpressListener = (AmericanExpressListener) listener;
        }

        flushCallbacks();
    }

    /**
     * Removes a previously added listener.
     *
     * @param listener the listener to remove.
     */
    public <T extends BraintreeListener> void removeListener(T listener) {
        if (listener instanceof ConfigurationListener) {
            mConfigurationListener = null;
        }

        if (listener instanceof BraintreeCancelListener) {
            mCancelListener = null;
        }

        if (listener instanceof PaymentMethodNoncesUpdatedListener) {
            mPaymentMethodNoncesUpdatedListener = null;
        }

        if (listener instanceof PaymentMethodNonceCreatedListener) {
            mPaymentMethodNonceCreatedListener = null;
        }

        if (listener instanceof PaymentMethodNonceDeletedListener) {
            mPaymentMethodNonceDeletedListener = null;
        }

        if (listener instanceof BraintreePaymentResultListener) {
            mBraintreePaymentResultListener = null;
        }

        if (listener instanceof BraintreeErrorListener) {
            mErrorListener = null;
        }

        if (listener instanceof UnionPayListener) {
            mUnionPayListener = null;
        }

        if (listener instanceof AmericanExpressListener) {
            mAmericanExpressListener = null;
        }
    }

    /**
     * @return {@link ArrayList<BraintreeListener>} of the currently attached listeners
     */
    public List<BraintreeListener> getListeners() {
        List<BraintreeListener> listeners = new ArrayList<>();

        if (mConfigurationListener != null) {
            listeners.add(mConfigurationListener);
        }

        if (mCancelListener != null) {
            listeners.add(mCancelListener);
        }

        if (mPaymentMethodNoncesUpdatedListener != null) {
            listeners.add(mPaymentMethodNoncesUpdatedListener);
        }

        if (mPaymentMethodNonceCreatedListener != null) {
            listeners.add(mPaymentMethodNonceCreatedListener);
        }

        if (mPaymentMethodNonceDeletedListener != null) {
            listeners.add(mPaymentMethodNonceDeletedListener);
        }

        if (mBraintreePaymentResultListener != null) {
            listeners.add(mBraintreePaymentResultListener);
        }

        if (mErrorListener != null) {
            listeners.add(mErrorListener);
        }

        if (mUnionPayListener != null) {
            listeners.add(mUnionPayListener);
        }

        if (mAmericanExpressListener != null) {
            listeners.add(mAmericanExpressListener);
        }

        return listeners;
    }

    /**
     * A boolean indicating whether the current customer's payment methods have been fetched with
     * {@link PaymentMethod#getPaymentMethodNonces(BraintreeFragment)} yet.
     *
     * @return {@code true} if the current customer's payment methods have been fetched, {@code false} otherwise.
     */
    public boolean hasFetchedPaymentMethodNonces() {
        return mHasFetchedPaymentMethodNonces;
    }

    /**
     * After fetching the current customer's {@link PaymentMethodNonce}s using
     * {@link PaymentMethod#getPaymentMethodNonces(BraintreeFragment)}, the {@link PaymentMethodNonce}s will be cached
     * for the life time of this instance of {@link BraintreeFragment} and can be returned without additional network
     * requests using this method.
     *
     * @return {@link List<PaymentMethodNonce>}s for the current customer.
     */
    public List<PaymentMethodNonce> getCachedPaymentMethodNonces() {
        return Collections.unmodifiableList(mCachedPaymentMethodNonces);
    }

    public void sendAnalyticsEvent(final String eventFragment) {
        final AnalyticsEvent request = new AnalyticsEvent(mContext, getSessionId(), mIntegrationType, eventFragment);
        waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (configuration.getAnalytics().isEnabled()) {
                    mAnalyticsDatabase.addEvent(request);
                }
            }
        });
    }

    private void flushAnalyticsEvents() {
        if (getConfiguration() != null && getConfiguration().toJson() != null &&
                getConfiguration().getAnalytics().isEnabled()) {
            Intent intent = new Intent(mContext, AnalyticsIntentService.class)
                    .putExtra(AnalyticsIntentService.EXTRA_AUTHORIZATION, getAuthorization().toString())
                    .putExtra(AnalyticsIntentService.EXTRA_CONFIGURATION, getConfiguration().toJson());

            try {
                getApplicationContext().startService(intent);
            } catch (RuntimeException e) {
                AnalyticsSender.send(getApplicationContext(), mAuthorization, getHttpClient(),
                        getConfiguration().getAnalytics().getUrl(), false);
            }
        }
    }

    protected void postConfigurationCallback() {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mConfigurationListener != null;
            }

            @Override
            public void run() {
                mConfigurationListener.onConfigurationFetched(getConfiguration());
            }
        });
    }

    protected void postCancelCallback(final int requestCode) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mCancelListener != null;
            }

            @Override
            public void run() {
                mCancelListener.onCancel(requestCode);
            }
        });
    }

    protected void postCallback(final PaymentMethodNonce paymentMethodNonce) {
        mCachedPaymentMethodNonces.add(0, paymentMethodNonce);

        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mPaymentMethodNonceCreatedListener != null;
            }

            @Override
            public void run() {
                mPaymentMethodNonceCreatedListener.onPaymentMethodNonceCreated(paymentMethodNonce);
            }
        });
    }

    protected void postCallback(final UnionPayCapabilities capabilities) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mUnionPayListener != null;
            }

            @Override
            public void run() {
                mUnionPayListener.onCapabilitiesFetched(capabilities);
            }
        });
    }

    protected void postUnionPayCallback(final String enrollmentId, final boolean smsCodeRequired) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mUnionPayListener != null;
            }

            @Override
            public void run() {
                mUnionPayListener.onSmsCodeSent(enrollmentId, smsCodeRequired);
            }
        });
    }

    protected void postCallback(final BraintreePaymentResult result) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mBraintreePaymentResultListener != null;
            }

            @Override
            public void run() {
                mBraintreePaymentResultListener.onBraintreePaymentResult(result);
            }
        });
    }

    protected void postAmericanExpressCallback(final AmericanExpressRewardsBalance rewardsBalance) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mAmericanExpressListener != null;
            }

            @Override
            public void run() {
                mAmericanExpressListener.onRewardsBalanceFetched(rewardsBalance);
            }
        });
    }

    protected void postCallback(final List<PaymentMethodNonce> paymentMethodNonceList) {
        mCachedPaymentMethodNonces.clear();
        mCachedPaymentMethodNonces.addAll(paymentMethodNonceList);
        mHasFetchedPaymentMethodNonces = true;
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mPaymentMethodNoncesUpdatedListener != null;
            }

            @Override
            public void run() {
                mPaymentMethodNoncesUpdatedListener.onPaymentMethodNoncesUpdated(paymentMethodNonceList);
            }
        });
    }

    protected void postPaymentMethodDeletedCallback(final PaymentMethodNonce paymentMethodNonce) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mPaymentMethodNonceDeletedListener != null;
            }

            @Override
            public void run() {
                mPaymentMethodNonceDeletedListener.onPaymentMethodNonceDeleted(paymentMethodNonce);
            }
        });
    }

    protected void postCallback(final Exception error) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mErrorListener != null;
            }

            @Override
            public void run() {
                mErrorListener.onError(error);
            }
        });
    }

    @VisibleForTesting
    protected void postOrQueueCallback(QueuedCallback callback) {
        if (!callback.shouldRun()) {
            mCallbackQueue.add(callback);
        } else {
            callback.run();
        }
    }

    @VisibleForTesting
    protected void flushCallbacks() {
        synchronized (mCallbackQueue) {
            Queue<QueuedCallback> queue = new ArrayDeque<>(mCallbackQueue);
            for (QueuedCallback callback : queue) {
                if (callback.shouldRun()) {
                    callback.run();
                    mCallbackQueue.remove(callback);
                }
            }
        }
    }

    @VisibleForTesting
    protected void fetchConfiguration() {
        if (getConfiguration() != null || ConfigurationManager.isFetchingConfiguration() || mAuthorization == null ||
                mHttpClient == null) {
            return;
        }

        if (mConfigurationRequestAttempts >= 3) {
            postCallback(new ConfigurationException("Configuration retry limit has been exceeded. Create a new " +
                    "BraintreeFragment and try again."));
            return;
        }

        mConfigurationRequestAttempts++;

        ConfigurationManager.getConfiguration(this, new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                setConfiguration(configuration);
                postConfigurationCallback();
                flushCallbacks();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(final Exception e) {
                final ConfigurationException exception =
                        new ConfigurationException("Request for configuration has failed: " + e.getMessage() + ". " +
                                "Future requests will retry up to 3 times", e);
                postCallback(exception);
                postOrQueueCallback(new QueuedCallback() {
                    @Override
                    public boolean shouldRun() {
                        return mConfigurationErrorListener != null;
                    }

                    @Override
                    public void run() {
                        mConfigurationErrorListener.onResponse(exception);
                    }
                });
                flushCallbacks();
            }
        });
    }

    protected void setConfigurationErrorListener(BraintreeResponseListener<Exception> listener) {
        mConfigurationErrorListener = listener;
    }

    protected void waitForConfiguration(final ConfigurationListener listener) {
        fetchConfiguration();

        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return getConfiguration() != null && isAdded();
            }

            @Override
            public void run() {
                listener.onConfigurationFetched(getConfiguration());
            }
        });
    }

    protected Authorization getAuthorization() {
        return mAuthorization;
    }

    protected Context getApplicationContext() {
        return mContext;
    }

    protected Configuration getConfiguration() {
        return mConfiguration;
    }

    protected void setConfiguration(Configuration configuration) {
        mConfiguration = configuration;
        getHttpClient().setBaseUrl(configuration.getClientApiUrl());

        if (configuration.getGraphQL().isEnabled()) {
            mGraphQLHttpClient = new BraintreeGraphQLHttpClient(configuration.getGraphQL().getUrl(),
                    mAuthorization.getBearer());
        }
    }

    protected BraintreeHttpClient getHttpClient() {
        return mHttpClient;
    }

    @Nullable
    protected BraintreeApiHttpClient getBraintreeApiHttpClient() {
        if (mBraintreeApiClient == null && getConfiguration() != null &&
                getConfiguration().getBraintreeApiConfiguration().isEnabled()) {
            mBraintreeApiClient = new BraintreeApiHttpClient(getConfiguration().getBraintreeApiConfiguration().getUrl(),
                    getConfiguration().getBraintreeApiConfiguration().getAccessToken());
        }

        return mBraintreeApiClient;
    }

    @Nullable
    protected BraintreeGraphQLHttpClient getGraphQLHttpClient() {
        return mGraphQLHttpClient;
    }

    protected String getSessionId() {
        return mSessionId;
    }

    protected String getIntegrationType() {
        return mIntegrationType;
    }

    /**
     * Obtain an instance of a {@link GoogleApiClient} that is connected or connecting to be used
     * for Android Pay. This instance will be automatically disconnected in
     * {@link BraintreeFragment#onStop()} and automatically connected in
     * {@link BraintreeFragment#onResume()}.
     * <p/>
     * Connection failed and connection suspended errors will be sent to
     * {@link BraintreeErrorListener#onError(Exception)}.
     *
     * @param listener {@link BraintreeResponseListener<GoogleApiClient>} to receive the
     *                 {@link GoogleApiClient} in
     *                 {@link BraintreeResponseListener<GoogleApiClient>#onResponse(GoogleApiClient)}.
     */
    public void getGoogleApiClient(final BraintreeResponseListener<GoogleApiClient> listener) {
        waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                GoogleApiClient googleApiClient = getGoogleApiClient();
                if (googleApiClient != null) {
                    listener.onResponse(googleApiClient);
                }
            }
        });
    }

    protected GoogleApiClient getGoogleApiClient() {
        if (getActivity() == null) {
            postCallback(new GoogleApiClientException(ErrorType.NotAttachedToActivity, 1));
            return null;
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                            .setEnvironment(GooglePayment.getEnvironment(getConfiguration().getGooglePayment()))
                            .setTheme(WalletConstants.THEME_LIGHT)
                            .build())
                    .build();
        }

        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.registerConnectionCallbacks(new ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {}

                @Override
                public void onConnectionSuspended(int i) {
                    postCallback(new GoogleApiClientException(ErrorType.ConnectionSuspended, i));
                }
            });

            mGoogleApiClient.registerConnectionFailedListener(new OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    postCallback(new GoogleApiClientException(ErrorType.ConnectionFailed, connectionResult.getErrorCode()));
                }
            });

            mGoogleApiClient.connect();
        }

        return mGoogleApiClient;
    }
}
