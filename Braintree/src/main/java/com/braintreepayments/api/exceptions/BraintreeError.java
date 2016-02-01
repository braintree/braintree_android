package com.braintreepayments.api.exceptions;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

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

    public static List<BraintreeError> fromJsonArray(JSONArray json) {
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

    public static BraintreeError fromJson(JSONObject json) {
        BraintreeError error = new BraintreeError();
        error.mField = json.optString(FIELD_KEY, null);
        error.mMessage = json.optString(MESSAGE_KEY, null);
        error.mFieldErrors = BraintreeError.fromJsonArray(json.optJSONArray(FIELD_ERRORS_KEY));

        return error;
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

    public String toString() {
        return "BraintreeError for " + mField + ": " + mMessage + " -> " +
                (mFieldErrors != null ? mFieldErrors.toString() : "");
    }

    public BraintreeError() {}

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

