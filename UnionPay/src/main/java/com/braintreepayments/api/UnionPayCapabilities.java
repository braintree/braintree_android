package com.braintreepayments.api;

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

    private boolean isUnionPay;
    private boolean isDebit;
    private boolean supportsTwoStepAuthAndCapture;
    private boolean isSupported;

    @NonNull
    public static UnionPayCapabilities fromJson(@NonNull String jsonString) {
        UnionPayCapabilities unionPayCapabilities = new UnionPayCapabilities();

        try {
            JSONObject json = new JSONObject(jsonString);
            unionPayCapabilities.isUnionPay = json.optBoolean(IS_UNIONPAY_KEY);
            unionPayCapabilities.isDebit = json.optBoolean(IS_DEBIT_KEY);

            if (json.has(UNIONPAY_KEY)) {
                JSONObject unionPay = json.getJSONObject(UNIONPAY_KEY);
                unionPayCapabilities.supportsTwoStepAuthAndCapture = unionPay.optBoolean(
                        SUPPORTS_TWO_STEP_AUTH_AND_CAPTURE_KEY);
                unionPayCapabilities.isSupported = unionPay.optBoolean(IS_SUPPORTED_KEY);
            }
        } catch (JSONException ignored) {}

        return unionPayCapabilities;
    }

    /**
     * @return true if this card is a UnionPay card.
     */
    public boolean isUnionPay() {
        return isUnionPay;
    }

    /**
     * @return true if this card ia a debit card.
     */
    public boolean isDebit() {
        return isDebit;
    }

    /**
     * @return true if this card allows for an authorization, but settling the transaction later.
     */
    public boolean supportsTwoStepAuthAndCapture() {
        return supportsTwoStepAuthAndCapture;
    }

    /**
     * @return true if Braintree can process this UnionPay card. When false, Braintree cannot process this card.
     * If Braintree cannot process this card, the customer should be informed.
     */
    public boolean isSupported() {
        return isSupported;
    }

    private UnionPayCapabilities() {}

    @Override
    public int describeContents() {
        return 0;
    }

    public UnionPayCapabilities(Parcel in) {
        isUnionPay = in.readByte() > 0;
        isDebit = in.readByte() > 0;
        supportsTwoStepAuthAndCapture = in.readByte() > 0;
        isSupported = in.readByte() > 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(isUnionPay ? (byte) 1 : 0);
        dest.writeByte(isDebit ? (byte) 1 : 0);
        dest.writeByte(supportsTwoStepAuthAndCapture ? (byte) 1 : 0);
        dest.writeByte(isSupported ? (byte) 1 : 0);
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
