package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the parameters that are needed to start the PayPal Checkout flow
 */
public class PayPalCheckoutRequest extends PayPalRequest implements Parcelable {

    /**
     * The call-to-action in the PayPal Checkout flow
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PayPalCheckoutRequest.USER_ACTION_DEFAULT, PayPalCheckoutRequest.USER_ACTION_COMMIT})
    @interface PayPalPaymentUserAction {
    }

    /**
     * Shows the default call-to-action text on the PayPal Express Checkout page. This option indicates that a final
     * confirmation will be shown on the merchant checkout site before the user's payment method is charged.
     */
    public static final String USER_ACTION_DEFAULT = "";

    /**
     * Shows a deterministic call-to-action. This option indicates to the user that their payment method will be charged
     * when they click the call-to-action button on the PayPal Checkout page, and that no final confirmation page will
     * be shown on the merchant's checkout page. This option works for both checkout and vault flows.
     */
    public static final String USER_ACTION_COMMIT = "commit";

    private String intent = PayPalPaymentIntent.AUTHORIZE;
    private String userAction = USER_ACTION_DEFAULT;
    private final String amount;
    private String currencyCode;
    private boolean shouldRequestBillingAgreement;
    private boolean shouldOfferPayLater;

    /**
     * @param amount The transaction amount in currency units (as * determined by setCurrencyCode).
     *               For example, "1.20" corresponds to one dollar and twenty cents. Amount must be a non-negative
     *               number, may optionally contain exactly 2 decimal places separated by '.' and is
     *               limited to 7 digits before the decimal point.
     *               <p>
     *               This amount may differ slightly from the transaction amount. The exact decline rules
     *               for mismatches between this client-side amount and the final amount in the Transaction
     *               are determined by the gateway.
     **/
    public PayPalCheckoutRequest(@NonNull String amount) {
        this.amount = amount;
    }

    /**
     * Optional: A valid ISO currency code to use for the transaction. Defaults to merchant currency
     * code if not set.
     * <p>
     * If unspecified, the currency code will be chosen based on the active merchant account in the
     * client token.
     *
     * @param currencyCode A currency code, such as "USD"
     */
    public void setCurrencyCode(@Nullable String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /**
     * Optional: Payment intent. Must be set to {@link PayPalPaymentIntent#SALE} for immediate payment,
     * {@link PayPalPaymentIntent#AUTHORIZE} to authorize a payment for capture later, or
     * {@link PayPalPaymentIntent#ORDER} to create an order.
     * <p>
     * Defaults to authorize.
     *
     * @param intent {@link PayPalPaymentIntent}
     * @see <a href="https://developer.paypal.com/docs/api/payments/v1/#definition-payment">"intent" under the "payment" definition</a>
     * @see <a href="https://developer.paypal.com/docs/integration/direct/payments/create-process-order/">Create and process orders</a>
     * for more information
     */
    public void setIntent(@NonNull @PayPalPaymentIntent String intent) {
        this.intent = intent;
    }

    /**
     * Optional: The call-to-action in the PayPal Checkout flow.
     * <p>
     * By default the final button will show the localized word for "Continue" and implies that the
     * final amount billed is not yet known. Setting the PayPalCheckoutRequest's userAction to
     * {@link PayPalCheckoutRequest#USER_ACTION_COMMIT} changes the button text to "Pay Now",
     * conveying to the user that billing will take place immediately.
     *
     * @param userAction Must be a be {@link PayPalPaymentUserAction} value:
     *                   <ul>
     *                   <li>{@link PayPalCheckoutRequest#USER_ACTION_COMMIT}</li>
     *                   <li>{@link PayPalCheckoutRequest#USER_ACTION_DEFAULT}</li>
     *                   </ul>
     * @see <a href="https://developer.paypal.com/docs/api/payments/v1/#definition-application_context">See "user_action" under the "application_context" definition</a>
     */
    public void setUserAction(@NonNull @PayPalPaymentUserAction String userAction) {
        this.userAction = userAction;
    }

    /**
     * Optional: Offers PayPal Pay Later if the customer qualifies. Defaults to false.
     *
     * @param shouldOfferPayLater Whether to offer PayPal Pay Later.
     */
    public void setShouldOfferPayLater(boolean shouldOfferPayLater) {
        this.shouldOfferPayLater = shouldOfferPayLater;
    }

    /**
     * Optional: If set to true, this enables the Checkout with Vault flow, where the customer will be
     * prompted to consent to a billing agreement during checkout.
     *
     * @param shouldRequestBillingAgreement Whether to request billing agreement during checkout.
     */
    public void setShouldRequestBillingAgreement(boolean shouldRequestBillingAgreement) {
        this.shouldRequestBillingAgreement = shouldRequestBillingAgreement;
    }

    @NonNull
    public String getAmount() {
        return amount;
    }

    @Nullable
    public String getCurrencyCode() {
        return currencyCode;
    }

    @PayPalPaymentIntent
    @NonNull
    public String getIntent() {
        return intent;
    }

    @PayPalPaymentUserAction
    @NonNull
    public String getUserAction() {
        return userAction;
    }

    public boolean getShouldOfferPayLater() {
        return shouldOfferPayLater;
    }

    public boolean getShouldRequestBillingAgreement() {
        return shouldRequestBillingAgreement;
    }

    String createRequestBody(Configuration configuration, Authorization authorization, String successUrl, String cancelUrl) throws JSONException {
        JSONObject parameters = new JSONObject()
                .put(RETURN_URL_KEY, successUrl)
                .put(CANCEL_URL_KEY, cancelUrl)
                .put(OFFER_PAY_LATER_KEY, shouldOfferPayLater);

        if (authorization instanceof ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer());
        } else {
            parameters.put(TOKENIZATION_KEY, authorization.getBearer());
        }

        if (shouldRequestBillingAgreement) {
            parameters.put(REQUEST_BILLING_AGREEMENT_KEY, true);
        }

        String billingAgreementDescription = getBillingAgreementDescription();
        if (shouldRequestBillingAgreement && !TextUtils.isEmpty(billingAgreementDescription)) {
            parameters.put(DESCRIPTION_KEY, billingAgreementDescription);
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

        if (getCorrelationId() != null) {
            parameters.put(CORRELATION_ID_KEY, getCorrelationId());
        }

        parameters.put(EXPERIENCE_PROFILE_KEY, experienceProfile);
        return parameters.toString();
    }

    PayPalCheckoutRequest(Parcel in) {
        super(in);
        intent = in.readString();
        userAction = in.readString();
        amount = in.readString();
        currencyCode = in.readString();
        shouldRequestBillingAgreement = in.readByte() != 0;
        shouldOfferPayLater = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(intent);
        dest.writeString(userAction);
        dest.writeString(amount);
        dest.writeString(currencyCode);
        dest.writeByte((byte) (shouldRequestBillingAgreement ? 1 : 0));
        dest.writeByte((byte) (shouldOfferPayLater ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PayPalCheckoutRequest> CREATOR = new Creator<PayPalCheckoutRequest>() {
        @Override
        public PayPalCheckoutRequest createFromParcel(Parcel in) {
            return new PayPalCheckoutRequest(in);
        }

        @Override
        public PayPalCheckoutRequest[] newArray(int size) {
            return new PayPalCheckoutRequest[size];
        }
    };
}
