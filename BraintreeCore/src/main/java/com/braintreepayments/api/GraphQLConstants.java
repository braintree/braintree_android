package com.braintreepayments.api;

final class GraphQLConstants {

    final class Keys {
        static final String QUERY = "query";
        static final String INPUT = "input";
        static final String VARIABLES = "variables";
        static final String ERRORS = "errors";
        static final String MESSAGE = "message";
        static final String ERROR_TYPE = "errorType";
        static final String EXTENSIONS = "extensions";
        static final String INPUT_PATH = "inputPath";
        static final String LEGACY_CODE = "legacyCode";
        static final String URL = "url";
        static final String FEATURES = "features";
        static final String OPERATION_NAME = "operationName";
    }

    final class ErrorTypes {
        static final String USER = "user_error";
    }

    final class ErrorMessages {
        static final String UNKNOWN = "An unknown error occurred.";
        static final String USER = "Input is invalid.";
    }

    final class LegacyErrorCodes {
        static final String VALIDATION_NOT_ALLOWED = "50000";
    }

    final class Headers {
        static final String API_VERSION = "2018-03-06";
    }

    final class Features {
        static final String TOKENIZE_CREDIT_CARDS = "tokenize_credit_cards";
    }
}
