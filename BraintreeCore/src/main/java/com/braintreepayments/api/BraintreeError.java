package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.braintreepayments.api.GraphQLConstants.ErrorTypes;
import com.braintreepayments.api.GraphQLConstants.Keys;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BraintreeError implements Parcelable {

    private static final String FIELD_KEY = "field";
    private static final String MESSAGE_KEY = "message";
    private static final String FIELD_ERRORS_KEY = "fieldErrors";
    private static final String CODE_KEY = "code";

    private static final int UNKNOWN_CODE = -1;

    private String field;
    private String message;
    private List<BraintreeError> fieldErrors;

    private int code;

    static List<BraintreeError> fromJsonArray(JSONArray json) {
        if (json == null) {
            json = new JSONArray();
        }

        List<BraintreeError> errors = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            try {
                errors.add(BraintreeError.fromJson(json.getJSONObject(i)));
            } catch (JSONException ignored) {}
        }

        return errors;
    }

    protected static List<BraintreeError> fromGraphQLJsonArray(JSONArray graphQLErrors) {
        List<BraintreeError> errors = new ArrayList<>();

        if (graphQLErrors == null) {
            return errors;
        }

        for (int i = 0; i < graphQLErrors.length(); i++) {
            try {
                JSONObject graphQLError = graphQLErrors.getJSONObject(i);
                JSONObject extensions = graphQLError.optJSONObject(Keys.EXTENSIONS);

                if (extensions == null || !ErrorTypes.USER.equals(extensions.optString(Keys.ERROR_TYPE))) {
                    continue;
                }

                ArrayList<String> inputPath = new ArrayList<>();
                JSONArray inputPathJSON = extensions.getJSONArray(Keys.INPUT_PATH);

                for (int j = 1; j < inputPathJSON.length(); j++) {
                    inputPath.add(inputPathJSON.getString(j));
                }

                addGraphQLFieldError(inputPath, graphQLError, errors);
            } catch (JSONException ignored) {}
        }

        return errors;
    }

    static BraintreeError fromJson(JSONObject json) {
        BraintreeError error = new BraintreeError();
        error.field = Json.optString(json, FIELD_KEY, null);
        error.message = Json.optString(json, MESSAGE_KEY, null);
        error.code = json.optInt(CODE_KEY, UNKNOWN_CODE);
        error.fieldErrors = BraintreeError.fromJsonArray(json.optJSONArray(FIELD_ERRORS_KEY));

        return error;
    }

    private static void addGraphQLFieldError(List<String> inputPath, JSONObject errorJSON, List<BraintreeError> errors) throws JSONException {
        String field = inputPath.get(0);

        if (inputPath.size() == 1) {
            BraintreeError error = new BraintreeError();
            error.field = field;
            error.message = errorJSON.getString(Keys.MESSAGE);

            int code = UNKNOWN_CODE;
            JSONObject extensions = errorJSON.optJSONObject(Keys.EXTENSIONS);
            if (extensions != null) {
                code = extensions.optInt(Keys.LEGACY_CODE, UNKNOWN_CODE);
            }
            error.code = code;

            error.fieldErrors = new ArrayList<>();

            errors.add(error);
            return;
        }

        BraintreeError nestedError = null;
        List<String> nestedInputPath = inputPath.subList(1, inputPath.size());

        for (BraintreeError error : errors) {
            if (error.field.equals(field)) {
                nestedError = error;
            }
        }

        if (nestedError == null) {
            nestedError = new BraintreeError();
            nestedError.field = field;
            nestedError.fieldErrors = new ArrayList<>();

            errors.add(nestedError);
        }

        addGraphQLFieldError(nestedInputPath, errorJSON, nestedError.fieldErrors);
    }

    /**
     * @return Human readable summary of the error for field. May be {@code null}.
     */
    @Nullable
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
     * @return Error code if one exists; defaults to {@link #UNKNOWN_CODE} otherwise
     */
    public int getCode() {
        return code;
    }

    /**
     * @return {@link BraintreeError} objects for any errors nested under this field.
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

    public String toString() {
        return "BraintreeError for " + field + ": " + message + " -> " +
                (fieldErrors != null ? fieldErrors.toString() : "");
    }

    BraintreeError() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(field);
        dest.writeString(message);
        dest.writeTypedList(fieldErrors);
    }

    protected BraintreeError(Parcel in) {
        field = in.readString();
        message = in.readString();
        fieldErrors = in.createTypedArrayList(BraintreeError.CREATOR);
    }

    public static final Creator<BraintreeError> CREATOR = new Creator<BraintreeError>() {
        public BraintreeError createFromParcel(Parcel source) {
            return new BraintreeError(source);
        }

        public BraintreeError[] newArray(int size) {
            return new BraintreeError[size];
        }
    };
}

