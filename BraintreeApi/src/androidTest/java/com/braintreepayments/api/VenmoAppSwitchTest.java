package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.TestClientTokenBuilder;

public class VenmoAppSwitchTest extends AndroidTestCase {

    private VenmoAppSwitch mVenmoAppSwitch;

    @Override
    public void setUp() {
        Configuration configuration = Configuration.fromJson(new TestClientTokenBuilder().build());
        mVenmoAppSwitch = new VenmoAppSwitch(getContext(), configuration);
    }

    public void testGetPackage() {
        assertEquals("com.venmo", mVenmoAppSwitch.getPackage());
    }

    public void testGetAppSwitchActivity() {
        assertEquals("CardChooserActivity", mVenmoAppSwitch.getAppSwitchActivity());
    }

    public void testGetCertificateSubject() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US", mVenmoAppSwitch.getCertificateSubject());
    }

    public void testGetCertificateIssuer() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US", mVenmoAppSwitch.getCertificateIssuer());
    }

    public void testGetPublicKeyHashCode() {
        assertEquals(-129711843, mVenmoAppSwitch.getPublicKeyHashCode());
    }

    public void testLaunchIntent() {
        Intent intent = mVenmoAppSwitch.getLaunchIntent();

        assertEquals(new ComponentName("com.venmo", "com.venmo.CardChooserActivity"),
                intent.getComponent());
        assertEquals("integration2_merchant_id", intent.getStringExtra(VenmoAppSwitch.EXTRA_MERCHANT_ID));
    }

    public void testIntentIncludesVenmoEnvironment() {
        Configuration configuration = Configuration.fromJson(new TestClientTokenBuilder().withOfflineVenmo().build());
        VenmoAppSwitch venmoAppSwitch = new VenmoAppSwitch(getContext(), configuration);
        Intent intent = venmoAppSwitch.getLaunchIntent();

        assertTrue(intent.getBooleanExtra(AppSwitch.EXTRA_OFFLINE, false));
    }

    public void testIntentIncludesLiveVenmoEnvironment() {
        Configuration configuration = Configuration.fromJson(new TestClientTokenBuilder().withLiveVenmo().build());
        VenmoAppSwitch venmoAppSwitch = new VenmoAppSwitch(getContext(), configuration);
        Intent intent = venmoAppSwitch.getLaunchIntent();

        assertFalse(intent.getBooleanExtra(AppSwitch.EXTRA_OFFLINE, true));
    }

    public void testIsUnavailableWhenVenmoIsOff() {
        assertFalse(mVenmoAppSwitch.isAvailable());
        assertFalse(mVenmoAppSwitch.getLaunchIntent().hasExtra(AppSwitch.EXTRA_OFFLINE));
    }

    public void testCanParseResponse() {
        Intent intent = new Intent().putExtra(AppSwitch.EXTRA_PAYMENT_METHOD_NONCE, "payment method nonce");
        assertEquals("payment method nonce",
                mVenmoAppSwitch.handleAppSwitchResponse(Activity.RESULT_OK, intent));
    }

    public void testHandleResponseReturnsNullOnUnsuccessfulResultCode() {
        assertNull(mVenmoAppSwitch.handleAppSwitchResponse(Activity.RESULT_CANCELED, new Intent()));
    }
}
