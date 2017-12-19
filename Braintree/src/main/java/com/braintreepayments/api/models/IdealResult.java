package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link IdealResult} representing an iDEAL bank payment.
 */
public class IdealResult extends BraintreePaymentResult {

    private String mId;
    private String mShortId;
    private String mStatus;

    /**
     * Convert an API response to an {@link IdealResult}.
     *
     * @param json Raw JSON representation of a {@link IdealResult}.
     * @return {@link IdealResult} representing an iDEAL bank payment.
     */
    public static IdealResult fromJson(String json) throws JSONException {
        JSONObject jsonObj = new JSONObject(json);
        IdealResult result = new IdealResult();
        JSONObject data = jsonObj.getJSONObject("data");

        result.mId = data.getString("id");
        result.mShortId = data.getString("short_id");
        result.mStatus = data.getString("status");

        return result;
    }

    /**
     * @return A shortened form of the identifier for the iDEAL payment.
     */
    public String getShortId() {
        return mShortId;
    }

    /**
     * @return The status of the iDEAL payment. Possible values are [PENDING, COMPLETE, FAILED]
     */
    public String getStatus() {
        return mStatus;
    }

    /**
     * @return The identifier for the iDEAL payment.
     */
    public String getId() {
        return mId;
    }

    private IdealResult() {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mId);
        dest.writeString(mShortId);
        dest.writeString(mStatus);
    }

    protected IdealResult(Parcel in) {
        super(in);
        mId = in.readString();
        mShortId = in.readString();
        mStatus = in.readString();
    }

    public static final Creator<IdealResult> CREATOR = new Creator<IdealResult>() {
        @Override
        public IdealResult createFromParcel(Parcel in) {
            return new IdealResult(in);
        }

        @Override
        public IdealResult[] newArray(int size) {
            return new IdealResult[size];
        }
    };
}
