package com.braintreepayments.api;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ViewSwitcher;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.internal.SignatureVerificationTestUtils;
import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.VenmoConfiguration;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.BraintreeActivityTestRule;
import com.braintreepayments.testutils.MockContextForVenmo;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.core.deps.guava.base.Preconditions.checkNotNull;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.BundleMatchers.hasEntry;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasHost;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasParamWithName;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasParamWithValue;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasPath;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasScheme;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.SharedPreferencesHelper.writeMockConfiguration;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PaymentButtonTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private Activity mActivity;
    private PaymentButton mPaymentButton;

    @Before
    public void setup() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test(timeout = 1000)
    public void newInstance_returnsAPaymentButtonFromATokenizationKey()
            throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY);
        mPaymentButton = PaymentButton.newInstance(mActivity, 0, paymentRequest);

        assertNotNull(mPaymentButton);
    }

    @Test(timeout = 1000)
    public void newInstance_returnsAPaymentButtonFromAClientToken()
            throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken(stringFromFixture("client_token.json"));
        mPaymentButton = PaymentButton.newInstance(mActivity, 0, paymentRequest);

        assertNotNull(mPaymentButton);
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionWhenCheckoutRequestIsMissingAuthorization()
            throws InvalidArgumentException {
        PaymentButton.newInstance(mActivity, 0, new PaymentRequest());
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionForABadTokenizationKey() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken("test_key_merchant");
        PaymentButton.newInstance(mActivity, 0, paymentRequest);
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionForABadClientToken() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken("{}");
        PaymentButton.newInstance(mActivity, 0, paymentRequest);
    }

    @Test(timeout = 1000)
    public void newInstance_returnsAnExistingInstance() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY);
        PaymentButton paymentButton1 = PaymentButton.newInstance(mActivity, 0, paymentRequest);
        getInstrumentation().waitForIdleSync();
        PaymentButton paymentButton2 = PaymentButton.newInstance(mActivity, 0, paymentRequest);

        assertEquals(paymentButton1, paymentButton2);
    }

    @Test(timeout = 1000)
    public void visibilityIsGoneIfAPaymentRequestIsNotPresent() {
        mPaymentButton = new PaymentButton();
        mActivity.getFragmentManager().beginTransaction().add(mPaymentButton, "test").commit();
        getInstrumentation().waitForIdleSync();

        assertEquals(View.GONE, mPaymentButton.getView().getVisibility());
    }

    @Test(timeout = 1000)
    public void showsLoadingIndicatorWhileWaitingForConfiguration() throws InvalidArgumentException {
        BraintreeFragment fragment = spy(BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY));
        when(fragment.getConfiguration()).thenReturn(null);
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(TOKENIZATION_KEY)) {
            @Override
            public void get(String path, HttpResponseCallback callback) {}
        };
        getInstrumentation().waitForIdleSync();

        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        mPaymentButton.mBraintreeFragment = fragment;
        getInstrumentation().waitForIdleSync();

        ViewSwitcher viewSwitcher = (ViewSwitcher) mPaymentButton.getView()
                .findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_method_view_switcher);
        assertEquals(1, viewSwitcher.getDisplayedChild());
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void setPaymentRequest_throwsExceptionForInvalidAuthorization() throws InvalidArgumentException {
        mPaymentButton = new PaymentButton();
        mActivity.getFragmentManager().beginTransaction().add(mPaymentButton, "test").commit();
        getInstrumentation().waitForIdleSync();

        mPaymentButton.setPaymentRequest(new PaymentRequest());
    }

    @Test(timeout = 1000)
    public void setPaymentRequest_initializesPaymentButton() throws InvalidArgumentException, JSONException,
            InterruptedException {
        mPaymentButton = new PaymentButton();
        mActivity.getFragmentManager().beginTransaction().add(mPaymentButton, "test").commit();
        getInstrumentation().waitForIdleSync();

        assertEquals(View.GONE, mPaymentButton.getView().getVisibility());

        mPaymentButton.setPaymentRequest(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
    }

    @Test(timeout = 1000)
    public void notVisibleWhenConfigurationFails() throws JSONException, InvalidArgumentException,
            InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity,
                stringFromFixture("client_token_with_bad_config_url.json"));
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals(
                        "Request for configuration has failed: Protocol not found: nullnullincorrect_url?configVersion=3&authorizationFingerprint=authorization_fingerprint. Future requests will retry up to 3 times",
                        error.getMessage());
                latch.countDown();
            }
        });
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken(stringFromFixture("client_token_with_bad_config_url.json"));
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content,
                paymentRequest);
        getInstrumentation().waitForIdleSync();

        latch.await();
        assertEquals(View.GONE, mPaymentButton.getView().getVisibility());
    }

    @Test(timeout = 1000)
    public void callsOnClickListener() throws InvalidArgumentException, InterruptedException {
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();
        final CountDownLatch latch = new CountDownLatch(1);
        mPaymentButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                latch.countDown();
            }
        });

        mPaymentButton.onClick(mPaymentButton.getView());
        latch.await();
    }

    @Test(timeout = 1000)
    public void doesNotCrashWhenNoOnClickListenerIsSet() throws InvalidArgumentException {
        mPaymentButton = PaymentButton.newInstance(mActivity, 0,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();

        mPaymentButton.onClick(mPaymentButton.getView());
    }

    @Test(timeout = 1000)
    public void notVisibleWhenNoMethodsAreEnabled() throws InvalidArgumentException, JSONException {
        getFragment(false, false, false);
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();

        assertEquals(View.GONE, mPaymentButton.getView().getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsPayPal() throws InvalidArgumentException, JSONException {
        getFragment(true, false, false);
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider)
                        .getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider_2)
                        .getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsVenmo()
            throws InvalidArgumentException, JSONException, InterruptedException {
        BraintreeFragment fragment = getFragment(false, true, false);
        PaymentRequest paymentRequest = new PaymentRequest().tokenizationKey(TOKENIZATION_KEY);
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        getInstrumentation().waitForIdleSync();
        mPaymentButton.mBraintreeFragment = fragment;
        setupPaymentButton(fragment.getConfiguration(), false);

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider)
                        .getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider_2)
                        .getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsAndroidPay()
            throws InvalidArgumentException, JSONException, InterruptedException {
        BraintreeFragment fragment = getFragment(false, false, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        mPaymentButton.mBraintreeFragment = fragment;
        getInstrumentation().waitForIdleSync();
        setupPaymentButton(fragment.getConfiguration(), true);

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider)
                        .getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider_2)
                        .getVisibility());
    }

    @Test(timeout = 1000)
    public void showsPayPalAndAndroidPay() throws InvalidArgumentException, JSONException, InterruptedException {
        BraintreeFragment fragment = getFragment(true, false, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        getInstrumentation().waitForIdleSync();
        setupPaymentButton(fragment.getConfiguration(), true);

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider)
                        .getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider_2)
                        .getVisibility());
    }

    @Test(timeout = 1000)
    public void showsPayPalAndVenmo() throws InvalidArgumentException, JSONException, InterruptedException {
        BraintreeFragment fragment = getFragment(true, true, false);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        getInstrumentation().waitForIdleSync();
        mPaymentButton.mBraintreeFragment = fragment;
        setupPaymentButton(fragment.getConfiguration(), false);

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider)
                        .getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider_2)
                        .getVisibility());
    }

    @Test(timeout = 1000)
    public void showsVenmoAndAndroidPay() throws InvalidArgumentException, JSONException, InterruptedException {
        BraintreeFragment fragment = getFragment(false, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        mPaymentButton.mBraintreeFragment = fragment;
        getInstrumentation().waitForIdleSync();
        setupPaymentButton(fragment.getConfiguration(), true);

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider)
                        .getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider_2)
                        .getVisibility());
    }

    @Test(timeout = 1000)
    public void showsAllMethodsAndDividers() throws InvalidArgumentException, JSONException, InterruptedException {
        BraintreeFragment fragment = getFragment(true, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        mPaymentButton.mBraintreeFragment = fragment;
        getInstrumentation().waitForIdleSync();
        setupPaymentButton(fragment.getConfiguration(), true);

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider)
                        .getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider_2)
                        .getVisibility());
    }

    @Test(timeout = 5000)
    public void doesNotShowPayPalWhenDisabled() throws InvalidArgumentException, JSONException, InterruptedException {
        BraintreeFragment fragment = getFragment(true, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build())
                .disablePayPal();
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        mPaymentButton.mBraintreeFragment = fragment;
        getInstrumentation().waitForIdleSync();
        setupPaymentButton(fragment.getConfiguration(), true);

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider)
                        .getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider_2)
                        .getVisibility());
    }

    @Test(timeout = 1000)
    public void doesNotShowVenmoWhenDisabled() throws InvalidArgumentException, JSONException, InterruptedException {
        BraintreeFragment fragment = getFragment(true, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build())
                .disableVenmo();
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        mPaymentButton.mBraintreeFragment = fragment;
        getInstrumentation().waitForIdleSync();
        setupPaymentButton(fragment.getConfiguration(), true);

        assertEquals(View.VISIBLE, mPaymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider)
                        .getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.getView().findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_button_divider_2)
                        .getVisibility());
    }

    @Test(timeout = 5000)
    public void startsPayWithPayPal() throws InvalidArgumentException, JSONException, InterruptedException {
        Looper.prepare();
        getFragment(true, true, true);
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content,
                new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
        getInstrumentation().waitForIdleSync();

        clickButton(com.braintreepayments.api.dropin.R.id.bt_paypal_button);

        intending(hasAction(equalTo(Intent.ACTION_VIEW))).respondWith(new ActivityResult(0, null));
        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(hasScheme("https")),
                hasData(hasHost("assets.staging.braintreepayments.com")),
                hasData(hasPath("/one-touch-login/")),
                hasData(hasParamWithName("payload")),
                hasData(not(hasScope("address"))),
                hasData(hasParamWithName("payloadEnc")),
                hasData(hasParamWithValue("x-success",
                        "com.braintreepayments.api.dropin.test.braintree://onetouch/v1/success")),
                hasData(hasParamWithValue("x-cancel",
                        "com.braintreepayments.api.dropin.test.braintree://onetouch/v1/cancel")),
                hasExtras(allOf(hasEntry(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH,
                        true)))));
    }

    @Test(timeout = 5000)
    public void startsPayWithPayPalWithAddressScope() throws InvalidArgumentException, JSONException,
            InterruptedException {
        Looper.prepare();
        getFragment(true, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS));
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content,
                paymentRequest);
        getInstrumentation().waitForIdleSync();

        clickButton(com.braintreepayments.api.dropin.R.id.bt_paypal_button);

        intending(hasAction(equalTo(Intent.ACTION_VIEW))).respondWith(new ActivityResult(0, null));
        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(hasScheme("https")),
                hasData(hasHost("assets.staging.braintreepayments.com")),
                hasData(hasPath("/one-touch-login/")),
                hasData(hasParamWithName("payload")),
                hasData(hasScope("address")),
                hasData(hasParamWithName("payloadEnc")),
                hasData(hasParamWithValue("x-success",
                        "com.braintreepayments.api.dropin.test.braintree://onetouch/v1/success")),
                hasData(hasParamWithValue("x-cancel",
                        "com.braintreepayments.api.dropin.test.braintree://onetouch/v1/cancel")),
                hasExtras(allOf(hasEntry(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH,
                        true)))));
    }

    @Test(timeout = 1000)
    public void startsPayWithVenmo() throws InvalidArgumentException, JSONException, InterruptedException {
        BraintreeFragment fragment = getFragment(true, true, false);
        Context mockContextForVenmo = new MockContextForVenmo()
                .venmoInstalled()
                .build();
        when(fragment.getApplicationContext()).thenReturn(mockContextForVenmo);
        SignatureVerificationTestUtils.disableSignatureVerification();
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY);
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        getInstrumentation().waitForIdleSync();
        mPaymentButton.mBraintreeFragment = fragment;
        setupPaymentButton(fragment.getConfiguration(), true);

        clickButton(com.braintreepayments.api.dropin.R.id.bt_venmo_button);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
    }

    @Test(timeout = 5000)
    public void startsPayWithAndroidPay() throws JSONException, InvalidArgumentException, InterruptedException {
        BraintreeFragment fragment = getFragment(true, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton = PaymentButton.newInstance(mActivity, android.R.id.content, paymentRequest);
        mPaymentButton.mBraintreeFragment = fragment;
        getInstrumentation().waitForIdleSync();
        setupPaymentButton(fragment.getConfiguration(), true);

        clickButton(com.braintreepayments.api.dropin.R.id.bt_android_pay_button);

        verify(fragment).sendAnalyticsEvent("android-pay.selected");
    }

    /** helpers */
    private BraintreeFragment getFragment(boolean paypalEnabled, boolean payWithVenmoEnabled, boolean androidPayEnabled)
            throws InvalidArgumentException, JSONException {
        String configuration;
        if (paypalEnabled && payWithVenmoEnabled && androidPayEnabled) {
            configuration =
                    stringFromFixture("configuration_with_android_pay_and_venmo_and_paypal.json");
        } else if (paypalEnabled && payWithVenmoEnabled) {
            configuration = stringFromFixture("configuration_with_paypal_and_venmo.json");
        } else if (paypalEnabled && androidPayEnabled) {
            configuration = stringFromFixture("configuration_with_android_pay_and_paypal.json");
        } else if (paypalEnabled) {
            configuration = stringFromFixture("configuration_with_paypal.json");
        } else if (payWithVenmoEnabled && androidPayEnabled) {
            configuration = stringFromFixture("configuration_with_venmo_and_android_pay.json");
        } else if (payWithVenmoEnabled) {
            configuration = stringFromFixture("configuration_with_venmo.json");
        } else if (androidPayEnabled) {
            configuration = stringFromFixture("configuration_with_android_pay.json");
        } else {
            configuration = stringFromFixture("configuration.json");
        }

        Authorization clientToken = Authorization.fromString(stringFromFixture("client_token.json"));

        writeMockConfiguration(clientToken.getConfigUrl(), configuration);
        BraintreeFragment fragment = spy(BraintreeFragment.newInstance(mActivity, clientToken.toString()));
        doNothing().when(fragment).startActivity(any(Intent.class));
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());

        if (payWithVenmoEnabled || androidPayEnabled) {
            Configuration configurationObj = spy(Configuration.fromJson(configuration));
            if (payWithVenmoEnabled) {
                VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
                when(venmoConfiguration.isEnabled(any(Context.class))).thenReturn(true);
                when(configurationObj.getPayWithVenmo()).thenReturn(venmoConfiguration);
            }
            if (androidPayEnabled) {
                AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
                when(androidPayConfiguration.isEnabled(any(Context.class))).thenReturn(true);
                when(androidPayConfiguration.getGoogleAuthorizationFingerprint())
                        .thenReturn("google-authorization-fingerprint");
                when(androidPayConfiguration.getSupportedNetworks()).thenReturn(new String[] { "visa" });
                when(configurationObj.getAndroidPay()).thenReturn(androidPayConfiguration);
                fragment.mGoogleApiClient = new GoogleApiClient.Builder(mActivityTestRule.getActivity())
                        .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                                .build())
                        .build();
            }
            when(fragment.getConfiguration()).thenReturn(configurationObj);
        }

        getInstrumentation().waitForIdleSync();

        return fragment;
    }

    private void setupPaymentButton(final Configuration configuration, final boolean isAndroidPayEnabled)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPaymentButton.setupButton(configuration, isAndroidPayEnabled);

                latch.countDown();
            }
        });

        latch.await();
        getInstrumentation().waitForIdleSync();
    }

    private void clickButton(final int id) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPaymentButton.getView().findViewById(id).performClick();
                latch.countDown();
            }
        });

        latch.await();
    }

    private Matcher<Uri> hasScope(final String scope) {
        checkNotNull(scope);

        return new TypeSafeMatcher<Uri>() {
            @Override
            public boolean matchesSafely(Uri uri) {
                String payload =
                        new String(Base64.decode(uri.getQueryParameter("payload"), Base64.DEFAULT));
                return payload.contains(scope);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has scope: " + scope);
            }
        };
    }
}
