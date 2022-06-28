package com.braintreepayments.api;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private BraintreeClient braintreeClient;
    private CreateMandateCallback createMandateCallback;
    private SEPADirectDebitTokenizeCallback sepaDirectDebitTokenizeCallback;
    private SEPADirectDebitRequest request;
    private PostalAddress billingAddress;
    private String returnUrl;

    @Before
    public void beforeEach() {
        braintreeClient = mock(BraintreeClient.class);
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
    public void createMandate_onSuccessfulHttpResponse_callsBackCreateMandateResult() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("sample-scheme")
                .sendPOSTSuccessfulResponse(Fixtures.SEPA_DEBIT_CREATE_MANDATE_RESPONSE)
                .build();

        SEPADirectDebitApi sut = new SEPADirectDebitApi(braintreeClient);
        sut.createMandate(request, returnUrl, createMandateCallback);

        ArgumentCaptor<CreateMandateResult> createMandateCaptor = ArgumentCaptor.forClass(CreateMandateResult.class);
        verify(createMandateCallback).onResult(createMandateCaptor.capture(), (Exception) isNull());

        CreateMandateResult result = createMandateCaptor.getValue();

        assertEquals("6610", result.getIbanLastFour());
        assertEquals("https://api.test19.stage.paypal.com/directdebit/mandate/authorize?cart_id=1JH42426EL748934W\u0026auth_code=C21_A.AAdcUj4loKRxLtfw336KxbGY7dA7UsLJQTpZU3cE2h49eKkhN1OjFcLxxxzOGVzRiwOzGLlS_cS2BU4ZLKjMnR6lZSG2iQ", result.getApprovalUrl());
        assertEquals("QkEtWDZDQkpCUU5TWENDVw", result.getBankReferenceToken());
        assertEquals("a-customer-id", result.getCustomerId());
        assertEquals(SEPADirectDebitMandateType.RECURRENT, result.getMandateType());
    }

    @Test
    public void createMandate_onInvalidResponseJSON_callsBackError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("sample-scheme")
                .sendPOSTSuccessfulResponse("not-json")
                .build();

        SEPADirectDebitApi sut = new SEPADirectDebitApi(braintreeClient);
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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("sample-scheme")
                .sendPOSTErrorResponse(exception)
                .build();

        SEPADirectDebitApi sut = new SEPADirectDebitApi(braintreeClient);
        sut.createMandate(request, returnUrl, createMandateCallback);

        verify(createMandateCallback).onResult((CreateMandateResult) isNull(), same(exception));
    }

    @Test
    public void tokenize_onSuccessfulHttpResponse_callsBackSEPADirectDebitNonce() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("sample-scheme")
                .sendPOSTSuccessfulResponse(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE)
                .build();

        SEPADirectDebitApi sut = new SEPADirectDebitApi(braintreeClient);
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
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("sample-scheme")
                .sendPOSTSuccessfulResponse("not-json")
                .build();

        SEPADirectDebitApi sut = new SEPADirectDebitApi(braintreeClient);
        sut.tokenize("1234", "a-customer-id", "a-bank-reference-token", "ONE_OFF", sepaDirectDebitTokenizeCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(sepaDirectDebitTokenizeCallback).onResult((SEPADirectDebitNonce) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof JSONException);
    }

    @Test
    public void tokenize_onHttpError_callsBackError() {
        final Exception error = new Exception("http error");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("sample-scheme")
                .sendPOSTErrorResponse(error)
                .build();

        SEPADirectDebitApi sut = new SEPADirectDebitApi(braintreeClient);
        sut.tokenize("1234", "a-customer-id", "a-bank-reference-token", "ONE_OFF", sepaDirectDebitTokenizeCallback);

        verify(sepaDirectDebitTokenizeCallback).onResult(null, error);
    }
}
