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
public class GooglePaymentsCardNonceUnitTest {

    @Test
    public void fromPaymentData_createsGooglePaymentsCardNonce() throws Exception {
        UserAddress billingAddress = getAddressObject();
        UserAddress shippingAddress = getAddressObject();
        PaymentData paymentData = getPaymentData("android-user@example.com", billingAddress, shippingAddress,
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        GooglePaymentsCardNonce googlePaymentsCardNonce = GooglePaymentsCardNonce.fromPaymentData(paymentData);

        assertEquals("Google Payments", googlePaymentsCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", googlePaymentsCardNonce.getNonce());
        assertEquals("MasterCard 0276", googlePaymentsCardNonce.getDescription());
        assertEquals("Visa", googlePaymentsCardNonce.getCardType());
        assertEquals("11", googlePaymentsCardNonce.getLastTwo());
        assertEquals("android-user@example.com", googlePaymentsCardNonce.getEmail());
        assertEquals(billingAddress, googlePaymentsCardNonce.getBillingAddress());
        assertEquals(shippingAddress, googlePaymentsCardNonce.getShippingAddress());
    }

    @Test
    public void fromJson_createsAndroidPayCardNonce() throws JSONException {
        GooglePaymentsCardNonce googlePaymentsCardNonce = GooglePaymentsCardNonce.fromJson(
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        assertEquals("Google Payments", googlePaymentsCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", googlePaymentsCardNonce.getNonce());
        assertEquals("Google Payments", googlePaymentsCardNonce.getDescription());
        assertEquals("Visa", googlePaymentsCardNonce.getCardType());
        assertEquals("11", googlePaymentsCardNonce.getLastTwo());
        assertNotNull(googlePaymentsCardNonce.getBinData());
        assertEquals(UNKNOWN, googlePaymentsCardNonce.getBinData().getPrepaid());
        assertEquals(YES, googlePaymentsCardNonce.getBinData().getHealthcare());
        assertEquals(NO, googlePaymentsCardNonce.getBinData().getDebit());
        assertEquals(UNKNOWN, googlePaymentsCardNonce.getBinData().getDurbinRegulated());
        assertEquals(UNKNOWN, googlePaymentsCardNonce.getBinData().getCommercial());
        assertEquals(UNKNOWN, googlePaymentsCardNonce.getBinData().getPayroll());
        assertEquals(UNKNOWN, googlePaymentsCardNonce.getBinData().getIssuingBank());
        assertEquals("Something", googlePaymentsCardNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", googlePaymentsCardNonce.getBinData().getProductId());
    }

    @Test
    public void parcelsCorrectly() throws Exception {
        UserAddress billingAddress = getAddressObject();
        UserAddress shippingAddress = getAddressObject();
        PaymentData paymentData = getPaymentData("android-user@example.com", billingAddress, shippingAddress,
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        GooglePaymentsCardNonce googlePaymentsCardNonce = GooglePaymentsCardNonce.fromPaymentData(paymentData);

        Parcel parcel = Parcel.obtain();
        googlePaymentsCardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePaymentsCardNonce parceled = GooglePaymentsCardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Google Payments", parceled.getTypeLabel());
        assertEquals("fake-android-pay-nonce", parceled.getNonce());
        assertEquals("MasterCard 0276", parceled.getDescription());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("android-user@example.com", parceled.getEmail());
        assertNotNull(parceled.getBillingAddress());
        assertNotNull(parceled.getShippingAddress());
        assertBinDataEqual(googlePaymentsCardNonce.getBinData(), parceled.getBinData());
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
