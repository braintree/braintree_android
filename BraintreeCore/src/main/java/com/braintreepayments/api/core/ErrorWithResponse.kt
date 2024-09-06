package com.braintreepayments.api.core

import android.os.Parcelable
import android.util.Log
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.GraphQLConstants.ErrorMessages
import com.braintreepayments.api.sharedutils.LoggingUtils
import kotlinx.parcelize.Parcelize
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
@Parcelize
data class ErrorWithResponse internal constructor(
    var statusCode: Int = 0,
    var errorResponse: String?,
    var fieldErrors: List<BraintreeError>? = null,
    override var message: String? = null,
) : Exception(), Parcelable {

    init {
        try {
            parseJson(errorResponse)
        } catch (e: JSONException) {
            Log.d(LoggingUtils.TAG, e.message.toString())
            fieldErrors = ArrayList()
        }
    }

    @Throws(JSONException::class)
    private fun parseJson(jsonString: String?) {
        jsonString?.let { JSONObject(it) }?.let { json ->
            message = json.optJSONObject(ERROR_KEY)
                ?.let { jsonObject ->
                    jsonObject.optString(MESSAGE_KEY, "").takeIf { it.isNotEmpty() }
                        ?: jsonObject.optString(DEV_MESSAGE_KEY, "")
                } ?: throw JSONException("Error key not found in JSON")
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

    companion object {
        private const val ERROR_KEY = "error"
        private const val MESSAGE_KEY = "message"
        private const val DEV_MESSAGE_KEY = "developer_message"
        private const val FIELD_ERRORS_KEY = "fieldErrors"
        private const val GRAPHQL_ERROR_CODE = 422

        /**
         * @suppress
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Throws(JSONException::class)
        @JvmStatic
        fun fromJson(json: String?) = ErrorWithResponse(errorResponse = json).apply {
            parseJson(json)
        }

        internal fun fromGraphQLJson(json: String?): ErrorWithResponse {
            val errorWithResponse = ErrorWithResponse(
                errorResponse = json,
                statusCode = GRAPHQL_ERROR_CODE
            )

            try {
                val errors = json
                    ?.let { JSONObject(it) }
                    ?.optJSONArray(GraphQLConstants.Keys.ERRORS)
                    ?: throw JSONException("Errors key not found in JSON")
                errorWithResponse.fieldErrors = BraintreeError.fromGraphQLJsonArray(errors)

                val fieldErrorsEmpty = errorWithResponse.fieldErrors?.isEmpty() ?: true
                errorWithResponse.message = if (fieldErrorsEmpty) {
                    errors.getJSONObject(0)?.getString(GraphQLConstants.Keys.MESSAGE)
                } else {
                    ErrorMessages.USER
                }
            } catch (e: JSONException) {
                errorWithResponse.apply {
                    Log.d(LoggingUtils.TAG, e.message.toString())
                    fieldErrors = ArrayList()
                }
            }
            return errorWithResponse
        }
    }
}
