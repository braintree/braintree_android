package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the parameters that are needed to start the PayPal Vault flow
 */
public class PayPalNativeCheckoutVaultRequest extends PayPalNativeRequest implements Parcelable {

    private boolean shouldOfferCredit;

    public PayPalNativeCheckoutVaultRequest() {
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

    String createRequestBody(Configuration configuration, Authorization authorization, String successUrl, String cancelUrl) throws JSONException {
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

        JSONObject experienceProfile = new JSONObject();
        experienceProfile.put(NO_SHIPPING_KEY, !isShippingAddressRequired());
        String displayName = getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = configuration.getPayPalDisplayName();
        }
        experienceProfile.put(DISPLAY_NAME_KEY, displayName);

        if (getLocaleCode() != null) {
            experienceProfile.put(LOCALE_CODE_KEY, getLocaleCode());
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

    PayPalNativeCheckoutVaultRequest(Parcel in) {
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

    public static final Creator<PayPalNativeCheckoutVaultRequest> CREATOR = new Creator<PayPalNativeCheckoutVaultRequest>() {
        @Override
        public PayPalNativeCheckoutVaultRequest createFromParcel(Parcel in) {
            return new PayPalNativeCheckoutVaultRequest(in);
        }

        @Override
        public PayPalNativeCheckoutVaultRequest[] newArray(int size) {
            return new PayPalNativeCheckoutVaultRequest[size];
        }
    };
}
