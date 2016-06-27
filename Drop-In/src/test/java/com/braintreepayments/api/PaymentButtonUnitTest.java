package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ViewSwitcher;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.VenmoConfiguration;
import com.braintreepayments.testutils.FragmentTestActivity;
import com.google.android.gms.identity.intents.model.CountrySpecification;
import com.google.android.gms.wallet.Cart;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest({ PayPal.class, Venmo.class, AndroidPay.class })
public class PaymentButtonUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Activity mActivity;
    private BraintreeFragment mBraintreeFragment;

    @Before
    public void setup() {
        mActivity = Robolectric.setupActivity(FragmentTestActivity.class);

        mBraintreeFragment = mock(BraintreeFragment.class);
        mActivity.getFragmentManager().beginTransaction().add(mBraintreeFragment, BraintreeFragment.TAG).commit();
    }

    @Test
    public void newInstance_returnsAPaymentButtonFromATokenizationKey() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY);

        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertNotNull(paymentButton);
    }

    @Test
    public void newInstance_returnsAPaymentButtonFromAClientToken() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken(stringFromFixture("client_token.json"));

        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertNotNull(paymentButton);
    }

    @Test(expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionWhenCheckoutRequestIsMissingAuthorization()
            throws InvalidArgumentException {
        getPaymentButton(new PaymentRequest());
    }

    @Test(expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionForABadTokenizationKey() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken("test_key_merchant");

        getPaymentButton(paymentRequest);
    }

    @Test(expected = InvalidArgumentException.class)
    public void newInstance_throwsAnExceptionForABadClientToken() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .clientToken("{}");

        getPaymentButton(paymentRequest);
    }

    @Test
    public void newInstance_returnsAnExistingInstance() throws InvalidArgumentException {
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY);

        PaymentButton paymentButton1 = getPaymentButton(paymentRequest);
        PaymentButton paymentButton2 = getPaymentButton(paymentRequest);

        assertEquals(paymentButton1, paymentButton2);
    }

    @Test
    public void visibilityIsGoneIfAPaymentRequestIsNotPresent() {
        setEnabledPaymentMethods(true, true, true);
        PaymentButton paymentButton = new PaymentButton();
        mActivity.getFragmentManager().beginTransaction().add(1, paymentButton, "test").commit();

        assertEquals(View.GONE, paymentButton.getView().getVisibility());
    }

    @Test(expected = InvalidArgumentException.class)
    public void setPaymentRequest_throwsExceptionForInvalidAuthorization() throws InvalidArgumentException {
        PaymentButton paymentButton = new PaymentButton();
        mActivity.getFragmentManager().beginTransaction()
                .remove(mBraintreeFragment)
                .add(1, paymentButton, "test")
                .commit();

        paymentButton.setPaymentRequest(new PaymentRequest());
    }

    @Test
    public void setPaymentRequest_initializesPaymentButton() throws InvalidArgumentException, JSONException,
            InterruptedException {
        setEnabledPaymentMethods(true, true, true);
        PaymentButton paymentButton = new PaymentButton();
        mActivity.getFragmentManager().beginTransaction().add(1, paymentButton, "test").commit();

        assertEquals(View.GONE, paymentButton.getView().getVisibility());

        paymentButton.setPaymentRequest(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
    }

    @Test
    public void showsLoadingIndicatorWhileWaitingForConfiguration() throws InvalidArgumentException {
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        ViewSwitcher viewSwitcher = (ViewSwitcher) paymentButton.getView()
                .findViewById(com.braintreepayments.api.dropin.R.id.bt_payment_method_view_switcher);
        assertEquals(1, viewSwitcher.getDisplayedChild());
    }

    @Test
    public void visibilityIsGoneWhenConfigurationFails() throws JSONException, InvalidArgumentException,
            InterruptedException {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0] != null) {
                    ((BraintreeResponseListener<Exception>) invocation.getArguments()[0]).onResponse(new Exception());
                }
                return null;
            }
        }).when(mBraintreeFragment).setConfigurationErrorListener(any(BraintreeResponseListener.class));

        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().clientToken(TOKENIZATION_KEY));

        assertEquals(View.GONE, paymentButton.getView().getVisibility());
    }

    @Test
    public void callsOnClickListener() throws InvalidArgumentException, InterruptedException {
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));
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

    @Test
    public void doesNotCrashWhenNoOnClickListenerIsSet() throws InvalidArgumentException {
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        paymentButton.onClick(paymentButton.getView());
    }

    @Test
    public void notVisibleWhenNoMethodsAreEnabled() throws InvalidArgumentException, JSONException {
        setEnabledPaymentMethods(false, false, false);
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        assertEquals(View.GONE, paymentButton.getView().getVisibility());
    }

    @Test
    public void onlyShowsPayPal() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, false, false);
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void onlyShowsVenmo() throws InvalidArgumentException {
        setEnabledPaymentMethods(false, true, false);
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void onlyShowsAndroidPay() throws InvalidArgumentException {
        setEnabledPaymentMethods(false, false, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void showsPayPalAndAndroidPay() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, false, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void showsPayPalAndVenmo() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, false);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void showsVenmoAndAndroidPay() throws InvalidArgumentException {
        setEnabledPaymentMethods(false, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void showsAllMethodsAndDividers() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build());
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void doesNotShowPayPalWhenDisabled() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build())
                .disablePayPal();
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void doesNotShowVenmoWhenDisabled() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build())
                .disableVenmo();
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void doesNotShowAndroidPayWhenDisabled() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(Cart.newBuilder().build())
                .disableAndroidPay();
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void doesNotShowAndroidPayWhenNoCartIsIncludedInPaymentRequest() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        assertEquals(View.VISIBLE, paymentButton.getView().getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_android_pay_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_venmo_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_paypal_button).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).getVisibility());
        assertEquals(View.VISIBLE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider).getVisibility());
        assertEquals(View.GONE, paymentButton.getView().findViewById(R.id.bt_payment_button_divider_2).getVisibility());
    }

    @Test
    public void startsPayWithPayPal() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        paymentButton.getView().findViewById(R.id.bt_paypal_button).performClick();

        verifyStatic();
        PayPal.authorizeAccount(mBraintreeFragment, null);
    }

    @Test
    public void startsPayWithPayPalWhenPayPalIsTheOnlyPaymentMethod() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, false, false);
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        paymentButton.getView().findViewById(R.id.bt_paypal_monogram_button).performClick();

        verifyStatic();
        PayPal.authorizeAccount(mBraintreeFragment, null);
    }

    @Test
    public void startsPayWithPayPalWithAddressScope() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        List<String> scopes = Collections.singletonList(PayPal.SCOPE_ADDRESS);
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .paypalAdditionalScopes(scopes);
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        paymentButton.getView().findViewById(R.id.bt_paypal_button).performClick();

        verifyStatic();
        PayPal.authorizeAccount(mBraintreeFragment, scopes);
    }

    @Test
    public void startsPayWithVenmo() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        PaymentButton paymentButton = getPaymentButton(new PaymentRequest().tokenizationKey(TOKENIZATION_KEY));

        paymentButton.getView().findViewById(R.id.bt_venmo_button).performClick();

        verifyStatic();
        Venmo.authorizeAccount(mBraintreeFragment, true);
    }

    @Test
    public void startsPayWithAndroidPay() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        Cart cart = Cart.newBuilder().build();
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayCart(cart);
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        paymentButton.getView().findViewById(R.id.bt_android_pay_button).performClick();

        verifyStatic();
        AndroidPay.performMaskedWalletRequest(mBraintreeFragment, cart, false, false, Collections.<CountrySpecification>emptyList(), 0);
    }

    @Test
    public void startsPayWithAndroidPayWithArguments() throws InvalidArgumentException {
        setEnabledPaymentMethods(true, true, true);
        Cart cart = Cart.newBuilder().build();
        PaymentRequest paymentRequest = new PaymentRequest()
                .tokenizationKey(TOKENIZATION_KEY)
                .androidPayPhoneNumberRequired(true)
                .androidPayShippingAddressRequired(true)
                .androidPayRequestCode(42)
                .androidPayAllowedCountriesForShipping("US", "GB")
                .androidPayCart(cart);
        PaymentButton paymentButton = getPaymentButton(paymentRequest);

        paymentButton.getView().findViewById(R.id.bt_android_pay_button).performClick();

        verifyStatic();
        AndroidPay.performMaskedWalletRequest(mBraintreeFragment, cart, true, true,
                paymentRequest.getAndroidPayAllowedCountriesForShipping(), 42);
    }

    /* helpers */
    private PaymentButton getPaymentButton(PaymentRequest paymentRequest) throws InvalidArgumentException {
        Robolectric.getForegroundThreadScheduler().pause();
        PaymentButton paymentButton = PaymentButton.newInstance(mActivity, 1, paymentRequest);
        Robolectric.getForegroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();

        return paymentButton;
    }

    private void setEnabledPaymentMethods(boolean paypalEnabled, boolean venmoEnabled,
            final boolean androidPayEnabled) {
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(venmoConfiguration.isEnabled(any(Context.class))).thenReturn(venmoEnabled);

        mockStatic(Venmo.class);
        doNothing().when(Venmo.class);
        Venmo.authorizeAccount(any(BraintreeFragment.class));

        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.isEnabled(any(Context.class))).thenReturn(androidPayEnabled);

        mockStatic(AndroidPay.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((BraintreeResponseListener<Boolean>) invocation.getArguments()[1]).onResponse(androidPayEnabled);
                return null;
            }
        }).when(AndroidPay.class);
        AndroidPay.isReadyToPay(any(BraintreeFragment.class), any(BraintreeResponseListener.class));
        doNothing().when(AndroidPay.class);
        AndroidPay.performMaskedWalletRequest(any(BraintreeFragment.class), any(Cart.class), anyBoolean(), anyBoolean(),
                anyCollectionOf(CountrySpecification.class), anyInt());

        mockStatic(PayPal.class);
        doNothing().when(PayPal.class);
        PayPal.authorizeAccount(any(BraintreeFragment.class));
        doNothing().when(PayPal.class);
        PayPal.authorizeAccount(any(BraintreeFragment.class), any(List.class));

        final Configuration configuration = mock(Configuration.class);
        when(configuration.isPayPalEnabled()).thenReturn(paypalEnabled);
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);

        when(mBraintreeFragment.getConfiguration()).thenReturn(configuration);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ConfigurationListener) invocation.getArguments()[0]).onConfigurationFetched(configuration);
                return null;
            }
        }).when(mBraintreeFragment).waitForConfiguration(any(ConfigurationListener.class));
    }
}
