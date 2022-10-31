package com.braintreepayments.api;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

public class UUIDHelperUnitTest {

    private BraintreeSharedPreferences braintreeSharedPreferences;
    private Context context;

    @Before
    public void beforeEach() {
        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
        context = mock(Context.class);
    }

    @Test
    public void getInstallationGUID_returnsNewGUIDWhenOneDoesNotExistAndPersistsIt() throws BraintreeSharedPreferencesException {
        when(braintreeSharedPreferences.getString("InstallationGUID", null)).thenReturn(null);

        UUIDHelper sut = new UUIDHelper();

        String uuid = sut.getInstallationGUID(braintreeSharedPreferences);
        assertNotNull(uuid);
        verify(braintreeSharedPreferences).putString("InstallationGUID", uuid);
    }

    @Test
    public void getInstallationGUID_whenSharedPrefsFails_returnsNewGUID() throws BraintreeSharedPreferencesException {
        BraintreeSharedPreferencesException sharedPrefsException =
                new BraintreeSharedPreferencesException("unexpected exception");

        when(
                braintreeSharedPreferences.getString(anyString(), anyString())
        ).thenThrow(sharedPrefsException);

        doThrow(sharedPrefsException)
                .when(braintreeSharedPreferences)
                .putString(anyString(), anyString());

        UUIDHelper sut = new UUIDHelper();

        String uuid = sut.getInstallationGUID(braintreeSharedPreferences);
        assertNotNull(uuid);
    }

    @Test
    public void getInstallationGUID_returnsExistingGUIDWhenOneExist() throws BraintreeSharedPreferencesException {
        String uuid = UUID.randomUUID().toString();
        when(braintreeSharedPreferences.getString("InstallationGUID", null)).thenReturn(uuid);

        UUIDHelper sut = new UUIDHelper();

        assertEquals(uuid, sut.getInstallationGUID(braintreeSharedPreferences));
    }
}
