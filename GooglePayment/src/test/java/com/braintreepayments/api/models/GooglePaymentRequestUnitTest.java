package com.braintreepayments.api.models;

import android.os.Parcel;

import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class GooglePaymentRequestUnitTest {

    @Test
    public void returnsAllValues() {
        ShippingAddressRequirements shippingAddressRequirements = ShippingAddressRequirements.newBuilder().build();
        TransactionInfo transactionInfo = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN)
                .build();

        GooglePaymentRequest request = new GooglePaymentRequest()
                .allowPrepaidCards(true)
                .billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL)
                .billingAddressRequired(true)
                .emailRequired(true)
                .phoneNumberRequired(true)
                .shippingAddressRequired(true)
                .shippingAddressRequirements(shippingAddressRequirements)
                .transactionInfo(transactionInfo)
                .uiRequired(true);

        assertEquals(true, request.getAllowPrepaidCards().booleanValue());
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, request.getBillingAddressFormat().intValue());
        assertEquals(true, request.isBillingAddressRequired().booleanValue());
        assertEquals(true, request.isEmailRequired().booleanValue());
        assertEquals(true, request.isPhoneNumberRequired().booleanValue());
        assertEquals(true, request.isShippingAddressRequired().booleanValue());
        assertEquals(shippingAddressRequirements, request.getShippingAddressRequirements());
        assertEquals(transactionInfo, request.getTransactionInfo());
        assertEquals(true, request.isUiRequired().booleanValue());
    }

    @Test
    public void returnsNullForAllValuesWhenNotSet() {
        GooglePaymentRequest request = new GooglePaymentRequest();

        assertNull(request.getAllowPrepaidCards());
        assertNull(request.getBillingAddressFormat());
        assertNull(request.isBillingAddressRequired());
        assertNull(request.isEmailRequired());
        assertNull(request.isPhoneNumberRequired());
        assertNull(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressRequirements());
        assertNull(request.getTransactionInfo());
        assertNull(request.isUiRequired());
    }

    @Test
    public void parcelsCorrectly_allFieldsPopulated_truthy() {
        GooglePaymentRequest request = new GooglePaymentRequest();

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
        request.uiRequired(true);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePaymentRequest parceled = GooglePaymentRequest.CREATOR.createFromParcel(parcel);

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
        assertTrue(parceled.isUiRequired());
    }

    @Test
    public void parcelsCorrectly_allFieldsPopulated_falsey() {
        GooglePaymentRequest request = new GooglePaymentRequest();

        TransactionInfo info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("10")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build();

        request.transactionInfo(info);
        request.emailRequired(false);
        request.phoneNumberRequired(false);
        request.shippingAddressRequired(false);
        request.billingAddressRequired(false);
        request.billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_FULL);

        ShippingAddressRequirements requirements = ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCode("US")
                .build();

        request.shippingAddressRequirements(requirements);
        request.allowPrepaidCards(false);
        request.uiRequired(false);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePaymentRequest parceled = GooglePaymentRequest.CREATOR.createFromParcel(parcel);

        assertEquals("USD", parceled.getTransactionInfo().getCurrencyCode());
        assertEquals("10", parceled.getTransactionInfo().getTotalPrice());
        assertEquals(WalletConstants.TOTAL_PRICE_STATUS_FINAL, parceled.getTransactionInfo().getTotalPriceStatus());
        assertFalse(parceled.isEmailRequired());
        assertFalse(parceled.isPhoneNumberRequired());
        assertFalse(parceled.isShippingAddressRequired());
        assertFalse(parceled.isBillingAddressRequired());
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, (int) parceled.getBillingAddressFormat());
        assertTrue(parceled.getShippingAddressRequirements().getAllowedCountryCodes().contains("US"));
        assertFalse(parceled.getAllowPrepaidCards());
        assertFalse(parceled.isUiRequired());
    }

    @Test
    public void parcelsCorrectly_allFieldsPopulated_null() {
        GooglePaymentRequest request = new GooglePaymentRequest();

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

        GooglePaymentRequest parceled = GooglePaymentRequest.CREATOR.createFromParcel(parcel);

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
        assertNull(parceled.isUiRequired());
    }
}
