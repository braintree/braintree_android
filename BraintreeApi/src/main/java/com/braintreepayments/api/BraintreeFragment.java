package com.braintreepayments.api;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.GoogleApiClientException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeListener;
import com.braintreepayments.api.interfaces.ConfigurationErrorListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.interfaces.PaymentMethodsUpdatedListener;
import com.braintreepayments.api.interfaces.QueuedCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.ClientKey;
import com.braintreepayments.api.models.ClientToken;
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

    public static final String EXTRA_CLIENT_TOKEN = "com.braintreepayments.api.EXTRA_CLIENT_TOKEN";
    public static final String EXTRA_CLIENT_KEY = "com.braintreepayments.api.EXTRA_CLIENT_KEY";
    public static final String EXTRA_INTEGRATION_TYPE = "com.braintreepayments.api.EXTRA_INTEGRATION_TYPE";
    public static final String TAG = "com.braintreepayments.api.BraintreeFragment";

    private Context mContext;
    private ClientKey mClientKey;
    private ClientToken mClientToken;
    private Configuration mConfiguration;

    @VisibleForTesting
    protected BraintreeHttpClient mHttpClient;

    private Object mGoogleApiClient;
    private Queue<QueuedCallback> mCallbackQueue = new ArrayDeque<>();
    private List<PaymentMethod> mCachedPaymentMethods = new ArrayList<>();
    private boolean mHasFetchedPaymentMethods = false;

    private ConfigurationListener mConfigurationListener;
    private ConfigurationErrorListener mConfigurationErrorListener;
    private BraintreeCancelListener mCancelListener;
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
     * @param clientToken The client token to use.
     * @return {@link BraintreeFragment}
     * @throws InvalidArgumentException If the client token is not valid json or cannot be parsed.
     */
    public static BraintreeFragment newInstance(Activity activity, String clientToken)
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
                bundle.putString(EXTRA_CLIENT_KEY, ClientKey.fromString(clientToken).getClientKey());
            } catch (InvalidArgumentException e) {
                try {
                    bundle.putString(EXTRA_CLIENT_TOKEN,
                            ClientToken.fromString(clientToken).toJson());
                } catch (JSONException e1) {
                    throw new InvalidArgumentException("Client key or client token was invalid.");
                }
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

        try {
            if (getArguments().containsKey(EXTRA_CLIENT_KEY)) {
                mClientKey = ClientKey.fromString(getArguments().getString(EXTRA_CLIENT_KEY));
                if (mHttpClient == null) {
                    mHttpClient = new BraintreeHttpClient(mClientKey);
                }
                sendAnalyticsEvent("started.client-key");
            } else {
                mClientToken = ClientToken.fromString(getArguments().getString(EXTRA_CLIENT_TOKEN));
                if (mHttpClient == null) {
                    mHttpClient = new BraintreeHttpClient(mClientToken);
                }
                sendAnalyticsEvent("started.client-token");
            }
        } catch (InvalidArgumentException | JSONException ignored) {
            // already checked in BraintreeFragment.newInstance
        }

        mContext = getActivity().getApplicationContext();
        mIntegrationType = getArguments().getString(EXTRA_INTEGRATION_TYPE);

        fetchConfiguration();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof BraintreeListener) {
            addListener((BraintreeListener) getActivity());
        }

        flushCallbacks();

        if (mGoogleApiClient != null) {
            ((GoogleApiClient) mGoogleApiClient).connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        AnalyticsManager.flushEvents(this);

        if (getActivity() instanceof BraintreeListener) {
            removeListener((BraintreeListener) getActivity());
        }

        if (mGoogleApiClient != null) {
            ((GoogleApiClient) mGoogleApiClient).disconnect();
        }
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED) {
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
            case PayPal.PAYPAL_AUTHORIZATION_REQUEST_CODE:
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

        if (listener instanceof ConfigurationErrorListener) {
            mConfigurationErrorListener = (ConfigurationErrorListener) listener;
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

        if (listener instanceof ConfigurationErrorListener) {
            mConfigurationErrorListener = null;
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

    protected void postCallback(final Throwable error) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return mErrorListener != null;
            }

            @Override
            public void run() {
                if (error instanceof ErrorWithResponse) {
                    mErrorListener.onRecoverableError((ErrorWithResponse) error);
                } else {
                    mErrorListener.onUnrecoverableError(error);
                }
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
        String configUrl;
        if (mClientKey != null) {
            configUrl = mClientKey.getConfigUrl();
        } else {
            configUrl = mClientToken.getConfigUrl();
        }

        configUrl = Uri.parse(configUrl)
                .buildUpon()
                .appendQueryParameter("configVersion", "3")
                .build()
                .toString();

        getHttpClient().get(configUrl, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    mConfiguration = Configuration.fromJson(responseBody);
                    getHttpClient().setBaseUrl(mConfiguration.getClientApiUrl());

                    postOrQueueCallback(new QueuedCallback() {
                        @Override
                        public boolean shouldRun() {
                            return mConfigurationListener != null;
                        }

                        @Override
                        public void run() {
                            mConfigurationListener.onConfigurationFetched();
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
                            mConfigurationErrorListener.onConfigurationError(e);
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
                        mConfigurationErrorListener.onConfigurationError(exception);
                    }
                });
            }
        });
    }

    protected void waitForConfiguration(final ConfigurationListener listener) {
        postOrQueueCallback(new QueuedCallback() {
            @Override
            public boolean shouldRun() {
                return getConfiguration() != null;
            }

            @Override
            public void run() {
                listener.onConfigurationFetched();
            }
        });
    }

    public Context getContext() {
        return mContext;
    }

    protected ClientKey getClientKey() {
        return mClientKey;
    }
    protected ClientToken getClientToken() {
        return mClientToken;
    }

    protected Configuration getConfiguration() {
        return mConfiguration;
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
     *
     * Connection failed and connection suspended errors will be sent to
     * {@link BraintreeErrorListener#onUnrecoverableError(Throwable)}.
     *
     * @return {@link GoogleApiClient}.
     */
    public GoogleApiClient getGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                            .setEnvironment(AndroidPay.getEnvironment(getConfiguration().getAndroidPay()))
                            .setTheme(WalletConstants.THEME_LIGHT)
                            .build())
                    .build();
        }

        GoogleApiClient googleApiClient = (GoogleApiClient) mGoogleApiClient;

        if (!googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
            googleApiClient.registerConnectionCallbacks(new ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {}

                @Override
                public void onConnectionSuspended(int i) {
                    postCallback(new GoogleApiClientException("Connection suspended: " + i));
                }
            });

            googleApiClient.registerConnectionFailedListener(new OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    postCallback(new GoogleApiClientException("Connection failed: " + connectionResult.getErrorCode()));
                }
            });

            googleApiClient.connect();
        }

        return googleApiClient;
    }
}
