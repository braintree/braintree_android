package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.MetadataBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VenmoAccountBuilder;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.models.VenmoConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to create and tokenize Venmo accounts. For more information see the <a href="https://developers.braintreepayments.com/guides/venmo/overview">documentation</a>
 */
// TODO: Rename class when API is finalized
public class Venmo {

    static final String VENMO_PACKAGE_NAME = "com.venmo";
    static final String APP_SWITCH_ACTIVITY = "controller.SetupMerchantActivity";
    static final String META_KEY = "_meta";
    static final String EXTRA_MERCHANT_ID = "com.braintreepayments.api.MERCHANT_ID";
    static final String EXTRA_ACCESS_TOKEN = "com.braintreepayments.api.ACCESS_TOKEN";
    static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.ENVIRONMENT";
    static final String EXTRA_BRAINTREE_DATA = "com.braintreepayments.api.EXTRA_BRAINTREE_DATA";
    static final String EXTRA_PAYMENT_METHOD_NONCE = "com.braintreepayments.api.EXTRA_PAYMENT_METHOD_NONCE";
    static final String EXTRA_USERNAME = "com.braintreepayments.api.EXTRA_USER_NAME";

    private BraintreeClient braintreeClient;
    private TokenizationClient tokenizationClient;
    private VenmoSharedPrefsWriter sharedPrefsWriter;
    private DeviceInspector deviceInspector;

    public Venmo(BraintreeClient braintreeClient, TokenizationClient tokenizationClient) {
        this(braintreeClient, tokenizationClient, new VenmoSharedPrefsWriter(), new DeviceInspector());
    }

    public Venmo(BraintreeClient braintreeClient, TokenizationClient tokenizationClient, VenmoSharedPrefsWriter sharedPrefsWriter, DeviceInspector deviceInspector) {
       this.braintreeClient = braintreeClient;
       this.tokenizationClient = tokenizationClient;
       this.sharedPrefsWriter = sharedPrefsWriter;
       this.deviceInspector = deviceInspector;
    }

    /**
     * Launches an {@link Intent} pointing to the Venmo app on the Google Play Store
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
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to {@link VenmoAuthorizeAccountCallback#onResult(boolean, Exception)}
     *
     * @param activity {@link FragmentActivity}
     * @param vault If true, and you are using Client Token authorization with a customer ID, this payment method will
     * be added to your customer's vault. @see <a href="https://developers.braintreepayments.com/guides/authorization/overview">our
     * docs on client authorization</a> for more info.
     * @param profileId The Venmo profile ID to be used during payment authorization. Customers will see the business
     * name and logo associated with this Venmo profile, and it will show up in the Venmo app as a "Connected Merchant".
     * Venmo profile IDs can be found in the Braintree Control Panel. Passing `null` will use the default Venmo profile.
     * @param callback {@link VenmoAuthorizeAccountCallback}
     */
    public void authorizeAccount(final FragmentActivity activity, final boolean vault, final String profileId, final VenmoAuthorizeAccountCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                braintreeClient.sendAnalyticsEvent("pay-with-venmo.selected");

                String venmoProfileId = profileId;
                if (TextUtils.isEmpty(venmoProfileId)) {
                    venmoProfileId = configuration.getPayWithVenmo().getMerchantId();
                }

                String exceptionMessage = "";
                if (!configuration.getPayWithVenmo().isAccessTokenValid()) {
                    exceptionMessage = "Venmo is not enabled";
                } else if (!deviceInspector.isVenmoAppSwitchAvailable(activity)) {
                    exceptionMessage = "Venmo is not installed";
                }

                if (!TextUtils.isEmpty(exceptionMessage)) {
                    callback.onResult(false, new AppSwitchNotAvailableException(exceptionMessage));
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
                } else {
                    sharedPrefsWriter.persistVenmoVaultOption(activity, vault && braintreeClient.getAuthorization() instanceof ClientToken);

                    activity.startActivityForResult(getLaunchIntent(activity, configuration.getPayWithVenmo(), venmoProfileId),
                            BraintreeRequestCodes.VENMO);
                    braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.started");
                }
            }
        });
    }

    public void onActivityResult(final Context context, int resultCode, Intent data, final VenmoOnActivityResultCallback callback) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            braintreeClient.sendAnalyticsEvent("pay-with-venmo.app-switch.success");
            String nonce = data.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);

            boolean shouldVault = sharedPrefsWriter.getVenmoVaultOption(context);
            boolean isClientToken = braintreeClient.getAuthorization() instanceof ClientToken;

            if (shouldVault && isClientToken) {
                VenmoAccountBuilder vaultBuilder = new VenmoAccountBuilder()
                    .nonce(nonce);
                tokenizationClient.tokenize(vaultBuilder, new PaymentMethodNonceCallback() {
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

    private Intent getLaunchIntent(Context context, VenmoConfiguration venmoConfiguration, String profileId) {
        Intent venmoIntent = getVenmoIntent()
                .putExtra(EXTRA_MERCHANT_ID, profileId)
                .putExtra(EXTRA_ACCESS_TOKEN, venmoConfiguration.getAccessToken())
                .putExtra(EXTRA_ENVIRONMENT, venmoConfiguration.getEnvironment());

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
}
