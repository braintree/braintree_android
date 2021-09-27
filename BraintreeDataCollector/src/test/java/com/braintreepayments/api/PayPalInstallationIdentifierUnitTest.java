package com.braintreepayments.api;

import static com.braintreepayments.api.SharedPreferencesHelper.getSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.test.core.app.ApplicationProvider;

@RunWith(RobolectricTestRunner.class)
public class PayPalInstallationIdentifierUnitTest {

    private Context context;
    private SharedPreferences prefs;
    private BraintreeSharedPreferences braintreeSharedPreferences;

    @Before
    public void setup() throws GeneralSecurityException, IOException {
        context = ApplicationProvider.getApplicationContext();
        prefs = getSharedPreferences(context, "com.braintreepayments.api.paypal");
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
        when(braintreeSharedPreferences.getSharedPreferences(context, "com.braintreepayments.api.paypal")).thenReturn(prefs);
        prefs.edit().clear().apply();
    }

    @Test
    public void getInstallationGUID_returnsNewGUIDWhenOneDoesNotExistAndPersistsIt() {
        PayPalInstallationIdentifier sut = new PayPalInstallationIdentifier();

        assertNull(prefs.getString("InstallationGUID", null));

        assertNotNull(sut.getInstallationGUID(context, braintreeSharedPreferences));

        assertNotNull(prefs.getString("InstallationGUID", null));
    }

    @Test
    public void getInstallationGUID_returnsExistingGUIDWhenOneExist() {
        PayPalInstallationIdentifier sut = new PayPalInstallationIdentifier();

        prefs.edit()
                .putString("InstallationGUID", UUID.randomUUID().toString())
                .apply();
        String existingGUID = prefs.getString("InstallationGUID", null);
        assertNotNull(existingGUID);

        assertEquals(existingGUID, sut.getInstallationGUID(context, braintreeSharedPreferences));

        assertEquals(existingGUID, prefs.getString("InstallationGUID", null));
    }
}
