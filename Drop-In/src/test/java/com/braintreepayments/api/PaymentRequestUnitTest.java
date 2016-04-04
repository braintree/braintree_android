package com.braintreepayments.api;

import android.content.Intent;
import android.os.Parcel;

import com.braintreepayments.api.dropin.R;
import com.google.android.gms.wallet.Cart;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;

@RunWith(RobolectricGradleTestRunner.class)
public class PaymentRequestUnitTest {

    @Test
    public void includesAllOptions() {
        Cart cart = Cart.newBuilder()
                .setTotalPrice("5.00")
                .build();
        Intent intent = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .amount("1.00")
                .collectDeviceData(true)
                .androidPayCart(cart)
                .androidPayShippingAddressRequired(true)
                .androidPayPhoneNumberRequired(true)
                .androidPayRequestCode(1)
                .androidPayAllowedCountriesForShipping("GB")
                .disableAndroidPay()
                .paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS))
                .disablePayPal()
                .disableVenmo()
                .actionBarTitle("title")
                .actionBarLogo(R.drawable.bt_amex)
                .primaryDescription("primary description")
                .secondaryDescription("secondary description")
                .submitButtonText("submit")
                .defaultFirst(true)
                .getIntent(RuntimeEnvironment.application);

        PaymentRequest paymentRequest = intent.getParcelableExtra(BraintreePaymentActivity.EXTRA_CHECKOUT_REQUEST);

        assertEquals(BraintreePaymentActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(TOKENIZATION_KEY, paymentRequest.getAuthorization());
        assertEquals("1.00", paymentRequest.getAmount());
        assertTrue(paymentRequest.shouldCollectDeviceData());
        assertEquals("5.00", paymentRequest.getAndroidPayCart().getTotalPrice());
        assertTrue(paymentRequest.isAndroidPayShippingAddressRequired());
        assertTrue(paymentRequest.isAndroidPayPhoneNumberRequired());
        assertEquals(1, paymentRequest.getAndroidPayRequestCode());
        assertFalse(paymentRequest.isAndroidPayEnabled());
        assertEquals(1, paymentRequest.getAndroidPayAllowedCountriesForShipping().size());
        assertEquals("GB", paymentRequest.getAndroidPayAllowedCountriesForShipping().get(0).getCountryCode());
        assertEquals(Collections.singletonList(PayPal.SCOPE_ADDRESS), paymentRequest.getPayPalAdditionalScopes());
        assertFalse(paymentRequest.isPayPalEnabled());
        assertFalse(paymentRequest.isVenmoEnabled());
        assertEquals("title", paymentRequest.getActionBarTitle());
        assertEquals(R.drawable.bt_amex, paymentRequest.getActionBarLogo());
        assertEquals("primary description", paymentRequest.getPrimaryDescription());
        assertEquals("secondary description", paymentRequest.getSecondaryDescription());
        assertEquals("submit", paymentRequest.getSubmitButtonText());
        assertTrue(paymentRequest.isDefaultFirst());
    }

    @Test
    public void hasCorrectDefaults() {
        Intent intent = new PaymentRequest()
                .getIntent(RuntimeEnvironment.application);

        PaymentRequest paymentRequest = intent.getParcelableExtra(BraintreePaymentActivity.EXTRA_CHECKOUT_REQUEST);

        assertEquals(BraintreePaymentActivity.class.getName(), intent.getComponent().getClassName());
        assertNull(paymentRequest.getAuthorization());
        assertNull(paymentRequest.getAmount());
        assertFalse(paymentRequest.shouldCollectDeviceData());
        assertNull(paymentRequest.getAndroidPayCart());
        assertFalse(paymentRequest.isAndroidPayShippingAddressRequired());
        assertFalse(paymentRequest.isAndroidPayPhoneNumberRequired());
        assertEquals(0, paymentRequest.getAndroidPayRequestCode());
        assertTrue(paymentRequest.isAndroidPayEnabled());
        assertTrue(paymentRequest.getAndroidPayAllowedCountriesForShipping().isEmpty());
        assertNull(paymentRequest.getPayPalAdditionalScopes());
        assertTrue(paymentRequest.isPayPalEnabled());
        assertTrue(paymentRequest.isVenmoEnabled());
        assertNull(paymentRequest.getActionBarTitle());
        assertEquals(0, paymentRequest.getActionBarLogo());
        assertNull(paymentRequest.getPrimaryDescription());
        assertNull(paymentRequest.getSecondaryDescription());
        assertNull(paymentRequest.getSubmitButtonText());
        assertFalse(paymentRequest.isDefaultFirst());
    }

    @Test
    public void isParcelable() {
        Cart cart = Cart.newBuilder()
                .setTotalPrice("5.00")
                .build();
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .amount("1.00")
                .collectDeviceData(true)
                .androidPayCart(cart)
                .androidPayShippingAddressRequired(true)
                .androidPayPhoneNumberRequired(true)
                .androidPayRequestCode(1)
                .androidPayAllowedCountriesForShipping("GB")
                .disableAndroidPay()
                .paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS))
                .disablePayPal()
                .disableVenmo()
                .actionBarTitle("title")
                .actionBarLogo(R.drawable.bt_amex)
                .primaryDescription("primary description")
                .secondaryDescription("secondary description")
                .defaultFirst(true)
                .submitButtonText("submit");

        Parcel parcel = Parcel.obtain();
        paymentRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PaymentRequest parceledPaymentRequest = PaymentRequest.CREATOR.createFromParcel(parcel);

        assertEquals(TOKENIZATION_KEY, parceledPaymentRequest.getAuthorization());
        assertEquals("1.00", parceledPaymentRequest.getAmount());
        assertTrue(parceledPaymentRequest.shouldCollectDeviceData());
        assertEquals("5.00", parceledPaymentRequest.getAndroidPayCart().getTotalPrice());
        assertTrue(parceledPaymentRequest.isAndroidPayShippingAddressRequired());
        assertTrue(parceledPaymentRequest.isAndroidPayPhoneNumberRequired());
        assertEquals(1, parceledPaymentRequest.getAndroidPayRequestCode());
        assertFalse(parceledPaymentRequest.isAndroidPayEnabled());
        assertEquals(1, parceledPaymentRequest.getAndroidPayAllowedCountriesForShipping().size());
        assertEquals("GB", parceledPaymentRequest.getAndroidPayAllowedCountriesForShipping().get(0).getCountryCode());
        assertEquals(Collections.singletonList(PayPal.SCOPE_ADDRESS), parceledPaymentRequest.getPayPalAdditionalScopes());
        assertFalse(parceledPaymentRequest.isPayPalEnabled());
        assertFalse(parceledPaymentRequest.isVenmoEnabled());
        assertEquals("title", parceledPaymentRequest.getActionBarTitle());
        assertEquals(R.drawable.bt_amex, parceledPaymentRequest.getActionBarLogo());
        assertEquals("primary description", parceledPaymentRequest.getPrimaryDescription());
        assertEquals("secondary description", parceledPaymentRequest.getSecondaryDescription());
        assertTrue(parceledPaymentRequest.isDefaultFirst());
        assertEquals("submit", parceledPaymentRequest.getSubmitButtonText());
    }

    @Test
    public void androidPayAllowedCountriesForShipping_defaultsToEmpty() {
        PaymentRequest paymentRequest = new PaymentRequest();

        assertTrue(paymentRequest.getAndroidPayAllowedCountriesForShipping().isEmpty());
    }
}
