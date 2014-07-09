package com.braintreepayments.api;

import android.os.SystemClock;
import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;

import java.util.List;

public class GetPaymentMethodsTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestUtils.setUp(getContext());
    }

    public void testGetPaymentMethodsReturnsAListIfEmpty()
            throws ErrorWithResponse, BraintreeException {
        BraintreeApi braintreeApi = new BraintreeApi(getContext(),
                new TestClientTokenBuilder().build());
        List<PaymentMethod> paymentMethods = braintreeApi.getPaymentMethods();

        assertEquals(0, paymentMethods.size());
    }

    public void testGetPaymentMethodsReturnsAListOfAddedPaymentMethods()
            throws ErrorWithResponse, BraintreeException {
        BraintreeApi braintreeApi = new BraintreeApi(getContext(),
                new TestClientTokenBuilder().withFakePayPal().build());
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4111111111111111")
                .expirationMonth("01")
                .expirationYear("2017");

        braintreeApi.create(cardBuilder);

        SystemClock.sleep(1000);

        cardBuilder = new CardBuilder()
                .cardNumber("371449635398431")
                .expirationMonth("01")
                .expirationYear("2017");

        braintreeApi.create(cardBuilder);

        SystemClock.sleep(1000);

        PayPalAccountBuilder paypalBuilder = new PayPalAccountBuilder()
                .authorizationCode("fake_auth_code");

        braintreeApi.create(paypalBuilder);

        List<PaymentMethod> paymentMethods = braintreeApi.getPaymentMethods();

        assertEquals(3, paymentMethods.size());
        assertEquals("PayPal", paymentMethods.get(0).getTypeLabel());
        assertEquals("31", ((Card) paymentMethods.get(1)).getLastTwo());
        assertEquals("11", ((Card) paymentMethods.get(2)).getLastTwo());
    }

    public void testGetPaymentMethodsReturnsAnError() throws ErrorWithResponse {
        ClientToken clientToken = TestUtils.clientTokenFromFixture(getContext(),
                "client_tokens/client_token.json");
        BraintreeApi braintreeApi = TestUtils.unexpectedExceptionThrowingApi(getContext(),
                clientToken);

        try {
            braintreeApi.getPaymentMethods();
            fail("Expected an exception");
        } catch (BraintreeException e) {
            assertTrue(true);
        }
    }

    public void testGetPaymentMethodsThrowsErrorWithResponse() throws BraintreeException {
        ClientToken clientToken = TestUtils.clientTokenFromFixture(getContext(),
                "client_tokens/client_token.json");
        BraintreeApi braintreeApi = TestUtils.apiWithExpectedResponse(getContext(), clientToken,
                FixturesHelper.stringFromFixture(getContext(), "errors/error_response.json"), 422);

        try {
            braintreeApi.getPaymentMethods();
            fail("Expected an exception");
        } catch (ErrorWithResponse e) {
            assertEquals("There was an error", e.getMessage());
        }
    }
}
