package com.braintreepayments.api

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GraphQLConstants {

    object Keys {
        const val QUERY = "query"
        const val INPUT = "input"
        const val VARIABLES = "variables"
        const val ERRORS = "errors"
        const val MESSAGE = "message"
        const val ERROR_TYPE = "errorType"
        const val EXTENSIONS = "extensions"
        const val INPUT_PATH = "inputPath"
        const val LEGACY_CODE = "legacyCode"
        const val URL = "url"
        const val FEATURES = "features"
        const val OPERATION_NAME = "operationName"
    }

    object ErrorTypes {
        const val USER = "user_error"
    }

    object ErrorMessages {
        const val UNKNOWN = "An unknown error occurred."
        const val USER = "Input is invalid."
    }

    object LegacyErrorCodes {
        const val VALIDATION_NOT_ALLOWED = "50000"
    }

    object Headers {
        const val API_VERSION = "2018-03-06"
    }

    object Features {
        const val TOKENIZE_CREDIT_CARDS = "tokenize_credit_cards"
    }
}
