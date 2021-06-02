package com.braintreepayments.api;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builder used to construct an local payment request.
 */
public class LocalPaymentRequest {
    private static final String INTENT_KEY = "intent";
    private static final String RETURN_URL_KEY = "returnUrl";
    private static final String CANCEL_URL_KEY = "cancelUrl";
    private static final String EXPERIENCE_PROFILE_KEY = "experienceProfile";
    private static final String NO_SHIPPING_KEY = "noShipping";
    private static final String FUNDING_SOURCE_KEY = "fundingSource";
    private static final String AMOUNT_KEY = "amount";
    private static final String CURRENCY_CODE_KEY = "currencyIsoCode";
    private static final String GIVEN_NAME_KEY = "firstName";
    private static final String SURNAME_KEY = "lastName";
    private static final String EMAIL_KEY = "payerEmail";
    private static final String PHONE_KEY = "phone";
    private static final String STREET_ADDRESS_KEY = "line1";
    private static final String EXTENDED_ADDRESS_KEY = "line2";
    private static final String LOCALITY_KEY = "city";
    private static final String REGION_KEY = "state";
    private static final String POSTAL_CODE_KEY = "postalCode";
    private static final String COUNTRY_CODE_KEY = "countryCode";
    private static final String MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId";
    private static final String PAYMENT_TYPE_COUNTRY_CODE_KEY = "paymentTypeCountryCode";
    private static final String BIC_KEY = "bic";

    private PostalAddress address;
    private String amount;
    private String currencyCode;
    private String email;
    private String givenName;
    private String merchantAccountId;
    private String paymentType;
    private String paymentTypeCountryCode;
    private String phone;
    private boolean shippingAddressRequired;
    private String surname;
    private String bankIdentificationCode;

    /**
     * @param address Optional - The address of the customer. An error will occur if this address is not valid.
     */
    public void setAddress(@NonNull PostalAddress address) {
        this.address = address;
    }

    /**
     * @param amount Optional - The amount for the transaction.
     */
    public void setAmount(@NonNull String amount) {
        this.amount = amount;
    }

    /**
     * @param bankIdentificationCode Optional - the Bank Identification Code of the customer (specific to iDEAL transactions).
     */
    public void setBic(@NonNull String bankIdentificationCode) {
        this.bankIdentificationCode = bankIdentificationCode;
    }


    /**
     * @param currencyCode Optional - A valid ISO currency code to use for the transaction. Defaults to merchant
     * currency code if not set.
     */
    public void setCurrencyCode(@NonNull String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /**
     * @param email Optional - Payer email of the customer.
     */
    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    /**
     * @param givenName Optional - Given (first) name of the customer.
     */
    public void setGivenName(@NonNull String givenName) {
        this.givenName = givenName;
    }

    /**
     * @param merchantAccountId Optional - A non-default merchant account to use for tokenization.
     */
    public void setMerchantAccountId(@NonNull String merchantAccountId) {
        this.merchantAccountId = merchantAccountId;
    }

    /**
     * @param paymentType - The type of payment
     */
    public void setPaymentType(@NonNull String paymentType) {
        this.paymentType = paymentType;
    }

    /**
     * @param paymentTypeCountryCode The country code of the local payment. This value must be one of
     *                               the supported country codes for a given local payment type listed.
     *                               For local payments supported in multiple countries, this value
     *                               may determine which banks are presented to the customer.
     *                               @see <a href=https://developers.braintreepayments.com/guides/local-payment-methods/client-side-custom/android/v3#invoke-payment-flow>Supported Country Codes</a>
     */
    public void setPaymentTypeCountryCode(@NonNull String paymentTypeCountryCode) {
        this.paymentTypeCountryCode = paymentTypeCountryCode;
    }

    /**
     * @param phone Optional - Phone number of the customer.
     */
    public void setPhone(@NonNull String phone) {
        this.phone = phone;
    }

    /**
     * @param shippingAddressRequired - Indicates whether or not the payment needs to be shipped. For digital goods,
     *                                this should be false. Defaults to false.
     */
    public void setShippingAddressRequired(boolean shippingAddressRequired) {
        this.shippingAddressRequired = shippingAddressRequired;
    }

    /**
     * @param surname Optional - Surname (last name) of the customer.
     */
    public void setSurname(@NonNull String surname) {
        this.surname = surname;
    }

    public PostalAddress getAddress() {
        return address;
    }

    public String getAmount() {
        return amount;
    }

    public String getBic() {
        return bankIdentificationCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getEmail() {
        return email;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getMerchantAccountId() {
        return merchantAccountId;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public String getPaymentTypeCountryCode() {
        return paymentTypeCountryCode;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isShippingAddressRequired() {
        return shippingAddressRequired;
    }

    public String getSurname() {
        return surname;
    }

    public String build(String returnUrl, String cancelUrl) {
        try {
            JSONObject payload = new JSONObject()
                    .put(INTENT_KEY, "sale")
                    .put(RETURN_URL_KEY, returnUrl)
                    .put(CANCEL_URL_KEY, cancelUrl)
                    .put(FUNDING_SOURCE_KEY, paymentType)
                    .put(AMOUNT_KEY, amount)
                    .put(CURRENCY_CODE_KEY, currencyCode)
                    .put(GIVEN_NAME_KEY, givenName)
                    .put(SURNAME_KEY, surname)
                    .put(EMAIL_KEY, email)
                    .put(PHONE_KEY, phone)
                    .put(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId)
                    .putOpt(PAYMENT_TYPE_COUNTRY_CODE_KEY, paymentTypeCountryCode)
                    .putOpt(BIC_KEY, bankIdentificationCode);

            if (address != null) {
                payload.put(STREET_ADDRESS_KEY, address.getStreetAddress())
                        .put(EXTENDED_ADDRESS_KEY, address.getExtendedAddress())
                        .put(LOCALITY_KEY, address.getLocality())
                        .put(REGION_KEY, address.getRegion())
                        .put(POSTAL_CODE_KEY, address.getPostalCode())
                        .put(COUNTRY_CODE_KEY, address.getCountryCodeAlpha2());
            }

            JSONObject experienceProfile = new JSONObject();
            experienceProfile.put(NO_SHIPPING_KEY, !shippingAddressRequired);
            payload.put(EXPERIENCE_PROFILE_KEY, experienceProfile);

            return payload.toString();
        } catch (JSONException ignored) {}

        return new JSONObject().toString();
    }
}
