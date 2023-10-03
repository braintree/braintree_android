package com.braintreepayments.api

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.braintreepayments.api.GraphQLConstants.ErrorTypes
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * @property field Field name this object represents.
 * @property message Human readable summary of the error for field. May be `null`.
 * @property code Error code if one exists; defaults to [.UNKNOWN_CODE] otherwise
 * @property fieldErrors [BraintreeError] objects for any errors nested under this field.
 */
open class BraintreeError : Parcelable {

    open var field: String? = null
        internal set

    open var message: String? = null
        internal set

    open var fieldErrors: MutableList<BraintreeError>? = null
        internal set

    // default value
    open var code = UNKNOWN_CODE
        internal set

    /**
     * Method to extract an error for an individual field, e.g. creditCard, customer, etc.
     *
     * @param field Name of the field desired, expected to be in camelCase.
     * @return [BraintreeError] for the field searched, or `null` if not found.
     */
    open fun errorFor(field: String): BraintreeError? {
        if (fieldErrors == null) return null
        for (error in fieldErrors!!) {
            if (error.field == field) {
                return error
            } else if (error.fieldErrors != null) {
                error.errorFor(field)?.let { return it }
            }
        }
        return null
    }

    override fun toString(): String {
        return "BraintreeError for $field: $message -> ${fieldErrors?.toString() ?: ""}"
    }

    internal constructor()

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(field)
        dest.writeString(message)
        dest.writeTypedList(fieldErrors)
    }

    protected constructor(inParcel: Parcel) {
        field = inParcel.readString()
        message = inParcel.readString()
        fieldErrors = inParcel.createTypedArrayList(CREATOR)
    }

    companion object {
        private const val FIELD_KEY = "field"
        private const val MESSAGE_KEY = "message"
        private const val FIELD_ERRORS_KEY = "fieldErrors"
        private const val CODE_KEY = "code"
        private const val UNKNOWN_CODE = -1

        internal fun fromJsonArray(input: JSONArray?): MutableList<BraintreeError> {
            val json = input ?: JSONArray()
            val errors = mutableListOf<BraintreeError>()
            for (i in 0 until json.length()) {
                try {
                    errors.add(fromJson(json.getJSONObject(i)))
                } catch (ignored: JSONException) {
                }
            }
            return errors
        }

        /**
         * @suppress
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun fromGraphQLJsonArray(graphQLErrors: JSONArray?): List<BraintreeError> {
            val errors = mutableListOf<BraintreeError>()
            if (graphQLErrors == null) {
                return errors
            }
            for (i in 0 until graphQLErrors.length()) {
                try {
                    val graphQLError = graphQLErrors.getJSONObject(i)
                    val extensions = graphQLError.optJSONObject(GraphQLConstants.Keys.EXTENSIONS)

                    val errorType = extensions?.optString(GraphQLConstants.Keys.ERROR_TYPE)
                    if (errorType != ErrorTypes.USER) {
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

        internal fun fromJson(json: JSONObject) = BraintreeError().apply {
            field = Json.optString(json, FIELD_KEY, null)
            message = Json.optString(json, MESSAGE_KEY, null)
            code = json.optInt(CODE_KEY, UNKNOWN_CODE)
            fieldErrors = fromJsonArray(json.optJSONArray(FIELD_ERRORS_KEY))
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

                errors?.add(error)
                return
            }

            var nestedError: BraintreeError? = null
            val nestedInputPath = inputPath.subList(1, inputPath.size)

            if (errors != null) {
                for (error in errors) {
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
            }

            addGraphQLFieldError(nestedInputPath, errorJSON, nestedError?.fieldErrors)
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<BraintreeError> {
            override fun createFromParcel(source: Parcel) = BraintreeError(source)
            override fun newArray(size: Int) = arrayOfNulls<BraintreeError>(size)
        }
    }
}
