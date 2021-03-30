package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class containing additional information for ThreeDSecure 2.0 Requests
 */
public class ThreeDSecureAdditionalInformation implements Parcelable {

    private ThreeDSecurePostalAddress mShippingAddress;
    private String mShippingMethodIndicator;
    private String mProductCode;
    private String mDeliveryTimeframe;
    private String mDeliveryEmail;
    private String mReorderIndicator;
    private String mPreorderIndicator;
    private String mPreorderDate;
    private String mGiftCardAmount;
    private String mGiftCardCurrencyCode;
    private String mGiftCardCount;
    private String mAccountAgeIndicator;
    private String mAccountCreateDate;
    private String mAccountChangeIndicator;
    private String mAccountChangeDate;
    private String mAccountPwdChangeIndicator;
    private String mAccountPwdChangeDate;
    private String mShippingAddressUsageIndicator;
    private String mShippingAddressUsageDate;
    private String mTransactionCountDay;
    private String mTransactionCountYear;
    private String mAddCardAttempts;
    private String mAccountPurchases;
    private String mFraudActivity;
    private String mShippingNameIndicator;
    private String mPaymentAccountIndicator;
    private String mPaymentAccountAge;
    private String mAddressMatch;
    private String mAccountId;
    private String mIpAddress;
    private String mOrderDescription;
    private String mTaxAmount;
    private String mUserAgent;
    private String mAuthenticationIndicator;
    private String mInstallment;
    private String mPurchaseDate;
    private String mRecurringEnd;
    private String mRecurringFrequency;
    private String mSdkMaxTimeout;
    private String mWorkPhoneNumber;

    public ThreeDSecureAdditionalInformation() {}

