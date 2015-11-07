package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.FlakyTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ViewSwitcher;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.test.TestActivity;
import com.google.android.gms.wallet.Cart;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PaymentButtonTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private Activity mActivity;

    @Before
    public void setup() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test(timeout = 1000)
    public void newInstance_returnsAPaymentButtonFromAClientKey() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY);
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity, paymentRequest);

        assertNotNull(paymentButton);
    }

    @Test(timeout = 1000)
    public void newInstance_returnsAPaymentButtonFromAClientToken()
            throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken(stringFromFixture("client_token.json"));
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity, paymentRequest);

        assertNotNull(paymentButton);
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionWhenCheckoutRequestIsMissingAuthorization()
            throws InvalidArgumentException {
        PaymentButton.newInstance(mActivity, new PaymentRequest());
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionForABadClientKey() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken("test_key_merchant");
        PaymentButton.newInstance(mActivity, paymentRequest);
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionForABadClientToken() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken("{}");
        PaymentButton.newInstance(mActivity, paymentRequest);
    }

    @Test(timeout = 1000)
    public void newInstance_returnsAnExistingInstance() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY);
        PaymentButton paymentButton1 = PaymentButton.newInstance(mActivity, paymentRequest);
        getInstrumentation().waitForIdleSync();
        PaymentButton paymentButton2 = PaymentButton.newInstance(mActivity, paymentRequest);

        assertEquals(paymentButton1, paymentButton2);
    }

    @Test(timeout = 1000)
    public void visibilityIsGoneIfAPaymentRequestIsNotPresent() {
        PaymentButton paymentButton = new PaymentButton();
        mActivity.getFragmentManager().beginTransaction().add(paymentButton, "test").commit();
        getInstrumentation().waitForIdleSync();

        assertEquals(View.GONE, paymentButton.getView().getVisibility());
    }

    @Test(timeout = 1000)
    public void showsLoadingIndicatorWhileWaitingForConfiguration()
            throws InvalidArgumentException {
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(TOKENIZATION_KEY)) {
            @Override
            public void get(String path, HttpResponseCallback callback) {}
        };
        getInstrumentation().waitForIdleSync();

        PaymentButton paymentButton = PaymentButton.newInstance(mActivity,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();

        ViewSwitcher viewSwitcher = (ViewSwitcher) paymentButton.getView().findViewById(R.id.bt_payment_method_view_switcher);
        assertEquals(1, viewSwitcher.getDisplayedChild());
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void setPaymentRequest_throwsExceptionForInvalidAuthorization()
            throws InvalidArgumentException {
        PaymentButton paymentButton = new PaymentButton();
        mActivity.getFragmentManager().beginTransaction().add(paymentButton, "test").commit();
        getInstrumentation().waitForIdleSync();

        paymentButton.setPaymentRequest(new PaymentRequest());
    }

    @Test(timeout = 1000)
    public void setPaymentRequest_initializesPaymentButton()
            throws InvalidArgumentException, JSONException {
        PaymentButton paymentButton = new PaymentButton();
        mActivity.getFragmentManager().beginTransaction().add(paymentButton, "test").commit();
        getInstrumentation().waitForIdleSync();

        assertEquals(View.GONE, paymentButton.getView().getVisibility());

        paymentButton.setPaymentRequest(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();
        SystemClock.sleep(100);

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
    }

    @Test(timeout = 1000)
    public void notVisibleWhenConfigurationFails()
            throws JSONException, InvalidArgumentException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity,
                stringFromFixture("client_token_with_bad_config_url.json"));
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals(
                        "Protocol not found: nullincorrect_url?configVersion=3&authorizationFingerprint=authorization_fingerprint",
                        error.getMessage());
                latch.countDown();
            }
        });
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken(stringFromFixture("client_token_with_bad_config_url.json"));
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity, paymentRequest);
        getInstrumentation().waitForIdleSync();

        latch.await();
        assertEquals(View.GONE, paymentButton.getView().getVisibility());
    }

    @Test(timeout = 1000)
    public void callsOnClickListener() throws InvalidArgumentException, InterruptedException {
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();
        final CountDownLatch latch = new CountDownLatch(1);
        paymentButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                latch.countDown();
            }
        });

        paymentButton.onClick(paymentButton.getView());
        latch.await();
    }

    @Test(timeout = 1000)
    public void doesNotCrashWhenNoOnClickListenerIsSet() throws InvalidArgumentException {
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();

        paymentButton.onClick(paymentButton.getView());
    }

    @Test(timeout = 1000)
    public void notVisibleWhenNoMethodsAreEnabled() throws InvalidArgumentException, JSONException {
        getFragment(false, false);
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();

        assertEquals(View.GONE, paymentButton.getView().getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsPayPal() throws InvalidArgumentException, JSONException {
        getFragment(true, false);
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsAndroidPay() throws InvalidArgumentException, JSONException {
        getFragment(false, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity, paymentRequest);
        getInstrumentation().waitForIdleSync();

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
    }

    @Test(timeout = 1000)
    public void showsAllMethodsAndDividers() throws InvalidArgumentException, JSONException {
        getFragment(true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity, paymentRequest);
        getInstrumentation().waitForIdleSync();

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE,
                paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE,
                paymentButton.getView().findViewById(R.id.bt_payment_button_divider)
                        .getVisibility());
    }

    @Test(timeout = 5000)
    @FlakyTest(tolerance = 3)
    public void startsPayWithPayPal() throws InvalidArgumentException, JSONException {
        Looper.prepare();
        BraintreeFragment fragment = getFragment(true, true);
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();
        paymentButton.mBraintreeFragment = fragment;

        paymentButton.getView().findViewById(R.id.bt_paypal_button).performClick();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();
        String payload = intentCaptor.getValue().getData().getQueryParameter("payload");
        String request = new String(Base64.decode(payload, Base64.DEFAULT));
        assertTrue(intent.getDataString()
                .startsWith("https://assets.staging.braintreepayments.com/one-touch-login"));
        assertFalse(request.contains("address"));
    }

    @Test(timeout = 5000)
    public void startsPayWithPayPalWithAddressScope() throws InvalidArgumentException,
            JSONException, InterruptedException {
        Looper.prepare();
        final BraintreeFragment fragment = getFragment(true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS));
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity, paymentRequest);
        getInstrumentation().waitForIdleSync();
        paymentButton.mBraintreeFragment = fragment;

        paymentButton.getView().findViewById(R.id.bt_paypal_button).performClick();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivity(intentCaptor.capture());

        String payload = intentCaptor.getValue().getData().getQueryParameter("payload");
        String request = new String(Base64.decode(payload, Base64.DEFAULT));
        assertTrue(request.contains("address"));
    }

    @Test(timeout = 1000)
    public void startsPayWithAndroidPay() throws JSONException, InvalidArgumentException {
        Looper.prepare();
        BraintreeFragment fragment = getFragment(true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity, paymentRequest);
        getInstrumentation().waitForIdleSync();
        paymentButton.mBraintreeFragment = fragment;

        paymentButton.getView().findViewById(R.id.bt_android_pay_button).performClick();

        verify(fragment).getGoogleApiClient();
    }

    /** helpers */
    private BraintreeFragment getFragment(boolean paypalEnabled, boolean androidPayEnabled)
            throws InvalidArgumentException, JSONException {
        String configuration;
        if (paypalEnabled && androidPayEnabled) {
            configuration = stringFromFixture("configuration_with_android_pay_and_paypal.json");
        } else if (paypalEnabled) {
            configuration = stringFromFixture("configuration_with_paypal.json");
        } else if (androidPayEnabled) {
            configuration = stringFromFixture("configuration_with_android_pay.json");
        } else {
            configuration = stringFromFixture("configuration.json");
        }

        Bundle bundle = new Bundle();
        bundle.putString(BraintreeFragment.EXTRA_CONFIGURATION, configuration);
        BraintreeFragment fragment = spy(BraintreeFragment.newInstance(mActivity,
                stringFromFixture("client_token.json"), bundle));
        doNothing().when(fragment).startActivity(any(Intent.class));
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        getInstrumentation().waitForIdleSync();

        return fragment;
    }
}
