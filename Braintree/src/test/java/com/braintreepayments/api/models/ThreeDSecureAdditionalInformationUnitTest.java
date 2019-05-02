package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureAdditionalInformationUnitTest {
    @Test
    public void writeToParcel() {
        ThreeDSecurePostalAddress shippingAddress = new ThreeDSecurePostalAddress()
                .firstName("shipping-given-name");

        ThreeDSecureAdditionalInformation preSerialized = new ThreeDSecureAdditionalInformation()
                .shippingAddress(shippingAddress)
                .shippingMethodIndicator("shipping-method-indicator")
                .productCode("product_code")
                .deliveryTimeframe("delivery_timeframe")
                .deliveryEmail("delivery_email")
                .reorderIndicator("reorder_indicator")
                .preorderIndicator("preorder_indicator")
                .preorderDate("preorder_date")
                .giftCardAmount("gift_card_amount")
                .giftCardCurrencyCode("gift_card_currency_code")
                .giftCardCount("gift_card_count")
                .accountAgeIndicator("account_age_indicator")
                .accountCreateDate("account_create_date")
                .accountChangeIndicator("account_change_indicator")
                .accountChangeDate("account_change_date")
                .accountPwdChangeIndicator("account_pwd_change_indicator")
                .accountPwdChangeDate("account_pwd_change_date")
                .shippingAddressUsageIndicator("shipping_address_usage_indicator")
                .shippingAddressUsageDate("shipping_address_usage_date")
                .transactionCountDay("transaction_count_day")
                .transactionCountYear("transaction_count_year")
                .addCardAttempts("add_card_attempts")
                .accountPurchases("account_purchases")
                .fraudActivity("fraud_activity")
                .shippingNameIndicator("shipping_name_indicator")
                .paymentAccountIndicator("payment_account_indicator")
                .paymentAccountAge("payment_account_age")
                .addressMatch("address_match")
                .accountId("account_id")
                .ipAddress("ip_address")
                .orderDescription("order_description")
                .taxAmount("tax_amount")
                .userAgent("user_agent")
                .authenticationIndicator("authentication_indicator")
                .installment("installment")
                .purchaseDate("purchase_date")
                .recurringEnd("recurring_end")
                .recurringFrequency("recurring_frequency");

        Parcel parcel = Parcel.obtain();
        preSerialized.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureAdditionalInformation postSerialized = ThreeDSecureAdditionalInformation.CREATOR.createFromParcel(parcel);

        assertEquals("shipping-given-name", postSerialized.getShippingAddress().getFirstName());
        assertEquals("shipping-method-indicator", postSerialized.getShippingMethodIndicator());
        assertEquals("product_code", postSerialized.getProductCode());
        assertEquals("delivery_timeframe", postSerialized.getDeliveryTimeframe());
        assertEquals("delivery_email", postSerialized.getDeliveryEmail());
        assertEquals("reorder_indicator", postSerialized.getReorderIndicator());
        assertEquals("preorder_indicator", postSerialized.getPreorderIndicator());
        assertEquals("preorder_date", postSerialized.getPreorderDate());
        assertEquals("gift_card_amount", postSerialized.getGiftCardAmount());
        assertEquals("gift_card_currency_code", postSerialized.getGiftCardCurrencyCode());
        assertEquals("gift_card_count", postSerialized.getGiftCardCount());
        assertEquals("account_age_indicator", postSerialized.getAccountAgeIndicator());
        assertEquals("account_create_date", postSerialized.getAccountCreateDate());
        assertEquals("account_change_indicator", postSerialized.getAccountChangeIndicator());
        assertEquals("account_change_date", postSerialized.getAccountChangeDate());
        assertEquals("account_pwd_change_indicator", postSerialized.getAccountPwdChangeIndicator());
        assertEquals("account_pwd_change_date", postSerialized.getAccountPwdChangeDate());
        assertEquals("shipping_address_usage_indicator", postSerialized.getShippingAddressUsageIndicator());
        assertEquals("shipping_address_usage_date", postSerialized.getShippingAddressUsageDate());
        assertEquals("transaction_count_day", postSerialized.getTransactionCountDay());
        assertEquals("transaction_count_year", postSerialized.getTransactionCountYear());
        assertEquals("add_card_attempts", postSerialized.getAddCardAttempts());
        assertEquals("account_purchases", postSerialized.getAccountPurchases());
        assertEquals("fraud_activity", postSerialized.getFraudActivity());
        assertEquals("shipping_name_indicator", postSerialized.getShippingNameIndicator());
        assertEquals("payment_account_indicator", postSerialized.getPaymentAccountIdicator());
        assertEquals("payment_account_age", postSerialized.getPaymentAccountAge());
        assertEquals("address_match", postSerialized.getAddressMatch());
        assertEquals("account_id", postSerialized.getAccountId());
        assertEquals("ip_address", postSerialized.getIpAddress());
        assertEquals("order_description", postSerialized.getOrderDescription());
        assertEquals("tax_amount", postSerialized.getTaxAmount());
        assertEquals("user_agent", postSerialized.getUserAgent());
        assertEquals("authentication_indicator", postSerialized.getAuthenticationIndicator());
        assertEquals("installment", postSerialized.getInstallment());
        assertEquals("purchase_date", postSerialized.getPurchaseDate());
        assertEquals("recurring_end", postSerialized.getRecurringEnd());
        assertEquals("recurring_frequency", postSerialized.getRecurringFrequency());
    }

    @Test
    public void toJson() throws JSONException {
        ThreeDSecurePostalAddress shippingAddress = new ThreeDSecurePostalAddress()
                .firstName("shipping-given-name")
                .lastName("shipping-surname")
                .phoneNumber("shipping-phone")
                .streetAddress("shipping-line1")
                .extendedAddress("shipping-line2")
                .locality("shipping-city")
                .region("shipping-state")
                .postalCode("shipping-postal-code")
                .countryCodeAlpha2("shipping-country-code");

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .shippingAddress(shippingAddress)
                .shippingMethodIndicator("shipping-method-indicator")
                .productCode("product_code")
                .deliveryTimeframe("delivery_timeframe")
                .deliveryEmail("delivery_email")
                .reorderIndicator("reorder_indicator")
                .preorderIndicator("preorder_indicator")
                .preorderDate("preorder_date")
                .giftCardAmount("gift_card_amount")
                .giftCardCurrencyCode("gift_card_currency_code")
                .giftCardCount("gift_card_count")
                .accountAgeIndicator("account_age_indicator")
                .accountCreateDate("account_create_date")
                .accountChangeIndicator("account_change_indicator")
                .accountChangeDate("account_change_date")
                .accountPwdChangeIndicator("account_pwd_change_indicator")
                .accountPwdChangeDate("account_pwd_change_date")
                .shippingAddressUsageIndicator("shipping_address_usage_indicator")
                .shippingAddressUsageDate("shipping_address_usage_date")
                .transactionCountDay("transaction_count_day")
                .transactionCountYear("transaction_count_year")
                .addCardAttempts("add_card_attempts")
                .accountPurchases("account_purchases")
                .fraudActivity("fraud_activity")
                .shippingNameIndicator("shipping_name_indicator")
                .paymentAccountIndicator("payment_account_indicator")
                .paymentAccountAge("payment_account_age")
                .addressMatch("address_match")
                .accountId("account_id")
                .ipAddress("ip_address")
                .orderDescription("order_description")
                .taxAmount("tax_amount")
                .userAgent("user_agent")
                .authenticationIndicator("authentication_indicator")
                .installment("installment")
                .purchaseDate("purchase_date")
                .recurringEnd("recurring_end")
                .recurringFrequency("recurring_frequency");

        JSONObject jsonParams = additionalInformation.toJson();

        assertEquals("shipping-given-name", jsonParams.getString("shipping_given_name"));
        assertEquals("shipping-surname", jsonParams.getString("shipping_surname"));
        assertEquals("shipping-phone", jsonParams.getString("shipping_phone"));
        assertEquals("shipping-line1", jsonParams.getString("shipping_line1"));
        assertEquals("shipping-line2", jsonParams.getString("shipping_line2"));
        assertFalse(jsonParams.has("shipping_line3"));
        assertEquals("shipping-city", jsonParams.getString("shipping_city"));
        assertEquals("shipping-state", jsonParams.getString("shipping_state"));
        assertEquals("shipping-postal-code", jsonParams.getString("shipping_postal_code"));
        assertEquals("shipping-country-code", jsonParams.getString("shipping_country_code"));
        assertEquals("shipping-method-indicator", jsonParams.getString("shipping_method_indicator"));
        assertEquals("product_code", jsonParams.getString("product_code"));
        assertEquals("delivery_timeframe", jsonParams.getString("delivery_timeframe"));
        assertEquals("delivery_email", jsonParams.getString("delivery_email"));
        assertEquals("reorder_indicator", jsonParams.getString("reorder_indicator"));
        assertEquals("preorder_indicator", jsonParams.getString("preorder_indicator"));
        assertEquals("preorder_date", jsonParams.getString("preorder_date"));
        assertEquals("gift_card_amount", jsonParams.getString("gift_card_amount"));
        assertEquals("gift_card_currency_code", jsonParams.getString("gift_card_currency_code"));
        assertEquals("gift_card_count", jsonParams.getString("gift_card_count"));
        assertEquals("account_age_indicator", jsonParams.getString("account_age_indicator"));
        assertEquals("account_create_date", jsonParams.getString("account_create_date"));
        assertEquals("account_change_indicator", jsonParams.getString("account_change_indicator"));
        assertEquals("account_change_date", jsonParams.getString("account_change_date"));
        assertEquals("account_pwd_change_indicator", jsonParams.getString("account_pwd_change_indicator"));
        assertEquals("account_pwd_change_date", jsonParams.getString("account_pwd_change_date"));
        assertEquals("shipping_address_usage_indicator", jsonParams.getString("shipping_address_usage_indicator"));
        assertEquals("shipping_address_usage_date", jsonParams.getString("shipping_address_usage_date"));
        assertEquals("transaction_count_day", jsonParams.getString("transaction_count_day"));
        assertEquals("transaction_count_year", jsonParams.getString("transaction_count_year"));
        assertEquals("add_card_attempts", jsonParams.getString("add_card_attempts"));
        assertEquals("account_purchases", jsonParams.getString("account_purchases"));
        assertEquals("fraud_activity", jsonParams.getString("fraud_activity"));
        assertEquals("shipping_name_indicator", jsonParams.getString("shipping_name_indicator"));
        assertEquals("payment_account_indicator", jsonParams.getString("payment_account_indicator"));
        assertEquals("payment_account_age", jsonParams.getString("payment_account_age"));
        assertEquals("address_match", jsonParams.getString("address_match"));
        assertEquals("account_id", jsonParams.getString("account_id"));
        assertEquals("ip_address", jsonParams.getString("ip_address"));
        assertEquals("order_description", jsonParams.getString("order_description"));
        assertEquals("tax_amount", jsonParams.getString("tax_amount"));
        assertEquals("user_agent", jsonParams.getString("user_agent"));
        assertEquals("authentication_indicator", jsonParams.getString("authentication_indicator"));
        assertEquals("installment", jsonParams.getString("installment"));
        assertEquals("purchase_date", jsonParams.getString("purchase_date"));
        assertEquals("recurring_end", jsonParams.getString("recurring_end"));
        assertEquals("recurring_frequency", jsonParams.getString("recurring_frequency"));
    }

    @Test
    public void testToJson_buildsEmptyParameters() {
        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation();

        JSONObject jsonParams = additionalInformation.toJson();

        assertTrue(jsonParams.isNull("shipping_given_name"));
        assertTrue(jsonParams.isNull("billingAddress"));
        assertTrue(jsonParams.isNull("firstName"));
        assertTrue(jsonParams.isNull("lastName"));
        assertTrue(jsonParams.isNull("shippingMethod"));
        assertTrue(jsonParams.isNull("mobilePhoneNumber"));
        assertTrue(jsonParams.isNull("shipping_method_indicator"));
        assertTrue(jsonParams.isNull("product_code"));
        assertTrue(jsonParams.isNull("delivery_timeframe"));
        assertTrue(jsonParams.isNull("delivery_email"));
        assertTrue(jsonParams.isNull("reorder_indicator"));
        assertTrue(jsonParams.isNull("preorder_indicator"));
        assertTrue(jsonParams.isNull("preorder_date"));
        assertTrue(jsonParams.isNull("gift_card_amount"));
        assertTrue(jsonParams.isNull("gift_card_currency_code"));
        assertTrue(jsonParams.isNull("gift_card_count"));
        assertTrue(jsonParams.isNull("account_age_indicator"));
        assertTrue(jsonParams.isNull("account_create_date"));
        assertTrue(jsonParams.isNull("account_change_indicator"));
        assertTrue(jsonParams.isNull("account_change_date"));
        assertTrue(jsonParams.isNull("account_pwd_change_indicator"));
        assertTrue(jsonParams.isNull("account_pwd_change_date"));
        assertTrue(jsonParams.isNull("shipping_address_usage_indicator"));
        assertTrue(jsonParams.isNull("shipping_address_usage_date"));
        assertTrue(jsonParams.isNull("transaction_count_day"));
        assertTrue(jsonParams.isNull("transaction_count_year"));
        assertTrue(jsonParams.isNull("add_card_attempts"));
        assertTrue(jsonParams.isNull("account_purchases"));
        assertTrue(jsonParams.isNull("fraud_activity"));
        assertTrue(jsonParams.isNull("shipping_name_indicator"));
        assertTrue(jsonParams.isNull("payment_account_indicator"));
        assertTrue(jsonParams.isNull("payment_account_age"));
        assertTrue(jsonParams.isNull("address_match"));
        assertTrue(jsonParams.isNull("account_id"));
        assertTrue(jsonParams.isNull("ip_address"));
        assertTrue(jsonParams.isNull("order_description"));
        assertTrue(jsonParams.isNull("tax_amount"));
        assertTrue(jsonParams.isNull("user_agent"));
        assertTrue(jsonParams.isNull("authentication_indicator"));
        assertTrue(jsonParams.isNull("installment"));
        assertTrue(jsonParams.isNull("purchase_date"));
        assertTrue(jsonParams.isNull("recurring_end"));
        assertTrue(jsonParams.isNull("recurring_frequency"));
    }
}
