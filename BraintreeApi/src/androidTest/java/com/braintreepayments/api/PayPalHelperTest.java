package com.braintreepayments.api;

import android.content.Intent;
import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalService;

public class PayPalHelperTest extends AndroidTestCase {

    public void testBuildsOfflinePayPalConfiguration() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(),
                "client_tokens/offline_paypal_client_token.json");

        PayPalConfiguration configuration = PayPalHelper.buildPayPalConfiguration(token);

        assertEquals("PayPalConfig: {environment:mock: languageOrLocale:null}", configuration.toString());
    }

    public void testBuildsLivePayPalConfiguration() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/live_paypal_client_token.json");

        PayPalConfiguration configuration = PayPalHelper.buildPayPalConfiguration(token);

        assertEquals("PayPalConfig: {environment:live: languageOrLocale:null}", configuration.toString());
    }

    public void testBuildsCustomPayPalConfiguration() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/custom_paypal_client_token.json");

        PayPalConfiguration configuration = PayPalHelper.buildPayPalConfiguration(token);

        assertEquals("PayPalConfig: {environment:custom: languageOrLocale:null}", configuration.toString());
    }

    public void testBuildsPayPalServiceIntentWithCustomStageUrlAndSslVerificationOffForCustomEnvironment() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/custom_paypal_client_token.json");

        Intent intent = PayPalHelper.buildPayPalServiceIntent(getContext(), token);

        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
        assertEquals("https://braintree.paypal.com/v1/",
                intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
        assertFalse(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
    }

    public void testDoesNotAddBonusExtrasToIntentForOffline() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/offline_paypal_client_token.json");

        Intent intent = PayPalHelper.buildPayPalServiceIntent(getContext(), token);

        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
        assertNull(intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
        assertTrue(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
    }

    public void testDoesNotAddBonusExtrasToIntentForLive() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/live_paypal_client_token.json");

        Intent intent = PayPalHelper.buildPayPalServiceIntent(getContext(), token);

        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
        assertNull(intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
        assertTrue(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
    }

    public void testThrowsConfigurationExceptionWhenResultExtrasInvalidResultCodeReturned() {
        boolean exceptionHappened = false;
        try {
            PayPalHelper.getBuilderFromActivity(null,
                    PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID, new Intent());
            fail("Configuration exception was not thrown");
        } catch (ConfigurationException e) {
            exceptionHappened = true;
        }

        assertTrue("Expected a ConfigurationException but nothing was raises", exceptionHappened);
    }

    public void testReturnsNullWhenResultCodeIsNotExpected() throws ConfigurationException {
        PayPalAccountBuilder payPalAccountBuilder = PayPalHelper.getBuilderFromActivity(null,
                Integer.MAX_VALUE, new Intent());
        assertNull(payPalAccountBuilder);
    }
}
