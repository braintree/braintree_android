package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.models.Configuration;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.BillingAgreementRequest;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.Request;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PayPalRequestTest {

    @Test(timeout = 1000)
    public void getCheckoutRequest_containsCorrectValues() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        CheckoutRequest request = PayPal.getCheckoutRequest("https://paypal.com/?token=pairingId",
                getTargetContext(), configuration.getPayPal());

        assertEquals(AuthorizationRequest.ENVIRONMENT_MOCK, request.getEnvironment());
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/cancel", request.getCancelUrl());
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/success", request.getSuccessUrl());
        assertEquals("paypal_client_id", request.getClientId());
        assertEquals("pairingId", request.getPairingId());
    }

    @Test(timeout = 1000)
    public void getCheckoutRequest_buildsWithLiveStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_live_paypal.json"));

        Request request = PayPal.getCheckoutRequest(null, getTargetContext(), configuration.getPayPal());

        assertEquals(AuthorizationRequest.ENVIRONMENT_LIVE, request.getEnvironment());
    }

    @Test(timeout = 1000)
    public void getCheckoutRequest_buildsWithOfflineStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        Request request = PayPal.getCheckoutRequest(null, getTargetContext(), configuration.getPayPal());

        assertEquals(AuthorizationRequest.ENVIRONMENT_MOCK, request.getEnvironment());
    }

    @Test(timeout = 1000)
    public void getCheckoutRequest_buildsWithCustomStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_custom_paypal.json"));

        Request request = PayPal.getCheckoutRequest(null, getTargetContext(), configuration.getPayPal());

        assertEquals("custom", request.getEnvironment());
    }

    @Test(timeout = 1000)
    public void getBillingAgreement_containsCorrectValues() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        BillingAgreementRequest request = PayPal.getBillingAgreementRequest(
                "https://paypal.com/?ba_token=pairingId",
                getTargetContext(), configuration.getPayPal());

        assertEquals(AuthorizationRequest.ENVIRONMENT_MOCK, request.getEnvironment());
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/cancel", request.getCancelUrl());
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/success", request.getSuccessUrl());
        assertEquals("paypal_client_id", request.getClientId());
        assertEquals("pairingId", request.getPairingId());
    }

    @Test(timeout = 1000)
    public void getBillingAgreementRequest_buildsWithLiveStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_live_paypal.json"));

        Request request = PayPal.getBillingAgreementRequest(null, getTargetContext(),
                configuration.getPayPal());

        assertEquals(AuthorizationRequest.ENVIRONMENT_LIVE, request.getEnvironment());
    }

    @Test(timeout = 1000)
    public void getBillingAgreementRequest_buildsWithOfflineStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        Request request = PayPal.getBillingAgreementRequest(null, getTargetContext(),
                configuration.getPayPal());

        assertEquals(AuthorizationRequest.ENVIRONMENT_MOCK, request.getEnvironment());
    }

    @Test(timeout = 1000)
    public void getBillingAgreementRequest_buildsWithCustomStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_custom_paypal.json"));

        Request request = PayPal.getBillingAgreementRequest(null, getTargetContext(),
                configuration.getPayPal());

        assertEquals("custom", request.getEnvironment());
    }

    @Test(timeout = 1000)
    public void getAuthorizationRequest_containsCorrectValues() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        AuthorizationRequest request = PayPal.getAuthorizationRequest(getTargetContext(),
                configuration.getPayPal(), TOKENIZATION_KEY);

        assertEquals(AuthorizationRequest.ENVIRONMENT_MOCK, request.getEnvironment());
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/cancel", request.getCancelUrl());
        assertEquals("com.braintreepayments.api.test.braintree://onetouch/v1/success",
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

    @Test(timeout = 1000)
    public void getAuthorizationRequest_buildsWithLiveStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_live_paypal.json"));

        Request request = PayPal.getAuthorizationRequest(getTargetContext(),
                configuration.getPayPal(), TOKENIZATION_KEY);

        assertEquals(AuthorizationRequest.ENVIRONMENT_LIVE, request.getEnvironment());
    }

    @Test(timeout = 1000)
    public void getAuthorizationRequest_buildsWithOfflineStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));

        Request request = PayPal.getAuthorizationRequest(getTargetContext(),
                configuration.getPayPal(), TOKENIZATION_KEY);

        assertEquals(AuthorizationRequest.ENVIRONMENT_MOCK, request.getEnvironment());
    }

    @Test(timeout = 1000)
    public void getAuthorizationRequest_buildsWithCustomStageUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_custom_paypal.json"));

        Request request = PayPal.getAuthorizationRequest(getTargetContext(),
                configuration.getPayPal(), TOKENIZATION_KEY);

        assertEquals("custom", request.getEnvironment());
    }
}

