package com.braintreepayments.api;

import static junit.framework.TestCase.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.os.Parcel;

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
    public void parcelsCorrectly() {
        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE);
        request.setDisplayName("venmo-user");
        request.setShouldVault(true);
        request.setProfileId("profile-id");

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VenmoRequest result = VenmoRequest.CREATOR.createFromParcel(parcel);

        assertEquals(VenmoPaymentMethodUsage.MULTI_USE, result.getPaymentMethodUsage());
        assertEquals("venmo-user", result.getDisplayName());
        assertTrue(result.getShouldVault());
        assertEquals("profile-id", result.getProfileId());
    }
}