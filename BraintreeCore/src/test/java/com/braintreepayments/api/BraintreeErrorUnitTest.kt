package com.braintreepayments.api

import android.os.Parcel
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BraintreeErrorUnitTest {

    @Test
    @Throws(JSONException::class)
    fun parcelsCorrectly() {
        val errorResponse = JSONObject(Fixtures.ERRORS_CREDIT_CARD_ERROR_RESPONSE)
        val errors: List<BraintreeError> =
            BraintreeError.fromJsonArray(errorResponse.getJSONArray("fieldErrors"))
        assertEquals(1, errors.size)

        val error = errors[0]
        val parcel = Parcel.obtain()
        error.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val parceled = BraintreeError.CREATOR.createFromParcel(parcel)

        assertEquals(error.field, parceled.field)
        assertEquals(error.message, parceled.message)
        assertEquals(error.fieldErrors!!.size, parceled.fieldErrors!!.size)
    }

    @Test
    @Throws(Exception::class)
    fun graphQLErrors_parcelCorrectly() {
        val errorResponse = JSONObject(Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_ERROR)
        val errors = BraintreeError.fromGraphQLJsonArray(errorResponse.getJSONArray("errors"))
        assertEquals(1, errors.size)

        val error = errors[0]
        val parcel = Parcel.obtain()
        error.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val parceled = BraintreeError.CREATOR.createFromParcel(parcel)

        assertEquals(error.field, parceled.field)
        assertEquals(error.message, parceled.message)
        assertEquals(error.fieldErrors!!.size, parceled.fieldErrors!!.size)
    }
}
