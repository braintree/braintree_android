package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 *  Error container returned when Braintree API returns a 400 Bad Request.
 *  A 400 occurs when a request is malformed or invalid (e.g. unexpected fields, invalid JSON, invalid version,
 *  invalid field value). Examine the errors details and either present the error(s) to the end user or review the
 *  request and configuration.
 *
 *  {@link BraintreeApiErrorResponse} parses the server's error response and exposes the errors.
 */
public class BraintreeApiErrorResponse extends Exception implements Parcelable {

    private String mMessage;
    private String mOriginalResponse;
    private List<BraintreeApiError> mErrors;

    public BraintreeApiErrorResponse(String jsonString) {
        mOriginalResponse = jsonString;

        try {
            JSONObject json = new JSONObject(jsonString);
            JSONObject error = json.getJSONObject("error");

            mMessage = Json.optString(error, "developer_message", "No message was returned");
            mErrors = BraintreeApiError.fromJsonArray(error.optJSONArray("details"));
        } catch (JSONException e) {
            mMessage = "Parsing error response failed";
        }
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
     * @return All the specific field errors as {@link BraintreeApiError}s.
     */
    @Nullable
    public List<BraintreeApiError> getErrors() {
        return mErrors;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMessage);
        dest.writeString(mOriginalResponse);
        dest.writeTypedList(mErrors);
    }

    protected BraintreeApiErrorResponse(Parcel in) {
        mMessage = in.readString();
        mOriginalResponse = in.readString();
        mErrors = in.createTypedArrayList(BraintreeApiError.CREATOR);
    }

    public static final Creator<BraintreeApiErrorResponse> CREATOR = new Creator<BraintreeApiErrorResponse>() {
        @Override
        public BraintreeApiErrorResponse createFromParcel(Parcel source) {
            return new BraintreeApiErrorResponse(source);
        }

        @Override
        public BraintreeApiErrorResponse[] newArray(int size) {
            return new BraintreeApiErrorResponse[size];
        }
    };
}
