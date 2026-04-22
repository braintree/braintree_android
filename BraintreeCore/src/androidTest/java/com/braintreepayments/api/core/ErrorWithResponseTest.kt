package com.braintreepayments.api.core

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.parcelize.parcelableCreator
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ErrorWithResponseTest {

    @Test(timeout = 1000)
    fun fromJson_parsesMessageFromErrorKey() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("message", "Credit card is invalid"))
        }.toString()

        val error = ErrorWithResponse.fromJson(json)

        assertEquals("Credit card is invalid", error.message)
    }

    @Test(timeout = 1000)
    fun fromJson_parsesErrorMessageKey() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("errorMessage", "Something went wrong"))
        }.toString()

        val error = ErrorWithResponse.fromJson(json)

        assertEquals("Something went wrong", error.message)
    }

    @Test(timeout = 1000)
    fun fromJson_parsesDeveloperMessageKey() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("developer_message", "Invalid params"))
        }.toString()

        val error = ErrorWithResponse.fromJson(json)

        assertEquals("Invalid params", error.message)
    }

    @Test(timeout = 1000)
    fun fromJson_parsesFieldErrors() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("message", "Validation failed"))
            put("fieldErrors", JSONArray().put(
                JSONObject().apply {
                    put("field", "creditCard")
                    put("message", "Credit card is invalid")
                    put("fieldErrors", JSONArray().put(
                        JSONObject().apply {
                            put("field", "number")
                            put("message", "Number is required")
                            put("code", 81714)
                        }
                    ))
                }
            ))
        }.toString()

        val error = ErrorWithResponse.fromJson(json)

        val fieldErrors = requireNotNull(error.fieldErrors)
        assertEquals(1, fieldErrors.size)
        assertEquals("creditCard", fieldErrors[0].field)
    }

    @Test(timeout = 1000, expected = org.json.JSONException::class)
    fun fromJson_withMalformedJson_throwsJSONException() {
        ErrorWithResponse.fromJson("not valid json")
    }

    @Test(timeout = 1000)
    fun fromJson_withNullJson_doesNotCrash() {
        val error = ErrorWithResponse.fromJson(null)
        assertNotNull(error)
    }

    @Test(timeout = 1000)
    fun fromGraphQLJson_parsesUserErrors() {
        val json = JSONObject().apply {
            put("errors", JSONArray().put(
                JSONObject().apply {
                    put("message", "Number is invalid")
                    put("extensions", JSONObject().apply {
                        put("errorType", "user_error")
                        put("legacyCode", 81724)
                        put("inputPath", JSONArray().apply {
                            put("input")
                            put("creditCardNumber")
                        })
                    })
                }
            ))
        }.toString()

        val error = ErrorWithResponse.fromGraphQLJson(json)

        assertEquals(422, error.statusCode)
        assertEquals("Input is invalid.", error.message)
        val fieldErrors = requireNotNull(error.fieldErrors)
        assertEquals(1, fieldErrors.size)
        assertEquals("creditCardNumber", fieldErrors[0].field)
    }

    @Test(timeout = 1000)
    fun fromGraphQLJson_withNonUserError_usesErrorMessage() {
        val json = JSONObject().apply {
            put("errors", JSONArray().put(
                JSONObject().apply {
                    put("message", "Some server error")
                    put("extensions", JSONObject().apply {
                        put("errorType", "developer_error")
                        put("inputPath", JSONArray().apply {
                            put("input")
                            put("field")
                        })
                    })
                }
            ))
        }.toString()

        val error = ErrorWithResponse.fromGraphQLJson(json)

        assertEquals("Some server error", error.message)
        val fieldErrors = requireNotNull(error.fieldErrors)
        assertTrue(fieldErrors.isEmpty())
    }

    @Test(timeout = 1000)
    fun fromGraphQLJson_withMalformedJson_setsParsingErrorMessage() {
        val error = ErrorWithResponse.fromGraphQLJson("not valid json")

        assertEquals("Parsing error response failed", error.message)
        val fieldErrors = requireNotNull(error.fieldErrors)
        assertTrue(fieldErrors.isEmpty())
    }

    @Test(timeout = 1000)
    fun fromGraphQLJson_withNullJson_doesNotCrash() {
        val error = ErrorWithResponse.fromGraphQLJson(null)
        assertNotNull(error)
        assertEquals(422, error.statusCode)
    }

    @Test(timeout = 1000)
    fun errorFor_findsDirectFieldError() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("message", "Validation failed"))
            put("fieldErrors", JSONArray().put(
                JSONObject().apply {
                    put("field", "creditCard")
                    put("message", "Credit card is invalid")
                }
            ))
        }.toString()

        val error = ErrorWithResponse.fromJson(json)
        val creditCardError = error.errorFor("creditCard")

        requireNotNull(creditCardError)
        assertEquals("creditCard", creditCardError.field)
    }

    @Test(timeout = 1000)
    fun errorFor_findsNestedFieldError() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("message", "Validation failed"))
            put("fieldErrors", JSONArray().put(
                JSONObject().apply {
                    put("field", "creditCard")
                    put("fieldErrors", JSONArray().put(
                        JSONObject().apply {
                            put("field", "number")
                            put("message", "Number is required")
                        }
                    ))
                }
            ))
        }.toString()

        val error = ErrorWithResponse.fromJson(json)
        val numberError = error.errorFor("number")

        requireNotNull(numberError)
        assertEquals("number", numberError.field)
    }

    @Test(timeout = 1000)
    fun errorFor_returnsNullWhenNotFound() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("message", "Validation failed"))
            put("fieldErrors", JSONArray().put(
                JSONObject().apply {
                    put("field", "creditCard")
                    put("message", "Credit card is invalid")
                }
            ))
        }.toString()

        val error = ErrorWithResponse.fromJson(json)
        assertNull(error.errorFor("customer"))
    }

    @Test(timeout = 1000)
    fun errorFor_returnsNullWhenFieldErrorsIsNull() {
        val error = ErrorWithResponse(errorResponse = null)
        assertNull(error.errorFor("creditCard"))
    }

    @Test(timeout = 1000)
    fun parceling_preservesAllFields() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("message", "Validation failed"))
            put("fieldErrors", JSONArray().put(
                JSONObject().apply {
                    put("field", "creditCard")
                    put("message", "Credit card is invalid")
                    put("code", 81724)
                }
            ))
        }.toString()

        val original = ErrorWithResponse.fromJson(json)
        original.statusCode = 422

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ErrorWithResponse>().createFromParcel(parcel)

        assertEquals(original.statusCode, restored.statusCode)
        assertEquals(original.message, restored.message)
        assertEquals(original.errorResponse, restored.errorResponse)
        val restoredFieldErrors = requireNotNull(restored.fieldErrors)
        assertEquals(1, restoredFieldErrors.size)
        assertEquals("creditCard", restoredFieldErrors[0].field)

        parcel.recycle()
    }

    @Test(timeout = 1000)
    fun toString_includesStatusCodeAndMessage() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("message", "Validation failed"))
        }.toString()

        val error = ErrorWithResponse.fromJson(json)
        error.statusCode = 422
        val result = error.toString()

        assertTrue(result.contains("422"))
        assertTrue(result.contains("Validation failed"))
    }

    @Test(timeout = 1000)
    fun errorResponse_preservesOriginalJson() {
        val json = JSONObject().apply {
            put("error", JSONObject().put("message", "Test error"))
        }.toString()

        val error = ErrorWithResponse.fromJson(json)

        assertEquals(json, error.errorResponse)
    }
}