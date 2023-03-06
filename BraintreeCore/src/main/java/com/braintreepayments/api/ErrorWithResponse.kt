package com.braintreepayments.api

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.braintreepayments.api.GraphQLConstants.ErrorMessages
import org.json.JSONException
import org.json.JSONObject

/**
 * Error container returned when the Braintree server returns a 422 Unprocessible Entity.
 * A 422 occurs when a request is properly formed, but the server was unable to take the requested
 * action due to bad user data.
 *
 * ErrorWithResponse parses the server's error response and exposes the errors.
 *
 * @property message Human readable top level summary of the error.
 */
open class ErrorWithResponse : Exception, Parcelable {

    /**
     * @return HTTP status code from the Braintree gateway.
     */
    var statusCode: Int = 0
        private set

    private var _message: String? = null

    override val message: String?
        get() = _message

    /**
     * @return The full error response as a [String].
     */
    var errorResponse: String? = null
        private set

    /**
     * @return All the field errors.
     */
    var fieldErrors: List<BraintreeError>? = null
        private set

    internal constructor(statusCode: Int, jsonString: String?) {
        this.statusCode = statusCode
        errorResponse = jsonString
        try {
            parseJson(jsonString)
        } catch (e: JSONException) {
            _message = "Parsing error response failed"
            fieldErrors = ArrayList()
        }
    }

    private constructor() {}

    @Throws(JSONException::class)
    private fun parseJson(jsonString: String?) {
        val json = JSONObject(jsonString)
        _message = json.getJSONObject(ERROR_KEY).getString(MESSAGE_KEY)
        fieldErrors = BraintreeError.fromJsonArray(json.optJSONArray(FIELD_ERRORS_KEY))
    }


    /**
     * Method to extract an error for an individual field, e.g. creditCard, customer, etc.
     *
     * @param field Name of the field desired, expected to be in camelCase.
     * @return [BraintreeError] for the field searched, or `null` if not found.
     */
    fun errorFor(field: String): BraintreeError? {
        var returnError: BraintreeError?
        if (fieldErrors != null) {
            for (error in fieldErrors!!) {
                if (error.field == field) {
                    return error
                } else if (error.fieldErrors != null) {
                    returnError = error.errorFor(field)
                    if (returnError != null) {
                        return returnError
                    }
                }
            }
        }
        return null
    }

    override fun toString(): String {
        return """
            ErrorWithResponse ($statusCode): $message
            ${fieldErrors.toString()}
            """.trimIndent()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(statusCode)
        dest.writeString(message)
        dest.writeString(errorResponse)
        dest.writeTypedList(fieldErrors)
    }

    protected constructor(`in`: Parcel) {
        statusCode = `in`.readInt()
        _message = `in`.readString()
        errorResponse = `in`.readString()
        fieldErrors = `in`.createTypedArrayList(BraintreeError.CREATOR)
    }

    companion object {
        private const val ERROR_KEY = "error"
        private const val MESSAGE_KEY = "message"
        private const val FIELD_ERRORS_KEY = "fieldErrors"

        @Throws(JSONException::class)
        fun fromJson(json: String?): ErrorWithResponse {
            val errorWithResponse = ErrorWithResponse()
            errorWithResponse.errorResponse = json
            errorWithResponse.parseJson(json)
            return errorWithResponse
        }

        fun fromGraphQLJson(json: String?): ErrorWithResponse {
            val errorWithResponse = ErrorWithResponse()
            errorWithResponse.errorResponse = json
            errorWithResponse.statusCode = 422
            try {
                val errors = JSONObject(json).getJSONArray(GraphQLConstants.Keys.ERRORS)
                errorWithResponse.fieldErrors = BraintreeError.fromGraphQLJsonArray(errors)
                if (errorWithResponse.fieldErrors?.isEmpty() == true) {
                    errorWithResponse._message =
                        errors.getJSONObject(0).getString(GraphQLConstants.Keys.MESSAGE)
                } else {
                    errorWithResponse._message = ErrorMessages.USER
                }
            } catch (e: JSONException) {
                errorWithResponse._message = "Parsing error response failed"
                errorWithResponse.fieldErrors = ArrayList()
            }
            return errorWithResponse
        }

        // Ref: https://medium.com/the-lazy-coders-journal/easy-parcelable-in-kotlin-the-lazy-coders-way-9683122f4c00
        @JvmField
        val CREATOR = object : Parcelable.Creator<ErrorWithResponse> {
            override fun createFromParcel(source: Parcel) = ErrorWithResponse(source)
            override fun newArray(size: Int) = arrayOfNulls<ErrorWithResponse>(size)
        }
    }
}