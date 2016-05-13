package com.braintreepayments.api;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.GoogleApiClientException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.interfaces.QueuedCallback;
import com.braintreepayments.api.interfaces.UnionPayListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.TokenizationKey;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * Core Braintree class that handles network requests and managing callbacks.
 */
public class BraintreeFragment extends Fragment {

    public static final String TAG = "com.braintreepayments.api.BraintreeFragment";

    private static final String EXTRA_AUTHORIZATION_TOKEN = "com.braintreepayments.api.EXTRA_AUTHORIZATION_TOKEN";
    private static final String EXTRA_INTEGRATION_TYPE = "com.braintreepayments.api.EXTRA_INTEGRATION_TYPE";
    private static final String EXTRA_SESSION_ID = "com.braintreepayments.api.EXTRA_SESSION_ID";

    @VisibleForTesting
    static final String EXTRA_BROWSER_SWITCHING = "com.braintreepayments.api.EXTRA_BROWSER_SWITCHING";
    @VisibleForTesting
    static final String EXTRA_CACHED_PAYMENT_METHOD_NONCES =
            "com.braintreepayments.api.EXTRA_CACHED_PAYMENT_METHOD_NONCES";
    @VisibleForTesting
    static final String EXTRA_FETCHED_PAYMENT_METHOD_NONCES =
            "com.braintreepayments.api.EXTRA_FETCHED_PAYMENT_METHOD_NONCES";

    @VisibleForTesting
    protected String mIntegrationType;
    @VisibleForTesting
    protected BraintreeHttpClient mHttpClient;
    @VisibleForTesting
    protected GoogleApiClient mGoogleApiClient;
    @VisibleForTesting
    protected Configuration mConfiguration;

    private CrashReporter mCrashReporter;
    private Context mContext;
    private Authorization mAuthorization;
    private final Queue<QueuedCallback> mCallbackQueue = new ArrayDeque<>();
    private final List<PaymentMethodNonce> mCachedPaymentMethodNonces = new ArrayList<>();
    private boolean mHasFetchedPaymentMethodNonces = false;
    private boolean mIsBrowserSwitching = false;
    private int mConfigurationRequestAttempts = 0;
    private String mSessionId;

    private ConfigurationListener mConfigurationListener;
    private BraintreeResponseListener<Exception> mConfigurationErrorListener;
    private BraintreeCancelListener mCancelListener;
    private PaymentMethodNoncesUpdatedListener mPaymentMethodNoncesUpdatedListener;
    private PaymentMethodNonceCreatedListener mPaymentMethodNonceCreatedListener;
    private BraintreeErrorListener mErrorListener;
    private UnionPayListener mUnionPayListener;

    public BraintreeFragment() {}

    /**
     * Create a new instance of {@link BraintreeFragment} using the client token and add it to the
     * {@link Activity}'s {@link FragmentManager}.
     *
     * @param activity The {@link Activity} to add the {@link Fragment} to.
     * @param authorization The tokenization key or client token to use.
     * @return {@link BraintreeFragment}
     * @throws InvalidArgumentException If the tokenization key or client token is not valid or cannot be
     *         parsed.
     */
    public static BraintreeFragment newInstance(Activity activity, String authorization)
            throws InvalidArgumentException {
        FragmentManager fm = activity.getFragmentManager();

        String integrationType = "custom";
        try {
            if (Class.forName("com.braintreepayments.api.BraintreePaymentActivity")
                    .isInstance(activity)) {
                integrationType = "dropin";
            }
        } catch (ClassNotFoundException ignored) {}

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

            bundle.putString(EXTRA_INTEGRATION_TYPE, integrationType);
            braintreeFragment.setArguments(bundle);
            fm.beginTransaction().add(braintreeFragment, TAG).commit();
        }

        braintreeFragment.mContext = activity.getApplicationContext();

        return braintreeFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mContext = getActivity().getApplicationContext();
        mCrashReporter = CrashReporter.setup(mContext);
        mIntegrationType = getArguments().getString(EXTRA_INTEGRATION_TYPE);
        mAuthorization = getArguments().getParcelable(EXTRA_AUTHORIZATION_TOKEN);

        if (mHttpClient == null) {
            mHttpClient = new BraintreeHttpClient(mAuthorization);
        }

        if (getConfiguration() == null) {
            fetchConfiguration();
        }

