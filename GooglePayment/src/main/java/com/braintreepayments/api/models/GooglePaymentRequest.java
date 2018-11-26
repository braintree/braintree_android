package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.GooglePayment;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants.BillingAddressFormat;

/**
 * Represents the parameters that are needed to use the Google Payments API.
 */
public class GooglePaymentRequest implements Parcelable {

    private TransactionInfo mTransactionInfo;
    private Boolean mEmailRequired = null;
    private Boolean mPhoneNumberRequired = null;
    private Boolean mBillingAddressRequired = null;
    private Integer mBillingAddressFormat;
    private Boolean mShippingAddressRequired = null;
    private ShippingAddressRequirements mShippingAddressRequirements;
    private Boolean mAllowPrepaidCards = null;
    private Boolean mUiRequired = null;

    public GooglePaymentRequest() {}

    /**
     * Details and the price of the transaction. Required.
     *
     * @param transactionInfo See {@link TransactionInfo}.
     * @return {@link GooglePaymentRequest}
     */
    public GooglePaymentRequest transactionInfo(TransactionInfo transactionInfo) {
        mTransactionInfo = transactionInfo;
        return this;
    }

    /**
     * Optional.
     *
     * @param emailRequired {@code true} if the buyer's email address is required to be returned, {@code false} otherwise.
     * @return {@link GooglePaymentRequest}
     */
    public GooglePaymentRequest emailRequired(boolean emailRequired) {
        mEmailRequired = emailRequired;
        return this;
    }

    /**
     * Optional.
     *
     * @param phoneNumberRequired {@code true} if the buyer's phone number is required to be returned as part of the
     * billing address and shipping address, {@code false} otherwise.
     * @return {@link GooglePaymentRequest}
     */
    public GooglePaymentRequest phoneNumberRequired(boolean phoneNumberRequired) {
        mPhoneNumberRequired = phoneNumberRequired;
        return this;
    }

    /**
     * Optional.
     *
     * @param billingAddressRequired {@code true} if the buyer's billing address is required to be returned,
     * {@code false} otherwise.
     * @return {@link GooglePaymentRequest}
     */
    public GooglePaymentRequest billingAddressRequired(boolean billingAddressRequired) {
        mBillingAddressRequired = billingAddressRequired;
        return this;
    }

    /**
     * Optional.
     *
     * @param billingAddressFormat the billing address format to return. {@link BillingAddressFormat}
     * @return {@link GooglePaymentRequest}
     */
    public GooglePaymentRequest billingAddressFormat(@BillingAddressFormat int billingAddressFormat) {
        mBillingAddressFormat = billingAddressFormat;
        return this;
    }

    /**
     * Optional.
     *
     * @param shippingAddressRequired {@code true} if the buyer's shipping address is required to be returned,
     * {@code false} otherwise.
     * @return {@link GooglePaymentRequest}
     */
    public GooglePaymentRequest shippingAddressRequired(boolean shippingAddressRequired) {
        mShippingAddressRequired = shippingAddressRequired;
        return this;
    }

    /**
     * Optional.
     *
     * @param shippingAddressRequirements the shipping address requirements. {@link ShippingAddressRequirements}
     * @return {@link GooglePaymentRequest}
     */
    public GooglePaymentRequest shippingAddressRequirements(ShippingAddressRequirements shippingAddressRequirements) {
        mShippingAddressRequirements = shippingAddressRequirements;
        return this;
    }

    /**
     * Optional.
     *
     * @param allowPrepaidCards {@code true} prepaid cards are allowed, {@code false} otherwise.
     * @return {@link GooglePaymentRequest}
     */
    public GooglePaymentRequest allowPrepaidCards(boolean allowPrepaidCards) {
        mAllowPrepaidCards = allowPrepaidCards;
        return this;
    }

