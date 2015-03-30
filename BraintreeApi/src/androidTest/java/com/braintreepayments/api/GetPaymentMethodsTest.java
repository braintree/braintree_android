package com.braintreepayments.api;

import android.os.SystemClock;
import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CoinbaseAccount;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import java.io.IOException;
import java.util.List;

import static com.braintreepayments.api.TestUtils.apiWithExpectedResponse;
import static com.braintreepayments.api.TestUtils.assertIsANonce;
import static com.braintreepayments.testutils.CardNumber.AMEX;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                .cardNumber(VISA)
                .expirationMonth("01")
                .expirationYear("2017");

        braintreeApi.create(cardBuilder);
        SystemClock.sleep(1000);

        cardBuilder = new CardBuilder()
                .cardNumber(AMEX)
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
        assertEquals("05", ((Card) paymentMethods.get(1)).getLastTwo());
        assertEquals("11", ((Card) paymentMethods.get(2)).getLastTwo());
    }

    public void testGetPaymentMethodsReturnsCoinbasePaymentMethods()
            throws ErrorWithResponse, BraintreeException {
        HttpResponse httpResponse = new HttpResponse(200,
                stringFromFixture(mContext, "get_payment_methods_with_coinbase.json"));
        HttpRequest mockHttpRequest = mock(HttpRequest.class);
        when(mockHttpRequest.get(contains("payment_methods"))).thenReturn(httpResponse);
        BraintreeApi braintreeApi = new BraintreeApi(getContext(), mockHttpRequest);

        List<PaymentMethod> paymentMethods = braintreeApi.getPaymentMethods();

        assertEquals(1, paymentMethods.size());

        CoinbaseAccount coinbaseAccount = (CoinbaseAccount) paymentMethods.get(0);
        assertEquals("Coinbase", coinbaseAccount.getTypeLabel());
        assertEquals("satoshi@example.com", coinbaseAccount.getEmail());
        assertIsANonce(coinbaseAccount.getNonce());
    }

    public void testGetPaymentMethodsReturnsAnError() throws ErrorWithResponse,
            BraintreeException {
        BraintreeApi braintreeApi = TestUtils.unexpectedExceptionThrowingApi(getContext());

        try {
            braintreeApi.getPaymentMethods();
            fail("Expected an exception");
        } catch (BraintreeException e) {
            assertTrue(true);
        }
    }

    public void testGetPaymentMethodsThrowsErrorWithResponse()
            throws IOException, ErrorWithResponse {
        BraintreeApi braintreeApi = apiWithExpectedResponse(getContext(), 422,
                stringFromFixture(getContext(), "error_response.json"));

        try {
            braintreeApi.getPaymentMethods();
            fail("Expected an exception");
        } catch (ErrorWithResponse e) {
            assertEquals("There was an error", e.getMessage());
        }
    }
}
