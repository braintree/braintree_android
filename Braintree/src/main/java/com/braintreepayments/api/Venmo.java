package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.AppHelper;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.internal.SignatureVerification;
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

import androidx.appcompat.app.AppCompatActivity;

/**
 * Used to create and tokenize Venmo accounts. For more information see the <a href="https://developers.braintreepayments.com/guides/venmo/overview">documentation</a>
 */
public class Venmo {

    private static final String VAULT_VENMO_KEY = "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY";

    private static final String META_KEY = "_meta";

    static final String EXTRA_MERCHANT_ID = "com.braintreepayments.api.MERCHANT_ID";
    static final String EXTRA_ACCESS_TOKEN = "com.braintreepayments.api.ACCESS_TOKEN";
    static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.ENVIRONMENT";
    static final String EXTRA_BRAINTREE_DATA = "com.braintreepayments.api.EXTRA_BRAINTREE_DATA";
    static final String EXTRA_PAYMENT_METHOD_NONCE = "com.braintreepayments.api.EXTRA_PAYMENT_METHOD_NONCE";
    static final String EXTRA_USERNAME = "com.braintreepayments.api.EXTRA_USER_NAME";

    static final String PACKAGE_NAME = "com.venmo";
    static final String APP_SWITCH_ACTIVITY = "controller.SetupMerchantActivity";
    static final String CERTIFICATE_SUBJECT = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    static final String CERTIFICATE_ISSUER = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    static final int PUBLIC_KEY_HASH_CODE = -129711843;

    /**
     * @param context A context to access the installed packages.
     * @return boolean depending on if the Venmo app is installed, and has a valid signature.
     */
    public static boolean isVenmoInstalled(Context context) {
        return AppHelper.isIntentAvailable(context, getVenmoIntent()) &&
                SignatureVerification.isSignatureValid(context, PACKAGE_NAME, CERTIFICATE_SUBJECT, CERTIFICATE_ISSUER,
                        PUBLIC_KEY_HASH_CODE);
    }

    /**
     * Launches an {@link Intent} pointing to the Venmo app on the Google Play Store
     *
     * @param fragment used to open the Venmo's Google Play Store
     */
    public static void openVenmoAppPageInGooglePlay(BraintreeFragment fragment) {
        fragment.sendAnalyticsEvent("android.pay-with-venmo.app-store.invoked");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://play.google.com/store/apps/details?id=" + PACKAGE_NAME));
        fragment.startActivity(intent);
    }

    private static Intent getVenmoIntent() {
        return new Intent().setComponent(new ComponentName(PACKAGE_NAME, PACKAGE_NAME + "." + APP_SWITCH_ACTIVITY));
    }

    static Intent getLaunchIntent(VenmoConfiguration venmoConfiguration, String profileId, BraintreeFragment fragment) {
        Intent venmoIntent = getVenmoIntent()
                .putExtra(EXTRA_MERCHANT_ID, profileId)
                .putExtra(EXTRA_ACCESS_TOKEN, venmoConfiguration.getAccessToken())
                .putExtra(EXTRA_ENVIRONMENT, venmoConfiguration.getEnvironment());

        try {
            JSONObject braintreeData = new JSONObject();

            JSONObject meta = new MetadataBuilder()
                    .sessionId(fragment.getSessionId())
                    .integration(fragment.getIntegrationType())
                    .version()
                    .build();

            braintreeData.put(META_KEY, meta);
            venmoIntent.putExtra(EXTRA_BRAINTREE_DATA, braintreeData.toString());
        } catch (JSONException ignored) {}

        return venmoIntent;
    }

