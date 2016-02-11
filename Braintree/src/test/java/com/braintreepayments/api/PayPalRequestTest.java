package com.braintreepayments.api;

import com.braintreepayments.api.models.Configuration;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.BillingAgreementRequest;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class PayPalRequestTest {

    @Test
    public void getCheckoutRequest_containsCorrectValues() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        CheckoutRequest request = PayPal.getCheckoutRequest("https://paypal.com/?token=pairingId",
                RuntimeEnvironment.application, configuration.getPayPal());

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/cancel", request.getCancelUrl());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/success", request.getSuccessUrl());
        assertEquals("paypal_client_id", request.getClientId());
        assertEquals("pairingId", request.getPairingId());
    }

    @Test
    public void getCheckoutRequest_buildsWithLiveStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_live_paypal.json"));

        Request request = PayPal.getCheckoutRequest(null, RuntimeEnvironment.application, configuration.getPayPal());

        assertEquals(EnvironmentManager.LIVE, request.getEnvironment());
    }

    @Test
    public void getCheckoutRequest_buildsWithOfflineStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        Request request = PayPal.getCheckoutRequest(null, RuntimeEnvironment.application, configuration.getPayPal());

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
    }

    @Test
    public void getCheckoutRequest_buildsWithCustomStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_custom_paypal.json"));

        Request request = PayPal.getCheckoutRequest(null, RuntimeEnvironment.application, configuration.getPayPal());

        assertEquals("custom", request.getEnvironment());
    }

    @Test
    public void getBillingAgreement_containsCorrectValues() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        BillingAgreementRequest request = PayPal.getBillingAgreementRequest(
                "https://paypal.com/?ba_token=pairingId",
                RuntimeEnvironment.application, configuration.getPayPal());

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/cancel", request.getCancelUrl());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/success", request.getSuccessUrl());
        assertEquals("paypal_client_id", request.getClientId());
        assertEquals("pairingId", request.getPairingId());
    }

    @Test
    public void getBillingAgreementRequest_buildsWithLiveStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_live_paypal.json"));

        Request request = PayPal.getBillingAgreementRequest(null, RuntimeEnvironment.application,
                configuration.getPayPal());

        assertEquals(EnvironmentManager.LIVE, request.getEnvironment());
    }

    @Test
    public void getBillingAgreementRequest_buildsWithOfflineStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        Request request = PayPal.getBillingAgreementRequest(null, RuntimeEnvironment.application,
                configuration.getPayPal());

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
    }

    @Test
    public void getBillingAgreementRequest_buildsWithCustomStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_custom_paypal.json"));

        Request request = PayPal.getBillingAgreementRequest(null, RuntimeEnvironment.application,
                configuration.getPayPal());

        assertEquals("custom", request.getEnvironment());
    }

    @Test
    public void getAuthorizationRequest_containsCorrectValues() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        AuthorizationRequest request = PayPal.getAuthorizationRequest(RuntimeEnvironment.application,
                configuration.getPayPal(), TOKENIZATION_KEY);

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/cancel", request.getCancelUrl());
        assertEquals("com.braintreepayments.api.braintree://onetouch/v1/success",
                request.getSuccessUrl());
        assertEquals("paypal_client_id", request.getClientId());
        assertEquals(configuration.getPayPal().getPrivacyUrl(), request.getPrivacyUrl());
        assertEquals(configuration.getPayPal().getUserAgreementUrl(), request.getUserAgreementUrl());

        String[] scopes = request.getScopeString().split(" ");
        Arrays.sort(scopes);
        assertEquals(2, scopes.length);
        assertEquals("email", scopes[0]);
        assertEquals("https://uri.paypal.com/services/payments/futurepayments", scopes[1]);
    }

    @Test
    public void getAuthorizationRequest_buildsWithLiveStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_live_paypal.json"));

        Request request = PayPal.getAuthorizationRequest(RuntimeEnvironment.application,
                configuration.getPayPal(), TOKENIZATION_KEY);

        assertEquals(EnvironmentManager.LIVE, request.getEnvironment());
    }

    @Test
    public void getAuthorizationRequest_buildsWithOfflineStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        Request request = PayPal.getAuthorizationRequest(RuntimeEnvironment.application,
                configuration.getPayPal(), TOKENIZATION_KEY);

        assertEquals(EnvironmentManager.MOCK, request.getEnvironment());
    }

    @Test
    public void getAuthorizationRequest_buildsWithCustomStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_custom_paypal.json"));

        Request request = PayPal.getAuthorizationRequest(RuntimeEnvironment.application,
                configuration.getPayPal(), TOKENIZATION_KEY);

        assertEquals("custom", request.getEnvironment());
    }
}

