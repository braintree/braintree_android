package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

/**
 * Represents the parameters that are needed to use the Google Pay API.
 */
public class GooglePayRequest implements Parcelable {

    private TransactionInfo transactionInfo;
    private boolean emailRequired;
    private boolean phoneNumberRequired;
    private boolean billingAddressRequired;
    private int billingAddressFormat;
    private boolean shippingAddressRequired;
    private ShippingAddressRequirements shippingAddressRequirements;
    private boolean allowPrepaidCards;
    private boolean payPalEnabled = true;
    private final HashMap<String, JSONObject> allowedPaymentMethods = new HashMap<>();
    private final HashMap<String, JSONObject> tokenizationSpecifications = new HashMap<>();
    private final HashMap<String, JSONArray> allowedAuthMethods = new HashMap<>();
    private final HashMap<String, JSONArray> allowedCardNetworks = new HashMap<>();
    private String environment;

    // NEXT_MAJOR_VERSION: remove googleMerchantId since it is no longer required/included in the
    // Google Pay API documentation
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
    public void setTransactionInfo(@Nullable TransactionInfo transactionInfo) {
        this.transactionInfo = transactionInfo;
    }

    /**
     * Optional.
     *
     * @param emailRequired {@code true} if the buyer's email address is required to be returned, {@code false} otherwise.
     */
    public void setEmailRequired(boolean emailRequired) {
        this.emailRequired = emailRequired;
    }

    /**
     * Optional.
     *
     * @param phoneNumberRequired {@code true} if the buyer's phone number is required to be returned as part of the
     *                            billing address and shipping address, {@code false} otherwise.
     */
    public void setPhoneNumberRequired(boolean phoneNumberRequired) {
        this.phoneNumberRequired = phoneNumberRequired;
    }

    /**
     * Optional.
     *
     * @param billingAddressRequired {@code true} if the buyer's billing address is required to be returned,
     *                               {@code false} otherwise.
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
     *                                {@code false} otherwise.
     */
    public void setShippingAddressRequired(boolean shippingAddressRequired) {
        this.shippingAddressRequired = shippingAddressRequired;
    }

