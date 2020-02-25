package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.PayPalAccountNonce;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Parcel.class, BraintreeSharedPreferences.class, Base64.class, PayPalAccountNonce.class })
public class PayPalTwoFactorAuthSharedPreferencesUnitTest {

    @Mock Parcel parcel;
    @Mock BraintreeFragment fragment;

    @Mock Context context;
    @Mock Parcelable.Creator<PayPalAccountNonce> payPalAccountNonceCreator;

    @Mock SharedPreferences sharedPreferences;
    @Mock SharedPreferences.Editor sharedPreferencesEditor;

    @Before
    public void setUp() {
        mockStatic(Base64.class);
        mockStatic(BraintreeSharedPreferences.class);
        mockStatic(Parcel.class);
        mockStatic(PayPalAccountNonce.class);

        // mock braintree fragment
        when(fragment.getApplicationContext()).thenReturn(context);

        // mock static parcel accessor
        when(Parcel.obtain()).thenReturn(parcel);

        // mock shared prefs
        when(BraintreeSharedPreferences.getSharedPreferences(context)).thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
    }

    @Test
    public void persistPayPalAccountNonce_persistsPayPalAccountNonceIntoSharedPrefs() {

        byte[] parcelBytes = new byte[0];
        when(parcel.marshall()).thenReturn(parcelBytes);

        when(Base64.encodeToString(parcelBytes, 0)).thenReturn("base64EncodedParcelString");
        when(sharedPreferencesEditor.putString(
                "com.braintreepayments.api.PayPalTwoFactorAuth.PAYPAL_TWO_FACTOR_AUTH_REQUEST_KEY",
                "base64EncodedParcelString"
        )).thenReturn(sharedPreferencesEditor);

        PayPalAccountNonce nonce = mock(PayPalAccountNonce.class);
        PayPalTwoFactorAuthSharedPreferences.persistPayPalAccountNonce(fragment, nonce);

        verify(nonce).writeToParcel(parcel, 0);
        verify(sharedPreferencesEditor).apply();
    }

    @Test
    public void getPersistedPayPalAccountNonce_restoresPayPalAccountNonceFromSharedPrefs() {

        when(sharedPreferences.getString(
                "com.braintreepayments.api.PayPalTwoFactorAuth.PAYPAL_TWO_FACTOR_AUTH_REQUEST_KEY",
                ""
        )).thenReturn("base64EncodedParcelString");

        byte[] decodedBytes = new byte[0];
        when(Base64.decode("base64EncodedParcelString", 0)).thenReturn(decodedBytes);

        // Ref: https://stackoverflow.com/a/8911517
        Whitebox.setInternalState(PayPalAccountNonce.class, "CREATOR", payPalAccountNonceCreator);

        PayPalAccountNonce expectedNonce = mock(PayPalAccountNonce.class);
        when(payPalAccountNonceCreator.createFromParcel(parcel)).thenReturn(expectedNonce);

        when(sharedPreferencesEditor.remove("com.braintreepayments.api.PayPalTwoFactorAuth.PAYPAL_TWO_FACTOR_AUTH_REQUEST_KEY")).thenReturn(sharedPreferencesEditor);

        // test
        PayPalAccountNonce actualNonce = PayPalTwoFactorAuthSharedPreferences.getPersistedPayPalAccountNonce(fragment);
        assertSame(expectedNonce, actualNonce);

        verify(parcel).unmarshall(decodedBytes, 0, decodedBytes.length);
        verify(parcel).setDataPosition(0);

        verify(sharedPreferencesEditor).apply();
    }
}
