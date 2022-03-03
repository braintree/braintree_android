package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Parameters for creating a SEPA Debit tokenization request.
 */
public class SEPADebitRequest {

    private String accountHolderName;
    private String iban;
    private String customerId;
    private SEPADebitMandateType mandateType;
    private PostalAddress billingAddress;
    private String merchantAccountId;

    /**
     * @return The account holder name
     */
    @Nullable
    public String getAccountHolderName() {
        return accountHolderName;
    }

    /**
     * @param accountHolderName The account holder name.
     */
    public void setAccountHolderName(@Nullable String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    /**
     * @return The full IBAN.
     */
    @NonNull
    public String getIban() {
        return iban;
    }

    /**
     * @param iban The full IBAN.
     */
    public void setIban(@NonNull String iban) {
        this.iban = iban;
    }

    /**
     * @return The customer ID.
     */
    @Nullable
    public String getCustomerId() {
        return customerId;
    }

    /**
     * @param customerId The customer ID.
     */
    public void setCustomerId(@Nullable String customerId) {
        this.customerId = customerId;
    }

    /**
     * @return The {@link SEPADebitMandateType}.
     */
    @NonNull
    public SEPADebitMandateType getMandateType() {
        return mandateType;
    }

    /**
     * @param mandateType The {@link SEPADebitMandateType}.
     */
    public void setMandateType(@NonNull SEPADebitMandateType mandateType) {
        this.mandateType = mandateType;
    }

    /**
     * @return The user's billing address.
     */
    @Nullable
    public PostalAddress getBillingAddress() {
        return billingAddress;
    }

    /**
     * Optional.
     * @param billingAddress The user's billing address.
     */
    public void setBillingAddress(@Nullable PostalAddress billingAddress) {
        this.billingAddress = billingAddress;
    }

    /**
     * @return A non-default merchant account to use for tokenization.
     */
    @Nullable
    public String getMerchantAccountId() {
        return merchantAccountId;
    }

    /**
     * Optional.
     * @param merchantAccountId A non-default merchant account to use for tokenization.
     */
    public void setMerchantAccountId(@Nullable String merchantAccountId) {
        this.merchantAccountId = merchantAccountId;
    }
}
