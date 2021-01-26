package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class ErrorsWithResponseUnitTest {

    @Test
    public void constructor_parsesErrorsCorrectly() {
        String response = Fixtures.ERRORS_CREDIT_CARD_ERROR_RESPONSE;
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
    public void constructor_handlesTopLevelErrors() {
        String topLevelError = Fixtures.ERRORS_AUTH_FINGERPRINT_ERROR;
        ErrorWithResponse errorWithResponse = new ErrorWithResponse(422, topLevelError);

        assertEquals("Authorization fingerprint is invalid", errorWithResponse.getMessage());
        assertEquals(1, errorWithResponse.getFieldErrors().size());
    }

    @Test
    public void constructor_canHandleMultipleCategories() {
        String errors = Fixtures.ERRORS_COMPLEX_ERROR_RESPONSE;
        ErrorWithResponse errorWithResponse = new ErrorWithResponse(422, errors);

        assertEquals(3, errorWithResponse.errorFor("creditCard").getFieldErrors().size());
        assertEquals("is invalid", errorWithResponse.errorFor("customer").getMessage());
        assertEquals(0, errorWithResponse.errorFor("customer").getFieldErrors().size());
    }

    @Test
    public void constructor_doesNotBlowUpParsingBadJson() {
        String badJson = Fixtures.RANDOM_JSON;
        ErrorWithResponse errorWithResponse = new ErrorWithResponse(422, badJson);

        assertEquals("Parsing error response failed", errorWithResponse.getMessage());
    }

    @Test
    public void fromJson_parsesErrorsCorrectly() throws JSONException {
        String response = Fixtures.ERRORS_CREDIT_CARD_ERROR_RESPONSE;
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
        ErrorWithResponse.fromJson(Fixtures.RANDOM_JSON);
    }

    @Test
    public void fromGraphQLJson_parsesErrorsCorrectly() {
        String response = Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_ERROR;
        ErrorWithResponse errorWithResponse = ErrorWithResponse.fromGraphQLJson(response);

        assertEquals("Input is invalid.", errorWithResponse.getMessage());
        assertEquals(422, errorWithResponse.getStatusCode());
        assertEquals("Expiration month is invalid",
                errorWithResponse.errorFor("creditCard").errorFor("expirationMonth").getMessage());
        assertEquals("Expiration year is invalid",
                errorWithResponse.errorFor("creditCard").errorFor("expirationYear").getMessage());
        assertEquals("CVV verification failed",
                errorWithResponse.errorFor("creditCard").errorFor("cvv").getMessage());
    }

    @Test
    public void fromGraphQLJson_parsesGraphQLCoercionErrorsCorrectly() {
        String response = Fixtures.ERRORS_GRAPHQL_COERCION_ERROR;
        ErrorWithResponse errorWithResponse = ErrorWithResponse.fromGraphQLJson(response);

        assertEquals("Variable 'input' has coerced Null value for NonNull type 'String!'", errorWithResponse.getMessage());
        assertEquals(422, errorWithResponse.getStatusCode());
    }

    @Test
    public void fromGraphQLJson_doesNotBlowUpParsingBadJson() {
        String badJson = Fixtures.RANDOM_JSON;
        ErrorWithResponse errorWithResponse = ErrorWithResponse.fromGraphQLJson(badJson);

        assertEquals("Parsing error response failed", errorWithResponse.getMessage());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        ErrorWithResponse error = ErrorWithResponse.fromJson(Fixtures.ERRORS_CREDIT_CARD_ERROR_RESPONSE);

        Parcel parcel = Parcel.obtain();
        error.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ErrorWithResponse parceled = ErrorWithResponse.CREATOR.createFromParcel(parcel);

        assertEquals(error.getStatusCode(), parceled.getStatusCode());
        assertEquals(error.getMessage(), parceled.getMessage());
        assertEquals(error.getErrorResponse(), parceled.getErrorResponse());
        assertEquals(error.getFieldErrors().size(), parceled.getFieldErrors().size());
    }
}
