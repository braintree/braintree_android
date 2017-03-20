package com.paypal.android.sdk.data.collector;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class InstallationIdentifierTest {

    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        mPrefs = getTargetContext().getSharedPreferences("PayPalOTC", Context.MODE_PRIVATE);
        mPrefs.edit().clear().apply();
    }

    @Test
    public void getInstallationGUID_returnsNewGUIDWhenOneDoesNotExistAndPersistsIt() {
        assertNull(mPrefs.getString("InstallationGUID", null));

        assertNotNull(InstallationIdentifier.getInstallationGUID(getTargetContext()));

        assertNotNull(mPrefs.getString("InstallationGUID", null));
    }

    @Test
    public void getInstallationGUID_returnsExistingGUIDWhenOneExist() {
        mPrefs.edit()
                .putString("InstallationGUID", UUID.randomUUID().toString())
                .apply();
        String existingGUID = mPrefs.getString("InstallationGUID", null);
        assertNotNull(existingGUID);

        assertEquals(existingGUID, InstallationIdentifier.getInstallationGUID(getTargetContext()));

        assertEquals(existingGUID, mPrefs.getString("InstallationGUID", null));
    }
}
