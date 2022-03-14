package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to create and tokenize Venmo accounts. For more information see the <a href="https://developer.paypal.com/braintree/docs/guides/venmo/overview">documentation</a>
 */
public class VenmoClient {

    static final String VENMO_PACKAGE_NAME = "com.venmo";
    static final String APP_SWITCH_ACTIVITY = "controller.SetupMerchantActivity";
    static final String META_KEY = "_meta";
    static final String EXTRA_MERCHANT_ID = "com.braintreepayments.api.MERCHANT_ID";
    static final String EXTRA_ACCESS_TOKEN = "com.braintreepayments.api.ACCESS_TOKEN";
    static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.ENVIRONMENT";
    static final String EXTRA_BRAINTREE_DATA = "com.braintreepayments.api.EXTRA_BRAINTREE_DATA";
    static final String EXTRA_PAYMENT_METHOD_NONCE = "com.braintreepayments.api.EXTRA_PAYMENT_METHOD_NONCE";
    static final String EXTRA_USERNAME = "com.braintreepayments.api.EXTRA_USER_NAME";
    static final String EXTRA_RESOURCE_ID = "com.braintreepayments.api.EXTRA_RESOURCE_ID";

    private final BraintreeClient braintreeClient;
    private final VenmoApi venmoApi;
    private final VenmoSharedPrefsWriter sharedPrefsWriter;
    private final DeviceInspector deviceInspector;
    private VenmoListener listener;

    @VisibleForTesting
    VenmoLifecycleObserver observer;

