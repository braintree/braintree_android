package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.WalletConstants.BillingAddressFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;

/**
 * Represents the parameters that are needed to use the Google Pay API.
 */
public class GooglePayRequest implements Parcelable {

    private TransactionInfo mTransactionInfo;
    private Boolean mEmailRequired = null;
    private Boolean mPhoneNumberRequired = null;
    private Boolean mBillingAddressRequired = null;
    private Integer mBillingAddressFormat;
    private Boolean mShippingAddressRequired = null;
    private ShippingAddressRequirements mShippingAddressRequirements;
    private Boolean mAllowPrepaidCards = null;
    private boolean mPayPalEnabled = true;
    private HashMap<String, JSONObject> mAllowedPaymentMethods = new HashMap<>();
    private HashMap<String, JSONObject> mTokenizationSpecifications = new HashMap<>();
    private HashMap<String, JSONArray> mAllowedAuthMethods = new HashMap<>();
    private HashMap<String, JSONArray> mAllowedCardNetworks = new HashMap<>();
    private String mEnvironment;
    private String mGoogleMerchantId;
    private String mGoogleMerchantName;
    private String mCountryCode;

    public GooglePayRequest() {
    }

    /**
     * Details and the price of the transaction. Required.
     *
     * @param transactionInfo See {@link TransactionInfo}.
     */
    public void setTransactionInfo(TransactionInfo transactionInfo) {
        mTransactionInfo = transactionInfo;
    }

    /**
     * Optional.
     *
     * @param emailRequired {@code true} if the buyer's email address is required to be returned, {@code false} otherwise.
     * @return {@link GooglePayRequest}
     */
    public void setEmailRequired(boolean emailRequired) {
        mEmailRequired = emailRequired;
    }

    /**
     * Optional.
     *
     * @param phoneNumberRequired {@code true} if the buyer's phone number is required to be returned as part of the
     * billing address and shipping address, {@code false} otherwise.
     */
    public void setPhoneNumberRequired(boolean phoneNumberRequired) {
        mPhoneNumberRequired = phoneNumberRequired;
    }

    /**
     * Optional.
     *
     * @param billingAddressRequired {@code true} if the buyer's billing address is required to be returned,
     * {@code false} otherwise.
     */
    public void setBillingAddressRequired(boolean billingAddressRequired) {
        mBillingAddressRequired = billingAddressRequired;
    }

    /**
     * Optional.
     *
     * @param billingAddressFormat the billing address format to return. {@link BillingAddressFormat}
     */
    public void setBillingAddressFormat(@BillingAddressFormat int billingAddressFormat) {
        mBillingAddressFormat = billingAddressFormat;
    }

    /**
     * Optional.
     *
     * @param shippingAddressRequired {@code true} if the buyer's shipping address is required to be returned,
     * {@code false} otherwise.
     */
    public void setShippingAddressRequired(boolean shippingAddressRequired) {
        mShippingAddressRequired = shippingAddressRequired;
    }

    /**
     * Optional.
     *
     * @param shippingAddressRequirements the shipping address requirements. {@link ShippingAddressRequirements}
     */
    public void setShippingAddressRequirements(ShippingAddressRequirements shippingAddressRequirements) {
        mShippingAddressRequirements = shippingAddressRequirements;
    }

    /**
     * Optional.
     *
     * @param allowPrepaidCards {@code true} prepaid cards are allowed, {@code false} otherwise.
     */
    public void setAllowPrepaidCards(boolean allowPrepaidCards) {
        mAllowPrepaidCards = allowPrepaidCards;
    }

    /**
     * Defines if PayPal should be an available payment method in Google Pay.
     * Defaults to {@code true}.
     * @param enablePayPal {@code true} by default. Allows PayPal to be a payment method in Google Pay.
     */
    public void setPayPalEnabled(boolean enablePayPal) {
        mPayPalEnabled = enablePayPal;
    }

