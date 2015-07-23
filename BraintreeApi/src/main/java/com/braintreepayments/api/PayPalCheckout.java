package com.braintreepayments.api;

import com.braintreepayments.api.models.PostalAddress;

import java.math.BigDecimal;

/**
 * Represents the parameters that are needed to kick off Checkout with PayPal
 *
 * In the checkout flow, the user is presented with details about the order and only agrees to a
 * single payment. The result is not eligible for being saved in the Vault; however, you will receive
 * shipping information and the user will not be able to revoke the consent.
 */
public class PayPalCheckout {

    private BigDecimal amount;
    private String currencyCode;
    private String localeCode;
    private Boolean enableShippingAddress;
    private Boolean addressOverride;
    private PostalAddress shippingAddress;

    /**
     * Constructs a description of a PayPal checkout for passing into {@link com.braintreepayments.api.Braintree#startCheckoutWithPayPal(android.app.Activity, int, PayPalCheckout)} The amount to present to the user for approval.
     *
     * @note This amount may differ slight from the transaction amount. The exact decline rules
     *        for mismatches between this client-side amount and the final amount in the Transaction
     *        are determined by the gateway.
     *
     * @param amount The transaction amount in currency units (as
     * determined by setCurrencyCode). For example, "1.20" corresponds to one dollar and twenty cents.
     */
    public PayPalCheckout(BigDecimal amount) {
        this.amount = amount;
        this.enableShippingAddress = true;
        this.addressOverride = false;
    }

    /**
     * The approximate amount of the transaction.
     *
     * @param amount The desired amount
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * The three character currency code for the amount.
     *
     * If unspecified, the currency code will be chosen based on the active merchant account in the client token.
     *
     * @param currencyCode A currency code, such as "USD"
     */
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /**
     * Whether to request the shippingAddress and return it.
     *
     * @param enableShippingAddress Whether to request the shippingAddress and return it.
     */
    public void setEnableShippingAddress(Boolean enableShippingAddress) {
        this.enableShippingAddress = enableShippingAddress;
    }

    /**
     * Whether to use a custom locale code.
     *
     * @param localeCode Whether to use a custom locale code.
     */
    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    /**
     * Whether to use a custom shipping address - be sure to set a shippingAddress
     *
     * @param addressOverride Whether to use a custom shipping address
     */
    public void setAddressOverride(Boolean addressOverride) {
        this.addressOverride = addressOverride;
    }

    /**
     * A custom shipping address to be used for the checkout flow. Be sure to set addressOverride.
     *
     * @param shippingAddress a custom {@link PostalAddress}
     */
    public void setShippingAddress(PostalAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public BigDecimal getAmount() { return this.amount; }
    public String getCurrencyCode() { return this.currencyCode; }
    public Boolean getEnableShippingAddress() { return this.enableShippingAddress; }
    public Boolean getAddressOverride() { return addressOverride; }
    public PostalAddress getShippingAddress() { return shippingAddress; }
    public String getLocaleCode() { return localeCode; }
}
