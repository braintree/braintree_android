package com.braintreepayments.api;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PayPalCheckoutRequest extends PayPalRequest {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PayPalCheckoutRequest.INTENT_ORDER, PayPalCheckoutRequest.INTENT_SALE, PayPalCheckoutRequest.INTENT_AUTHORIZE})
    @interface PayPalPaymentIntent {}
    public static final String INTENT_ORDER = "order";
    public static final String INTENT_SALE = "sale";
    public static final String INTENT_AUTHORIZE = "authorize";

    private String intent = INTENT_AUTHORIZE;
    private String amount;
    private String currencyCode;
    private boolean offerPayLater;

    /**
     * This amount may differ slightly from the transaction amount. The exact decline rules
     * for mismatches between this client-side amount and the final amount in the Transaction
     * are determined by the gateway.
     *
     * @param amount The transaction amount in currency units (as * determined by setCurrencyCode).
     * For example, "1.20" corresponds to one dollar and twenty cents. Amount must be a non-negative
     * number, may optionally contain exactly 2 decimal places separated by '.', optional
     * thousands separator ',', limited to 7 digits before the decimal point.
     **/
    public PayPalCheckoutRequest(String amount) {
        this.amount = amount;
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
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }


    /**
     * Payment intent. Must be set to {@link #INTENT_SALE} for immediate payment,
     * {@link #INTENT_AUTHORIZE} to authorize a payment for capture later, or
     * {@link #INTENT_ORDER} to create an order.
     *
     * Defaults to authorize. Only works in the Single Payment flow.
     *
     * @param intent Must be a {@link PayPalPaymentIntent} value:
     * <ul>
     * <li>{@link PayPalCheckoutRequest#INTENT_AUTHORIZE} to authorize a payment for capture later </li>
     * <li>{@link PayPalCheckoutRequest#INTENT_ORDER} to create an order </li>
     * <li>{@link PayPalCheckoutRequest#INTENT_SALE} for immediate payment </li>
     * </ul>
     *
     * @see <a href="https://developer.paypal.com/docs/api/payments/v1/#definition-payment">"intent" under the "payment" definition</a>
     * @see <a href="https://developer.paypal.com/docs/integration/direct/payments/create-process-order/">Create and process orders</a>
     * for more information
     *
     */
    public void setIntent(@PayPalPaymentIntent String intent) {
        this.intent = intent;
    }

    /**
     * Offers PayPal Pay Later prominently in the payment flow. Defaults to false. Only available with PayPal Checkout.
     *
     * @param offerPayLater Whether to offer PayPal Pay Later.
     */
    public void setOfferPayLater(boolean offerPayLater) {
        this.offerPayLater = offerPayLater;
    }

    public String getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    @PayPalPaymentIntent
    public String getIntent() {
        return intent;
    }

    public boolean shouldOfferPayLater() {
        return offerPayLater;
    }

    String createRequestBody(Configuration configuration) {
       return "";
    }
}