    /**
     * Optional.
     *
     * @param shippingAddressRequirements the shipping address requirements. {@link ShippingAddressRequirements}
     */
    public void setShippingAddressRequirements(@Nullable ShippingAddressRequirements shippingAddressRequirements) {
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
     *
     * @param enablePayPal {@code true} by default. Allows PayPal to be a payment method in Google Pay.
     */
    public void setPayPalEnabled(boolean enablePayPal) {
        payPalEnabled = enablePayPal;
    }

    /**
     * Simple wrapper to assign given parameters to specified paymentMethod
     *
     * @param paymentMethodType The paymentMethod to add to
     * @param parameters        Parameters to assign to the paymentMethod
     */
    public void setAllowedPaymentMethod(@NonNull String paymentMethodType, @NonNull JSONObject parameters) {
        allowedPaymentMethods.put(paymentMethodType, parameters);
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's tokenizationSpecification
     *
     * @param paymentMethodType The paymentMethod to attached tokenizationSpecification parameters to
     * @param parameters        The tokenizationSpecification parameters to attach
     */
    public void setTokenizationSpecificationForType(@NonNull String paymentMethodType, @NonNull JSONObject parameters) {
        tokenizationSpecifications.put(paymentMethodType, parameters);
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's allowedAuthMethods
     *
     * @param paymentMethodType the paymentMethod to attach allowedAuthMethods to
     * @param authMethods       the authMethods to allow the paymentMethodType to transact with
     */
    public void setAllowedAuthMethods(@NonNull String paymentMethodType, @NonNull JSONArray authMethods) {
        allowedAuthMethods.put(paymentMethodType, authMethods);
    }

    /**
     * Simple wrapper to configure the GooglePayRequest's cardNetworks
     *
     * @param paymentMethodType the paymentMethod to attach cardNetworks to
     * @param cardNetworks      the cardNetworks to allow the paymentMethodType to transact with
     */
    public void setAllowedCardNetworks(@NonNull String paymentMethodType, @NonNull JSONArray cardNetworks) {
        allowedCardNetworks.put(paymentMethodType, cardNetworks);
    }

    /**
     * Optional.
     *
     * @deprecated Google Merchant ID is no longer required and will be removed.
     *
     * @param merchantId The merchant ID that Google Pay has provided.
     */
    @Deprecated
    public void setGoogleMerchantId(@Nullable String merchantId) {
        googleMerchantId = merchantId;
    }

    /**
     * Optional.
     *
     * @param merchantName The merchant name that will be presented in Google Pay
     */
    public void setGoogleMerchantName(@Nullable String merchantName) {
        googleMerchantName = merchantName;
    }

    public void setEnvironment(@Nullable String environment) {
        this.environment = "PRODUCTION".equalsIgnoreCase(environment) ? "PRODUCTION" : "TEST";
    }

    /**
     * ISO 3166-1 alpha-2 country code where the transaction is processed. This is required for
     * merchants based in European Economic Area (EEA) countries.
     * <p>
     * NOTE: to support Elo cards, country code must be set to "BR"
     *
     * @param countryCode The country code where the transaction is processed
     */
    public void setCountryCode(@Nullable String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * Assemble all declared parts of a GooglePayRequest to a JSON string
     * for use in making requests against Google
     *
     * @return String
     */
    public String toJson() {
        JSONObject transactionInfoJson = new JSONObject();
        TransactionInfo transactionInfo = getTransactionInfo();
        JSONArray allowedPaymentMethods = new JSONArray();
        JSONObject shippingAddressParameters = new JSONObject();
        ArrayList<String> allowedCountryCodeList;

        if (isShippingAddressRequired()) {
            allowedCountryCodeList = shippingAddressRequirements.getAllowedCountryCodes();

            if (allowedCountryCodeList != null && allowedCountryCodeList.size() > 0) {
                try {
                    shippingAddressParameters.put("allowedCountryCodes", new JSONArray(allowedCountryCodeList));
                } catch (JSONException ignored) {
                }
            }
            try {
                shippingAddressParameters.put("phoneNumberRequired", isPhoneNumberRequired());
            } catch (JSONException ignored) {
            }
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

                if ("CARD".equals(pm.getKey())) {
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
        } catch (JSONException ignored) {
        }

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
        if (billingAddressFormat == WalletConstants.BILLING_ADDRESS_FORMAT_FULL) {
            format = "FULL";
        }
        return format;
    }

    /**
     * Details and the price of the transaction. Required.
     *
     * @return See {@link TransactionInfo}.
     */
    public TransactionInfo getTransactionInfo() {
        return transactionInfo;
    }

    /**
     * @return If the buyer's email address is required to be returned.
     */
    public boolean isEmailRequired() {
        return emailRequired;
    }

    /**
     * @return If the buyer's phone number is required to be returned as part of the
     * billing address and shipping address.
     */
    public boolean isPhoneNumberRequired() {
        return phoneNumberRequired;
    }

    /**
     * @return If the buyer's billing address is required to be returned.
     */
    public boolean isBillingAddressRequired() {
        return billingAddressRequired;
    }

    /**
     * @return If the buyer's billing address is required to be returned.
     */
    @BillingAddressFormat
    public int getBillingAddressFormat() {
        return billingAddressFormat;
    }

    /**
     * @return If the buyer's shipping address is required to be returned.
     */
    public boolean isShippingAddressRequired() {
        return shippingAddressRequired;
    }

    /**
     * @return The shipping address requirements. See {@link ShippingAddressRequirements}.
     */
    @Nullable
    public ShippingAddressRequirements getShippingAddressRequirements() {
        return shippingAddressRequirements;
    }

    /**
     * @return If prepaid cards are allowed.
     */
    public boolean getAllowPrepaidCards() {
        return allowPrepaidCards;
    }

    /**
     * @return If PayPal should be an available payment method in Google Pay.
     */
    public boolean isPayPalEnabled() {
        return payPalEnabled;
    }

    /**
     * @return Allowed payment methods for a given payment method type.
     */
    @Nullable
    public JSONObject getAllowedPaymentMethod(String type) {
        return allowedPaymentMethods.get(type);
    }

    /**
     * @return Tokenization specification for a given payment method type.
     */
    @Nullable
    public JSONObject getTokenizationSpecificationForType(String type) {
        return tokenizationSpecifications.get(type);
    }

    /**
     * @return Allowed authentication methods for a given payment method type.
     */
    @Nullable
    public JSONArray getAllowedAuthMethodsForType(String type) {
        return allowedAuthMethods.get(type);
    }

    /**
     * @return Allowed card networks for a given payment method type.
     */
    @Nullable
    public JSONArray getAllowedCardNetworksForType(String type) {
        return allowedCardNetworks.get(type);
    }

    @Nullable
    public String getEnvironment() {
        return environment;
    }

    /**
     * @deprecated Google Merchant ID is no longer required and will be removed.
     *
     * @return The merchant ID that Google Pay has provided.
     */
    @Deprecated
    @Nullable
    public String getGoogleMerchantId() {
        return googleMerchantId;
    }

    /**
     * @return The merchant name that will be presented in Google Pay.
     */
    @Nullable
    public String getGoogleMerchantName() {
        return googleMerchantName;
    }

    /**
     * @return The country code where the transaction is processed.
     */
    @Nullable
    public String getCountryCode() {
        return countryCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(transactionInfo, flags);
        dest.writeByte((byte) (emailRequired ? 1 : 0));
        dest.writeByte((byte) (phoneNumberRequired ? 1 : 0));
        dest.writeByte((byte) (billingAddressRequired ? 1 : 0));
        dest.writeInt(billingAddressFormat);
        dest.writeByte((byte) (shippingAddressRequired ? 1 : 0));
        dest.writeParcelable(shippingAddressRequirements, flags);
        dest.writeByte((byte) (allowPrepaidCards ? 1 : 0));
        dest.writeByte((byte) (payPalEnabled ? 1 : 0));
        dest.writeString(environment);
        dest.writeString(googleMerchantId);
        dest.writeString(googleMerchantName);
        dest.writeString(countryCode);
    }

    GooglePayRequest(Parcel in) {
        transactionInfo = in.readParcelable(TransactionInfo.class.getClassLoader());
        emailRequired = in.readByte() != 0;
        phoneNumberRequired = in.readByte() != 0;
        billingAddressRequired = in.readByte() != 0;
        billingAddressFormat = in.readInt();
        shippingAddressRequired = in.readByte() != 0;
        shippingAddressRequirements = in.readParcelable(ShippingAddressRequirements.class.getClassLoader());
        allowPrepaidCards = in.readByte() != 0;
        payPalEnabled = in.readByte() != 0;
        environment = in.readString();
        googleMerchantId = in.readString();
        googleMerchantName = in.readString();
        countryCode = in.readString();
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
