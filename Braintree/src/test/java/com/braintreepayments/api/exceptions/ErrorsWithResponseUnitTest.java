package com.braintreepayments.api.exceptions;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricGradleTestRunner.class)
public class ErrorsWithResponseUnitTest {

    @Test
    public void parsesErrorsCorrectly() {
        String response = stringFromFixture("errors/credit_card_error_response.json");

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

    @Test
    public void fromJson_parsesErrorsCorrectly() throws JSONException {
        String response = stringFromFixture("errors/credit_card_error_response.json");

        ErrorWithResponse errorWithResponse = ErrorWithResponse.fromJson(response);

        assertEquals("Credit card is invalid", errorWithResponse.getMessage());
        assertNull(errorWithResponse.errorFor("creditCard").errorFor("postalCode"));
        assertEquals("Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code",
                errorWithResponse.errorFor("creditCard").errorFor("base").getMessage());
        assertEquals("Credit card number is required",
                errorWithResponse.errorFor("creditCard").errorFor("number").getMessage());
        assertEquals("Expiration year is invalid",
                errorWithResponse.errorFor("creditCard").errorFor("expirationYear").getMessage());
    }

    @Test(expected = JSONException.class)
    public void fromJson_throwsExceptionIfJsonParsingFails() throws JSONException {
        ErrorWithResponse.fromJson(stringFromFixture("random_json.json"));
    }

    @Test
    public void handlesTopLevelErrors() {
        String topLevelError = stringFromFixture("errors/auth_fingerprint_error.json");

        ErrorWithResponse errorWithResponse = new ErrorWithResponse(422, topLevelError);

        assertEquals("Authorization fingerprint is invalid", errorWithResponse.getMessage());
        assertEquals(1, errorWithResponse.getFieldErrors().size());
    }

    @Test
    public void canHandleMultipleCategories() {
        String errors = stringFromFixture("errors/complex_error_response.json");

        ErrorWithResponse errorWithResponse = new ErrorWithResponse(422, errors);

        assertEquals(3, errorWithResponse.errorFor("creditCard").getFieldErrors().size());
        assertEquals("is invalid", errorWithResponse.errorFor("customer").getMessage());
        assertEquals(0, errorWithResponse.errorFor("customer").getFieldErrors().size());
    }

    @Test
    public void doesNotBlowUpParsingBadJson() {
        String badJson = stringFromFixture("random_json.json");

        ErrorWithResponse errorWithResponse = new ErrorWithResponse(422, badJson);

        assertEquals("Parsing error response failed", errorWithResponse.getMessage());
    }
}
