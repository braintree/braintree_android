package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

/**
 * Used to create and tokenize Venmo accounts. For more information see the <a
 * href="https://developer.paypal.com/braintree/docs/guides/venmo/overview">documentation</a>
 */
public class VenmoClient {

    static final String VENMO_PACKAGE_NAME = "com.venmo";
    private final BraintreeClient braintreeClient;
    private final VenmoApi venmoApi;
    private final VenmoSharedPrefsWriter sharedPrefsWriter;
    private final DeviceInspector deviceInspector;

    private VenmoClient(BraintreeClient braintreeClient, ApiClient apiClient) {
        this(braintreeClient, new VenmoApi(braintreeClient, apiClient),
                new VenmoSharedPrefsWriter(), new DeviceInspector());
    }

    /**
     * Initializes a new {@link VenmoClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public VenmoClient(@NonNull Context context, @NonNull String authorization) {
        this(new BraintreeClient(context, authorization));
    }

    @VisibleForTesting
    VenmoClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new ApiClient(braintreeClient));
    }

    @VisibleForTesting
    VenmoClient(BraintreeClient braintreeClient, VenmoApi venmoApi,
                VenmoSharedPrefsWriter sharedPrefsWriter, DeviceInspector deviceInspector) {
        this.braintreeClient = braintreeClient;
        this.sharedPrefsWriter = sharedPrefsWriter;
        this.deviceInspector = deviceInspector;
        this.venmoApi = venmoApi;
    }

    /**
     * Launches an Android Intent pointing to the Venmo app on the Google Play Store
     *
     * @param activity used to open the Venmo's Google Play Store
     */
    public void showVenmoInGooglePlayStore(@NonNull FragmentActivity activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://play.google.com/store/apps/details?id=" + VENMO_PACKAGE_NAME));
        activity.startActivity(intent);
    }


