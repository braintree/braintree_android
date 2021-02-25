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

    private String mField;
    private String mMessage;
    private List<BraintreeError> mFieldErrors;

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
        error.mField = Json.optString(json, FIELD_KEY, null);
        error.mMessage = Json.optString(json, MESSAGE_KEY, null);
        error.mFieldErrors = BraintreeError.fromJsonArray(json.optJSONArray(FIELD_ERRORS_KEY));

        return error;
    }

    private static void addGraphQLFieldError(List<String> inputPath, JSONObject errorJSON, List<BraintreeError> errors) throws JSONException {
        String field = inputPath.get(0);

        if (inputPath.size() == 1) {
            BraintreeError error = new BraintreeError();
            error.mField = field;
            error.mMessage = errorJSON.getString(Keys.MESSAGE);
            error.mFieldErrors = new ArrayList<>();

            errors.add(error);
            return;
        }

        BraintreeError nestedError = null;
        List<String> nestedInputPath = inputPath.subList(1, inputPath.size());

        for (BraintreeError error : errors) {
            if (error.mField.equals(field)) {
                nestedError = error;
            }
        }

        if (nestedError == null) {
            nestedError = new BraintreeError();
            nestedError.mField = field;
            nestedError.mFieldErrors = new ArrayList<>();

            errors.add(nestedError);
        }

        addGraphQLFieldError(nestedInputPath, errorJSON, nestedError.mFieldErrors);
    }

    /**
     * @return Human readable summary of the error for field. May be {@code null}.
     */
    @Nullable
    public String getMessage() {
        return mMessage;
    }

    /**
     * @return Field name this object represents.
     */
    public String getField() {
        return mField;
    }

    /**
     * @return {@link BraintreeError} objects for any errors nested under this field.
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
        if (mFieldErrors != null) {
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

    public String toString() {
        return "BraintreeError for " + mField + ": " + mMessage + " -> " +
                (mFieldErrors != null ? mFieldErrors.toString() : "");
    }

    BraintreeError() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mField);
        dest.writeString(mMessage);
        dest.writeTypedList(mFieldErrors);
    }

    protected BraintreeError(Parcel in) {
        mField = in.readString();
        mMessage = in.readString();
        mFieldErrors = in.createTypedArrayList(BraintreeError.CREATOR);
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

