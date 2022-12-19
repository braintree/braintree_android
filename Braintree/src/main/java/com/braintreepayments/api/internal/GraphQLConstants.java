package com.braintreepayments.api.internal;

public final class GraphQLConstants {
    public final class Keys {
        public static final String QUERY = "query";
        public static final String INPUT = "input";
        public static final String VARIABLES = "variables";
        public static final String ERRORS = "errors";
        public static final String MESSAGE = "message";
        public static final String ERROR_TYPE = "errorType";
        public static final String EXTENSIONS = "extensions";
        public static final String INPUT_PATH = "inputPath";
        public static final String LEGACY_CODE = "legacyCode";
        public static final String URL = "url";
        public static final String FEATURES = "features";
        public static final String OPERATION_NAME = "operationName";
    }

    public final class ErrorTypes {
        public static final String USER = "user_error";
    }

    public final class ErrorMessages {
        public static final String UNKNOWN = "An unknown error occurred.";
        public static final String USER = "Input is invalid.";
    }

    public final class LegacyErrorCodes {
        public static final String VALIDATION_NOT_ALLOWED = "50000";
    }

    public final class Headers {
        public static final String API_VERSION = "2018-03-06";
    }

    public final class Features {
        public static final String TOKENIZE_CREDIT_CARDS = "tokenize_credit_cards";
    }
}
