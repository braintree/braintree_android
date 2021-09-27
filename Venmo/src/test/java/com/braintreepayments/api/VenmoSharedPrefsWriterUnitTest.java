package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RunWith(RobolectricTestRunner.class)
public class VenmoSharedPrefsWriterUnitTest {

    private Context context;
    private SharedPreferences sharedPrefs;
    private BraintreeSharedPreferences braintreeSharedPreferences;

    @Before
    public void beforeEach() throws GeneralSecurityException, IOException {
        context = ApplicationProvider.getApplicationContext();
        sharedPrefs = SharedPreferencesHelper.getSharedPreferences(context);
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
        when(braintreeSharedPreferences.getSharedPreferences(context)).thenReturn(sharedPrefs);
        sharedPrefs.edit()
            .putBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", false)
            .apply();
    }

    @Test
    public void persistVenmoVaultOption_persistsVaultOption() {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.persistVenmoVaultOption(context, braintreeSharedPreferences, true);
        assertTrue(
            sharedPrefs.getBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", false)
        );
    }

    @Test
    public void getVenmoVaultOption_retrievesVaultOptionFromSharedPrefs() {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.persistVenmoVaultOption(context, braintreeSharedPreferences, true);
        assertTrue(sut.getVenmoVaultOption(context, braintreeSharedPreferences));
    }
}