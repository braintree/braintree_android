package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.braintreepayments.api.GraphQLConstants.ErrorMessages;
import com.braintreepayments.api.GraphQLConstants.Keys;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 *  Error container returned when the Braintree server returns a 422 Unprocessible Entity.
 *  A 422 occurs when a request is properly formed, but the server was unable to take the requested
 *  action due to bad user data.
 *
 *  ErrorWithResponse parses the server's error response and exposes the errors.
 */
public class ErrorWithResponse extends Exception implements Parcelable {
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String FIELD_ERRORS_KEY = "fieldErrors";

    private int statusCode;
    private String message;
    private String originalResponse;
    private List<BraintreeError> fieldErrors;

    ErrorWithResponse(int statusCode, String jsonString) {
        this.statusCode = statusCode;
        originalResponse = jsonString;

        try {
            parseJson(jsonString);
        } catch (JSONException e) {
            message = "Parsing error response failed";
            fieldErrors = new ArrayList<>();
        }
    }

    private ErrorWithResponse() {}

    static ErrorWithResponse fromJson(String json) throws JSONException {
        ErrorWithResponse errorWithResponse = new ErrorWithResponse();
        errorWithResponse.originalResponse = json;
        errorWithResponse.parseJson(json);

        return errorWithResponse;
    }

    static ErrorWithResponse fromGraphQLJson(String json) {
        ErrorWithResponse errorWithResponse = new ErrorWithResponse();
        errorWithResponse.originalResponse = json;
        errorWithResponse.statusCode = 422;

        try {
            JSONArray errors = new JSONObject(json).getJSONArray(Keys.ERRORS);

            errorWithResponse.fieldErrors = BraintreeError.fromGraphQLJsonArray(errors);

            if (errorWithResponse.fieldErrors.isEmpty()) {
                errorWithResponse.message = errors.getJSONObject(0).getString(Keys.MESSAGE);
            } else {
                errorWithResponse.message = ErrorMessages.USER;
            }
        } catch (JSONException e) {
            errorWithResponse.message = "Parsing error response failed";
            errorWithResponse.fieldErrors = new ArrayList<>();
        }

        return errorWithResponse;
    }

    private void parseJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        message = json.getJSONObject(ERROR_KEY).getString(MESSAGE_KEY);
        fieldErrors = BraintreeError.fromJsonArray(json.optJSONArray(FIELD_ERRORS_KEY));
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
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * @return The full error response as a {@link String}.
     */
    public String getErrorResponse() {
        return originalResponse;
    }

    /**
     * @return All the field errors.
     */
    public List<BraintreeError> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Method to extract an error for an individual field, e.g. creditCard, customer, etc.
     *
     * @param field Name of the field desired, expected to be in camelCase.
     * @return {@link BraintreeError} for the field searched, or {@code null} if not found.
     */
    @Nullable
    public BraintreeError errorFor(String field) {
        BraintreeError returnError;
        if (fieldErrors != null) {
            for (BraintreeError error : fieldErrors) {
                if (error.getField().equals(field)) {
                    return error;
                } else if (error.getFieldErrors() != null) {
                    returnError = error.errorFor(field);
                    if (returnError != null) {
                        return returnError;
                    }
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(statusCode);
        dest.writeString(message);
        dest.writeString(originalResponse);
        dest.writeTypedList(fieldErrors);
    }

    protected ErrorWithResponse(Parcel in) {
        statusCode = in.readInt();
        message = in.readString();
        originalResponse = in.readString();
        fieldErrors = in.createTypedArrayList(BraintreeError.CREATOR);
    }

    public static final Creator<ErrorWithResponse> CREATOR = new Creator<ErrorWithResponse>() {
        public ErrorWithResponse createFromParcel(Parcel source) {
            return new ErrorWithResponse(source);
        }

        public ErrorWithResponse[] newArray(int size) {
            return new ErrorWithResponse[size];
        }
    };
}
