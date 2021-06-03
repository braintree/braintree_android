package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class containing additional information for ThreeDSecure 2.0 Requests
 */
public class ThreeDSecureAdditionalInformation implements Parcelable {

    private ThreeDSecurePostalAddress shippingAddress;
    private String shippingMethodIndicator;
    private String productCode;
    private String deliveryTimeframe;
    private String deliveryEmail;
    private String reorderIndicator;
    private String preorderIndicator;
    private String preorderDate;
    private String giftCardAmount;
    private String giftCardCurrencyCode;
    private String giftCardCount;
    private String accountAgeIndicator;
    private String accountCreateDate;
    private String accountChangeIndicator;
    private String accountChangeDate;
    private String accountPwdChangeIndicator;
    private String accountPwdChangeDate;
    private String shippingAddressUsageIndicator;
    private String shippingAddressUsageDate;
    private String transactionCountDay;
    private String transactionCountYear;
    private String addCardAttempts;
    private String accountPurchases;
    private String fraudActivity;
    private String shippingNameIndicator;
    private String paymentAccountIndicator;
    private String paymentAccountAge;
    private String addressMatch;
    private String accountId;
    private String ipAddress;
    private String orderDescription;
    private String taxAmount;
    private String userAgent;
    private String authenticationIndicator;
    private String installment;
    private String purchaseDate;
    private String recurringEnd;
    private String recurringFrequency;
    private String sdkMaxTimeout;
    private String workPhoneNumber;

    public ThreeDSecureAdditionalInformation() {}

