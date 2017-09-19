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

import static com.braintreepayments.api.models.BinData.NO;
import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.api.models.BinData.YES;
import static com.braintreepayments.testutils.Assertions.assertBinDataEqual;
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
        UserAddress billingAddress = mock(UserAddress.class);
        UserAddress shippingAddress = mock(UserAddress.class);
        FullWallet wallet = getFullWallet(stringFromFixture("payment_methods/android_pay_card_response.json"),
                billingAddress, shippingAddress);

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
        UserAddress billingAddress = mock(UserAddress.class);
        UserAddress shippingAddress = mock(UserAddress.class);
        Cart cart = Cart.newBuilder().build();
        FullWallet wallet = getFullWallet(stringFromFixture("payment_methods/android_pay_card_response.json"),
                billingAddress, shippingAddress);

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
    public void parcelsCorrectly() throws Exception {
        UserAddress billingAddress = getAddressObject();
        UserAddress shippingAddress = getAddressObject();
        Cart cart = Cart.newBuilder().build();
        FullWallet wallet = getFullWallet(stringFromFixture("payment_methods/android_pay_card_response.json"),
                billingAddress, shippingAddress);
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

        assertBinDataEqual(androidPayCardNonce.getBinData(), parceled.getBinData());
    }

    private FullWallet getFullWallet(String response, UserAddress billingAddress, UserAddress shippingAddress) {
        PaymentMethodToken paymentMethodToken = mock(PaymentMethodToken.class);
        when(paymentMethodToken.getToken()).thenReturn(response);

        FullWallet wallet = mock(FullWallet.class);
        when(wallet.getPaymentMethodToken()).thenReturn(paymentMethodToken);
        when(wallet.getPaymentDescriptions()).thenReturn(new String[] { "MasterCard 0276" });
        when(wallet.getEmail()).thenReturn("android-user@example.com");
        when(wallet.getBuyerBillingAddress()).thenReturn(billingAddress);
        when(wallet.getBuyerShippingAddress()).thenReturn(shippingAddress);
        when(wallet.getGoogleTransactionId()).thenReturn("google-transaction-id");

        return wallet;
    }

    private UserAddress getAddressObject() throws Exception {
        Constructor<UserAddress> constructor = UserAddress.class.getDeclaredConstructor(new Class[0]);
        constructor.setAccessible(true);
        return constructor.newInstance(new Object[0]);
    }
}
