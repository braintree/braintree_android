package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.api.PayPalTwoFactorAuth.SUCCESS_PATH;
import static com.braintreepayments.api.PayPalTwoFactorAuth.CANCEL_PATH;

/**
 * Represents the parameters that are needed to perform a PayPal two factor authentication lookup.
 */
public class PayPalTwoFactorAuthRequest {

    private String mNonce;
    private String mAmount;
    private String mCurrencyCode;

    /**
     * The {@link PaymentMethodNonce} generated from a payment method token stored in the Braintree Vault.
     * See <a href="https://developers.braintreepayments.com/guides/payment-methods">Braintree Vault</a>
     * @see <a href="https://developers.braintreepayments.com/reference/request/payment-method-nonce/create">Create a Nonce from a Payment Method Token</a>
     * @param nonce {@link PaymentMethodNonce} to be used for a two factor authentication lookup.
     */
    public PayPalTwoFactorAuthRequest nonce(String nonce) {
        this.mNonce = nonce;
        return this;
    }

    /**
     * The amount of the transaction that the authenticated nonce will be used in, as a {@link String}.
     * This is required to determine if authentication is required. Passing an amount other than
     * the final total can result in a declined transaction.
     * @param amount The amount of the transaction in the current merchant account's currency. This must be expressed in numbers with an optional decimal (using `.`) and precision up to the hundredths place. For example, if you're processing a transaction for 1.234,56 € then `amount` should be `1234.56`.
     */
    public PayPalTwoFactorAuthRequest amount(String amount) {
        this.mAmount = amount;
        return this;
    }

    /**
     * The currency code to be used with the amount specified in {@link #amount(String)}.
     * @param currencyCode
     */
    public PayPalTwoFactorAuthRequest currencyCode(String currencyCode) {
        this.mCurrencyCode = currencyCode;
        return this;
    }

    /**
     * The nonce to use in the two factor authentication lookup.
     */
    public String getNonce() {
        return mNonce;
    }

    /**
     * The amount to verify for the pending transaction.
     */
    public String getAmount() {
        return mAmount;
    }

    /**
     * The currency code to use for the specified amount.
     */
    public String getCurrencyCode() { return mCurrencyCode; }

    /**
     * Format the request as a JSON payload.
     * @return request formatted as JSON.
     */
    public String toJson(String authorizationFingerprint, String returnUrlScheme) {
        JSONObject parameters = new JSONObject();
        try {
            String baseUrl = returnUrlScheme + "://";
            parameters
                    .put("authorization_fingerprint", authorizationFingerprint)
                    .put("amount", this.mAmount)
                    .put("currency_iso_code", mCurrencyCode)
                    .put("return_url", baseUrl + SUCCESS_PATH)
                    .put("cancel_url", baseUrl + CANCEL_PATH)
                    .put("vault_initiated_checkout_payment_method_token", this.mNonce);
        } catch (JSONException ignored) { }
        return parameters.toString();
    }
}
