package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.annotation.StringDef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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


    private static final String NO_SHIPPING_KEY = "no_shipping";
    private static final String ADDRESS_OVERRIDE_KEY = "address_override";
    private static final String LOCALE_CODE_KEY = "locale_code";
    private static final String DESCRIPTION_KEY = "description";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorization_fingerprint";
    private static final String TOKENIZATION_KEY = "client_key";
    private static final String RETURN_URL_KEY = "return_url";
    private static final String OFFER_CREDIT_KEY = "offer_paypal_credit";
    private static final String OFFER_PAY_LATER_KEY = "offer_pay_later";
    private static final String CANCEL_URL_KEY = "cancel_url";
    private static final String EXPERIENCE_PROFILE_KEY = "experience_profile";
    private static final String AMOUNT_KEY = "amount";
    private static final String CURRENCY_ISO_CODE_KEY = "currency_iso_code";
    private static final String INTENT_KEY = "intent";
    private static final String LANDING_PAGE_TYPE_KEY = "landing_page_type";
    private static final String DISPLAY_NAME_KEY = "brand_name";
    private static final String SHIPPING_ADDRESS_KEY = "shipping_address";
    private static final String MERCHANT_ACCOUNT_ID = "merchant_account_id";
    private static final String LINE_ITEMS_KEY = "line_items";

    private static final String CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource";
    private static final String SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement";

    private static final String USER_ACTION_KEY = "useraction";

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

    String createRequestBody(Configuration configuration, Authorization authorization, String successUrl, String cancelUrl) throws JSONException {
        JSONObject parameters = new JSONObject()
                .put(RETURN_URL_KEY, successUrl)
                .put(CANCEL_URL_KEY, cancelUrl);

        if (authorization instanceof ClientToken) {
            parameters.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer());
        } else {
            parameters.put(TOKENIZATION_KEY, authorization.getBearer());
        }

//        if (isBillingAgreement) {
//            String billingAgreementDescription = payPalRequest.getBillingAgreementDescription();
//            if (!TextUtils.isEmpty(billingAgreementDescription)) {
//                parameters.put(DESCRIPTION_KEY, billingAgreementDescription);
//            }
//        } else {
//            String currencyCode = payPalRequest.getCurrencyCode();
//            if (currencyCode == null) {
//                currencyCode = configuration.getPayPalCurrencyIsoCode();
//            }
//
//            parameters
//                    .put(AMOUNT_KEY, payPalRequest.getAmount())
//                    .put(CURRENCY_ISO_CODE_KEY, currencyCode)
//                    .put(INTENT_KEY, payPalRequest.getIntent());
//
//            if (!payPalRequest.getLineItems().isEmpty()) {
//                JSONArray lineItems = new JSONArray();
//                for (PayPalLineItem lineItem : payPalRequest.getLineItems()) {
//                    lineItems.put(lineItem.toJson());
//                }
//                parameters.put(LINE_ITEMS_KEY, lineItems);
//            }
//        }
//
        JSONObject experienceProfile = new JSONObject();
        experienceProfile.put(NO_SHIPPING_KEY, !mShippingAddressRequired);
        experienceProfile.put(LANDING_PAGE_TYPE_KEY, mLandingPageType);
        String displayName = mDisplayName;
        if (TextUtils.isEmpty(displayName)) {
            displayName = configuration.getPayPalDisplayName();
        }
        experienceProfile.put(DISPLAY_NAME_KEY, displayName);

        if (mLocaleCode != null) {
            experienceProfile.put(LOCALE_CODE_KEY, mLocaleCode);
        }

//        if (mShippingAddressOverride != null) {
//            experienceProfile.put(ADDRESS_OVERRIDE_KEY, !mShippingAddressEditable);
//
//            JSONObject shippingAddressJson;
//            if (isBillingAgreement) {
//                shippingAddressJson = new JSONObject();
//                parameters.put(SHIPPING_ADDRESS_KEY, shippingAddressJson);
//            } else {
//                shippingAddressJson = parameters;
//            }
//
//            PostalAddress shippingAddress = mShippingAddressOverride;
//            shippingAddressJson.put(PostalAddressParser.LINE_1_KEY, shippingAddress.getStreetAddress());
//            shippingAddressJson.put(PostalAddressParser.LINE_2_KEY, shippingAddress.getExtendedAddress());
//            shippingAddressJson.put(PostalAddressParser.LOCALITY_KEY, shippingAddress.getLocality());
//            shippingAddressJson.put(PostalAddressParser.REGION_KEY, shippingAddress.getRegion());
//            shippingAddressJson.put(PostalAddressParser.POSTAL_CODE_UNDERSCORE_KEY, shippingAddress.getPostalCode());
//            shippingAddressJson.put(PostalAddressParser.COUNTRY_CODE_UNDERSCORE_KEY, shippingAddress.getCountryCodeAlpha2());
//            shippingAddressJson.put(PostalAddressParser.RECIPIENT_NAME_UNDERSCORE_KEY, shippingAddress.getRecipientName());
//        } else {
//            experienceProfile.put(ADDRESS_OVERRIDE_KEY, false);
//        }

        if (mMerchantAccountId != null) {
            parameters.put(MERCHANT_ACCOUNT_ID, mMerchantAccountId);
        }

        parameters.put(EXPERIENCE_PROFILE_KEY, experienceProfile);
        return parameters.toString();
    }
}
