package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Deprecated. Use PayPal module instead.
 */
@Deprecated
public class PayPalNativeCheckoutLineItem implements Parcelable {

    /**
     * The type of PayPal line item.
     * <p>
     * {@link #KIND_CREDIT} A line item that is a credit.
     * {@link #KIND_DEBIT} A line item that debits.
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PayPalNativeCheckoutLineItem.KIND_CREDIT, PayPalNativeCheckoutLineItem.KIND_DEBIT})
    @interface PayPalLineItemKind {
    }

    @Deprecated
    public static final String KIND_CREDIT = "credit";
    @Deprecated
    public static final String KIND_DEBIT = "debit";

    /**
     * The upc type of PayPal line item.
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PayPalNativeCheckoutLineItem.UPC_TYPE_A, PayPalNativeCheckoutLineItem.UPC_TYPE_B, PayPalNativeCheckoutLineItem.UPC_TYPE_C, PayPalNativeCheckoutLineItem.UPC_TYPE_D, PayPalNativeCheckoutLineItem.UPC_TYPE_E, PayPalNativeCheckoutLineItem.UPC_TYPE_2, PayPalNativeCheckoutLineItem.UPC_TYPE_5})
    @interface PayPalLineItemUpcType {
    }

    @Deprecated
    public static final String UPC_TYPE_A = "UPC-A";
    @Deprecated
    public static final String UPC_TYPE_B = "UPC-B";
    @Deprecated
    public static final String UPC_TYPE_C = "UPC-C";
    @Deprecated
    public static final String UPC_TYPE_D = "UPC-D";
    @Deprecated
    public static final String UPC_TYPE_E = "UPC-E";
    @Deprecated
    public static final String UPC_TYPE_2 = "UPC-2";
    @Deprecated
    public static final String UPC_TYPE_5 = "UPC-5";

    private static final String DESCRIPTION_KEY = "description";
    private static final String IMAGE_URL_KEY = "image_url";
    private static final String KIND_KEY = "kind";
    private static final String NAME_KEY = "name";
    private static final String PRODUCT_CODE_KEY = "product_code";
    private static final String QUANTITY_KEY = "quantity";
    private static final String UNIT_AMOUNT_KEY = "unit_amount";
    private static final String UNIT_TAX_AMOUNT_KEY = "unit_tax_amount";
    private static final String UPC_CODE_KEY = "upc_code";
    private static final String UPC_TYPE_KEY = "upc_type";
    private static final String URL_KEY = "url";

    private String description;
    private String imageUrl;
    private String kind;
    private String name;
    private String productCode;
    private String quantity;
    private String unitAmount;
    private String unitTaxAmount;
    private String upcCode;
    private String upcType;
    private String url;

    /**
     * Deprecated. Use PayPal module instead.
     * Constructs a line item for PayPal checkout flows. All parameters are required.
     *
     * @param kind       The {@link PayPalLineItemKind} kind.
     * @param name       The name of the item to display.
     * @param quantity   The quantity of the item.
     * @param unitAmount The unit amount.
     */
    @Deprecated
    public PayPalNativeCheckoutLineItem(@NonNull @PayPalLineItemKind String kind,
                                        @NonNull String name,
                                        @NonNull String quantity,
                                        @NonNull String unitAmount) {
        this.kind = kind;
        this.name = name;
        this.quantity = quantity;
        this.unitAmount = unitAmount;
    }

    /**
     * Deprecated. Use PayPal module instead.
     * Item description. Maximum 127 characters.
     *
     * @param description The description to display.
     */
   @Deprecated
    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    /**
     * The image URL to product information.
     * Deprecated. Use PayPal module instead.
     *
     * @param imageUrl The image URL with additional information.
     */
    @Deprecated
    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Indicates whether the line item is a debit (sale) or credit (refund) to the customer.
     *
     * Deprecated. Use PayPal module instead.
     * @param kind The {@link PayPalLineItemKind} kind.
     */
    @Deprecated
    public void setKind(@NonNull @PayPalLineItemKind String kind) {
        this.kind = kind;
    }

    /**
     * Item name. Maximum 127 characters.
     *
     * Deprecated. Use PayPal module instead.
     * @param name The name to display
     */
    @Deprecated
    public void setName(@NonNull String name) {
        this.name = name;
    }

    /**
     * Product or UPC code for the item. Maximum 127 characters.
     *
     * Deprecated. Use PayPal module instead.
     * @param productCode The product code.
     */
    @Deprecated
    public void setProductCode(@NonNull String productCode) {
        this.productCode = productCode;
    }

    /**
     * Number of units of the item purchased. This value must be a whole number and can't be negative or zero.
     *
     * Deprecated. Use PayPal module instead.
     * @param quantity The quantity.
     */
    @Deprecated
    public void setQuantity(@NonNull String quantity) {
        this.quantity = quantity;
    }

