package com.braintreepayments.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class VenmoSharedPrefsWriterUnitTest {

    private BraintreeSharedPreferences braintreeSharedPreferences;

    @Before
    public void beforeEach() {
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
    }

    @Test
    public void persistVenmoVaultOption_persistsVaultOption() throws UnexpectedException {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.persistVenmoVaultOption(braintreeSharedPreferences, true);
        verify(braintreeSharedPreferences).putBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", true);
    }

    @Test
    public void getVenmoVaultOption_retrievesVaultOptionFromSharedPrefs() throws UnexpectedException {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.getVenmoVaultOption(braintreeSharedPreferences);
        verify(braintreeSharedPreferences).getBoolean("com.braintreepayments.api.Venmo.VAULT_VENMO_KEY");
    }
}