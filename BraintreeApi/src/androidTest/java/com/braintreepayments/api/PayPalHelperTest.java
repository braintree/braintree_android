package com.braintreepayments.api;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PayPalTouchActivity;

import static com.braintreepayments.api.TestUtils.getConfigurationFromFixture;

public class PayPalHelperTest extends AndroidTestCase {

    public void testBuildsOfflinePayPalConfiguration() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_offline_paypal.json");

        PayPalConfiguration payPalConfiguration =
                PayPalHelper.buildPayPalConfiguration(configuration.getPayPal());

        assertTrue(payPalConfiguration.toString().contains("environment:mock"));
    }

    public void testBuildsLivePayPalConfiguration() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_live_paypal.json");

        PayPalConfiguration payPalConfiguration =
                PayPalHelper.buildPayPalConfiguration(configuration.getPayPal());

        assertTrue(payPalConfiguration.toString().contains("environment:live"));
    }

    public void testBuildsCustomPayPalConfiguration() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_custom_paypal.json");

        PayPalConfiguration payPalConfiguration =
                PayPalHelper.buildPayPalConfiguration(configuration.getPayPal());

        assertTrue(payPalConfiguration.toString().contains("environment:custom"));
    }

    public void testBuildsPayPalServiceIntentWithCustomStageUrlAndSslVerificationOffForCustomEnvironment() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_custom_paypal.json");

        Intent intent = PayPalHelper.buildPayPalServiceIntent(getContext(), configuration.getPayPal());

        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
        assertEquals("https://braintree.paypal.com/v1/",
                intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
        assertFalse(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
    }

    public void testDoesNotAddBonusExtrasToIntentForOffline() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_offline_paypal.json");

        Intent intent = PayPalHelper.buildPayPalServiceIntent(getContext(), configuration.getPayPal());

        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
        assertNull(intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
        assertTrue(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
    }

    public void testDoesNotAddBonusExtrasToIntentForLive() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_live_paypal.json");

        Intent intent = PayPalHelper.buildPayPalServiceIntent(getContext(), configuration.getPayPal());

        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
        assertNull(intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
        assertTrue(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
    }

    public void testThrowsConfigurationExceptionWhenResultExtrasInvalidResultCodeReturned() {
        try {
            PayPalHelper.getBuilderFromActivity(null,
                    PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID, new Intent());
            fail("Expected a ConfigurationException but nothing was thrown");
        } catch (ConfigurationException e) {
            assertEquals("Result extras were invalid", e.getMessage());
        }
    }

    public void testReturnsNullWhenResultCodeIsNotExpected() throws ConfigurationException {
        PayPalAccountBuilder payPalAccountBuilder = PayPalHelper.getBuilderFromActivity(null,
                Integer.MAX_VALUE, new Intent());
        assertNull(payPalAccountBuilder);
    }

    public void testIsPayPalIntentReturnsTrueForPayPalTouchIntent() {
        Intent intent = new Intent().putExtra(PayPalTouchActivity.EXTRA_LOGIN_CONFIRMATION, newParcelable());
        assertTrue(PayPalHelper.isPayPalIntent(intent));
    }

    public void testIsPayPalIntentReturnsTrueForPayPalSDKIntent() {
        Intent intent = new Intent().putExtra(PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION, newParcelable());
        assertTrue(PayPalHelper.isPayPalIntent(intent));
    }

    public void testIsPayPalIntentReturnsFalseForNonEmptyIntent() {
        Intent intent = new Intent().putExtra(AppSwitch.EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        assertFalse(PayPalHelper.isPayPalIntent(intent));
    }

    public void testIsPayPalIntentReturnsFalseForEmptyIntent() {
        assertFalse(PayPalHelper.isPayPalIntent(new Intent()));
    }

    private Parcelable newParcelable() {
        return new Parcelable() {
            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
            }
        };
    }
}
