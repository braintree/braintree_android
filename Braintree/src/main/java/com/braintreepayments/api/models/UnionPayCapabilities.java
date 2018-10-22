package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

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
    private static final String IS_SUPPORTED_KEY = "isSupported";

    private boolean mIsUnionPay;
    private boolean mIsDebit;
    private boolean mSupportsTwoStepAuthAndCapture;
    private boolean mIsSupported;

    @NonNull
    public static UnionPayCapabilities fromJson(@NonNull String jsonString) {
        UnionPayCapabilities unionPayCapabilities = new UnionPayCapabilities();

        try {
            JSONObject json = new JSONObject(jsonString);
            unionPayCapabilities.mIsUnionPay = json.optBoolean(IS_UNIONPAY_KEY);
            unionPayCapabilities.mIsDebit = json.optBoolean(IS_DEBIT_KEY);

            if (json.has(UNIONPAY_KEY)) {
                JSONObject unionPay = json.getJSONObject(UNIONPAY_KEY);
                unionPayCapabilities.mSupportsTwoStepAuthAndCapture = unionPay.optBoolean(
                        SUPPORTS_TWO_STEP_AUTH_AND_CAPTURE_KEY);
                unionPayCapabilities.mIsSupported = unionPay.optBoolean(IS_SUPPORTED_KEY);
            }
        } catch (JSONException ignored) {}

        return unionPayCapabilities;
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
     * @return true if Braintree can process this UnionPay card. When false, Braintree cannot process this card.
     * If Braintree cannot process this card, the customer should be informed.
     */
    public boolean isSupported() {
        return mIsSupported;
    }

    private UnionPayCapabilities() {}

    @Override
    public int describeContents() {
        return 0;
    }

    public UnionPayCapabilities(Parcel in) {
        mIsUnionPay = in.readByte() > 0;
        mIsDebit = in.readByte() > 0;
        mSupportsTwoStepAuthAndCapture = in.readByte() > 0;
        mIsSupported = in.readByte() > 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(mIsUnionPay ? (byte) 1 : 0);
        dest.writeByte(mIsDebit ? (byte) 1 : 0);
        dest.writeByte(mSupportsTwoStepAuthAndCapture ? (byte) 1 : 0);
        dest.writeByte(mIsSupported ? (byte) 1 : 0);
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
