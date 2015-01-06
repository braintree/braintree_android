package com.braintreepayments.api.exceptions;

import android.test.InstrumentationTestCase;

import com.braintreepayments.testutils.FixturesHelper;

public class ErrorsWithResponseTest extends InstrumentationTestCase {

    public void testParsesErrorsCorrectly() throws UnexpectedException {
        String response = FixturesHelper.stringFromFixture(getInstrumentation().getContext(),
                "errors/credit_card_error_response.json");

        ErrorWithResponse errorWithResponse = new ErrorWithResponse(422, response);

        assertEquals("Credit card is invalid", errorWithResponse.getMessage());
        assertEquals(422, errorWithResponse.getStatusCode());

        assertNull(errorWithResponse.errorFor("creditCard").errorFor("postalCode"));

        assertEquals("Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code",
                errorWithResponse.errorFor("creditCard").errorFor("base").getMessage());
        assertEquals("Credit card number is required",
                errorWithResponse.errorFor("creditCard").errorFor("number").getMessage());
        assertEquals("Expiration year is invalid",
                errorWithResponse.errorFor("creditCard").errorFor("expirationYear").getMessage());
    }

    public void testHandlesTopLevelErrors() throws UnexpectedException {
        String topLevelError = FixturesHelper.stringFromFixture(getInstrumentation().getContext(),
                "errors/auth_fingerprint_error.json");

        ErrorWithResponse errorWithResponse = new ErrorWithResponse(422, topLevelError);

        assertEquals("Authorization fingerprint is invalid", errorWithResponse.getMessage());
        assertEquals(1, errorWithResponse.getFieldErrors().size());
    }

    public void testCanHandleMultipleCategories() throws UnexpectedException {
        String errors = FixturesHelper.stringFromFixture(getInstrumentation().getContext(),
                "errors/complex_error_response.json");

        ErrorWithResponse errorWithResponse = new ErrorWithResponse(422, errors);

        assertEquals(3, errorWithResponse.errorFor("creditCard").getFieldErrors().size());

        assertEquals("is invalid", errorWithResponse.errorFor("customer").getMessage());
        assertNull(errorWithResponse.errorFor("customer").getFieldErrors());
    }
}
