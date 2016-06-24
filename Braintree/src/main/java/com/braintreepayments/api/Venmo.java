package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.internal.AppHelper;
import com.braintreepayments.api.internal.SignatureVerification;
import com.braintreepayments.api.models.BraintreeMetadataBuilder;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.models.VenmoConfiguration;

import org.json.JSONObject;

/**
 * Class containing Venmo specific logic.
 */
public class Venmo {

    static final String EXTRA_MERCHANT_ID = "com.braintreepayments.api.MERCHANT_ID";
    static final String EXTRA_ACCESS_TOKEN = "com.braintreepayments.api.ACCESS_TOKEN";
    static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.ENVIRONMENT";
    static final String EXTRA_METADATA = "com.braintreepayments.api.METADATA";
    static final String EXTRA_PAYMENT_METHOD_NONCE = "com.braintreepayments.api.EXTRA_PAYMENT_METHOD_NONCE";
    static final String EXTRA_USERNAME = "com.braintreepayments.api.EXTRA_USER_NAME";

    static final String PACKAGE_NAME = "com.venmo";
    static final String APP_SWITCH_ACTIVITY = "controller.SetupMerchantActivity";
    static final String CERTIFICATE_SUBJECT = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    static final String CERTIFICATE_ISSUER = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    static final int PUBLIC_KEY_HASH_CODE = -129711843;

    static final int VENMO_REQUEST_CODE = 13488;

    /**
     * @param context A context to access the installed packages.
     * @return boolean depending on if the Venmo app is installed, and has a valid signature.
     */
    public static boolean isVenmoInstalled(Context context) {
        return AppHelper.isIntentAvailable(context, getVenmoIntent()) &&
                SignatureVerification.isSignatureValid(context, PACKAGE_NAME, CERTIFICATE_SUBJECT, CERTIFICATE_ISSUER,
                        PUBLIC_KEY_HASH_CODE);
    }

    private static Intent getVenmoIntent() {
        return new Intent().setComponent(new ComponentName(PACKAGE_NAME, PACKAGE_NAME + "." + APP_SWITCH_ACTIVITY));
    }

    static Intent getLaunchIntent(VenmoConfiguration venmoConfiguration, String integrationType, String sessionId) {
        JSONObject meta = new BraintreeMetadataBuilder()
                .sessionId(sessionId)
                .integration(integrationType)
                .version()
                .platform()
                .build();

        Intent venmoIntent = getVenmoIntent()
                .putExtra(EXTRA_MERCHANT_ID, venmoConfiguration.getMerchantId())
                .putExtra(EXTRA_ACCESS_TOKEN, venmoConfiguration.getAccessToken())
                .putExtra(EXTRA_ENVIRONMENT, venmoConfiguration.getEnvironment());

        if (meta != null) {
            venmoIntent.putExtra(EXTRA_METADATA, meta.toString());
        }

        return venmoIntent;
    }

    /**
     * Start the Pay With Venmo flow. This will app switch to the Venmo app.
     * <p/>
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to
     * {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}.
     *
     * @param fragment {@link BraintreeFragment}
     */
    public static void authorizeAccount(final BraintreeFragment fragment) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                authorizeAccount(fragment, configuration.getPayWithVenmo());
            }
        });
    }

    static void authorizeAccount(BraintreeFragment fragment, VenmoConfiguration venmoConfiguration) {
        fragment.sendAnalyticsEvent("pay-with-venmo.selected");

        String exceptionMessage = "";
        if(!venmoConfiguration.isAccessTokenValid()) {
            exceptionMessage = "Venmo is not enabled";
        } else if (!Venmo.isVenmoInstalled(fragment.getApplicationContext())) {
            exceptionMessage = "Venmo is not installed";
        } else if (!venmoConfiguration.isVenmoWhitelisted(fragment.getApplicationContext().getContentResolver())) {
            exceptionMessage = "Venmo is not whitelisted";
        }

        if(!TextUtils.isEmpty(exceptionMessage)) {
            fragment.postCallback(new AppSwitchNotAvailableException(exceptionMessage));
            fragment.sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
        } else {
            fragment.startActivityForResult(
                    Venmo.getLaunchIntent(venmoConfiguration, fragment.getIntegrationType(), fragment.getSessionId()),
                    VENMO_REQUEST_CODE);
            fragment.sendAnalyticsEvent("pay-with-venmo.app-switch.started");
        }
    }

    static void onActivityResult(final BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String nonce = data.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);
            String venmoUsername = data.getStringExtra(EXTRA_USERNAME);
            VenmoAccountNonce venmoAccountNonce =
                    new VenmoAccountNonce(nonce, venmoUsername, venmoUsername);
            fragment.postCallback(venmoAccountNonce);
            fragment.sendAnalyticsEvent("pay-with-venmo.app-switch.success");
        } else if (resultCode == Activity.RESULT_CANCELED) {
            fragment.sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
        }
    }
}
