package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PayPalTouchActivity;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import java.util.Collections;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.BraintreeTestUtils.getConfigurationFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
public class PayPalTest {

    @Test(timeout = 1000)
    @SmallTest
    public void startPayPalService_stopsAndStartsService() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_offline_paypal.json");
        Context context = mock(Context.class);

        PayPal.startPaypalService(context, configuration.getPayPal());

        InOrder order = inOrder(context);
        order.verify(context).stopService(any(Intent.class));
        order.verify(context).startService(any(Intent.class));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getLaunchIntent_returnsCorrectIntent() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_offline_paypal.json");

        Intent intent = PayPal.getLaunchIntent(getTargetContext(), configuration.getPayPal(),
                Collections.singletonList("address"));

        assertEquals(PayPalProfileSharingActivity.class.getName(), intent.getComponent().getClassName());
        assertTrue(intent.hasExtra(PayPalTouchActivity.EXTRA_REQUESTED_SCOPES));
        assertTrue(intent.hasExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void buildPayPalConfiguration_buildsOfflinePayPalConfiguration() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_offline_paypal.json");

        PayPalConfiguration payPalConfiguration =
                PayPal.buildPayPalConfiguration(configuration.getPayPal());

        assertTrue(payPalConfiguration.toString().contains("environment:mock"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void buildPayPalConfiguration_buildsLivePayPalConfiguration() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_live_paypal.json");

        PayPalConfiguration payPalConfiguration =
                PayPal.buildPayPalConfiguration(configuration.getPayPal());

        assertTrue(payPalConfiguration.toString().contains("environment:live"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void buildPayPalConfiguration_buildsCustomPayPalConfiguration() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_custom_paypal.json");

        PayPalConfiguration payPalConfiguration =
                PayPal.buildPayPalConfiguration(configuration.getPayPal());

        assertTrue(payPalConfiguration.toString().contains("environment:custom"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void buildPayPalServiceIntent_buildsIntentWithCustomStageUrlAndSslVerificationOffForCustomEnvironment()
            throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_custom_paypal.json");

        Intent intent = PayPal.buildPayPalServiceIntent(getTargetContext(), configuration.getPayPal());

        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
        assertEquals("https://braintree.paypal.com/v1/",
                intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
        assertFalse(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void buildPayPalServiceIntent_doesNotAddExtrasToIntentForOffline() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_offline_paypal.json");

        Intent intent = PayPal.buildPayPalServiceIntent(getTargetContext(), configuration.getPayPal());

        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
        assertNull(intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
        assertTrue(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void buildPayPalServiceIntent_doesNotAddExtrasToIntentForLive() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_live_paypal.json");

        Intent intent = PayPal.buildPayPalServiceIntent(getTargetContext(), configuration.getPayPal());

        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
        assertNull(intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
        assertTrue(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getBuilderFromActivity_throwsConfigurationExceptionWhenResultExtrasInvalidResultCodeReturned() {
        try {
            PayPal.getBuilderFromActivity(null,
                    PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID, new Intent());
            fail("Expected a ConfigurationException but nothing was thrown");
        } catch (ConfigurationException e) {
            assertEquals("Result extras were invalid", e.getMessage());
        }
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getBuilderFromActivity_returnsNullWhenResultCodeIsNotExpected() throws ConfigurationException {
        PayPalAccountBuilder payPalAccountBuilder = PayPal.getBuilderFromActivity(null,
                Integer.MAX_VALUE, new Intent());
        assertNull(payPalAccountBuilder);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void isPayPalIntent_returnsTrueForPayPalTouchIntent() {
        Intent intent = new Intent().putExtra(PayPalTouchActivity.EXTRA_LOGIN_CONFIRMATION, newParcelable());
        assertTrue(PayPal.isPayPalIntent(intent));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void isPayPalIntent_returnsTrueForPayPalSDKIntent() {
        Intent intent = new Intent().putExtra(PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION, newParcelable());
        assertTrue(PayPal.isPayPalIntent(intent));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void isPayPalIntent_returnsFalseForNonEmptyIntent() {
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        assertFalse(PayPal.isPayPalIntent(intent));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void isPayPalIntent_returnsFalseForEmptyIntent() {
        assertFalse(PayPal.isPayPalIntent(new Intent()));
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
