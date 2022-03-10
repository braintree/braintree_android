package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Parameters for creating a SEPA Direct Debit tokenization request.
 */
public class SEPADirectDebitRequest {

    private String accountHolderName;
    private String iban;
    private String customerId;
    private SEPADirectDebitMandateType mandateType;
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
     * Required.
     * @param accountHolderName The account holder name.
     */
    public void setAccountHolderName(@Nullable String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    /**
     * @return The full IBAN.
     */
    @Nullable
    public String getIban() {
        return iban;
    }

    /**
     * Required.
     * @param iban The full IBAN.
     */
    public void setIban(@Nullable String iban) {
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
     * Required.
     * @param customerId The customer ID.
     */
    public void setCustomerId(@Nullable String customerId) {
        this.customerId = customerId;
    }

    /**
     * @return The {@link SEPADirectDebitMandateType}.
     */
    @Nullable
    public SEPADirectDebitMandateType getMandateType() {
        return mandateType;
    }

    /**
     * Required.
     * @param mandateType The {@link SEPADebitMandateType}.
     */
    public void setMandateType(@Nullable SEPADirectDebitMandateType mandateType) {
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
     * Required.
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
