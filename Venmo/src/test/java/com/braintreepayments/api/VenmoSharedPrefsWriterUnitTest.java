package com.braintreepayments.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RunWith(RobolectricTestRunner.class)
public class VenmoSharedPrefsWriterUnitTest {

    private Context context;
    private BraintreeSharedPreferences braintreeSharedPreferences;

    @Before
    public void beforeEach() throws GeneralSecurityException, IOException {
        context = ApplicationProvider.getApplicationContext();
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
    }

    @Test
    public void persistVenmoVaultOption_persistsVaultOption() throws GeneralSecurityException, IOException {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.persistVenmoVaultOption(context, braintreeSharedPreferences, true);
        verify(braintreeSharedPreferences).putBoolean(context, "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", true);
    }

    @Test
    public void getVenmoVaultOption_retrievesVaultOptionFromSharedPrefs() throws GeneralSecurityException, IOException {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.getVenmoVaultOption(context, braintreeSharedPreferences);
        verify(braintreeSharedPreferences).getBoolean(context, "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY");
    }
}