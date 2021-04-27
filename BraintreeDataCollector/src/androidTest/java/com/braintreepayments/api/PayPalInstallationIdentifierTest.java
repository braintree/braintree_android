package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PayPalInstallationIdentifierTest {

    private SharedPreferences prefs;

    @Before
    public void setup() {
        prefs = ApplicationProvider.getApplicationContext().getSharedPreferences("com.braintreepayments.api.paypal", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    @Test
    public void getInstallationGUID_returnsNewGUIDWhenOneDoesNotExistAndPersistsIt() {
        PayPalInstallationIdentifier sut = new PayPalInstallationIdentifier();

        assertNull(prefs.getString("InstallationGUID", null));

        assertNotNull(sut.getInstallationGUID(ApplicationProvider.getApplicationContext()));

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

        assertEquals(existingGUID, sut.getInstallationGUID(ApplicationProvider.getApplicationContext()));

        assertEquals(existingGUID, prefs.getString("InstallationGUID", null));
    }
}
