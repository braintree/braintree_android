package com.braintreepayments.api;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.braintreepayments.api.exceptions.GoogleApiClientException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.interfaces.PaymentMethodsUpdatedListener;
import com.braintreepayments.api.interfaces.QueuedCallback;
import com.braintreepayments.api.internal.BraintreeBroadcastReceiver;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.TokenizationKey;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethod;
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
import java.util.Queue;

/**
 * Core Braintree class that handles network requests and managing callbacks.
 */
public class BraintreeFragment extends Fragment {

    public static final String TAG = "com.braintreepayments.api.BraintreeFragment";

    protected static final String EXTRA_CONFIGURATION =
            "com.braintreepayments.api.EXTRA_CONFIGURATION";

    private static final String EXTRA_AUTHORIZATION_TOKEN =
            "com.braintreepayments.api.EXTRA_AUTHORIZATION_TOKEN";
    private static final String EXTRA_INTEGRATION_TYPE =
            "com.braintreepayments.api.EXTRA_INTEGRATION_TYPE";

    private Context mContext;
    private Authorization mAuthorization;
    private Configuration mConfiguration;
    private static BraintreeBroadcastReceiver sBroadcastReceiver;

    @VisibleForTesting
    protected BraintreeHttpClient mHttpClient;
    protected GoogleApiClient mGoogleApiClient;

    private Queue<QueuedCallback> mCallbackQueue = new ArrayDeque<>();
    private List<PaymentMethod> mCachedPaymentMethods = new ArrayList<>();
    private boolean mHasFetchedPaymentMethods = false;

    protected BraintreeCancelListener mCancelListener;
    private ConfigurationListener mConfigurationListener;
    private BraintreeResponseListener<Exception> mConfigurationErrorListener;
    private PaymentMethodsUpdatedListener mPaymentMethodsUpdatedListener;
    private PaymentMethodCreatedListener mPaymentMethodCreatedListener;
    private BraintreeErrorListener mErrorListener;

    @VisibleForTesting
    protected String mIntegrationType;

    public BraintreeFragment() {}

    /**
     * Create a new instance of {@link BraintreeFragment} using the client token and add it to the
     * {@link Activity}'s {@link FragmentManager}.
     *
     * @param activity The {@link Activity} to add the {@link Fragment} to.
     * @param authorization The tokenization key or client token to use.
     * @return {@link BraintreeFragment}
     * @throws InvalidArgumentException If the client token is not valid json or cannot be parsed.
     */
    public static BraintreeFragment newInstance(Activity activity, String authorization)
            throws InvalidArgumentException {
        return newInstance(activity, authorization, new Bundle());
    }

