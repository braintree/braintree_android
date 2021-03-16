package com.braintreepayments.api;

import androidx.annotation.StringDef;

import org.json.JSONException;

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
public abstract class PayPalRequest {

    static final String NO_SHIPPING_KEY = "no_shipping";
    static final String ADDRESS_OVERRIDE_KEY = "address_override";
    static final String LOCALE_CODE_KEY = "locale_code";
    static final String DESCRIPTION_KEY = "description";
    static final String AUTHORIZATION_FINGERPRINT_KEY = "authorization_fingerprint";
    static final String TOKENIZATION_KEY = "client_key";
    static final String RETURN_URL_KEY = "return_url";
    static final String OFFER_CREDIT_KEY = "offer_paypal_credit";
    static final String OFFER_PAY_LATER_KEY = "offer_pay_later";
    static final String CANCEL_URL_KEY = "cancel_url";
    static final String EXPERIENCE_PROFILE_KEY = "experience_profile";
    static final String AMOUNT_KEY = "amount";
    static final String CURRENCY_ISO_CODE_KEY = "currency_iso_code";
    static final String INTENT_KEY = "intent";
    static final String LANDING_PAGE_TYPE_KEY = "landing_page_type";
    static final String DISPLAY_NAME_KEY = "brand_name";
    static final String SHIPPING_ADDRESS_KEY = "shipping_address";
    static final String MERCHANT_ACCOUNT_ID = "merchant_account_id";
    static final String LINE_ITEMS_KEY = "line_items";

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
    private String mMerchantAccountId;
    private final ArrayList<PayPalLineItem> mLineItems = new ArrayList<>();

    /**
     * Constructs a request for PayPal Single Payment and Billing Agreement flows.
     */
    public PayPalRequest() {
        mShippingAddressRequired = false;
    }

    /**
     * Defaults to false. When set to true, the shipping address selector will be displayed.
     *
     * @param shippingAddressRequired Whether to hide the shipping address in the flow.
     */
    public void setShippingAddressRequired(boolean shippingAddressRequired) {
        mShippingAddressRequired = shippingAddressRequired;
    }

    /**
     * Defaults to false. Set to true to enable user editing of the shipping address.
     * Only applies when {@link PayPalRequest#setShippingAddressOverride(PostalAddress)} is set
     * with a {@link PostalAddress}.
     *
     * @param shippingAddressEditable Whether to allow the the shipping address to be editable.
     */
    public void setShippingAddressEditable(boolean shippingAddressEditable) {
        mShippingAddressEditable = shippingAddressEditable;
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
    public void setLocaleCode(String localeCode) {
        mLocaleCode = localeCode;
    }

    /**
     * The merchant name displayed in the PayPal flow; defaults to the company name on your Braintree account.
     *
     * @param displayName The name to be displayed in the PayPal flow.
     */
    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    /**
     * Display a custom description to the user for a billing agreement.
     *
     * @param description The description to display.
     */
    public void setBillingAgreementDescription(String description) {
        mBillingAgreementDescription = description;
    }

    /**
     * A custom shipping address to be used for the checkout flow.
     *
     * @param shippingAddressOverride a custom {@link PostalAddress}
     */
    public void setShippingAddressOverride(PostalAddress shippingAddressOverride) {
        mShippingAddressOverride = shippingAddressOverride;
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
    public void setLandingPageType(@PayPalLandingPageType String landingPageType) {
        mLandingPageType = landingPageType;
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
    public void setUserAction(@PayPalPaymentUserAction String userAction) {
        mUserAction = userAction;
    }

    /**
     * Specify a merchant account Id other than the default to use during tokenization.
     *
     * @param merchantAccountId the non-default merchant account Id.
     */
    public void setMerchantAccountId(String merchantAccountId) {
        mMerchantAccountId = merchantAccountId;
    }

    /**
     * The line items for this transaction. It can include up to 249 line items.
     *
     * @param lineItems a collection of {@link PayPalLineItem}
     */
    public void setLineItems(Collection<PayPalLineItem> lineItems) {
        mLineItems.clear();
        mLineItems.addAll(lineItems);
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

    abstract String createRequestBody(Configuration configuration, Authorization authorization, String successUrl, String cancelUrl) throws JSONException;
}
