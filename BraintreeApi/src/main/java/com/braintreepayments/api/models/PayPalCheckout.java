package com.braintreepayments.api.models;

/**
 * Represents the parameters that are needed to kick off Checkout with PayPal
 *
 * In the checkout flow, the user is presented with details about the order and only agrees to a
 * single payment. The result is not eligible for being saved in the Vault; however, you will receive
 * shipping information and the user will not be able to revoke the consent.
 *
 * @see <a href="https://developer.paypal.com/docs/api/#inputfields-object">PayPal REST API Reference</a>
 */
public class PayPalCheckout {

    private String mAmount;
    private String mCurrencyCode;
    private String mLocaleCode;
    private boolean mNoShipping;
    private PostalAddress mShippingAddress;

    /**
     * Constructs a description of a PayPal checkout for Single Payment and Billing Agreements.
     *
     * @note This amount may differ slight from the transaction amount. The exact decline rules
     *        for mismatches between this client-side amount and the final amount in the Transaction
     *        are determined by the gateway.
     *
     * @param amount The transaction amount in currency units (as
     * determined by setCurrencyCode). For example, "1.20" corresponds to one dollar and twenty cents.
     * Amount must be a non-negative number, may optionally contain exactly 2 decimal places separated
     * by '.', optional thousands separator ',', limited to 7 digits before the decimal point.
     */
    public PayPalCheckout(String amount) {
        mAmount = amount;
        mNoShipping = false;
    }

    /**
     * Constructs a {@link PayPalCheckout} with a null amount.
     */
    public PayPalCheckout() {
        this(null);
    }

    /**
     * Optional: A valid ISO currency code to use for the transaction. Defaults to merchant currency
     * code if not set.
     *
     * If unspecified, the currency code will be chosen based on the active merchant account in the
     * client token.
     *
     * @param currencyCode A currency code, such as "USD"
     */
    public PayPalCheckout currencyCode(String currencyCode) {
        mCurrencyCode = currencyCode;
        return this;
    }

    /**
     * Defaults to false. When set to true, the shipping address selector will not be displayed.
     *
     * @param noShipping Whether to hide the shipping address in the flow.
     */
    public PayPalCheckout noShipping(boolean noShipping) {
        mNoShipping = noShipping;
        return this;
    }

    /**
     * Whether to use a custom locale code.
     *
     * @param localeCode Whether to use a custom locale code.
     */
    public PayPalCheckout localeCode(String localeCode) {
        mLocaleCode = localeCode;
        return this;
    }

    /**
     * A custom shipping address to be used for the checkout flow.
     *
     * @param shippingAddress a custom {@link PostalAddress}
     */
    public PayPalCheckout shippingAddress(PostalAddress shippingAddress) {
        mShippingAddress = shippingAddress;
        return this;
    }

    public String getAmount() {
        return mAmount;
    }

    public String getCurrencyCode() {
        return mCurrencyCode;
    }

    public boolean getNoShipping() {
        return mNoShipping;
    }

    public PostalAddress getShippingAddress() {
        return mShippingAddress;
    }

    public String getLocaleCode() {
        return mLocaleCode;
    }
}
