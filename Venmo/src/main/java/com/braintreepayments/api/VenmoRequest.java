package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A VenmoRequest specifies options that contribute to the Venmo flow
 */
public class VenmoRequest implements Parcelable {

    private boolean shouldVault;
    private String profileId;
    private String displayName;
    private boolean collectCustomerShippingAddress;
    private boolean collectCustomerBillingAddress;
    private String totalAmount;
    private String subTotalAmount;
    private String discountAmount;
    private String taxAmount;
    private String shippingAmount;
    private ArrayList<VenmoLineItem> lineItems;
    private boolean isFinalAmount;

    private final @VenmoPaymentMethodUsage int paymentMethodUsage;

    /**
     * Request to tokenize a Venmo account.
     *
     * @param paymentMethodUsage {@link VenmoPaymentMethodUsage} for the tokenized Venmo account:
     *                           either multi-use or single use.
     */
    public VenmoRequest(@VenmoPaymentMethodUsage int paymentMethodUsage) {
        this.paymentMethodUsage = paymentMethodUsage;
        lineItems = new ArrayList<>();
    }

    /**
     * @param shouldVault Optional - Whether or not to automatically vault the Venmo Account.
     *                    Vaulting will only occur if a client token with a customer ID is being used.
     *                    Defaults to false.
     *
     *                    Also when shouldVault is true, {@link VenmoPaymentMethodUsage} on the
     *                    {@link VenmoRequest} must be set to
     *                    {@link VenmoPaymentMethodUsage.MULTI_USE}.
     */
    public void setShouldVault(boolean shouldVault) {
        this.shouldVault = shouldVault;
    }

    /**
     * @param profileId Optional - The Venmo profile ID to be used during payment authorization.
     *                  Customers will see the business name and logo associated with this Venmo
     *                  profile, and it will show up in the Venmo app as a "Connected Merchant".
     *                  Venmo profile IDs can be found in the Braintree Control Panel. Leaving this
     *                  `null` will use the default Venmo profile.
     */
    public void setProfileId(@Nullable String profileId) {
        this.profileId = profileId;
    }

    /**
     * @return Whether or not to automatically vault the Venmo Account.
     */
    public boolean getShouldVault() {
        return shouldVault;
    }

    /**
     * @return The Venmo profile ID to be used during payment authorization.
     */
    @Nullable
    public String getProfileId() {
        return profileId;
    }

    /**
     * @return {@link VenmoPaymentMethodUsage} for the tokenized Venmo account: either multi-use or
     * single use.
     */
    public @VenmoPaymentMethodUsage int getPaymentMethodUsage() {
        return paymentMethodUsage;
    }

    /**
     * @param displayName Optional. The business name that will be displayed in the Venmo app
     *                    payment approval screen. Only used by merchants onboarded as PayFast
     *                    channel partners.
     */
    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return The business name that will be displayed in the Venmo app payment approval screen.
     * Only used by merchants onboarded as PayFast channel partners.
     */
    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    String getPaymentMethodUsageAsString() {
        switch (paymentMethodUsage) {
            case VenmoPaymentMethodUsage.SINGLE_USE:
                return "SINGLE_USE";
            case VenmoPaymentMethodUsage.MULTI_USE:
                return "MULTI_USE";
            default:
                return null;
        }
    }

    /**
     * @param flag Optional. Whether or not shipping address should be collected and displayed on
     *             Venmo paysheet.
     */
    public void setCollectCustomerShippingAddress(boolean flag) {
        this.collectCustomerShippingAddress = flag;
    }

    /**
     * @return The boolean value of the flag that signifies whether customer shipping address will
     * be collected.
     */
    public boolean getCollectCustomerShippingAddress() {
        return collectCustomerShippingAddress;
    }

    String getCollectCustomerShippingAddressAsString() {
        return String.valueOf(this.collectCustomerShippingAddress);
    }

    /**
     * @param flag Optional. Whether or not billing address should be collected and displayed on
     *             Venmo paysheet.
     */
    public void setCollectCustomerBillingAddress(boolean flag) {
        this.collectCustomerBillingAddress = flag;
    }

    /**
     * @return The boolean value of the flag that signifies whether customer billing address will be
     * collected.
     */
    public boolean getCollectCustomerBillingAddress() {
        return collectCustomerBillingAddress;
    }

    String getCollectCustomerBillingAddressAsString() {
        return String.valueOf(this.collectCustomerBillingAddress);
    }

    /**
     * @param subTotalAmount Optional. The subtotal amount of the transaction, excluding taxes,
     *                       discounts, and shipping.
     *                       <p>
     *                       If this value is set, `totalAmount` must also be set.
     */
    public void setSubTotalAmount(@Nullable String subTotalAmount) {
        this.subTotalAmount = subTotalAmount;
    }

