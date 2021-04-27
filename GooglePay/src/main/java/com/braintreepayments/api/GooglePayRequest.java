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

    private TransactionInfo transactionInfo;
    private Boolean emailRequired = null;
    private Boolean phoneNumberRequired = null;
    private Boolean billingAddressRequired = null;
    private Integer billingAddressFormat;
    private Boolean shippingAddressRequired = null;
    private ShippingAddressRequirements shippingAddressRequirements;
    private Boolean allowPrepaidCards = null;
    private boolean payPalEnabled = true;
    private HashMap<String, JSONObject> allowedPaymentMethods = new HashMap<>();
    private HashMap<String, JSONObject> tokenizationSpecifications = new HashMap<>();
    private HashMap<String, JSONArray> allowedAuthMethods = new HashMap<>();
    private HashMap<String, JSONArray> allowedCardNetworks = new HashMap<>();
    private String environment;
    private String googleMerchantId;
    private String googleMerchantName;
    private String countryCode;

    public GooglePayRequest() {
    }

    /**
     * Details and the price of the transaction. Required.
     *
     * @param transactionInfo See {@link TransactionInfo}.
     */
    public void setTransactionInfo(TransactionInfo transactionInfo) {
        this.transactionInfo = transactionInfo;
    }

    /**
     * Optional.
     *
     * @param emailRequired {@code true} if the buyer's email address is required to be returned, {@code false} otherwise.
     * @return {@link GooglePayRequest}
     */
    public void setEmailRequired(boolean emailRequired) {
        this.emailRequired = emailRequired;
    }

    /**
     * Optional.
     *
     * @param phoneNumberRequired {@code true} if the buyer's phone number is required to be returned as part of the
     * billing address and shipping address, {@code false} otherwise.
     */
    public void setPhoneNumberRequired(boolean phoneNumberRequired) {
        this.phoneNumberRequired = phoneNumberRequired;
    }

    /**
     * Optional.
     *
     * @param billingAddressRequired {@code true} if the buyer's billing address is required to be returned,
     * {@code false} otherwise.
     */
    public void setBillingAddressRequired(boolean billingAddressRequired) {
        this.billingAddressRequired = billingAddressRequired;
    }

    /**
     * Optional.
     *
     * @param billingAddressFormat the billing address format to return. {@link BillingAddressFormat}
     */
    public void setBillingAddressFormat(@BillingAddressFormat int billingAddressFormat) {
        this.billingAddressFormat = billingAddressFormat;
    }

    /**
     * Optional.
     *
     * @param shippingAddressRequired {@code true} if the buyer's shipping address is required to be returned,
     * {@code false} otherwise.
     */
    public void setShippingAddressRequired(boolean shippingAddressRequired) {
        this.shippingAddressRequired = shippingAddressRequired;
    }

    /**
     * Optional.
     *
     * @param shippingAddressRequirements the shipping address requirements. {@link ShippingAddressRequirements}
     */
    public void setShippingAddressRequirements(ShippingAddressRequirements shippingAddressRequirements) {
        this.shippingAddressRequirements = shippingAddressRequirements;
    }

    /**
     * Optional.
     *
     * @param allowPrepaidCards {@code true} prepaid cards are allowed, {@code false} otherwise.
     */
    public void setAllowPrepaidCards(boolean allowPrepaidCards) {
        this.allowPrepaidCards = allowPrepaidCards;
    }

    /**
     * Defines if PayPal should be an available payment method in Google Pay.
     * Defaults to {@code true}.
     * @param enablePayPal {@code true} by default. Allows PayPal to be a payment method in Google Pay.
     */
    public void setPayPalEnabled(boolean enablePayPal) {
        payPalEnabled = enablePayPal;
    }

    /**
     * Simple wrapper to assign given parameters to specified paymentMethod
     * @param paymentMethodType The paymentMethod to add to
     * @param parameters Parameters to assign to the paymentMethod
     */
    public void setAllowedPaymentMethod(String paymentMethodType, JSONObject parameters) {
        allowedPaymentMethods.put(paymentMethodType, parameters);
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's tokenizationSpecification
     * @param paymentMethodType The paymentMethod to attached tokenizationSpecification parameters to
     * @param parameters The tokenizationSpecification parameters to attach
     */
    public void setTokenizationSpecificationForType(String paymentMethodType, JSONObject parameters) {
        tokenizationSpecifications.put(paymentMethodType, parameters);
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's allowedAuthMethods
     * @param paymentMethodType the paymentMethod to attach allowedAuthMethods to
     * @param authMethods the authMethods to allow the paymentMethodType to transact with
     */
    public void setAllowedAuthMethods(String paymentMethodType, JSONArray authMethods) {
        allowedAuthMethods.put(paymentMethodType, authMethods);
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's cardNetworks
     * @param paymentMethodType the paymentMethod to attach cardNetworks to
     * @param cardNetworks the cardNetworks to allow the paymentMethodType to transact with
     */
    public void setAllowedCardNetworks(String paymentMethodType, JSONArray cardNetworks) {
        allowedCardNetworks.put(paymentMethodType, cardNetworks);
    }

    /**
     * @param merchantId The merchant ID that Google Pay has provided.
     */
    public void setGoogleMerchantId(String merchantId) {
        googleMerchantId = merchantId;
    }

    /**
     * @param merchantName The merchant name that will be presented in Google Pay
     */
    public void setGoogleMerchantName(String merchantName) {
        googleMerchantName = merchantName;
    }

    public void setEnvironment(String environment) {
        this.environment = "PRODUCTION".equals(environment.toUpperCase()) ? "PRODUCTION" : "TEST";
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
        this.countryCode = countryCode;
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
            allowedCountryCodeList = shippingAddressRequirements.getAllowedCountryCodes();

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

            if (countryCode != null) {
                transactionInfoJson.put("countryCode", countryCode);
            }

        } catch (JSONException ignored) {
        }

        for (Map.Entry<String, JSONObject> pm : this.allowedPaymentMethods.entrySet()) {
            try {
                JSONObject paymentMethod = new JSONObject()
                        .put("type", pm.getKey())
                        .put("parameters", pm.getValue())
                        .put("tokenizationSpecification", tokenizationSpecifications.get(pm.getKey()));

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
                    .put("environment", environment)
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
        if (billingAddressFormat != null &&
                billingAddressFormat == WalletConstants.BILLING_ADDRESS_FORMAT_FULL) {
            format = "FULL";
        }
        return format;
    }

    public TransactionInfo getTransactionInfo() {
        return transactionInfo;
    }

    @Nullable
    public Boolean isEmailRequired() {
        return emailRequired;
    }

    @Nullable
    public Boolean isPhoneNumberRequired() {
        return phoneNumberRequired;
    }

    @Nullable
    public Boolean isBillingAddressRequired() {
        return billingAddressRequired;
    }

    @Nullable
    @BillingAddressFormat
    public Integer getBillingAddressFormat() {
        return billingAddressFormat;
    }

    @Nullable
    public Boolean isShippingAddressRequired() {
        return shippingAddressRequired;
    }

    @Nullable
    public ShippingAddressRequirements getShippingAddressRequirements() {
        return shippingAddressRequirements;
    }

    @Nullable
    public Boolean getAllowPrepaidCards() {
        return allowPrepaidCards;
    }

    public Boolean isPayPalEnabled() {
        return payPalEnabled;
    }

    public JSONObject getAllowedPaymentMethod(String type) {
        return allowedPaymentMethods.get(type);
    }

    public JSONObject getTokenizationSpecificationForType(String type) {
        return tokenizationSpecifications.get(type);
    }

    public JSONArray getAllowedAuthMethodsForType(String type) {
        return allowedAuthMethods.get(type);
    }

    public JSONArray getAllowedCardNetworksForType(String type) {
        return allowedCardNetworks.get(type);
    }

    public String getEnvironment() {
        return environment;
    }

    public String getGoogleMerchantId() {
        return googleMerchantId;
    }

    public String getGoogleMerchantName() {
        return googleMerchantName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(transactionInfo, flags);
        dest.writeByte((byte) (emailRequired == null ? 0 : emailRequired ? 1 : 2));
        dest.writeByte((byte) (phoneNumberRequired == null ? 0 : phoneNumberRequired ? 1 : 2));
        dest.writeByte((byte) (billingAddressRequired == null ? 0 : billingAddressRequired ? 1 : 2));
        if (billingAddressFormat == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(billingAddressFormat);
        }
        dest.writeByte((byte) (shippingAddressRequired == null ? 0 : shippingAddressRequired ? 1 : 2));
        dest.writeParcelable(shippingAddressRequirements, flags);
        dest.writeByte((byte) (allowPrepaidCards == null ? 0 : allowPrepaidCards ? 1 : 2));
        dest.writeString(environment);
        dest.writeString(googleMerchantId);
        dest.writeString(googleMerchantName);
    }

    protected GooglePayRequest(Parcel in) {
        transactionInfo = in.readParcelable(TransactionInfo.class.getClassLoader());
        byte emailRequired = in.readByte();
        this.emailRequired = emailRequired == 0 ? null : emailRequired == 1;
        byte phoneNumberRequired = in.readByte();
        this.phoneNumberRequired = phoneNumberRequired == 0 ? null : phoneNumberRequired == 1;
        byte billingAddressRequired = in.readByte();
        this.billingAddressRequired = billingAddressRequired == 0 ? null : billingAddressRequired == 1;
        if (in.readByte() == 0) {
            billingAddressFormat = null;
        } else {
            billingAddressFormat = in.readInt();
        }
        byte shippingAddressRequired = in.readByte();
        this.shippingAddressRequired = shippingAddressRequired == 0 ? null : shippingAddressRequired == 1;
        shippingAddressRequirements = in.readParcelable(ShippingAddressRequirements.class.getClassLoader());
        byte allowPrepaidCards = in.readByte();
        this.allowPrepaidCards = allowPrepaidCards == 0 ? null : allowPrepaidCards == 1;
        environment = in.readString();
        googleMerchantId = in.readString();
        googleMerchantName = in.readString();
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
