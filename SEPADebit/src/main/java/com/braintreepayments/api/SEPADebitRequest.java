package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Parameters for creating a SEPA Debit tokenization request.
 */
public class SEPADebitRequest {

    private String iban;
    private String customerId;
    private String bankReferenceToken;
    private SEPADebitMandateType mandateType;
    private PostalAddress billingAddress;
    private String merchantAccountId;

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
     * @return The bank reference token.
     */
    @NonNull
    public String getBankReferenceToken() {
        return bankReferenceToken;
    }

    /**
     * @param bankReferenceToken The bank reference token.
     */
    public void setBankReferenceToken(@NonNull String bankReferenceToken) {
        this.bankReferenceToken = bankReferenceToken;
    }

    /**
     * @return The mandate type - either recurring or one off.
     */
    @NonNull
    public SEPADebitMandateType getMandateType() {
        return mandateType;
    }

    /**
     * @param mandateType The mandate type - either recurring or one off.
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
