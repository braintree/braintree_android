package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(RobolectricTestRunner.class)
public class LocalPaymentApiUnitTest {

    private LocalPaymentStartCallback localPaymentStartCallback;
    private LocalPaymentBrowserSwitchResultCallback localPaymentBrowserSwitchResultCallback;

    @Before
    public void beforeEach() {
        localPaymentStartCallback = mock(LocalPaymentStartCallback.class);
        localPaymentBrowserSwitchResultCallback = mock(LocalPaymentBrowserSwitchResultCallback.class);
    }

    @Test
    public void createPaymentMethod_sendsCorrectPostParams() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("sample-scheme")
                .build();

        LocalPaymentApi sut = new LocalPaymentApi(braintreeClient);
        sut.createPaymentMethod(getIdealLocalPaymentRequest(), localPaymentStartCallback);

        String expectedPath = "/v1/local_payments/create";
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq(expectedPath), bodyCaptor.capture(), any(HttpResponseCallback.class));

        String requestBody = bodyCaptor.getValue();
        JSONObject json = new JSONObject(requestBody);
        assertEquals("Doe", json.getString("lastName"));
        assertEquals("1.10", json.getString("amount"));
        assertEquals("Den Haag", json.getString("city"));
        assertEquals("2585 GJ", json.getString("postalCode"));
        assertEquals("sale", json.getString("intent"));
        assertEquals("Jon", json.getString("firstName"));
        assertEquals("639847934", json.getString("phone"));
        assertEquals("NL", json.getString("countryCode"));
        assertEquals("EUR", json.getString("currencyIsoCode"));
        assertEquals("ideal", json.getString("fundingSource"));
        assertEquals("jon@getbraintree.com", json.getString("payerEmail"));
        assertEquals("836486 of 22321 Park Lake", json.getString("line1"));
        assertEquals("Apt 2", json.getString("line2"));
        assertEquals("CA", json.getString("state"));
        assertEquals("local-merchant-account-id", json.getString("merchantAccountId"));
        assertTrue(json.getJSONObject("experienceProfile").getBoolean("noShipping"));
        assertEquals("My Brand!", json.getJSONObject("experienceProfile").getString("brandName"));
        String expectedCancelUrl = Uri.parse("sample-scheme://local-payment-cancel").toString();
        String expectedReturnUrl = Uri.parse("sample-scheme://local-payment-success").toString();
        assertEquals(expectedCancelUrl, json.getString("cancelUrl"));
        assertEquals(expectedReturnUrl, json.getString("returnUrl"));
    }

    @Test
    public void createPaymentMethod_onPOSTError_forwardsErrorToCallback() {
        Exception error = new Exception("error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTErrorResponse(error)
                .build();

        LocalPaymentApi sut = new LocalPaymentApi(braintreeClient);
        sut.createPaymentMethod(getIdealLocalPaymentRequest(), localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult((LocalPaymentResult) isNull(), same(error));

    }

    @Test
    public void createPaymentMethod_onJSONError_forwardsJSONErrorToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse(Fixtures.ERROR_RESPONSE)
                .build();

        LocalPaymentApi sut = new LocalPaymentApi(braintreeClient);
        sut.createPaymentMethod(getIdealLocalPaymentRequest(), localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult((LocalPaymentResult) isNull(), captor.capture());

        assertTrue(captor.getValue() instanceof JSONException);
    }

    @Test
    public void createPaymentMethod_onPOSTSuccess_returnsResultToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_CREATE_RESPONSE)
                .build();

        LocalPaymentApi sut = new LocalPaymentApi(braintreeClient);
        sut.createPaymentMethod(getIdealLocalPaymentRequest(), localPaymentStartCallback);

        ArgumentCaptor<LocalPaymentResult> captor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentStartCallback).onResult(captor.capture(), (Exception) isNull());

        LocalPaymentResult result = captor.getValue();
        assertNotNull(result);
        assertEquals("local-payment-id-123", result.getPaymentId());
    }

    @Test
    public void tokenize_sendsCorrectPostParams() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();

        LocalPaymentApi sut = new LocalPaymentApi(braintreeClient);
        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        sut.tokenize("local-merchant-account-id", webUrl, "sample-correlation-id", localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        String expectedUrl = "/v1/payment_methods/paypal_accounts";

        verify(braintreeClient).sendPOST(eq(expectedUrl), bodyCaptor.capture(), any(HttpResponseCallback.class));
        String requestBody = bodyCaptor.getValue();

        JSONObject expectedJSON = new JSONObject();
        expectedJSON.put("merchant_account_id", "local-merchant-account-id");

        JSONObject paypalAccount = new JSONObject()
                .put("intent", "sale")
                .put("response", new JSONObject().put("webURL", webUrl))
                .put("options", new JSONObject().put("validate", false))
                .put("response_type", "web")
                .put("correlation_id", "sample-correlation-id");
        expectedJSON.put("paypal_account", paypalAccount);

        JSONObject metaData = new JSONObject()
                .put("source", "client")
                .put("integration", "sample-integration-type")
                .put("sessionId", "sample-session-id");
        expectedJSON.put("_meta", metaData);

        JSONAssert.assertEquals(expectedJSON, new JSONObject(requestBody), true);
    }

    @Test
    public void tokenize_onPOSTError_forwardsErrorToCallback() {
        Exception error = new Exception("error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .sendPOSTErrorResponse(error)
                .build();

        LocalPaymentApi sut = new LocalPaymentApi(braintreeClient);
        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        sut.tokenize("local-merchant-account-id", webUrl, "sample-correlation-id", localPaymentBrowserSwitchResultCallback);

        verify(localPaymentBrowserSwitchResultCallback).onResult((LocalPaymentNonce) isNull(), same(error));
    }

    @Test
    public void tokenize_onJSONError_forwardsErrorToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .sendPOSTSuccessfulResponse("not-json")
                .build();

        LocalPaymentApi sut = new LocalPaymentApi(braintreeClient);
        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        sut.tokenize("local-merchant-account-id", webUrl, "sample-correlation-id", localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult((LocalPaymentNonce) isNull(), captor.capture());

        assertTrue(captor.getValue() instanceof JSONException);
    }

    @Test
    public void tokenize_onPOSTSuccess_returnsResultToCallback() throws JSONException {
        LocalPaymentNonce expectedResult = LocalPaymentNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE));

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
                .build();

        LocalPaymentApi sut = new LocalPaymentApi(braintreeClient);
        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        sut.tokenize("local-merchant-account-id", webUrl, "sample-correlation-id", localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<LocalPaymentNonce> captor = ArgumentCaptor.forClass(LocalPaymentNonce.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult(captor.capture(), (Exception) isNull());

        LocalPaymentNonce result = captor.getValue();
        assertNotNull(result);
        assertEquals(expectedResult.getString(), result.getString());
    }

    private LocalPaymentRequest getIdealLocalPaymentRequest() {
        PostalAddress address = new PostalAddress();
        address.setStreetAddress("836486 of 22321 Park Lake");
        address.setExtendedAddress("Apt 2");
        address.setCountryCodeAlpha2("NL");
        address.setLocality("Den Haag");
        address.setRegion("CA");
        address.setPostalCode("2585 GJ");

        LocalPaymentRequest request = new LocalPaymentRequest();
        request.setPaymentType("ideal");
        request.setAmount("1.10");
        request.setAddress(address);
        request.setPhone("639847934");
        request.setEmail("jon@getbraintree.com");
        request.setGivenName("Jon");
        request.setSurname("Doe");
        request.setShippingAddressRequired(false);
        request.setMerchantAccountId("local-merchant-account-id");
        request.setCurrencyCode("EUR");
        request.setPaymentTypeCountryCode("NL");
        request.setDisplayName("My Brand!");

        return request;
    }
}
