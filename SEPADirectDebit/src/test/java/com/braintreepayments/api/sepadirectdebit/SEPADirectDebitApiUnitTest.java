package com.braintreepayments.api.sepadirectdebit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.testutils.Fixtures;
import com.braintreepayments.api.testutils.MockBraintreeClientBuilder;
import com.braintreepayments.api.sharedutils.HttpResponseCallback;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SEPADirectDebitApiUnitTest {

    private CreateMandateCallback createMandateCallback;
    private SEPADirectDebitInternalTokenizeCallback sepaDirectDebitTokenizeCallback;
    private SEPADirectDebitRequest request;
    private PostalAddress billingAddress;
    private String returnUrl;

    @Before
    public void beforeEach() {
        createMandateCallback = mock(CreateMandateCallback.class);
        sepaDirectDebitTokenizeCallback = mock(SEPADirectDebitInternalTokenizeCallback.class);

        request = new SEPADirectDebitRequest();
        request.setAccountHolderName("John Doe");
        request.setCustomerId("a-customer-id");
        request.setIban("FR7618106000321234566666610");
        request.setMandateType(SEPADirectDebitMandateType.RECURRENT);
        request.setMerchantAccountId("a_merchant_account_id");
        request.setLocale("fr-FR");

        billingAddress = new PostalAddress();
        billingAddress.setStreetAddress("Kantstraße 70");
        billingAddress.setExtendedAddress("#170");
        billingAddress.setLocality("Freistaat Sachsen");
        billingAddress.setRegion("Annaberg-buchholz");
        billingAddress.setPostalCode("09456");
        billingAddress.setCountryCodeAlpha2("FR");

        request.setBillingAddress(billingAddress);

        returnUrl = "com.example";
    }

    @Test
    public void createMandate_onSuccessfulHttpResponse_callsBackCreateMandateResult() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("sample-scheme")
                .sendPOSTSuccessfulResponse(Fixtures.SEPA_DEBIT_CREATE_MANDATE_RESPONSE)
                .build();

        SEPADirectDebitApi sut = new SEPADirectDebitApi(braintreeClient);
        sut.createMandate(request, returnUrl, createMandateCallback);

        ArgumentCaptor<CreateMandateResult> createMandateCaptor =
                ArgumentCaptor.forClass(CreateMandateResult.class);
        verify(createMandateCallback).onResult(createMandateCaptor.capture(), isNull());

        CreateMandateResult result = createMandateCaptor.getValue();

        assertEquals("6610", result.getIbanLastFour());
        assertEquals(
                "https://api.test19.stage.paypal.com/directdebit/mandate/authorize?cart_id=1JH42426EL748934W\u0026auth_code=C21_A.AAdcUj4loKRxLtfw336KxbGY7dA7UsLJQTpZU3cE2h49eKkhN1OjFcLxxxzOGVzRiwOzGLlS_cS2BU4ZLKjMnR6lZSG2iQ",
                result.getApprovalUrl());
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
        verify(createMandateCallback).onResult(isNull(), captor.capture());
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

        verify(createMandateCallback).onResult(isNull(), same(exception));
    }

    @Test
    public void tokenize_onSuccessfulHttpResponse_callsBackSEPADirectDebitNonce() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("sample-scheme")
                .sendPOSTSuccessfulResponse(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE)
                .build();

        SEPADirectDebitApi sut = new SEPADirectDebitApi(braintreeClient);
        sut.tokenize("1234", "a-customer-id", "a-bank-reference-token", "ONE_OFF",
                sepaDirectDebitTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitNonce> captor =
                ArgumentCaptor.forClass(SEPADirectDebitNonce.class);
        verify(sepaDirectDebitTokenizeCallback).onResult(captor.capture(), isNull());

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
        sut.tokenize("1234", "a-customer-id", "a-bank-reference-token", "ONE_OFF",
                sepaDirectDebitTokenizeCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(sepaDirectDebitTokenizeCallback).onResult(isNull(),
                captor.capture());

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
        sut.tokenize("1234", "a-customer-id", "a-bank-reference-token", "ONE_OFF",
                sepaDirectDebitTokenizeCallback);

        verify(sepaDirectDebitTokenizeCallback).onResult(null, error);
    }

    @Test
    public void createMandate_properlyFormatsPOSTBody() throws JSONException {
        BraintreeClient mockBraintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("com.example")
                .build();

        SEPADirectDebitApi sut = new SEPADirectDebitApi(mockBraintreeClient);
        sut.createMandate(request, returnUrl, createMandateCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockBraintreeClient).sendPOST(eq("v1/sepa_debit"), String.valueOf(captor.capture()),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject json = new JSONObject(result);
        assertEquals("com.example://sepa/cancel", json.getString("cancel_url"));
        assertEquals("com.example://sepa/success", json.getString("return_url"));
        assertEquals("a_merchant_account_id", json.getString("merchant_account_id"));
        assertEquals("fr-FR", json.getString("locale"));

        JSONObject sepaJson = json.getJSONObject("sepa_debit");
        assertEquals("John Doe", sepaJson.getString("account_holder_name"));
        assertEquals("a-customer-id", sepaJson.getString("merchant_or_partner_customer_id"));
        assertEquals("FR7618106000321234566666610", sepaJson.getString("iban"));
        assertEquals("RECURRENT", sepaJson.getString("mandate_type"));

        JSONObject billingAddressJson = sepaJson.getJSONObject("billing_address");
        assertEquals("Kantstraße 70", billingAddressJson.getString("address_line_1"));
        assertEquals("#170", billingAddressJson.getString("address_line_2"));
        assertEquals("Freistaat Sachsen", billingAddressJson.getString("admin_area_1"));
        assertEquals("Annaberg-buchholz", billingAddressJson.getString("admin_area_2"));
        assertEquals("09456", billingAddressJson.getString("postal_code"));
        assertEquals("FR", billingAddressJson.getString("country_code"));
    }
}
