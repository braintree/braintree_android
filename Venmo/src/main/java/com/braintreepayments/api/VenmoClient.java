package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

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

    private final BraintreeClient braintreeClient;
    private final TokenizationClient tokenizationClient;
    private final VenmoSharedPrefsWriter sharedPrefsWriter;
    private final DeviceInspector deviceInspector;

    public VenmoClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new TokenizationClient(braintreeClient), new VenmoSharedPrefsWriter(), new DeviceInspector());
    }

    @VisibleForTesting
    VenmoClient(BraintreeClient braintreeClient, TokenizationClient tokenizationClient, VenmoSharedPrefsWriter sharedPrefsWriter, DeviceInspector deviceInspector) {
       this.braintreeClient = braintreeClient;
       this.tokenizationClient = tokenizationClient;
       this.sharedPrefsWriter = sharedPrefsWriter;
       this.deviceInspector = deviceInspector;
    }

    /**
     * Launches an Android Intent pointing to the Venmo app on the Google Play Store
     *
     * @param activity used to open the Venmo's Google Play Store
     */
    public void showVenmoInGooglePlayStore(FragmentActivity activity) {
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
     * @param activity Android FragmentActivity
     * @param request {@link VenmoRequest}
     * @param callback {@link VenmoTokenizeAccountCallback}
     */
    public void tokenizeVenmoAccount(final FragmentActivity activity, final VenmoRequest request, final VenmoTokenizeAccountCallback callback) {
        braintreeClient.sendAnalyticsEvent("pay-with-venmo.selected");
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
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

                sharedPrefsWriter.persistVenmoVaultOption(activity, request.getShouldVault() && braintreeClient.getAuthorization() instanceof ClientToken);

                String venmoProfileId = request.getProfileId();
                if (TextUtils.isEmpty(venmoProfileId)) {
                    venmoProfileId = configuration.getVenmoMerchantId();
                }
                activity.startActivityForResult(getLaunchIntent(configuration, venmoProfileId),
                        BraintreeRequestCodes.VENMO);
                braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.started");
            }
        });
    }

    /**
     * @param context Android Context
     * @param resultCode a code associated with the Activity result
     * @param data Android Intent
     * @param callback {@link VenmoOnActivityResultCallback}
     */
    public void onActivityResult(final Context context, int resultCode, Intent data, final VenmoOnActivityResultCallback callback) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.success");
            String nonce = data.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);

            boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(context);
            boolean isClientToken = braintreeClient.getAuthorization() instanceof ClientToken;

            if (shouldVault && isClientToken) {
                VenmoAccount venmoAccountBuilder = new VenmoAccount();
                venmoAccountBuilder.setNonce(nonce);
                tokenizationClient.tokenize(venmoAccountBuilder, new PaymentMethodNonceCallback() {
                    @Override
                    public void success(PaymentMethodNonce paymentMethodNonce) {
                        callback.onResult((VenmoAccountNonce) paymentMethodNonce, null);
                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.vault.success");
                    }

                    @Override
                    public void failure(Exception exception) {
                        callback.onResult(null, exception);
                        braintreeClient.sendAnalyticsEvent("pay-with-venmo.vault.failed");
                    }
                });
            } else {
                String venmoUsername = data.getStringExtra(EXTRA_USERNAME);
                VenmoAccountNonce venmoAccountNonce = new VenmoAccountNonce(nonce, venmoUsername, venmoUsername);
                callback.onResult(venmoAccountNonce, null);
            }
        } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
        }
    }

    private static Intent getVenmoIntent() {
        return new Intent().setComponent(new ComponentName(VENMO_PACKAGE_NAME, VENMO_PACKAGE_NAME + "." + APP_SWITCH_ACTIVITY));
    }

    private Intent getLaunchIntent(Configuration configuration, String profileId) {
        Intent venmoIntent = getVenmoIntent()
                .putExtra(EXTRA_MERCHANT_ID, profileId)
                .putExtra(EXTRA_ACCESS_TOKEN, configuration.getVenmoAccessToken())
                .putExtra(EXTRA_ENVIRONMENT, configuration.getVenmoEnvironment());

        try {
            JSONObject braintreeData = new JSONObject();

            JSONObject meta = new MetadataBuilder()
                    .sessionId(braintreeClient.getSessionId())
                    .integration(braintreeClient.getIntegrationType())
                    .version()
                    .build();

            braintreeData.put(META_KEY, meta);
            venmoIntent.putExtra(EXTRA_BRAINTREE_DATA, braintreeData.toString());
        } catch (JSONException ignored) {}

        return venmoIntent;
    }

    /**
     * Check if Venmo app switch is available.
     *
     * @param context Application Context
     * @return true if the Venmo app is installed, false otherwise
     */
    public boolean isVenmoAppSwitchAvailable(Context context) {
        return deviceInspector.isVenmoAppSwitchAvailable(context);
    }
}
