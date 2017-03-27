package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringDef;

import com.braintreepayments.api.BraintreeFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the parameters that are needed to start a Checkout with PayPal
 *
 * In the checkout flow, the user is presented with details about the order and only agrees to a
 * single payment. The result is not eligible for being saved in the Vault; however, you will receive
 * shipping information and the user will not be able to revoke the consent.
 *
 * @see <a href="https://developer.paypal.com/docs/api/#inputfields-object">PayPal REST API Reference</a>
 */
public class PayPalRequest implements Parcelable {

    /**
     * Payment intent. Must be set to sale for immediate payment, authorize to authorize a payment for capture later, or
     * order to create an order. Defaults to authorize. Only works in the Single Payment flow.
     *
     * @see <a href="https://developer.paypal.com/docs/integration/direct/payments/capture-payment/">Capture payments later</a>
     * and
     * <a href="https://developer.paypal.com/docs/integration/direct/payments/create-process-order/">Create and process orders</a>
     * for more information
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PayPalRequest.INTENT_ORDER, PayPalRequest.INTENT_SALE, PayPalRequest.INTENT_AUTHORIZE})
    @interface PayPalPaymentIntent {}
    public static final String INTENT_ORDER = "order";
    public static final String INTENT_SALE = "sale";
    public static final String INTENT_AUTHORIZE = "authorize";

    /**
     * Use this option to specify the PayPal page to display when a user lands on the PayPal site to complete the payment.
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PayPalRequest.LANDING_PAGE_TYPE_BILLING, PayPalRequest.LANDING_PAGE_TYPE_LOGIN})
    @interface PayPalLandingPageType {}

    /**
     * A non-PayPal account landing page is used.
     */
    public static final String LANDING_PAGE_TYPE_BILLING = "billing";

    /**
     * A PayPal account login page is used.
     */
    public static final String LANDING_PAGE_TYPE_LOGIN = "login";

