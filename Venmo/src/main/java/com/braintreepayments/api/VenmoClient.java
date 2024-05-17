package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

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

    /**
     * Used for linking events from the client to server side request
     * In the Venmo flow this will be a Payment Context ID
     */
    private String payPalContextId = null;

    /**
     * Used for sending the type of flow, universal vs deeplink to FPTI
     */
    private String linkType = null;

    /**
     * True if `tokenize()` was called with a Vault request object type
     */
    private Boolean isVaultRequest = false;

    @VisibleForTesting
    VenmoLifecycleObserver observer;

    @VisibleForTesting
    BrowserSwitchResult pendingBrowserSwitchResult;

    /**
     * Create a new instance of {@link VenmoClient} from within an Activity using a {@link BraintreeClient}.
     *
     * @param activity        a {@link FragmentActivity}
     * @param braintreeClient a {@link BraintreeClient}
     */
    public VenmoClient(@NonNull FragmentActivity activity, @NonNull BraintreeClient braintreeClient) {
        this(activity, activity.getLifecycle(), braintreeClient, new ApiClient(braintreeClient));
    }

    /**
     * Create a new instance of {@link VenmoClient} from within a Fragment using a {@link BraintreeClient}.
     *
     * @param fragment        a {@link Fragment}
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
     * <p>
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
        if (pendingBrowserSwitchResult != null) {
            deliverBrowserSwitchResultToListener(pendingBrowserSwitchResult);
        }
    }

    /**
     * Launches an Android Intent pointing to the Venmo app on the Google Play Store
     *
     * @param activity used to open the Venmo's Google Play Store
     */
    public void showVenmoInGooglePlayStore(@NonNull FragmentActivity activity) {
        braintreeClient.sendAnalyticsEvent("android.pay-with-venmo.app-store.invoked", payPalContextId, linkType, isVaultRequest);
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
                    deliverVenmoFailure(error);
                }
            }
        });
    }

    /**
     * Start the Pay With Venmo flow. This will app switch to the Venmo app.
     * <p>
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to {@link VenmoTokenizeAccountCallback#onResult(Exception)}
     * <p>
     * Deprecated. Use {@link VenmoClient#tokenizeVenmoAccount(FragmentActivity, VenmoRequest)}.
     *
     * @param activity Android FragmentActivity
     * @param request  {@link VenmoRequest}
     * @param callback {@link VenmoTokenizeAccountCallback}
     */
    @Deprecated
    public void tokenizeVenmoAccount(@NonNull final FragmentActivity activity, @NonNull final VenmoRequest request, @NonNull final VenmoTokenizeAccountCallback callback) {
        linkType = request.getFallbackToWeb() ? "universal" : "deeplink";
        braintreeClient.sendAnalyticsEvent("pay-with-venmo.selected", payPalContextId, linkType, isVaultRequest);
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (configuration == null) {
                    callback.onResult(error);
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed", payPalContextId, linkType, isVaultRequest);
                    return;
                }

                String exceptionMessage = null;
                if (!request.getFallbackToWeb()) {
                     if (!deviceInspector.isVenmoAppSwitchAvailable(activity)) {
                        exceptionMessage = "Venmo is not installed";
                    }
                }

                if (!configuration.isVenmoEnabled()) {
                    exceptionMessage = "Venmo is not enabled";
                }

                if (exceptionMessage != null) {
                    callback.onResult(new AppSwitchNotAvailableException(exceptionMessage));
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed", payPalContextId, linkType, isVaultRequest);
                    return;
                }

                // Merchants are not allowed to collect user addresses unless ECD (Enriched Customer Data) is enabled on the BT Control Panel.
                if ((request.getCollectCustomerShippingAddress() || request.getCollectCustomerBillingAddress()) && !configuration.getVenmoEnrichedCustomerDataEnabled()) {
                    callback.onResult(new BraintreeException("Cannot collect customer data when ECD is disabled. Enable this feature in the Control Panel to collect this data."));
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed", payPalContextId, linkType, isVaultRequest);
                    return;
                }

                String venmoProfileId = request.getProfileId();
                if (TextUtils.isEmpty(venmoProfileId)) {
                    venmoProfileId = configuration.getVenmoMerchantId();
                }

                final String finalVenmoProfileId = venmoProfileId;
                venmoApi.createPaymentContext(request, venmoProfileId, new VenmoApiCallback() {
                    @Override
                    public void onResult(@Nullable final String paymentContextId, @Nullable Exception exception) {
                        if (exception == null) {

                            if (paymentContextId != null && !paymentContextId.isEmpty()) {
                                payPalContextId = paymentContextId;
                            }

                            braintreeClient.getAuthorization(new AuthorizationCallback() {
                                @Override
                                public void onAuthorizationResult(@Nullable Authorization authorization, @Nullable Exception authError) {
                                    if (authorization != null) {
                                        startVenmoActivityForResult(activity, request, configuration, authorization, finalVenmoProfileId, paymentContextId);
                                    } else {
                                        callback.onResult(authError);
                                    }
                                }
                            });
                        } else {
                            callback.onResult(exception);
                            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed", payPalContextId, linkType, isVaultRequest);
                        }
                    }
                });
            }
        });
    }

    private void startVenmoActivityForResult(
            final FragmentActivity activity,
            final VenmoRequest request,
            final Configuration configuration,
            Authorization authorization,
            final String venmoProfileId,
            @Nullable final String paymentContextId
    ) {
        boolean isClientTokenAuth = (authorization instanceof ClientToken);
        boolean shouldVault = request.getShouldVault() && isClientTokenAuth;
        sharedPrefsWriter.persistVenmoVaultOption(activity, shouldVault);
        if (observer != null) {
            VenmoIntentData intentData = new VenmoIntentData(configuration, venmoProfileId, paymentContextId, braintreeClient.getSessionId(), braintreeClient.getIntegrationType());
            if (request.getFallbackToWeb()) {
                try {
                    startAppLinkFlow(activity, intentData);
                } catch (JSONException | BrowserSwitchException exception) {
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.failure", payPalContextId, linkType, isVaultRequest);
                    deliverVenmoFailure(exception);
                }
            } else {
                observer.launch(intentData);
            }
        } else {
            Intent launchIntent = getLaunchIntent(configuration, venmoProfileId, paymentContextId);
            activity.startActivityForResult(launchIntent, BraintreeRequestCodes.VENMO);
        }
        braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.started", payPalContextId, linkType, isVaultRequest);
    }

    void onVenmoResult(final VenmoResult venmoResult) {
        if (venmoResult.getError() == null) {
            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.success", payPalContextId, linkType, isVaultRequest);

            braintreeClient.getAuthorization(new AuthorizationCallback() {
                @Override
                public void onAuthorizationResult(@Nullable Authorization authorization, @Nullable final Exception authError) {
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
                                                        deliverVenmoSuccess(venmoAccountNonce);
                                                    } else if (error != null) {
                                                        deliverVenmoFailure(error);
                                                    }
                                                }
                                            });
                                        } else {
                                            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure", payPalContextId, linkType, isVaultRequest);
                                            deliverVenmoSuccess(nonce);
                                        }
                                    } else {
                                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure", payPalContextId, linkType, isVaultRequest);
                                        deliverVenmoFailure(error);
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
                                            deliverVenmoSuccess(venmoAccountNonce);
                                        } else if (error != null) {
                                            deliverVenmoFailure(error);
                                        }
                                    }
                                });
                            } else {
                                String venmoUsername = venmoResult.getVenmoUsername();
                                VenmoAccountNonce venmoAccountNonce = new VenmoAccountNonce(nonce, venmoUsername, false);
                                deliverVenmoSuccess(venmoAccountNonce);
                            }

                        }
                    } else if (authError != null) {
                        deliverVenmoFailure(authError);
                    }
                }
            });

        } else if (venmoResult.getError() != null) {
            if (venmoResult.getError() instanceof UserCanceledException) {
                braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.canceled", payPalContextId, linkType, isVaultRequest);
            }
            deliverVenmoFailure(venmoResult.getError());
        }
    }

    private void deliverVenmoSuccess(VenmoAccountNonce venmoAccountNonce) {
        if (listener != null) {
            listener.onVenmoSuccess(venmoAccountNonce);
        } else {
            Log.w(LoggingUtils.TAG, LoggingUtils.LISTENER_WARNING);
        }
    }

    private void deliverVenmoFailure(Exception error) {
        if (listener != null) {
            listener.onVenmoFailure(error);
        } else {
            Log.w(LoggingUtils.TAG, LoggingUtils.LISTENER_WARNING);
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
            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.success", payPalContextId, linkType, isVaultRequest);

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
                                            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure", payPalContextId, linkType, isVaultRequest);
                                            callback.onResult(nonce, null);
                                        }

                                    } else {
                                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure", payPalContextId, linkType, isVaultRequest);
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
            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.canceled", payPalContextId, linkType, isVaultRequest);
            callback.onResult(null, new UserCanceledException("User canceled Venmo."));
        }
    }

    private void vaultVenmoAccountNonce(String nonce, final VenmoOnActivityResultCallback callback) {
        venmoApi.vaultVenmoAccountNonce(nonce, new VenmoOnActivityResultCallback() {
            @Override
            public void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error) {
                if (venmoAccountNonce != null) {
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.vault.success", payPalContextId, linkType, isVaultRequest);
                } else {
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.vault.failed", payPalContextId, linkType, isVaultRequest);
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
     * Use this method with the manual browser switch integration pattern.
     *
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback            {@link VenmoOnActivityResultCallback}
     */
    public void onBrowserSwitchResult(@NonNull BrowserSwitchResult browserSwitchResult, @NonNull final VenmoOnActivityResultCallback callback) {
        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.canceled", payPalContextId, linkType, isVaultRequest);
                callback.onResult(null, new UserCanceledException("User canceled Venmo."));
                break;
            case BrowserSwitchStatus.SUCCESS:
                Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUri != null) {
                    if (deepLinkUri.getPath().contains("success")) {
                        String resourceId = parseResourceId(String.valueOf(deepLinkUri));
                        String paymentMethodNonce = parsePaymentMethodNonce(String.valueOf(deepLinkUri));
                        String username = parseUsername(String.valueOf(deepLinkUri));
                        Context context = braintreeClient.getApplicationContext();

                        braintreeClient.getAuthorization(new AuthorizationCallback() {
                            @Override
                            public void onAuthorizationResult(@Nullable Authorization authorization, @Nullable Exception authError) {
                            if (authorization != null) {
                                final boolean isClientTokenAuth = (authorization instanceof ClientToken);

                                if (resourceId != null) {
                                    venmoApi.createNonceFromPaymentContext(resourceId, new VenmoOnActivityResultCallback() {
                                        @Override
                                        public void onResult(@Nullable VenmoAccountNonce nonce, @Nullable Exception error) {
                                            if (nonce != null) {
                                                boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(context);
                                                if (shouldVault && isClientTokenAuth) {
                                                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.success", payPalContextId, linkType, isVaultRequest);
                                                    vaultVenmoAccountNonce(nonce.getString(), callback);
                                                } else {
                                                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.success", payPalContextId, linkType, isVaultRequest);
                                                    callback.onResult(nonce, null);
                                                }
                                            } else {
                                                braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.failure", payPalContextId, linkType, isVaultRequest);
                                                callback.onResult(null, error);
                                            }
                                        }
                                    });
                                } else if (paymentMethodNonce != null && username != null) {
                                    boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(context);
                                    if (shouldVault && isClientTokenAuth) {
                                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.success", payPalContextId, linkType, isVaultRequest);
                                        vaultVenmoAccountNonce(paymentMethodNonce, callback);
                                    } else {
                                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.success", payPalContextId, linkType, isVaultRequest);
                                        VenmoAccountNonce venmoAccountNonce = new VenmoAccountNonce(paymentMethodNonce, username, false);
                                        callback.onResult(venmoAccountNonce, null);
                                    }
                                }
                            } else if (authError != null) {
                                braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.failure", payPalContextId, linkType, isVaultRequest);
                                callback.onResult(null, authError);
                            }
                        }
                        });
                    } else if (deepLinkUri.getPath().contains("cancel")) {
                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.canceled", payPalContextId, linkType, isVaultRequest);
                        callback.onResult(null, new UserCanceledException("User canceled Venmo."));
                    } else if (deepLinkUri.getPath().contains("error")) {
                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.failure", payPalContextId, linkType, isVaultRequest);
                        callback.onResult(null, new Exception("Error returned from Venmo."));
                    }
                } else {
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.failure", payPalContextId, linkType, isVaultRequest);
                    callback.onResult(null, new Exception("Unknown error"));
                }
                break;
        }
    }

    /**
     * After calling {@link VenmoClient#tokenizeVenmoAccount(FragmentActivity, VenmoRequest)},
     * call this method in your Activity or Fragment's onResume() method to see if a response
     * was provided through deep linking.
     *
     * If a BrowserSwitchResult exists, call {@link VenmoClient#onBrowserSwitchResult(BrowserSwitchResult)}
     * to allow the SDK to continue tokenization of the VenmoAccount.
     *
     * Make sure to call {@link VenmoClient#clearActiveBrowserSwitchRequests(Context)} after
     * successfully parsing a BrowserSwitchResult to guard against multiple invocations of browser
     * switch event handling.
     *
     * @param context The context used to check for pending browser switch requests
     * @param intent The intent containing a potential deep link response. May be null.
     * @return {@link BrowserSwitchResult} when a result has been parsed successfully from a deep link; null when an input Intent is null
     */
    @Nullable
    public BrowserSwitchResult parseBrowserSwitchResult(@NonNull Context context, @Nullable Intent intent) {
        int requestCode = BraintreeRequestCodes.VENMO;
        return braintreeClient.parseBrowserSwitchResult(context, requestCode, intent);
    }

    /**
     * Make sure to call this method after {@link VenmoClient#parseBrowserSwitchResult(Context, Intent)}
     * parses a {@link BrowserSwitchResult} successfully to prevent multiple invocations of browser
     * switch event handling logic.
     *
     * @param context The context used to clear pending browser switch requests
     */
    public void clearActiveBrowserSwitchRequests(@NonNull Context context) {
        braintreeClient.clearActiveBrowserSwitchRequests(context);
    }

    void onBrowserSwitchResult(@NonNull BrowserSwitchResult browserSwitchResult) {
        this.pendingBrowserSwitchResult = browserSwitchResult;
        if (listener != null) {
            // NEXT_MAJOR_VERSION: remove all manual browser switching methods
            deliverBrowserSwitchResultToListener(pendingBrowserSwitchResult);
        }
    }

    // NEXT_MAJOR_VERSION: remove all manual browser switching methods
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

    private String parseResourceId(String deepLinkUri) {
        String resourceIdFromBrowserSwitch = Uri.parse(deepLinkUri).getQueryParameter("resource_id");
        if (resourceIdFromBrowserSwitch != null) {
            return resourceIdFromBrowserSwitch;
        } else {
            String cleanedAppSwitchUri = deepLinkUri.replaceFirst("&","?");
            String resourceIdFromAppSwitch = Uri.parse(String.valueOf(cleanedAppSwitchUri)).getQueryParameter("resource_id");
            if (resourceIdFromAppSwitch != null) {
                return resourceIdFromAppSwitch;
            } else {
                return null;
            }
        }
    }

    private String parsePaymentMethodNonce(String deepLinkUri) {
        String paymentMethodNonceFromBrowserSwitch = Uri.parse(deepLinkUri).getQueryParameter("payment_method_nonce");
        if (paymentMethodNonceFromBrowserSwitch != null) {
            return paymentMethodNonceFromBrowserSwitch;
        } else {
            String cleanedAppSwitchUri = deepLinkUri.replaceFirst("&","?");
            String paymentMethodNonceFromAppSwitch = Uri.parse(String.valueOf(cleanedAppSwitchUri)).getQueryParameter("payment_method_nonce");
            if (paymentMethodNonceFromAppSwitch != null) {
                return paymentMethodNonceFromAppSwitch;
            } else {
                return null;
            }
        }
    }

    private String parseUsername(String deepLinkUri) {
        String usernameFromBrowserSwitch = Uri.parse(deepLinkUri).getQueryParameter("username");
        if (usernameFromBrowserSwitch != null) {
            return usernameFromBrowserSwitch;
        } else {
            String cleanedAppSwitchUri = deepLinkUri.replaceFirst("&","?");
            String usernameFromAppSwitch = Uri.parse(String.valueOf(cleanedAppSwitchUri)).getQueryParameter("username");
            if (usernameFromAppSwitch != null) {
                return usernameFromAppSwitch;
            } else {
                return null;
            }
        }
    }

    private void deliverBrowserSwitchResultToListener(final BrowserSwitchResult browserSwitchResult) {
        onBrowserSwitchResult(browserSwitchResult, new VenmoOnActivityResultCallback() {
            @Override
            public void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error) {
                if (listener != null) {
                    if (venmoAccountNonce != null) {
                        listener.onVenmoSuccess(venmoAccountNonce);
                    } else if (error != null) {
                        listener.onVenmoFailure(error);
                    }
                }
            }
        });

        this.pendingBrowserSwitchResult = null;
    }

    @VisibleForTesting
    void startAppLinkFlow(FragmentActivity activity, VenmoIntentData input) throws JSONException, BrowserSwitchException {
        JSONObject metadata = new MetadataBuilder()
                .sessionId(input.getSessionId())
                .integration(input.getIntegrationType())
                .version()
                .build();

        JSONObject braintreeData = new JSONObject()
                .put("_meta", metadata);

        String applicationName = "ApplicationNameUnknown";
        Context context = braintreeClient.getApplicationContext();
        if (context != null) {
            if (context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString() != null) {
                applicationName = context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
            }
        }

        Uri venmoBaseURL = Uri.parse("https://venmo.com/go/checkout")
                .buildUpon()
                .appendQueryParameter("x-success", braintreeClient.getReturnUrlScheme() + "://x-callback-url/vzero/auth/venmo/success")
                .appendQueryParameter("x-error", braintreeClient.getReturnUrlScheme() + "://x-callback-url/vzero/auth/venmo/error")
                .appendQueryParameter("x-cancel", braintreeClient.getReturnUrlScheme() + "://x-callback-url/vzero/auth/venmo/cancel")
                .appendQueryParameter("x-source", applicationName)
                .appendQueryParameter("braintree_merchant_id", input.getProfileId())
                .appendQueryParameter("braintree_access_token", input.getConfiguration().getVenmoAccessToken())
                .appendQueryParameter("braintree_environment", input.getConfiguration().getVenmoEnvironment())
                .appendQueryParameter("resource_id", input.getPaymentContextId())
                .appendQueryParameter("braintree_sdk_data", Base64.encodeToString(braintreeData.toString().getBytes(), 0))
                .appendQueryParameter("customerClient", "MOBILE_APP")
                .build();

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.VENMO)
                .url(venmoBaseURL)
                .returnUrlScheme(braintreeClient.getReturnUrlScheme());

        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
        braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-links.started", payPalContextId, linkType, isVaultRequest);
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
     * @param context  Android Context
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
