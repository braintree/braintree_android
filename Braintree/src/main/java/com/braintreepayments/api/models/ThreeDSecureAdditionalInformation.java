package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.braintreepayments.api.ThreeDSecure;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * A class containing additional information for ThreeDSecure 2.0 Requests
 */
public class ThreeDSecureAdditionalInformation implements Parcelable {

    private String mBillingGivenName;
    private String mBillingSurname;
    private String mBillingPhoneNumber;
    private String mEmail;
    private String mShippingMethod;
    private ThreeDSecurePostalAddress mBillingAddress;

    public ThreeDSecureAdditionalInformation() {}

    /**
     * Optional. Set the billing given name
     *
     * @param billingGivenName Billing given name associated with the transaction.
     * */
    public ThreeDSecureAdditionalInformation billingGivenName(String billingGivenName) {
        mBillingGivenName = billingGivenName;
        return this;
    }

    /**
     * Optional. Set the billing surname
     *
     * @param billingSurname Billing surname associated with the transaction.
     * */
    public ThreeDSecureAdditionalInformation billingSurname(String billingSurname) {
        mBillingSurname = billingSurname;
        return this;
    }

    /**
     * Optional. Set the mobilePhoneNumber
     *
     * @param billingPhoneNumber The mobile phone number used for verification. Only numbers. Remove dashes, parentheses and other characters.
     * */
    public ThreeDSecureAdditionalInformation billingPhoneNumber(String billingPhoneNumber) {
        mBillingPhoneNumber = billingPhoneNumber;
        return this;
    }

    /**
     * Optional. Set the email
     *
     * @param email The email used for verification.
     * */
    public ThreeDSecureAdditionalInformation email(String email) {
        mEmail = email;
        return this;
    }

    /**
     * Optional. Set the shippingMethod
     * Possible Values:
     * 01 Same Day
     * 02 Overnight / Expedited
     * 03 Priority (2-3 Days)
     * 04 Ground
     * 05 Electronic Delivery
     * 06 Ship to Store
     *
     * @param shippingMethod The 2-digit string indicating the shipping method chosen for the transaction.
     * */
    public ThreeDSecureAdditionalInformation shippingMethod(String shippingMethod) {
        mShippingMethod = shippingMethod;
        return this;
    }

    /**
     * Optional. Set the billing address
     *
     * @param billingAddress Billing address.
     * */
    public ThreeDSecureAdditionalInformation billingAddress(ThreeDSecurePostalAddress billingAddress) {
        mBillingAddress = billingAddress;
        return this;
    }

    /**
     * @return Billing given name associated with the transaction.
     */
    public String getBillingGivenName() {
        return mBillingGivenName;
    }

    /**
     * @return Billing surname associated with the transaction.
     */
    public String getBillingSurname() {
        return mBillingSurname;
    }

    /**
     * @return Billing phone number associated with the transaction.
     */
    public String getBillingPhoneNumber() {
        return mBillingPhoneNumber;
    }

    /**
     * @return Email.
     */
    public String getEmail() {
        return mEmail;
    }

    /**
     * @return Shipping method.
     */
    public String getShippingMethod() {
        return mShippingMethod;
    }

    /**
     * @return Billing address.
     */
    public ThreeDSecurePostalAddress getBillingAddress() {
        return mBillingAddress;
    }

    public ThreeDSecureAdditionalInformation(Parcel in) {
        mBillingGivenName = in.readString();
        mBillingSurname = in.readString();
        mBillingPhoneNumber = in.readString();
        mEmail = in.readString();
        mShippingMethod = in.readString();
        mBillingAddress = in.readParcelable(ThreeDSecurePostalAddress.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mBillingGivenName);
        dest.writeString(mBillingSurname);
        dest.writeString(mBillingPhoneNumber);
        dest.writeString(mEmail);
        dest.writeString(mShippingMethod);
        dest.writeParcelable(mBillingAddress, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ThreeDSecureAdditionalInformation> CREATOR = new Parcelable.Creator<ThreeDSecureAdditionalInformation>() {
        @Override
        public ThreeDSecureAdditionalInformation createFromParcel(Parcel in) {
            return new ThreeDSecureAdditionalInformation(in);
        }

        @Override
        public ThreeDSecureAdditionalInformation[] newArray(int size) {
            return new ThreeDSecureAdditionalInformation[size];
        }
    };

    /**
     * @return JSONObject representation of {@link ThreeDSecureAdditionalInformation}.
     */
    public JSONObject toJson() {
        JSONObject additionalInformation = new JSONObject();

        try {
            additionalInformation.putOpt(ThreeDSecurePostalAddress.FIRST_NAME_KEY, mBillingGivenName);
            additionalInformation.putOpt(ThreeDSecurePostalAddress.LAST_NAME_KEY, mBillingSurname);
            additionalInformation.putOpt(ThreeDSecureRequest.MOBILE_PHONE_NUMBER_KEY, mBillingPhoneNumber);
            additionalInformation.putOpt(ThreeDSecureRequest.EMAIL_KEY, mEmail);
            additionalInformation.putOpt(ThreeDSecureRequest.SHIPPING_METHOD_KEY, mShippingMethod);

            if (mBillingAddress != null) {
                additionalInformation.putOpt(ThreeDSecurePostalAddress.STREET_ADDRESS_KEY, mBillingAddress.getStreetAddress());
                additionalInformation.putOpt(ThreeDSecurePostalAddress.EXTENDED_ADDRESS_KEY, mBillingAddress.getExtendedAddress());
                additionalInformation.putOpt(ThreeDSecurePostalAddress.LOCALITY_KEY, mBillingAddress.getLocality());
                additionalInformation.putOpt(ThreeDSecurePostalAddress.REGION_KEY, mBillingAddress.getRegion());
                additionalInformation.putOpt(ThreeDSecurePostalAddress.POSTAL_CODE_KEY, mBillingAddress.getPostalCode());
                additionalInformation.putOpt(ThreeDSecurePostalAddress.COUNTRY_CODE_ALPHA_2_KEY, mBillingAddress.getCountryCodeAlpha2());
                additionalInformation.putOpt(ThreeDSecurePostalAddress.PHONE_NUMBER_KEY, mBillingAddress.getPhoneNumber());
            }
        } catch (JSONException ignored) {}

        return additionalInformation;
    }

}
