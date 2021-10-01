package com.braintreepayments.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class VenmoSharedPrefsWriterUnitTest {

    private Context context;
    private BraintreeSharedPreferences braintreeSharedPreferences;

    @Before
    public void beforeEach() throws GeneralSecurityException, IOException {
        context = mock(Context.class);
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
    }

    @Test
    public void persistVenmoVaultOption_persistsVaultOption() throws GeneralSecurityException, IOException {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter(braintreeSharedPreferences);
        sut.persistVenmoVaultOption(context, true);
        verify(braintreeSharedPreferences).putBoolean(context, "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", true);
    }

    @Test
    public void getVenmoVaultOption_retrievesVaultOptionFromSharedPrefs() throws GeneralSecurityException, IOException {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter(braintreeSharedPreferences);
        sut.getVenmoVaultOption(context);
        verify(braintreeSharedPreferences).getBoolean(context, "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY");
    }
}