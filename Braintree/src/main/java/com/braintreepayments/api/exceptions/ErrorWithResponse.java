package com.braintreepayments.api.exceptions;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.braintreepayments.api.Json;

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

    public static final String GRAPHQL_ERRORS_KEY = "errors";

    private static final String GRAPHQL_EXTENSIONS_KEY = "extensions";
    private static final String GRAPHQL_ERROR_TYPE_KEY = "errorType";
    private static final String GRAPHQL_ERROR_DETAILS_KEY = "errorDetails";
    private static final String GRAPHQL_USER_ERROR = "user_error";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String FIELD_ERRORS_KEY = "fieldErrors";

    private int mStatusCode;
    private String mMessage;
    private String mOriginalResponse;
    private List<BraintreeError> mFieldErrors;

    public ErrorWithResponse(int statusCode, String jsonString) {
        mStatusCode = statusCode;
        mOriginalResponse = jsonString;

        try {
            parseJson(jsonString);
        } catch (JSONException e) {
            mMessage = "Parsing error response failed";
            mFieldErrors = new ArrayList<>();
        }
    }

    private ErrorWithResponse() {}

    public static ErrorWithResponse fromJson(String json) throws JSONException {
        ErrorWithResponse errorWithResponse = new ErrorWithResponse();
        errorWithResponse.mOriginalResponse = json;
        errorWithResponse.parseJson(json);

        return errorWithResponse;
    }

    public static ErrorWithResponse fromGraphQLJson(String json) {
        ErrorWithResponse errorWithResponse = new ErrorWithResponse();
        errorWithResponse.mOriginalResponse = json;
        errorWithResponse.mStatusCode = 422;

        try {
            JSONArray errors = new JSONObject(json).getJSONArray(GRAPHQL_ERRORS_KEY);
            JSONObject error = null;
            for (int i = 0; i < errors.length(); i++) {
                JSONObject errorExtension = errors.getJSONObject(i).optJSONObject(GRAPHQL_EXTENSIONS_KEY);
                if (errorExtension != null && Json.optString(errorExtension, GRAPHQL_ERROR_TYPE_KEY, "").equals(GRAPHQL_USER_ERROR)) {
                    error = errors.getJSONObject(i);
                }
            }

            if (error == null) {
                error = errors.getJSONObject(0);
            }

            errorWithResponse.mMessage = error.getString(MESSAGE_KEY);
            errorWithResponse.mFieldErrors = BraintreeError.fromGraphQLJsonArray(
                    error.getJSONObject(GRAPHQL_EXTENSIONS_KEY).optJSONArray(GRAPHQL_ERROR_DETAILS_KEY));
        } catch (JSONException e) {
            errorWithResponse.mMessage = "Parsing error response failed";
            errorWithResponse.mFieldErrors = new ArrayList<>();
        }

        return errorWithResponse;
    }

    private void parseJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        mMessage = json.getJSONObject(ERROR_KEY).getString(MESSAGE_KEY);
        mFieldErrors = BraintreeError.fromJsonArray(json.optJSONArray(FIELD_ERRORS_KEY));
    }

    /**
     * @return HTTP status code from the Braintree gateway.
     */
    public int getStatusCode() {
        return mStatusCode;
    }

    /**
     * @return Human readable top level summary of the error.
     */
    @Override
    public String getMessage() {
        return mMessage;
    }

    /**
     * @return The full error response as a {@link String}.
     */
    public String getErrorResponse() {
        return mOriginalResponse;
    }

    /**
     * @return All the field errors.
     */
    public List<BraintreeError> getFieldErrors() {
        return mFieldErrors;
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
        if(mFieldErrors != null) {
            for (BraintreeError error : mFieldErrors) {
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
        return "ErrorWithResponse (" + mStatusCode + "): " + mMessage + "\n" +
                mFieldErrors.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mStatusCode);
        dest.writeString(mMessage);
        dest.writeString(mOriginalResponse);
        dest.writeTypedList(mFieldErrors);
    }

    protected ErrorWithResponse(Parcel in) {
        mStatusCode = in.readInt();
        mMessage = in.readString();
        mOriginalResponse = in.readString();
        mFieldErrors = in.createTypedArrayList(BraintreeError.CREATOR);
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
