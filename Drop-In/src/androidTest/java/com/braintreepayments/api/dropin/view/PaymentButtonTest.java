package com.braintreepayments.api.dropin.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.AndroidTestCase;
import android.view.View;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.dropin.R;
import com.google.android.gms.wallet.WalletConstants;
import com.paypal.android.sdk.payments.PayPalTouchActivity;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PaymentButtonTest extends AndroidTestCase {

    private Braintree mBraintree;

    @Override
    public void setUp() {
        mBraintree = mock(Braintree.class);
    }

    public void testNotInflatedByDefault() {
        PaymentButton button = new PaymentButton(getContext());
        assertNull(button.findViewById(R.id.bt_paypal_button));
    }

    public void testNotVisibleWhenNoMethodsAreEnabled() {
        when(mBraintree.isPayPalEnabled()).thenReturn(false);
        when(mBraintree.isVenmoEnabled()).thenReturn(false);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(false);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        assertEquals(View.GONE, button.getVisibility());
    }

    public void testOnlyShowsPayPal() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(false);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    public void testOnlyShowsVenmo() {
        when(mBraintree.isPayPalEnabled()).thenReturn(false);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    public void testOnlyShowsAndroidPay() {
        when(mBraintree.isPayPalEnabled()).thenReturn(false);
        when(mBraintree.isVenmoEnabled()).thenReturn(false);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    public void testShowsAllMethodsAndDividers() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    public void testShowsSecondTwoMethodsWithCorrectDivider() {
        when(mBraintree.isPayPalEnabled()).thenReturn(false);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    public void testStartsPayWithPayPal() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        button.findViewById(R.id.bt_paypal_button).performClick();
        verify(mBraintree).startPayWithPayPal(null, PaymentButton.REQUEST_CODE);
    }

    public void testStartsPayWithVenmo() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        button.findViewById(R.id.bt_venmo_button).performClick();
        verify(mBraintree).startPayWithVenmo(null, PaymentButton.REQUEST_CODE);
    }

    public void testStartsPayWithAndroidPay() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        button.findViewById(R.id.bt_android_pay_button).performClick();
        verify(mBraintree).startPayWithAndroidPay(null, PaymentButton.REQUEST_CODE);
    }

    public void testDoesNotLaunchFinishMethodsOnNonOkResponses() {
        PaymentButton button = new PaymentButton(getContext());

        button.onActivityResult(PaymentButton.REQUEST_CODE, Activity.RESULT_CANCELED, new Intent());
        verify(mBraintree, never()).finishPayWithPayPal(any(Activity.class), anyInt(), any(Intent.class));
        verify(mBraintree, never()).finishPayWithVenmo(anyInt(), any(Intent.class));
        verify(mBraintree, never()).finishPayWithAndroidPay(anyInt(), any(Intent.class));
    }

    public void testDoesNotLaunchFinishMethodsOnUnknownRequestCode() {
        PaymentButton button = new PaymentButton(getContext());

        button.onActivityResult(PaymentButton.REQUEST_CODE - 1, Activity.RESULT_CANCELED, new Intent());
        verify(mBraintree, never()).finishPayWithPayPal(any(Activity.class), anyInt(), any(Intent.class));
        verify(mBraintree, never()).finishPayWithVenmo(anyInt(), any(Intent.class));
        verify(mBraintree, never()).finishPayWithAndroidPay(anyInt(), any(Intent.class));
    }

    public void testAllowsRequestCodeOverride() {
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree, 500);
        button.onActivityResult(500, Activity.RESULT_OK, new Intent());
        verify(mBraintree, never()).finishPayWithPayPal(any(Activity.class), anyInt(),
                any(Intent.class));
        verify(mBraintree, never()).finishPayWithAndroidPay(anyInt(), any(Intent.class));
        verify(mBraintree).finishPayWithVenmo(anyInt(), any(Intent.class));
    }

    public void testFinishesPayPalOnPayPalIntent() {
        PaymentButton button = new PaymentButton(getContext());
        button.initialize(null, mBraintree);

        Intent intent = new Intent()
                .putExtra(PayPalTouchActivity.EXTRA_LOGIN_CONFIRMATION, newParcelable());
        button.onActivityResult(PaymentButton.REQUEST_CODE, Activity.RESULT_OK, intent);
        verify(mBraintree).finishPayWithPayPal(null, Activity.RESULT_OK, intent);
        verify(mBraintree, never()).finishPayWithVenmo(anyInt(), any(Intent.class));
        verify(mBraintree, never()).finishPayWithAndroidPay(anyInt(), any(Intent.class));
    }

    public void testFinishesVenmo() {
        PaymentButton button = new PaymentButton(getContext());
        button.initialize(null, mBraintree);

        Intent intent = new Intent();
        button.onActivityResult(PaymentButton.REQUEST_CODE, Activity.RESULT_OK, intent);
        verify(mBraintree, never()).finishPayWithPayPal(any(Activity.class), anyInt(), any(Intent.class));
        verify(mBraintree).finishPayWithVenmo(Activity.RESULT_OK, intent);
    }

    public void testFinishesAndroidPayOnAndroidPayIntent() {
        PaymentButton button = new PaymentButton(getContext());
        button.initialize(null, mBraintree);

        Intent intent = new Intent()
                .putExtra(WalletConstants.EXTRA_FULL_WALLET, newParcelable());
        button.onActivityResult(PaymentButton.REQUEST_CODE, Activity.RESULT_OK, intent);
        verify(mBraintree).finishPayWithAndroidPay(Activity.RESULT_OK, intent);
        verify(mBraintree, never()).finishPayWithPayPal(any(Activity.class), anyInt(), any(Intent.class));
        verify(mBraintree, never()).finishPayWithVenmo(anyInt(), any(Intent.class));
    }

    private Parcelable newParcelable() {
        return new Parcelable() {
            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
            }
        };
    }
}
