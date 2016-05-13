package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builder used to construct a UnionPay card tokenization request.
 */
public class UnionPayCardBuilder extends BaseCardBuilder<UnionPayCardBuilder> implements Parcelable {

    private static final String UNIONPAY_ENROLLMENT_KEY = "unionPayEnrollment";
    private static final String UNIONPAY_KEY = "creditCard";
    private static final String BASE_OPTIONS_KEY = "options";
    private static final String MOBILE_COUNTRY_CODE_KEY = "mobileCountryCode";
    private static final String MOBILE_PHONE_NUMBER_KEY = "mobileNumber";
    private static final String SMS_CODE_KEY = "smsCode";
    private static final String ENROLLMENT_ID_KEY = "id";

    private String mMobileCountryCode;
    private String mMobilePhoneNumber;
    private String mSmsCode;
    private String mEnrollmentId;

    public UnionPayCardBuilder() {}

    /**
     * @param mobileCountryCode The mobile country code to use when sending the auth code via SMS.
     * @return {@link com.braintreepayments.api.models.UnionPayCardBuilder}
     */
    public UnionPayCardBuilder mobileCountryCode(String mobileCountryCode) {
        mMobileCountryCode = mobileCountryCode;
        return this;
    }

    /**
     * @param mobilePhoneNumber The mobile phone number to use when sending the auth code via SMS.
     * @return {@link com.braintreepayments.api.models.UnionPayCardBuilder}
     */
    public UnionPayCardBuilder mobilePhoneNumber(String mobilePhoneNumber) {
        mMobilePhoneNumber = mobilePhoneNumber;
        return this;
    }

    /**
     * @param smsCode The auth code sent to the user via SMS.
     * @return {@link com.braintreepayments.api.models.UnionPayCardBuilder}
     */
    public UnionPayCardBuilder smsCode(String smsCode) {
        mSmsCode = smsCode;
        return this;
    }

    /**
     * @param enrollmentId The UnionPay enrollment ID
     * @return {@link com.braintreepayments.api.models.UnionPayCardBuilder}
     */
    public UnionPayCardBuilder enrollmentId(String enrollmentId) {
        mEnrollmentId = enrollmentId;
        return this;
    }

    public JSONObject buildEnrollment() throws JSONException {
        JSONObject unionPayEnrollment = new JSONObject();
        unionPayEnrollment.put(NUMBER_KEY, mCardnumber);
        unionPayEnrollment.put(EXPIRATION_MONTH_KEY, mExpirationMonth);
        unionPayEnrollment.put(EXPIRATION_YEAR_KEY, mExpirationYear);
        unionPayEnrollment.put(EXPIRATION_DATE_KEY, mExpirationDate);
        unionPayEnrollment.put(MOBILE_COUNTRY_CODE_KEY, mMobileCountryCode);
        unionPayEnrollment.put(MOBILE_PHONE_NUMBER_KEY, mMobilePhoneNumber);

        JSONObject payload = new JSONObject();
        payload.put(UNIONPAY_ENROLLMENT_KEY, unionPayEnrollment);

        return payload;
    }

    @Override
    protected void build(JSONObject json, JSONObject paymentMethodNonceJson) throws JSONException {
        super.build(json, paymentMethodNonceJson);

        JSONObject baseOptionsJson = new JSONObject();
        baseOptionsJson.put(SMS_CODE_KEY, mSmsCode);
        baseOptionsJson.put(ENROLLMENT_ID_KEY, mEnrollmentId);

        json.put(UNIONPAY_KEY, paymentMethodNonceJson);
        json.put(BASE_OPTIONS_KEY, baseOptionsJson);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mMobileCountryCode);
        dest.writeString(mMobilePhoneNumber);
        dest.writeString(mSmsCode);
        dest.writeString(mEnrollmentId);
    }

    protected UnionPayCardBuilder(Parcel in) {
        super(in);
        mMobileCountryCode = in.readString();
        mMobilePhoneNumber = in.readString();
        mSmsCode = in.readString();
        mEnrollmentId = in.readString();
    }

    public static final Creator<UnionPayCardBuilder> CREATOR = new Creator<UnionPayCardBuilder>() {
        @Override
        public UnionPayCardBuilder createFromParcel(Parcel in) {
            return new UnionPayCardBuilder(in);
        }

        @Override
        public UnionPayCardBuilder[] newArray(int size) {
            return new UnionPayCardBuilder[size];
        }
    };
}
