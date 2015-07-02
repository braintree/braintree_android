package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalConfiguration;
import com.braintreepayments.testutils.TestClientTokenBuilder;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchActivity;

//Todo update tests
public class PayPalTest extends AndroidTestCase {

    public void testBuildsOfflinePayPalConfiguration() {
        Configuration configuration = TestUtils.getConfigurationFromFixture(getContext(),
                "configuration_with_offline_paypal.json");

        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        assertEquals("offline", payPalConfiguration.getEnvironment());
    }

    public void testBuildsLivePayPalConfiguration() {
        Configuration configuration = TestUtils.getConfigurationFromFixture(getContext(),
                "configuration_with_live_paypal.json");

        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        assertEquals("live", payPalConfiguration.getEnvironment());
    }

    public void testBuildsCustomPayPalConfiguration() {
        Configuration configuration = TestUtils.getConfigurationFromFixture(getContext(),
                "configuration_with_custom_paypal.json");

        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        assertEquals("custom", payPalConfiguration.getEnvironment());
    }

    public void testBuildsPayPalAuthorizationRequestForCustomEnvironment() throws ConfigurationException {
        Configuration configuration = TestUtils.getConfigurationFromFixture(getContext(),
                "configuration_with_custom_paypal.json");

        ClientToken clientToken = ClientToken.fromString(new TestClientTokenBuilder().build());

        AuthorizationRequest authorizationRequest = PayPal.buildPayPalAuthorizationConfiguration(
                getContext(), configuration, clientToken);

        assertNotNull(authorizationRequest);
        assertEquals("custom", authorizationRequest.getEnvironment());
    }

    public void testBuildsPayPalAuthorizationRequestForMockEnvironmentFromOfflineConfiguration() throws ConfigurationException {
        Configuration configuration = TestUtils.getConfigurationFromFixture(getContext(),
                "configuration_with_offline_paypal.json");

        ClientToken clientToken = ClientToken.fromString(new TestClientTokenBuilder().build());

        AuthorizationRequest authorizationRequest = PayPal.buildPayPalAuthorizationConfiguration(
                getContext(), configuration, clientToken);

        assertNotNull(authorizationRequest);
        assertEquals("mock", authorizationRequest.getEnvironment());
    }

    public void testBuildsPayPalAuthorizationRequestForLiveEnvironment() throws ConfigurationException {
        Configuration configuration = TestUtils.getConfigurationFromFixture(getContext(),
                "configuration_with_live_paypal.json");

        ClientToken clientToken = ClientToken.fromString(new TestClientTokenBuilder().build());

        AuthorizationRequest authorizationRequest = PayPal.buildPayPalAuthorizationConfiguration(
                getContext(), configuration, clientToken);

        assertNotNull(authorizationRequest);
        assertEquals("live", authorizationRequest.getEnvironment());
    }

    public void testThrowsConfigurationExceptionWhenContextIsInvalid() {
        try {
            PayPal.getBuilderFromActivity(null,
                    Activity.RESULT_OK, new Intent());
            fail("Expected a ConfigurationException but nothing was thrown");
        } catch (ConfigurationException e) {
            assertEquals("Cannot return PayPalAccountBuilder with invalid context or resultCode", e.getMessage());
        }
    }

    public void testThrowsConfigurationExceptionWhenResultCodeIsInvalid() {
        try {
            PayPal.getBuilderFromActivity(null,
                    Integer.MAX_VALUE, new Intent());
            fail("Expected a ConfigurationException but nothing was thrown");
        } catch (ConfigurationException e) {
            assertEquals("Cannot return PayPalAccountBuilder with invalid context or resultCode", e.getMessage());
        }
    }

    public void testIsPayPalIntentReturnsTrueForPayPalOtcResult() {
        Intent intent = new Intent().putExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT, newParcelable());
        assertTrue(PayPal.isPayPalIntent(intent));
    }

    public void testIsPayPalIntentReturnsFalseForNonEmptyIntent() {
        Intent intent = new Intent().putExtra(AppSwitch.EXTRA_PAYMENT_METHOD_NONCE, "nonce");
        assertFalse(PayPal.isPayPalIntent(intent));
    }

    public void testIsPayPalIntentReturnsFalseForEmptyIntent() {
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
