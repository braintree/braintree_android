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
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to create and tokenize Venmo accounts. For more information see the <a href="https://developers.braintreepayments.com/guides/venmo/overview">documentation</a>
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
    private final ApiClient apiClient;
    private final VenmoSharedPrefsWriter sharedPrefsWriter;
    private final DeviceInspector deviceInspector;

    public VenmoClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new ApiClient(braintreeClient), new VenmoSharedPrefsWriter(), new DeviceInspector());
    }

    @VisibleForTesting
    VenmoClient(BraintreeClient braintreeClient, ApiClient apiClient, VenmoSharedPrefsWriter sharedPrefsWriter, DeviceInspector deviceInspector) {
        this.braintreeClient = braintreeClient;
        this.apiClient = apiClient;
        this.sharedPrefsWriter = sharedPrefsWriter;
        this.deviceInspector = deviceInspector;
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
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to {@link VenmoTokenizeAccountCallback#onResult(Exception)}
     *
     * @param activity Android FragmentActivity
     * @param request  {@link VenmoRequest}
     * @param callback {@link VenmoTokenizeAccountCallback}
     */
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

                JSONObject params = new JSONObject();
                try {
                    params.put("query", "mutation CreateVenmoPaymentContext($input: CreateVenmoPaymentContextInput!) { createVenmoPaymentContext(input: $input) { venmoPaymentContext { id } } }");
                    JSONObject input = new JSONObject();
                    input.put("paymentMethodUsage", request.getPaymentMethodUsageAsString());
                    input.put("merchantProfileId", venmoProfileId);
                    input.put("customerClient", "MOBILE_APP");
                    input.put("intent", "CONTINUE");

                    input.putOpt("displayName", request.getDisplayName());

                    JSONObject variables = new JSONObject();
                    variables.put("input", input);
                    params.put("variables", variables);
                } catch (JSONException e) {
                    callback.onResult(new BraintreeException("unexpected error"));
                }

                final String finalVenmoProfileId = venmoProfileId;
                braintreeClient.sendGraphQLPOST(params.toString(), new HttpResponseCallback() {

                    @Override
                    public void onResult(String responseBody, Exception httpError) {
                        if (responseBody != null) {
                            String paymentContextId = parsePaymentContextId(responseBody);
                            if (TextUtils.isEmpty(paymentContextId)) {
                                braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
                                callback.onResult(new BraintreeException("Failed to fetch a Venmo paymentContextId while constructing the requestURL."));
                                return;
                            }
                            startVenmoActivityForResult(activity, request, configuration, finalVenmoProfileId, paymentContextId);
                        } else {
                            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
                            callback.onResult(httpError);
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

                    Intent launchIntent = getLaunchIntent(configuration, venmoProfileId, paymentContextId);
                    activity.startActivityForResult(launchIntent, BraintreeRequestCodes.VENMO);
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.started");
                }
            }
        });
    }

    private static String parsePaymentContextId(String createPaymentContextResponse) {
        String paymentContextId = null;
        try {
            JSONObject data = new JSONObject(createPaymentContextResponse).getJSONObject("data");
            JSONObject createVenmoPaymentContext = data.getJSONObject("createVenmoPaymentContext");
            JSONObject venmoPaymentContext = createVenmoPaymentContext.getJSONObject("venmoPaymentContext");
            paymentContextId = venmoPaymentContext.getString("id");
        } catch (JSONException ignored) { /* do nothing */ }

        return paymentContextId;
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
                            JSONObject params = new JSONObject();
                            try {
                                params.put("query", "query PaymentContext($id: ID!) { node(id: $id) { ... on VenmoPaymentContext { paymentMethodId userName payerInfo { firstName lastName phoneNumber email externalId userName } } } }");
                                JSONObject variables = new JSONObject();
                                variables.put("id", paymentContextId);
                                params.put("variables", variables);

                                braintreeClient.sendGraphQLPOST(params.toString(), new HttpResponseCallback() {

                                    @Override
                                    public void onResult(String responseBody, Exception httpError) {
                                        if (responseBody != null) {
                                            try {
                                                JSONObject data = new JSONObject(responseBody).getJSONObject("data");
                                                VenmoAccountNonce nonce = VenmoAccountNonce.fromJSON(data.getJSONObject("node"));

                                                boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(context);
                                                if (shouldVault && isClientTokenAuth) {
                                                    vaultVenmoAccountNonce(nonce.getString(), callback);
                                                } else {
                                                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
                                                    callback.onResult(nonce, null);
                                                }
                                            } catch (JSONException exception) {
                                                braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
                                                callback.onResult(null, exception);
                                            }
                                        } else {
                                            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failure");
                                            callback.onResult(null, httpError);
                                        }
                                    }
                                });

                            } catch (JSONException exception) {
                                callback.onResult(null, exception);
                            }

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

    private void vaultVenmoAccountNonce(String nonce, final VenmoOnActivityResultCallback callback) {
        VenmoAccount venmoAccount = new VenmoAccount();
        venmoAccount.setNonce(nonce);

        apiClient.tokenizeREST(venmoAccount, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (tokenizationResponse != null) {
                    try {
                        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(tokenizationResponse);
                        callback.onResult(venmoAccountNonce, null);
                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.vault.success");
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    callback.onResult(null, exception);
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.vault.failed");
                }
            }
        });
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
