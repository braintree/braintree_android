package com.braintreepayments.api.exceptions;

import com.braintreepayments.api.Utils;

import java.util.List;

/**
 *  Immutable error container returned when the Braintree server receives a 422 Unprocessible Entity.
 *  A 422 occurs when a request is properly formed, but the server was unable to take the requested
 *  action due to bad user data.
 *
 *  ErrorWithResponse parses the server's error response for use in guiding users toward resolving
 *  the problem(s) with their submission.
 */
public final class ErrorWithResponse extends Exception {

    private final int statusCode;
    private final List<BraintreeError> fieldErrors;
    private final String message;

    public ErrorWithResponse(int statusCode, String jsonString) throws UnexpectedException {
        this.statusCode = statusCode;

        BraintreeErrors errors = Utils.getGson().fromJson(jsonString, BraintreeErrors.class);

        try {
            this.fieldErrors = errors.getFieldErrors();
            this.message = errors.getMessage();
        } catch (NullPointerException e) {
            throw new UnexpectedException("Parsing error response failed");
        }
    }

    /**
     * @return HTTP status code from the Braintree gateway.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return Human readable top level summary of the error.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return Recursive {@link BraintreeError} structure of errors for individual data fields.
     */
    public List<BraintreeError> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Method to extract an error for an individual top-level field, e.g. credit card, customer, etc.
     * This method is NOT recursive.
     * @param field Name of the field desired, expected to be in camelCase.
     * @return {@link BraintreeError} for the field searched, or {@code null} if not found.
    */
    public BraintreeError errorFor(String field) {
        if(fieldErrors != null) {
            for (BraintreeError error : fieldErrors) {
                if (error.field.equals(field)) {
                    return error;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ErrorWithResponse (" + statusCode + "): " + message + "\n" +
                fieldErrors.toString();
    }

    private final class BraintreeErrors {
        private List<BraintreeError> fieldErrors;
        private BraintreeError error;

        protected String getMessage() {
            return error.getMessage();
        }

        protected List<BraintreeError> getFieldErrors() {
            return fieldErrors;
        }
    }

    public final class BraintreeError {
        private List<BraintreeError> fieldErrors;
        private String field;
        private String message;

        /**
         * @return Human readable summary of the error for field. May be {@code null}.
         */
        public String getMessage() {
            return message;
        }

        /**
         * @return Field name this object represents.
         */
        public String getField() {
            return field;
        }

        /**
         * @return {@link com.braintreepayments.api.exceptions.ErrorWithResponse.BraintreeError} objects
         *         for any errors nested under this field.
         */
        public List<BraintreeError> getFieldErrors() {
            return fieldErrors;
        }

        /**
         * Searches for child errors matching a field name.
         * This function is NOT recursive.
         * @param field Name of the field desired, expected to be in camelCase.
         * @return {@link BraintreeError} for the field searched, or {@code null} if not found.
         */
        public BraintreeError errorFor(String field) {
            if(fieldErrors != null) {
                for (BraintreeError error : fieldErrors) {
                    if (error.field.equals(field)) {
                        return error;
                    }
                }
            }
            return null;
        }

        public String toString() {
            return "BraintreeError for " + field + ": " + message + " -> " + (fieldErrors != null ? fieldErrors.toString() : "");
        }
    }
}
