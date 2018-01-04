package com.braintreepayments.api.models;

import android.os.Parcel;

import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentMethodToken;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Constructor;

import static com.braintreepayments.api.models.BinData.NO;
import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.api.models.BinData.YES;
import static com.braintreepayments.testutils.Assertions.assertBinDataEqual;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class GooglePaymentCardNonceUnitTest {

    @Test
    public void fromPaymentData_createsGooglePaymentCardNonce() throws Exception {
        UserAddress billingAddress = getAddressObject();
        UserAddress shippingAddress = getAddressObject();
        PaymentData paymentData = getPaymentData("android-user@example.com", billingAddress, shippingAddress,
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromPaymentData(paymentData);

        assertEquals("Google Pay", googlePaymentCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", googlePaymentCardNonce.getNonce());
        assertEquals("MasterCard 0276", googlePaymentCardNonce.getDescription());
        assertEquals("Visa", googlePaymentCardNonce.getCardType());
        assertEquals("11", googlePaymentCardNonce.getLastTwo());
        assertEquals("1234", googlePaymentCardNonce.getLastFour());
        assertEquals("android-user@example.com", googlePaymentCardNonce.getEmail());
        assertEquals(billingAddress, googlePaymentCardNonce.getBillingAddress());
        assertEquals(shippingAddress, googlePaymentCardNonce.getShippingAddress());
    }

    @Test
    public void fromJson_createsAndroidPayCardNonce() throws JSONException {
        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromJson(
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        assertEquals("Google Pay", googlePaymentCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", googlePaymentCardNonce.getNonce());
        assertEquals("Google Pay", googlePaymentCardNonce.getDescription());
        assertEquals("Visa", googlePaymentCardNonce.getCardType());
        assertEquals("11", googlePaymentCardNonce.getLastTwo());
        assertEquals("1234", googlePaymentCardNonce.getLastFour());
        assertNotNull(googlePaymentCardNonce.getBinData());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getPrepaid());
        assertEquals(YES, googlePaymentCardNonce.getBinData().getHealthcare());
        assertEquals(NO, googlePaymentCardNonce.getBinData().getDebit());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getDurbinRegulated());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getCommercial());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getPayroll());
        assertEquals(UNKNOWN, googlePaymentCardNonce.getBinData().getIssuingBank());
        assertEquals("Something", googlePaymentCardNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", googlePaymentCardNonce.getBinData().getProductId());
    }

    @Test
    public void parcelsCorrectly() throws Exception {
        UserAddress billingAddress = getAddressObject();
        UserAddress shippingAddress = getAddressObject();
        PaymentData paymentData = getPaymentData("android-user@example.com", billingAddress, shippingAddress,
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        GooglePaymentCardNonce googlePaymentCardNonce = GooglePaymentCardNonce.fromPaymentData(paymentData);

        Parcel parcel = Parcel.obtain();
        googlePaymentCardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePaymentCardNonce parceled = GooglePaymentCardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Google Pay", parceled.getTypeLabel());
        assertEquals("fake-android-pay-nonce", parceled.getNonce());
        assertEquals("MasterCard 0276", parceled.getDescription());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("1234", parceled.getLastFour());
        assertEquals("android-user@example.com", parceled.getEmail());
        assertNotNull(parceled.getBillingAddress());
        assertNotNull(parceled.getShippingAddress());
        assertBinDataEqual(googlePaymentCardNonce.getBinData(), parceled.getBinData());
    }

    private PaymentData getPaymentData(String email, UserAddress billingAddress, UserAddress shippingAddress,
            String response) throws Exception {
        Constructor<PaymentMethodToken> paymentMethodTokenConstructor = PaymentMethodToken.class
                .getDeclaredConstructor(int.class, String.class);
        paymentMethodTokenConstructor.setAccessible(true);
        PaymentMethodToken paymentMethodToken = paymentMethodTokenConstructor.newInstance(0, response);

        Constructor<CardInfo> cardInfoConstructor = CardInfo.class.getDeclaredConstructor(String.class, String.class,
                String.class, int.class, UserAddress.class);
        cardInfoConstructor.setAccessible(true);
        CardInfo cardInfo = cardInfoConstructor.newInstance("MasterCard 0276", null, null, 0, billingAddress);

        Constructor<PaymentData> paymentDataConstructor = PaymentData.class.getDeclaredConstructor(String.class,
                CardInfo.class, UserAddress.class, PaymentMethodToken.class);
        paymentDataConstructor.setAccessible(true);
        return paymentDataConstructor.newInstance(email, cardInfo, shippingAddress, paymentMethodToken);
    }

    private UserAddress getAddressObject() throws Exception {
        Constructor<UserAddress> constructor = UserAddress.class.getDeclaredConstructor(new Class[0]);
        constructor.setAccessible(true);
        return constructor.newInstance(new Object[0]);
    }
}
