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

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureAdditionalInformationTest {

    @Test
    public void constructsCorrectly() {
        ThreeDSecurePostalAddress postalAddress = new ThreeDSecurePostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. 3")
                .locality("Oakland")
                .region("CA")
                .postalCode("94602")
                .countryCodeAlpha2("US")
                .firstName("John")
                .lastName("Fakerson");

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .billingAddress(postalAddress)
                .billingPhoneNumber("billing-phone-number")
                .billingGivenName("billing-given-name")
                .billingSurname("billing-surname")
                .email("email")
                .shippingMethod("shipping-method");

        assertEquals("billing-phone-number", additionalInformation.getBillingPhoneNumber());
        assertEquals("billing-given-name", additionalInformation.getBillingGivenName());
        assertEquals("billing-surname", additionalInformation.getBillingSurname());
        assertEquals("email", additionalInformation.getEmail());
        assertEquals("shipping-method", additionalInformation.getShippingMethod());
        assertEquals(postalAddress, additionalInformation.getBillingAddress());
    }

    @Test
    public void testWriteToParcel_serializesCorrectly() {
        ThreeDSecurePostalAddress postalAddress = new ThreeDSecurePostalAddress()
                .streetAddress("street-address")
                .extendedAddress("extended-address")
                .countryCodeAlpha2("country-code");

        ThreeDSecureAdditionalInformation preSerialized = new ThreeDSecureAdditionalInformation()
                .billingAddress(postalAddress)
                .billingPhoneNumber("billing-phone-number")
                .billingGivenName("billing-given-name")
                .billingSurname("billing-surname")
                .email("email")
                .shippingMethod("shipping-method")
                .shippingMethodIndicator("shipping-method-indicator")
                .productCode("productCode")
                .deliveryTimeframe("deliveryTimeframe")
                .deliveryEmail("deliveryEmail")
                .reorderIndicator("reorderIndicator")
                .preorderIndicator("preorderIndicator")
                .preorderDate("preorderDate")
                .giftCardAmount("giftCardAmount")
                .giftCardCurrencyCode("giftCardCurrencyCode")
                .giftCardCount("giftCardCount")
                .accountAgeIndicator("accountAgeIndicator")
                .accountCreateDate("accountCreateDate")
                .accountChangeIndicator("accountChangeIndicator")
                .accountChangeDate("accountChangeDate")
                .accountPwdChangeIndicator("accountPwdChangeIndicator")
                .accountPwdChangeDate("accountPwdChangeDate")
                .shippingAddressUsageIndicator("shippingAddressUsageIndicator")
                .shippingAddressUsageDate("shippingAddressUsageDate")
                .transactionCountDay("transactionCountDay")
                .transactionCountYear("transactionCountYear")
                .addCardAttempts("addCardAttempts")
                .accountPurchases("accountPurchases")
                .fraudActivity("fraudActivity")
                .shippingNameIndicator("shippingNameIndicator")
                .paymentAccountIndicator("paymentAccountIndicator")
                .paymentAccountAge("paymentAccountAge")
                .addressMatch("addressMatch")
                .accountId("accountId")
                .ipAddress("ipAddress")
                .orderDescription("orderDescription")
                .taxAmount("taxAmount")
                .userAgent("userAgent")
                .authenticationIndicator("authenticationIndicator")
                .installment("installment")
                .purchaseDate("purchaseDate")
                .recurringEnd("recurringEnd")
                .recurringFrequency("recurringFrequency");

        Parcel parcel = Parcel.obtain();
        preSerialized.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureAdditionalInformation postSerialized = ThreeDSecureAdditionalInformation.CREATOR.createFromParcel(parcel);

        assertNotNull(postSerialized);
        assertEquals("billing-phone-number", postSerialized.getBillingPhoneNumber());
        assertEquals("billing-given-name", postSerialized.getBillingGivenName());
        assertEquals("billing-surname", postSerialized.getBillingSurname());
        assertEquals("email", postSerialized.getEmail());
        assertEquals("shipping-method", postSerialized.getShippingMethod());
        assertEquals("country-code", postSerialized.getBillingAddress().getCountryCodeAlpha2());
        assertEquals("street-address", postSerialized.getBillingAddress().getStreetAddress());
        assertEquals("extended-address", postSerialized.getBillingAddress().getExtendedAddress());
        assertEquals("shipping-method", postSerialized.getShippingMethod());
        assertEquals("shipping-method-indicator", postSerialized.getShippingMethodIndicator());
        assertEquals("productCode", postSerialized.getProductCode());
        assertEquals("deliveryTimeframe", postSerialized.getDeliveryTimeframe());
        assertEquals("deliveryEmail", postSerialized.getDeliveryEmail());
        assertEquals("reorderIndicator", postSerialized.getReorderIndicator());
        assertEquals("preorderIndicator", postSerialized.getPreorderIndicator());
        assertEquals("preorderDate", postSerialized.getPreorderDate());
        assertEquals("giftCardAmount", postSerialized.getGiftCardAmount());
        assertEquals("giftCardCurrencyCode", postSerialized.getGiftCardCurrencyCode());
        assertEquals("giftCardCount", postSerialized.getGiftCardCount());
        assertEquals("accountAgeIndicator", postSerialized.getAccountAgeIndicator());
        assertEquals("accountCreateDate", postSerialized.getAccountCreateDate());
        assertEquals("accountChangeIndicator", postSerialized.getAccountChangeIndicator());
        assertEquals("accountChangeDate", postSerialized.getAccountChangeDate());
        assertEquals("accountPwdChangeIndicator", postSerialized.getAccountPwdChangeIndicator());
        assertEquals("accountPwdChangeDate", postSerialized.getAccountPwdChangeDate());
        assertEquals("shippingAddressUsageIndicator", postSerialized.getShippingAddressUsageIndicator());
        assertEquals("shippingAddressUsageDate", postSerialized.getShippingAddressUsageDate());
        assertEquals("transactionCountDay", postSerialized.getTransactionCountDay());
        assertEquals("transactionCountYear", postSerialized.getTransactionCountYear());
        assertEquals("addCardAttempts", postSerialized.getAddCardAttempts());
        assertEquals("accountPurchases", postSerialized.getAccountPurchases());
        assertEquals("fraudActivity", postSerialized.getFraudActivity());
        assertEquals("shippingNameIndicator", postSerialized.getShippingNameIndicator());
        assertEquals("paymentAccountIndicator", postSerialized.getPaymentAccountIdicator());
        assertEquals("paymentAccountAge", postSerialized.getPaymentAccountAge());
        assertEquals("addressMatch", postSerialized.getAddressMatch());
        assertEquals("accountId", postSerialized.getAccountId());
        assertEquals("ipAddress", postSerialized.getIpAddress());
        assertEquals("orderDescription", postSerialized.getOrderDescription());
        assertEquals("taxAmount", postSerialized.getTaxAmount());
        assertEquals("userAgent", postSerialized.getUserAgent());
        assertEquals("authenticationIndicator", postSerialized.getAuthenticationIndicator());
        assertEquals("installment", postSerialized.getInstallment());
        assertEquals("purchaseDate", postSerialized.getPurchaseDate());
        assertEquals("recurringEnd", postSerialized.getRecurringEnd());
        assertEquals("recurringFrequency", postSerialized.getRecurringFrequency());
    }

    @Test
    public void testToJson_buildsAllParameters() throws JSONException {
        ThreeDSecurePostalAddress postalAddress = new ThreeDSecurePostalAddress()
                .streetAddress("street-address")
                .extendedAddress("extended-address")
                .locality("locality");

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .billingAddress(postalAddress)
                .billingPhoneNumber("billing-phone-number")
                .billingGivenName("billing-given-name")
                .billingSurname("billing-surname")
                .email("email")
                .shippingMethod("shipping-method");

        JSONObject jsonParams = additionalInformation.toJson();

        assertEquals("billing-given-name", jsonParams.get("firstName"));
        assertEquals("billing-surname", jsonParams.get("lastName"));
        assertEquals("billing-phone-number", jsonParams.get("mobilePhoneNumber"));
        assertEquals("email", jsonParams.get("email"));
        assertEquals("shipping-method", jsonParams.get("shippingMethod"));
        assertEquals("street-address", jsonParams.get("line1"));
        assertEquals("extended-address", jsonParams.get("line2"));
        assertEquals("locality", jsonParams.get("city"));
    }

    @Test
    public void testToJson_buildsPartialParameters() throws JSONException {
        ThreeDSecurePostalAddress postalAddress = new ThreeDSecurePostalAddress()
                .streetAddress("street-address")
                .extendedAddress("extended-address")
                .locality("locality");

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .billingAddress(postalAddress)
                .billingSurname("billing-surname")
                .email("email")
                .shippingMethod("shipping-method")
                .shippingMethodIndicator("shipping-method-indicator")
                .productCode("productCode")
                .deliveryTimeframe("deliveryTimeframe")
                .deliveryEmail("deliveryEmail")
                .reorderIndicator("reorderIndicator")
                .preorderIndicator("preorderIndicator")
                .preorderDate("preorderDate")
                .giftCardAmount("giftCardAmount")
                .giftCardCurrencyCode("giftCardCurrencyCode")
                .giftCardCount("giftCardCount")
                .accountAgeIndicator("accountAgeIndicator")
                .accountCreateDate("accountCreateDate")
                .accountChangeIndicator("accountChangeIndicator")
                .accountChangeDate("accountChangeDate")
                .accountPwdChangeIndicator("accountPwdChangeIndicator")
                .accountPwdChangeDate("accountPwdChangeDate")
                .shippingAddressUsageIndicator("shippingAddressUsageIndicator")
                .shippingAddressUsageDate("shippingAddressUsageDate")
                .transactionCountDay("transactionCountDay")
                .transactionCountYear("transactionCountYear")
                .addCardAttempts("addCardAttempts")
                .accountPurchases("accountPurchases")
                .fraudActivity("fraudActivity")
                .shippingNameIndicator("shippingNameIndicator")
                .paymentAccountIndicator("paymentAccountIndicator")
                .paymentAccountAge("paymentAccountAge")
                .addressMatch("addressMatch")
                .accountId("accountId")
                .ipAddress("ipAddress")
                .orderDescription("orderDescription")
                .taxAmount("taxAmount")
                .userAgent("userAgent")
                .authenticationIndicator("authenticationIndicator")
                .installment("installment")
                .purchaseDate("purchaseDate")
                .recurringEnd("recurringEnd")
                .recurringFrequency("recurringFrequency");

        JSONObject jsonParams = additionalInformation.toJson();

        assertTrue(jsonParams.isNull("firstName"));
        assertEquals("billing-surname", jsonParams.get("lastName"));
        assertTrue(jsonParams.isNull("mobilePhoneNumber"));
        assertEquals("street-address", jsonParams.get("line1"));
        assertEquals("extended-address", jsonParams.get("line2"));
        assertTrue(jsonParams.isNull("postalCode"));
        assertTrue(jsonParams.isNull("state"));
        assertTrue(jsonParams.isNull("countryCode"));
        assertEquals("shipping-method-indicator", jsonParams.getString("shippingMethodIndicator"));
        assertEquals("productCode", jsonParams.getString("productCode"));
        assertEquals("deliveryTimeframe", jsonParams.getString("deliveryTimeframe"));
        assertEquals("deliveryEmail", jsonParams.getString("deliveryEmail"));
        assertEquals("reorderIndicator", jsonParams.getString("reorderIndicator"));
        assertEquals("preorderIndicator", jsonParams.getString("preorderIndicator"));
        assertEquals("preorderDate", jsonParams.getString("preorderDate"));
        assertEquals("giftCardAmount", jsonParams.getString("giftCardAmount"));
        assertEquals("giftCardCurrencyCode", jsonParams.getString("giftCardCurrencyCode"));
        assertEquals("giftCardCount", jsonParams.getString("giftCardCount"));
        assertEquals("accountAgeIndicator", jsonParams.getString("accountAgeIndicator"));
        assertEquals("accountCreateDate", jsonParams.getString("accountCreateDate"));
        assertEquals("accountChangeIndicator", jsonParams.getString("accountChangeIndicator"));
        assertEquals("accountChangeDate", jsonParams.getString("accountChangeDate"));
        assertEquals("accountPwdChangeIndicator", jsonParams.getString("accountPwdChangeIndicator"));
        assertEquals("accountPwdChangeDate", jsonParams.getString("accountPwdChangeDate"));
        assertEquals("shippingAddressUsageIndicator", jsonParams.getString("shippingAddressUsageIndicator"));
        assertEquals("shippingAddressUsageDate", jsonParams.getString("shippingAddressUsageDate"));
        assertEquals("transactionCountDay", jsonParams.getString("transactionCountDay"));
        assertEquals("transactionCountYear", jsonParams.getString("transactionCountYear"));
        assertEquals("addCardAttempts", jsonParams.getString("addCardAttempts"));
        assertEquals("accountPurchases", jsonParams.getString("accountPurchases"));
        assertEquals("fraudActivity", jsonParams.getString("fraudActivity"));
        assertEquals("shippingNameIndicator", jsonParams.getString("shippingNameIndicator"));
        assertEquals("paymentAccountIndicator", jsonParams.getString("paymentAccountIndicator"));
        assertEquals("paymentAccountAge", jsonParams.getString("paymentAccountAge"));
        assertEquals("addressMatch", jsonParams.getString("addressMatch"));
        assertEquals("accountId", jsonParams.getString("accountId"));
        assertEquals("ipAddress", jsonParams.getString("ipAddress"));
        assertEquals("orderDescription", jsonParams.getString("orderDescription"));
        assertEquals("taxAmount", jsonParams.getString("taxAmount"));
        assertEquals("userAgent", jsonParams.getString("userAgent"));
        assertEquals("authenticationIndicator", jsonParams.getString("authenticationIndicator"));
        assertEquals("installment", jsonParams.getString("installment"));
        assertEquals("purchaseDate", jsonParams.getString("purchaseDate"));
        assertEquals("recurringEnd", jsonParams.getString("recurringEnd"));
        assertEquals("recurringFrequency", jsonParams.getString("recurringFrequency"));
    }

    @Test
    public void testToJson_buildsEmptyParameters() {
        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation();

        JSONObject jsonParams = additionalInformation.toJson();

        assertTrue(jsonParams.isNull("billingAddress"));
        assertTrue(jsonParams.isNull("firstName"));
        assertTrue(jsonParams.isNull("lastName"));
        assertTrue(jsonParams.isNull("shippingMethod"));
        assertTrue(jsonParams.isNull("mobilePhoneNumber"));
        assertTrue(jsonParams.isNull("shippingMethodIndicator"));
        assertTrue(jsonParams.isNull("productCode"));
        assertTrue(jsonParams.isNull("deliveryTimeframe"));
        assertTrue(jsonParams.isNull("deliveryEmail"));
        assertTrue(jsonParams.isNull("reorderIndicator"));
        assertTrue(jsonParams.isNull("preorderIndicator"));
        assertTrue(jsonParams.isNull("preorderDate"));
        assertTrue(jsonParams.isNull("giftCardAmount"));
        assertTrue(jsonParams.isNull("giftCardCurrencyCode"));
        assertTrue(jsonParams.isNull("giftCardCount"));
        assertTrue(jsonParams.isNull("accountAgeIndicator"));
        assertTrue(jsonParams.isNull("accountCreateDate"));
        assertTrue(jsonParams.isNull("accountChangeIndicator"));
        assertTrue(jsonParams.isNull("accountChangeDate"));
        assertTrue(jsonParams.isNull("accountPwdChangeIndicator"));
        assertTrue(jsonParams.isNull("accountPwdChangeDate"));
        assertTrue(jsonParams.isNull("shippingAddressUsageIndicator"));
        assertTrue(jsonParams.isNull("shippingAddressUsageDate"));
        assertTrue(jsonParams.isNull("transactionCountDay"));
        assertTrue(jsonParams.isNull("transactionCountYear"));
        assertTrue(jsonParams.isNull("addCardAttempts"));
        assertTrue(jsonParams.isNull("accountPurchases"));
        assertTrue(jsonParams.isNull("fraudActivity"));
        assertTrue(jsonParams.isNull("shippingNameIndicator"));
        assertTrue(jsonParams.isNull("paymentAccountIndicator"));
        assertTrue(jsonParams.isNull("paymentAccountAge"));
        assertTrue(jsonParams.isNull("addressMatch"));
        assertTrue(jsonParams.isNull("accountId"));
        assertTrue(jsonParams.isNull("ipAddress"));
        assertTrue(jsonParams.isNull("orderDescription"));
        assertTrue(jsonParams.isNull("taxAmount"));
        assertTrue(jsonParams.isNull("userAgent"));
        assertTrue(jsonParams.isNull("authenticationIndicator"));
        assertTrue(jsonParams.isNull("installment"));
        assertTrue(jsonParams.isNull("purchaseDate"));
        assertTrue(jsonParams.isNull("recurringEnd"));
        assertTrue(jsonParams.isNull("recurringFrequency"));
    }
}
