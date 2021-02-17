package com.braintreepayments.api;

import android.os.Parcel;

import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class GooglePayRequestUnitTest {

    @Test
    public void returnsAllValues() {
        ShippingAddressRequirements shippingAddressRequirements = ShippingAddressRequirements.newBuilder().build();
        TransactionInfo transactionInfo = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN)
                .build();

        GooglePayRequest request = new GooglePayRequest()
                .allowPrepaidCards(true)
                .billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL)
                .billingAddressRequired(true)
                .emailRequired(true)
                .phoneNumberRequired(true)
                .shippingAddressRequired(true)
                .shippingAddressRequirements(shippingAddressRequirements)
                .transactionInfo(transactionInfo)
                .environment("production")
                .googleMerchantId("google-merchant-id")
                .googleMerchantName("google-merchant-name");

        assertEquals(true, request.getAllowPrepaidCards().booleanValue());
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, request.getBillingAddressFormat().intValue());
        assertEquals(true, request.isBillingAddressRequired().booleanValue());
        assertEquals(true, request.isEmailRequired().booleanValue());
        assertEquals(true, request.isPhoneNumberRequired().booleanValue());
        assertEquals(true, request.isShippingAddressRequired().booleanValue());
        assertEquals(shippingAddressRequirements, request.getShippingAddressRequirements());
        assertEquals(transactionInfo, request.getTransactionInfo());
        assertEquals("PRODUCTION", request.getEnvironment());
        assertEquals("google-merchant-id", request.getGoogleMerchantId());
        assertEquals("google-merchant-name", request.getGoogleMerchantName());
    }

    @Test
    public void returnsNullForAllValuesWhenNotSet() {
        GooglePayRequest request = new GooglePayRequest();

        assertNull(request.getAllowPrepaidCards());
        assertNull(request.getBillingAddressFormat());
        assertNull(request.isBillingAddressRequired());
        assertNull(request.isEmailRequired());
        assertNull(request.isPhoneNumberRequired());
        assertNull(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressRequirements());
        assertNull(request.getTransactionInfo());
        assertNull(request.getEnvironment());
        assertNull(request.getEnvironment());
        assertNull(request.getGoogleMerchantId());
        assertNull(request.getGoogleMerchantName());
    }

    @Test
    public void parcelsCorrectly() {
        GooglePayRequest request = new GooglePayRequest();

        TransactionInfo info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("10")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build();

        request.transactionInfo(info);
        request.emailRequired(true);
        request.phoneNumberRequired(true);
        request.shippingAddressRequired(true);
        request.billingAddressRequired(true);
        request.billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL);

        ShippingAddressRequirements requirements = ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCode("US")
                .build();

        request.shippingAddressRequirements(requirements);
        request.allowPrepaidCards(true);
        request.environment("production");

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePayRequest parceled = GooglePayRequest.CREATOR.createFromParcel(parcel);

        assertEquals("USD", parceled.getTransactionInfo().getCurrencyCode());
        assertEquals("10", parceled.getTransactionInfo().getTotalPrice());
        assertEquals(WalletConstants.TOTAL_PRICE_STATUS_FINAL, parceled.getTransactionInfo().getTotalPriceStatus());
        assertTrue(parceled.isEmailRequired());
        assertTrue(parceled.isPhoneNumberRequired());
        assertTrue(parceled.isShippingAddressRequired());
        assertTrue(parceled.isBillingAddressRequired());
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, (int) parceled.getBillingAddressFormat());
        assertTrue(parceled.getShippingAddressRequirements().getAllowedCountryCodes().contains("US"));
        assertTrue(parceled.getAllowPrepaidCards());
        assertEquals("PRODUCTION", parceled.getEnvironment());
    }

    @Test
    public void parcelsCorrectly_allFieldsPopulated_null() throws NoSuchFieldException{
        GooglePayRequest request = new GooglePayRequest();

        TransactionInfo info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("10")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build();

        request.transactionInfo(info);
        request.billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL);

        ShippingAddressRequirements requirements = ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCode("US")
                .build();

        request.shippingAddressRequirements(requirements);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePayRequest parceled = GooglePayRequest.CREATOR.createFromParcel(parcel);

        assertEquals("USD", parceled.getTransactionInfo().getCurrencyCode());
        assertEquals("10", parceled.getTransactionInfo().getTotalPrice());
        assertEquals(WalletConstants.TOTAL_PRICE_STATUS_FINAL, parceled.getTransactionInfo().getTotalPriceStatus());
        assertNull(parceled.isEmailRequired());
        assertNull(parceled.isPhoneNumberRequired());
        assertNull(parceled.isShippingAddressRequired());
        assertNull(parceled.isBillingAddressRequired());
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, (int) parceled.getBillingAddressFormat());
        assertTrue(parceled.getShippingAddressRequirements().getAllowedCountryCodes().contains("US"));
        assertNull(parceled.getAllowPrepaidCards());
        assertNull(parceled.getEnvironment());
        assertNull(parceled.getGoogleMerchantId());
        assertNull(parceled.getGoogleMerchantName());
    }

    @Test
    public void generatesToJsonRequest() throws JSONException {
        GooglePayRequest request = new GooglePayRequest();
        String expected = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_REQUEST;
        List<String> shippingAllowedCountryCodes = Arrays.asList("US", "CA", "MX", "GB");

        ShippingAddressRequirements shippingAddressRequirements = ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCodes(shippingAllowedCountryCodes)
                .build();


        TransactionInfo info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("12.24")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build();

        JSONObject tokenizationSpecificationParams = new JSONObject()
                .put("type", "PAYMENT_GATEWAY")
                .put("parameters", new JSONObject()
                        .put("gateway", "braintree")
                        .put("braintree:apiVersion", "v1")
                        .put("braintree:sdkVersion", "BETA")
                        .put("braintree:merchantId", "BRAINTREE_MERCHANT_ID")
                        .put("braintree:authorizationFingerprint", "BRAINTREE_AUTH_FINGERPRINT")
                );

        JSONArray cardAllowedAuthMethods = new JSONArray()
                .put("PAN_ONLY")
                .put("CRYPTOGRAM_3DS");

        JSONArray cardAllowedCardNetworks = new JSONArray()
                .put("VISA")
                .put("AMEX")
                .put("JCB")
                .put("DISCOVER")
                .put("MASTERCARD");

        JSONObject cardAllowedPaymentMethodParams = new JSONObject()
                .put("allowedAuthMethods", cardAllowedAuthMethods)
                .put("allowedCardNetworks", cardAllowedCardNetworks);

        JSONObject paypalAllowedPaymentMethodParams = new JSONObject()
                .put("purchase_context", "{\"purchase_context\":{\"purchase_units\":[{\"payee\":{\"client_id\":\"FAKE_PAYPAL_CLIENT_ID\"},\"recurring_payment\":false}]}}");

        request.transactionInfo(info)
                .phoneNumberRequired(true)
                .emailRequired(true)
                .shippingAddressRequired(true)
                .shippingAddressRequirements(shippingAddressRequirements)
                .billingAddressRequired(true)
                .allowPrepaidCards(true)
                .setAllowedPaymentMethod("CARD", cardAllowedPaymentMethodParams)
                .setTokenizationSpecificationForType("CARD", tokenizationSpecificationParams)
                .setAllowedPaymentMethod("PAYPAL", paypalAllowedPaymentMethodParams)
                .setTokenizationSpecificationForType("PAYPAL", tokenizationSpecificationParams);

        request.environment("production");
        request.googleMerchantId("GOOGLE_MERCHANT_ID");
        request.googleMerchantName("GOOGLE_MERCHANT_NAME");

        String actual = request.toJson();

        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    public void allowsNullyOptionalParameters() throws JSONException {
        GooglePayRequest request = new GooglePayRequest();
        String expected = "{\"apiVersion\":2,\"apiVersionMinor\":0,\"allowedPaymentMethods\":[],\"shippingAddressRequired\":true,\"merchantInfo\":{},\"transactionInfo\":{\"totalPriceStatus\":\"FINAL\",\"totalPrice\":\"12.24\",\"currencyCode\":\"USD\"},\"shippingAddressParameters\":{}}";

        ShippingAddressRequirements nullyShippingAddressRequirements = ShippingAddressRequirements.newBuilder().build();

        TransactionInfo info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("12.24")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build();

        request.transactionInfo(info)
                .shippingAddressRequired(true)
                .shippingAddressRequirements(nullyShippingAddressRequirements);

        String actual = request.toJson();

        JSONAssert.assertEquals(expected, actual, false);
    }
}