    /**
     * Simple wrapper to assign given parameters to specified paymentMethod
     * @param paymentMethodType The paymentMethod to add to
     * @param parameters Parameters to assign to the paymentMethod
     */
    public void setAllowedPaymentMethod(String paymentMethodType, JSONObject parameters) {
        mAllowedPaymentMethods.put(paymentMethodType, parameters);
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's tokenizationSpecification
     * @param paymentMethodType The paymentMethod to attached tokenizationSpecification parameters to
     * @param parameters The tokenizationSpecification parameters to attach
     */
    public void setTokenizationSpecificationForType(String paymentMethodType, JSONObject parameters) {
        mTokenizationSpecifications.put(paymentMethodType, parameters);
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's allowedAuthMethods
     * @param paymentMethodType the paymentMethod to attach allowedAuthMethods to
     * @param authMethods the authMethods to allow the paymentMethodType to transact with
     */
    public void setAllowedAuthMethods(String paymentMethodType, JSONArray authMethods) {
        mAllowedAuthMethods.put(paymentMethodType, authMethods);
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's cardNetworks
     * @param paymentMethodType the paymentMethod to attach cardNetworks to
     * @param cardNetworks the cardNetworks to allow the paymentMethodType to transact with
     */
    public void setAllowedCardNetworks(String paymentMethodType, JSONArray cardNetworks) {
        mAllowedCardNetworks.put(paymentMethodType, cardNetworks);
    }

    /**
     * @param merchantId The merchant ID that Google Pay has provided.
     */
    public void setGoogleMerchantId(String merchantId) {
        mGoogleMerchantId = merchantId;
    }

    /**
     * @param merchantName The merchant name that will be presented in Google Pay
     */
    public void setGoogleMerchantName(String merchantName) {
        mGoogleMerchantName = merchantName;
    }

    public void setEnvironment(String environment) {
        mEnvironment = "PRODUCTION".equals(environment.toUpperCase()) ? "PRODUCTION" : "TEST";
    }

    /**
     * ISO 3166-1 alpha-2 country code where the transaction is processed. This is required for
     * merchants based in European Economic Area (EEA) countries.
     *
     * NOTE: to support Elo cards, country code must be set to "BR"
     *
     * @param countryCode
     */
    public void setCountryCode(String countryCode) {
        mCountryCode = countryCode;
    }

    /**
     * Assemble all declared parts of a GooglePayRequest to a JSON string
     * for use in making requests against Google
     * @return String
     */
    public String toJson() {
        JSONObject transactionInfoJson = new JSONObject();
        TransactionInfo transactionInfo = getTransactionInfo();
        JSONArray allowedPaymentMethods = new JSONArray();
        JSONObject shippingAddressParameters = new JSONObject();
        JSONArray allowedCountryCodes = new JSONArray();
        ArrayList<String> allowedCountryCodeList;

        if (isShippingAddressRequired()) {
            allowedCountryCodeList = mShippingAddressRequirements.getAllowedCountryCodes();

            if (allowedCountryCodeList != null && allowedCountryCodeList.size() > 0) {
                try {
                    shippingAddressParameters.put("allowedCountryCodes", new JSONArray(allowedCountryCodeList));
                } catch (JSONException ignored) { }
            }
            try {
                shippingAddressParameters.put("phoneNumberRequired", isPhoneNumberRequired());
            } catch (JSONException ignored) { }
        }

        try {
            String totalPriceStatus = totalPriceStatusToString();
            transactionInfoJson
                    .put("totalPriceStatus", totalPriceStatus)
                    .put("totalPrice", transactionInfo.getTotalPrice())
                    .put("currencyCode", transactionInfo.getCurrencyCode());

            if (mCountryCode != null) {
                transactionInfoJson.put("countryCode", mCountryCode);
            }

        } catch (JSONException ignored) {
        }

        for (Map.Entry<String, JSONObject> pm : mAllowedPaymentMethods.entrySet()) {
            try {
                JSONObject paymentMethod = new JSONObject()
                        .put("type", pm.getKey())
                        .put("parameters", pm.getValue())
                        .put("tokenizationSpecification", mTokenizationSpecifications.get(pm.getKey()));

                if (pm.getKey() == "CARD") {
                    try {
                        pm.getValue().get("billingAddressParameters");
                    } catch (JSONException ignored) {
                        JSONObject paymentMethodParams = paymentMethod.getJSONObject("parameters");
                        paymentMethodParams
                                .put("billingAddressRequired", isBillingAddressRequired())
                                .put("allowPrepaidCards", getAllowPrepaidCards());

                        if (isBillingAddressRequired()) {
                            paymentMethodParams
                                    .put("billingAddressParameters", new JSONObject()
                                            .put("format", billingAddressFormatToString())
                                            .put("phoneNumberRequired", isPhoneNumberRequired()));
                        }
                    }
                }

                allowedPaymentMethods.put(paymentMethod);
            } catch (JSONException ignored) {
            }
        }

        JSONObject merchantInfo = new JSONObject();

        try {
            if (!TextUtils.isEmpty(getGoogleMerchantId())) {
                merchantInfo.put("merchantId", getGoogleMerchantId());
            }

            if (!TextUtils.isEmpty(getGoogleMerchantName())) {
                merchantInfo.put("merchantName", getGoogleMerchantName());
            }
        } catch (JSONException ignored) {}

        JSONObject json = new JSONObject();

        try {
            json
                    .put("apiVersion", 2)
                    .put("apiVersionMinor", 0)
                    .put("allowedPaymentMethods", allowedPaymentMethods)
                    .put("emailRequired", isEmailRequired())
                    .put("shippingAddressRequired", isShippingAddressRequired())
                    .put("environment", mEnvironment)
                    .put("merchantInfo", merchantInfo)
                    .put("transactionInfo", transactionInfoJson);

            if (isShippingAddressRequired()) {
                json.put("shippingAddressParameters", shippingAddressParameters);
            }
        } catch (JSONException ignored) {
        }

        return json.toString();
    }

    private String totalPriceStatusToString() {
        switch (getTransactionInfo().getTotalPriceStatus()) {
            case WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN:
                return "NOT_CURRENTLY_KNOWN";
            case WalletConstants.TOTAL_PRICE_STATUS_ESTIMATED:
                return "ESTIMATED";
            case WalletConstants.TOTAL_PRICE_STATUS_FINAL:
            default:
                return "FINAL";
        }
    }

    public String billingAddressFormatToString() {
        String format = "MIN";
        if (mBillingAddressFormat != null &&
                mBillingAddressFormat == WalletConstants.BILLING_ADDRESS_FORMAT_FULL) {
            format = "FULL";
        }
        return format;
    }

    public TransactionInfo getTransactionInfo() {
        return mTransactionInfo;
    }

    @Nullable
    public Boolean isEmailRequired() {
        return mEmailRequired;
    }

    @Nullable
    public Boolean isPhoneNumberRequired() {
        return mPhoneNumberRequired;
    }

    @Nullable
    public Boolean isBillingAddressRequired() {
        return mBillingAddressRequired;
    }

    @Nullable
    @BillingAddressFormat
    public Integer getBillingAddressFormat() {
        return mBillingAddressFormat;
    }

    @Nullable
    public Boolean isShippingAddressRequired() {
        return mShippingAddressRequired;
    }

    @Nullable
    public ShippingAddressRequirements getShippingAddressRequirements() {
        return mShippingAddressRequirements;
    }

    @Nullable
    public Boolean getAllowPrepaidCards() {
        return mAllowPrepaidCards;
    }

    public Boolean isPayPalEnabled() {
        return mPayPalEnabled;
    }

    public JSONObject getAllowedPaymentMethod(String type) {
        return mAllowedPaymentMethods.get(type);
    }

    public JSONObject getTokenizationSpecificationForType(String type) {
        return mTokenizationSpecifications.get(type);
    }

    public JSONArray getAllowedAuthMethodsForType(String type) {
        return mAllowedAuthMethods.get(type);
    }

    public JSONArray getAllowedCardNetworksForType(String type) {
        return mAllowedCardNetworks.get(type);
    }

    public String getEnvironment() {
        return mEnvironment;
    }

    public String getGoogleMerchantId() {
        return mGoogleMerchantId;
    }

    public String getGoogleMerchantName() {
        return mGoogleMerchantName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mTransactionInfo, flags);
        dest.writeByte((byte) (mEmailRequired == null ? 0 : mEmailRequired ? 1 : 2));
        dest.writeByte((byte) (mPhoneNumberRequired == null ? 0 : mPhoneNumberRequired ? 1 : 2));
        dest.writeByte((byte) (mBillingAddressRequired == null ? 0 : mBillingAddressRequired ? 1 : 2));
        if (mBillingAddressFormat == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(mBillingAddressFormat);
        }
        dest.writeByte((byte) (mShippingAddressRequired == null ? 0 : mShippingAddressRequired ? 1 : 2));
        dest.writeParcelable(mShippingAddressRequirements, flags);
        dest.writeByte((byte) (mAllowPrepaidCards == null ? 0 : mAllowPrepaidCards ? 1 : 2));
        dest.writeString(mEnvironment);
        dest.writeString(mGoogleMerchantId);
        dest.writeString(mGoogleMerchantName);
    }

    protected GooglePayRequest(Parcel in) {
        mTransactionInfo = in.readParcelable(TransactionInfo.class.getClassLoader());
        byte emailRequired = in.readByte();
        mEmailRequired = emailRequired == 0 ? null : emailRequired == 1;
        byte phoneNumberRequired = in.readByte();
        mPhoneNumberRequired = phoneNumberRequired == 0 ? null : phoneNumberRequired == 1;
        byte billingAddressRequired = in.readByte();
        mBillingAddressRequired = billingAddressRequired == 0 ? null : billingAddressRequired == 1;
        if (in.readByte() == 0) {
            mBillingAddressFormat = null;
        } else {
            mBillingAddressFormat = in.readInt();
        }
        byte shippingAddressRequired = in.readByte();
        mShippingAddressRequired = shippingAddressRequired == 0 ? null : shippingAddressRequired == 1;
        mShippingAddressRequirements = in.readParcelable(ShippingAddressRequirements.class.getClassLoader());
        byte allowPrepaidCards = in.readByte();
        mAllowPrepaidCards = allowPrepaidCards == 0 ? null : allowPrepaidCards == 1;
        mEnvironment = in.readString();
        mGoogleMerchantId = in.readString();
        mGoogleMerchantName = in.readString();
    }

    public static final Creator<GooglePayRequest> CREATOR = new Creator<GooglePayRequest>() {
        @Override
        public GooglePayRequest createFromParcel(Parcel in) {
            return new GooglePayRequest(in);
        }

        @Override
        public GooglePayRequest[] newArray(int size) {
            return new GooglePayRequest[size];
        }
    };
}
