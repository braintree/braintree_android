package com.braintreepayments.api

import android.os.Parcel
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ErrorsWithResponseUnitTest {

    @Test
    fun constructor_parsesErrorsCorrectly() {
        val response = Fixtures.ERRORS_CREDIT_CARD_ERROR_RESPONSE
        val errorWithResponse = ErrorWithResponse(422, response)
        assertEquals("Credit card is invalid", errorWithResponse.message)
        assertEquals(422, errorWithResponse.statusCode)
        assertNull(errorWithResponse.errorFor("creditCard")?.errorFor("postalCode"))
        assertEquals(
            "Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code",
            errorWithResponse.errorFor("creditCard")?.errorFor("base")?.message
        )
        assertEquals(
            "Credit card number is required",
            errorWithResponse.errorFor("creditCard")?.errorFor("number")?.message
        )
        assertEquals(
            "Expiration year is invalid",
            errorWithResponse.errorFor("creditCard")?.errorFor("expirationYear")?.message
        )
    }

    @Test
    fun constructor_handlesTopLevelErrors() {
        val topLevelError = Fixtures.ERRORS_AUTH_FINGERPRINT_ERROR
        val errorWithResponse = ErrorWithResponse(422, topLevelError)
        assertEquals("Authorization fingerprint is invalid", errorWithResponse.message)
        assertEquals(1, errorWithResponse.fieldErrors?.size)
    }

    @Test
    fun constructor_canHandleMultipleCategories() {
        val errors = Fixtures.ERRORS_COMPLEX_ERROR_RESPONSE
        val errorWithResponse = ErrorWithResponse(422, errors)
        assertEquals(3, errorWithResponse.errorFor("creditCard")?.fieldErrors?.size)
        assertEquals("is invalid", errorWithResponse.errorFor("customer")?.message)
        assertEquals(0, errorWithResponse.errorFor("customer")?.fieldErrors?.size)
    }

    @Test
    fun constructor_doesNotBlowUpParsingBadJson() {
        val badJson = Fixtures.RANDOM_JSON
        val errorWithResponse = ErrorWithResponse(422, badJson)
        assertEquals("Parsing error response failed", errorWithResponse.message)
    }

    @Test
    @Throws(JSONException::class)
    fun fromJson_parsesErrorsCorrectly() {
        val response = Fixtures.ERRORS_CREDIT_CARD_ERROR_RESPONSE
        val errorWithResponse = ErrorWithResponse.fromJson(response)
        assertEquals("Credit card is invalid", errorWithResponse.message)
        assertNull(errorWithResponse.errorFor("creditCard")?.errorFor("postalCode"))
        assertEquals(
            "Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code",
            errorWithResponse.errorFor("creditCard")?.errorFor("base")?.message
        )
        assertEquals(
            "Credit card number is required",
            errorWithResponse.errorFor("creditCard")?.errorFor("number")?.message
        )
        assertEquals(
            "Expiration year is invalid",
            errorWithResponse.errorFor("creditCard")?.errorFor("expirationYear")?.message
        )
    }

    @Test(expected = JSONException::class)
    @Throws(JSONException::class)
    fun fromJson_throwsExceptionIfJsonParsingFails() {
        ErrorWithResponse.fromJson(Fixtures.RANDOM_JSON)
    }

    @Test
    fun fromGraphQLJson_parsesErrorsCorrectly() {
        val response = Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_ERROR
        val errorWithResponse = ErrorWithResponse.fromGraphQLJson(response)
        assertEquals("Input is invalid.", errorWithResponse.message)
        assertEquals(422, errorWithResponse.statusCode)
        assertEquals(
            "Expiration month is invalid",
            errorWithResponse.errorFor("creditCard")?.errorFor("expirationMonth")?.message
        )
        assertEquals(
            "Expiration year is invalid",
            errorWithResponse.errorFor("creditCard")?.errorFor("expirationYear")?.message
        )
        assertEquals(
            "CVV verification failed",
            errorWithResponse.errorFor("creditCard")?.errorFor("cvv")?.message
        )
    }

    @Test
    fun fromGraphQLJson_parsesGraphQLCoercionErrorsCorrectly() {
        val response = Fixtures.ERRORS_GRAPHQL_COERCION_ERROR
        val errorWithResponse = ErrorWithResponse.fromGraphQLJson(response)
        assertEquals(
            "Variable 'input' has coerced Null value for NonNull type 'String!'",
            errorWithResponse.message
        )
        assertEquals(422, errorWithResponse.statusCode)
    }

    @Test
    fun fromGraphQLJson_doesNotBlowUpParsingBadJson() {
        val badJson = Fixtures.RANDOM_JSON
        val errorWithResponse = ErrorWithResponse.fromGraphQLJson(badJson)
        assertEquals("Parsing error response failed", errorWithResponse.message)
    }

    @Test
    fun REST_tokenizeCardDuplicate() {
        val errorWithResponse = ErrorWithResponse(422, Fixtures.ERRORS_CREDIT_CARD_DUPLICATE)
        assertEquals(
            81724, errorWithResponse.errorFor("creditCard")?.errorFor("number")?.code
        )
    }

    @Test
    fun GraphQL_tokenizeCardDuplicate() {
        val errorWithResponse =
            ErrorWithResponse.fromGraphQLJson(Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_DUPLICATE_ERROR)
        assertEquals(
            81724, errorWithResponse.errorFor("creditCard")?.errorFor("number")?.code
        )
    }

    @Test
    @Throws(JSONException::class)
    fun parcelsCorrectly() {
        val error = ErrorWithResponse.fromJson(Fixtures.ERRORS_CREDIT_CARD_ERROR_RESPONSE)
        val parcel = Parcel.obtain()
        error.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val parceled = ErrorWithResponse.CREATOR.createFromParcel(parcel)
        assertEquals(error.statusCode, parceled.statusCode)
        assertEquals(error.message, parceled.message)
        assertEquals(error.errorResponse, parceled.errorResponse)
        assertEquals(error.fieldErrors?.size, parceled.fieldErrors?.size)
    }
}
