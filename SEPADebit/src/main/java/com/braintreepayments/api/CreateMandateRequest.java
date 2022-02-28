package com.braintreepayments.api;

class CreateMandateRequest {

      private String accountHolderName;
      private String customerId;
      private String iban;
      private SEPADebitMandateType mandateType;
      private PostalAddress billingAddress;
      private String cancelUrl;
      private String returnUrl;
      private String merchantAccountId;

      String getAccountHolderName() {
            return accountHolderName;
      }

      void setAccountHolderName(String accountHolderName) {
            this.accountHolderName = accountHolderName;
      }

      String getCustomerId() {
            return customerId;
      }

      void setCustomerId(String customerId) {
            this.customerId = customerId;
      }

      String getIban() {
            return iban;
      }

      void setIban(String iban) {
            this.iban = iban;
      }

      SEPADebitMandateType getMandateType() {
            return mandateType;
      }

      void setMandateType(SEPADebitMandateType mandateType) {
            this.mandateType = mandateType;
      }

      PostalAddress getBillingAddress() {
            return billingAddress;
      }

      void setBillingAddress(PostalAddress billingAddress) {
            this.billingAddress = billingAddress;
      }

      String getCancelUrl() {
            return cancelUrl;
      }

      void setCancelUrl(String cancelUrl) {
            this.cancelUrl = cancelUrl;
      }

      String getReturnUrl() {
            return returnUrl;
      }

      void setReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
      }

      String getMerchantAccountId() {
            return merchantAccountId;
      }

      void setMerchantAccountId(String merchantAccountId) {
            this.merchantAccountId = merchantAccountId;
      }
}
