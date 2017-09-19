package com.braintreepayments.api.models;

import android.support.annotation.Nullable;

import com.braintreepayments.api.BraintreeFragment;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants.BillingAddressFormat;

/**
 * Represents the parameters that are needed to use the Google Payments API.
 */
public class GooglePaymentsRequest {

    private TransactionInfo mTransactionInfo;
    private Boolean mEmailRequired = null;
    private Boolean mPhoneNumberRequired = null;
    private Boolean mBillingAddressRequired = null;
    private Integer mBillingAddressFormat;
    private Boolean mShippingAddressRequired = null;
    private ShippingAddressRequirements mShippingAddressRequirements;
    private Boolean mAllowPrepaidCards = null;
    private Boolean mUiRequired = null;

    /**
     * Details and the price of the transaction. Required.
     *
     * @param transactionInfo See {@link TransactionInfo}.
     * @return {@link GooglePaymentsRequest}
     */
    public GooglePaymentsRequest transactionInfo(TransactionInfo transactionInfo) {
        mTransactionInfo = transactionInfo;
        return this;
    }

    /**
     * Optional.
     *
     * @param emailRequired {@code true} if the buyer's email address is required to be returned, {@code false} otherwise.
     * @return {@link GooglePaymentsRequest}
     */
    public GooglePaymentsRequest emailRequired(boolean emailRequired) {
        mEmailRequired = emailRequired;
        return this;
    }

    /**
     * Optional.
     *
     * @param phoneNumberRequired {@code true} if the buyer's phone number is required to be returned as part of the
     * billing address and shipping address, {@code false} otherwise.
     * @return {@link GooglePaymentsRequest}
     */
    public GooglePaymentsRequest phoneNumberRequired(boolean phoneNumberRequired) {
        mPhoneNumberRequired = phoneNumberRequired;
        return this;
    }

    /**
     * Optional.
     *
     * @param billingAddressRequired {@code true} if the buyer's billing address is required to be returned,
     * {@code false} otherwise.
     * @return {@link GooglePaymentsRequest}
     */
    public GooglePaymentsRequest billingAddressRequired(boolean billingAddressRequired) {
        mBillingAddressRequired = billingAddressRequired;
        return this;
    }

    /**
     * Optional.
     *
     * @param billingAddressFormat the billing address format to return. {@link BillingAddressFormat}
     * @return {@link GooglePaymentsRequest}
     */
    public GooglePaymentsRequest billingAddressFormat(@BillingAddressFormat int billingAddressFormat) {
        mBillingAddressFormat = billingAddressFormat;
        return this;
    }

    /**
     * Optional.
     *
     * @param shippingAddressRequired {@code true} if the buyer's shipping address is required to be returned,
     * {@code false} otherwise.
     * @return {@link GooglePaymentsRequest}
     */
    public GooglePaymentsRequest shippingAddressRequired(boolean shippingAddressRequired) {
        mShippingAddressRequired = shippingAddressRequired;
        return this;
    }

    /**
     * Optional.
     *
     * @param shippingAddressRequirements the shipping address requirements. {@link ShippingAddressRequirements}
     * @return {@link GooglePaymentsRequest}
     */
    public GooglePaymentsRequest shippingAddressRequirements(ShippingAddressRequirements shippingAddressRequirements) {
        mShippingAddressRequirements = shippingAddressRequirements;
        return this;
    }

    /**
     * Optional.
     *
     * @param allowPrepaidCards {@code true} prepaid cards are allowed, {@code false} otherwise.
     * @return {@link GooglePaymentsRequest}
     */
    public GooglePaymentsRequest allowPrepaidCards(boolean allowPrepaidCards) {
        mAllowPrepaidCards = allowPrepaidCards;
        return this;
    }

    /**
     * When this is set to false,
     * {@link com.braintreepayments.api.GooglePayments#requestPayment(BraintreeFragment, GooglePaymentsRequest)}
     * will attempt to skip the UI and directly return the data from the buyer's previous selection. The merchant must
     * be whitelisted for not showing UI. Please contact Google if you think your use case would benefit from skipping UI.
     *
     * Optional.
     *
     * @param uiRequired {@code false} if the UI should not be shown, {@code true} otherwise.
     * @return {@link GooglePaymentsRequest}
     */
    public GooglePaymentsRequest uiRequired(boolean uiRequired) {
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
}
