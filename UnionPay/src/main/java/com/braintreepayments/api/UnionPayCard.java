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

    private String mobileCountryCode;
    private String mobilePhoneNumber;
    private String smsCode;
    private String enrollmentId;

    public UnionPayCard() {}

    /**
     * @param mobileCountryCode The mobile country code to use when sending the auth code via SMS.
     */
    public void setMobileCountryCode(String mobileCountryCode) {
        if (TextUtils.isEmpty(mobileCountryCode)) {
            this.mobileCountryCode = null;
        } else {
            this.mobileCountryCode = mobileCountryCode;
        }
    }

    /**
     * @param mobilePhoneNumber The mobile phone number to use when sending the auth code via SMS.
     */
    public void setMobilePhoneNumber(String mobilePhoneNumber) {
        if (TextUtils.isEmpty(mobilePhoneNumber)) {
            this.mobilePhoneNumber = null;
        } else {
            this.mobilePhoneNumber = mobilePhoneNumber;
        }
    }

    /**
     * @param smsCode The auth code sent to the user via SMS.
     */
    public void setSmsCode(String smsCode) {
        if (TextUtils.isEmpty(smsCode)) {
            this.smsCode = null;
        } else {
            this.smsCode = smsCode;
        }
    }

    /**
     * @param enrollmentId The UnionPay enrollment ID
     */
    public void setEnrollmentId(String enrollmentId) {
        if (TextUtils.isEmpty(enrollmentId)) {
            this.enrollmentId = null;
        } else {
            this.enrollmentId = enrollmentId;
        }
    }

    public JSONObject buildEnrollment() throws JSONException {
        JSONObject unionPayEnrollment = new JSONObject();
        unionPayEnrollment.put(NUMBER_KEY, number);
        unionPayEnrollment.put(EXPIRATION_MONTH_KEY, expirationMonth);
        unionPayEnrollment.put(EXPIRATION_YEAR_KEY, expirationYear);
        unionPayEnrollment.put(MOBILE_COUNTRY_CODE_KEY, mobileCountryCode);
        unionPayEnrollment.put(MOBILE_PHONE_NUMBER_KEY, mobilePhoneNumber);

        JSONObject payload = new JSONObject();
        payload.put(UNIONPAY_ENROLLMENT_KEY, unionPayEnrollment);

        return payload;
    }

    @Override
    JSONObject buildJSON() {
        JSONObject json = super.buildJSON();

        try {
            JSONObject paymentMethodNonceJson = json.getJSONObject(BaseCard.CREDIT_CARD_KEY);
            JSONObject options = paymentMethodNonceJson.optJSONObject(OPTIONS_KEY);
            if (options == null) {
                options = new JSONObject();
                paymentMethodNonceJson.put(OPTIONS_KEY, options);
            }
            JSONObject unionPayEnrollment = new JSONObject();
            unionPayEnrollment.put(SMS_CODE_KEY, smsCode);
            unionPayEnrollment.put(ENROLLMENT_ID_KEY, enrollmentId);
            options.put(UNIONPAY_ENROLLMENT_KEY, unionPayEnrollment);

            json.put(UNIONPAY_KEY, paymentMethodNonceJson);

        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return json;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mobileCountryCode);
        dest.writeString(mobilePhoneNumber);
        dest.writeString(smsCode);
        dest.writeString(enrollmentId);
    }

    protected UnionPayCard(Parcel in) {
        super(in);
        mobileCountryCode = in.readString();
        mobilePhoneNumber = in.readString();
        smsCode = in.readString();
        enrollmentId = in.readString();
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
