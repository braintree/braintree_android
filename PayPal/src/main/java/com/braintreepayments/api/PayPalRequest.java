package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents the parameters that are needed to start a Checkout with PayPal
 *
 * In the checkout flow, the user is presented with details about the order and only agrees to a
 * single payment. The result is not eligible for being saved in the Vault; however, you will receive
 * shipping information and the user will not be able to revoke the consent.
 *
 * @see <a href="https://developer.paypal.com/docs/api/#inputfields-object">PayPal REST API Reference</a>
 */
public class PayPalRequest {

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

    private String mLocaleCode;
    private String mBillingAgreementDescription;
    private boolean mShippingAddressRequired;
    private boolean mShippingAddressEditable = false;
    private PostalAddress mShippingAddressOverride;
    private String mLandingPageType;
    private String mUserAction = USER_ACTION_DEFAULT;
    private String mDisplayName;
    private boolean mOfferCredit;
    private String mMerchantAccountId;
    private final ArrayList<PayPalLineItem> mLineItems = new ArrayList<>();

    /**
     * Constructs a request for PayPal Single Payment and Billing Agreement flows.
     */
    public PayPalRequest() {
        mShippingAddressRequired = false;
        mOfferCredit = false;
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
     * Defaults to false. Set to true to enable user editing of the shipping address.
     * Only applies when {@link PayPalRequest#shippingAddressOverride(PostalAddress)} is set
     * with a {@link PostalAddress}.
     *
     * @param shippingAddressEditable Whether to allow the the shipping address to be editable.
     */
    public PayPalRequest shippingAddressEditable(boolean shippingAddressEditable) {
        mShippingAddressEditable = shippingAddressEditable;
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
     * Use this option to specify the PayPal page to display when a user lands on the PayPal site to complete the payment.
     *
     * @param landingPageType Must be a {@link PayPalLandingPageType} value:
     * <ul>
     * <li>{@link #LANDING_PAGE_TYPE_BILLING}</li>
     * <li>{@link #LANDING_PAGE_TYPE_LOGIN}</li>
     *
     * @see <a href="https://developer.paypal.com/docs/api/payments/v1/#definition-application_context">See "landing_page" under the "application_context" definition</a>
     */
    public PayPalRequest landingPageType(@PayPalLandingPageType String landingPageType) {
        mLandingPageType = landingPageType;
        return this;
    }

    /**
     * Set the checkout user action which determines the button text.
     *
     * @param userAction Must be a be {@link PayPalPaymentUserAction} value:
     * <ul>
     * <li>{@link #USER_ACTION_COMMIT}</li>
     * <li>{@link #USER_ACTION_DEFAULT}</li>
     * </ul>
     *
     * @see <a href="https://developer.paypal.com/docs/api/payments/v1/#definition-application_context">See "user_action" under the "application_context" definition</a>
     */
    public PayPalRequest userAction(@PayPalPaymentUserAction String userAction) {
        mUserAction = userAction;
        return this;
    }

    /**
     * Offers PayPal Credit prominently in the payment flow. Defaults to false. Only available with Billing Agreements
     * and PayPal Checkout.
     *
     * @param offerCredit Whether to offer PayPal Credit.
     */
    public PayPalRequest offerCredit(boolean offerCredit) {
        mOfferCredit = offerCredit;
        return this;
    }

    /**
     * Specify a merchant account Id other than the default to use during tokenization.
     *
     * @param merchantAccountId the non-default merchant account Id.
     */
    public PayPalRequest merchantAccountId(String merchantAccountId) {
        mMerchantAccountId = merchantAccountId;
        return this;
    }

    /**
     * The line items for this transaction. It can include up to 249 line items.
     *
     * @param lineItems a collection of {@link PayPalLineItem}
     */
    public PayPalRequest lineItems(Collection<PayPalLineItem> lineItems) {
        mLineItems.clear();
        mLineItems.addAll(lineItems);
        return this;
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

    public boolean isShippingAddressEditable() {
        return mShippingAddressEditable;
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

    public String getMerchantAccountId() {
        return mMerchantAccountId;
    }

    public ArrayList<PayPalLineItem> getLineItems() {
        return mLineItems;
    }

    @PayPalLandingPageType
    public String getLandingPageType() {
        return mLandingPageType;
    }

    @PayPalPaymentUserAction
    public String getUserAction() {
        return mUserAction;
    }
}
