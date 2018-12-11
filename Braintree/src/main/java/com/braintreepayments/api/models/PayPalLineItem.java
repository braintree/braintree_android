package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringDef;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PayPalLineItem implements Parcelable {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PayPalLineItem.KIND_CREDIT, PayPalLineItem.KIND_DEBIT})
    @interface PayPalLineItemKind {}
    public static final String KIND_CREDIT = "credit";
    public static final String KIND_DEBIT = "debit";

    private static final String DESCRIPTION_KEY = "description";
    private static final String KIND_KEY = "kind";
    private static final String NAME_KEY = "name";
    private static final String PRODUCT_CODE_KEY = "product_code";
    private static final String QUANTITY_KEY = "quantity";
    private static final String UNIT_AMOUNT_KEY = "unit_amount";
    private static final String UNIT_TAX_AMOUNT_KEY = "unit_tax_amount";
    private static final String URL_KEY = "url";

    private String mDescription;
    private String mKind;
    private String mName;
    private String mProductCode;
    private String mQuantity;
    private String mUnitAmount;
    private String mUnitTaxAmount;
    private String mUrl;

    /**
     * Constructs a line item for PayPal checkout flows. All parameters are required.
     *
     * @param kind The {@link PayPalLineItemKind} kind.
     * @param name The name of the item to display.
     * @param quantity The quantity of the item.
     * @param unitAmount The unit amount.
     */
    public PayPalLineItem(@PayPalLineItemKind  String kind, String name, String quantity, String unitAmount) {
        mKind = kind;
        mName = name;
        mQuantity = quantity;
        mUnitAmount = unitAmount;
    }

    /**
     * Item description. Maximum 127 characters.
     *
     * @param description The description to display.
     */
    public void setDescription(String description) {
        mDescription = description;
    }

    /**
     * Indicates whether the line item is a debit (sale) or credit (refund) to the customer.
     *
     * @param kind The {@link PayPalLineItemKind} kind.
     */
    public void setKind(@PayPalLineItemKind String kind) {
        mKind = kind;
    }

    /**
     * Item name. Maximum 127 characters.
     *
     * @param name The name to display
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Product or UPC code for the item. Maximum 127 characters.
     *
     * @param productCode The product code.
     */
    public void setProductCode(String productCode) {
        mProductCode = productCode;
    }

    /**
     * Number of units of the item purchased. This value must be a whole number and can't be negative or zero.
     *
     * @param quantity The quantity.
     */
    public void setQuantity(String quantity) {
        mQuantity = quantity;
    }

    /**
     * Per-unit price of the item. Can include up to 2 decimal places. This value can't be negative or zero.
     *
     * @param unitAmount The unit amount.
     */
    public void setUnitAmount(String unitAmount) {
        mUnitAmount = unitAmount;
    }

    /**
     * Per-unit tax price of the item. Can include up to 2 decimal places. This value can't be negative or zero.
     *
     * @param unitTaxAmount The unit tax amount.
     */
    public void setUnitTaxAmount(String unitTaxAmount) {
        mUnitTaxAmount = unitTaxAmount;
    }

    /**
     * The URL to product information.
     *
     * @param url The URL with additional information.
     */
    public void setUrl(String url) {
        mUrl = url;
    }

    public String getDescription() {
        return mDescription;
    }

    @PayPalLineItemKind
    public String getKind() {
        return mKind;
    }

    public String getName() {
        return mName;
    }

    public String getProductCode() {
        return mProductCode;
    }

    public String getQuantity() {
        return mQuantity;
    }

    public String getUnitAmount() {
        return mUnitAmount;
    }

    public String getUnitTaxAmount() {
        return mUnitTaxAmount;
    }

    public String getUrl() {
        return mUrl;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .putOpt(DESCRIPTION_KEY, mDescription)
                    .putOpt(KIND_KEY, mKind)
                    .putOpt(NAME_KEY, mName)
                    .putOpt(PRODUCT_CODE_KEY, mProductCode)
                    .putOpt(QUANTITY_KEY, mQuantity)
                    .putOpt(UNIT_AMOUNT_KEY, mUnitAmount)
                    .putOpt(UNIT_TAX_AMOUNT_KEY, mUnitTaxAmount)
                    .putOpt(URL_KEY, mUrl);
        } catch (JSONException ignored) {}

        return new JSONObject();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mDescription);
        dest.writeString(mKind);
        dest.writeString(mName);
        dest.writeString(mProductCode);
        dest.writeString(mQuantity);
        dest.writeString(mUnitAmount);
        dest.writeString(mUnitTaxAmount);
        dest.writeString(mUrl);
    }

    private PayPalLineItem(Parcel in) {
        mDescription = in.readString();
        mKind = in.readString();
        mName = in.readString();
        mProductCode = in.readString();
        mQuantity = in.readString();
        mUnitAmount = in.readString();
        mUnitTaxAmount = in.readString();
        mUrl = in.readString();
    }

    public static final Creator<PayPalLineItem> CREATOR = new Creator<PayPalLineItem>() {
        public PayPalLineItem createFromParcel(Parcel source) {
            return new PayPalLineItem(source);
        }

        public PayPalLineItem[] newArray(int size) {
            return new PayPalLineItem[size];
        }
    };
}
