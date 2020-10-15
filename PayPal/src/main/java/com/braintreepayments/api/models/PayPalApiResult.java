package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.braintreepayments.api.enums.PayPalApiResponseType;
import com.braintreepayments.api.enums.PayPalApiResultType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides all the information associated with a login request for an authorization code.<br/>
 */
public final class PayPalApiResult implements Parcelable {

    private static final String TAG = PayPalApiResult.class.getSimpleName();

    private final String mEnvironment;
    private final PayPalApiResultType mResultType;
    private final PayPalApiResponseType mResponseType;
    private final JSONObject mResponse;
    private final String mUserEmail;
    private final Throwable mError;

    /**
     * Construct a PayPalOneTouchResult for a success
     */
    public PayPalApiResult(String environment, PayPalApiResponseType responseType, JSONObject response,
                           String userEmail) {
        this(PayPalApiResultType.Success, environment, responseType, response, userEmail, null);
    }

    /**
     * Construct a PayPalOneTouchResult for a failure
     *
     * @param error The error to return to the caller
     * @note the error may come from the remote authenticator or directly from lib-otc (e.g. due to
     * response parsing errors)
     */
    public PayPalApiResult(Throwable error) {
        this(PayPalApiResultType.Error, null, null, null, null, error);
    }

    /**
     * Construct a PayPalOneTouchResult for a cancellation
     */
    public PayPalApiResult() {
        this(PayPalApiResultType.Cancel, null, null, null, null, null);
    }

    /**
     * Construct a PayPalOneTouchResult for any generic data.  For internal use only.
     */
    private PayPalApiResult(PayPalApiResultType resultType, String environment, PayPalApiResponseType responseType,
                            JSONObject response, String userEmail, Throwable error) {
        mEnvironment = environment;
        mResultType = resultType;
        mResponseType = responseType;
        mResponse = response;
        mUserEmail = userEmail;
        mError = error;
    }

    public PayPalApiResultType getResultType() {
        return mResultType;
    }

    /**
     * @return The JSON object to send to your server.
     */
    public JSONObject getResponse() {
        try {
            JSONObject client = new JSONObject();
            client.put("environment", mEnvironment);

            JSONObject response = new JSONObject();
            response.put("client", client);

            if (null != mResponse) {
                JSONObject result = mResponse;
                response.put("response", result);
            }

            if (null != mResponseType) {
                response.put("response_type", mResponseType.name());
            }

            if (null != mUserEmail) {
                JSONObject user = new JSONObject();
                user.put("display_string", mUserEmail);
                response.put("user", user);
            }

            return response;
        } catch (JSONException e) {
            Log.e(TAG, "Error encoding JSON", e);
            return null;
        }
    }

    /**
     * @return the error if the response type is error
     */
    public Throwable getError() {
        return mError;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mEnvironment);
        dest.writeSerializable(mResultType);
        dest.writeSerializable(mResponseType);
        dest.writeString(mResponse == null ? null : mResponse.toString());
        dest.writeString(mUserEmail);
        dest.writeSerializable(mError);
    }

    private PayPalApiResult(Parcel in) {
        mEnvironment = in.readString();
        mResultType = (PayPalApiResultType) in.readSerializable();
        mResponseType = (PayPalApiResponseType) in.readSerializable();

        JSONObject jsonResponse = null;
        try {
            String jsonString = in.readString();
            if (null != jsonString) {
                jsonResponse = new JSONObject(jsonString);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to read parceled JSON for mResponse", e);
        }
        mResponse = jsonResponse;

        mUserEmail = in.readString();
        mError = (Throwable) in.readSerializable();
    }

    public static final Parcelable.Creator<PayPalApiResult> CREATOR = new Parcelable.Creator<PayPalApiResult>() {
        public PayPalApiResult createFromParcel(Parcel source) {
            return new PayPalApiResult(source);
        }

        public PayPalApiResult[] newArray(int size) {
            return new PayPalApiResult[size];
        }
    };
}
