package com.braintreepayments.api;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SEPADebitApiUnitTest {

    private HttpClient httpClient;
    private CreateMandateCallback createMandateCallback;

    @Before
    public void beforeEach() {
        httpClient = mock(HttpClient.class);
        createMandateCallback = mock(CreateMandateCallback.class);
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

        SEPADebitApi sut = new SEPADebitApi(httpClient);

        PostalAddress billingAddress = new PostalAddress();
        billingAddress.setStreetAddress("Kantstraße 70");
        billingAddress.setExtendedAddress("#170");
        billingAddress.setLocality("Freistaat Sachsen");
        billingAddress.setRegion("Annaberg-buchholz");
        billingAddress.setPostalCode("09456");
        billingAddress.setCountryCodeAlpha2("FR");

        SEPADebitRequest request = new SEPADebitRequest();
        request.setAccountHolderName("John Doe");
        request.setCustomerId("a-customer-id");
        request.setIban("FR7618106000321234566666610");
        request.setMandateType(SEPADebitMandateType.RECURRENT);
        request.setBillingAddress(billingAddress);

        sut.createMandate(request, null, createMandateCallback);

        ArgumentCaptor<CreateMandateResult> captor = ArgumentCaptor.forClass(CreateMandateResult.class);
        verify(createMandateCallback).onResult(captor.capture(), (Exception) isNull());

        CreateMandateResult result = captor.getValue();
        assertEquals("6610", result.getIbanLastFour());
        assertEquals("https://api.test19.stage.paypal.com/directdebit/mandate/authorize?cart_id=1JH42426EL748934W\u0026auth_code=C21_A.AAdcUj4loKRxLtfw336KxbGY7dA7UsLJQTpZU3cE2h49eKkhN1OjFcLxxxzOGVzRiwOzGLlS_cS2BU4ZLKjMnR6lZSG2iQ", result.getApprovalUrl());
        assertEquals("QkEtWDZDQkpCUU5TWENDVw", result.getBankReferenceToken());
        assertEquals("a-customer-id", result.getCustomerId());
        assertEquals(SEPADebitMandateType.RECURRENT, result.getMandateType());
    }

    @Test
    public void createMandate_onInvalidResponseJSON_callsBackError() {

    }

    @Test
    public void createMandate_onHttpResponseError_callsBackError() {

    }
}
