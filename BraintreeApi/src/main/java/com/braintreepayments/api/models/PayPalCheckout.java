package com.braintreepayments.api.models;

import java.math.BigDecimal;

/**
 * Represents the parameters that are needed to kick off Checkout with PayPal
 *
 * In the checkout flow, the user is presented with details about the order and only agrees to a
 * single payment. The result is not eligible for being saved in the Vault; however, you will receive
 * shipping information and the user will not be able to revoke the consent.
 */
public class PayPalCheckout {

    private BigDecimal mAmount;
    private String mCurrencyCode;
    private String mLocaleCode;
    private Boolean mEnableShippingAddress;
    private Boolean mAddressOverride;
    private PostalAddress mShippingAddress;

    /**
     * Constructs a description of a PayPal checkout for Single Payment and Billing Agreements.
     *
     * @note This mAmount may differ slight from the transaction mAmount. The exact decline rules
     *        for mismatches between this client-side mAmount and the final mAmount in the Transaction
     *        are determined by the gateway.
     *
     * @param amount The transaction mAmount in currency units (as
     * determined by setCurrencyCode). For example, "1.20" corresponds to one dollar and twenty cents.
     */
    public PayPalCheckout(BigDecimal amount) {
        mAmount = amount;
        mEnableShippingAddress = true;
        mAddressOverride = false;
    }

    /**
     * Constructs a {@link PayPalCheckout} with a null amount.
     */
    public PayPalCheckout() {
        this(null);
    }

    /**
     * The approximate mAmount of the transaction.
     *
     * @param amount The desired mAmount
     */
    public void setAmount(BigDecimal amount) {
        mAmount = amount;
    }

    /**
     * The three character currency code for the mAmount.
     *
     * If unspecified, the currency code will be chosen based on the active merchant account in the client token.
     *
     * @param currencyCode A currency code, such as "USD"
     */
    public void setCurrencyCode(String currencyCode) {
        mCurrencyCode = currencyCode;
    }

    /**
     * Whether to request the mShippingAddress and return it.
     *
     * @param enableShippingAddress Whether to request the mShippingAddress and return it.
     */
    public void setEnableShippingAddress(Boolean enableShippingAddress) {
        mEnableShippingAddress = enableShippingAddress;
    }

    /**
     * Whether to use a custom locale code.
     *
     * @param localeCode Whether to use a custom locale code.
     */
    public void setLocaleCode(String localeCode) {
        mLocaleCode = localeCode;
    }

    /**
     * Whether to use a custom shipping address - be sure to set a mShippingAddress
     *
     * @param addressOverride Whether to use a custom shipping address
     */
    public void setAddressOverride(Boolean addressOverride) {
        mAddressOverride = addressOverride;
    }

    /**
     * A custom shipping address to be used for the checkout flow. Be sure to set mAddressOverride.
     *
     * @param shippingAddress a custom {@link PostalAddress}
     */
    public void setShippingAddress(PostalAddress shippingAddress) {
        mShippingAddress = shippingAddress;
    }

    public BigDecimal getAmount() {
        return mAmount;
    }

    public String getCurrencyCode() {
        return mCurrencyCode;
    }

    public Boolean getEnableShippingAddress() {
        return mEnableShippingAddress;
    }

    public Boolean getAddressOverride() {
        return mAddressOverride;
    }

    public PostalAddress getShippingAddress() {
        return mShippingAddress;
    }

    public String getLocaleCode() {
        return mLocaleCode;
    }
}
