package com.braintreepayments.api.models;

import android.os.Parcel;

import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.PaymentMethodToken;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@PrepareForTest({ PaymentMethodToken.class, UserAddress.class, FullWallet.class })
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
public class AndroidPayCardNonceUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void fromFullWallet_createsAndroidPayCardNonce() throws JSONException {
        PaymentMethodToken paymentMethodToken = mock(PaymentMethodToken.class);
        when(paymentMethodToken.getToken())
                .thenReturn(stringFromFixture("payment_methods/android_pay_card_response.json"));
        UserAddress billingAddress = mock(UserAddress.class);
        UserAddress shippingAddress = mock(UserAddress.class);
        FullWallet wallet = mock(FullWallet.class);
        when(wallet.getPaymentMethodToken()).thenReturn(paymentMethodToken);
        when(wallet.getEmail()).thenReturn("android-user@example.com");
        when(wallet.getBuyerBillingAddress()).thenReturn(billingAddress);
        when(wallet.getBuyerShippingAddress()).thenReturn(shippingAddress);

        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromFullWallet(wallet);

        assertEquals("Android Pay", androidPayCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", androidPayCardNonce.getNonce());
        assertEquals("Android Pay", androidPayCardNonce.getDescription());
        assertEquals("Visa", androidPayCardNonce.getCardType());
        assertEquals("11", androidPayCardNonce.getLastTwo());
        assertEquals("android-user@example.com", androidPayCardNonce.getEmail());
        assertEquals(billingAddress, androidPayCardNonce.getBillingAddress());
        assertEquals(shippingAddress, androidPayCardNonce.getShippingAddress());
    }

    @Test
    public void fromJson_createsAndroidPayCardNonce() throws JSONException {
        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromJson(
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        assertEquals("Android Pay", androidPayCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", androidPayCardNonce.getNonce());
        assertEquals("Android Pay", androidPayCardNonce.getDescription());
        assertEquals("Visa", androidPayCardNonce.getCardType());
        assertEquals("11", androidPayCardNonce.getLastTwo());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        PaymentMethodToken paymentMethodToken = mock(PaymentMethodToken.class);
        when(paymentMethodToken.getToken())
                .thenReturn(stringFromFixture("payment_methods/android_pay_card_response.json"));
        UserAddress billingAddress = getAddressObject();
        UserAddress shippingAddress = getAddressObject();
        FullWallet wallet = mock(FullWallet.class);
        when(wallet.getPaymentMethodToken()).thenReturn(paymentMethodToken);
        when(wallet.getEmail()).thenReturn("android-user@example.com");
        when(wallet.getBuyerBillingAddress()).thenReturn(billingAddress);
        when(wallet.getBuyerShippingAddress()).thenReturn(shippingAddress);
        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromFullWallet(wallet);
        Parcel parcel = Parcel.obtain();
        androidPayCardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        AndroidPayCardNonce parceled = AndroidPayCardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Android Pay", parceled.getTypeLabel());
        assertEquals("fake-android-pay-nonce", parceled.getNonce());
        assertEquals("Android Pay", parceled.getDescription());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("android-user@example.com", parceled.getEmail());
        assertNotNull(parceled.getBillingAddress());
        assertNotNull(parceled.getShippingAddress());
    }

    /* helpers */
    private UserAddress getAddressObject() {
        try {
            Constructor<UserAddress> constructor = UserAddress.class.getDeclaredConstructor(new Class[0]);
            constructor.setAccessible(true);
            return constructor.newInstance(new Object[0]);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
