package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
public class PayPalInstallationIdentifierUnitTest {

    private Context context;
    private BraintreeSharedPreferences braintreeSharedPreferences;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
    }

    @Test
    public void getInstallationGUID_returnsNewGUIDWhenOneDoesNotExistAndPersistsIt() throws GeneralSecurityException, IOException {
        when(braintreeSharedPreferences.getString(context, "com.braintreepayments.api.paypal", "InstallationGUID")).thenReturn(null);

        PayPalInstallationIdentifier sut = new PayPalInstallationIdentifier();

        String uuid = sut.getInstallationGUID(context, braintreeSharedPreferences);
        assertNotNull(uuid);
        verify(braintreeSharedPreferences).putString(context, "com.braintreepayments.api.paypal", "InstallationGUID", uuid);
    }

    @Test
    public void getInstallationGUID_returnsExistingGUIDWhenOneExist() throws GeneralSecurityException, IOException {
        String uuid = UUID.randomUUID().toString();
        when(braintreeSharedPreferences.getString(context, "com.braintreepayments.api.paypal", "InstallationGUID")).thenReturn(uuid);

        PayPalInstallationIdentifier sut = new PayPalInstallationIdentifier();

        assertEquals(uuid, sut.getInstallationGUID(context, braintreeSharedPreferences));
    }
}
