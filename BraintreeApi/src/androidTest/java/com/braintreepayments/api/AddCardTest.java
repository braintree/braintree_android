package com.braintreepayments.api;

import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;

import static com.braintreepayments.api.CardNumber.VISA;

public class AddCardTest extends AndroidTestCase {

    private BraintreeApi mBraintreeApi;

    public void setUp() {
        TestUtils.setUp(getContext());
        mBraintreeApi = new BraintreeApi(getContext(), new TestClientTokenBuilder().build());
    }

    public void testCanAddCard() throws ErrorWithResponse, BraintreeException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("01")
                .expirationYear("2017");

        Card addedCard = mBraintreeApi.create(cardBuilder);

        assertNotNull(addedCard.getNonce());
    }

    public void testCanTokenizeInvalidCard() throws ErrorWithResponse, BraintreeException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("This is a credit card");

        String nonce = mBraintreeApi.tokenize(cardBuilder);

        assertNotNull(nonce);
    }

    public void testThrowsErrorOnServerFailure() throws BraintreeException {
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