package com.braintreepayments.api.venmo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences;

import org.junit.Before;
import org.junit.Test;

public class VenmoSharedPrefsWriterUnitTest {

    private BraintreeSharedPreferences braintreeSharedPreferences;

    @Before
    public void beforeEach() {
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
    }

    @Test
    public void persistVenmoVaultOption_persistsVaultOption() {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.persistVenmoVaultOption(braintreeSharedPreferences, true);
        verify(braintreeSharedPreferences).putBoolean(
                "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY", true);
    }

    @Test
    public void getVenmoVaultOption_retrievesVaultOptionFromSharedPrefs() {
        VenmoSharedPrefsWriter sut = new VenmoSharedPrefsWriter();
        sut.getVenmoVaultOption(braintreeSharedPreferences);
        verify(braintreeSharedPreferences).getBoolean(
                "com.braintreepayments.api.Venmo.VAULT_VENMO_KEY");
    }
}