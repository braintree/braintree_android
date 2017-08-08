package com.braintreepayments.api.models;

import android.os.Parcel;

import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.PaymentMethodToken;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.braintreepayments.api.models.BinData.NO;
import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.api.models.BinData.YES;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
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
        when(wallet.getPaymentDescriptions()).thenReturn(new String[] { "MasterCard 0276" });
        when(wallet.getEmail()).thenReturn("android-user@example.com");
        when(wallet.getBuyerBillingAddress()).thenReturn(billingAddress);
        when(wallet.getBuyerShippingAddress()).thenReturn(shippingAddress);
        when(wallet.getGoogleTransactionId()).thenReturn("google-transaction-id");

        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromFullWallet(wallet);

        assertEquals("Android Pay", androidPayCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", androidPayCardNonce.getNonce());
        assertEquals("MasterCard 0276", androidPayCardNonce.getDescription());
        assertEquals("Visa", androidPayCardNonce.getCardType());
        assertEquals("11", androidPayCardNonce.getLastTwo());
        assertEquals("android-user@example.com", androidPayCardNonce.getEmail());
        assertEquals(billingAddress, androidPayCardNonce.getBillingAddress());
        assertEquals(shippingAddress, androidPayCardNonce.getShippingAddress());
        assertEquals("google-transaction-id", androidPayCardNonce.getGoogleTransactionId());
    }

    @Test
    public void fromFullWallet_withCart_createsAndroidPayCardNonce() throws JSONException {
        PaymentMethodToken paymentMethodToken = mock(PaymentMethodToken.class);
        when(paymentMethodToken.getToken())
                .thenReturn(stringFromFixture("payment_methods/android_pay_card_response.json"));
        UserAddress billingAddress = mock(UserAddress.class);
        UserAddress shippingAddress = mock(UserAddress.class);
        FullWallet wallet = mock(FullWallet.class);
        Cart cart = Cart.newBuilder().build();
        when(wallet.getPaymentMethodToken()).thenReturn(paymentMethodToken);
        when(wallet.getPaymentDescriptions()).thenReturn(new String[] { "MasterCard 0276" });
        when(wallet.getEmail()).thenReturn("android-user@example.com");
        when(wallet.getBuyerBillingAddress()).thenReturn(billingAddress);
        when(wallet.getBuyerShippingAddress()).thenReturn(shippingAddress);
        when(wallet.getGoogleTransactionId()).thenReturn("google-transaction-id");

        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromFullWallet(wallet, cart);

        assertEquals("Android Pay", androidPayCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", androidPayCardNonce.getNonce());
        assertEquals("MasterCard 0276", androidPayCardNonce.getDescription());
        assertEquals("Visa", androidPayCardNonce.getCardType());
        assertEquals("11", androidPayCardNonce.getLastTwo());
        assertEquals("android-user@example.com", androidPayCardNonce.getEmail());
        assertEquals(billingAddress, androidPayCardNonce.getBillingAddress());
        assertEquals(shippingAddress, androidPayCardNonce.getShippingAddress());
        assertEquals("google-transaction-id", androidPayCardNonce.getGoogleTransactionId());
        assertEquals(cart, androidPayCardNonce.getCart());
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
        assertNotNull(androidPayCardNonce.getBinData());
        assertEquals(UNKNOWN, androidPayCardNonce.getBinData().getPrepaid());
        assertEquals(YES, androidPayCardNonce.getBinData().getHealthcare());
        assertEquals(NO, androidPayCardNonce.getBinData().getDebit());
        assertEquals(UNKNOWN, androidPayCardNonce.getBinData().getDurbinRegulated());
        assertEquals(UNKNOWN, androidPayCardNonce.getBinData().getCommercial());
        assertEquals(UNKNOWN, androidPayCardNonce.getBinData().getPayroll());
        assertEquals(UNKNOWN, androidPayCardNonce.getBinData().getIssuingBank());
        assertEquals("Something", androidPayCardNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", androidPayCardNonce.getBinData().getProductId());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        PaymentMethodToken paymentMethodToken = mock(PaymentMethodToken.class);
        when(paymentMethodToken.getToken())
                .thenReturn(stringFromFixture("payment_methods/android_pay_card_response.json"));
        UserAddress billingAddress = getAddressObject();
        UserAddress shippingAddress = getAddressObject();
        FullWallet wallet = mock(FullWallet.class);
        Cart cart = Cart.newBuilder().build();
        when(wallet.getPaymentMethodToken()).thenReturn(paymentMethodToken);
        when(wallet.getPaymentDescriptions()).thenReturn(new String[] { "MasterCard 0276" });
        when(wallet.getEmail()).thenReturn("android-user@example.com");
        when(wallet.getBuyerBillingAddress()).thenReturn(billingAddress);
        when(wallet.getBuyerShippingAddress()).thenReturn(shippingAddress);
        when(wallet.getGoogleTransactionId()).thenReturn("google-transaction-id");
        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromFullWallet(wallet, cart);
        Parcel parcel = Parcel.obtain();
        androidPayCardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        AndroidPayCardNonce parceled = AndroidPayCardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Android Pay", parceled.getTypeLabel());
        assertEquals("fake-android-pay-nonce", parceled.getNonce());
        assertEquals("MasterCard 0276", parceled.getDescription());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("android-user@example.com", parceled.getEmail());
        assertNotNull(parceled.getBillingAddress());
        assertNotNull(parceled.getShippingAddress());
        assertEquals("google-transaction-id", parceled.getGoogleTransactionId());
        assertNotNull(parceled.getCart());

        assertNotNull(parceled.getBinData());
        assertEquals(UNKNOWN, parceled.getBinData().getPrepaid());
        assertEquals(YES, parceled.getBinData().getHealthcare());
        assertEquals(NO, parceled.getBinData().getDebit());
        assertEquals(UNKNOWN, parceled.getBinData().getDurbinRegulated());
        assertEquals(UNKNOWN, parceled.getBinData().getCommercial());
        assertEquals(UNKNOWN, parceled.getBinData().getPayroll());
        assertEquals(UNKNOWN, parceled.getBinData().getIssuingBank());
        assertEquals("Something", parceled.getBinData().getCountryOfIssuance());
        assertEquals("123", parceled.getBinData().getProductId());
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
