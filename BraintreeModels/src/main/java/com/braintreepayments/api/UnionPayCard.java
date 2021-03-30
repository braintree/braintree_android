package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builder used to construct a UnionPay card tokenization request.
 */
public class UnionPayCard extends BaseCard implements Parcelable {

    private static final String UNIONPAY_ENROLLMENT_KEY = "unionPayEnrollment";
    private static final String UNIONPAY_KEY = "creditCard";
    private static final String MOBILE_COUNTRY_CODE_KEY = "mobileCountryCode";
    private static final String MOBILE_PHONE_NUMBER_KEY = "mobileNumber";
    private static final String SMS_CODE_KEY = "smsCode";
    private static final String ENROLLMENT_ID_KEY = "id";

    private String mMobileCountryCode;
    private String mMobilePhoneNumber;
    private String mSmsCode;
    private String mEnrollmentId;

    public UnionPayCard() {}

    /**
     * @param mobileCountryCode The mobile country code to use when sending the auth code via SMS.
     */
    public void setMobileCountryCode(String mobileCountryCode) {
        if (TextUtils.isEmpty(mobileCountryCode)) {
            mMobileCountryCode = null;
        } else {
            mMobileCountryCode = mobileCountryCode;
        }
    }

    /**
     * @param mobilePhoneNumber The mobile phone number to use when sending the auth code via SMS.
     */
    public void setMobilePhoneNumber(String mobilePhoneNumber) {
        if (TextUtils.isEmpty(mobilePhoneNumber)) {
            mMobilePhoneNumber = null;
        } else {
            mMobilePhoneNumber = mobilePhoneNumber;
        }
    }

    /**
     * @param smsCode The auth code sent to the user via SMS.
     */
    public void setSmsCode(String smsCode) {
        if (TextUtils.isEmpty(smsCode)) {
            mSmsCode = null;
        } else {
            mSmsCode = smsCode;
        }
    }

    /**
     * @param enrollmentId The UnionPay enrollment ID
     */
    public void setEnrollmentId(String enrollmentId) {
        if (TextUtils.isEmpty(enrollmentId)) {
            mEnrollmentId = null;
        } else {
            mEnrollmentId = enrollmentId;
        }
    }

    /**
     * @deprecated UnionPay enrollment performs validation. This value will not be used for UnionPay payment methods.
     *
     * @param validate Ignored
     */
    @Deprecated
    @Override
    public void setValidate(boolean validate) {
        // prevent validation for union pay methods
    }

    public JSONObject buildEnrollment() throws JSONException {
        JSONObject unionPayEnrollment = new JSONObject();
        unionPayEnrollment.put(NUMBER_KEY, mNumber);
        unionPayEnrollment.put(EXPIRATION_MONTH_KEY, mExpirationMonth);
        unionPayEnrollment.put(EXPIRATION_YEAR_KEY, mExpirationYear);
        unionPayEnrollment.put(MOBILE_COUNTRY_CODE_KEY, mMobileCountryCode);
        unionPayEnrollment.put(MOBILE_PHONE_NUMBER_KEY, mMobilePhoneNumber);

        JSONObject payload = new JSONObject();
        payload.put(UNIONPAY_ENROLLMENT_KEY, unionPayEnrollment);

        return payload;
    }

    @Override
    protected void buildJSON(JSONObject json, JSONObject paymentMethodNonceJson) throws JSONException {
        super.buildJSON(json, paymentMethodNonceJson);

        JSONObject options = paymentMethodNonceJson.optJSONObject(OPTIONS_KEY);
        if (options == null) {
            options = new JSONObject();
            paymentMethodNonceJson.put(OPTIONS_KEY, options);
        }
        JSONObject unionPayEnrollment = new JSONObject();
        unionPayEnrollment.put(SMS_CODE_KEY, mSmsCode);
        unionPayEnrollment.put(ENROLLMENT_ID_KEY, mEnrollmentId);
        options.put(UNIONPAY_ENROLLMENT_KEY, unionPayEnrollment);

        json.put(UNIONPAY_KEY, paymentMethodNonceJson);
    }

    @Override
    protected void buildGraphQL(JSONObject base, JSONObject variables) {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mMobileCountryCode);
        dest.writeString(mMobilePhoneNumber);
        dest.writeString(mSmsCode);
        dest.writeString(mEnrollmentId);
    }

    protected UnionPayCard(Parcel in) {
        super(in);
        mMobileCountryCode = in.readString();
        mMobilePhoneNumber = in.readString();
        mSmsCode = in.readString();
        mEnrollmentId = in.readString();
    }

    public static final Creator<UnionPayCard> CREATOR = new Creator<UnionPayCard>() {
        @Override
        public UnionPayCard createFromParcel(Parcel in) {
            return new UnionPayCard(in);
        }

        @Override
        public UnionPayCard[] newArray(int size) {
            return new UnionPayCard[size];
        }
    };
}
