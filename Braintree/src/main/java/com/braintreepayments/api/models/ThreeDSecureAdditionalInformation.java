package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class containing additional information for ThreeDSecure 2.0 Requests
 */
public class ThreeDSecureAdditionalInformation implements Parcelable {

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

    public ThreeDSecureAdditionalInformation() {}

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
    public ThreeDSecureAdditionalInformation shippingMethodIndicator(String shippingMethodIndicator) {
        mShippingMethodIndicator = shippingMethodIndicator;
        return this;
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
    public ThreeDSecureAdditionalInformation productCode(String productCode) {
        mProductCode = productCode;
        return this;
    }

    /**
     * Optional. The 2-digit number indicating the delivery timeframe

     * Possible values:
     * 01 Electronic delivery
     * 02 Same day shipping
     * 03 Overnight shipping
     * 04 Two or more day shipping
     */
    public ThreeDSecureAdditionalInformation deliveryTimeframe(String deliveryTimeframe) {
        mDeliveryTimeframe = deliveryTimeframe;
        return this;
    }

    /**
     * Optional. For electronic delivery, email address to which the merchandise was delivered
     */
    public ThreeDSecureAdditionalInformation deliveryEmail(String deliveryEmail) {
        mDeliveryEmail = deliveryEmail;
        return this;
    }

    /**
     * Optional. The 2-digit number indicating whether the cardholder is reordering previously purchased merchandise

     * Possible values:
     * 01 First time ordered
     * 02 Reordered
     */
    public ThreeDSecureAdditionalInformation reorderIndicator(String reorderIndicator) {
        mReorderIndicator = reorderIndicator;
        return this;
    }

    /**
     * Optional. The 2-digit number indicating whether the cardholder is placing an order with a future availability or release date

     * Possible values:
     * 01 Merchandise available
     * 02 Future availability
     */
    public ThreeDSecureAdditionalInformation preorderIndicator(String preorderIndicator) {
        mPreorderIndicator = preorderIndicator;
        return this;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating expected date that a pre-ordered purchase will be available
     */
    public ThreeDSecureAdditionalInformation preorderDate(String preorderDate) {
        mPreorderDate = preorderDate;
        return this;
    }

    /**
     * Optional. The purchase amount total for prepaid gift cards in major units
     */
    public ThreeDSecureAdditionalInformation giftCardAmount(String giftCardAmount) {
        mGiftCardAmount = giftCardAmount;
        return this;
    }

    /**
     * Optional. ISO 4217 currency code for the gift card purchased
     */
    public ThreeDSecureAdditionalInformation giftCardCurrencyCode(String giftCardCurrencyCode) {
        mGiftCardCurrencyCode = giftCardCurrencyCode;
        return this;
    }

    /**
     * Optional. Total count of individual prepaid gift cards purchased
     */
    public ThreeDSecureAdditionalInformation giftCardCount(String giftCardCount) {
        mGiftCardCount = giftCardCount;
        return this;
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
    public ThreeDSecureAdditionalInformation accountAgeIndicator(String accountAgeIndicator) {
        mAccountAgeIndicator = accountAgeIndicator;
        return this;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the cardholder opened the account.
     */
    public ThreeDSecureAdditionalInformation accountCreateDate(String accountCreateDate) {
        mAccountCreateDate = accountCreateDate;
        return this;
    }

    /**
     * Optional. The 2-digit value representing the length of time since the last change to the cardholder account. This includes shipping address, new payment account or new user added.

     * Possible values:
     * 01 Changed during transaction
     * 02 Less than 30 days
     * 03 30-60 days
     * 04 More than 60 days
     */
    public ThreeDSecureAdditionalInformation accountChangeIndicator(String accountChangeIndicator) {
        mAccountChangeIndicator = accountChangeIndicator;
        return this;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the cardholder's account was last changed. This includes changes to the billing or shipping address, new payment accounts or new users added.
     */
    public ThreeDSecureAdditionalInformation accountChangeDate(String accountChangeDate) {
        mAccountChangeDate = accountChangeDate;
        return this;
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
    public ThreeDSecureAdditionalInformation accountPwdChangeIndicator(String accountPwdChangeIndicator) {
        mAccountPwdChangeIndicator = accountPwdChangeIndicator;
        return this;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the cardholder last changed or reset password on account.
     */
    public ThreeDSecureAdditionalInformation accountPwdChangeDate(String accountPwdChangeDate) {
        mAccountPwdChangeDate = accountPwdChangeDate;
        return this;
    }

    /**
     * Optional. The 2-digit value indicating when the shipping address used for transaction was first used.

     * Possible values:
     * 01 This transaction
     * 02 Less than 30 days
     * 03 30-60 days
     * 04 More than 60 days
     */
    public ThreeDSecureAdditionalInformation shippingAddressUsageIndicator(String shippingAddressUsageIndicator) {
        mShippingAddressUsageIndicator = shippingAddressUsageIndicator;
        return this;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date when the shipping address used for this transaction was first used.
     */
    public ThreeDSecureAdditionalInformation shippingAddressUsageDate(String shippingAddressUsageDate) {
        mShippingAddressUsageDate = shippingAddressUsageDate;
        return this;
    }

    /**
     * Optional. Number of transactions (successful or abandoned) for this cardholder account within the last 24 hours.
     */
    public ThreeDSecureAdditionalInformation transactionCountDay(String transactionCountDay) {
        mTransactionCountDay = transactionCountDay;
        return this;
    }

    /**
     * Optional. Number of transactions (successful or abandoned) for this cardholder account within the last year.
     */
    public ThreeDSecureAdditionalInformation transactionCountYear(String transactionCountYear) {
        mTransactionCountYear = transactionCountYear;
        return this;
    }

    /**
     * Optional. Number of add card attempts in the last 24 hours.
     */
    public ThreeDSecureAdditionalInformation addCardAttempts(String addCardAttempts) {
        mAddCardAttempts = addCardAttempts;
        return this;
    }

    /**
     * Optional. Number of purchases with this cardholder account during the previous six months.
     */
    public ThreeDSecureAdditionalInformation accountPurchases(String accountPurchases) {
        mAccountPurchases = accountPurchases;
        return this;
    }

    /**
     * Optional. The 2-digit value indicating whether the merchant experienced suspicious activity (including previous fraud) on the account.

     * Possible values:
     * 01 No suspicious activity
     * 02 Suspicious activity observed
     */
    public ThreeDSecureAdditionalInformation fraudActivity(String fraudActivity) {
        mFraudActivity = fraudActivity;
        return this;
    }

    /**
     * Optional. The 2-digit value indicating if the cardholder name on the account is identical to the shipping name used for the transaction.

     * Possible values:
     * 01 Account name identical to shipping name
     * 02 Account name different than shipping name
     */
    public ThreeDSecureAdditionalInformation shippingNameIndicator(String shippingNameIndicator) {
        mShippingNameIndicator = shippingNameIndicator;
        return this;
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
    public ThreeDSecureAdditionalInformation paymentAccountIndicator(String paymentAccountIndicator) {
        mPaymentAccountIndicator = paymentAccountIndicator;
        return this;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date the payment account was added to the cardholder account.
     */
    public ThreeDSecureAdditionalInformation paymentAccountAge(String paymentAccountAge) {
        mPaymentAccountAge = paymentAccountAge;
        return this;
    }

    /**
     * Optional. The 1-character value (Y/N) indicating whether cardholder billing and shipping addresses match.
     */
    public ThreeDSecureAdditionalInformation addressMatch(String addressMatch) {
        mAddressMatch = addressMatch;
        return this;
    }

    /**
     * Optional. Additional cardholder account information.
     */
    public ThreeDSecureAdditionalInformation accountId(String accountId) {
        mAccountId = accountId;
        return this;
    }

    /**
     * Optional. The IP address of the consumer. IPv4 and IPv6 are supported.
     */
    public ThreeDSecureAdditionalInformation ipAddress(String ipAddress) {
        mIpAddress = ipAddress;
        return this;
    }

    /**
     * Optional. Brief description of items purchased.
     */
    public ThreeDSecureAdditionalInformation orderDescription(String orderDescription) {
        mOrderDescription = orderDescription;
        return this;
    }

    /**
     * Optional. Unformatted tax amount without any decimalization (ie. $123.67 = 12367).
     */
    public ThreeDSecureAdditionalInformation taxAmount(String taxAmount) {
        mTaxAmount = taxAmount;
        return this;
    }

    /**
     * Optional. The exact content of the HTTP user agent header.
     */
    public ThreeDSecureAdditionalInformation userAgent(String userAgent) {
        mUserAgent = userAgent;
        return this;
    }

    /**
     * Optional. The 2-digit number indicating the type of authentication request. This field is required if a recurring or installment transaction request.

     * Possible values:
     * 02 Recurring transaction
     * 03 Installment transaction
     */
    public ThreeDSecureAdditionalInformation authenticationIndicator(String authenticationIndicator) {
        mAuthenticationIndicator = authenticationIndicator;
        return this;
    }

    /**
     * Optional.  An integer value greater than 1 indicating the maximum number of permitted authorizations for installment payments. Required for recurring and installement transaction requests.
     */
    public ThreeDSecureAdditionalInformation installment(String installment) {
        mInstallment = installment;
        return this;
    }

    /**
     * Optional. The 14-digit number (format: YYYYMMDDHHMMSS) indicating the date in UTC of original purchase. Required for recurring and installement transaction requests.
     */
    public ThreeDSecureAdditionalInformation purchaseDate(String purchaseDate) {
        mPurchaseDate = purchaseDate;
        return this;
    }

    /**
     * Optional. The 8-digit number (format: YYYYMMDD) indicating the date after which no further recurring authorizations should be performed. Required for recurring and installement transaction requests.
     */
    public ThreeDSecureAdditionalInformation recurringEnd(String recurringEnd) {
        mRecurringEnd = recurringEnd;
        return this;
    }

    /**
     * Optional. Integer value indicating the minimum number of days between recurring authorizations. A frequency of monthly is indicated by the value 28. Multiple of 28 days will be used to indicate months (ex. 6 months = 168). Required for recurring and installement transaction requests.
     */
    public ThreeDSecureAdditionalInformation recurringFrequency(String recurringFrequency) {
        mRecurringFrequency = recurringFrequency;
        return this;
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

    public ThreeDSecureAdditionalInformation(Parcel in) {
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
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
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
            additionalInformation.putOpt("shippingMethodIndicator", mShippingMethodIndicator);
            additionalInformation.putOpt("productCode", mProductCode);
            additionalInformation.putOpt("deliveryTimeframe", mDeliveryTimeframe);
            additionalInformation.putOpt("deliveryEmail", mDeliveryEmail);
            additionalInformation.putOpt("reorderIndicator", mReorderIndicator);
            additionalInformation.putOpt("preorderIndicator", mPreorderIndicator);
            additionalInformation.putOpt("preorderDate", mPreorderDate);
            additionalInformation.putOpt("giftCardAmount", mGiftCardAmount);
            additionalInformation.putOpt("giftCardCurrencyCode", mGiftCardCurrencyCode);
            additionalInformation.putOpt("giftCardCount", mGiftCardCount);
            additionalInformation.putOpt("accountAgeIndicator", mAccountAgeIndicator);
            additionalInformation.putOpt("accountCreateDate", mAccountCreateDate);
            additionalInformation.putOpt("accountChangeIndicator", mAccountChangeIndicator);
            additionalInformation.putOpt("accountChangeDate", mAccountChangeDate);
            additionalInformation.putOpt("accountPwdChangeIndicator", mAccountPwdChangeIndicator);
            additionalInformation.putOpt("accountPwdChangeDate", mAccountPwdChangeDate);
            additionalInformation.putOpt("shippingAddressUsageIndicator", mShippingAddressUsageIndicator);
            additionalInformation.putOpt("shippingAddressUsageDate", mShippingAddressUsageDate);
            additionalInformation.putOpt("transactionCountDay", mTransactionCountDay);
            additionalInformation.putOpt("transactionCountYear", mTransactionCountYear);
            additionalInformation.putOpt("addCardAttempts", mAddCardAttempts);
            additionalInformation.putOpt("accountPurchases", mAccountPurchases);
            additionalInformation.putOpt("fraudActivity", mFraudActivity);
            additionalInformation.putOpt("shippingNameIndicator", mShippingNameIndicator);
            additionalInformation.putOpt("paymentAccountIndicator", mPaymentAccountIndicator);
            additionalInformation.putOpt("paymentAccountAge", mPaymentAccountAge);
            additionalInformation.putOpt("addressMatch", mAddressMatch);
            additionalInformation.putOpt("accountId", mAccountId);
            additionalInformation.putOpt("ipAddress", mIpAddress);
            additionalInformation.putOpt("orderDescription", mOrderDescription);
            additionalInformation.putOpt("taxAmount", mTaxAmount);
            additionalInformation.putOpt("userAgent", mUserAgent);
            additionalInformation.putOpt("authenticationIndicator", mAuthenticationIndicator);
            additionalInformation.putOpt("installment", mInstallment);
            additionalInformation.putOpt("purchaseDate", mPurchaseDate);
            additionalInformation.putOpt("recurringEnd", mRecurringEnd);
            additionalInformation.putOpt("recurringFrequency", mRecurringFrequency);
        } catch (JSONException ignored) {}

        return additionalInformation;
    }

}
