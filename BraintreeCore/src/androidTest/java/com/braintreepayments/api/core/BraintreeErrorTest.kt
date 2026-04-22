package com.braintreepayments.api.core

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.parcelize.parcelableCreator
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class BraintreeErrorTest {

    @Test(timeout = 1000)
    fun fromJson_parsesFieldMessageAndCode() {
        val json = JSONObject().apply {
            put("field", "creditCard")
            put("message", "Credit card is invalid")
            put("code", 81724)
        }

        val error = BraintreeError.fromJson(json)

        assertEquals("creditCard", error.field)
        assertEquals("Credit card is invalid", error.message)
        assertEquals(81724, error.code)
    }

    @Test(timeout = 1000)
    fun fromJson_withMissingFields_returnsDefaults() {
        val error = BraintreeError.fromJson(JSONObject())

        assertNull(error.field)
        assertNull(error.message)
        assertEquals(-1, error.code)
    }

    @Test(timeout = 1000)
    fun fromJson_parsesNestedFieldErrors() {
        val nestedError = JSONObject().apply {
            put("field", "number")
            put("message", "Number is required")
            put("code", 81714)
        }
        val json = JSONObject().apply {
            put("field", "creditCard")
            put("message", "Credit card is invalid")
            put("fieldErrors", JSONArray().put(nestedError))
        }

        val error = BraintreeError.fromJson(json)

        val fieldErrors = requireNotNull(error.fieldErrors)
        assertEquals(1, fieldErrors.size)
        assertEquals("number", fieldErrors[0].field)
        assertEquals("Number is required", fieldErrors[0].message)
        assertEquals(81714, fieldErrors[0].code)
    }

    @Test(timeout = 1000)
    fun fromJsonArray_parsesMultipleErrors() {
        val jsonArray = JSONArray().apply {
            put(JSONObject().apply {
                put("field", "creditCard")
                put("message", "Credit card is invalid")
            })
            put(JSONObject().apply {
                put("field", "customer")
                put("message", "Customer is invalid")
            })
        }

        val errors = BraintreeError.fromJsonArray(jsonArray)

        assertEquals(2, errors.size)
        assertEquals("creditCard", errors[0].field)
        assertEquals("customer", errors[1].field)
    }

    @Test(timeout = 1000)
    fun fromJsonArray_withNull_returnsEmptyList() {
        val errors = BraintreeError.fromJsonArray(null)
        assertTrue(errors.isEmpty())
    }

    @Test(timeout = 1000)
    fun fromJsonArray_withEmptyArray_returnsEmptyList() {
        val errors = BraintreeError.fromJsonArray(JSONArray())
        assertTrue(errors.isEmpty())
    }

    @Test(timeout = 1000)
    fun errorFor_findsDirectChildError() {
        val error = BraintreeError(
            field = "creditCard",
            fieldErrors = mutableListOf(
                BraintreeError(field = "number", message = "Number is required"),
                BraintreeError(field = "expirationDate", message = "Expiration is required")
            )
        )

        val numberError = error.errorFor("number")

        requireNotNull(numberError)
        assertEquals("number", numberError.field)
        assertEquals("Number is required", numberError.message)
    }

    @Test(timeout = 1000)
    fun errorFor_findsNestedError() {
        val error = BraintreeError(
            field = "creditCard",
            fieldErrors = mutableListOf(
                BraintreeError(
                    field = "billingAddress",
                    fieldErrors = mutableListOf(
                        BraintreeError(field = "postalCode", message = "Postal code is required")
                    )
                )
            )
        )

        val postalCodeError = error.errorFor("postalCode")

        requireNotNull(postalCodeError)
        assertEquals("postalCode", postalCodeError.field)
    }

    @Test(timeout = 1000)
    fun errorFor_returnsNullWhenNotFound() {
        val error = BraintreeError(
            field = "creditCard",
            fieldErrors = mutableListOf(
                BraintreeError(field = "number", message = "Number is required")
            )
        )

        assertNull(error.errorFor("cvv"))
    }

    @Test(timeout = 1000)
    fun errorFor_returnsNullWhenFieldErrorsIsNull() {
        val error = BraintreeError(field = "creditCard")
        assertNull(error.errorFor("number"))
    }

    @Test(timeout = 1000)
    fun fromGraphQLJsonArray_parsesUserErrors() {
        val graphQLError = JSONObject().apply {
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

        val errors = BraintreeError.fromGraphQLJsonArray(JSONArray().put(graphQLError))

        assertEquals(1, errors.size)
        assertEquals("creditCardNumber", errors[0].field)
        assertEquals("Number is invalid", errors[0].message)
        assertEquals(81724, errors[0].code)
    }

    @Test(timeout = 1000)
    fun fromGraphQLJsonArray_skipsNonUserErrors() {
        val graphQLError = JSONObject().apply {
            put("message", "Server error")
            put("extensions", JSONObject().apply {
                put("errorType", "developer_error")
                put("inputPath", JSONArray().apply {
                    put("input")
                    put("creditCardNumber")
                })
            })
        }

        val errors = BraintreeError.fromGraphQLJsonArray(JSONArray().put(graphQLError))

        assertTrue(errors.isEmpty())
    }

    @Test(timeout = 1000)
    fun fromGraphQLJsonArray_parsesNestedInputPaths() {
        val graphQLError = JSONObject().apply {
            put("message", "Postal code is invalid")
            put("extensions", JSONObject().apply {
                put("errorType", "user_error")
                put("legacyCode", 81813)
                put("inputPath", JSONArray().apply {
                    put("input")
                    put("creditCard")
                    put("billingAddress")
                    put("postalCode")
                })
            })
        }

        val errors = BraintreeError.fromGraphQLJsonArray(JSONArray().put(graphQLError))

        assertEquals(1, errors.size)
        assertEquals("creditCard", errors[0].field)
        val billingAddress = requireNotNull(errors[0].errorFor("billingAddress"))
        val postalCode = requireNotNull(billingAddress.errorFor("postalCode"))
        assertEquals("Postal code is invalid", postalCode.message)
    }

    @Test(timeout = 1000)
    fun fromGraphQLJsonArray_withNull_returnsEmptyList() {
        val errors = BraintreeError.fromGraphQLJsonArray(null)
        assertTrue(errors.isEmpty())
    }

    @Test(timeout = 1000)
    fun parceling_preservesAllFields() {
        val original = BraintreeError(
            field = "creditCard",
            message = "Credit card is invalid",
            code = 81724,
            fieldErrors = mutableListOf(
                BraintreeError(field = "number", message = "Number is required", code = 81714)
            )
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<BraintreeError>().createFromParcel(parcel)

        assertEquals(original.field, restored.field)
        assertEquals(original.message, restored.message)
        assertEquals(original.code, restored.code)
        val restoredFieldErrors = requireNotNull(restored.fieldErrors)
        assertEquals(1, restoredFieldErrors.size)
        assertEquals("number", restoredFieldErrors[0].field)

        parcel.recycle()
    }

    @Test(timeout = 1000)
    fun toString_includesFieldAndMessage() {
        val error = BraintreeError(field = "creditCard", message = "Credit card is invalid")
        val result = error.toString()

        assertTrue(result.contains("creditCard"))
        assertTrue(result.contains("Credit card is invalid"))
    }
}
