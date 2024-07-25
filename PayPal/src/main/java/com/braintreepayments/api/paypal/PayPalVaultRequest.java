package com.braintreepayments.api.paypal;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.braintreepayments.api.core.Authorization;
import com.braintreepayments.api.core.ClientToken;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.core.PostalAddressParser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the parameters that are needed to start the PayPal Vault flow
 */
public class PayPalVaultRequest extends PayPalRequest implements Parcelable {

    private boolean shouldOfferCredit;

    private String userAuthenticationEmail;

    private boolean enablePayPalAppSwitch = false;

    /**
     * @param hasUserLocationConsent is an optional parameter that informs the SDK
     * if your application has obtained consent from the user to collect location data in compliance with
     * <a href="https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive">Google Play Developer Program policies</a>
     * This flag enables PayPal to collect necessary information required for Fraud Detection and Risk Management.
     *
     * @see <a href="https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive">User Data policies for the Google Play Developer Program </a>
     * @see <a href="https://support.google.com/googleplay/android-developer/answer/9799150?hl=en#Prominent%20in-app%20disclosure">Examples of prominent in-app disclosures</a>
     */
    public PayPalVaultRequest(boolean hasUserLocationConsent) {
        super(hasUserLocationConsent);
    }

    /**
     * Optional: Offers PayPal Credit if the customer qualifies. Defaults to false.
     *
     * @param shouldOfferCredit Whether to offer PayPal Credit.
     */
    public void setShouldOfferCredit(boolean shouldOfferCredit) {
        this.shouldOfferCredit = shouldOfferCredit;
    }

    public boolean getShouldOfferCredit() {
        return shouldOfferCredit;
    }

    /**
     * Optional: User email to initiate a quicker authentication flow in cases where the user has a
     * PayPal Account with the same email.
     *
     * @param userAuthenticationEmail - email address of the payer
     */
    public void setUserAuthenticationEmail(@Nullable String userAuthenticationEmail) {
        this.userAuthenticationEmail = userAuthenticationEmail;
    }

    @Nullable
    public String getUserAuthenticationEmail() {
        return this.userAuthenticationEmail;
    }

    /**
     * Optional: Used to determine if the customer will use the PayPal app switch flow.
     * Defaults to `false`.
     * - Warning: This property is currently in beta and may change or be removed in future releases.
     *
     * @param enablePayPalAppSwitch - A boolean value indicating whether to enable the PayPal app switch flow.
     */
    public void setEnablePayPalAppSwitch(boolean enablePayPalAppSwitch) {
        this.enablePayPalAppSwitch = enablePayPalAppSwitch;
    }

    public boolean getEnablePayPalAppSwitch() {
        return this.enablePayPalAppSwitch;
    }

    String createRequestBody(
        Configuration configuration,
        Authorization authorization,
        String successUrl,
        String cancelUrl,
        @Nullable String universalLink
    ) throws JSONException {

        JSONObject parameters = new JSONObject()
                .put(RETURN_URL_KEY, successUrl)
                .put(CANCEL_URL_KEY, cancelUrl)
                .put(OFFER_CREDIT_KEY, shouldOfferCredit);

        if (authorization instanceof ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer());
        } else {
            parameters.put(TOKENIZATION_KEY, authorization.getBearer());
        }

        String billingAgreementDescription = getBillingAgreementDescription();
        if (!TextUtils.isEmpty(billingAgreementDescription)) {
            parameters.put(DESCRIPTION_KEY, billingAgreementDescription);
        }

        parameters.putOpt(PAYER_EMAIL_KEY, userAuthenticationEmail);

        if (enablePayPalAppSwitch && universalLink != null && !universalLink.isEmpty()) {
            parameters.put(ENABLE_APP_SWITCH_KEY, enablePayPalAppSwitch);
            parameters.put(OS_VERSION_KEY, Build.VERSION.SDK_INT);
            parameters.put(OS_TYPE_KEY, "Android");
            parameters.put(MERCHANT_APP_RETURN_URL_KEY, universalLink);
        }

        JSONObject experienceProfile = new JSONObject();
        experienceProfile.put(NO_SHIPPING_KEY, !isShippingAddressRequired());
        experienceProfile.put(LANDING_PAGE_TYPE_KEY, getLandingPageType());
        String displayName = getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = configuration.getPayPalDisplayName();
        }
        experienceProfile.put(DISPLAY_NAME_KEY, displayName);

        if (getLocaleCode() != null) {
            experienceProfile.put(LOCALE_CODE_KEY, getLocaleCode());
        }

        if (getShippingAddressOverride() != null) {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, !isShippingAddressEditable());

            JSONObject shippingAddressJson = new JSONObject();
            parameters.put(SHIPPING_ADDRESS_KEY, shippingAddressJson);

            PostalAddress shippingAddress = getShippingAddressOverride();
            shippingAddressJson.put(PostalAddressParser.LINE_1_KEY,
                    shippingAddress.getStreetAddress());
            shippingAddressJson.put(PostalAddressParser.LINE_2_KEY,
                    shippingAddress.getExtendedAddress());
            shippingAddressJson.put(PostalAddressParser.LOCALITY_KEY,
                    shippingAddress.getLocality());
            shippingAddressJson.put(PostalAddressParser.REGION_KEY, shippingAddress.getRegion());
            shippingAddressJson.put(PostalAddressParser.POSTAL_CODE_UNDERSCORE_KEY,
                    shippingAddress.getPostalCode());
            shippingAddressJson.put(PostalAddressParser.COUNTRY_CODE_UNDERSCORE_KEY,
                    shippingAddress.getCountryCodeAlpha2());
            shippingAddressJson.put(PostalAddressParser.RECIPIENT_NAME_UNDERSCORE_KEY,
                    shippingAddress.getRecipientName());
        } else {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, false);
        }

        if (getMerchantAccountId() != null) {
            parameters.put(MERCHANT_ACCOUNT_ID, getMerchantAccountId());
        }

        if (getRiskCorrelationId() != null) {
            parameters.put(CORRELATION_ID_KEY, getRiskCorrelationId());
        }

        parameters.put(EXPERIENCE_PROFILE_KEY, experienceProfile);
        return parameters.toString();
    }

    PayPalVaultRequest(Parcel in) {
        super(in);
        shouldOfferCredit = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte((byte) (shouldOfferCredit ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PayPalVaultRequest> CREATOR = new Creator<PayPalVaultRequest>() {
        @Override
        public PayPalVaultRequest createFromParcel(Parcel in) {
            return new PayPalVaultRequest(in);
        }

        @Override
        public PayPalVaultRequest[] newArray(int size) {
            return new PayPalVaultRequest[size];
        }
    };
}
