package com.braintreepayments.api.googlepay;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import android.os.Parcel;

import com.braintreepayments.api.testutils.Fixtures;
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

@RunWith(RobolectricTestRunner.class)
public class GooglePayRequestUnitTest {

    @Test
    public void returnsAllValues() {
        GooglePayShippingAddressParameters shippingAddressRequirements = new GooglePayShippingAddressParameters();

        GooglePayRequest request = new GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL);
        request.setTotalPriceLabel("test");
        request.setAllowPrepaidCards(true);
        request.setBillingAddressFormat(GooglePayBillingAddressFormat.FULL);
        request.setBillingAddressRequired(true);
        request.setEmailRequired(true);
        request.setPhoneNumberRequired(true);
        request.setShippingAddressRequired(true);
        request.setShippingAddressParameters(shippingAddressRequirements);
        request.setEnvironment("production");
        request.setGoogleMerchantName("google-merchant-name");

        assertTrue(request.getAllowPrepaidCards());
        assertEquals(GooglePayBillingAddressFormat.FULL, request.getBillingAddressFormat());
        assertTrue(request.isBillingAddressRequired());
        assertTrue(request.isEmailRequired());
        assertTrue(request.isPhoneNumberRequired());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(shippingAddressRequirements, request.getShippingAddressParameters());
        assertEquals("USD", request.getCurrencyCode());
        assertEquals("1.00", request.getTotalPrice());
        assertEquals(GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL, request.getTotalPriceStatus());
        assertEquals("PRODUCTION", request.getEnvironment());
        assertEquals("google-merchant-name", request.getGoogleMerchantName());
        assertEquals("test", request.getTotalPriceLabel());
    }

    @Test
    public void constructor_setsDefaultValues() {
        GooglePayRequest request = new GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL);

        assertFalse(request.getAllowPrepaidCards());
        assertEquals(GooglePayBillingAddressFormat.MIN, request.getBillingAddressFormat());
        assertFalse(request.isBillingAddressRequired());
        assertFalse(request.isEmailRequired());
        assertFalse(request.isPhoneNumberRequired());
        assertFalse(request.isShippingAddressRequired());
        assertTrue(request.getAllowCreditCards());
        assertNull(request.getShippingAddressParameters());
        assertNull(request.getEnvironment());
        assertNull(request.getEnvironment());
        assertNull(request.getGoogleMerchantName());
    }

    @Test
    public void parcelsCorrectly() {
        GooglePayRequest request = new GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL);

        request.setTotalPriceLabel("test");
        request.setEmailRequired(true);
        request.setPhoneNumberRequired(true);
        request.setShippingAddressRequired(true);
        request.setBillingAddressRequired(true);
        request.setBillingAddressFormat(GooglePayBillingAddressFormat.FULL);

        GooglePayShippingAddressParameters requirements = new GooglePayShippingAddressParameters(
                List.of("US"), true);

        request.setShippingAddressParameters(requirements);
        request.setAllowPrepaidCards(true);
        request.setEnvironment("production");

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePayRequest parceled = GooglePayRequest.CREATOR.createFromParcel(parcel);

        assertEquals("USD", parceled.getCurrencyCode());
        assertEquals("1.00", parceled.getTotalPrice());
        assertEquals(GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL, parceled.getTotalPriceStatus());
        assertEquals("test", parceled.getTotalPriceLabel());
        assertTrue(parceled.isEmailRequired());
        assertTrue(parceled.isPhoneNumberRequired());
        assertTrue(parceled.isShippingAddressRequired());
        assertTrue(parceled.isBillingAddressRequired());
        assertEquals(GooglePayBillingAddressFormat.FULL, parceled.getBillingAddressFormat());
        assertTrue(parceled.getShippingAddressParameters().getAllowedCountryCodes().contains("US"));
        assertTrue(parceled.getAllowPrepaidCards());
        assertEquals("PRODUCTION", parceled.getEnvironment());
    }

    @Test
    public void parcelsCorrectly_allFieldsPopulated_null() {
        GooglePayRequest request = new GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL);

        TransactionInfo info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("10")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build();

        request.setBillingAddressFormat(GooglePayBillingAddressFormat.FULL);

        GooglePayShippingAddressParameters requirements = new GooglePayShippingAddressParameters(
                List.of("US"), true);

        request.setShippingAddressParameters(requirements);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePayRequest parceled = GooglePayRequest.CREATOR.createFromParcel(parcel);

        assertEquals("USD", parceled.getCurrencyCode());
        assertEquals("1.00", parceled.getTotalPrice());
        assertEquals(GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL, parceled.getTotalPriceStatus());
        assertFalse(parceled.isEmailRequired());
        assertFalse(parceled.isPhoneNumberRequired());
        assertFalse(parceled.isShippingAddressRequired());
        assertFalse(parceled.isBillingAddressRequired());
        assertEquals(GooglePayBillingAddressFormat.FULL, parceled.getBillingAddressFormat());
        assertTrue(parceled.getShippingAddressParameters().getAllowedCountryCodes().contains("US"));
        assertFalse(parceled.getAllowPrepaidCards());
        assertNull(parceled.getEnvironment());
        assertNull(parceled.getGoogleMerchantName());
        assertNull(parceled.getTotalPriceLabel());
    }

    @Test
    public void generatesToJsonRequest() throws JSONException {
        GooglePayRequest request = new GooglePayRequest("USD", "12.24", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL);
        String expected = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_REQUEST;
        List<String> shippingAllowedCountryCodes = Arrays.asList("US", "CA", "MX", "GB");

        GooglePayShippingAddressParameters requirements = new GooglePayShippingAddressParameters(
                shippingAllowedCountryCodes, true);

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

        request.setCountryCode("US");
        request.setPhoneNumberRequired(true);
        request.setEmailRequired(true);
        request.setShippingAddressRequired(true);
        request.setShippingAddressParameters(requirements);
        request.setBillingAddressRequired(true);
        request.setAllowPrepaidCards(true);
        request.setAllowCreditCards(true);
        request.setAllowedPaymentMethod("CARD", cardAllowedPaymentMethodParams);
        request.setTokenizationSpecificationForType("CARD", tokenizationSpecificationParams);
        request.setAllowedPaymentMethod("PAYPAL", paypalAllowedPaymentMethodParams);
        request.setTokenizationSpecificationForType("PAYPAL", tokenizationSpecificationParams);
        request.setTotalPriceLabel("Test Label");

        request.setEnvironment("production");
        request.setGoogleMerchantName("GOOGLE_MERCHANT_NAME");

        String actual = request.toJson();

        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    public void generatesToJsonRequest_whenCreditCardNotAllowed_billingAddressRequired() throws JSONException {
        GooglePayRequest request = new GooglePayRequest("USD", "12.24", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL);
        String expected = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_REQUEST_NO_CREDIT_CARDS;
        List<String> shippingAllowedCountryCodes = Arrays.asList("US", "CA", "MX", "GB");

        GooglePayShippingAddressParameters requirements = new GooglePayShippingAddressParameters(
                shippingAllowedCountryCodes, true);

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

        request.setCountryCode("US");
        request.setPhoneNumberRequired(true);
        request.setEmailRequired(true);
        request.setShippingAddressRequired(true);
        request.setShippingAddressParameters(requirements);
        request.setBillingAddressRequired(true);
        request.setAllowPrepaidCards(true);
        request.setAllowCreditCards(false);
        request.setAllowedPaymentMethod("CARD", cardAllowedPaymentMethodParams);
        request.setTokenizationSpecificationForType("CARD", tokenizationSpecificationParams);
        request.setAllowedPaymentMethod("PAYPAL", paypalAllowedPaymentMethodParams);
        request.setTokenizationSpecificationForType("PAYPAL", tokenizationSpecificationParams);

        request.setEnvironment("production");
        request.setGoogleMerchantName("GOOGLE_MERCHANT_NAME");

        String actual = request.toJson();

        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    public void allowsNullyOptionalParameters() throws JSONException {
        GooglePayRequest request = new GooglePayRequest("USD", "12.24", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL);
        String expected = "{\"apiVersion\":2,\"apiVersionMinor\":0,\"allowedPaymentMethods\":[],\"shippingAddressRequired\":true,\"merchantInfo\":{},\"transactionInfo\":{\"totalPriceStatus\":\"FINAL\",\"totalPrice\":\"12.24\",\"currencyCode\":\"USD\"},\"shippingAddressParameters\":{}}";

        GooglePayShippingAddressParameters nullyShippingAddressRequirements = new GooglePayShippingAddressParameters();

        request.setShippingAddressRequired(true);
        request.setShippingAddressParameters(nullyShippingAddressRequirements);

        String actual = request.toJson();

        JSONAssert.assertEquals(expected, actual, false);
    }
}
