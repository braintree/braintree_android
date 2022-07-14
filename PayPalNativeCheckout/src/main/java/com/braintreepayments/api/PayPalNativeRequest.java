package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.paypal.checkout.shipping.OnShippingChange;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents the parameters that are needed to tokenize a PayPal account.
 * See {@link PayPalNativeCheckoutRequest} and {@link PayPalNativeCheckoutVaultRequest}.
 */
public abstract class PayPalNativeRequest implements Parcelable {

    static final String NO_SHIPPING_KEY = "no_shipping";
    static final String LOCALE_CODE_KEY = "locale_code";
    static final String REQUEST_BILLING_AGREEMENT_KEY = "request_billing_agreement";
    static final String BILLING_AGREEMENT_DETAILS_KEY = "billing_agreement_details";
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
    static final String MERCHANT_ACCOUNT_ID = "merchant_account_id";
    static final String CORRELATION_ID_KEY = "correlation_id";
    static final String LINE_ITEMS_KEY = "line_items";

    private String localeCode;
    private String billingAgreementDescription;
    private boolean shippingAddressRequired;
    private boolean shippingAddressEditable = false;
    private PostalAddress shippingAddressOverride;
    private String landingPageType;
    private String displayName;
    private String merchantAccountId;
    private String riskCorrelationId;
    private final ArrayList<PayPalNativeCheckoutLineItem> lineItems;
    private String returnUrl;
    private OnShippingChange onShippingChange;

    /**
     * Constructs a request for PayPal Checkout and Vault flows.
     */
    public PayPalNativeRequest() {
        shippingAddressRequired = false;
        lineItems = new ArrayList<>();
    }

    /**
     * Defaults to false. When set to true, the shipping address selector will be displayed.
     *
     * @param shippingAddressRequired Whether to hide the shipping address in the flow.
     */
    public void setShippingAddressRequired(boolean shippingAddressRequired) {
        this.shippingAddressRequired = shippingAddressRequired;
    }

    /**
     * Defaults to false. Set to true to enable user editing of the shipping address.
     * Only applies when {@link PayPalNativeRequest#setShippingAddressOverride(PostalAddress)} is set
     * with a {@link PostalAddress}.
     *
     * @param shippingAddressEditable Whether to allow the the shipping address to be editable.
     */
    public void setShippingAddressEditable(boolean shippingAddressEditable) {
        this.shippingAddressEditable = shippingAddressEditable;
    }

    /**
     * Optional: A locale code to use for the transaction.
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
     * @param localeCode A locale code to use for the transaction.
     */
    public void setLocaleCode(@Nullable String localeCode) {
        this.localeCode = localeCode;
    }

    /**
     * Optional: The merchant name displayed in the PayPal flow; defaults to the company name on your Braintree account.
     *
     * @param displayName The name to be displayed in the PayPal flow.
     */
    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    /**
     * Optional: Display a custom description to the user for a billing agreement.
     *
     * @param description The description to display.
     */
    public void setBillingAgreementDescription(@Nullable String description) {
        billingAgreementDescription = description;
    }

    /**
     * Optional: A valid shipping address to be displayed in the transaction flow. An error will occur if this address is not valid
     *
     * @param shippingAddressOverride a custom {@link PostalAddress}
     */
    public void setShippingAddressOverride(@Nullable PostalAddress shippingAddressOverride) {
        this.shippingAddressOverride = shippingAddressOverride;
    }

    /**
     * Optional: Specify a merchant account Id other than the default to use during tokenization.
     *
     * @param merchantAccountId the non-default merchant account Id.
     */
    public void setMerchantAccountId(@Nullable String merchantAccountId) {
        this.merchantAccountId = merchantAccountId;
    }

    /**
     * Optional: A risk correlation ID created with Set Transaction Context on your server.
     *
     * @param riskCorrelationId the correlation ID.
     */
    public void setRiskCorrelationId(@Nullable String riskCorrelationId) {
        this.riskCorrelationId = riskCorrelationId;
    }

    /**
     * Optional: The line items for this transaction. It can include up to 249 line items.
     *
     * @param lineItems a collection of {@link PayPalNativeCheckoutLineItem}
     */
    public void setLineItems(@NonNull Collection<PayPalNativeCheckoutLineItem> lineItems) {
        this.lineItems.clear();
        this.lineItems.addAll(lineItems);
    }

    public void setReturnUrl(@NonNull String returnUrl) {
        this.returnUrl = returnUrl;
    }

    /**
     * Optional: The OnShippingChange callback that handles shipping charge changes
     *
     * @param onShippingChange how to handle the shipping change callback
     */
    public void setOnShippingChange(OnShippingChange onShippingChange) {
        this.onShippingChange = onShippingChange;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    @Nullable
    public String getLocaleCode() {
        return localeCode;
    }

    @Nullable
    public String getBillingAgreementDescription() {
        return billingAgreementDescription;
    }

    public boolean isShippingAddressRequired() {
        return shippingAddressRequired;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public String getMerchantAccountId() {
        return merchantAccountId;
    }

    @Nullable
    public String getRiskCorrelationId() {
        return riskCorrelationId;
    }

    @Nullable
    public OnShippingChange getOnShippingChange() {
        return onShippingChange;
    }

    @NonNull
    public ArrayList<PayPalNativeCheckoutLineItem> getLineItems() {
        return lineItems;
    }

    abstract String createRequestBody(Configuration configuration, Authorization authorization, String successUrl, String cancelUrl) throws JSONException;

    protected PayPalNativeRequest(Parcel in) {
        localeCode = in.readString();
        billingAgreementDescription = in.readString();
        shippingAddressRequired = in.readByte() != 0;
        shippingAddressEditable = in.readByte() != 0;
        shippingAddressOverride = in.readParcelable(PostalAddress.class.getClassLoader());
        landingPageType = in.readString();
        displayName = in.readString();
        merchantAccountId = in.readString();
        riskCorrelationId = in.readString();
        lineItems = in.createTypedArrayList(PayPalNativeCheckoutLineItem.CREATOR);
        returnUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(localeCode);
        parcel.writeString(billingAgreementDescription);
        parcel.writeByte((byte) (shippingAddressRequired ? 1 : 0));
        parcel.writeByte((byte) (shippingAddressEditable ? 1 : 0));
        parcel.writeParcelable(shippingAddressOverride, i);
        parcel.writeString(landingPageType);
        parcel.writeString(displayName);
        parcel.writeString(merchantAccountId);
        parcel.writeString(riskCorrelationId);
        parcel.writeTypedList(lineItems);
        parcel.writeString(returnUrl);

    }
}