    /**
     * Start the Pay With Venmo flow. This will return a {@link VenmoPaymentAuthRequestParams} that will be
     * used to authenticate the user by switching to the Venmo app in {@link 
     * VenmoLauncher#launch(VenmoPaymentAuthRequest.ReadyToLaunch)}
     * <p>
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to
     * {@link VenmoPaymentAuthRequestCallback#onVenmoPaymentAuthRequest(VenmoPaymentAuthRequest)}
     *
     * @param activity Android FragmentActivity
     * @param request  {@link VenmoRequest}
     * @param callback {@link VenmoPaymentAuthRequestCallback}
     */
    public void createPaymentAuthRequest(@NonNull final FragmentActivity activity,
                                         @NonNull final VenmoRequest request,
                                         @NonNull VenmoPaymentAuthRequestCallback callback) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_STARTED);
        braintreeClient.getConfiguration((configuration, error) -> {
            if (configuration == null && error != null) {
                callbackPaymentAuthFailure(callback, new VenmoPaymentAuthRequest.Failure(error));
                return;
            }

            if (!configuration.isVenmoEnabled()) {
                callbackPaymentAuthFailure(callback,
                        new VenmoPaymentAuthRequest.Failure(new AppSwitchNotAvailableException("Venmo is not enabled")));
                return;
            }
            if (!deviceInspector.isVenmoAppSwitchAvailable(activity)) {
                braintreeClient.sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_FAILED);
                callbackPaymentAuthFailure(callback,
                        new VenmoPaymentAuthRequest.Failure(new AppSwitchNotAvailableException("Venmo is not installed")));
                return;
            }

            // Merchants are not allowed to collect user addresses unless ECD (Enriched Customer
            // Data) is enabled on the BT Control Panel.
            if ((request.getCollectCustomerShippingAddress() ||
                    request.getCollectCustomerBillingAddress()) &&
                    !configuration.getVenmoEnrichedCustomerDataEnabled()) {
                callbackPaymentAuthFailure(callback, new VenmoPaymentAuthRequest.Failure(new BraintreeException(
                        "Cannot collect customer data when ECD is disabled. Enable this feature " +
                                "in the Control Panel to collect this data.")));
                return;
            }

            String venmoProfileId = request.getProfileId();
            if (TextUtils.isEmpty(venmoProfileId)) {
                venmoProfileId = configuration.getVenmoMerchantId();
            }

            final String finalVenmoProfileId = venmoProfileId;
            venmoApi.createPaymentContext(request, venmoProfileId,
                    (paymentContextId, exception) -> {
                        if (exception == null) {
                            createPaymentAuthRequest(activity, request, configuration,
                                    braintreeClient.getAuthorization(), finalVenmoProfileId,
                                    paymentContextId, callback);
                        } else {
                            callbackPaymentAuthFailure(callback, new VenmoPaymentAuthRequest.Failure(exception));
                        }
                    });
        });
    }

    private void createPaymentAuthRequest(
            final FragmentActivity activity,
            final VenmoRequest request,
            final Configuration configuration,
            Authorization authorization,
            final String venmoProfileId,
            @Nullable final String paymentContextId,
            VenmoPaymentAuthRequestCallback callback
    ) {
        boolean isClientTokenAuth = (authorization instanceof ClientToken);
        boolean shouldVault = request.getShouldVault() && isClientTokenAuth;
        sharedPrefsWriter.persistVenmoVaultOption(activity, shouldVault);
        VenmoPaymentAuthRequestParams params =
                new VenmoPaymentAuthRequestParams(configuration, venmoProfileId, paymentContextId,
                        braintreeClient.getSessionId(), braintreeClient.getIntegrationType());
        callback.onVenmoPaymentAuthRequest(new VenmoPaymentAuthRequest.ReadyToLaunch(params));
    }

    /**
     * After successfully authenticating a Venmo user account via {@link 
     * VenmoClient#createPaymentAuthRequest(FragmentActivity, VenmoRequest, VenmoPaymentAuthRequestCallback)},
     * this method should be invoked to tokenize the account to retrieve a
     * {@link VenmoAccountNonce}.
     * 
     * @param venmoPaymentAuthResult the result of {@link VenmoLauncher#launch(VenmoPaymentAuthRequest.ReadyToLaunch)}
     * @param callback a {@link VenmoInternalCallback} to receive a {@link VenmoAccountNonce} or
     *                 error from Venmo tokenization
     */
    public void tokenize(@NonNull final VenmoPaymentAuthResult venmoPaymentAuthResult,
                         @NonNull VenmoTokenizeCallback callback) {
        if (venmoPaymentAuthResult.getError() == null) {
            braintreeClient.sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_SUCCEEDED);

            final boolean isClientTokenAuth = (braintreeClient.getAuthorization() instanceof ClientToken);

            String paymentContextId = venmoPaymentAuthResult.getPaymentContextId();
            if (paymentContextId != null) {
                venmoApi.createNonceFromPaymentContext(paymentContextId, (nonce, error) -> {
                    if (nonce != null) {
                        boolean shouldVault =
                                sharedPrefsWriter.getVenmoVaultOption(
                                        braintreeClient.getApplicationContext());
                        if (shouldVault && isClientTokenAuth) {
                            vaultVenmoAccountNonce(nonce.getString(),
                                    (venmoAccountNonce, vaultError) -> {
                                        if (venmoAccountNonce != null) {
                                            callbackSuccess(callback, new VenmoResult.Success(venmoAccountNonce));
                                        } else if (vaultError != null) {
                                            callbackTokenizeFailure(callback, new VenmoResult.Failure(vaultError));
                                        }
                                    });
                        } else {
                            callbackSuccess(callback, new VenmoResult.Success(nonce));
                        }
                    } else if (error != null) {
                        callbackTokenizeFailure(callback, new VenmoResult.Failure(error));
                    }
                });
            } else {
                String nonce = venmoPaymentAuthResult.getVenmoAccountNonce();

                boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(
                        braintreeClient.getApplicationContext());
                if (shouldVault && isClientTokenAuth) {
                    vaultVenmoAccountNonce(nonce, (venmoAccountNonce, error) -> {
                        if (venmoAccountNonce != null) {
                            callbackSuccess(callback, new VenmoResult.Success(venmoAccountNonce));
                        } else if (error != null) {
                            callbackTokenizeFailure(callback, new VenmoResult.Failure(error));
                        }
                    });
                } else {
                    String venmoUsername = venmoPaymentAuthResult.getVenmoUsername();
                    VenmoAccountNonce venmoAccountNonce =
                            new VenmoAccountNonce(nonce, venmoUsername, false);
                    callbackSuccess(callback, new VenmoResult.Success(venmoAccountNonce));
                }

            }
        } else if (venmoPaymentAuthResult.getError() != null) {
            if (venmoPaymentAuthResult.getError() instanceof UserCanceledException) {
                callbackTokenizeCancel(callback);
            } else {
                callbackTokenizeFailure(callback, new VenmoResult.Failure(venmoPaymentAuthResult.getError()));
            }
        }
    }

    private void vaultVenmoAccountNonce(String nonce, final VenmoInternalCallback callback) {
        venmoApi.vaultVenmoAccountNonce(nonce, (venmoAccountNonce, error) -> callback.onResult(venmoAccountNonce, error));
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
     * Before starting the Venmo flow, use this method to check whether Venmo is supported and set
     * up on the device. When the callback is called with {@code true}, show the Venmo button. When
     * it is called with {@code false}, display other checkout options.
     *
     * @param context  Android Context
     * @param callback {@link VenmoIsReadyToPayCallback}
     */
    public void isReadyToPay(final Context context, final VenmoIsReadyToPayCallback callback) {
        braintreeClient.getConfiguration((configuration, configError) -> {
            if (configuration != null) {
                boolean isReadyToPay =
                        configuration.isVenmoEnabled() && isVenmoAppSwitchAvailable(context);
                if (isReadyToPay) {
                    callback.onVenmoReadinessResult(VenmoReadinessResult.ReadyToPay.INSTANCE);
                } else {
                   callback.onVenmoReadinessResult(VenmoReadinessResult.NotReadyToPay.INSTANCE);
                }
            } else if (configError != null) {
                callback.onVenmoReadinessResult(new VenmoReadinessResult.Failure(configError));
            }
        });
    }

    private void callbackPaymentAuthFailure(VenmoPaymentAuthRequestCallback callback, VenmoPaymentAuthRequest request) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED);
        callback.onVenmoPaymentAuthRequest(request);
    }

    private void callbackSuccess(VenmoTokenizeCallback callback, VenmoResult venmoResult) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED);
        callback.onVenmoResult(venmoResult);
    }

    private void callbackTokenizeCancel(VenmoTokenizeCallback callback) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_CANCELED);
        callback.onVenmoResult(VenmoResult.Cancel.INSTANCE);
    }
    
    private void callbackTokenizeFailure(VenmoTokenizeCallback callback, VenmoResult venmoResult) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED);
        callback.onVenmoResult(venmoResult);
    }
}
