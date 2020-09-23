package com.paypal.android.sdk.data.collector;

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
public class InstallationIdentifierTest {

    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        mPrefs = ApplicationProvider.getApplicationContext().getSharedPreferences("PayPalOTC", Context.MODE_PRIVATE);
        mPrefs.edit().clear().apply();
    }

    @Test
    public void getInstallationGUID_returnsNewGUIDWhenOneDoesNotExistAndPersistsIt() {
        assertNull(mPrefs.getString("InstallationGUID", null));

        assertNotNull(InstallationIdentifier.getInstallationGUID(ApplicationProvider.getApplicationContext()));

        assertNotNull(mPrefs.getString("InstallationGUID", null));
    }

    @Test
    public void getInstallationGUID_returnsExistingGUIDWhenOneExist() {
        mPrefs.edit()
                .putString("InstallationGUID", UUID.randomUUID().toString())
                .apply();
        String existingGUID = mPrefs.getString("InstallationGUID", null);
        assertNotNull(existingGUID);

        assertEquals(existingGUID, InstallationIdentifier.getInstallationGUID(ApplicationProvider.getApplicationContext()));

        assertEquals(existingGUID, mPrefs.getString("InstallationGUID", null));
    }
}