    /**
     * Create a new instance of {@link VenmoClient} from within an Activity using a {@link BraintreeClient}.
     *
     * @param activity a {@link FragmentActivity}
     * @param braintreeClient a {@link BraintreeClient}
     */
    public VenmoClient(@NonNull FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {
        this(activity, activity.getLifecycle(), braintreeClient, new ApiClient(braintreeClient));
    }

    /**
     * Create a new instance of {@link VenmoClient} from within a Fragment using a {@link BraintreeClient}.
     *
     * @param fragment a {@link Fragment}
     * @param braintreeClient a {@link BraintreeClient}
     */
    public VenmoClient(@NonNull Fragment fragment, @NonNull BraintreeClient braintreeClient) {
        this(fragment.requireActivity(), fragment.getLifecycle(), braintreeClient, new ApiClient(braintreeClient));
    }

    private VenmoClient(FragmentActivity activity, Lifecycle lifecycle, BraintreeClient braintreeClient, ApiClient apiClient) {
        this(activity, lifecycle, braintreeClient, new VenmoApi(braintreeClient, apiClient), new VenmoSharedPrefsWriter(), new DeviceInspector());
    }

    /**
     * Create a new instance of {@link VenmoClient} using a {@link BraintreeClient}.
     *
     * Deprecated. Use {@link VenmoClient(Fragment, BraintreeClient)} or
     * {@link VenmoClient(FragmentActivity, BraintreeClient)} instead.
     *
     * @param braintreeClient a {@link BraintreeClient}
     */
    @Deprecated
    public VenmoClient(@NonNull BraintreeClient braintreeClient) {
        this(null, null, braintreeClient, new ApiClient(braintreeClient));
    }

    @VisibleForTesting
    VenmoClient(FragmentActivity activity, Lifecycle lifecycle, BraintreeClient braintreeClient, VenmoApi venmoApi, VenmoSharedPrefsWriter sharedPrefsWriter, DeviceInspector deviceInspector) {
        this.braintreeClient = braintreeClient;
        this.sharedPrefsWriter = sharedPrefsWriter;
        this.deviceInspector = deviceInspector;
        this.venmoApi = venmoApi;
        if (activity != null && lifecycle != null) {
            addObserver(activity, lifecycle);
        }
    }

    private void addObserver(@NonNull FragmentActivity activity, @NonNull Lifecycle lifecycle) {
        observer = new VenmoLifecycleObserver(activity.getActivityResultRegistry(), this);
        lifecycle.addObserver(observer);
    }

    /**
     * Add a {@link VenmoListener} to your client to receive results or errors from the Venmo flow.
     * This method must be invoked on a {@link VenmoClient(Fragment, BraintreeClient)} or
     * {@link VenmoClient(FragmentActivity, BraintreeClient)} in order to receive results.
     *
     * @param listener a {@link VenmoListener}
     */
    public void setListener(VenmoListener listener) {
        this.listener = listener;
    }

    /**
     * Launches an Android Intent pointing to the Venmo app on the Google Play Store
     *
     * @param activity used to open the Venmo's Google Play Store
     */
    public void showVenmoInGooglePlayStore(@NonNull FragmentActivity activity) {
        braintreeClient.sendAnalyticsEvent("android.pay-with-venmo.app-store.invoked");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://play.google.com/store/apps/details?id=" + VENMO_PACKAGE_NAME));
        activity.startActivity(intent);
    }

    /**
     * Start the Pay With Venmo flow. This will app switch to the Venmo app.
     * <p>
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to {@link VenmoListener#onVenmoFailure(Exception)}
     *
     * @param activity Android FragmentActivity
     * @param request  {@link VenmoRequest}
     */
    public void tokenizeVenmoAccount(@NonNull final FragmentActivity activity, @NonNull final VenmoRequest request) {
        tokenizeVenmoAccount(activity, request, new VenmoTokenizeAccountCallback() {
            @Override
            public void onResult(@Nullable Exception error) {
                if (error != null) {
                    listener.onVenmoFailure(error);
                }
            }
        });
    }

    /**
     * Start the Pay With Venmo flow. This will app switch to the Venmo app.
     * <p>
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to {@link VenmoTokenizeAccountCallback#onResult(Exception)}
     *
     * Deprecated. Use {@link VenmoClient#tokenizeVenmoAccount(FragmentActivity, VenmoRequest)}.
     *
     * @param activity Android FragmentActivity
     * @param request  {@link VenmoRequest}
     * @param callback {@link VenmoTokenizeAccountCallback}
     */
    @Deprecated
    public void tokenizeVenmoAccount(@NonNull final FragmentActivity activity, @NonNull final VenmoRequest request, @NonNull final VenmoTokenizeAccountCallback callback) {
        braintreeClient.sendAnalyticsEvent("pay-with-venmo.selected");
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (configuration == null) {
                    callback.onResult(error);
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
                    return;
                }

                String exceptionMessage = null;
                if (!configuration.isVenmoEnabled()) {
                    exceptionMessage = "Venmo is not enabled";
                } else if (!deviceInspector.isVenmoAppSwitchAvailable(activity)) {
                    exceptionMessage = "Venmo is not installed";
                }

                if (exceptionMessage != null) {
                    callback.onResult(new AppSwitchNotAvailableException(exceptionMessage));
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
                    return;
                }

                String venmoProfileId = request.getProfileId();
                if (TextUtils.isEmpty(venmoProfileId)) {
                    venmoProfileId = configuration.getVenmoMerchantId();
                }

                final String finalVenmoProfileId = venmoProfileId;
                venmoApi.createPaymentContext(request, venmoProfileId, new VenmoApiCallback() {
                    @Override
                    public void onResult(@Nullable String paymentContextId, @Nullable Exception exception) {
                        if (exception == null) {
                            startVenmoActivityForResult(activity, request, configuration, finalVenmoProfileId, paymentContextId);
                        } else {
                            callback.onResult(exception);
                            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
                        }
                    }
                });
            }
        });
    }

    private void startVenmoActivityForResult(final FragmentActivity activity, final VenmoRequest request, final Configuration configuration, final String venmoProfileId, @Nullable final String paymentContextId) {
        braintreeClient.getAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorizationResult(@Nullable Authorization authorization, @Nullable Exception error) {
                if (authorization != null) {
                    boolean isClientTokenAuth = (authorization instanceof ClientToken);
                    boolean shouldVault = request.getShouldVault() && isClientTokenAuth;
                    sharedPrefsWriter.persistVenmoVaultOption(activity, shouldVault);
                    if (observer != null) {
                        VenmoIntentData intentData = new VenmoIntentData(configuration, venmoProfileId, paymentContextId, braintreeClient.getSessionId(), braintreeClient.getIntegrationType());
                        observer.launch(intentData);
                    } else {
                        Intent launchIntent = getLaunchIntent(configuration, venmoProfileId, paymentContextId);
                        activity.startActivityForResult(launchIntent, BraintreeRequestCodes.VENMO);
                    }
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.started");
                }
            }
        });
    }

    void onVenmoResult(final VenmoResult venmoResult) {
        if (venmoResult.getError() == null) {
            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.success");

            braintreeClient.getAuthorization(new AuthorizationCallback() {
                @Override
                public void onAuthorizationResult(@Nullable Authorization authorization, @Nullable Exception authError) {
                    if (authorization != null) {
                        final boolean isClientTokenAuth = (authorization instanceof ClientToken);

                        String paymentContextId = venmoResult.getPaymentContextId();
                        if (paymentContextId != null) {
                            venmoApi.createNonceFromPaymentContext(paymentContextId, new VenmoOnActivityResultCallback() {
                                @Override
                                public void onResult(@Nullable VenmoAccountNonce nonce, @Nullable Exception error) {
                                    if (nonce != null) {
                                        boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(braintreeClient.getApplicationContext());
                                        if (shouldVault && isClientTokenAuth) {
                                            vaultVenmoAccountNonce(nonce.getString(), new VenmoOnActivityResultCallback() {
                                                @Override
                                                public void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error) {
                                                    if (venmoAccountNonce != null) {
                                                        listener.onVenmoSuccess(venmoAccountNonce);
                                                    } else if (error != null) {
                                                        listener.onVenmoFailure(error);
                                                    }
                                                }
                                            });
                                        } else {
                                            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
                                            listener.onVenmoSuccess(nonce);
                                        }
                                    } else {
                                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
                                        listener.onVenmoFailure(error);
                                    }
                                }
                            });
                        } else {
                            String nonce = venmoResult.getVenmoAccountNonce();

                            boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(braintreeClient.getApplicationContext());
                            if (shouldVault && isClientTokenAuth) {
                                vaultVenmoAccountNonce(nonce, new VenmoOnActivityResultCallback() {
                                    @Override
                                    public void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error) {
                                        if (venmoAccountNonce != null) {
                                            listener.onVenmoSuccess(venmoAccountNonce);
                                        } else if (error != null) {
                                            listener.onVenmoFailure(error);
                                        }
                                    }
                                });
                            } else {
                                String venmoUsername = venmoResult.getVenmoUsername();
                                VenmoAccountNonce venmoAccountNonce = new VenmoAccountNonce(nonce, venmoUsername, false);
                                listener.onVenmoSuccess(venmoAccountNonce);
                            }
                        }
                    } else if (authError != null) {
                        listener.onVenmoFailure(authError);
                    }
                }
            });

        } else if (venmoResult.getError() != null) {
            if (venmoResult.getError() instanceof UserCanceledException) {
                braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
            }
            listener.onVenmoFailure(venmoResult.getError());
        }
    }

    /**
     * @param context    Android Context
     * @param resultCode a code associated with the Activity result
     * @param data       Android Intent
     * @param callback   {@link VenmoOnActivityResultCallback}
     */
    public void onActivityResult(@NonNull final Context context, int resultCode, @Nullable final Intent data, @NonNull final VenmoOnActivityResultCallback callback) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.success");

            braintreeClient.getAuthorization(new AuthorizationCallback() {
                @Override
                public void onAuthorizationResult(@Nullable Authorization authorization, @Nullable Exception authError) {
                    if (authorization != null) {
                        final boolean isClientTokenAuth = (authorization instanceof ClientToken);

                        String paymentContextId = data.getStringExtra(EXTRA_RESOURCE_ID);
                        if (paymentContextId != null) {
                            venmoApi.createNonceFromPaymentContext(paymentContextId, new VenmoOnActivityResultCallback() {
                                @Override
                                public void onResult(@Nullable VenmoAccountNonce nonce, @Nullable Exception error) {
                                        if (nonce != null) {
                                            boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(context);
                                            if (shouldVault && isClientTokenAuth) {
                                                vaultVenmoAccountNonce(nonce.getString(), callback);
                                            } else {
                                                braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
                                                callback.onResult(nonce, null);
                                            }
                                        } else {
                                            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
                                            callback.onResult(null, error);
                                        }
                                }
                            });
                        } else {
                            String nonce = data.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);

                            boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(context);
                            if (shouldVault && isClientTokenAuth) {
                                vaultVenmoAccountNonce(nonce, callback);
                            } else {
                                String venmoUsername = data.getStringExtra(EXTRA_USERNAME);
                                VenmoAccountNonce venmoAccountNonce = new VenmoAccountNonce(nonce, venmoUsername, false);
                                callback.onResult(venmoAccountNonce, null);
                            }
                        }
                    } else if (authError != null) {
                        callback.onResult(null, authError);
                    }
                }
            });

        } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
            callback.onResult(null, new UserCanceledException("User canceled Venmo."));
        }
    }

    private void vaultVenmoAccountNonce(String nonce, final VenmoOnActivityResultCallback callback) {
        venmoApi.vaultVenmoAccountNonce(nonce, new VenmoOnActivityResultCallback() {
            @Override
            public void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error) {
                if (venmoAccountNonce != null) {
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.vault.success");
                } else {
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.vault.failed");
                }
                callback.onResult(venmoAccountNonce, error);
            }
        });
    }

    private static Intent getVenmoIntent() {
        return new Intent().setComponent(new ComponentName(VENMO_PACKAGE_NAME, VENMO_PACKAGE_NAME + "." + APP_SWITCH_ACTIVITY));
    }

    private Intent getLaunchIntent(Configuration configuration, String profileId, String paymentContextId) {
        Intent venmoIntent = getVenmoIntent()
                .putExtra(EXTRA_MERCHANT_ID, profileId)
                .putExtra(EXTRA_ACCESS_TOKEN, configuration.getVenmoAccessToken())
                .putExtra(EXTRA_ENVIRONMENT, configuration.getVenmoEnvironment());

        if (paymentContextId != null) {
            venmoIntent.putExtra(EXTRA_RESOURCE_ID, paymentContextId);
        }

        try {
            JSONObject braintreeData = new JSONObject();

            JSONObject meta = new MetadataBuilder()
                    .sessionId(braintreeClient.getSessionId())
                    .integration(braintreeClient.getIntegrationType())
                    .version()
                    .build();

            braintreeData.put(META_KEY, meta);
            venmoIntent.putExtra(EXTRA_BRAINTREE_DATA, braintreeData.toString());
        } catch (JSONException ignored) {
        }

        return venmoIntent;
    }

    /**
     * Check if Venmo app switch is available.
     *
     * @param context Application Context
     * @return true if the Venmo app is installed, false otherwise
     */
    public boolean isVenmoAppSwitchAvailable(@NonNull Context context) {
        return deviceInspector.isVenmoAppSwitchAvailable(context);
    }

    /**
     * Before starting the Venmo flow, use this method to check whether
     * Venmo is supported and set up on the device. When the callback is called with
     * {@code true}, show the Venmo button. When it is called with {@code false}, display other
     * checkout options.
     *
     * @param context Android Context
     * @param callback {@link VenmoIsReadyToPayCallback}
     */
    public void isReadyToPay(final Context context, final VenmoIsReadyToPayCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception configError) {
                if (configuration != null) {
                    boolean result =
                        configuration.isVenmoEnabled() && isVenmoAppSwitchAvailable(context);
                    callback.onResult(result, null);
                } else {
                    callback.onResult(false, configError);
                }
            }
        });
    }
}
