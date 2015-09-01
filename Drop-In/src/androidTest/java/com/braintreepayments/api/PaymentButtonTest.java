package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.support.test.rule.ActivityTestRule;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalConfiguration;
import com.braintreepayments.api.test.TestActivity;
import com.google.android.gms.wallet.Cart;
import com.paypal.android.sdk.payments.PayPalOAuthScopes;
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;
import com.paypal.android.sdk.payments.PayPalTouchActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SmallTest
public class PaymentButtonTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private PaymentButton mPaymentButton;

    @Before
    public void setup() {
        mPaymentButton = spy(new PaymentButton(getTargetContext()));
    }

    @Test(timeout = 1000)
    public void notInflatedByDefault() {
        assertNull(mPaymentButton.findViewById(R.id.bt_paypal_button));
        assertNull(mPaymentButton.findViewById(R.id.bt_venmo_button));
        assertNull(mPaymentButton.findViewById(R.id.bt_android_pay_button));
    }

    @Test(timeout = 1000)
    public void notVisibleWhenNoMethodsAreEnabled() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(false, false, false);
        mPaymentButton.initialize(getFragment());

        assertEquals(View.GONE, mPaymentButton.getVisibility());
    }

    @Test(timeout = 1000)
    public void notVisibleWhenConfigurationFails() throws JSONException, InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(),
                stringFromFixture("client_token_with_bad_config_url.json"));
        getInstrumentation().waitForIdleSync();

        mPaymentButton.initialize(fragment);
        SystemClock.sleep(100);

        assertEquals(View.GONE, mPaymentButton.getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsPayPal() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(true, false, false);
        mPaymentButton.initialize(getFragment());

        assertEquals(View.VISIBLE, mPaymentButton.getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, mPaymentButton.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsVenmo() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(false, true, false);
        mPaymentButton.initialize(getFragment());

        assertEquals(View.VISIBLE, mPaymentButton.getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, mPaymentButton.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsAndroidPay() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(false, false, true);
        mPaymentButton.initialize(getFragment());

        assertEquals(View.VISIBLE, mPaymentButton.getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, mPaymentButton.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, mPaymentButton.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test(timeout = 1000)
    public void showsAllMethodsAndDividers() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(true, true, true);
        mPaymentButton.initialize(getFragment());

        assertEquals(View.VISIBLE, mPaymentButton.getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test(timeout = 1000)
    public void showsSecondTwoMethodsWithCorrectDivider()
            throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(false, true, true);
        mPaymentButton.initialize(getFragment());

        assertEquals(View.VISIBLE, mPaymentButton.getVisibility());
        assertEquals(View.GONE, mPaymentButton.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test(timeout = 1000)
    public void startsPayWithPayPal() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(true, true, true);
        BraintreeFragment fragment = getFragment();
        mPaymentButton.initialize(fragment);

        mPaymentButton.findViewById(R.id.bt_paypal_button).performClick();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(intentCaptor.capture(), anyInt());
        assertEquals(PayPalProfileSharingActivity.class.getName(),
                intentCaptor.getValue().getComponent().getClassName());
    }

    @Test(timeout = 1000)
    public void startsPayWithPayPalWithAddressScope() throws InvalidArgumentException,
            JSONException {
        setEnabledPaymentMethods(true, true, true);
        BraintreeFragment fragment = getFragment();
        List<String> scopes = Collections.singletonList(PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS);
        mPaymentButton.setAdditionalPayPalScopes(scopes);
        mPaymentButton.initialize(fragment);

        mPaymentButton.findViewById(R.id.bt_paypal_button).performClick();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(intentCaptor.capture(), anyInt());
        Intent intent = intentCaptor.getValue();
        String paypalScopes = intent.getParcelableExtra(PayPalTouchActivity.EXTRA_REQUESTED_SCOPES)
                .toString();
        assertEquals(PayPalProfileSharingActivity.class.getName(),
                intent.getComponent().getClassName());
        assertTrue(paypalScopes.contains("https://uri.paypal.com/services/payments/futurepayments"));
        assertTrue(paypalScopes.contains("email"));
        assertTrue(paypalScopes.contains("address"));
    }

    @Test(timeout = 1000)
    public void startsPayWithVenmo() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(true, true, true);
        BraintreeFragment fragment = getFragment();
        mPaymentButton.initialize(fragment);

        mPaymentButton.findViewById(R.id.bt_venmo_button).performClick();

        verify(fragment).sendAnalyticsEvent("venmo.selected");
    }

    @Test(timeout = 1000)
    public void startsPayWithAndroidPay() throws JSONException, InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        BraintreeFragment fragment = getFragment();
        mPaymentButton.setAndroidPayOptions(Cart.newBuilder().build(), 1);
        mPaymentButton.initialize(fragment);

        mPaymentButton.findViewById(R.id.bt_android_pay_button).performClick();

        verify(fragment).getGoogleApiClient();
    }

    /* helpers */
    private void setEnabledPaymentMethods(boolean paypalEnabled, boolean venmoEnabled, boolean androidPayEnabled) {
        doReturn(paypalEnabled).when(mPaymentButton).isPayPalEnabled();
        doReturn(venmoEnabled).when(mPaymentButton).isVenmoEnabled();
        doReturn(androidPayEnabled).when(mPaymentButton).isAndroidPayEnabled();
    }

    private BraintreeFragment getFragment() throws InvalidArgumentException, JSONException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("merchant-id");
        when(configuration.getVenmoState()).thenReturn("offline");

        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);

        PayPalConfiguration paypalConfiguration = PayPalConfiguration.fromJson(
                new JSONObject(stringFromFixture("paypal_configuration.json")));
        when(configuration.getPayPal()).thenReturn(paypalConfiguration);

        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getDisplayName()).thenReturn("Test");
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint()).thenReturn(
                "google-auth-fingerprint");
        when(androidPayConfiguration.getSupportedNetworks()).thenReturn(new String[0]);
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);

        Activity activity = mActivityTestRule.getActivity();
        BraintreeFragment fragment = spy(BraintreeFragment.newInstance(activity,
                stringFromFixture("client_token.json")));
        doNothing().when(fragment).fetchConfiguration();
        when(fragment.getContext()).thenReturn(getTargetContext());
        when(fragment.getConfiguration()).thenReturn(configuration);
        doNothing().when(fragment).startActivity(any(Intent.class));
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        getInstrumentation().waitForIdleSync();

        return fragment;
    }
}