    protected static BraintreeFragment newInstance(Activity activity, String authorizationString,
            Bundle bundle) throws InvalidArgumentException {
        FragmentManager fm = activity.getFragmentManager();

        String integrationType = "custom";
        try {
            if (Class.forName("com.braintreepayments.api.BraintreePaymentActivity")
                    .isInstance(activity)) {
                integrationType = "dropin";
            }
        } catch (ClassNotFoundException ignored) {}

        if (bundle.containsKey(EXTRA_CONFIGURATION)) {
            try {
                Configuration.fromJson(bundle.getString(EXTRA_CONFIGURATION));
            } catch (JSONException e) {
                throw new InvalidArgumentException(e.getMessage());
            }
        }

        BraintreeFragment braintreeFragment = (BraintreeFragment) fm.findFragmentByTag(TAG);
        if (braintreeFragment == null) {
            braintreeFragment = new BraintreeFragment();

            try {
                Authorization authorization = Authorization.fromString(authorizationString);
                bundle.putParcelable(EXTRA_AUTHORIZATION_TOKEN, authorization);
            } catch (InvalidArgumentException e) {
                throw new InvalidArgumentException("Tokenization Key or client token was invalid.");
            }

            bundle.putString(EXTRA_INTEGRATION_TYPE, integrationType);
            braintreeFragment.setArguments(bundle);
            fm.beginTransaction().add(braintreeFragment, TAG).commit();
        }

        return braintreeFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mContext = getActivity().getApplicationContext();
        mIntegrationType = getArguments().getString(EXTRA_INTEGRATION_TYPE);
        mAuthorization = getArguments().getParcelable(EXTRA_AUTHORIZATION_TOKEN);
        if (sBroadcastReceiver == null) {
            sBroadcastReceiver = new BraintreeBroadcastReceiver();
        }

        if (mHttpClient == null) {
            mHttpClient = new BraintreeHttpClient(mAuthorization);
        }

        if (mAuthorization instanceof TokenizationKey) {
            sendAnalyticsEvent("started.client-key");
        } else {
            sendAnalyticsEvent("started.client-token");
        }

        if (getArguments().getString(EXTRA_CONFIGURATION) == null) {
            fetchConfiguration();
        } else {
            try {
                setConfiguration(Configuration.fromJson(getArguments().getString(EXTRA_CONFIGURATION)));
            } catch (JSONException ignored) {}
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof BraintreeListener) {
            addListener((BraintreeListener) getActivity());
        }

        flushCallbacks();

        sBroadcastReceiver.register(this);
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
    public void onStop() {
        super.onStop();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sBroadcastReceiver.unregister(getApplicationContext());
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED && data != null) {
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

        switch (requestCode) {
            case PayPal.PAYPAL_REQUEST_CODE:
                PayPal.onActivityResult(this, resultCode, data);
                break;
            case Venmo.VENMO_REQUEST_CODE:
                Venmo.onActivityResult(this, resultCode, data);
                break;
            case ThreeDSecure.THREE_D_SECURE_REQUEST_CODE:
                ThreeDSecure.onActivityResult(this, resultCode, data);
                break;
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

        if (listener instanceof PaymentMethodsUpdatedListener) {
            mPaymentMethodsUpdatedListener = (PaymentMethodsUpdatedListener) listener;
        }

        if (listener instanceof PaymentMethodCreatedListener) {
            mPaymentMethodCreatedListener = (PaymentMethodCreatedListener) listener;
        }

        if (listener instanceof BraintreeErrorListener) {
            mErrorListener = (BraintreeErrorListener) listener;
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

        if (listener instanceof PaymentMethodsUpdatedListener) {
            mPaymentMethodsUpdatedListener = null;
        }

        if (listener instanceof PaymentMethodCreatedListener) {
            mPaymentMethodCreatedListener = null;
        }

        if (listener instanceof BraintreeErrorListener) {
            mErrorListener = null;
        }
    }

    protected void sendAnalyticsEvent(final String eventFragment) {
        AnalyticsManager.sendRequest(this, mIntegrationType, eventFragment);
    }

    protected void postCallback(final PaymentMethod paymentMethod) {
        mCachedPaymentMethods.add(0, paymentMethod);
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mPaymentMethodCreatedListener != null;
            }

            @Override
            public void run() {
                mPaymentMethodCreatedListener.onPaymentMethodCreated(paymentMethod);
            }
        });
    }

    protected void postCallback(final List<PaymentMethod> paymentMethodList) {
        mCachedPaymentMethods = paymentMethodList;
        mHasFetchedPaymentMethods = true;
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mPaymentMethodsUpdatedListener != null;
            }

            @Override
            public void run() {
                mPaymentMethodsUpdatedListener.onPaymentMethodsUpdated(paymentMethodList);
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
        String configUrl = Uri.parse(mAuthorization.getConfigUrl())
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();

        getHttpClient().get(configUrl, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    setConfiguration(Configuration.fromJson(responseBody));

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
                } catch (final JSONException e) {
                    postCallback(e);
                    postOrQueueCallback(new QueuedCallback() {
                        @Override
                        public boolean shouldRun() {
                            return mConfigurationErrorListener != null;
                        }

                        @Override
                        public void run() {
                            mConfigurationErrorListener.onResponse(e);
                        }
                    });
                }
            }

            @Override
            public void failure(final Exception exception) {
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
            }
        });
    }

    void setConfigurationErrorListener(BraintreeResponseListener<Exception> listener) {
        mConfigurationErrorListener = listener;
    }

    protected void waitForConfiguration(final ConfigurationListener listener) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return getConfiguration() != null;
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

    protected boolean hasFetchedPaymentMethods() {
        return mHasFetchedPaymentMethods;
    }

    protected List<PaymentMethod> getCachedPaymentMethods() {
        return Collections.unmodifiableList(mCachedPaymentMethods);
    }

    /**
     * Obtain an instance of a {@link GoogleApiClient} that is connected or connecting to be used
     * for Android Pay. This instance will be automatically disconnected in
     * {@link Fragment#onPause()} and automatically connected in {@link Fragment#onResume()}.
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
