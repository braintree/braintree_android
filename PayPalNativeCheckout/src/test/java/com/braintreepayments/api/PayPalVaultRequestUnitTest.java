package com.braintreepayments.api;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PayPalVaultRequestUnitTest {

    @Test
    public void newPayPalVaultRequest_setsDefaultValues() {
        PayPalNativeCheckoutVaultRequest request = new PayPalNativeCheckoutVaultRequest();

        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getDisplayName());
        assertFalse(request.getShouldOfferCredit());
    }

    @Test
    public void setsValuesCorrectly() {
        PostalAddress postalAddress = new PostalAddress();
        PayPalNativeCheckoutVaultRequest request = new PayPalNativeCheckoutVaultRequest();
        request.setLocaleCode("US");
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressOverride(postalAddress);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setShouldOfferCredit(true);

        assertEquals("US", request.getLocaleCode());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals("Display Name", request.getDisplayName());
        assertEquals("123-correlation", request.getRiskCorrelationId());
        assertTrue(request.getShouldOfferCredit());
    }

    @Test
    public void parcelsCorrectly() {
        PayPalNativeCheckoutVaultRequest request = new PayPalNativeCheckoutVaultRequest();
        request.setLocaleCode("en-US");
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressEditable(true);
        request.setShouldOfferCredit(true);

        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setRecipientName("Postal Address");
        request.setShippingAddressOverride(postalAddress);

        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setMerchantAccountId("merchant_account_id");

        ArrayList<PayPalNativeCheckoutLineItem> lineItems = new ArrayList<>();
        lineItems.add(new PayPalNativeCheckoutLineItem(PayPalNativeCheckoutLineItem.KIND_DEBIT, "An Item", "1", "1"));
        request.setLineItems(lineItems);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PayPalNativeCheckoutVaultRequest result = PayPalNativeCheckoutVaultRequest.CREATOR.createFromParcel(parcel);

        assertEquals("en-US", result.getLocaleCode());
        assertEquals("Billing Agreement Description",
                result.getBillingAgreementDescription());
        assertTrue(result.getShouldOfferCredit());
        assertTrue(result.isShippingAddressRequired());
        assertEquals("Display Name", result.getDisplayName());
        assertEquals("123-correlation", result.getRiskCorrelationId());
        assertEquals("merchant_account_id", result.getMerchantAccountId());
        assertEquals(1, result.getLineItems().size());
        assertEquals("An Item", result.getLineItems().get(0).getName());
    }
}
