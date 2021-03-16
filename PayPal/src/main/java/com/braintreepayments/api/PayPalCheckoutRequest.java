package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.annotation.StringDef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    String createRequestBody(Configuration configuration, Authorization authorization, String successUrl, String cancelUrl) throws JSONException {
        JSONObject parameters = new JSONObject()
                .put(RETURN_URL_KEY, successUrl)
                .put(CANCEL_URL_KEY, cancelUrl)
                .put(OFFER_PAY_LATER_KEY, offerPayLater);

        if (authorization instanceof ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer());
        } else {
            parameters.put(TOKENIZATION_KEY, authorization.getBearer());
        }

        String currencyCode = getCurrencyCode();
        if (currencyCode == null) {
            currencyCode = configuration.getPayPalCurrencyIsoCode();
        }

        parameters
                .put(AMOUNT_KEY, amount)
                .put(CURRENCY_ISO_CODE_KEY, currencyCode)
                .put(INTENT_KEY, intent);

        if (!getLineItems().isEmpty()) {
            JSONArray lineItems = new JSONArray();
            for (PayPalLineItem lineItem : getLineItems()) {
                lineItems.put(lineItem.toJson());
            }
            parameters.put(LINE_ITEMS_KEY, lineItems);
        }

        JSONObject experienceProfile = new JSONObject();
        experienceProfile.put(NO_SHIPPING_KEY, !isShippingAddressRequired());
        experienceProfile.put(LANDING_PAGE_TYPE_KEY, getLandingPageType());
        String displayName = getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = configuration.getPayPalDisplayName();
        }
        experienceProfile.put(DISPLAY_NAME_KEY, displayName);

        if (getLocaleCode() != null) {
            experienceProfile.put(LOCALE_CODE_KEY, getLocaleCode());
        }

        if (getShippingAddressOverride() != null) {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, !isShippingAddressEditable());

            PostalAddress shippingAddress = getShippingAddressOverride();
            parameters.put(PostalAddressParser.LINE_1_KEY, shippingAddress.getStreetAddress());
            parameters.put(PostalAddressParser.LINE_2_KEY, shippingAddress.getExtendedAddress());
            parameters.put(PostalAddressParser.LOCALITY_KEY, shippingAddress.getLocality());
            parameters.put(PostalAddressParser.REGION_KEY, shippingAddress.getRegion());
            parameters.put(PostalAddressParser.POSTAL_CODE_UNDERSCORE_KEY, shippingAddress.getPostalCode());
            parameters.put(PostalAddressParser.COUNTRY_CODE_UNDERSCORE_KEY, shippingAddress.getCountryCodeAlpha2());
            parameters.put(PostalAddressParser.RECIPIENT_NAME_UNDERSCORE_KEY, shippingAddress.getRecipientName());
        } else {
            experienceProfile.put(ADDRESS_OVERRIDE_KEY, false);
        }

        if (getMerchantAccountId() != null) {
            parameters.put(MERCHANT_ACCOUNT_ID, getMerchantAccountId());
        }

        parameters.put(EXPERIENCE_PROFILE_KEY, experienceProfile);
        return parameters.toString();
    }
}
