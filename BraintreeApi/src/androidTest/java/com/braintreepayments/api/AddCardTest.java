package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class AddCardTest {

    private BraintreeApi mBraintreeApi;

    @Before
    public void setUp() throws JSONException {
        TestUtils.setUp(getTargetContext());
        mBraintreeApi = new BraintreeApi(getTargetContext(), new TestClientTokenBuilder().build());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void canAddCard() throws ErrorWithResponse, BraintreeException, JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("01")
                .expirationYear("2017");

        Card addedCard = mBraintreeApi.create(cardBuilder);

        assertNotNull(addedCard.getNonce());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void canTokenizeInvalidCard()
            throws ErrorWithResponse, BraintreeException, JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("This is a credit card");

        String nonce = mBraintreeApi.tokenize(cardBuilder);

        assertNotNull(nonce);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void throwsErrorOnServerFailure() throws BraintreeException, JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .expirationMonth("01");

        try {
            mBraintreeApi.create(cardBuilder);
            fail("Expected an exception");
        } catch (ErrorWithResponse e) {
            assertEquals(422, e.getStatusCode());
            assertNotNull(e.getFieldErrors());

            // @formatter:off
            assertEquals("Credit card is invalid",  e.getMessage());
            assertEquals(1, e.getFieldErrors().size());
            assertEquals(3, e.errorFor("creditCard").getFieldErrors().size());
            assertEquals("Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code", e.errorFor("creditCard").errorFor("base").getMessage());
            assertEquals("Expiration year is invalid", e.errorFor("creditCard").errorFor("expirationYear").getMessage());
            assertEquals("Credit card number is required", e.errorFor("creditCard").errorFor("number").getMessage());
            assertNull(e.errorFor("creditCard").errorFor("expirationMonth"));
            // @formatter:on
        }
    }
}