        if (savedInstanceState != null) {
            List<PaymentMethodNonce> paymentMethodNonces =
                    savedInstanceState.getParcelableArrayList(EXTRA_CACHED_PAYMENT_METHOD_NONCES);
            if (paymentMethodNonces != null) {
                mCachedPaymentMethodNonces.addAll(paymentMethodNonces);
            }

            mHasFetchedPaymentMethodNonces = savedInstanceState.getBoolean(EXTRA_FETCHED_PAYMENT_METHOD_NONCES);
            mIsBrowserSwitching = savedInstanceState.getBoolean(EXTRA_BROWSER_SWITCHING);
            mSessionId = savedInstanceState.getString(EXTRA_SESSION_ID);
        } else {
            mSessionId = DeviceMetadata.getFormattedUUID();
            if (mAuthorization instanceof TokenizationKey) {
                sendAnalyticsEvent("started.client-key");
            } else {
                sendAnalyticsEvent("started.client-token");
            }
        }

        mCrashReporter.sendPreviousCrashes(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof BraintreeListener) {
            addListener((BraintreeListener) getActivity());
        }

        flushCallbacks();

        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected() &&
                !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }

        if (mIsBrowserSwitching) {
            int resultCode = Activity.RESULT_CANCELED;
            if (BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse != null) {
                resultCode = Activity.RESULT_OK;
            }

            onActivityResult(PayPal.PAYPAL_REQUEST_CODE, resultCode,
                    BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse);

            BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse = null;
            mIsBrowserSwitching = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        AnalyticsManager.flushEvents(this);

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
        outState.putBoolean(EXTRA_BROWSER_SWITCHING, mIsBrowserSwitching);
        outState.putString(EXTRA_SESSION_ID, mSessionId);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCrashReporter.tearDown();
    }

    @Override
    public void startActivity(Intent intent) {
        if (intent.hasExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH)) {
            BraintreeBrowserSwitchActivity.sLastBrowserSwitchResponse = null;
            mIsBrowserSwitching = true;
            getApplicationContext().startActivity(intent);
        } else {
            super.startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PayPal.PAYPAL_REQUEST_CODE:
                PayPal.onActivityResult(this, resultCode, data);
                break;
            case ThreeDSecure.THREE_D_SECURE_REQUEST_CODE:
                ThreeDSecure.onActivityResult(this, resultCode, data);
                break;
            case Venmo.VENMO_REQUEST_CODE:
                Venmo.onActivityResult(this, resultCode, data);
                break;
        }

        if (resultCode == Activity.RESULT_CANCELED) {
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

        if (listener instanceof BraintreeErrorListener) {
            mErrorListener = (BraintreeErrorListener) listener;
        }

        if (listener instanceof UnionPayListener) {
            mUnionPayListener = (UnionPayListener) listener;
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

        if (listener instanceof BraintreeErrorListener) {
            mErrorListener = null;
        }
    }

    protected void sendAnalyticsEvent(final String eventFragment) {
        AnalyticsManager.sendRequest(this, mIntegrationType, eventFragment);
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

    protected void postUnionPayCallback(final String enrollmentId) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mUnionPayListener != null;
            }

            @Override
            public void run() {
                mUnionPayListener.onSmsCodeSent(enrollmentId);
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
        Queue<QueuedCallback> queue = new ArrayDeque<>();
        queue.addAll(mCallbackQueue);
        for (QueuedCallback callback : queue) {
            if (callback.shouldRun()) {
                callback.run();
                mCallbackQueue.remove(callback);
            }
        }
    }

    @VisibleForTesting
    protected void fetchConfiguration() {
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
                flushCallbacks();
            }
        }, new BraintreeResponseListener<Exception>() {
            @Override
            public void onResponse(final Exception e) {
                final ConfigurationException exception =
                        new ConfigurationException("Request for configuration has failed: " + e.getMessage() + ". " +
                                "Future requests will retry up to 3 times");
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
        if (getConfiguration() == null && !ConfigurationManager.isFetchingConfiguration() && mAuthorization != null) {
            fetchConfiguration();
        }

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
    }

    protected BraintreeHttpClient getHttpClient() {
        return mHttpClient;
    }

    protected String getSessionId() {
        return mSessionId;
    }

    protected boolean hasFetchedPaymentMethodNonces() {
        return mHasFetchedPaymentMethodNonces;
    }

    protected List<PaymentMethodNonce> getCachedPaymentMethodNonces() {
        return Collections.unmodifiableList(mCachedPaymentMethodNonces);
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
                listener.onResponse(getGoogleApiClient());
            }
        });
    }

    protected GoogleApiClient getGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                            .setEnvironment(AndroidPay.getEnvironment(getConfiguration().getAndroidPay()))
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
                    postCallback(new GoogleApiClientException("Connection suspended: " + i));
                }
            });

            mGoogleApiClient.registerConnectionFailedListener(new OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    postCallback(new GoogleApiClientException(
                            "Connection failed: " + connectionResult.getErrorCode()));
                }
            });

            mGoogleApiClient.connect();
        }

        return mGoogleApiClient;
    }
}
