//package com.braintreepayments.api;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.support.test.rule.ActivityTestRule;
//import android.support.test.runner.AndroidJUnit4;
//import android.test.suitebuilder.annotation.SmallTest;
//
//import com.braintreepayments.api.exceptions.ErrorWithResponse;
//import com.braintreepayments.api.interfaces.BraintreeErrorListener;
//import com.braintreepayments.api.models.AnalyticsConfiguration;
//import com.braintreepayments.api.models.Configuration;
//import com.braintreepayments.api.test.TestActivity;
//import com.paypal.android.sdk.payments.PayPalConfiguration;
//import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
//import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;
//import com.paypal.android.sdk.payments.PayPalService;
//import com.paypal.android.sdk.payments.PayPalTouchActivity;
//
//import org.json.JSONException;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InOrder;
//
//import java.util.Collections;
//import java.util.concurrent.CountDownLatch;
//
//import static android.support.test.InstrumentationRegistry.getTargetContext;
//import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
//import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
//import static junit.framework.Assert.assertEquals;
//import static junit.framework.Assert.assertFalse;
//import static junit.framework.Assert.assertNotNull;
//import static junit.framework.Assert.assertNull;
//import static junit.framework.Assert.assertTrue;
//import static org.mockito.Matchers.any;
//import static org.mockito.Matchers.anyInt;
//import static org.mockito.Matchers.eq;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.inOrder;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@RunWith(AndroidJUnit4.class)
//public class PayPalTest {
//
//    @Rule
//    public final ActivityTestRule<TestActivity> mActivityTestRule =
//            new ActivityTestRule<>(TestActivity.class);
//
//    private Activity mActivity;
//
//    @Before
//    public void setUp() {
//        mActivity = mActivityTestRule.getActivity();
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void startPayPalService_stopsAndStartsService() throws JSONException {
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_offline_paypal.json"));
//        Context context = mock(Context.class);
//
//        PayPal.startPaypalService(context, configuration.getPayPal());
//
//        InOrder order = inOrder(context);
//        order.verify(context).stopService(any(Intent.class));
//        order.verify(context).startService(any(Intent.class));
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void authorizeAccount_startsPayPal() throws JSONException {
//        ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_offline_paypal.json"));
//        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
//        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
//
//        PayPal.authorizeAccount(fragment);
//
//        verify(fragment).startActivityForResult(launchIntentCaptor.capture(),
//                eq(PayPal.PAYPAL_AUTHORIZATION_REQUEST_CODE));
//        Intent intent = launchIntentCaptor.getValue();
//        assertEquals(PayPalProfileSharingActivity.class.getName(),
//                intent.getComponent().getClassName());
//        assertTrue(intent.hasExtra(PayPalTouchActivity.EXTRA_REQUESTED_SCOPES));
//        String paypalScopes = intent.getParcelableExtra(PayPalTouchActivity.EXTRA_REQUESTED_SCOPES)
//                .toString();
//        assertTrue(paypalScopes.contains("https://uri.paypal.com/services/payments/futurepayments"));
//        assertTrue(paypalScopes.contains("email"));
//        assertTrue(intent.hasExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void authorizeAccount_includesAdditionalScopes() throws JSONException {
//        ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_offline_paypal.json"));
//        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
//        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
//
//        PayPal.authorizeAccount(fragment, Collections.singletonList("address"));
//
//        verify(fragment).startActivityForResult(launchIntentCaptor.capture(),
//                eq(PayPal.PAYPAL_AUTHORIZATION_REQUEST_CODE));
//        String paypalScopes =
//                launchIntentCaptor.getValue().getParcelableExtra(PayPalTouchActivity.EXTRA_REQUESTED_SCOPES)
//                .toString();
//        assertTrue(paypalScopes.contains("https://uri.paypal.com/services/payments/futurepayments"));
//        assertTrue(paypalScopes.contains("email"));
//        assertTrue(paypalScopes.contains("address"));
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void authorizeAccount_sendsAnalyticsEvent() throws JSONException, InterruptedException {
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_offline_paypal.json"));
//        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
//        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
//
//        PayPal.authorizeAccount(fragment);
//
//        verify(fragment).sendAnalyticsEvent("paypal.selected");
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void buildPayPalConfiguration_buildsOfflinePayPalConfiguration() throws JSONException {
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_offline_paypal.json"));
//
//        PayPalConfiguration payPalConfiguration =
//                PayPal.buildPayPalConfiguration(configuration.getPayPal());
//
//        assertTrue(payPalConfiguration.toString().contains("environment:mock"));
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void buildPayPalConfiguration_buildsLivePayPalConfiguration() throws JSONException {
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_live_paypal.json"));
//
//        PayPalConfiguration payPalConfiguration =
//                PayPal.buildPayPalConfiguration(configuration.getPayPal());
//
//        assertTrue(payPalConfiguration.toString().contains("environment:live"));
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void buildPayPalConfiguration_buildsCustomPayPalConfiguration() throws JSONException {
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_custom_paypal.json"));
//
//        PayPalConfiguration payPalConfiguration =
//                PayPal.buildPayPalConfiguration(configuration.getPayPal());
//
//        assertTrue(payPalConfiguration.toString().contains("environment:custom"));
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void buildPayPalServiceIntent_buildsIntentWithCustomStageUrlAndSslVerificationOffForCustomEnvironment()
//            throws JSONException {
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_custom_paypal.json"));
//
//        Intent intent = PayPal.buildPayPalServiceIntent(getTargetContext(),
//                configuration.getPayPal());
//
//        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
//        assertEquals("https://braintree.paypal.com/v1/",
//                intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
//        assertFalse(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void buildPayPalServiceIntent_doesNotAddExtrasToIntentForOffline() throws JSONException {
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_offline_paypal.json"));
//
//        Intent intent = PayPal.buildPayPalServiceIntent(getTargetContext(),
//                configuration.getPayPal());
//
//        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
//        assertNull(intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
//        assertTrue(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void buildPayPalServiceIntent_doesNotAddExtrasToIntentForLive() throws JSONException {
//        Configuration configuration = Configuration.fromJson(
//                stringFromFixture("configuration_with_live_paypal.json"));
//
//        Intent intent = PayPal.buildPayPalServiceIntent(getTargetContext(),
//                configuration.getPayPal());
//
//        assertNotNull(intent.getParcelableExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION));
//        assertNull(intent.getStringExtra("com.paypal.android.sdk.baseEnvironmentUrl"));
//        assertTrue(intent.getBooleanExtra("com.paypal.android.sdk.enableStageSsl", true));
//    }
//
//    @Test(timeout = 1000)
//    @SmallTest
//    public void onActivityResult_postsConfigurationExceptionWhenResultExtrasInvalidResultCodeReturned()
//            throws InterruptedException {
//        final CountDownLatch latch = new CountDownLatch(1);
//        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
//        when(analyticsConfiguration.isEnabled()).thenReturn(true);
//        Configuration configuration = mock(Configuration.class);
//        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
//        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
//        fragment.addListener(new BraintreeErrorListener() {
//            @Override
//            public void onUnrecoverableError(Throwable throwable) {
//                assertEquals("PayPal result extras were invalid", throwable.getMessage());
//                latch.countDown();
//            }
//
//            @Override
//            public void onRecoverableError(ErrorWithResponse error) {
//            }
//        });
//
//        PayPal.onActivityResult(fragment, PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID,
//                new Intent());
//
//        latch.await();
//    }
//}
