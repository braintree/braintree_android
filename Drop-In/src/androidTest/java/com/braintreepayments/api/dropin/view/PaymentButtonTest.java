package com.braintreepayments.api.dropin.view;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.test.AndroidTestCase;
import android.view.View;

import com.braintreepayments.api.AppSwitch;
import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.BraintreeBrowserSwitchActivity;
import com.braintreepayments.api.dropin.R;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.WalletConstants;
import com.paypal.android.sdk.payments.PayPalOAuthScopes;
import com.paypal.android.sdk.payments.PayPalTouchActivity;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
        when(mBraintree.isCoinbaseEnabled()).thenReturn(false);
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
        assertEquals(View.GONE, button.findViewById(R.id.bt_coinbase_button).getVisibility());
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
        assertEquals(View.GONE, button.findViewById(R.id.bt_coinbase_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    public void testOnlyShowsCoinbase() {
        when(mBraintree.isPayPalEnabled()).thenReturn(false);
        when(mBraintree.isVenmoEnabled()).thenReturn(false);
        when(mBraintree.isCoinbaseEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_coinbase_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE,
                button.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    public void testOnlyShowsAndroidPay() {
        when(mBraintree.isPayPalEnabled()).thenReturn(false);
        when(mBraintree.isVenmoEnabled()).thenReturn(false);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.setAndroidPayOptions(Cart.newBuilder().build(), false, false, false);
        button.initialize(null, mBraintree);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_coinbase_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    public void testShowsAndroidPayIfBillingAgreementIsTrue() {
        when(mBraintree.isPayPalEnabled()).thenReturn(false);
        when(mBraintree.isVenmoEnabled()).thenReturn(false);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.setAndroidPayOptions(null, true, false, false);
        button.initialize(null, mBraintree);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
    }

    public void testDoesntShowAndroidPayIfSetAndroidPayOptionsWasNotCalled() {
        when(mBraintree.isPayPalEnabled()).thenReturn(false);
        when(mBraintree.isVenmoEnabled()).thenReturn(false);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        assertEquals(View.GONE, button.getVisibility());
        assertEquals(View.GONE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
    }

    public void testShowsAllMethodsAndDividers() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        when(mBraintree.isCoinbaseEnabled()).thenReturn(true);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.setAndroidPayOptions(Cart.newBuilder().build(), false, false, false);
        button.initialize(null, mBraintree);
        assertEquals(View.VISIBLE, button.getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_coinbase_button).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.VISIBLE, button.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    public void testShowsSecondTwoMethodsWithCorrectDivider() {
        when(mBraintree.isPayPalEnabled()).thenReturn(false);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.setAndroidPayOptions(Cart.newBuilder().build(), false, false, false);
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
        verify(mBraintree).startPayWithPayPal(null, PaymentButton.REQUEST_CODE, null);
    }

    public void testStartsPayWithPayPalWithAddressScope() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        List<String> scopes = Arrays.asList(PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS);
        button.setAdditionalPayPalScopes(scopes);
        button.initialize(null, mBraintree);
        button.findViewById(R.id.bt_paypal_button).performClick();
        verify(mBraintree).startPayWithPayPal(null, PaymentButton.REQUEST_CODE, scopes);
    }

    public void testStartsPayWithVenmo() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        button.findViewById(R.id.bt_venmo_button).performClick();
        verify(mBraintree).startPayWithVenmo(null, PaymentButton.REQUEST_CODE);
    }

    public void testStartsPayWithCoinbase() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        when(mBraintree.isCoinbaseEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree);
        button.findViewById(R.id.bt_coinbase_button).performClick();
        verify(mBraintree).startPayWithCoinbase(null, PaymentButton.REQUEST_CODE);
    }

    public void testStartsPayWithAndroidPay() {
        when(mBraintree.isPayPalEnabled()).thenReturn(true);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        when(mBraintree.isAndroidPayEnabled()).thenReturn(true);
        PaymentButton button = new PaymentButton(getContext());
        Cart cart = Cart.newBuilder().build();

        button.setAndroidPayOptions(cart, true, true, true);
        button.initialize(null, mBraintree);
        button.findViewById(R.id.bt_android_pay_button).performClick();
        verify(mBraintree).performAndroidPayMaskedWalletRequest(null, PaymentButton.REQUEST_CODE,
                cart, true, true, true);
    }

    public void testDoesNotLaunchFinishMethodsOnNonOkResponses() {
        PaymentButton button = new PaymentButton(getContext());

        Intent intent = new Intent();
        button.onActivityResult(PaymentButton.REQUEST_CODE, Activity.RESULT_CANCELED, intent);
        verifyNoMoreInteractions(mBraintree);
    }

    public void testDoesNotLaunchFinishMethodsOnUnknownRequestCode() {
        PaymentButton button = new PaymentButton(getContext());

        Intent intent = new Intent();
        button.onActivityResult(PaymentButton.REQUEST_CODE - 1, Activity.RESULT_CANCELED, intent);
        verifyNoMoreInteractions(mBraintree);
    }

    public void testAllowsRequestCodeOverride() {
        PaymentButton button = new PaymentButton(getContext());

        button.initialize(null, mBraintree, 500);
        Intent intent = new Intent().putExtra(AppSwitch.EXTRA_PAYMENT_METHOD_NONCE, "");
        button.onActivityResult(500, Activity.RESULT_OK, intent);
        verify(mBraintree).onActivityResult(null, 500, Activity.RESULT_OK, intent);
    }

    public void testFinishesPayPalOnPayPalIntent() {
        PaymentButton button = new PaymentButton(getContext());
        button.initialize(null, mBraintree);

        Intent intent = new Intent()
                .putExtra(PayPalTouchActivity.EXTRA_LOGIN_CONFIRMATION, newParcelable());
        button.onActivityResult(PaymentButton.REQUEST_CODE, Activity.RESULT_OK, intent);
        verify(mBraintree).onActivityResult(null, PaymentButton.REQUEST_CODE, Activity.RESULT_OK,
                intent);
    }

    public void testFinishesCoinbaseOnCoinbaseIntent() {
        Activity mockActivity = mock(Activity.class);
        when(mockActivity.getPackageName()).thenReturn("com.braintree.test");
        PaymentButton button = new PaymentButton(getContext());
        button.initialize(mockActivity, mBraintree);

        Intent intent = new Intent()
                .putExtra(BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL,
                        Uri.parse("com.braintree.test.braintree://coinbase?code=1234"));
        button.onActivityResult(PaymentButton.REQUEST_CODE, Activity.RESULT_OK, intent);
        verify(mBraintree).finishPayWithCoinbase(Activity.RESULT_OK, intent);
    }

    public void testFinishesVenmo() {
        PaymentButton button = new PaymentButton(getContext());
        button.initialize(null, mBraintree);

        Intent intent = new Intent().putExtra(AppSwitch.EXTRA_PAYMENT_METHOD_NONCE, "");
        button.onActivityResult(PaymentButton.REQUEST_CODE, Activity.RESULT_OK, intent);
        verify(mBraintree).onActivityResult(null, PaymentButton.REQUEST_CODE, Activity.RESULT_OK,
                intent);
    }

    public void testFinishesAndroidPayOnAndroidPayIntent() {
        PaymentButton button = new PaymentButton(getContext());
        button.initialize(null, mBraintree);

        Intent intent = new Intent()
                .putExtra(WalletConstants.EXTRA_MASKED_WALLET, newParcelable());
        button.onActivityResult(PaymentButton.REQUEST_CODE, Activity.RESULT_OK, intent);
        verify(mBraintree).onActivityResult(null, PaymentButton.REQUEST_CODE, Activity.RESULT_OK,
                intent);
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
