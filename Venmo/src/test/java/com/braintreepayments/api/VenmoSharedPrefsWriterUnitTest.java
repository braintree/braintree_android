package com.braintreepayments.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;

public class VenmoSharedPrefsWriterUnitTest {

    private Context context;
    private BraintreeSharedPreferences braintreeSharedPreferences;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
    }

    @Test
    public void persistVenmoVaultOption_persistsVaultOption() throws UnexpectedException {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter(braintreeSharedPreferences);
        sut.persistVenmoVaultOption(context, true);
        verify(braintreeSharedPreferences).putBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", true);
    }

    @Test
    public void getVenmoVaultOption_retrievesVaultOptionFromSharedPrefs() throws UnexpectedException {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter(braintreeSharedPreferences);
        sut.getVenmoVaultOption(context);
        verify(braintreeSharedPreferences).getBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY");
    }
}