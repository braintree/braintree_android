package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.TestClientTokenBuilder;

public class VenmoTest extends AndroidTestCase {

    public void testGetPackage() {
        assertEquals("com.venmo", Venmo.PACKAGE_NAME);
    }

    public void testGetAppSwitchActivity() {
        assertEquals("CardChooserActivity", Venmo.APP_SWITCH_ACTIVITY);
    }

    public void testGetCertificateSubject() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US",
                Venmo.CERTIFICATE_SUBJECT);
    }

    public void testGetCertificateIssuer() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US",
                Venmo.CERTIFICATE_ISSUER);
    }

    public void testGetPublicKeyHashCode() {
        assertEquals(-129711843, Venmo.PUBLIC_KEY_HASH_CODE);
    }

    public void testLaunchIntent() {
        Intent intent =
                Venmo.getLaunchIntent(Configuration.fromJson(new TestClientTokenBuilder().build()));

        assertEquals(new ComponentName("com.venmo", "com.venmo.CardChooserActivity"),
                intent.getComponent());
        assertEquals("integration2_merchant_id", intent.getStringExtra(
                Venmo.EXTRA_MERCHANT_ID));
    }

    public void testIntentIncludesVenmoEnvironment() {
        Intent intent = Venmo.getLaunchIntent(
                Configuration.fromJson(new TestClientTokenBuilder().withOfflineVenmo().build()));

        assertTrue(intent.getBooleanExtra(Venmo.EXTRA_OFFLINE, false));
    }

    public void testIntentIncludesLiveVenmoEnvironment() {
        Intent intent = Venmo.getLaunchIntent(
                Configuration.fromJson(new TestClientTokenBuilder().withLiveVenmo().build()));

        assertFalse(intent.getBooleanExtra(Venmo.EXTRA_OFFLINE, true));
    }

    public void testIsUnavailableWhenVenmoIsOff() {
        Configuration configuration = Configuration.fromJson(new TestClientTokenBuilder().build());
        assertFalse(Venmo.isAvailable(mContext, configuration));
        assertFalse(Venmo.getLaunchIntent(configuration).hasExtra(Venmo.EXTRA_OFFLINE));
    }

    public void testCanParseResponse() {
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "payment method nonce");
        assertEquals("payment method nonce", Venmo.handleAppSwitchResponse(intent));
    }

    public void testIsVenmoAppSwitchResponseReturnsTrueForAppSwitchResponses() {
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "");

        assertTrue(Venmo.isVenmoAppSwitchResponse(intent));
    }

    public void testIsVenmoAppSwitchResponseReturnsFalseForNonAppSwitchResponses() {
        assertFalse(Venmo.isVenmoAppSwitchResponse(new Intent()));
    }
}