    /**
     * @see <a href="https://developer.paypal.com/docs/classic/express-checkout/integration-guide/ECCustomizing/#allowing-buyers-to-complete-their-purchases-on-paypal">PayPal Express Checkout Guide</a>
     * for more information
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PayPalRequest.USER_ACTION_DEFAULT, PayPalRequest.USER_ACTION_COMMIT})
    @interface PayPalPaymentUserAction {}

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

    private String mAmount;
    private String mCurrencyCode;
    private String mLocaleCode;
    private String mBillingAgreementDescription;
    private boolean mShippingAddressRequired;
    private PostalAddress mShippingAddressOverride;
    private String mIntent = INTENT_AUTHORIZE;
    private String mLandingPageType;
    private String mUserAction = USER_ACTION_DEFAULT;
    private String mDisplayName;
    private boolean mOfferCredit;

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
    public PayPalRequest(String amount) {
        mAmount = amount;
        mShippingAddressRequired = false;
        mOfferCredit = false;
    }

    /**
     * Constructs a {@link PayPalRequest} with a null amount.
     */
    public PayPalRequest() {
        mAmount = null;
        mShippingAddressRequired = false;
        mOfferCredit = false;
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
    public PayPalRequest currencyCode(String currencyCode) {
        mCurrencyCode = currencyCode;
        return this;
    }

    /**
     * Defaults to false. When set to true, the shipping address selector will be displayed.
     *
     * @param shippingAddressRequired Whether to hide the shipping address in the flow.
     */
    public PayPalRequest shippingAddressRequired(boolean shippingAddressRequired) {
        mShippingAddressRequired = shippingAddressRequired;
        return this;
    }

    /**
     * Whether to use a custom locale code.
     * <br>
     * Supported locales are:
     * <br>
     * <code>da_DK</code>,
     * <code>de_DE</code>,
     * <code>en_AU</code>,
     * <code>en_GB</code>,
     * <code>en_US</code>,
     * <code>es_ES</code>,
     * <code>es_XC</code>,
     * <code>fr_CA</code>,
     * <code>fr_FR</code>,
     * <code>fr_XC</code>,
     * <code>id_ID</code>,
     * <code>it_IT</code>,
     * <code>ja_JP</code>,
     * <code>ko_KR</code>,
     * <code>nl_NL</code>,
     * <code>no_NO</code>,
     * <code>pl_PL</code>,
     * <code>pt_BR</code>,
     * <code>pt_PT</code>,
     * <code>ru_RU</code>,
     * <code>sv_SE</code>,
     * <code>th_TH</code>,
     * <code>tr_TR</code>,
     * <code>zh_CN</code>,
     * <code>zh_HK</code>,
     * <code>zh_TW</code>,
     * <code>zh_XC</code>.
     *
     * @param localeCode Whether to use a custom locale code.
     */
    public PayPalRequest localeCode(String localeCode) {
        mLocaleCode = localeCode;
        return this;
    }

    /**
     * The merchant name displayed in the PayPal flow; defaults to the company name on your Braintree account.
     *
     * @param displayName The name to be displayed in the PayPal flow.
     */
    public PayPalRequest displayName(String displayName) {
        mDisplayName = displayName;
        return this;
    }

    /**
     * Display a custom description to the user for a billing agreement.
     *
     * @param description The description to display.
     */
    public PayPalRequest billingAgreementDescription(String description) {
        mBillingAgreementDescription = description;
        return this;
    }

    /**
     * A custom shipping address to be used for the checkout flow.
     *
     * @param shippingAddressOverride a custom {@link PostalAddress}
     */
    public PayPalRequest shippingAddressOverride(PostalAddress shippingAddressOverride) {
        mShippingAddressOverride = shippingAddressOverride;
        return this;
    }

    /**
     * Payment intent. Only applies when calling
     * {@link com.braintreepayments.api.PayPal#requestOneTimePayment(BraintreeFragment, PayPalRequest)}.
     *
     * @param intent Can be either {@link PayPalRequest#INTENT_AUTHORIZE}, {@link PayPalRequest#INTENT_ORDER}, or {@link PayPalRequest#INTENT_SALE}.
     */
    public PayPalRequest intent(@PayPalPaymentIntent String intent) {
        mIntent = intent;
        return this;
    }

    /**
     * Use this option to specify the PayPal page to display when a user lands on the PayPal site to complete the payment.
     *
     * @param landingPageType Can be either {@link PayPalRequest#LANDING_PAGE_TYPE_BILLING} or {@link PayPalRequest#LANDING_PAGE_TYPE_LOGIN}.
     */
    public PayPalRequest landingPageType(@PayPalLandingPageType String landingPageType) {
        mLandingPageType = landingPageType;
        return this;
    }

    /**
     * Set the checkout user action.
     *
     * @param userAction Can be either {@link PayPalRequest#USER_ACTION_COMMIT} or {@link PayPalRequest#USER_ACTION_DEFAULT}.
     */
    public PayPalRequest userAction(@PayPalPaymentUserAction String userAction) {
        mUserAction = userAction;
        return this;
    }

    /**
     * Offers PayPal Credit prominently in the payment flow. Defaults to false. Only available with PayPal Checkout.
     *
     * @param offerCredit Whether to offer PayPal Credit.
     */
    public PayPalRequest offerCredit(boolean offerCredit) {
        mOfferCredit = offerCredit;
        return this;
    }

    public String getAmount() {
        return mAmount;
    }

    public String getCurrencyCode() {
        return mCurrencyCode;
    }

    public String getLocaleCode() {
        return mLocaleCode;
    }

    public String getBillingAgreementDescription() {
        return mBillingAgreementDescription;
    }

    public boolean isShippingAddressRequired() {
        return mShippingAddressRequired;
    }

    public PostalAddress getShippingAddressOverride() {
        return mShippingAddressOverride;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public boolean shouldOfferCredit() {
        return mOfferCredit;
    }

    @PayPalPaymentIntent
    public String getIntent() {
        return mIntent;
    }

    @PayPalLandingPageType
    public String getLandingPageType() {
        return mLandingPageType;
    }

    @PayPalPaymentUserAction
    public String getUserAction() {
        return mUserAction;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mAmount);
        parcel.writeString(mCurrencyCode);
        parcel.writeString(mLocaleCode);
        parcel.writeString(mBillingAgreementDescription);
        parcel.writeByte(mShippingAddressRequired ? (byte) 1:0);
        parcel.writeParcelable(mShippingAddressOverride, i);
        parcel.writeString(mIntent);
        parcel.writeString(mLandingPageType);
        parcel.writeString(mUserAction);
        parcel.writeString(mDisplayName);
        parcel.writeByte(mOfferCredit ? (byte) 1:0);
    }

    public PayPalRequest(Parcel in) {
        mAmount = in.readString();
        mCurrencyCode = in.readString();
        mLocaleCode = in.readString();
        mBillingAgreementDescription = in.readString();
        mShippingAddressRequired = in.readByte() > 0;
        mShippingAddressOverride = in.readParcelable(PostalAddress.class.getClassLoader());
        mIntent = in.readString();
        mLandingPageType = in.readString();
        mUserAction = in.readString();
        mDisplayName = in.readString();
        mOfferCredit = in.readByte() > 0;
    }

    public static final Creator<PayPalRequest> CREATOR = new Creator<PayPalRequest>() {
        @Override
        public PayPalRequest createFromParcel(Parcel in) {
            return new PayPalRequest(in);
        }

        @Override
        public PayPalRequest[] newArray(int size) {
            return new PayPalRequest[size];
        }
    };
}
