package com.braintreepayments.api;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
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
    public void getInstallationGUID_returnsNewGUIDWhenOneDoesNotExistAndPersistsIt() throws UnexpectedException {
        when(braintreeSharedPreferences.getString(context, "InstallationGUID", null)).thenReturn(null);

        UUIDHelper sut = new UUIDHelper();

        String uuid = sut.getInstallationGUID(context, braintreeSharedPreferences);
        assertNotNull(uuid);
        verify(braintreeSharedPreferences).putString(context, "InstallationGUID", uuid);
    }

    @Test
    public void getInstallationGUID_returnsExistingGUIDWhenOneExist() throws UnexpectedException {
        String uuid = UUID.randomUUID().toString();
        when(braintreeSharedPreferences.getString(context, "InstallationGUID", null)).thenReturn(uuid);

        UUIDHelper sut = new UUIDHelper();

        assertEquals(uuid, sut.getInstallationGUID(context, braintreeSharedPreferences));
    }
}
