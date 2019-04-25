package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Iterator;

import androidx.annotation.StringDef;

/**
 * A class to contain 3D Secure request information used for authentication
 */
public class ThreeDSecureRequest implements Parcelable {
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({VERSION_1, VERSION_2})
    @interface ThreeDSecureVersion {}
    public static final String VERSION_1 = "1";
    public static final String VERSION_2 = "2";

    protected static final String AMOUNT_KEY = "amount";
    protected static final String CUSTOMER_KEY = "customer";
    protected static final String BILLING_ADDRESS_KEY = "billingAddress";
    protected static final String MOBILE_PHONE_NUMBER_KEY = "mobilePhoneNumber";
    protected static final String EMAIL_KEY = "email";
    protected static final String SHIPPING_METHOD_KEY = "shippingMethod";
    protected static final String BIN_KEY = "bin";
    protected static final String ADDITIONAL_INFORMATION_KEY = "additionalInformation";

    private String mNonce;
    private String mAmount;
    private String mMobilePhoneNumber;
    private String mEmail;
    private String mShippingMethod;
    private ThreeDSecurePostalAddress mBillingAddress;
    private String mBin;
    private @ThreeDSecureVersion String mVersionRequested = VERSION_1;
    private ThreeDSecureAdditionalInformation mAdditionalInformation;

    /**
     * Set the nonce
     *
     * @param nonce The nonce that represents a card to perform a 3D Secure verification against.
     * */
    public ThreeDSecureRequest nonce(String nonce) {
        mNonce = nonce;
        return this;
    }

    /**
     * Set the amount
     *
     * @param amount The amount of the transaction in the current merchant account's currency.
     * */
    public ThreeDSecureRequest amount(String amount) {
        mAmount = amount;
        return this;
    }

    /**
     * Optional. Set the mobilePhoneNumber
     *
     * @param mobilePhoneNumber The mobile phone number used for verification. Only numbers. Remove dashes, parentheses and other characters.
     * */
    public ThreeDSecureRequest mobilePhoneNumber(String mobilePhoneNumber) {
        mMobilePhoneNumber = mobilePhoneNumber;
        return this;
    }

    /**
     * Optional. Set the email
     *
     * @param email The email used for verification.
     * */
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
     * */
    public ThreeDSecureRequest shippingMethod(String shippingMethod) {
        mShippingMethod = shippingMethod;
        return this;
    }

    /**
     * Optional. Set the billingAddress
     *
     * @param billingAddress The billing address used for verification.
     * */
    public ThreeDSecureRequest billingAddress(ThreeDSecurePostalAddress billingAddress) {
        mBillingAddress = billingAddress;
        return this;
    }

    /**
     * Optional. Set the BIN
     *
     * @param bin The BIN (Bank Identification Number) of the tokenized card.
     * */
    public ThreeDSecureRequest bin(String bin) {
        mBin = bin;
        return this;
    }

    /**
     * Optional. Set the desired ThreeDSecure version.
     * Possible Values defined at {@link ThreeDSecureVersion}.
     * <ul>
     * <li>{@link #VERSION_2} if ThreeDSecure V2 flows are desired, when possible.</li>
     * <li>{@link #VERSION_1} if only ThreeDSecure V1 flows are desired. Default value.</li>
     * </ul>
     *
     * Will default to {@link #VERSION_1}.
     *
     * @param versionRequested {@link ThreeDSecureVersion} The desired ThreeDSecure version.
     * */
    public ThreeDSecureRequest versionRequested(
            @ThreeDSecureVersion String versionRequested) {
        mVersionRequested = versionRequested;
        return this;
    }

    /**
     * Optional. The additional information used for verification
     *
     * @param additionalInformation Additional information.
     * */
    public ThreeDSecureRequest additionalInformation(ThreeDSecureAdditionalInformation additionalInformation) {
        mAdditionalInformation = additionalInformation;
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
     * @return The BIN of the tokenized card
     */
    public String getBin() {
        return mBin;
    }

    /**
     * @return The requested ThreeDSecure version
     */
    public @ThreeDSecureVersion String getVersionRequested() {
        return mVersionRequested;
    }

    /**
     * @return The additional information used for verification
     */
    public ThreeDSecureAdditionalInformation getAdditionalInformation() {
        return mAdditionalInformation;
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
        dest.writeString(mBin);
        dest.writeString(mVersionRequested);
        dest.writeParcelable(mAdditionalInformation, flags);
    }

    public ThreeDSecureRequest(Parcel in) {
        mNonce = in.readString();
        mAmount = in.readString();
        mMobilePhoneNumber = in.readString();
        mEmail = in.readString();
        mShippingMethod = in.readString();
        mBillingAddress = in.readParcelable(ThreeDSecurePostalAddress.class.getClassLoader());
        mBin = in.readString();
        mVersionRequested = in.readString();
        mAdditionalInformation = in.readParcelable(ThreeDSecureAdditionalInformation.class.getClassLoader());
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
        JSONObject base = new JSONObject();
        JSONObject additionalInformation;
        try {
            base.put(AMOUNT_KEY, mAmount);

            if (mAdditionalInformation != null) {
                additionalInformation = buildAdditionalInformationV2();
            } else {
                additionalInformation = buildAdditionalInformationV1();
            }

            base.put(ADDITIONAL_INFORMATION_KEY, additionalInformation);
            if (VERSION_2.equals(mVersionRequested)) {
                // Formats proper POST url by excluding dfReferenceId if 3DS 1.0 is desired (even when 2.0 is possible)
                base.put("df_reference_id", dfReferenceId);
            }

        } catch (JSONException ignored) {}

        return base.toString();
    }

    private JSONObject buildAdditionalInformationV1 () {
        JSONObject additionalInformation = new JSONObject();

        try {
            additionalInformation.putOpt(MOBILE_PHONE_NUMBER_KEY, mMobilePhoneNumber);
            additionalInformation.putOpt(EMAIL_KEY, mEmail);
            additionalInformation.putOpt(SHIPPING_METHOD_KEY, mShippingMethod);
            additionalInformation.putOpt(BIN_KEY, mBin);

            if (mBillingAddress != null) {
                JSONObject postalAddress = mBillingAddress.toJson();

                // Merge postal address fields into additional information
                JSONObject[] mergedAdditionalInformation = new JSONObject[] {additionalInformation, postalAddress};
                for (JSONObject obj : mergedAdditionalInformation) {
                    Iterator iterator = obj.keys();
                    while (iterator.hasNext()) {
                        String key = (String)iterator.next();
                        additionalInformation.put(key, obj.get(key));
                    }
                }
            }
        } catch (JSONException ignored) {}

        return additionalInformation;
    }

    private JSONObject buildAdditionalInformationV2 () {
        return mAdditionalInformation.toJson();
    }

}