    /**
     * Optional. Set the shipping address
     *
     * @param shippingAddress The shipping address used for verification.
     *
     * */
    public void setShippingAddress(@Nullable ThreeDSecurePostalAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    /**
     * Optional. The 2-digit string indicating the shipping method chosen for the transaction
     *
     * Possible Values:
     * 01 Ship to cardholder billing address
     * 02 Ship to another verified address on file with merchant
     * 03 Ship to address that is different than billing address
     * 04 Ship to store (store address should be populated on request)
     * 05 Digital goods
     * 06 Travel and event tickets, not shipped
     * 07 Other
     */
    public void setShippingMethodIndicator(@Nullable String shippingMethodIndicator) {
        this.shippingMethodIndicator = shippingMethodIndicator;
    }

    /**
     * Optional. The 3-letter string representing the merchant product code

     * Possible Values:
     * AIR Airline
     * GEN General Retail
     * DIG Digital Goods
     * SVC Services
     * RES Restaurant
     * TRA Travel
     * DSP Cash Dispensing
     * REN Car Rental
     * GAS Fueld
     * LUX Luxury Retail
     * ACC Accommodation Retail
     * TBD Other
     */
    public void setProductCode(@Nullable String productCode) {
        this.productCode = productCode;
    }

    /**
     * Optional. The 2-digit number indicating the delivery timeframe

     * Possible values:
     * 01 Electronic delivery
     * 02 Same day shipping
     * 03 Overnight shipping
     * 04 Two or more day shipping
     */
    public void setDeliveryTimeframe(@Nullable String deliveryTimeframe) {
        this.deliveryTimeframe = deliveryTimeframe;
    }

    /**
     * Optional. For electronic delivery, email address to which the merchandise was delivered
     */
    public void setDeliveryEmail(@Nullable String deliveryEmail) {
        this.deliveryEmail = deliveryEmail;
    }

    /**
     * Optional. The 2-digit number indicating whether the cardholder is reordering previously purchased merchandise

     * Possible values:
     * 01 First time ordered
     * 02 Reordered
     */
    public void setReorderIndicator(@Nullable String reorderIndicator) {
        this.reorderIndicator = reorderIndicator;
    }

    /**
     * Optional. The 2-digit number indicating whether the cardholder is placing an order with a future availability or release date

     * Possible values:
     * 01 Merchandise available
     * 02 Future availability
     */
    public void setPreorderIndicator(@Nullable String preorderIndicator) {
        this.preorderIndicator = preorderIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating expected date that a pre-ordered purchase will be available
     */
    public void setPreorderDate(@Nullable String preorderDate) {
        this.preorderDate = preorderDate;
    }

    /**
     * Optional. The purchase amount total for prepaid gift cards in major units
     */
    public void setGiftCardAmount(@Nullable String giftCardAmount) {
        this.giftCardAmount = giftCardAmount;
    }

    /**
     * Optional. ISO 4217 currency code for the gift card purchased
     */
    public void setGiftCardCurrencyCode(@Nullable String giftCardCurrencyCode) {
        this.giftCardCurrencyCode = giftCardCurrencyCode;
    }

    /**
     * Optional. Total count of individual prepaid gift cards purchased
     */
    public void setGiftCardCount(@Nullable String giftCardCount) {
        this.giftCardCount = giftCardCount;
    }

    /**
     * Optional. The 2-digit value representing the length of time cardholder has had account.

     * Possible values:
     * 01 No account
     * 02 Created during transaction
     * 03 Less than 30 days
     * 04 30-60 days
     * 05 More than 60 days
     */
    public void setAccountAgeIndicator(@Nullable String accountAgeIndicator) {
        this.accountAgeIndicator = accountAgeIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the cardholder opened the account.
     */
    public void setAccountCreateDate(@Nullable String accountCreateDate) {
        this.accountCreateDate = accountCreateDate;
    }

    /**
     * Optional. The 2-digit value representing the length of time since the last change to the cardholder account. This includes shipping address, new payment account or new user added.

     * Possible values:
     * 01 Changed during transaction
     * 02 Less than 30 days
     * 03 30-60 days
     * 04 More than 60 days
     */
    public void setAccountChangeIndicator(@Nullable String accountChangeIndicator) {
        this.accountChangeIndicator = accountChangeIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the cardholder's account was last changed. This includes changes to the billing or shipping address, new payment accounts or new users added.
     */
    public void setAccountChangeDate(@Nullable String accountChangeDate) {
        this.accountChangeDate = accountChangeDate;
    }

    /**
     * Optional. The 2-digit value representing the length of time since the cardholder changed or reset the password on the account.

     * Possible values:
     * 01 No change
     * 02 Changed during transaction
     * 03 Less than 30 days
     * 04 30-60 days
     * 05 More than 60 days
     */
    public void setAccountPwdChangeIndicator(@Nullable String accountPwdChangeIndicator) {
        this.accountPwdChangeIndicator = accountPwdChangeIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the cardholder last changed or reset password on account.
     */
    public void setAccountPwdChangeDate(@Nullable String accountPwdChangeDate) {
        this.accountPwdChangeDate = accountPwdChangeDate;
    }

    /**
     * Optional. The 2-digit value indicating when the shipping address used for transaction was first used.

     * Possible values:
     * 01 This transaction
     * 02 Less than 30 days
     * 03 30-60 days
     * 04 More than 60 days
     */
    public void setShippingAddressUsageIndicator(@Nullable String shippingAddressUsageIndicator) {
        this.shippingAddressUsageIndicator = shippingAddressUsageIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date when the shipping address used for this transaction was first used.
     */
    public void setShippingAddressUsageDate(@Nullable String shippingAddressUsageDate) {
        this.shippingAddressUsageDate = shippingAddressUsageDate;
    }

    /**
     * Optional. Number of transactions (successful or abandoned) for this cardholder account within the last 24 hours.
     */
    public void setTransactionCountDay(@Nullable String transactionCountDay) {
        this.transactionCountDay = transactionCountDay;
    }

    /**
     * Optional. Number of transactions (successful or abandoned) for this cardholder account within the last year.
     */
    public void setTransactionCountYear(@Nullable String transactionCountYear) {
        this.transactionCountYear = transactionCountYear;
    }

    /**
     * Optional. Number of add card attempts in the last 24 hours.
     */
    public void setAddCardAttempts(@Nullable String addCardAttempts) {
        this.addCardAttempts = addCardAttempts;
    }

    /**
     * Optional. Number of purchases with this cardholder account during the previous six months.
     */
    public void setAccountPurchases(@Nullable String accountPurchases) {
        this.accountPurchases = accountPurchases;
    }

    /**
     * Optional. The 2-digit value indicating whether the merchant experienced suspicious activity (including previous fraud) on the account.

     * Possible values:
     * 01 No suspicious activity
     * 02 Suspicious activity observed
     */
    public void setFraudActivity(@Nullable String fraudActivity) {
        this.fraudActivity = fraudActivity;
    }

    /**
     * Optional. The 2-digit value indicating if the cardholder name on the account is identical to the shipping name used for the transaction.

     * Possible values:
     * 01 Account name identical to shipping name
     * 02 Account name different than shipping name
     */
    public void setShippingNameIndicator(@Nullable String shippingNameIndicator) {
        this.shippingNameIndicator = shippingNameIndicator;
    }

    /**
     * Optional. The 2-digit value indicating the length of time that the payment account was enrolled in the merchant account.

     * Possible values:
     * 01 No account (guest checkout)
     * 02 During the transaction
     * 03 Less than 30 days
     * 04 30-60 days
     * 05 More than 60 days
     */
    public void setPaymentAccountIndicator(@Nullable String paymentAccountIndicator) {
        this.paymentAccountIndicator = paymentAccountIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the payment account was added to the cardholder account.
     */
    public void setPaymentAccountAge(@Nullable String paymentAccountAge) {
        this.paymentAccountAge = paymentAccountAge;
    }

    /**
     * Optional. The 1-character value (Y/N) indicating whether cardholder billing and shipping addresses match.
     */
    public void setAddressMatch(@Nullable String addressMatch) {
        this.addressMatch = addressMatch;
    }

    /**
     * Optional. Additional cardholder account information.
     */
    public void setAccountId(@Nullable String accountId) {
        this.accountId = accountId;
    }

    /**
     * Optional. The IP address of the consumer. IPv4 and IPv6 are supported.
     */
    public void setIpAddress(@Nullable String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Optional. Brief description of items purchased.
     */
    public void setOrderDescription(@Nullable String orderDescription) {
        this.orderDescription = orderDescription;
    }

    /**
     * Optional. Unformatted tax amount without any decimalization (ie. $123.67 = 12367).
     */
    public void setTaxAmount(@Nullable String taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * Optional. The exact content of the HTTP user agent header.
     */
    public void setUserAgent(@Nullable String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Optional. The 2-digit number indicating the type of authentication request.

     * Possible values:
     * 02 Recurring transaction
     * 03 Installment transaction
     */
    public void setAuthenticationIndicator(@Nullable String authenticationIndicator) {
        this.authenticationIndicator = authenticationIndicator;
    }

    /**
     * Optional.  An integer value greater than 1 indicating the maximum number of permitted authorizations for installment payments.
     */
    public void setInstallment(@Nullable String installment) {
        this.installment = installment;
    }

    /**
     * Optional. The 14-digit number (format: YYYYMMDDHHMMSS) indicating the date in UTC of original purchase.
     */
    public void setPurchaseDate(@Nullable String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date after which no further recurring authorizations should be performed..
     */
    public void setRecurringEnd(@Nullable String recurringEnd) {
        this.recurringEnd = recurringEnd;
    }

    /**
     * Optional. Integer value indicating the minimum number of days between recurring authorizations. A frequency of monthly is indicated by the value 28. Multiple of 28 days will be used to indicate months (ex. 6 months = 168).
     */
    public void setRecurringFrequency(@Nullable String recurringFrequency) {
        this.recurringFrequency = recurringFrequency;
    }

    /**
     * Optional. The 2-digit number of minutes (minimum 05) to set the maximum amount of time for all 3DS 2.0 messages to be communicated between all components.
     */
    public void setSdkMaxTimeout(@Nullable String sdkMaxTimeout) {
        this.sdkMaxTimeout = sdkMaxTimeout;
    }

    /**
     * Optional. The work phone number used for verification. Only numbers; remove dashes, parenthesis and other characters.
     */
    public void setWorkPhoneNumber(@Nullable String workPhoneNumber) {
        this.workPhoneNumber = workPhoneNumber;
    }

    /**
     * @return shipping address
     */
    @Nullable
    public ThreeDSecurePostalAddress getShippingAddress() {
        return shippingAddress;
    }

    /**
     * @return shipping method indicator
     */
    @Nullable
    public String getShippingMethodIndicator() {
        return shippingMethodIndicator;
    }

    /**
     * @return product code
     */
    @Nullable
    public String getProductCode() {
        return productCode;
    }

    /**
     * @return delivery time frame
     */
    @Nullable
    public String getDeliveryTimeframe() {
        return deliveryTimeframe;
    }

    /**
     * @return delivery email
     */
    @Nullable
    public String getDeliveryEmail() {
        return deliveryEmail;
    }

    /**
     * @return reorder indicator
     */
    @Nullable
    public String getReorderIndicator() {
        return reorderIndicator;
    }

    /**
     * @return preorder indicator
     */
    @Nullable
    public String getPreorderIndicator() {
        return preorderIndicator;
    }

    /**
     * @return Preorder date
     */
    @Nullable
    public String getPreorderDate() {
        return preorderDate;
    }

    /**
     * @return Gift card amount
     */
    @Nullable
    public String getGiftCardAmount() {
        return giftCardAmount;
    }

    /**
     * @return Gift card currency code
     */
    @Nullable
    public String getGiftCardCurrencyCode() {
        return giftCardCurrencyCode;
    }

    /**
     * @return Gift card count
     */
    @Nullable
    public String getGiftCardCount() {
        return giftCardCount;
    }

    /**
     * @return Account age indicator
     */
    @Nullable
    public String getAccountAgeIndicator() {
        return accountAgeIndicator;
    }

    /**
     * @return Account create date
     */
    @Nullable
    public String getAccountCreateDate() {
        return accountCreateDate;
    }

    /**
     * @return Account change indicator
     */
    @Nullable
    public String getAccountChangeIndicator() {
        return accountChangeIndicator;
    }

    /**
     * @return Account change date
     */
    @Nullable
    public String getAccountChangeDate() {
        return accountChangeDate;
    }

    /**
     * @return Account password change indicator
     */
    @Nullable
    public String getAccountPwdChangeIndicator() {
        return accountPwdChangeIndicator;
    }

    /**
     * @return Account password change date
     */
    @Nullable
    public String getAccountPwdChangeDate() {
        return accountPwdChangeDate;
    }

    /**
     * @return Shipping address usage indicator
     */
    @Nullable
    public String getShippingAddressUsageIndicator() {
        return shippingAddressUsageIndicator;
    }

    /**
     * @return Shipping address usage date
     */
    @Nullable
    public String getShippingAddressUsageDate() {
        return shippingAddressUsageDate;
    }

    /**
     * @return Transaction count day
     */
    @Nullable
    public String getTransactionCountDay() {
        return transactionCountDay;
    }

    /**
     * @return Transaction count year
     */
    @Nullable
    public String getTransactionCountYear() {
        return transactionCountYear;
    }

    /**
     * @return Add card attempts
     */
    @Nullable
    public String getAddCardAttempts() {
        return addCardAttempts;
    }

    /**
     * @return Account purchases
     */
    @Nullable
    public String getAccountPurchases() {
        return accountPurchases;
    }

    /**
     * @return Fraud activity
     */
    @Nullable
    public String getFraudActivity() {
        return fraudActivity;
    }

    /**
     * @return Shipping name indicator
     */
    @Nullable
    public String getShippingNameIndicator() {
        return shippingNameIndicator;
    }

    /**
     * @return Payment account indicator
     */
    @Nullable
    public String getPaymentAccountIdicator() {
        return paymentAccountIndicator;
    }

    /**
     * @return Payment account age
     */
    @Nullable
    public String getPaymentAccountAge() {
        return paymentAccountAge;
    }

    /**
     * @return Address match
     */
    @Nullable
    public String getAddressMatch() {
        return addressMatch;
    }

    /**
     * @return Account ID
     */
    @Nullable
    public String getAccountId() {
        return accountId;
    }

    /**
     * @return Ip address
     */
    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @return Order description
     */
    @Nullable
    public String getOrderDescription() {
        return orderDescription;
    }

    /**
     * @return Tax amount
     */
    @Nullable
    public String getTaxAmount() {
        return taxAmount;
    }

    /**
     * @return User agent
     */
    @Nullable
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * @return Authentication indicator
     */
    @Nullable
    public String getAuthenticationIndicator() {
        return authenticationIndicator;
    }

    /**
     * @return Installment
     */
    @Nullable
    public String getInstallment() {
        return installment;
    }

    /**
     * @return Purchase date
     */
    @Nullable
    public String getPurchaseDate() {
        return purchaseDate;
    }

    /**
     * @return Recurring end
     */
    @Nullable
    public String getRecurringEnd() {
        return recurringEnd;
    }

    /**
     * @return Recurring frequency
     */
    @Nullable
    public String getRecurringFrequency() {
        return recurringFrequency;
    }

    /**
     * @return SDK max timeout
     */
    @Nullable
    public String getSdkMaxTimeout() {
        return sdkMaxTimeout;
    }

    /**
     * @return Work phone number
     */
    @Nullable
    public String getWorkPhoneNumber() {
        return workPhoneNumber;
    }

    public ThreeDSecureAdditionalInformation(Parcel in) {
        shippingAddress = in.readParcelable(ThreeDSecurePostalAddress.class.getClassLoader());
        shippingMethodIndicator = in.readString();
        productCode = in.readString();
        deliveryTimeframe = in.readString();
        deliveryEmail = in.readString();
        reorderIndicator = in.readString();
        preorderIndicator = in.readString();
        preorderDate = in.readString();
        giftCardAmount = in.readString();
        giftCardCurrencyCode = in.readString();
        giftCardCount = in.readString();
        accountAgeIndicator = in.readString();
        accountCreateDate = in.readString();
        accountChangeIndicator = in.readString();
        accountChangeDate = in.readString();
        accountPwdChangeIndicator = in.readString();
        accountPwdChangeDate = in.readString();
        shippingAddressUsageIndicator = in.readString();
        shippingAddressUsageDate = in.readString();
        transactionCountDay = in.readString();
        transactionCountYear = in.readString();
        addCardAttempts = in.readString();
        accountPurchases = in.readString();
        fraudActivity = in.readString();
        shippingNameIndicator = in.readString();
        paymentAccountIndicator = in.readString();
        paymentAccountAge = in.readString();
        addressMatch = in.readString();
        accountId = in.readString();
        ipAddress = in.readString();
        orderDescription = in.readString();
        taxAmount = in.readString();
        userAgent = in.readString();
        authenticationIndicator = in.readString();
        installment = in.readString();
        purchaseDate = in.readString();
        recurringEnd = in.readString();
        recurringFrequency = in.readString();
        sdkMaxTimeout = in.readString();
        workPhoneNumber = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(shippingAddress, flags);
        dest.writeString(shippingMethodIndicator);
        dest.writeString(productCode);
        dest.writeString(deliveryTimeframe);
        dest.writeString(deliveryEmail);
        dest.writeString(reorderIndicator);
        dest.writeString(preorderIndicator);
        dest.writeString(preorderDate);
        dest.writeString(giftCardAmount);
        dest.writeString(giftCardCurrencyCode);
        dest.writeString(giftCardCount);
        dest.writeString(accountAgeIndicator);
        dest.writeString(accountCreateDate);
        dest.writeString(accountChangeIndicator);
        dest.writeString(accountChangeDate);
        dest.writeString(accountPwdChangeIndicator);
        dest.writeString(accountPwdChangeDate);
        dest.writeString(shippingAddressUsageIndicator);
        dest.writeString(shippingAddressUsageDate);
        dest.writeString(transactionCountDay);
        dest.writeString(transactionCountYear);
        dest.writeString(addCardAttempts);
        dest.writeString(accountPurchases);
        dest.writeString(fraudActivity);
        dest.writeString(shippingNameIndicator);
        dest.writeString(paymentAccountIndicator);
        dest.writeString(paymentAccountAge);
        dest.writeString(addressMatch);
        dest.writeString(accountId);
        dest.writeString(ipAddress);
        dest.writeString(orderDescription);
        dest.writeString(taxAmount);
        dest.writeString(userAgent);
        dest.writeString(authenticationIndicator);
        dest.writeString(installment);
        dest.writeString(purchaseDate);
        dest.writeString(recurringEnd);
        dest.writeString(recurringFrequency);
        dest.writeString(sdkMaxTimeout);
        dest.writeString(workPhoneNumber);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ThreeDSecureAdditionalInformation> CREATOR = new Parcelable.Creator<ThreeDSecureAdditionalInformation>() {
        @Override
        public ThreeDSecureAdditionalInformation createFromParcel(Parcel in) {
            return new ThreeDSecureAdditionalInformation(in);
        }

        @Override
        public ThreeDSecureAdditionalInformation[] newArray(int size) {
            return new ThreeDSecureAdditionalInformation[size];
        }
    };

    /**
     * @return JSONObject representation of {@link ThreeDSecureAdditionalInformation}.
     */
    public JSONObject toJson() {
        JSONObject additionalInformation = new JSONObject();
        try {
            if (shippingAddress != null) {
                additionalInformation.putOpt("shipping_given_name", shippingAddress.getGivenName());
                additionalInformation.putOpt("shipping_surname", shippingAddress.getSurname());
                additionalInformation.putOpt("shipping_phone", shippingAddress.getPhoneNumber());

                additionalInformation.putOpt("shipping_line1", shippingAddress.getStreetAddress());
                additionalInformation.putOpt("shipping_line2", shippingAddress.getExtendedAddress());
                additionalInformation.putOpt("shipping_line3", shippingAddress.getLine3());
                additionalInformation.putOpt("shipping_city", shippingAddress.getLocality());
                additionalInformation.putOpt("shipping_state", shippingAddress.getRegion());
                additionalInformation.putOpt("shipping_postal_code", shippingAddress.getPostalCode());
                additionalInformation.putOpt("shipping_country_code", shippingAddress.getCountryCodeAlpha2());
            }

            additionalInformation.putOpt("shipping_method_indicator", shippingMethodIndicator);
            additionalInformation.putOpt("product_code", productCode);
            additionalInformation.putOpt("delivery_timeframe", deliveryTimeframe);
            additionalInformation.putOpt("delivery_email", deliveryEmail);
            additionalInformation.putOpt("reorder_indicator", reorderIndicator);
            additionalInformation.putOpt("preorder_indicator", preorderIndicator);
            additionalInformation.putOpt("preorder_date", preorderDate);
            additionalInformation.putOpt("gift_card_amount", giftCardAmount);
            additionalInformation.putOpt("gift_card_currency_code", giftCardCurrencyCode);
            additionalInformation.putOpt("gift_card_count", giftCardCount);
            additionalInformation.putOpt("account_age_indicator", accountAgeIndicator);
            additionalInformation.putOpt("account_create_date", accountCreateDate);
            additionalInformation.putOpt("account_change_indicator", accountChangeIndicator);
            additionalInformation.putOpt("account_change_date", accountChangeDate);
            additionalInformation.putOpt("account_pwd_change_indicator", accountPwdChangeIndicator);
            additionalInformation.putOpt("account_pwd_change_date", accountPwdChangeDate);
            additionalInformation.putOpt("shipping_address_usage_indicator", shippingAddressUsageIndicator);
            additionalInformation.putOpt("shipping_address_usage_date", shippingAddressUsageDate);
            additionalInformation.putOpt("transaction_count_day", transactionCountDay);
            additionalInformation.putOpt("transaction_count_year", transactionCountYear);
            additionalInformation.putOpt("add_card_attempts", addCardAttempts);
            additionalInformation.putOpt("account_purchases", accountPurchases);
            additionalInformation.putOpt("fraud_activity", fraudActivity);
            additionalInformation.putOpt("shipping_name_indicator", shippingNameIndicator);
            additionalInformation.putOpt("payment_account_indicator", paymentAccountIndicator);
            additionalInformation.putOpt("payment_account_age", paymentAccountAge);
            additionalInformation.putOpt("address_match", addressMatch);
            additionalInformation.putOpt("account_id", accountId);
            additionalInformation.putOpt("ip_address", ipAddress);
            additionalInformation.putOpt("order_description", orderDescription);
            additionalInformation.putOpt("tax_amount", taxAmount);
            additionalInformation.putOpt("user_agent", userAgent);
            additionalInformation.putOpt("authentication_indicator", authenticationIndicator);
            additionalInformation.putOpt("installment", installment);
            additionalInformation.putOpt("purchase_date", purchaseDate);
            additionalInformation.putOpt("recurring_end", recurringEnd);
            additionalInformation.putOpt("recurring_frequency", recurringFrequency);
            additionalInformation.putOpt("sdk_max_timeout", sdkMaxTimeout);
            additionalInformation.putOpt("work_phone_number", workPhoneNumber);
        } catch (JSONException ignored) {}

        return additionalInformation;
    }

}
