package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.os.SystemClock;
import android.support.test.rule.ActivityTestRule;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalConfiguration;
import com.braintreepayments.api.test.TestActivity;
import com.google.android.gms.wallet.Cart;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchActivity;
import com.paypal.android.sdk.onetouch.core.ResponseType;
import com.paypal.android.sdk.onetouch.core.Result;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
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
        assertNull(mPaymentButton.findViewById(R.id.bt_android_pay_button));
    }

    @Test(timeout = 1000)
    public void notVisibleWhenNoMethodsAreEnabled() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(false, false);
        mPaymentButton.initialize(getFragment(), new PaymentRequest());

        assertEquals(View.GONE, mPaymentButton.getVisibility());
    }

    @Test(timeout = 1000)
    public void notVisibleWhenConfigurationFails() throws JSONException, InvalidArgumentException {
        setEnabledPaymentMethods(true, true);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(),
                stringFromFixture("client_token_with_bad_config_url.json"));
        getInstrumentation().waitForIdleSync();

        mPaymentButton.initialize(fragment, new PaymentRequest());
        SystemClock.sleep(100);

        assertEquals(View.GONE, mPaymentButton.getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsPayPal() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(true, false);
        mPaymentButton.initialize(getFragment(), new PaymentRequest());

        assertEquals(View.VISIBLE, mPaymentButton.getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider).getVisibility());
    }

    @Test(timeout = 1000)
    public void onlyShowsAndroidPay() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(false, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton.initialize(getFragment(), paymentRequest);

        assertEquals(View.VISIBLE, mPaymentButton.getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, mPaymentButton.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider).getVisibility());
    }

    @Test(timeout = 1000)
    public void showsAllMethodsAndDividers() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton.initialize(getFragment(), paymentRequest);

        assertEquals(View.VISIBLE, mPaymentButton.getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider).getVisibility());
    }

    @Test(timeout = 1000)
    public void showsSecondTwoMethodsWithCorrectDivider()
            throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(false, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton.initialize(getFragment(), paymentRequest);

        assertEquals(View.VISIBLE, mPaymentButton.getVisibility());
        assertEquals(View.GONE, mPaymentButton.findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE,
                mPaymentButton.findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE,
                mPaymentButton.findViewById(R.id.bt_payment_button_divider).getVisibility());
    }

    @Test(timeout = 5000)
    public void startsPayWithPayPal() throws InvalidArgumentException, JSONException {
        Looper.prepare();
        setEnabledPaymentMethods(true, true);
        BraintreeFragment fragment = getFragment();
        mPaymentButton.initialize(fragment, new PaymentRequest());
        mPaymentButton.findViewById(R.id.bt_paypal_button).performClick();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(intentCaptor.capture(),
                eq(PayPal.PAYPAL_REQUEST_CODE));

        Intent intent = intentCaptor.getValue();
        assertEquals(BraintreeBrowserSwitchActivity.class.getName(),
                intent.getComponent().getClassName());
    }

    @Test(timeout = 1000)
    public void startsPayWithPayPalWithAddressScope() throws InvalidArgumentException,
            JSONException, InterruptedException {
        Looper.prepare();
        setEnabledPaymentMethods(true, true);
        final BraintreeFragment fragment = getFragment();
        PaymentRequest paymentRequest = new PaymentRequest()
                .paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS));
        mPaymentButton.initialize(fragment, paymentRequest);

        final CountDownLatch latch = new CountDownLatch(1);
        fragment.mHttpClient = new BraintreeHttpClient(ClientToken.fromString(stringFromFixture("client_token.json"))) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                try {
                    JSONObject obj = new JSONObject(data);
                    assertNotNull(obj);
                } catch (JSONException e) {
                    fail(e.getMessage());
                }
                latch.countDown();
            }
        };

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent paypalIntent = new Intent();
                Result r = createFakeResult();
                paypalIntent.putExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT, r);

                fragment.onActivityResult(PayPal.PAYPAL_REQUEST_CODE, Activity.RESULT_OK, paypalIntent);
                return null;
            }
        }).when(fragment).startActivityForResult(any(Intent.class),
                eq(PayPal.PAYPAL_REQUEST_CODE));

        mPaymentButton.findViewById(R.id.bt_paypal_button).performClick();

        latch.await();
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(intentCaptor.capture(), anyInt());
        Intent intent = intentCaptor.getValue();
        assertEquals(BraintreeBrowserSwitchActivity.class.getName(),
                intent.getComponent().getClassName());

    }

    @Test(timeout = 1000)
    public void startsPayWithAndroidPay() throws JSONException, InvalidArgumentException {
        Looper.prepare();
        setEnabledPaymentMethods(true, true);
        BraintreeFragment fragment = getFragment();
        PaymentRequest paymentRequest = new PaymentRequest()
                .androidPayCart(Cart.newBuilder().build());
        mPaymentButton.initialize(fragment, paymentRequest);
        getInstrumentation().waitForIdleSync();

        mPaymentButton.findViewById(R.id.bt_android_pay_button).performClick();

        verify(fragment).getGoogleApiClient();
    }

    /* helpers */
    private void setEnabledPaymentMethods(boolean paypalEnabled,
            boolean androidPayEnabled) {
        doReturn(paypalEnabled).when(mPaymentButton).isPayPalEnabled();
        doReturn(androidPayEnabled).when(mPaymentButton).isAndroidPayEnabled();
    }

    private BraintreeFragment getFragment()
            throws InvalidArgumentException, JSONException {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.isPayPalEnabled()).thenReturn(true);
        when(configuration.getMerchantId()).thenReturn("merchant-id");

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
        Authorization clientToken = Authorization.fromString(stringFromFixture("client_token.json"));
        BraintreeFragment fragment = spy(BraintreeFragment.newInstance(activity,
                stringFromFixture("client_token.json")));
        doNothing().when(fragment).fetchConfiguration();
        when(fragment.getApplicationContext()).thenReturn(getTargetContext());
        when(fragment.getConfiguration()).thenReturn(configuration);
        doNothing().when(fragment).startActivity(any(Intent.class));
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        when(fragment.getAuthorization()).thenReturn(clientToken);
        getInstrumentation().waitForIdleSync();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object arg0 = invocation.getArguments()[0];
                ConfigurationListener configurationListener = (ConfigurationListener) arg0;
                configurationListener.onConfigurationFetched(configuration);
                return null;
            }
        }).when(fragment).waitForConfiguration(any(ConfigurationListener.class));

        return fragment;
    }

    private Result createFakeResult() {
        Class resultClass = Result.class;
        try {
            Constructor<Result> constructor =
                    resultClass.getDeclaredConstructor(String.class, ResponseType.class,
                            JSONObject.class, String.class);
            constructor.setAccessible(true);
            Result result = constructor.newInstance(
                    "var1",
                    ResponseType.authorization_code,
                    new JSONObject(),
                    "var4");


            return result;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
