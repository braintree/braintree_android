package com.braintreepayments.api.utils;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.dropin.utils.PaymentMethodType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class PaymentMethodTypeTest {

    @Test
    public void forType_returnsCorrectPaymentMethodType() {
        assertEquals(PaymentMethodType.VISA, PaymentMethodType.forType("Visa"));
        assertEquals(PaymentMethodType.MASTERCARD, PaymentMethodType.forType("MasterCard"));
        assertEquals(PaymentMethodType.DISCOVER, PaymentMethodType.forType("Discover"));
        assertEquals(PaymentMethodType.AMEX, PaymentMethodType.forType("American Express"));
        assertEquals(PaymentMethodType.JCB, PaymentMethodType.forType("JCB"));
        assertEquals(PaymentMethodType.DINERS, PaymentMethodType.forType("Diners"));
        assertEquals(PaymentMethodType.MAESTRO, PaymentMethodType.forType("Maestro"));
        assertEquals(PaymentMethodType.PAYPAL, PaymentMethodType.forType("PayPal"));
        assertEquals(PaymentMethodType.ANDROID_PAY, PaymentMethodType.forType("Android Pay"));
        assertEquals(PaymentMethodType.UNKNOWN, PaymentMethodType.forType("unknown"));
        assertEquals(PaymentMethodType.PAY_WITH_VENMO, PaymentMethodType.forType("Venmo"));
    }

    @Test
    public void forType_returnsUnknownForRandomString() {
        assertEquals(PaymentMethodType.UNKNOWN, PaymentMethodType.forType("payment method"));
    }

    @Test
    public void getDrawable_returnsCorrectDrawables() {
        assertEquals(R.drawable.bt_visa, PaymentMethodType.VISA.getDrawable());
        assertEquals(R.drawable.bt_mastercard, PaymentMethodType.MASTERCARD.getDrawable());
        assertEquals(R.drawable.bt_discover, PaymentMethodType.DISCOVER.getDrawable());
        assertEquals(R.drawable.bt_amex, PaymentMethodType.AMEX.getDrawable());
        assertEquals(R.drawable.bt_jcb, PaymentMethodType.JCB.getDrawable());
        assertEquals(R.drawable.bt_diners, PaymentMethodType.DINERS.getDrawable());
        assertEquals(R.drawable.bt_maestro, PaymentMethodType.MAESTRO.getDrawable());
        assertEquals(R.drawable.bt_paypal, PaymentMethodType.PAYPAL.getDrawable());
        assertEquals(R.drawable.bt_android_pay, PaymentMethodType.ANDROID_PAY.getDrawable());
        assertEquals(0, PaymentMethodType.UNKNOWN.getDrawable());
        assertEquals(R.drawable.bt_venmo, PaymentMethodType.PAY_WITH_VENMO.getDrawable());
    }

    @Test
    public void getLocalizedName_returnsCorrectString() {
        assertEquals(R.string.bt_descriptor_visa, PaymentMethodType.VISA.getLocalizedName());
        assertEquals(R.string.bt_descriptor_mastercard, PaymentMethodType.MASTERCARD.getLocalizedName());
        assertEquals(R.string.bt_descriptor_discover, PaymentMethodType.DISCOVER.getLocalizedName());
        assertEquals(R.string.bt_descriptor_amex, PaymentMethodType.AMEX.getLocalizedName());
        assertEquals(R.string.bt_descriptor_jcb, PaymentMethodType.JCB.getLocalizedName());
        assertEquals(R.string.bt_descriptor_diners, PaymentMethodType.DINERS.getLocalizedName());
        assertEquals(R.string.bt_descriptor_maestro, PaymentMethodType.MAESTRO.getLocalizedName());
        assertEquals(R.string.bt_descriptor_paypal, PaymentMethodType.PAYPAL.getLocalizedName());
        assertEquals(R.string.bt_descriptor_android_pay, PaymentMethodType.ANDROID_PAY.getLocalizedName());
        assertEquals(R.string.bt_descriptor_unknown, PaymentMethodType.UNKNOWN.getLocalizedName());
        assertEquals(R.string.bt_descriptor_pay_with_venmo, PaymentMethodType.PAY_WITH_VENMO.getLocalizedName());
    }

    @Test
    public void getCanonicalName_returnsCorrectString() {
        assertEquals("Visa", PaymentMethodType.VISA.getCanonicalName());
        assertEquals("MasterCard", PaymentMethodType.MASTERCARD.getCanonicalName());
        assertEquals("Discover", PaymentMethodType.DISCOVER.getCanonicalName());
        assertEquals("American Express", PaymentMethodType.AMEX.getCanonicalName());
        assertEquals("JCB", PaymentMethodType.JCB.getCanonicalName());
        assertEquals("Diners", PaymentMethodType.DINERS.getCanonicalName());
        assertEquals("Maestro", PaymentMethodType.MAESTRO.getCanonicalName());
        assertEquals("PayPal", PaymentMethodType.PAYPAL.getCanonicalName());
        assertEquals("Android Pay", PaymentMethodType.ANDROID_PAY.getCanonicalName());
        assertEquals("unknown", PaymentMethodType.UNKNOWN.getCanonicalName());
        assertEquals("Venmo", PaymentMethodType.PAY_WITH_VENMO.getCanonicalName());
    }

    @Test
    public void containsOnlyKnownPaymentMethodTypes() {
        assertEquals(11, PaymentMethodType.values().length);
    }
}
