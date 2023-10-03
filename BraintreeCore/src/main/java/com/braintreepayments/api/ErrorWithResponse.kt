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
 * @property statusCode HTTP status code from the Braintree gateway.
 * @property message Human readable top level summary of the error.
 * @property errorResponse The full error response as a [String].
 * @property fieldErrors All the field errors.
 */
open class ErrorWithResponse : Exception, Parcelable {

    open var statusCode: Int = 0
        internal set

    private var _message: String? = null
    override val message: String?
        get() = _message

    private var _originalResponse: String? = null
    open val errorResponse: String?
        get() = _originalResponse

    open var fieldErrors: List<BraintreeError>? = null
        internal set

    private constructor()

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(statusCode: Int, jsonString: String?) {
        this.statusCode = statusCode
        _originalResponse = jsonString
        try {
            parseJson(jsonString)
        } catch (e: JSONException) {
            _message = "Parsing error response failed"
            fieldErrors = ArrayList()
        }
    }

    @Throws(JSONException::class)
    private fun parseJson(jsonString: String?) {
        jsonString?.let { JSONObject(it) }?.let { json ->
            _message = json.getJSONObject(ERROR_KEY).getString(MESSAGE_KEY)
            fieldErrors = BraintreeError.fromJsonArray(json.optJSONArray(FIELD_ERRORS_KEY))
        }
    }

    /**
     * Method to extract an error for an individual field, e.g. creditCard, customer, etc.
     *
     * @param field Name of the field desired, expected to be in camelCase.
     * @return [BraintreeError] for the field searched, or `null` if not found.
     */
    fun errorFor(field: String): BraintreeError? {
        if (fieldErrors == null) return null

        for (error in fieldErrors!!) {
            if (error.field == field) {
                return error
            } else if (error.fieldErrors != null) {
                val returnError = error.errorFor(field)
                if (returnError != null) {
                    return returnError
                }
            }
        }
        return null
    }

    override fun toString(): String {
        return """
            ErrorWithResponse ($statusCode): $message
            $fieldErrors
        """.trimIndent()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(statusCode)
        dest.writeString(message)
        dest.writeString(_originalResponse)
        dest.writeTypedList(fieldErrors)
    }

    protected constructor(inParcel: Parcel) {
        statusCode = inParcel.readInt()
        _message = inParcel.readString()
        _originalResponse = inParcel.readString()
        fieldErrors = inParcel.createTypedArrayList(BraintreeError.CREATOR)
    }

    companion object {
        private const val ERROR_KEY = "error"
        private const val MESSAGE_KEY = "message"
        private const val FIELD_ERRORS_KEY = "fieldErrors"
        private const val GRAPHQL_ERROR_CODE = 422

        /**
         * @suppress
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Throws(JSONException::class)
        @JvmStatic
        fun fromJson(json: String?) = ErrorWithResponse().apply {
            _originalResponse = json
            parseJson(json)
        }

        internal fun fromGraphQLJson(json: String?): ErrorWithResponse {
            val errorWithResponse = ErrorWithResponse().apply {
                _originalResponse = json
                statusCode = GRAPHQL_ERROR_CODE
            }

            try {
                val errors =
                    json?.let { JSONObject(it) }?.getJSONArray(GraphQLConstants.Keys.ERRORS)
                errorWithResponse.fieldErrors = BraintreeError.fromGraphQLJsonArray(errors)

                val fieldErrorsEmpty = errorWithResponse.fieldErrors?.isEmpty() ?: true
                if (fieldErrorsEmpty) {
                    errorWithResponse._message =
                        errors?.getJSONObject(0)?.getString(GraphQLConstants.Keys.MESSAGE)
                } else {
                    errorWithResponse._message = ErrorMessages.USER
                }
            } catch (e: JSONException) {
                errorWithResponse.apply {
                    _message = "Parsing error response failed"
                    fieldErrors = ArrayList()
                }
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
