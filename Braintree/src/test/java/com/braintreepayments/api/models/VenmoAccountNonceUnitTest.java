package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.models.BinData.NO;
import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.api.models.BinData.YES;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class VenmoAccountNonceUnitTest {

    private static final String NONCE = "nonce";
    private static final String DESCRIPTION = "description";
    private static final String USERNAME = "username";
    private static final VenmoAccountNonce VENMO_NONCE = new VenmoAccountNonce(NONCE, DESCRIPTION, USERNAME);

    @Test
    public void fromJson_parsesResponse() throws JSONException {
        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.
                fromJson(stringFromFixture("payment_methods/venmo_account_response.json"));

        assertEquals("venmojoe", venmoAccountNonce.getDescription());
        assertEquals("venmojoe", venmoAccountNonce.getUsername());
        assertEquals("fake-venmo-nonce", venmoAccountNonce.getNonce());
        assertEquals("Venmo", venmoAccountNonce.getTypeLabel());
        assertNotNull(venmoAccountNonce.getBinData());
        BinData binData = venmoAccountNonce.getBinData();
        assertEquals(UNKNOWN, binData.getPrepaid());
        assertEquals(YES, binData.getHealthcare());
        assertEquals(NO, binData.getDebit());
        assertEquals(UNKNOWN, binData.getDurbinRegulated());
        assertEquals(UNKNOWN, binData.getCommercial());
        assertEquals(UNKNOWN, binData.getPayroll());
        assertEquals(UNKNOWN, binData.getIssuingBank());
        assertEquals("Something", binData.getCountryOfIssuance());
        assertEquals("123", binData.getProductId());
    }

    @Test
    public void getTypeLabel_returnsPayWithVenmo() {
        assertEquals("Venmo", VENMO_NONCE.getTypeLabel());
    }

    @Test
    public void getNonce_returnsNonce() {
        assertEquals(NONCE, VENMO_NONCE.getNonce());
    }

    @Test
    public void getDescription_returnsDescription() {
        assertEquals(DESCRIPTION, VENMO_NONCE.getDescription());
    }

    @Test
    public void getUsername_returnsUsername() {
        assertEquals(USERNAME, VENMO_NONCE.getUsername());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJson(
                stringFromFixture("payment_methods/venmo_account_response.json"));
        Parcel parcel = Parcel.obtain();
        venmoAccountNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        VenmoAccountNonce parceled = VenmoAccountNonce.CREATOR.createFromParcel(parcel);

        assertEquals(venmoAccountNonce.getNonce(), parceled.getNonce());
        assertEquals(venmoAccountNonce.getDescription(), parceled.getDescription());
        assertEquals(venmoAccountNonce.getUsername(), parceled.getUsername());
        assertBinData(venmoAccountNonce.getBinData(), parceled.getBinData());
    }

    private void assertBinData(BinData expected, BinData actual) {
        assertEquals(expected.getPrepaid(), actual.getPrepaid());
        assertEquals(expected.getHealthcare(), actual.getHealthcare());
        assertEquals(expected.getDebit(), actual.getDebit());
        assertEquals(expected.getDurbinRegulated(), actual.getDurbinRegulated());
        assertEquals(expected.getCommercial(), actual.getCommercial());
        assertEquals(expected.getPayroll(), actual.getPayroll());
        assertEquals(expected.getIssuingBank(), actual.getIssuingBank());
        assertEquals(expected.getCountryOfIssuance(), actual.getCountryOfIssuance());
        assertEquals(expected.getProductId(), actual.getProductId());
    }
}
