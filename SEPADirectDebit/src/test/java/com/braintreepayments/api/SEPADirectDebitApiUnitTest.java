package com.braintreepayments.api;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SEPADirectDebitApiUnitTest {

    private HttpClient httpClient;
    private CreateMandateCallback createMandateCallback;
    private SEPADirectDebitTokenizeCallback sepaDirectDebitTokenizeCallback;
    private SEPADirectDebitRequest request;
    private PostalAddress billingAddress;
    private String returnUrl;

    @Before
    public void beforeEach() {
        httpClient = mock(HttpClient.class);
        createMandateCallback = mock(CreateMandateCallback.class);
        sepaDirectDebitTokenizeCallback = mock(SEPADirectDebitTokenizeCallback.class);

        request = new SEPADirectDebitRequest();
        request.setAccountHolderName("John Doe");
        request.setCustomerId("a-customer-id");
        request.setIban("FR7618106000321234566666610");
        request.setMandateType(SEPADirectDebitMandateType.RECURRENT);
        request.setMerchantAccountId("a_merchant_account_id");

        billingAddress = new PostalAddress();
        billingAddress.setStreetAddress("Kantstraße 70");
        billingAddress.setExtendedAddress("#170");
        billingAddress.setLocality("Freistaat Sachsen");
        billingAddress.setRegion("Annaberg-buchholz");
        billingAddress.setPostalCode("09456");
        billingAddress.setCountryCodeAlpha2("FR");

        returnUrl = "com.example";
    }

    @Test
    public void createMandate_onSuccessfulHttpResponse_callsBackCreateMandateResult() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.onResult(Fixtures.SEPA_DEBIT_CREATE_MANDATE_RESPONSE, null);
                return null;
            }
        }).when(httpClient).sendRequest(any(HttpRequest.class), any(HttpResponseCallback.class));

        SEPADirectDebitApi sut = new SEPADirectDebitApi(httpClient);

        sut.createMandate(request, returnUrl, createMandateCallback);

        ArgumentCaptor<CreateMandateResult> captor = ArgumentCaptor.forClass(CreateMandateResult.class);
        verify(createMandateCallback).onResult(captor.capture(), (Exception) isNull());

        CreateMandateResult result = captor.getValue();
        assertEquals("6610", result.getIbanLastFour());
        assertEquals("https://api.test19.stage.paypal.com/directdebit/mandate/authorize?cart_id=1JH42426EL748934W\u0026auth_code=C21_A.AAdcUj4loKRxLtfw336KxbGY7dA7UsLJQTpZU3cE2h49eKkhN1OjFcLxxxzOGVzRiwOzGLlS_cS2BU4ZLKjMnR6lZSG2iQ", result.getApprovalUrl());
        assertEquals("QkEtWDZDQkpCUU5TWENDVw", result.getBankReferenceToken());
        assertEquals("a-customer-id", result.getCustomerId());
        assertEquals(SEPADirectDebitMandateType.RECURRENT, result.getMandateType());
    }

    @Test
    public void createMandate_buildHttpRequest_withAllParams() throws JSONException {
        request.setBillingAddress(billingAddress);

        SEPADirectDebitApi sut = new SEPADirectDebitApi(httpClient);
        sut.createMandate(request, returnUrl, createMandateCallback);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture(), ArgumentMatchers.<HttpResponseCallback>any());

        HttpRequest httpRequest = captor.getValue();
        assertEquals("POST", httpRequest.getMethod());
        assertEquals("application/json", httpRequest.getHeaders().get("Content-Type"));
        assertEquals("development_testing_pwpp_multi_account_merchant", httpRequest.getHeaders().get("Client-Key"));
        assertEquals("merchants/pwpp_multi_account_merchant/client_api/v1/sepa_debit", httpRequest.getPath());

        JSONObject data =new JSONObject(new String(httpRequest.getData()));
        JSONObject sepaDebitData = data.getJSONObject("sepa_debit");
        assertEquals("a_merchant_account_id", data.get("merchant_account_id"));

        assertEquals("John Doe", sepaDebitData.get("account_holder_name"));
        assertEquals("a-customer-id", sepaDebitData.get("customer_id"));
        assertEquals("FR7618106000321234566666610", sepaDebitData.get("iban"));
        assertEquals("RECURRENT", sepaDebitData.get("mandate_type"));

        JSONObject billingAddressData = sepaDebitData.getJSONObject("billing_address");

        assertEquals("Kantstraße 70", billingAddressData.get("address_line_1"));
        assertEquals("#170", billingAddressData.get("address_line_2"));
        assertEquals("Freistaat Sachsen", billingAddressData.get("admin_area_1"));
        assertEquals("Annaberg-buchholz", billingAddressData.get("admin_area_2"));
        assertEquals("09456", billingAddressData.get("postal_code"));
        assertEquals("FR", billingAddressData.get("country_code"));
    }

    @Test
    public void createMandate_onInvalidResponseJSON_callsBackError() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.onResult("not-json", null);
                return null;
            }
        }).when(httpClient).sendRequest(any(HttpRequest.class), any(HttpResponseCallback.class));

        SEPADirectDebitApi sut = new SEPADirectDebitApi(httpClient);
        sut.createMandate(request, returnUrl, createMandateCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(createMandateCallback).onResult((CreateMandateResult) isNull(), captor.capture());
        Exception error = captor.getValue();

        assertNotNull(error);
        assertTrue(error instanceof JSONException);
    }

    @Test
    public void createMandate_onHttpResponseError_callsBackError() {
        final Exception exception = new Exception("http-error");

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.onResult(null, exception);
                return null;
            }
        }).when(httpClient).sendRequest(any(HttpRequest.class), any(HttpResponseCallback.class));

        SEPADirectDebitApi sut = new SEPADirectDebitApi(httpClient);
        sut.createMandate(request, returnUrl, createMandateCallback);

        verify(createMandateCallback).onResult((CreateMandateResult) isNull(), same(exception));
    }

    @Test
    public void tokenize_buildsHttpRequest_withAllParams() throws JSONException {
        SEPADirectDebitApi sut = new SEPADirectDebitApi(httpClient);
        sut.tokenize("1234", "a-customer-id", "a-bank-reference-token", "ONE_OFF", sepaDirectDebitTokenizeCallback);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendRequest(captor.capture(), ArgumentMatchers.<HttpResponseCallback>any());

        HttpRequest httpRequest = captor.getValue();
        assertEquals("POST", httpRequest.getMethod());
        assertEquals("application/json", httpRequest.getHeaders().get("Content-Type"));
        assertEquals("development_testing_pwpp_multi_account_merchant", httpRequest.getHeaders().get("Client-Key"));
        assertEquals("merchants/pwpp_multi_account_merchant/client_api/v1/payment_methods/sepa_debit_accounts", httpRequest.getPath());

        JSONObject data =new JSONObject(new String(httpRequest.getData()));
        JSONObject accountData = data.getJSONObject("sepa_debit_account");
        assertEquals("a-customer-id", accountData.get("customer_id"));
        assertEquals("1234", accountData.get("iban_last_chars"));
        assertEquals("ONE_OFF", accountData.get("mandate_type"));
        assertEquals("a-bank-reference-token", accountData.get("bank_reference_token"));
    }

    @Test
    public void tokenize_onSuccessfulHttpResponse_callsBackSEPADirectDebitNonce() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.onResult(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE, null);
                return null;
            }
        }).when(httpClient).sendRequest(any(HttpRequest.class), any(HttpResponseCallback.class));

        SEPADirectDebitApi sut = new SEPADirectDebitApi(httpClient);
        sut.tokenize("1234", "a-customer-id", "a-bank-reference-token", "ONE_OFF", sepaDirectDebitTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitNonce> captor = ArgumentCaptor.forClass(SEPADirectDebitNonce.class);
        verify(sepaDirectDebitTokenizeCallback).onResult(captor.capture(), (Exception) isNull());

        SEPADirectDebitNonce result = captor.getValue();
        assertEquals("1234", result.getIbanLastFour());
        assertEquals("a-customer-id", result.getCustomerId());
        assertEquals(SEPADirectDebitMandateType.ONE_OFF, result.getMandateType());
    }

    @Test
    public void tokenize_onSuccessfulHttpResponse_whenJSONResponseInvalid_callsBackJSONException() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.onResult("not-json", null);
                return null;
            }
        }).when(httpClient).sendRequest(any(HttpRequest.class), any(HttpResponseCallback.class));

        SEPADirectDebitApi sut = new SEPADirectDebitApi(httpClient);
        sut.tokenize("1234", "a-customer-id", "a-bank-reference-token", "ONE_OFF", sepaDirectDebitTokenizeCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(sepaDirectDebitTokenizeCallback).onResult((SEPADirectDebitNonce) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof JSONException);
    }

    @Test
    public void tokenize_onHttpError_callsBackError() {
        final Exception error = new Exception("http error");
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.onResult(null, error);
                return null;
            }
        }).when(httpClient).sendRequest(any(HttpRequest.class), any(HttpResponseCallback.class));

        SEPADirectDebitApi sut = new SEPADirectDebitApi(httpClient);
        sut.tokenize("1234", "a-customer-id", "a-bank-reference-token", "ONE_OFF", sepaDirectDebitTokenizeCallback);

        verify(sepaDirectDebitTokenizeCallback).onResult(null, error);
    }
}
