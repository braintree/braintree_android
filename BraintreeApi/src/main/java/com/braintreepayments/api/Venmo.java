package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.SignatureVerification;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethod;

import org.json.JSONException;

import java.util.List;

import static com.braintreepayments.api.PaymentMethodTokenizer.PAYMENT_METHOD_ENDPOINT;
import static com.braintreepayments.api.PaymentMethodTokenizer.versionedPath;

/**
 * Class containing Venmo specific logic.
 */
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

    protected static final int VENMO_REQUEST_CODE = 13488;

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

    /**
     * Start the Pay With Venmo flow. This will app switch to the Venmo app.
     *
     * If the Venmo app is not available, {@link AppSwitchNotAvailableException} will be sent to
     * {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onUnrecoverableError(Throwable)}.
     *
     * @param fragment {@link BraintreeFragment}
     */
    public static void authorize(final BraintreeFragment fragment) {
        fragment.sendAnalyticsEvent("venmo.selected");

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (isAvailable(fragment.getContext(), configuration)) {
                    fragment.startActivityForResult(Venmo.getLaunchIntent(configuration),
                            VENMO_REQUEST_CODE);
                    fragment.sendAnalyticsEvent("venmo.app-switch.started");
                } else {
                    fragment.sendAnalyticsEvent("venmo.app-switch.failed");
                    fragment.postCallback(new AppSwitchNotAvailableException("Venmo is not available"));
                }
            }
        });
    }

    protected static void onActivityResult(final BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            final String nonce = data.getStringExtra(EXTRA_PAYMENT_METHOD_NONCE);

            if (TextUtils.isEmpty(nonce)) {
                fragment.sendAnalyticsEvent("venmo.app-switch.failed");
                fragment.postCallback(
                        new UnexpectedException("No nonce present in response from Venmo app"));
                return;
            }

            fragment.sendAnalyticsEvent("venmo.app-switch.authorized");

            fragment.getHttpClient().get(versionedPath(PAYMENT_METHOD_ENDPOINT + "/" + nonce),
                    new HttpResponseCallback() {
                        @Override
                        public void success(String responseBody) {
                            try {
                                List<PaymentMethod> paymentMethodsList =
                                        PaymentMethod.parsePaymentMethods(responseBody);
                                if (paymentMethodsList.size() == 1) {
                                    fragment.postCallback(paymentMethodsList.get(0));
                                    fragment.sendAnalyticsEvent("venmo.app-switch.nonce-received");
                                } else {
                                    fragment.postCallback(new ServerException(
                                            "Unexpected payment method response format."));
                                    fragment.sendAnalyticsEvent("venmo.app-switch.failed");
                                }
                            } catch (JSONException e) {
                                fragment.postCallback(e);
                                fragment.sendAnalyticsEvent("venmo.app-switch.failed");
                            }
                        }

                        @Override
                        public void failure(Exception exception) {
                            fragment.postCallback(exception);
                            fragment.sendAnalyticsEvent("venmo.app-switch.failed");
                        }
                    });
        } else if (resultCode == Activity.RESULT_CANCELED) {
            fragment.sendAnalyticsEvent("venmo.app-switch.canceled");
        }
    }
}
