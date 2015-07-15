package com.braintreepayments.api;

import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.TestUtils.apiWithExpectedResponse;
import static com.braintreepayments.testutils.CardNumber.AMEX;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class GetPaymentMethodsTest {

    @Before
    public void setUp() {
        TestUtils.setUp(getTargetContext());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_returnsAnEmptyListIfEmpty()
            throws ErrorWithResponse, BraintreeException, JSONException {
        BraintreeApi braintreeApi = new BraintreeApi(getTargetContext(),
                new TestClientTokenBuilder().build());
        List<PaymentMethod> paymentMethods = braintreeApi.getPaymentMethods();

        assertEquals(0, paymentMethods.size());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_returnsAListOfPaymentMethods()
            throws ErrorWithResponse, BraintreeException, JSONException {
        BraintreeApi braintreeApi = new BraintreeApi(getTargetContext(),
                new TestClientTokenBuilder().withPayPal().build());
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
                .consentCode("fake_auth_code");

        braintreeApi.create(paypalBuilder);

        List<PaymentMethod> paymentMethods = braintreeApi.getPaymentMethods();

        assertEquals(3, paymentMethods.size());
        assertEquals("PayPal", paymentMethods.get(0).getTypeLabel());
        assertEquals("05", ((Card) paymentMethods.get(1)).getLastTwo());
        assertEquals("11", ((Card) paymentMethods.get(2)).getLastTwo());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_throwsAnError() throws ErrorWithResponse,
            BraintreeException {
        BraintreeApi braintreeApi = TestUtils.unexpectedExceptionThrowingApi(getTargetContext());

        try {
            braintreeApi.getPaymentMethods();
            fail("Expected an exception");
        } catch (BraintreeException e) {
            assertTrue(true);
        }
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_throwsErrorWithResponse()
            throws IOException, ErrorWithResponse {
        BraintreeApi braintreeApi = apiWithExpectedResponse(getTargetContext(), 422,
                stringFromFixture(getTargetContext(), "error_response.json"));

        try {
            braintreeApi.getPaymentMethods();
            fail("Expected an exception");
        } catch (ErrorWithResponse e) {
            assertEquals("There was an error", e.getMessage());
        }
    }
}