    /**
     * Per-unit price of the item. Can include up to 2 decimal places. This value can't be negative or zero.
     *
     * @param unitAmount The unit amount.
     */
    public void setUnitAmount(@NonNull String unitAmount) {
        this.unitAmount = unitAmount;
    }

    /**
     * Deprecated. Use PayPal module instead.
     * Per-unit tax price of the item. Can include up to 2 decimal places. This value can't be negative or zero.
     *
     * @param unitTaxAmount The unit tax amount.
     */
    @Deprecated
    public void setUnitTaxAmount(@NonNull String unitTaxAmount) {
        this.unitTaxAmount = unitTaxAmount;
    }

    /**
     * UPC code of the item.
     *
     * Deprecated. Use PayPal module instead.
     * @param upcCode The UPC code.
     */
    @Deprecated
    public void setUpcCode(@Nullable String upcCode) {
        this.upcCode = upcCode;
    }

    /**
     * Deprecated. Use PayPal module instead.
     * UPC type of the item.
     *
     * @param upcType The UPC type.
     */
    @Deprecated
    public void setUpcType(@Nullable @PayPalLineItemUpcType String upcType) {
        this.upcType = upcType;
    }

    /**
     * Deprecated. Use PayPal module instead.
     * The URL to product information.
     *
     * @param url The URL with additional information.
     */
    @Deprecated
    public void setUrl(@NonNull String url) {
        this.url = url;
    }

    @Deprecated
    @Nullable
    public String getDescription() {
        return description;
    }

    @Deprecated
    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    @Deprecated
    @PayPalLineItemKind
    @Nullable
    public String getKind() {
        return kind;
    }

    @Deprecated
    @Nullable
    public String getName() {
        return name;
    }

   @Deprecated
    @Nullable
    public String getProductCode() {
        return productCode;
    }

    @Deprecated
    @Nullable
    public String getQuantity() {
        return quantity;
    }

    @Deprecated
    @Nullable
    public String getUnitAmount() {
        return unitAmount;
    }

    @Deprecated
    @Nullable
    public String getUnitTaxAmount() {
        return unitTaxAmount;
    }

    @Deprecated
    @Nullable
    public String getUpcCode() {
        return upcCode;
    }

    @Deprecated
    @PayPalLineItemUpcType
    @Nullable
    public String getUpcType() {
        return upcType;
    }

    @Deprecated
    @Nullable
    public String getUrl() {
        return url;
    }

    @Deprecated
    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .putOpt(DESCRIPTION_KEY, description)
                    .putOpt(IMAGE_URL_KEY, imageUrl)
                    .putOpt(KIND_KEY, kind)
                    .putOpt(NAME_KEY, name)
                    .putOpt(PRODUCT_CODE_KEY, productCode)
                    .putOpt(QUANTITY_KEY, quantity)
                    .putOpt(UNIT_AMOUNT_KEY, unitAmount)
                    .putOpt(UNIT_TAX_AMOUNT_KEY, unitTaxAmount)
                    .putOpt(UPC_CODE_KEY, upcCode)
                    .putOpt(UPC_TYPE_KEY, upcType)
                    .putOpt(URL_KEY, url);
        } catch (JSONException ignored) {
        }

        return new JSONObject();
    }

    PayPalNativeCheckoutLineItem(Parcel in) {
        description = in.readString();
        imageUrl = in.readString();
        kind = in.readString();
        name = in.readString();
        productCode = in.readString();
        quantity = in.readString();
        unitAmount = in.readString();
        unitTaxAmount = in.readString();
        upcCode = in.readString();
        upcType = in.readString();
        url = in.readString();
    }

    @Deprecated
    public static final Creator<PayPalNativeCheckoutLineItem> CREATOR = new Creator<PayPalNativeCheckoutLineItem>() {
        @Deprecated
        @Override
        public PayPalNativeCheckoutLineItem createFromParcel(Parcel in) {
            return new PayPalNativeCheckoutLineItem(in);
        }

        @Deprecated
        @Override
        public PayPalNativeCheckoutLineItem[] newArray(int size) {
            return new PayPalNativeCheckoutLineItem[size];
        }
    };

    @Deprecated
    @Override
    public int describeContents() {
        return 0;
    }

    @Deprecated
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(description);
        parcel.writeString(imageUrl);
        parcel.writeString(kind);
        parcel.writeString(name);
        parcel.writeString(productCode);
        parcel.writeString(quantity);
        parcel.writeString(unitAmount);
        parcel.writeString(unitTaxAmount);
        parcel.writeString(upcCode);
        parcel.writeString(upcType);
        parcel.writeString(url);
    }

}
