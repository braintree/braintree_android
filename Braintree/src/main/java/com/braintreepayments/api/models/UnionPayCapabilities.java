package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.braintreepayments.api.BraintreeFragment;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that has details about the capabilities of a certain UnionPay card.
 */
public class UnionPayCapabilities implements Parcelable {

    private static final String IS_UNIONPAY_KEY = "isUnionPay";
    private static final String IS_DEBIT_KEY = "isDebit";
    private static final String UNIONPAY_KEY = "unionPay";
    private static final String SUPPORTS_TWO_STEP_AUTH_AND_CAPTURE_KEY = "supportsTwoStepAuthAndCapture";
    private static final String IS_UNIONPAY_ENROLLMENT_REQUIRED_KEY = "isUnionPayEnrollmentRequired";

    private final boolean mIsUnionPay;
    private final boolean mIsDebit;
    private final boolean mSupportsTwoStepAuthAndCapture;
    private final boolean mIsUnionPayEnrollmentRequired;

    public static UnionPayCapabilities fromJson(@NonNull String jsonString) {
        boolean isUnionPay = false;
        boolean isDebit = false;
        boolean supportsTwoStepAuthAndCapture = false;
        boolean isUnionPayEnrollmentRequired = false;
        try {
            JSONObject json = new JSONObject(jsonString);
            isUnionPay = json.optBoolean(IS_UNIONPAY_KEY);
            isDebit = json.optBoolean(IS_DEBIT_KEY);

            if (json.has(UNIONPAY_KEY)) {
                JSONObject unionPay = json.getJSONObject(UNIONPAY_KEY);
                supportsTwoStepAuthAndCapture = unionPay.optBoolean(SUPPORTS_TWO_STEP_AUTH_AND_CAPTURE_KEY);
                isUnionPayEnrollmentRequired = unionPay.optBoolean(IS_UNIONPAY_ENROLLMENT_REQUIRED_KEY);
            }
        } catch (JSONException ignored) {}

        return new UnionPayCapabilities(isUnionPay, isDebit, supportsTwoStepAuthAndCapture,
                isUnionPayEnrollmentRequired);
    }

    UnionPayCapabilities(boolean isUnionPay, boolean isDebit, boolean supportsTwoStepAuthAndCapture,
            boolean isUnionPayEnrollmentRequired) {
        mIsUnionPay = isUnionPay;
        mIsDebit = isDebit;
        mSupportsTwoStepAuthAndCapture = supportsTwoStepAuthAndCapture;
        mIsUnionPayEnrollmentRequired = isUnionPayEnrollmentRequired;
    }

    /**
     * @return true if this card is a UnionPay card.
     */
    public boolean isUnionPay() {
        return mIsUnionPay;
    }

    /**
     * @return true if this card ia a debit card.
     */
    public boolean isDebit() {
        return mIsDebit;
    }

    /**
     * @return true if this card allows for an authorization, but settling the transaction later.
     */
    public boolean supportsTwoStepAuthAndCapture() {
        return mSupportsTwoStepAuthAndCapture;
    }

    /**
     * @return true if a {@link com.braintreepayments.api.UnionPay#enroll(BraintreeFragment, UnionPayCardBuilder)}
     * should be completed.
     */
    public boolean isUnionPayEnrollmentRequired() {
        return mIsUnionPayEnrollmentRequired;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public UnionPayCapabilities(Parcel in) {
        mIsUnionPay = in.readByte() > 0;
        mIsDebit = in.readByte() > 0;
        mSupportsTwoStepAuthAndCapture = in.readByte() > 0;
        mIsUnionPayEnrollmentRequired = in.readByte() > 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(mIsUnionPay ? (byte) 1 : 0);
        dest.writeByte(mIsDebit ? (byte) 1 : 0);
        dest.writeByte(mSupportsTwoStepAuthAndCapture ? (byte) 1 : 0);
        dest.writeByte(mIsUnionPayEnrollmentRequired ? (byte) 1 : 0);
    }

    public static final Creator<UnionPayCapabilities> CREATOR = new Creator<UnionPayCapabilities>() {
        @Override
        public UnionPayCapabilities createFromParcel(Parcel in) {
            return new UnionPayCapabilities(in);
        }

        @Override
        public UnionPayCapabilities[] newArray(int size) {
            return new UnionPayCapabilities[size];
        }
    };
}
