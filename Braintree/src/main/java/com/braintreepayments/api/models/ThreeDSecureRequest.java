package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.StringDef;

import com.cardinalcommerce.shared.userinterfaces.UiCustomization;

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

    private String mNonce;
    private String mAmount;
    private String mMobilePhoneNumber;
    private String mEmail;
    private String mShippingMethod;
    private ThreeDSecurePostalAddress mBillingAddress;
    private @ThreeDSecureVersion String mVersionRequested = VERSION_1;
    private ThreeDSecureAdditionalInformation mAdditionalInformation;
    private boolean mChallengeRequested = false;
    private boolean mExemptionRequested = false;
    private UiCustomization mUiCustomization;

    /**
     * Set the nonce
     *
     * @param nonce The nonce that represents a card to perform a 3D Secure verification against.
     */
    public ThreeDSecureRequest nonce(String nonce) {
        mNonce = nonce;
        return this;
    }

    /**
     * Set the amount
     *
     * @param amount The amount of the transaction in the current merchant account's currency. This must be expressed in numbers with an optional decimal (using `.`) and precision up to the hundredths place. For example, if you're processing a transaction for 1.234,56 € then `amount` should be `1234.56`.
     */
    public ThreeDSecureRequest amount(String amount) {
        mAmount = amount;
        return this;
    }

    /**
     * Optional. Set the mobilePhoneNumber
     *
     * @param mobilePhoneNumber The mobile phone number used for verification. Only numbers. Remove dashes, parentheses and other characters.
     */
    public ThreeDSecureRequest mobilePhoneNumber(String mobilePhoneNumber) {
        mMobilePhoneNumber = mobilePhoneNumber;
        return this;
    }

    /**
     * Optional. Set the email
     *
     * @param email The email used for verification.
     */
    public ThreeDSecureRequest email(String email) {
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
     */
    public ThreeDSecureRequest shippingMethod(String shippingMethod) {
        mShippingMethod = shippingMethod;
        return this;
    }

    /**
     * Optional. Set the billingAddress
     *
     * @param billingAddress The billing address used for verification.
     */
    public ThreeDSecureRequest billingAddress(ThreeDSecurePostalAddress billingAddress) {
        mBillingAddress = billingAddress;
        return this;
    }

    /**
     * Optional. Set the desired ThreeDSecure version.
     * Possible Values defined at {@link ThreeDSecureVersion}.
     * <ul>
     * <li>{@link #VERSION_2} if ThreeDSecure V2 flows are desired, when possible.</li>
     * <li>{@link #VERSION_1} if only ThreeDSecure V1 flows are desired. Default value.</li>
     * </ul>
     * <p>
     * Will default to {@link #VERSION_1}.
     *
     * @param versionRequested {@link ThreeDSecureVersion} The desired ThreeDSecure version.
     */
    public ThreeDSecureRequest versionRequested(@ThreeDSecureVersion String versionRequested) {
        mVersionRequested = versionRequested;
        return this;
    }

    /**
     * Optional. The additional information used for verification
     *
     * @param additionalInformation Additional information.
     */
    public ThreeDSecureRequest additionalInformation(ThreeDSecureAdditionalInformation additionalInformation) {
        mAdditionalInformation = additionalInformation;
        return this;
    }

    /**
     * Optional If set to true, the customer will be asked to complete the authentication challenge if possible
     *
     * @param challengeRequested decides if a challenge will be forced.
     */
    public ThreeDSecureRequest challengeRequested(boolean challengeRequested) {
        mChallengeRequested = challengeRequested;
        return this;
    }

    /**
     * Optional If set to true, an exemption to the authentication challenge will be requested
     *
     * @param exemptionRequested decides if a exemption will be requested.
     */
    public ThreeDSecureRequest exemptionRequested(boolean exemptionRequested) {
        mExemptionRequested = exemptionRequested;
        return this;
    }

    /**
     * Optional UI Customization for the 3DS 2 challenge views.
     * See <a href="https://cardinaldocs.atlassian.net/wiki/spaces/CMSDK/pages/863698999/UI+Customization">UiCustomization documentation</a>.
     *
     * @param uiCustomization specifies how 3DS 2 challenge views should be customized.
     */
    public ThreeDSecureRequest uiCustomization(UiCustomization uiCustomization) {
        mUiCustomization = uiCustomization;
        return this;
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
    public String getShippingMethod() {
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

    /**
     * @return If a exemption has been requested
     */
    public boolean isExemptionRequested() {
        return mExemptionRequested;
    }

    /**
     * @return The UI customization for 3DS challenge views.
     */
    public UiCustomization getUiCustomization() {
        return mUiCustomization;
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
        dest.writeString(mShippingMethod);
        dest.writeParcelable(mBillingAddress, flags);
        dest.writeString(mVersionRequested);
        dest.writeParcelable(mAdditionalInformation, flags);
        dest.writeByte(mChallengeRequested ? (byte) 1 : 0);
        dest.writeByte(mExemptionRequested ? (byte) 1 : 0);
        dest.writeSerializable(mUiCustomization);
    }

    public ThreeDSecureRequest(Parcel in) {
        mNonce = in.readString();
        mAmount = in.readString();
        mMobilePhoneNumber = in.readString();
        mEmail = in.readString();
        mShippingMethod = in.readString();
        mBillingAddress = in.readParcelable(ThreeDSecurePostalAddress.class.getClassLoader());
        mVersionRequested = in.readString();
        mAdditionalInformation = in.readParcelable(ThreeDSecureAdditionalInformation.class.getClassLoader());
        mChallengeRequested = in.readByte() > 0;
        mExemptionRequested = in.readByte() > 0;
        mUiCustomization = (UiCustomization) in.readSerializable();
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

            additionalInfo.putOpt("mobile_phone_number", getMobilePhoneNumber());
            additionalInfo.putOpt("shipping_method", getShippingMethod());
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
            base.put("exemption_requested", mExemptionRequested);
        } catch (JSONException ignored) {
        }

        return base.toString();
    }
}
