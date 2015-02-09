package com.braintreepayments.api.exceptions;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

import java.util.ArrayList;
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
    private final String message;
    private final List<BraintreeError> fieldErrors;

    public ErrorWithResponse(int statusCode, String jsonString) {
        this(statusCode, new Gson().fromJson(jsonString, BraintreeErrors.class));
    }

    public ErrorWithResponse(int statusCode, BraintreeErrors errors) {
        this.statusCode = statusCode;

        if (errors != null) {
            fieldErrors = errors.getFieldErrors();
            message = errors.getMessage();
        } else {
            fieldErrors = null;
            message = "Parsing error response failed";
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

    public final static class BraintreeErrors implements Parcelable {
        private BraintreeError error;
        private List<BraintreeError> fieldErrors = new ArrayList<BraintreeError>();

        public BraintreeErrors() {}

        protected String getMessage() {
            if (error != null) {
                return error.getMessage();
            } else {
                return "Parsing error response failed";
            }
        }

        protected List<BraintreeError> getFieldErrors() {
            return fieldErrors;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedList(fieldErrors);
            dest.writeParcelable(this.error, 0);
        }

        private BraintreeErrors(Parcel in) {
            in.readTypedList(fieldErrors, BraintreeError.CREATOR);
            this.error = in.readParcelable(BraintreeError.class.getClassLoader());
        }

        public static final Creator<BraintreeErrors> CREATOR = new Creator<BraintreeErrors>() {
            public BraintreeErrors createFromParcel(Parcel source) {
                return new BraintreeErrors(source);
            }

            public BraintreeErrors[] newArray(int size) {return new BraintreeErrors[size];}
        };
    }

    public final static class BraintreeError implements android.os.Parcelable {
        private String field;
        private String message;
        private List<BraintreeError> fieldErrors = new ArrayList<BraintreeError>();

        public BraintreeError() {}

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

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeList(this.fieldErrors);
            dest.writeString(this.field);
            dest.writeString(this.message);
        }

        private BraintreeError(Parcel in) {
            this.fieldErrors = new ArrayList<BraintreeError>();
            in.readList(this.fieldErrors, ArrayList.class.getClassLoader());
            this.field = in.readString();
            this.message = in.readString();
        }

        public static final Creator<BraintreeError> CREATOR = new Creator<BraintreeError>() {
            public BraintreeError createFromParcel(Parcel source) {
                return new BraintreeError(source);
            }

            public BraintreeError[] newArray(int size) {return new BraintreeError[size];}
        };
    }

}