    /**
     * Start the Pay With Venmo flow. This will app switch to the Venmo app.
     * <p/>
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to {@link
     * com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}.
     *
     * @param fragment {@link BraintreeFragment}
     * @param vault If true, and you are using Client Token authorization with a customer ID, this payment method will
     * be added to your customer's vault. @see <a href="https://developers.braintreepayments.com/guides/authorization/overview">our
     * docs on client authorization</a> for more info.
     * @param profileId The Venmo profile ID to be used during payment authorization. Customers will see the business
     * name and logo associated with this Venmo profile, and it will show up in the Venmo app as a "Connected Merchant".
     * Venmo profile IDs can be found in the Braintree Control Panel. Passing `null` will use the default Venmo profile.
     */
    public static void authorizeAccount(final BraintreeFragment fragment, final boolean vault, final String profileId) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fragment.sendAnalyticsEvent("pay-with-venmo.selected");

                String venmoProfileId = profileId;
                if (TextUtils.isEmpty(venmoProfileId)) {
                    venmoProfileId = configuration.getPayWithVenmo().getMerchantId();
                }

                String exceptionMessage = "";
                if (!configuration.getPayWithVenmo().isAccessTokenValid()) {
                    exceptionMessage = "Venmo is not enabled";
                } else if (!Venmo.isVenmoInstalled(fragment.getApplicationContext())) {
                    exceptionMessage = "Venmo is not installed";
                }

                if (!TextUtils.isEmpty(exceptionMessage)) {
                    fragment.postCallback(new AppSwitchNotAvailableException(exceptionMessage));
                    fragment.sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
                } else {
                    persistVenmoVaultOption(vault && fragment.getAuthorization() instanceof ClientToken,
                            fragment.getApplicationContext());

                    fragment.startActivityForResult(getLaunchIntent(configuration.getPayWithVenmo(), venmoProfileId, fragment),
                            BraintreeRequestCodes.VENMO);
                    fragment.sendAnalyticsEvent("pay-with-venmo.app-switch.started");
                }
            }
        });
    }

    /**
     * Start the Venmo flow. This will app switch to the Venmo app.
     * <p/>
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to {@link
     * com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}.
     *
     * @param fragment {@link BraintreeFragment}
     * @param vault If true, and you are using Client Token authorization with a customer ID, this payment method will
     * be added to your customer's vault. @see <a href="https://developers.braintreepayments.com/guides/authorization/overview">our
     * docs on client authorization</a> for more info.
     */
    public static void authorizeAccount(final BraintreeFragment fragment, final boolean vault) {
        authorizeAccount(fragment, vault, null);
    }

    public static void authorizeAccount(final BraintreeFragment fragment) {
        authorizeAccount(fragment, false, null);
    }

    private static void persistVenmoVaultOption(boolean shouldVault, Context context) {
        BraintreeSharedPreferences.getSharedPreferences(context).edit()
                .putBoolean(VAULT_VENMO_KEY, shouldVault)
                .apply();
    }

    private static boolean shouldVault(Context context) {
        return BraintreeSharedPreferences.getSharedPreferences(context)
                .getBoolean(VAULT_VENMO_KEY, false);
    }

    static void onActivityResult(final BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            fragment.sendAnalyticsEvent("pay-with-venmo.app-switch.success");
            String nonce = data.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);

            if (shouldVault(fragment.getApplicationContext()) && fragment.getAuthorization() instanceof ClientToken) {
                vault(fragment, nonce);
            } else {
                String venmoUsername = data.getStringExtra(EXTRA_USERNAME);
                VenmoAccountNonce venmoAccountNonce = new VenmoAccountNonce(nonce, venmoUsername, venmoUsername);
                fragment.postCallback(venmoAccountNonce);
            }
        } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            fragment.sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
        }
    }

    private static void vault(final BraintreeFragment fragment, String nonce) {
        VenmoAccountBuilder vaultBuilder = new VenmoAccountBuilder()
                .nonce(nonce);
        TokenizationClient.tokenize(fragment, vaultBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                fragment.postCallback(paymentMethodNonce);
                fragment.sendAnalyticsEvent("pay-with-venmo.vault.success");
            }

            @Override
            public void failure(Exception exception) {
                fragment.postCallback(exception);
                fragment.sendAnalyticsEvent("pay-with-venmo.vault.failed");
            }
        });
    }
}