    /**
     * @return The subtotal amount of the transaction, excluding taxes, discounts, and shipping.
     */
    @Nullable
    public String getSubTotalAmount() {
        return subTotalAmount;
    }

    /**
     * @param shippingAmount Optional. The shipping amount charged for the transaction.
     *                       <p>
     *                       If this value is set, `totalAmount` must also be set.
     */
    public void setShippingAmount(@Nullable String shippingAmount) {
        this.shippingAmount = shippingAmount;
    }

    /**
     * @return he shipping amount charged for the transaction.
     */
    @Nullable
    public String getShippingAmount() {
        return shippingAmount;
    }

    /**
     * @param discountAmount Optional. The total discount amount applied on the transaction.
     *                       <p>
     *                       If this value is set, `totalAmount` must also be set.
     */
    public void setDiscountAmount(@Nullable String discountAmount) {
        this.discountAmount = discountAmount;
    }

    /**
     * @return The total discount amount applied on the transaction.
     */
    @Nullable
    public String getDiscountAmount() {
        return discountAmount;
    }

    /**
     * @param taxAmount Optional. The total tax amount applied to the transaction.
     *                  <p>
     *                  If this value is set, `totalAmount` must also be set.
     */
    public void setTaxAmount(@Nullable String taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * @return The total tax amount applied to the transaction.
     */
    @Nullable
    public String getTaxAmount() {
        return taxAmount;
    }

    /**
     * @param totalAmount Optional. The grand total amount of the transaction that will be displayed
     *                    on the paysheet.
     */
    public void setTotalAmount(@Nullable String totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * @return The grand total amount of the transaction that will be displayed on the paysheet.
     */
    @Nullable
    public String getTotalAmount() {
        return totalAmount;
    }

    /**
     * Optional: The line items for this transaction. Can include up to 249 line items.
     * <p>
     * If this value is set, `totalAmount` must also be set.
     *
     * @param lineItems a collection of {@link VenmoLineItem}
     */
    public void setLineItems(@NonNull Collection<VenmoLineItem> lineItems) {
        this.lineItems.clear();
        this.lineItems.addAll(lineItems);
    }

    /**
     * @return The line items for this transaction. Can include up to 249 line items.
     */
    @NonNull
    public ArrayList<VenmoLineItem> getLineItems() {
        return lineItems;
    }

    /**
     * @param isFinalAmount Optional - Indicates whether the purchase amount is the final amount.
     *                    Defaults to false.
     */
    public void setIsFinalAmount(boolean isFinalAmount) {
        this.isFinalAmount = isFinalAmount;
    }

    /**
     * @return The boolean value of the flag that signifies whether the purchase amount is the final amount.
     */
    public boolean getIsFinalAmount() {
        return isFinalAmount;
    }

    /**
     * @return Whether or not the purchase amount is the final amount as a string value.
     */
    String getIsFinalAmountAsString() {
        return String.valueOf(this.isFinalAmount);
    }

    protected VenmoRequest(Parcel in) {
        shouldVault = in.readByte() != 0;
        collectCustomerBillingAddress = in.readByte() != 0;
        collectCustomerShippingAddress = in.readByte() != 0;
        profileId = in.readString();
        displayName = in.readString();
        paymentMethodUsage = in.readInt();
        subTotalAmount = in.readString();
        discountAmount = in.readString();
        shippingAmount = in.readString();
        taxAmount = in.readString();
        totalAmount = in.readString();
        lineItems = in.createTypedArrayList(VenmoLineItem.CREATOR);
        isFinalAmount = in.readByte() != 0;
    }

    public static final Creator<VenmoRequest> CREATOR = new Creator<VenmoRequest>() {
        @Override
        public VenmoRequest createFromParcel(Parcel in) {
            return new VenmoRequest(in);
        }

        @Override
        public VenmoRequest[] newArray(int size) {
            return new VenmoRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (shouldVault ? 1 : 0));
        parcel.writeByte((byte) (collectCustomerBillingAddress ? 1 : 0));
        parcel.writeByte((byte) (collectCustomerShippingAddress ? 1 : 0));
        parcel.writeString(profileId);
        parcel.writeString(displayName);
        parcel.writeInt(paymentMethodUsage);
        parcel.writeString(subTotalAmount);
        parcel.writeString(discountAmount);
        parcel.writeString(shippingAmount);
        parcel.writeString(taxAmount);
        parcel.writeString(totalAmount);
        parcel.writeTypedList(lineItems);
        parcel.writeByte((byte) (isFinalAmount ? 1 : 0));
    }
}
