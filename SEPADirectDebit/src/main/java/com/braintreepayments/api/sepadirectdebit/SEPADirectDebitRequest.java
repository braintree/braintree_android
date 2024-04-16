package com.braintreepayments.api.sepadirectdebit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.braintreepayments.api.core.PostalAddress;

/**
 * Parameters for creating a SEPA Direct Debit tokenization request.
 */
public class SEPADirectDebitRequest {

    private String accountHolderName;
    private String iban;
    private String customerId;
    private SEPADirectDebitMandateType mandateType = SEPADirectDebitMandateType.ONE_OFF;
    private PostalAddress billingAddress;
    private String merchantAccountId;
    private String locale;

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
    @NonNull
    public SEPADirectDebitMandateType getMandateType() {
        return mandateType;
    }

    /**
     * Optional. If not set, defaults to ONE_OFF.
     * @param mandateType The {@link SEPADirectDebitMandateType}.
     */
    public void setMandateType(@NonNull SEPADirectDebitMandateType mandateType) {
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

    /**
     * @return A locale code to use for creating a mandate.
     */
    @Nullable
    public String getLocale() {
        return locale;
    }

    /**
     * Optional.
     * @param locale A locale code to use for creating a mandate.
     *
     * @see <a href="https://developer.paypal.com/reference/locale-codes/">Documentation</a>
     * for possible values. Locale code should be supplied as a BCP-47 formatted locale code.
     */
    public void setLocale(@Nullable String locale) {
        this.locale = locale;
    }
}
