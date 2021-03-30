package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.StringDef;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class to contain 3D Secure request information used for authentication
 */
public class ThreeDSecureRequest implements Parcelable {
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({VERSION_1, VERSION_2})
    @interface ThreeDSecureVersion {}
    public static final String VERSION_1 = "1";
    public static final String VERSION_2 = "2";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({CREDIT, DEBIT})
    @interface ThreeDSecureAccountType {}
    public static final String CREDIT = "credit";
    public static final String DEBIT = "debit";

    private String mNonce;
    private String mAmount;
    private String mMobilePhoneNumber;
    private String mEmail;
    private @ThreeDSecureShippingMethod int mShippingMethod;
    private ThreeDSecurePostalAddress mBillingAddress;
    private @ThreeDSecureVersion String mVersionRequested = VERSION_2;
    private @ThreeDSecureAccountType String mAccountType;
    private ThreeDSecureAdditionalInformation mAdditionalInformation;
    private boolean mChallengeRequested = false;
    private boolean mDataOnlyRequested = false;
    private boolean mExemptionRequested = false;
    private ThreeDSecureV2UiCustomization mV2UiCustomization;
    private ThreeDSecureV1UiCustomization mV1UiCustomization;

    /**
     * Set the nonce
     *
     * @param nonce The nonce that represents a card to perform a 3D Secure verification against.
     */
    public void setNonce(String nonce) {
        mNonce = nonce;
    }

    /**
     * Set the amount
     *
     * @param amount The amount of the transaction in the current merchant account's currency. This must be expressed in numbers with an optional decimal (using `.`) and precision up to the hundredths place. For example, if you're processing a transaction for 1.234,56 € then `amount` should be `1234.56`.
     */
    public void setAmount(String amount) {
        mAmount = amount;
    }

    /**
     * Optional. Set the mobilePhoneNumber
     *
     * @param mobilePhoneNumber The mobile phone number used for verification. Only numbers. Remove dashes, parentheses and other characters.
     */
    public void setMobilePhoneNumber(String mobilePhoneNumber) {
        mMobilePhoneNumber = mobilePhoneNumber;
    }

