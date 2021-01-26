package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 *  Specific error returned when Braintree API returns a 400 Bad Request.
 *  A 400 occurs when a request is malformed or invalid (e.g. unexpected fields, invalid JSON, invalid version,
 *  invalid field value). Examine the errors details and either present the error(s) to the end user or review the
 *  request and configuration.
 *
 *  {@link BraintreeApiError} parses the server's specific errors exposes them.
 */
public class BraintreeApiError implements Parcelable {

    private String mCode;
    private String mMessage;
    private String mIn;
    private String mAt;

    public static List<BraintreeApiError> fromJsonArray(JSONArray json) {
        if (json == null) {
            json = new JSONArray();
        }

        List<BraintreeApiError> errors = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            try {
                errors.add(BraintreeApiError.fromJson(json.getJSONObject(i)));
            } catch (JSONException ignored) {}
        }

        return errors;
    }

    public static BraintreeApiError fromJson(JSONObject json) {
        BraintreeApiError error = new BraintreeApiError();
        error.mCode = Json.optString(json, "code", null);
        error.mMessage = Json.optString(json, "developer_message", null);
        error.mIn = Json.optString(json, "in", null);
        error.mAt = Json.optString(json, "at", null);

        return error;
    }

    /**
     * @return The error code.
     */
    @Nullable
    public String getCode() {
        return mCode;
    }

    /**
     * @return Human readable summary of the error.
     */
    @Nullable
    public String getMessage() {
        return mMessage;
    }

    /**
     * @return Where the error was.
     */
    @Nullable
    public String getIn() {
        return mIn;
    }

    /**
     * @return Field the error was in.
     */
    @Nullable
    public String getAt() {
        return mAt;
    }

    public String toString() {
        return "BraintreeApiError " + mCode + " for " + mIn + ": " + mMessage;
    }

    public BraintreeApiError() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCode);
        dest.writeString(mMessage);
        dest.writeString(mIn);
        dest.writeString(mAt);
    }

    protected BraintreeApiError(Parcel in) {
        mCode = in.readString();
        mMessage = in.readString();
        mIn = in.readString();
        mAt = in.readString();
    }

    public static final Creator<BraintreeApiError> CREATOR = new Creator<BraintreeApiError>() {
        @Override
        public BraintreeApiError createFromParcel(Parcel source) {
            return new BraintreeApiError(source);
        }

        @Override
        public BraintreeApiError[] newArray(int size) {
            return new BraintreeApiError[size];
        }
    };
}
