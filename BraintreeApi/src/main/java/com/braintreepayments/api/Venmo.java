package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.Nullable;

import com.braintreepayments.api.internal.SignatureVerification;
import com.braintreepayments.api.models.Configuration;

import java.util.List;

public class Venmo {

    public static final String EXTRA_MERCHANT_ID = "com.braintreepayments.api.MERCHANT_ID";
    public static final String EXTRA_PAYMENT_METHOD_NONCE = "com.braintreepayments.api.EXTRA_PAYMENT_METHOD_NONCE";
    public static final String EXTRA_OFFLINE = "com.braintreepayments.api.OFFLINE";

    public static final String VENMO_SOURCE = "venmo-app";

    protected static final String PACKAGE_NAME = "com.venmo";
    protected static final String APP_SWITCH_ACTIVITY = "CardChooserActivity";
    protected static final String CERTIFICATE_SUBJECT = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    protected static final String CERTIFICATE_ISSUER = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    protected static final int PUBLIC_KEY_HASH_CODE = -129711843;

    /**
     * Checks the given {@link Intent} to see if it contains a {@link Venmo#EXTRA_PAYMENT_METHOD_NONCE}.
     *
     * @param intent The {@link Intent} to check.
     * @return {@code true} is the {@link Intent} contains a {@link Venmo#EXTRA_PAYMENT_METHOD_NONCE},
     *         {@code false} otherwise.
     */
    protected static boolean isVenmoAppSwitchResponse(Intent intent) {
        return intent.hasExtra(EXTRA_PAYMENT_METHOD_NONCE);
    }

    /**
     * Extracts the nonce from the given {@link Intent}.
     *
     * @param data the {@link Intent} returned in {@link android.app.Activity#onActivityResult(int, int, Intent)}
     * @return the {@link String} nonce, or {@code null}.
     */
    @Nullable
    protected static String handleAppSwitchResponse(Intent data) {
        if (data != null) {
            return data.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);
        }

        return null;
    }

    protected static boolean isAvailable(Context context, Configuration configuration) {
        if (configuration.getVenmoState().equals("off")) {
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(
                getLaunchIntent(configuration), 0);

        return (activities.size() == 1 &&
                PACKAGE_NAME.equals(activities.get(0).activityInfo.packageName) &&
                SignatureVerification.isSignatureValid(context, PACKAGE_NAME, CERTIFICATE_SUBJECT, CERTIFICATE_ISSUER, PUBLIC_KEY_HASH_CODE));
    }

    protected static Intent getLaunchIntent(Configuration configuration) {
        Intent intent = new Intent()
                .setComponent(new ComponentName(
                        PACKAGE_NAME, PACKAGE_NAME + "." + APP_SWITCH_ACTIVITY))
                .putExtra(EXTRA_MERCHANT_ID, configuration.getMerchantId());

        if (configuration.getVenmoState().equals("offline")) {
            intent.putExtra(EXTRA_OFFLINE, true);
        } else if (configuration.getVenmoState().equals("live")) {
            intent.putExtra(EXTRA_OFFLINE, false);
        }

        return intent;
    }
}