    /**
     * Optional. Set the email
     *
     * @param email The email used for verification.
     */
    public void setEmail(String email) {
        mEmail = email;
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
     */
    public void setShippingMethod(@ThreeDSecureShippingMethod int shippingMethod) {
        mShippingMethod = shippingMethod;
    }

    /**
     * Optional. Set the billingAddress
     *
     * @param billingAddress The billing address used for verification.
     */
    public void setBillingAddress(ThreeDSecurePostalAddress billingAddress) {
        mBillingAddress = billingAddress;
    }

    /**
     * Optional. Set the desired ThreeDSecure version.
     * Possible Values defined at {@link ThreeDSecureVersion}.
     * <ul>
     * <li>{@link #VERSION_2} if ThreeDSecure V2 flows are desired, when possible. Default value.</li>
     * <li>{@link #VERSION_1} if only ThreeDSecure V1 flows are desired.</li>
     * </ul>
     * <p>
     * Will default to {@link #VERSION_2}.
     *
     * @param versionRequested {@link ThreeDSecureVersion} The desired ThreeDSecure version.
     */
    public void setVersionRequested(@ThreeDSecureVersion String versionRequested) {
        mVersionRequested = versionRequested;
    }

    /**
     * Optional. The account type selected by the cardholder. Some cards can be processed using
     * either a credit or debit account and cardholders have the option to choose which account to use.
     * Possible values defined at {@link ThreeDSecureAccountType}.
     *
     * @param accountType {@link ThreeDSecureAccountType} The account type selected by the cardholder.
     */
    public void setAccountType(@ThreeDSecureAccountType String accountType) {
        mAccountType = accountType;
    }

    /**
     * Optional. The additional information used for verification
     *
     * @param additionalInformation Additional information.
     */
    public void setAdditionalInformation(ThreeDSecureAdditionalInformation additionalInformation) {
        mAdditionalInformation = additionalInformation;
    }

    /**
     * Optional If set to true, the customer will be asked to complete the authentication challenge if possible
     *
     * @param challengeRequested decides if a challenge will be forced.
     */
    public void setChallengeRequested(boolean challengeRequested) {
        mChallengeRequested = challengeRequested;
    }

    public void setDataOnlyRequested(boolean dataOnlyRequested) {
        mDataOnlyRequested = dataOnlyRequested;
    }

    /**
     * Optional If set to true, an exemption to the authentication challenge will be requested
     *
     * @param exemptionRequested decides if a exemption will be requested.
     */
    public void setExemptionRequested(boolean exemptionRequested) {
        mExemptionRequested = exemptionRequested;
    }

    /**
     * Optional UI Customization for the 3DS2 challenge views.
     * See <a href="https://cardinaldocs.atlassian.net/wiki/spaces/CMSDK/pages/863698999/UI+Customization">UiCustomization documentation</a>.
     *
     * @param v2UiCustomization specifies how 3DS2 challenge views should be customized.
     */
    public void setV2UiCustomization(ThreeDSecureV2UiCustomization v2UiCustomization) {
        mV2UiCustomization = v2UiCustomization;
    }

    /**
     * Optional UI Customization for the 3DS1 challenge views.
     *
     * @param v1UiCustomization specifies how 3DS1 challenge views should be customized.
     */
    public void setV1UiCustomization(ThreeDSecureV1UiCustomization v1UiCustomization) {
        mV1UiCustomization = v1UiCustomization;
    }

    /**
     * @return The nonce to use for 3D Secure verification
     */
    public String getNonce() {
        return mNonce;
    }

    /**
     * @return The amount to use for 3D Secure verification
     */
    public String getAmount() {
        return mAmount;
    }

    /**
     * @return The mobile phone number to use for 3D Secure verification
     */
    public String getMobilePhoneNumber() {
        return mMobilePhoneNumber;
    }

    /**
     * @return The email to use for 3D Secure verification
     */
    public String getEmail() {
        return mEmail;
    }

    /**
     * @return The shipping method to use for 3D Secure verification
     */
    public @ThreeDSecureShippingMethod int getShippingMethod() {
        return mShippingMethod;
    }

    /**
     * @return The billing address to use for 3D Secure verification
     */
    public ThreeDSecurePostalAddress getBillingAddress() {
        return mBillingAddress;
    }

    /**
     * @return The requested ThreeDSecure version
     */
    public @ThreeDSecureVersion String getVersionRequested() {
        return mVersionRequested;
    }

    /**
     * @return The account type
     */
    public @ThreeDSecureAccountType String getAccountType() {
        return mAccountType;
    }

    /**
     * @return The additional information used for verification
     * {@link ThreeDSecureAdditionalInformation} is only used for
     * {@link ThreeDSecureRequest#VERSION_2} requests.
     */
    public ThreeDSecureAdditionalInformation getAdditionalInformation() {
        return mAdditionalInformation;
    }

    /**
     * @return If a challenge has been requested
     */
    public boolean isChallengeRequested() {
        return mChallengeRequested;
    }

    public boolean isDataOnlyRequested() {
        return mDataOnlyRequested;
    }

    /**
     * @return If a exemption has been requested
     */
    public boolean isExemptionRequested() {
        return mExemptionRequested;
    }

    /**
     * @return The UI customization for 3DS2 challenge views.
     */
    public ThreeDSecureV2UiCustomization getV2UiCustomization() {
        return mV2UiCustomization;
    }

    /**
     * @return The UI customization for 3DS1 challenge views.
     */
    public ThreeDSecureV1UiCustomization getV1UiCustomization() {
        return mV1UiCustomization;
    }

    public ThreeDSecureRequest() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mNonce);
        dest.writeString(mAmount);
        dest.writeString(mMobilePhoneNumber);
        dest.writeString(mEmail);
        dest.writeInt(mShippingMethod);
        dest.writeParcelable(mBillingAddress, flags);
        dest.writeString(mVersionRequested);
        dest.writeParcelable(mAdditionalInformation, flags);
        dest.writeByte(mChallengeRequested ? (byte) 1 : 0);
        dest.writeByte(mDataOnlyRequested ? (byte) 1 : 0);
        dest.writeByte(mExemptionRequested ? (byte) 1 : 0);
        dest.writeParcelable(mV2UiCustomization, flags);
        dest.writeParcelable(mV1UiCustomization, flags);
        dest.writeString(mAccountType);
    }

    public ThreeDSecureRequest(Parcel in) {
        mNonce = in.readString();
        mAmount = in.readString();
        mMobilePhoneNumber = in.readString();
        mEmail = in.readString();
        mShippingMethod = in.readInt();
        mBillingAddress = in.readParcelable(ThreeDSecurePostalAddress.class.getClassLoader());
        mVersionRequested = in.readString();
        mAdditionalInformation = in.readParcelable(ThreeDSecureAdditionalInformation.class.getClassLoader());
        mChallengeRequested = in.readByte() > 0;
        mDataOnlyRequested = in.readByte() > 0;
        mExemptionRequested = in.readByte() > 0;
        mV2UiCustomization = in.readParcelable(ThreeDSecureV2UiCustomization.class.getClassLoader());
        mV1UiCustomization = in.readParcelable(ThreeDSecureV1UiCustomization.class.getClassLoader());
        mAccountType = in.readString();
    }

    public static final Creator<ThreeDSecureRequest> CREATOR = new Creator<ThreeDSecureRequest>() {
        public ThreeDSecureRequest createFromParcel(Parcel source) {
            return new ThreeDSecureRequest(source);
        }

        public ThreeDSecureRequest[] newArray(int size) {
            return new ThreeDSecureRequest[size];
        }
    };

    /**
     * @return String representation of {@link ThreeDSecureRequest} for API use.
     */
    public String build(String dfReferenceId) {
        JSONObject additionalInfo;
        JSONObject base = new JSONObject();
        ThreeDSecurePostalAddress billing = getBillingAddress();

        if (getAdditionalInformation() == null) {
            additionalInfo = new JSONObject();
        } else {
            additionalInfo = getAdditionalInformation().toJson();
        }

        try {
            base.put("amount", mAmount);
            base.put("additional_info", additionalInfo);
            base.putOpt("account_type", mAccountType);

            additionalInfo.putOpt("mobile_phone_number", getMobilePhoneNumber());
            additionalInfo.putOpt("shipping_method", getShippingMethodAsString());
            additionalInfo.putOpt("email", getEmail());

            if (billing != null) {
                additionalInfo.putOpt("billing_given_name", billing.getGivenName());
                additionalInfo.putOpt("billing_surname", billing.getSurname());
                additionalInfo.putOpt("billing_line1", billing.getStreetAddress());
                additionalInfo.putOpt("billing_line2", billing.getExtendedAddress());
                additionalInfo.putOpt("billing_line3", billing.getLine3());
                additionalInfo.putOpt("billing_city", billing.getLocality());
                additionalInfo.putOpt("billing_state", billing.getRegion());
                additionalInfo.putOpt("billing_postal_code", billing.getPostalCode());
                additionalInfo.putOpt("billing_country_code", billing.getCountryCodeAlpha2());
                additionalInfo.putOpt("billing_phone_number", billing.getPhoneNumber());
            }

            if (VERSION_2.equals(getVersionRequested())) {
                base.putOpt("df_reference_id", dfReferenceId);
            }

            base.put("challenge_requested", mChallengeRequested);
            base.put("data_only_requested", mDataOnlyRequested);
            base.put("exemption_requested", mExemptionRequested);
        } catch (JSONException ignored) {
        }

        return base.toString();
    }

    private String getShippingMethodAsString() {
        switch (mShippingMethod) {
            case ThreeDSecureShippingMethod.SAME_DAY:
                return "01";
            case ThreeDSecureShippingMethod.EXPEDITED:
                return "02";
            case ThreeDSecureShippingMethod.PRIORITY:
                return "03";
            case ThreeDSecureShippingMethod.GROUND:
                return "04";
            case ThreeDSecureShippingMethod.ELECTRONIC_DELIVERY:
                return "05";
            case ThreeDSecureShippingMethod.SHIP_TO_STORE:
                return "06";
            default:
                return null;
        }
    }
}
