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
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class GooglePayRequestUnitTest {

    @Test
    public void returnsAllValues() {
        ShippingAddressRequirements shippingAddressRequirements = ShippingAddressRequirements.newBuilder().build();
        TransactionInfo transactionInfo = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN)
                .build();

        GooglePayRequest request = new GooglePayRequest();
        request.setAllowPrepaidCards(true);
        request.setBillingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL);
        request.setBillingAddressRequired(true);
        request.setEmailRequired(true);
        request.setPhoneNumberRequired(true);
        request.setShippingAddressRequired(true);
        request.setShippingAddressRequirements(shippingAddressRequirements);
        request.setTransactionInfo(transactionInfo);
        request.setEnvironment("production");
        request.setGoogleMerchantId("google-merchant-id");
        request.setGoogleMerchantName("google-merchant-name");

        assertTrue(request.getAllowPrepaidCards());
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, request.getBillingAddressFormat());
        assertTrue(request.isBillingAddressRequired());
        assertTrue(request.isEmailRequired());
        assertTrue(request.isPhoneNumberRequired());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(shippingAddressRequirements, request.getShippingAddressRequirements());
        assertEquals(transactionInfo, request.getTransactionInfo());
        assertEquals("PRODUCTION", request.getEnvironment());
        assertEquals("google-merchant-id", request.getGoogleMerchantId());
        assertEquals("google-merchant-name", request.getGoogleMerchantName());
    }

    @Test
    public void constructor_setsDefaultValues() {
        GooglePayRequest request = new GooglePayRequest();

        assertFalse(request.getAllowPrepaidCards());
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_MIN, request.getBillingAddressFormat());
        assertFalse(request.isBillingAddressRequired());
        assertFalse(request.isEmailRequired());
        assertFalse(request.isPhoneNumberRequired());
        assertFalse(request.isShippingAddressRequired());
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

        request.setTransactionInfo(info);
        request.setEmailRequired(true);
        request.setPhoneNumberRequired(true);
        request.setShippingAddressRequired(true);
        request.setBillingAddressRequired(true);
        request.setBillingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL);

        ShippingAddressRequirements requirements = ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCode("US")
                .build();

        request.setShippingAddressRequirements(requirements);
        request.setAllowPrepaidCards(true);
        request.setEnvironment("production");

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
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, parceled.getBillingAddressFormat());
        assertTrue(parceled.getShippingAddressRequirements().getAllowedCountryCodes().contains("US"));
        assertTrue(parceled.getAllowPrepaidCards());
        assertEquals("PRODUCTION", parceled.getEnvironment());
    }

    @Test
    public void parcelsCorrectly_allFieldsPopulated_null() {
        GooglePayRequest request = new GooglePayRequest();

        TransactionInfo info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("10")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build();

        request.setTransactionInfo(info);
        request.setBillingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL);

        ShippingAddressRequirements requirements = ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCode("US")
                .build();

        request.setShippingAddressRequirements(requirements);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePayRequest parceled = GooglePayRequest.CREATOR.createFromParcel(parcel);

        assertEquals("USD", parceled.getTransactionInfo().getCurrencyCode());
        assertEquals("10", parceled.getTransactionInfo().getTotalPrice());
        assertEquals(WalletConstants.TOTAL_PRICE_STATUS_FINAL, parceled.getTransactionInfo().getTotalPriceStatus());
        assertFalse(parceled.isEmailRequired());
        assertFalse(parceled.isPhoneNumberRequired());
        assertFalse(parceled.isShippingAddressRequired());
        assertFalse(parceled.isBillingAddressRequired());
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, parceled.getBillingAddressFormat());
        assertTrue(parceled.getShippingAddressRequirements().getAllowedCountryCodes().contains("US"));
        assertFalse(parceled.getAllowPrepaidCards());
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

        request.setTransactionInfo(info);
        request.setCountryCode("US");
        request.setPhoneNumberRequired(true);
        request.setEmailRequired(true);
        request.setShippingAddressRequired(true);
        request.setShippingAddressRequirements(shippingAddressRequirements);
        request.setBillingAddressRequired(true);
        request.setAllowPrepaidCards(true);
        request.setAllowedPaymentMethod("CARD", cardAllowedPaymentMethodParams);
        request.setTokenizationSpecificationForType("CARD", tokenizationSpecificationParams);
        request.setAllowedPaymentMethod("PAYPAL", paypalAllowedPaymentMethodParams);
        request.setTokenizationSpecificationForType("PAYPAL", tokenizationSpecificationParams);

        request.setEnvironment("production");
        request.setGoogleMerchantId("GOOGLE_MERCHANT_ID");
        request.setGoogleMerchantName("GOOGLE_MERCHANT_NAME");

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

        request.setTransactionInfo(info);
        request.setShippingAddressRequired(true);
        request.setShippingAddressRequirements(nullyShippingAddressRequirements);

        String actual = request.toJson();

        JSONAssert.assertEquals(expected, actual, false);
    }
}
