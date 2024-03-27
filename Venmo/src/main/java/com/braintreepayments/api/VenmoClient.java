package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONObject;

import java.util.Objects;

/**
 * Used to create and tokenize Venmo accounts. For more information see the <a
 * href="https://developer.paypal.com/braintree/docs/guides/venmo/overview">documentation</a>
 */
public class VenmoClient {

    private final BraintreeClient braintreeClient;
    private final VenmoApi venmoApi;
    private final VenmoSharedPrefsWriter sharedPrefsWriter;
    private final DeviceInspector deviceInspector;

    /**
     * Used for linking events from the client to server side request
     * In the Venmo flow this will be a Payment Context ID
     */
    private String payPalContextId = null;

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
     * Start the Pay With Venmo flow. This will return a {@link VenmoPaymentAuthRequestParams} that
     * will be used to authenticate the user by switching to the Venmo app or mobile browser in
     * {@link VenmoLauncher#launch(ComponentActivity, VenmoPaymentAuthRequest.ReadyToLaunch)}
     *
     * @param context  Android Context
     * @param request  {@link VenmoRequest}
     * @param callback {@link VenmoPaymentAuthRequestCallback}
     */
    public void createPaymentAuthRequest(@NonNull final Context context,
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
                            if (paymentContextId != null && !paymentContextId.isEmpty()) {
                                payPalContextId = paymentContextId;
                            }
                            createPaymentAuthRequest(context, request, configuration,
                                    braintreeClient.getAuthorization(), finalVenmoProfileId,
                                    paymentContextId, callback);
                        } else {
                            callbackPaymentAuthFailure(callback, new VenmoPaymentAuthRequest.Failure(exception));
                        }
                    });
        });
    }

    private void createPaymentAuthRequest(
            final Context context,
            final VenmoRequest request,
            final Configuration configuration,
            Authorization authorization,
            final String venmoProfileId,
            @Nullable final String paymentContextId,
            VenmoPaymentAuthRequestCallback callback
    ) {
        boolean isClientTokenAuth = (authorization instanceof ClientToken);
        boolean shouldVault = request.getShouldVault() && isClientTokenAuth;
        sharedPrefsWriter.persistVenmoVaultOption(context, shouldVault);

        JSONObject braintreeData = new MetadataBuilder()
                .sessionId(braintreeClient.getSessionId())
                .integration(braintreeClient.getIntegrationType())
                .version()
                .build();

        String applicationName =
                context.getPackageManager().getApplicationLabel(context.getApplicationInfo())
                        .toString();

        Uri venmoBaseURL = Uri.parse("https://venmo.com/go/checkout")
                .buildUpon()
                .appendQueryParameter("x-success", braintreeClient.getReturnUrlScheme() + "://x-callback-url/vzero/auth/venmo/success")
                .appendQueryParameter("x-error", braintreeClient.getReturnUrlScheme() + "://x-callback-url/vzero/auth/venmo/error")
                .appendQueryParameter("x-cancel", braintreeClient.getReturnUrlScheme() + "://x-callback-url/vzero/auth/venmo/cancel")
                .appendQueryParameter("x-source", applicationName)
                .appendQueryParameter("braintree_merchant_id", venmoProfileId)
                .appendQueryParameter("braintree_access_token", configuration.getVenmoAccessToken())
                .appendQueryParameter("braintree_environment", configuration.getVenmoEnvironment())
                .appendQueryParameter("resource_id", paymentContextId)
                .appendQueryParameter("braintree_sdk_data", braintreeData.toString())
                .appendQueryParameter("customerClient", "MOBILE_APP")
                .build();

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.VENMO)
                .url(venmoBaseURL)
                .returnUrlScheme(braintreeClient.getReturnUrlScheme());
        VenmoPaymentAuthRequestParams params = new VenmoPaymentAuthRequestParams(
                browserSwitchOptions);

        callback.onVenmoPaymentAuthRequest(new VenmoPaymentAuthRequest.ReadyToLaunch(params));
    }

    /**
     * After successfully authenticating a Venmo user account via {@link 
     * VenmoClient#createPaymentAuthRequest(Context, VenmoRequest, VenmoPaymentAuthRequestCallback)},
     * this method should be invoked to tokenize the account to retrieve a
     * {@link VenmoAccountNonce}.
     * 
     * @param paymentAuthResult the result of {@link VenmoLauncher#handleReturnToApp(VenmoPendingRequest.Started, Intent)}
     * @param callback a {@link VenmoInternalCallback} to receive a {@link VenmoAccountNonce} or
     *                 error from Venmo tokenization
     */
    public void tokenize(@NonNull final VenmoPaymentAuthResult.Success paymentAuthResult,
                         @NonNull VenmoTokenizeCallback callback) {
            VenmoPaymentAuthResultInfo venmoPaymentAuthResultInfo = paymentAuthResult.getPaymentAuthInfo();
            BrowserSwitchResultInfo browserSwitchResultInfo = venmoPaymentAuthResultInfo.getBrowserSwitchResultInfo();

        Uri deepLinkUri = browserSwitchResultInfo.getDeepLinkUrl();
        if (deepLinkUri != null) {
            braintreeClient.sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_SUCCEEDED);
            if (Objects.requireNonNull(deepLinkUri.getPath()).contains("success")) {
                String paymentContextId = parseResourceId(String.valueOf(deepLinkUri));
                String paymentMethodNonce = parsePaymentMethodNonce(String.valueOf(deepLinkUri));
                String username = parseUsername(String.valueOf(deepLinkUri));

                final boolean isClientTokenAuth = (braintreeClient.getAuthorization() instanceof ClientToken);

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

                } else if (paymentMethodNonce != null && username != null) {
                    boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(
                            braintreeClient.getApplicationContext());
                    if (shouldVault && isClientTokenAuth) {
                        vaultVenmoAccountNonce(paymentMethodNonce, (venmoAccountNonce, error) -> {
                            if (venmoAccountNonce != null) {
                                callbackSuccess(callback, new VenmoResult.Success(venmoAccountNonce));
                            } else if (error != null) {
                                callbackTokenizeFailure(callback, new VenmoResult.Failure(error));
                            }
                        });
                    } else {
                        VenmoAccountNonce venmoAccountNonce =
                                new VenmoAccountNonce(paymentMethodNonce, username, false);
                        callbackSuccess(callback, new VenmoResult.Success(venmoAccountNonce));
                    }
                }
            } else if (deepLinkUri.getPath().contains("cancel")) {
                callbackTokenizeCancel(callback);
            } else if (deepLinkUri.getPath().contains("error")) {
                callbackTokenizeFailure(callback, new VenmoResult.Failure(new Exception("Error returned from Venmo.")));
            }
        } else {
            callbackTokenizeFailure(callback, new VenmoResult.Failure(new Exception("Unknown error")));
        }
    }

    private String parseResourceId(String deepLinkUri) {
        String resourceIdFromBrowserSwitch = Uri.parse(deepLinkUri).getQueryParameter("resource_id");
        if (resourceIdFromBrowserSwitch != null) {
            return resourceIdFromBrowserSwitch;
        } else {
            String cleanedAppSwitchUri = deepLinkUri.replaceFirst("&","?");
            return Uri.parse(cleanedAppSwitchUri).getQueryParameter("resource_id");
        }
    }

    private String parsePaymentMethodNonce(String deepLinkUri) {
        String paymentMethodNonceFromBrowserSwitch = Uri.parse(deepLinkUri).getQueryParameter("payment_method_nonce");
        if (paymentMethodNonceFromBrowserSwitch != null) {
            return paymentMethodNonceFromBrowserSwitch;
        } else {
            String cleanedAppSwitchUri = deepLinkUri.replaceFirst("&","?");
            return Uri.parse(cleanedAppSwitchUri).getQueryParameter("payment_method_nonce");
        }
    }

    private String parseUsername(String deepLinkUri) {
        String usernameFromBrowserSwitch = Uri.parse(deepLinkUri).getQueryParameter("username");
        if (usernameFromBrowserSwitch != null) {
            return usernameFromBrowserSwitch;
        } else {
            String cleanedAppSwitchUri = deepLinkUri.replaceFirst("&","?");
            return Uri.parse(cleanedAppSwitchUri).getQueryParameter("username");
        }
    }

    private void vaultVenmoAccountNonce(String nonce, final VenmoInternalCallback callback) {
        venmoApi.vaultVenmoAccountNonce(nonce, callback);
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
    // TODO: How should this method behave with web fallback enabled?
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
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, payPalContextId);
        callback.onVenmoPaymentAuthRequest(request);
    }

    private void callbackSuccess(VenmoTokenizeCallback callback, VenmoResult venmoResult) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED, payPalContextId);
        callback.onVenmoResult(venmoResult);
    }

    private void callbackTokenizeCancel(VenmoTokenizeCallback callback) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_CANCELED, payPalContextId);
        callback.onVenmoResult(VenmoResult.Cancel.INSTANCE);
    }
    
    private void callbackTokenizeFailure(VenmoTokenizeCallback callback, VenmoResult venmoResult) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, payPalContextId);
        callback.onVenmoResult(venmoResult);
    }
}
