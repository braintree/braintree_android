package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
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

    private String nonce;
    private String amount;
    private String mobilePhoneNumber;
    private String email;
    private @ThreeDSecureShippingMethod int shippingMethod;
    private ThreeDSecurePostalAddress billingAddress;
    private @ThreeDSecureVersion String versionRequested = VERSION_2;
    private @ThreeDSecureAccountType String accountType;
    private ThreeDSecureAdditionalInformation additionalInformation;
    private boolean challengeRequested = false;
    private boolean dataOnlyRequested = false;
    private boolean exemptionRequested = false;
    private Boolean cardAddChallengeRequested;
    private ThreeDSecureV2UiCustomization v2UiCustomization;
    private ThreeDSecureV1UiCustomization v1UiCustomization;

    /**
     * Set the nonce
     *
     * @param nonce The nonce that represents a card to perform a 3D Secure verification against.
     */
    public void setNonce(@Nullable String nonce) {
        this.nonce = nonce;
    }

    /**
     * Set the amount
     *
     * @param amount The amount of the transaction in the current merchant account's currency. This must be expressed in numbers with an optional decimal (using `.`) and precision up to the hundredths place. For example, if you're processing a transaction for 1.234,56 € then `amount` should be `1234.56`.
     */
    public void setAmount(@Nullable String amount) {
        this.amount = amount;
    }

    /**
     * Optional. Set the mobilePhoneNumber
     *
     * @param mobilePhoneNumber The mobile phone number used for verification. Only numbers. Remove dashes, parentheses and other characters.
     */
    public void setMobilePhoneNumber(@Nullable String mobilePhoneNumber) {
        this.mobilePhoneNumber = mobilePhoneNumber;
    }

    /**
     * Optional. Set the email
     *
     * @param email The email used for verification.
     */
    public void setEmail(@Nullable String email) {
        this.email = email;
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
        this.shippingMethod = shippingMethod;
    }

    /**
     * Optional. Set the billingAddress
     *
     * @param billingAddress The billing address used for verification.
     */
    public void setBillingAddress(@Nullable ThreeDSecurePostalAddress billingAddress) {
        this.billingAddress = billingAddress;
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
    public void setVersionRequested(@Nullable @ThreeDSecureVersion String versionRequested) {
        this.versionRequested = versionRequested;
    }

    /**
     * Optional. The account type selected by the cardholder. Some cards can be processed using
     * either a credit or debit account and cardholders have the option to choose which account to use.
     * Possible values defined at {@link ThreeDSecureAccountType}.
     *
     * @param accountType {@link ThreeDSecureAccountType} The account type selected by the cardholder.
     */
    public void setAccountType(@Nullable @ThreeDSecureAccountType String accountType) {
        this.accountType = accountType;
    }

    /**
     * Optional. The additional information used for verification
     *
     * @param additionalInformation Additional information.
     */
    public void setAdditionalInformation(@Nullable ThreeDSecureAdditionalInformation additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    /**
     * Optional If set to true, the customer will be asked to complete the authentication challenge if possible
     *
     * @param challengeRequested decides if a challenge will be forced.
     */
    public void setChallengeRequested(boolean challengeRequested) {
        this.challengeRequested = challengeRequested;
    }

    public void setDataOnlyRequested(boolean dataOnlyRequested) {
        this.dataOnlyRequested = dataOnlyRequested;
    }

    /**
     * Optional If set to true, an exemption to the authentication challenge will be requested
     *
     * @param exemptionRequested decides if a exemption will be requested.
     */
    public void setExemptionRequested(boolean exemptionRequested) {
        this.exemptionRequested = exemptionRequested;
    }

    /**
     * Optional. An authentication created using this flag should only be used for adding a
     * payment method to the merchant's vault and not for creating transactions.
     *
     * @param cardAddChallengeRequested If set to true, the authentication challenge will be requested
     *                                  from the issuer to confirm adding new card to the merchant's
     *                                  vault. If not set and amount is 0, the authentication challenge
     *                                  will be presented to the user. If set to false, when the amount
     *                                  is 0, the authentication challenge will not be presented to the user.
     *
     */
    public void setCardAddChallengeRequested(boolean cardAddChallengeRequested) {
        this.cardAddChallengeRequested = cardAddChallengeRequested;
    }

    /**
     * Optional UI Customization for the 3DS2 challenge views.
     * See <a href="https://cardinaldocs.atlassian.net/wiki/spaces/CMSDK/pages/863698999/UI+Customization">UiCustomization documentation</a>.
     *
     * @param v2UiCustomization specifies how 3DS2 challenge views should be customized.
     */
    public void setV2UiCustomization(@Nullable ThreeDSecureV2UiCustomization v2UiCustomization) {
        this.v2UiCustomization = v2UiCustomization;
    }

    /**
     * Optional UI Customization for the 3DS1 challenge views.
     *
     * @param v1UiCustomization specifies how 3DS1 challenge views should be customized.
     */
    public void setV1UiCustomization(@Nullable ThreeDSecureV1UiCustomization v1UiCustomization) {
        this.v1UiCustomization = v1UiCustomization;
    }

    /**
     * @return The nonce to use for 3D Secure verification
     */
    @Nullable
    public String getNonce() {
        return nonce;
    }

    /**
     * @return The amount to use for 3D Secure verification
     */
    @Nullable
    public String getAmount() {
        return amount;
    }

    /**
     * @return The mobile phone number to use for 3D Secure verification
     */
    @Nullable
    public String getMobilePhoneNumber() {
        return mobilePhoneNumber;
    }

    /**
     * @return The email to use for 3D Secure verification
     */
    @Nullable
    public String getEmail() {
        return email;
    }

    /**
     * @return The shipping method to use for 3D Secure verification
     */
    public @ThreeDSecureShippingMethod int getShippingMethod() {
        return shippingMethod;
    }

    /**
     * @return The billing address to use for 3D Secure verification
     */
    @Nullable
    public ThreeDSecurePostalAddress getBillingAddress() {
        return billingAddress;
    }

    /**
     * @return The requested ThreeDSecure version
     */
    @Nullable
    public @ThreeDSecureVersion String getVersionRequested() {
        return versionRequested;
    }

    /**
     * @return The account type
     */
    @Nullable
    public @ThreeDSecureAccountType String getAccountType() {
        return accountType;
    }

    /**
     * @return The additional information used for verification
     * {@link ThreeDSecureAdditionalInformation} is only used for
     * {@link ThreeDSecureRequest#VERSION_2} requests.
     */
    @Nullable
    public ThreeDSecureAdditionalInformation getAdditionalInformation() {
        return additionalInformation;
    }

    /**
     * @return If a challenge has been requested
     */
    public boolean isChallengeRequested() {
        return challengeRequested;
    }

    public boolean isDataOnlyRequested() {
        return dataOnlyRequested;
    }

    /**
     * @return If a exemption has been requested
     */
    public boolean isExemptionRequested() {
        return exemptionRequested;
    }

    /**
     * @return If the authentication challenge will be requested from the issuer to confirm adding
     * new card to the merchant's vault.
     */
    public boolean isCardAddChallengeRequested() {
        return cardAddChallengeRequested;
    }

    /**
     * @return The UI customization for 3DS2 challenge views.
     */
    @Nullable
    public ThreeDSecureV2UiCustomization getV2UiCustomization() {
        return v2UiCustomization;
    }

    /**
     * @return The UI customization for 3DS1 challenge views.
     */
    @Nullable
    public ThreeDSecureV1UiCustomization getV1UiCustomization() {
        return v1UiCustomization;
    }

    public ThreeDSecureRequest() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nonce);
        dest.writeString(amount);
        dest.writeString(mobilePhoneNumber);
        dest.writeString(email);
        dest.writeInt(shippingMethod);
        dest.writeParcelable(billingAddress, flags);
        dest.writeString(versionRequested);
        dest.writeParcelable(additionalInformation, flags);
        dest.writeByte(challengeRequested ? (byte) 1 : 0);
        dest.writeByte(dataOnlyRequested ? (byte) 1 : 0);
        dest.writeByte(exemptionRequested ? (byte) 1 : 0);
        dest.writeByte(cardAddChallengeRequested ? (byte) 1 : 0);
        dest.writeParcelable(v2UiCustomization, flags);
        dest.writeParcelable(v1UiCustomization, flags);
        dest.writeString(accountType);
    }

    public ThreeDSecureRequest(Parcel in) {
        nonce = in.readString();
        amount = in.readString();
        mobilePhoneNumber = in.readString();
        email = in.readString();
        shippingMethod = in.readInt();
        billingAddress = in.readParcelable(ThreeDSecurePostalAddress.class.getClassLoader());
        versionRequested = in.readString();
        additionalInformation = in.readParcelable(ThreeDSecureAdditionalInformation.class.getClassLoader());
        challengeRequested = in.readByte() > 0;
        dataOnlyRequested = in.readByte() > 0;
        exemptionRequested = in.readByte() > 0;
        cardAddChallengeRequested = in.readByte() > 0;
        v2UiCustomization = in.readParcelable(ThreeDSecureV2UiCustomization.class.getClassLoader());
        v1UiCustomization = in.readParcelable(ThreeDSecureV1UiCustomization.class.getClassLoader());
        accountType = in.readString();
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
            base.put("amount", amount);
            base.put("additional_info", additionalInfo);
            base.putOpt("account_type", accountType);

            if (cardAddChallengeRequested) {
               base.put("card_add", cardAddChallengeRequested);
            }

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

            base.put("challenge_requested", challengeRequested);
            base.put("data_only_requested", dataOnlyRequested);
            base.put("exemption_requested", exemptionRequested);
        } catch (JSONException ignored) {
        }

        return base.toString();
    }

    private String getShippingMethodAsString() {
        switch (shippingMethod) {
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
