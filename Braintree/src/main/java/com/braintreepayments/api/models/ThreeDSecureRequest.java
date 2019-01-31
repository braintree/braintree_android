package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * A class to contain 3D Secure request information used for authentication
 */
public class ThreeDSecureRequest implements Parcelable {

    protected static final String AMOUNT_KEY = "amount";
    protected static final String CUSTOMER_KEY = "customer";
    protected static final String BILLING_ADDRESS_KEY = "billingAddress";
    protected static final String MOBILE_PHONE_NUMBER_KEY = "mobilePhoneNumber";
    protected static final String EMAIL_KEY = "email";
    protected static final String SHIPPING_METHOD_KEY = "shippingMethod";

    private String mNonce;
    private String mAmount;
    private String mMobilePhoneNumber;
    private String mEmail;
    private String mShippingMethod;
    private ThreeDSecurePostalAddress mBillingAddress;

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
    }

    public ThreeDSecureRequest(Parcel in) {
        mNonce = in.readString();
        mAmount = in.readString();
        mMobilePhoneNumber = in.readString();
        mEmail = in.readString();
        mShippingMethod = in.readString();
        mBillingAddress = in.readParcelable(ThreeDSecurePostalAddress.class.getClassLoader());
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

    //Todo: Finish this method and figure out why no ThreeDSecureRequest parameters are being initialized
    public String build(String dfReferenceId) {
        JSONObject base = new JSONObject();
        JSONObject additionalInformation = new JSONObject();

        try {
            base.put(AMOUNT_KEY, mAmount);

            additionalInformation.putOpt(MOBILE_PHONE_NUMBER_KEY, mMobilePhoneNumber);
            additionalInformation.putOpt(EMAIL_KEY, mEmail);
            additionalInformation.putOpt(SHIPPING_METHOD_KEY, mShippingMethod);
            Log.d("AdditionalInfo Log 1: ", additionalInformation.toString());

            if (mBillingAddress != null) {
                JSONObject postalAddress = mBillingAddress.toJson();

                // Merge postal address fields into additional information
                JSONObject[] mergedAdditionalInformation = new JSONObject[] {additionalInformation, postalAddress};
                for (JSONObject obj : mergedAdditionalInformation) {
                    Iterator iterator = obj.keys();
                    while (iterator.hasNext()) {
                        String key = (String)iterator.next();
                        additionalInformation.put(key, obj.get(key));
                        // Log.d("Added: ", key);
                    }
                }
            } else {
                Log.d("mBillingAddress: ", "NIL");
            }

            base.put("additionalInformation", additionalInformation);
            base.put("df_reference_id", dfReferenceId);

            Log.d("AdditionalInfo Log 2: ", additionalInformation.toString());
            Log.d("request", dfReferenceId);
        } catch (JSONException ignored) {}

        Log.d("3DS Request Params: ", base.toString());
        return base.toString();
    }

}
