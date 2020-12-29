package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class VenmoSharedPrefsWriterUnitTest {

    private Context context;
    private SharedPreferences sharedPrefs;

    @Before
    public void beforeEach() {
        context = ApplicationProvider.getApplicationContext();
        sharedPrefs = context.getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);
        sharedPrefs.edit()
            .putBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", false)
            .apply();
    }

    @Test
    public void persistVenmoVaultOption_persistsVaultOption() {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.persistVenmoVaultOption(context, true);
        assertTrue(
            sharedPrefs.getBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", false)
        );
    }

    @Test
    public void getVenmoVaultOption_retrievesVaultOptionFromSharedPrefs() {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.persistVenmoVaultOption(context, true);
        assertTrue(sut.getVenmoVaultOption(context));
    }
}