    /**
     * When this is set to false,
     * {@link GooglePayment#requestPayment(BraintreeFragment, GooglePaymentRequest)}
     * will attempt to skip the UI and directly return the data from the buyer's previous selection. The merchant must
     * be whitelisted for not showing UI. Please contact Google if you think your use case would benefit from skipping UI.
     *
     * Optional.
     *
     * @param uiRequired {@code false} if the UI should not be shown, {@code true} otherwise.
     * @return {@link GooglePaymentRequest}
     */
    public GooglePaymentRequest uiRequired(boolean uiRequired) {
        mUiRequired = uiRequired;
        return this;
    }

    public TransactionInfo getTransactionInfo() {
        return mTransactionInfo;
    }

    @Nullable
    public Boolean isEmailRequired() {
        return mEmailRequired;
    }

    @Nullable
    public Boolean isPhoneNumberRequired() {
        return mPhoneNumberRequired;
    }

    @Nullable
    public Boolean isBillingAddressRequired() {
        return mBillingAddressRequired;
    }

    @Nullable
    @BillingAddressFormat
    public Integer getBillingAddressFormat() {
        return mBillingAddressFormat;
    }

    @Nullable
    public Boolean isShippingAddressRequired() {
        return mShippingAddressRequired;
    }

    @Nullable
    public ShippingAddressRequirements getShippingAddressRequirements() {
        return mShippingAddressRequirements;
    }

    @Nullable
    public Boolean getAllowPrepaidCards() {
        return mAllowPrepaidCards;
    }

    @Nullable
    public Boolean isUiRequired() {
        return mUiRequired;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mTransactionInfo, flags);
        dest.writeByte((byte) (mEmailRequired == null ? 0 : mEmailRequired ? 1 : 2));
        dest.writeByte((byte) (mPhoneNumberRequired == null ? 0 : mPhoneNumberRequired ? 1 : 2));
        dest.writeByte((byte) (mBillingAddressRequired == null ? 0 : mBillingAddressRequired ? 1 : 2));
        if (mBillingAddressFormat == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(mBillingAddressFormat);
        }
        dest.writeByte((byte) (mShippingAddressRequired == null ? 0 : mShippingAddressRequired ? 1 : 2));
        dest.writeParcelable(mShippingAddressRequirements, flags);
        dest.writeByte((byte) (mAllowPrepaidCards == null ? 0 : mAllowPrepaidCards ? 1 : 2));
        dest.writeByte((byte) (mUiRequired == null ? 0 : mUiRequired ? 1 : 2));
    }

    protected GooglePaymentRequest(Parcel in) {
        mTransactionInfo = in.readParcelable(TransactionInfo.class.getClassLoader());
        byte emailRequired = in.readByte();
        mEmailRequired = emailRequired == 0 ? null : emailRequired == 1;
        byte phoneNumberRequired = in.readByte();
        mPhoneNumberRequired = phoneNumberRequired == 0 ? null : phoneNumberRequired == 1;
        byte billingAddressRequired = in.readByte();
        mBillingAddressRequired = billingAddressRequired == 0 ? null : billingAddressRequired == 1;
        if (in.readByte() == 0) {
            mBillingAddressFormat = null;
        } else {
            mBillingAddressFormat = in.readInt();
        }
        byte shippingAddressRequired = in.readByte();
        mShippingAddressRequired = shippingAddressRequired == 0 ? null : shippingAddressRequired == 1;
        mShippingAddressRequirements = in.readParcelable(ShippingAddressRequirements.class.getClassLoader());
        byte allowPrepaidCards = in.readByte();
        mAllowPrepaidCards = allowPrepaidCards == 0 ? null : allowPrepaidCards == 1;
        byte uiRequired = in.readByte();
        mUiRequired = uiRequired == 0 ? null : uiRequired == 1;
    }

    public static final Creator<GooglePaymentRequest> CREATOR = new Creator<GooglePaymentRequest>() {
        @Override
        public GooglePaymentRequest createFromParcel(Parcel in) {
            return new GooglePaymentRequest(in);
        }

        @Override
        public GooglePaymentRequest[] newArray(int size) {
            return new GooglePaymentRequest[size];
        }
    };
}
