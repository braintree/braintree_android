package com.braintreepayments.api;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
@RunWith(RobolectricTestRunner.class)
public class VenmoRequestUnitTest {

    @Test
    public void getPaymentMethodUsageAsString_whenSingleUse_returnsStringEquivalent() {
        VenmoRequest sut = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        assertEquals("SINGLE_USE", sut.getPaymentMethodUsageAsString());
    }

    @Test
    public void getPaymentMethodUsageAsString_whenMultiUse_returnsStringEquivalent() {
        VenmoRequest sut = new VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE);
        assertEquals("MULTI_USE", sut.getPaymentMethodUsageAsString());
    }

    @Test
    public void getCollectCustomerShippingAddressAsString_returnsStringEquivalent() {
        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE);
        request.setCollectCustomerShippingAddress(true);
        assertEquals("true", request.getCollectCustomerShippingAddressAsString());

        request = new VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE);
        request.setCollectCustomerShippingAddress(false);
        assertEquals("false", request.getCollectCustomerShippingAddressAsString());
    }

    @Test
    public void getCollectCustomerBillingAddressAsString_returnsStringEquivalent() {
        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE);
        request.setCollectCustomerBillingAddress(true);
        assertEquals("true", request.getCollectCustomerBillingAddressAsString());

        request = new VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE);
        request.setCollectCustomerBillingAddress(false);
        assertEquals("false", request.getCollectCustomerBillingAddressAsString());
    }

    @Test
    public void parcelsCorrectly() {
        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE);
        request.setDisplayName("venmo-user");
        request.setShouldVault(true);
        request.setProfileId("profile-id");
        request.setCollectCustomerBillingAddress(true);
        request.setCollectCustomerShippingAddress(true);
        request.setSubTotalAmount("10.00");
        request.setTaxAmount("1.00");
        request.setDiscountAmount("2.00");
        request.setShippingAmount("1.00");
        request.setTotalAmount("10.00");

        ArrayList<VenmoLineItem> lineItems = new ArrayList<>();
        lineItems.add(new VenmoLineItem(VenmoLineItem.KIND_DEBIT, "An Item", 1, "10.00"));
        request.setLineItems(lineItems);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VenmoRequest result = VenmoRequest.CREATOR.createFromParcel(parcel);

        assertEquals(VenmoPaymentMethodUsage.MULTI_USE, result.getPaymentMethodUsage());
        assertEquals("venmo-user", result.getDisplayName());
        assertTrue(result.getShouldVault());
        assertEquals("profile-id", result.getProfileId());
        assertTrue(result.getCollectCustomerBillingAddress());
        assertTrue(result.getCollectCustomerShippingAddress());
        assertEquals("10.00", result.getSubTotalAmount());
        assertEquals("1.00", result.getTaxAmount());
        assertEquals("2.00", result.getDiscountAmount());
        assertEquals("1.00", result.getShippingAmount());
        assertEquals("10.00", result.getTotalAmount());
        assertEquals(1, result.getLineItems().size());
        assertEquals("An Item", result.getLineItems().get(0).getName());
    }
}