    /**
     * Optional. Set the shipping address
     *
     * @param shippingAddress The shipping address used for verification.
     *
     * */
    public void setShippingAddress(ThreeDSecurePostalAddress shippingAddress) {
        mShippingAddress = shippingAddress;
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
    public void setShippingMethodIndicator(String shippingMethodIndicator) {
        mShippingMethodIndicator = shippingMethodIndicator;
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
    public void setProductCode(String productCode) {
        mProductCode = productCode;
    }

    /**
     * Optional. The 2-digit number indicating the delivery timeframe

     * Possible values:
     * 01 Electronic delivery
     * 02 Same day shipping
     * 03 Overnight shipping
     * 04 Two or more day shipping
     */
    public void setDeliveryTimeframe(String deliveryTimeframe) {
        mDeliveryTimeframe = deliveryTimeframe;
    }

    /**
     * Optional. For electronic delivery, email address to which the merchandise was delivered
     */
    public void setDeliveryEmail(String deliveryEmail) {
        mDeliveryEmail = deliveryEmail;
    }

    /**
     * Optional. The 2-digit number indicating whether the cardholder is reordering previously purchased merchandise

     * Possible values:
     * 01 First time ordered
     * 02 Reordered
     */
    public void setReorderIndicator(String reorderIndicator) {
        mReorderIndicator = reorderIndicator;
    }

    /**
     * Optional. The 2-digit number indicating whether the cardholder is placing an order with a future availability or release date

     * Possible values:
     * 01 Merchandise available
     * 02 Future availability
     */
    public void setPreorderIndicator(String preorderIndicator) {
        mPreorderIndicator = preorderIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating expected date that a pre-ordered purchase will be available
     */
    public void setPreorderDate(String preorderDate) {
        mPreorderDate = preorderDate;
    }

    /**
     * Optional. The purchase amount total for prepaid gift cards in major units
     */
    public void setGiftCardAmount(String giftCardAmount) {
        mGiftCardAmount = giftCardAmount;
    }

    /**
     * Optional. ISO 4217 currency code for the gift card purchased
     */
    public void setGiftCardCurrencyCode(String giftCardCurrencyCode) {
        mGiftCardCurrencyCode = giftCardCurrencyCode;
    }

    /**
     * Optional. Total count of individual prepaid gift cards purchased
     */
    public void setGiftCardCount(String giftCardCount) {
        mGiftCardCount = giftCardCount;
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
    public void setAccountAgeIndicator(String accountAgeIndicator) {
        mAccountAgeIndicator = accountAgeIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the cardholder opened the account.
     */
    public void setAccountCreateDate(String accountCreateDate) {
        mAccountCreateDate = accountCreateDate;
    }

    /**
     * Optional. The 2-digit value representing the length of time since the last change to the cardholder account. This includes shipping address, new payment account or new user added.

     * Possible values:
     * 01 Changed during transaction
     * 02 Less than 30 days
     * 03 30-60 days
     * 04 More than 60 days
     */
    public void setAccountChangeIndicator(String accountChangeIndicator) {
        mAccountChangeIndicator = accountChangeIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the cardholder's account was last changed. This includes changes to the billing or shipping address, new payment accounts or new users added.
     */
    public void setAccountChangeDate(String accountChangeDate) {
        mAccountChangeDate = accountChangeDate;
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
    public void setAccountPwdChangeIndicator(String accountPwdChangeIndicator) {
        mAccountPwdChangeIndicator = accountPwdChangeIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the cardholder last changed or reset password on account.
     */
    public void setAccountPwdChangeDate(String accountPwdChangeDate) {
        mAccountPwdChangeDate = accountPwdChangeDate;
    }

    /**
     * Optional. The 2-digit value indicating when the shipping address used for transaction was first used.

     * Possible values:
     * 01 This transaction
     * 02 Less than 30 days
     * 03 30-60 days
     * 04 More than 60 days
     */
    public void setShippingAddressUsageIndicator(String shippingAddressUsageIndicator) {
        mShippingAddressUsageIndicator = shippingAddressUsageIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date when the shipping address used for this transaction was first used.
     */
    public void setShippingAddressUsageDate(String shippingAddressUsageDate) {
        mShippingAddressUsageDate = shippingAddressUsageDate;
    }

    /**
     * Optional. Number of transactions (successful or abandoned) for this cardholder account within the last 24 hours.
     */
    public void setTransactionCountDay(String transactionCountDay) {
        mTransactionCountDay = transactionCountDay;
    }

    /**
     * Optional. Number of transactions (successful or abandoned) for this cardholder account within the last year.
     */
    public void setTransactionCountYear(String transactionCountYear) {
        mTransactionCountYear = transactionCountYear;
    }

    /**
     * Optional. Number of add card attempts in the last 24 hours.
     */
    public void setAddCardAttempts(String addCardAttempts) {
        mAddCardAttempts = addCardAttempts;
    }

    /**
     * Optional. Number of purchases with this cardholder account during the previous six months.
     */
    public void setAccountPurchases(String accountPurchases) {
        mAccountPurchases = accountPurchases;
    }

    /**
     * Optional. The 2-digit value indicating whether the merchant experienced suspicious activity (including previous fraud) on the account.

     * Possible values:
     * 01 No suspicious activity
     * 02 Suspicious activity observed
     */
    public void setFraudActivity(String fraudActivity) {
        mFraudActivity = fraudActivity;
    }

    /**
     * Optional. The 2-digit value indicating if the cardholder name on the account is identical to the shipping name used for the transaction.

     * Possible values:
     * 01 Account name identical to shipping name
     * 02 Account name different than shipping name
     */
    public void setShippingNameIndicator(String shippingNameIndicator) {
        mShippingNameIndicator = shippingNameIndicator;
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
    public void setPaymentAccountIndicator(String paymentAccountIndicator) {
        mPaymentAccountIndicator = paymentAccountIndicator;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the payment account was added to the cardholder account.
     */
    public void setPaymentAccountAge(String paymentAccountAge) {
        mPaymentAccountAge = paymentAccountAge;
    }

    /**
     * Optional. The 1-character value (Y/N) indicating whether cardholder billing and shipping addresses match.
     */
    public void setAddressMatch(String addressMatch) {
        mAddressMatch = addressMatch;
    }

    /**
     * Optional. Additional cardholder account information.
     */
    public void setAccountId(String accountId) {
        mAccountId = accountId;
    }

    /**
     * Optional. The IP address of the consumer. IPv4 and IPv6 are supported.
     */
    public void setIpAddress(String ipAddress) {
        mIpAddress = ipAddress;
    }

    /**
     * Optional. Brief description of items purchased.
     */
    public void setOrderDescription(String orderDescription) {
        mOrderDescription = orderDescription;
    }

    /**
     * Optional. Unformatted tax amount without any decimalization (ie. $123.67 = 12367).
     */
    public void setTaxAmount(String taxAmount) {
        mTaxAmount = taxAmount;
    }

    /**
     * Optional. The exact content of the HTTP user agent header.
     */
    public void setUserAgent(String userAgent) {
        mUserAgent = userAgent;
    }

    /**
     * Optional. The 2-digit number indicating the type of authentication request.

     * Possible values:
     * 02 Recurring transaction
     * 03 Installment transaction
     */
    public void setAuthenticationIndicator(String authenticationIndicator) {
        mAuthenticationIndicator = authenticationIndicator;
    }

    /**
     * Optional.  An integer value greater than 1 indicating the maximum number of permitted authorizations for installment payments.
     */
    public void setInstallment(String installment) {
        mInstallment = installment;
    }

    /**
     * Optional. The 14-digit number (format: YYYYMMDDHHMMSS) indicating the date in UTC of original purchase.
     */
    public void setPurchaseDate(String purchaseDate) {
        mPurchaseDate = purchaseDate;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date after which no further recurring authorizations should be performed..
     */
    public void setRecurringEnd(String recurringEnd) {
        mRecurringEnd = recurringEnd;
    }

    /**
     * Optional. Integer value indicating the minimum number of days between recurring authorizations. A frequency of monthly is indicated by the value 28. Multiple of 28 days will be used to indicate months (ex. 6 months = 168).
     */
    public void setRecurringFrequency(String recurringFrequency) {
        mRecurringFrequency = recurringFrequency;
    }

    /**
     * Optional. The 2-digit number of minutes (minimum 05) to set the maximum amount of time for all 3DS 2.0 messages to be communicated between all components.
     */
    public void setSdkMaxTimeout(String sdkMaxTimeout) {
        mSdkMaxTimeout = sdkMaxTimeout;
    }

    /**
     * Optional. The work phone number used for verification. Only numbers; remove dashes, parenthesis and other characters.
     */
    public void setWorkPhoneNumber(String workPhoneNumber) {
        mWorkPhoneNumber = workPhoneNumber;
    }

    /**
     * @return shipping address
     */
    public ThreeDSecurePostalAddress getShippingAddress() {
        return mShippingAddress;
    }

    /**
     * @return shipping method indicator
     */
    public String getShippingMethodIndicator() {
        return mShippingMethodIndicator;
    }

    /**
     * @return product code
     */
    public String getProductCode() {
        return mProductCode;
    }

    /**
     * @return delivery time frame
     */
    public String getDeliveryTimeframe() {
        return mDeliveryTimeframe;
    }

    /**
     * @return delivery email
     */
    public String getDeliveryEmail() {
        return mDeliveryEmail;
    }

    /**
     * @return reorder indicator
     */
    public String getReorderIndicator() {
        return mReorderIndicator;
    }

    /**
     * @return preorder indicator
     */
    public String getPreorderIndicator() {
        return mPreorderIndicator;
    }

    /**
     * @return Preorder date
     */
    public String getPreorderDate() {
        return mPreorderDate;
    }

    /**
     * @return Gift card amount
     */
    public String getGiftCardAmount() {
        return mGiftCardAmount;
    }

    /**
     * @return Gift card currency code
     */
    public String getGiftCardCurrencyCode() {
        return mGiftCardCurrencyCode;
    }

    /**
     * @return Gift card count
     */
    public String getGiftCardCount() {
        return mGiftCardCount;
    }

    /**
     * @return Account age indicator
     */
    public String getAccountAgeIndicator() {
        return mAccountAgeIndicator;
    }

    /**
     * @return Account create date
     */
    public String getAccountCreateDate() {
        return mAccountCreateDate;
    }

    /**
     * @return Account change indicator
     */
    public String getAccountChangeIndicator() {
        return mAccountChangeIndicator;
    }

    /**
     * @return Account change date
     */
    public String getAccountChangeDate() {
        return mAccountChangeDate;
    }

    /**
     * @return Account password change indicator
     */
    public String getAccountPwdChangeIndicator() {
        return mAccountPwdChangeIndicator;
    }

    /**
     * @return Account password change date
     */
    public String getAccountPwdChangeDate() {
        return mAccountPwdChangeDate;
    }

    /**
     * @return Shipping address usage indicator
     */
    public String getShippingAddressUsageIndicator() {
        return mShippingAddressUsageIndicator;
    }

    /**
     * @return Shipping address usage date
     */
    public String getShippingAddressUsageDate() {
        return mShippingAddressUsageDate;
    }

    /**
     * @return Transaction count day
     */
    public String getTransactionCountDay() {
        return mTransactionCountDay;
    }

    /**
     * @return Transaction count year
     */
    public String getTransactionCountYear() {
        return mTransactionCountYear;
    }

    /**
     * @return Add card attempts
     */
    public String getAddCardAttempts() {
        return mAddCardAttempts;
    }

    /**
     * @return Account purchases
     */
    public String getAccountPurchases() {
        return mAccountPurchases;
    }

    /**
     * @return Fraud activity
     */
    public String getFraudActivity() {
        return mFraudActivity;
    }

    /**
     * @return Shipping name indicator
     */
    public String getShippingNameIndicator() {
        return mShippingNameIndicator;
    }

    /**
     * @return Payment account indicator
     */
    public String getPaymentAccountIdicator() {
        return mPaymentAccountIndicator;
    }

    /**
     * @return Payment account age
     */
    public String getPaymentAccountAge() {
        return mPaymentAccountAge;
    }

    /**
     * @return Address match
     */
    public String getAddressMatch() {
        return mAddressMatch;
    }

    /**
     * @return Account ID
     */
    public String getAccountId() {
        return mAccountId;
    }

    /**
     * @return Ip address
     */
    public String getIpAddress() {
        return mIpAddress;
    }

    /**
     * @return Order description
     */
    public String getOrderDescription() {
        return mOrderDescription;
    }

    /**
     * @return Tax amount
     */
    public String getTaxAmount() {
        return mTaxAmount;
    }

    /**
     * @return User agent
     */
    public String getUserAgent() {
        return mUserAgent;
    }

    /**
     * @return Authentication indicator
     */
    public String getAuthenticationIndicator() {
        return mAuthenticationIndicator;
    }

    /**
     * @return Installment
     */
    public String getInstallment() {
        return mInstallment;
    }

    /**
     * @return Purchase date
     */
    public String getPurchaseDate() {
        return mPurchaseDate;
    }

    /**
     * @return Recurring end
     */
    public String getRecurringEnd() {
        return mRecurringEnd;
    }

    /**
     * @return Recurring frequency
     */
    public String getRecurringFrequency() {
        return mRecurringFrequency;
    }

    /**
     * @return SDK max timeout
     */
    public String getSdkMaxTimeout() {
        return mSdkMaxTimeout;
    }

    /**
     * @return Work phone number
     */
    public String getWorkPhoneNumber() {
        return mWorkPhoneNumber;
    }

    public ThreeDSecureAdditionalInformation(Parcel in) {
        mShippingAddress = in.readParcelable(ThreeDSecurePostalAddress.class.getClassLoader());
        mShippingMethodIndicator = in.readString();
        mProductCode = in.readString();
        mDeliveryTimeframe = in.readString();
        mDeliveryEmail = in.readString();
        mReorderIndicator = in.readString();
        mPreorderIndicator = in.readString();
        mPreorderDate = in.readString();
        mGiftCardAmount = in.readString();
        mGiftCardCurrencyCode = in.readString();
        mGiftCardCount = in.readString();
        mAccountAgeIndicator = in.readString();
        mAccountCreateDate = in.readString();
        mAccountChangeIndicator = in.readString();
        mAccountChangeDate = in.readString();
        mAccountPwdChangeIndicator = in.readString();
        mAccountPwdChangeDate = in.readString();
        mShippingAddressUsageIndicator = in.readString();
        mShippingAddressUsageDate = in.readString();
        mTransactionCountDay = in.readString();
        mTransactionCountYear = in.readString();
        mAddCardAttempts = in.readString();
        mAccountPurchases = in.readString();
        mFraudActivity = in.readString();
        mShippingNameIndicator = in.readString();
        mPaymentAccountIndicator = in.readString();
        mPaymentAccountAge = in.readString();
        mAddressMatch = in.readString();
        mAccountId = in.readString();
        mIpAddress = in.readString();
        mOrderDescription = in.readString();
        mTaxAmount = in.readString();
        mUserAgent = in.readString();
        mAuthenticationIndicator = in.readString();
        mInstallment = in.readString();
        mPurchaseDate = in.readString();
        mRecurringEnd = in.readString();
        mRecurringFrequency = in.readString();
        mSdkMaxTimeout = in.readString();
        mWorkPhoneNumber = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mShippingAddress, flags);
        dest.writeString(mShippingMethodIndicator);
        dest.writeString(mProductCode);
        dest.writeString(mDeliveryTimeframe);
        dest.writeString(mDeliveryEmail);
        dest.writeString(mReorderIndicator);
        dest.writeString(mPreorderIndicator);
        dest.writeString(mPreorderDate);
        dest.writeString(mGiftCardAmount);
        dest.writeString(mGiftCardCurrencyCode);
        dest.writeString(mGiftCardCount);
        dest.writeString(mAccountAgeIndicator);
        dest.writeString(mAccountCreateDate);
        dest.writeString(mAccountChangeIndicator);
        dest.writeString(mAccountChangeDate);
        dest.writeString(mAccountPwdChangeIndicator);
        dest.writeString(mAccountPwdChangeDate);
        dest.writeString(mShippingAddressUsageIndicator);
        dest.writeString(mShippingAddressUsageDate);
        dest.writeString(mTransactionCountDay);
        dest.writeString(mTransactionCountYear);
        dest.writeString(mAddCardAttempts);
        dest.writeString(mAccountPurchases);
        dest.writeString(mFraudActivity);
        dest.writeString(mShippingNameIndicator);
        dest.writeString(mPaymentAccountIndicator);
        dest.writeString(mPaymentAccountAge);
        dest.writeString(mAddressMatch);
        dest.writeString(mAccountId);
        dest.writeString(mIpAddress);
        dest.writeString(mOrderDescription);
        dest.writeString(mTaxAmount);
        dest.writeString(mUserAgent);
        dest.writeString(mAuthenticationIndicator);
        dest.writeString(mInstallment);
        dest.writeString(mPurchaseDate);
        dest.writeString(mRecurringEnd);
        dest.writeString(mRecurringFrequency);
        dest.writeString(mSdkMaxTimeout);
        dest.writeString(mWorkPhoneNumber);
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
            if (mShippingAddress != null) {
                additionalInformation.putOpt("shipping_given_name", mShippingAddress.getGivenName());
                additionalInformation.putOpt("shipping_surname", mShippingAddress.getSurname());
                additionalInformation.putOpt("shipping_phone", mShippingAddress.getPhoneNumber());

                additionalInformation.putOpt("shipping_line1", mShippingAddress.getStreetAddress());
                additionalInformation.putOpt("shipping_line2", mShippingAddress.getExtendedAddress());
                additionalInformation.putOpt("shipping_line3", mShippingAddress.getLine3());
                additionalInformation.putOpt("shipping_city", mShippingAddress.getLocality());
                additionalInformation.putOpt("shipping_state", mShippingAddress.getRegion());
                additionalInformation.putOpt("shipping_postal_code", mShippingAddress.getPostalCode());
                additionalInformation.putOpt("shipping_country_code", mShippingAddress.getCountryCodeAlpha2());
            }

            additionalInformation.putOpt("shipping_method_indicator", mShippingMethodIndicator);
            additionalInformation.putOpt("product_code", mProductCode);
            additionalInformation.putOpt("delivery_timeframe", mDeliveryTimeframe);
            additionalInformation.putOpt("delivery_email", mDeliveryEmail);
            additionalInformation.putOpt("reorder_indicator", mReorderIndicator);
            additionalInformation.putOpt("preorder_indicator", mPreorderIndicator);
            additionalInformation.putOpt("preorder_date", mPreorderDate);
            additionalInformation.putOpt("gift_card_amount", mGiftCardAmount);
            additionalInformation.putOpt("gift_card_currency_code", mGiftCardCurrencyCode);
            additionalInformation.putOpt("gift_card_count", mGiftCardCount);
            additionalInformation.putOpt("account_age_indicator", mAccountAgeIndicator);
            additionalInformation.putOpt("account_create_date", mAccountCreateDate);
            additionalInformation.putOpt("account_change_indicator", mAccountChangeIndicator);
            additionalInformation.putOpt("account_change_date", mAccountChangeDate);
            additionalInformation.putOpt("account_pwd_change_indicator", mAccountPwdChangeIndicator);
            additionalInformation.putOpt("account_pwd_change_date", mAccountPwdChangeDate);
            additionalInformation.putOpt("shipping_address_usage_indicator", mShippingAddressUsageIndicator);
            additionalInformation.putOpt("shipping_address_usage_date", mShippingAddressUsageDate);
            additionalInformation.putOpt("transaction_count_day", mTransactionCountDay);
            additionalInformation.putOpt("transaction_count_year", mTransactionCountYear);
            additionalInformation.putOpt("add_card_attempts", mAddCardAttempts);
            additionalInformation.putOpt("account_purchases", mAccountPurchases);
            additionalInformation.putOpt("fraud_activity", mFraudActivity);
            additionalInformation.putOpt("shipping_name_indicator", mShippingNameIndicator);
            additionalInformation.putOpt("payment_account_indicator", mPaymentAccountIndicator);
            additionalInformation.putOpt("payment_account_age", mPaymentAccountAge);
            additionalInformation.putOpt("address_match", mAddressMatch);
            additionalInformation.putOpt("account_id", mAccountId);
            additionalInformation.putOpt("ip_address", mIpAddress);
            additionalInformation.putOpt("order_description", mOrderDescription);
            additionalInformation.putOpt("tax_amount", mTaxAmount);
            additionalInformation.putOpt("user_agent", mUserAgent);
            additionalInformation.putOpt("authentication_indicator", mAuthenticationIndicator);
            additionalInformation.putOpt("installment", mInstallment);
            additionalInformation.putOpt("purchase_date", mPurchaseDate);
            additionalInformation.putOpt("recurring_end", mRecurringEnd);
            additionalInformation.putOpt("recurring_frequency", mRecurringFrequency);
            additionalInformation.putOpt("sdk_max_timeout", mSdkMaxTimeout);
            additionalInformation.putOpt("work_phone_number", mWorkPhoneNumber);
        } catch (JSONException ignored) {}

        return additionalInformation;
    }

}
