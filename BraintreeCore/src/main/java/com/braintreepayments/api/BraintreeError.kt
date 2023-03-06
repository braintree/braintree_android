package com.braintreepayments.api

import android.os.Parcel
import android.os.Parcelable
import com.braintreepayments.api.GraphQLConstants.ErrorTypes
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BraintreeError : Parcelable {
    /**
     * @return Field name this object represents.
     */
    var field: String? = null
        private set

    /**
     * @return Human readable summary of the error for field. May be `null`.
     */
    var message: String? = null
        private set
    internal var fieldErrors: MutableList<BraintreeError>? = null

    /**
     * @return Error code if one exists; defaults to [.UNKNOWN_CODE] otherwise
     */
    // default value
    var code = UNKNOWN_CODE
        private set

    /**
     * @return [BraintreeError] objects for any errors nested under this field.
     */
    fun getFieldErrors(): List<BraintreeError>? {
        return fieldErrors
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
                } else if (error.getFieldErrors() != null) {
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
        return "BraintreeError for " + field + ": " + message + " -> " +
                if (fieldErrors != null) fieldErrors.toString() else ""
    }

    internal constructor() {}

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(field)
        dest.writeString(message)
        dest.writeTypedList(fieldErrors)
    }

    protected constructor(`in`: Parcel) {
        field = `in`.readString()
        message = `in`.readString()
        fieldErrors = `in`.createTypedArrayList(CREATOR)
    }

    companion object {
        private const val FIELD_KEY = "field"
        private const val MESSAGE_KEY = "message"
        private const val FIELD_ERRORS_KEY = "fieldErrors"
        private const val CODE_KEY = "code"
        private const val UNKNOWN_CODE = -1
        fun fromJsonArray(json: JSONArray?): MutableList<BraintreeError> {
            var json = json
            if (json == null) {
                json = JSONArray()
            }
            val errors: MutableList<BraintreeError> = ArrayList()
            for (i in 0 until json.length()) {
                try {
                    errors.add(fromJson(json.getJSONObject(i)))
                } catch (ignored: JSONException) {
                }
            }
            return errors
        }

        fun fromGraphQLJsonArray(graphQLErrors: JSONArray?): List<BraintreeError> {
            val errors: MutableList<BraintreeError> = ArrayList()
            if (graphQLErrors == null) {
                return errors
            }
            for (i in 0 until graphQLErrors.length()) {
                try {
                    val graphQLError = graphQLErrors.getJSONObject(i)
                    val extensions = graphQLError.optJSONObject(GraphQLConstants.Keys.EXTENSIONS)
                    if (extensions == null || ErrorTypes.USER != extensions.optString(
                            GraphQLConstants.Keys.ERROR_TYPE
                        )
                    ) {
                        continue
                    }
                    val inputPath = ArrayList<String>()
                    val inputPathJSON = extensions.getJSONArray(GraphQLConstants.Keys.INPUT_PATH)
                    for (j in 1 until inputPathJSON.length()) {
                        inputPath.add(inputPathJSON.getString(j))
                    }
                    addGraphQLFieldError(inputPath, graphQLError, errors)
                } catch (ignored: JSONException) {
                }
            }
            return errors
        }

        fun fromJson(json: JSONObject): BraintreeError {
            val error = BraintreeError()
            error.field = Json.optString(json, FIELD_KEY, null)
            error.message = Json.optString(json, MESSAGE_KEY, null)
            error.code = json.optInt(CODE_KEY, UNKNOWN_CODE)
            error.fieldErrors = fromJsonArray(json.optJSONArray(FIELD_ERRORS_KEY))
            return error
        }

        @Throws(JSONException::class)
        private fun addGraphQLFieldError(
            inputPath: List<String>,
            errorJSON: JSONObject,
            errors: MutableList<BraintreeError>?
        ) {
            val field = inputPath[0]
            if (inputPath.size == 1) {
                val error = BraintreeError()
                error.field = field
                error.message = errorJSON.getString(GraphQLConstants.Keys.MESSAGE)
                val extensions = errorJSON.optJSONObject(GraphQLConstants.Keys.EXTENSIONS)
                if (extensions != null) {
                    error.code = extensions.optInt(GraphQLConstants.Keys.LEGACY_CODE, UNKNOWN_CODE)
                }
                error.fieldErrors = ArrayList()
                errors!!.add(error)
                return
            }
            var nestedError: BraintreeError? = null
            val nestedInputPath = inputPath.subList(1, inputPath.size)
            for (error in errors!!) {
                if (error.field == field) {
                    nestedError = error
                }
            }
            if (nestedError == null) {
                nestedError = BraintreeError()
                nestedError.field = field
                nestedError.fieldErrors = ArrayList()
                errors.add(nestedError)
            }
            addGraphQLFieldError(nestedInputPath, errorJSON, nestedError.fieldErrors)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<BraintreeError> =
            object : Parcelable.Creator<BraintreeError> {
                override fun createFromParcel(source: Parcel) = BraintreeError(source)
                override fun newArray(size: Int) = arrayOfNulls<BraintreeError>(size)
            }
    }
}