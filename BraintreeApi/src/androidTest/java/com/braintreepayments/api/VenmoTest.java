package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.TokenizationKey;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.CardNumber;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.tokenize;
import static com.braintreepayments.api.internal.SignatureVerificationTestUtils.disableSignatureVerification;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class VenmoTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void packageIsCorrect() {
        assertEquals("com.venmo", Venmo.PACKAGE_NAME);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void appSwitchActivityIsCorrect() {
        assertEquals("CardChooserActivity", Venmo.APP_SWITCH_ACTIVITY);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void certificateSubjectIsCorrect() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US",
                Venmo.CERTIFICATE_SUBJECT);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void certificateIssuerIsCorrect() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US",
                Venmo.CERTIFICATE_ISSUER);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void publicKeyHashCodeIsCorrect() {
        assertEquals(-129711843, Venmo.PUBLIC_KEY_HASH_CODE);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getLaunchIntent_returnsCorrectIntent() throws JSONException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getVenmoState()).thenReturn("offline");
        when(configuration.getMerchantId()).thenReturn("merchant_id");

        Intent intent = Venmo.getLaunchIntent(configuration);

        assertEquals(new ComponentName("com.venmo", "com.venmo.CardChooserActivity"),
                intent.getComponent());
        assertEquals("merchant_id", intent.getStringExtra(Venmo.EXTRA_MERCHANT_ID));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getLaunchIntent_includesVenmoEnvironment() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getVenmoState()).thenReturn("offline");

        Intent intent = Venmo.getLaunchIntent(configuration);

        assertTrue(intent.getBooleanExtra(Venmo.EXTRA_OFFLINE, false));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getLaunchIntent_includesLiveVenmoEnvironment() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getVenmoState()).thenReturn("live");

        Intent intent = Venmo.getLaunchIntent(configuration);

        assertFalse(intent.getBooleanExtra(Venmo.EXTRA_OFFLINE, true));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void isAvailable_returnsFalseWhenVenmoIsOff() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getVenmoState()).thenReturn("off");

        assertFalse(Venmo.isAvailable(getTargetContext(), configuration));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void isAvailable_returnsFalseWhenVenmoNotInstalled() throws JSONException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getVenmoState()).thenReturn("live");

        assertFalse(Venmo.isAvailable(getTargetContext(), configuration));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void performAppSwitch_appSwitchesWithVenmoLaunchIntent() {
        ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
        Configuration configuration = getConfiguration();
        when(configuration.getVenmoState()).thenReturn("offline");
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        doNothing().when(fragment).sendAnalyticsEvent(anyString());
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "com.venmo";
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(Collections.singletonList(resolveInfo));
        Context context = mock(Context.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(fragment.getApplicationContext()).thenReturn(context);
        disableSignatureVerification();

        Venmo.authorize(fragment);

        verify(fragment).startActivityForResult(launchIntentCaptor.capture(),
                eq(Venmo.VENMO_REQUEST_CODE));
        Intent launchIntent = launchIntentCaptor.getValue();
        assertEquals("com.venmo/com.venmo.CardChooserActivity",
                launchIntent.getComponent().flattenToString());
    }

    @Test(timeout = 1000)
    @MediumTest
    public void performAppSwitch_sendsAnalyticsEvent() {
        Configuration configuration = getConfiguration();
        when(configuration.getVenmoState()).thenReturn("off");
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        when(fragment.getHttpClient()).thenReturn(mock(BraintreeHttpClient.class));

        Venmo.authorize(fragment);

        verify(fragment).sendAnalyticsEvent("venmo.selected");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void performAppSwitch_sendsAnalyticsEventWhenStarted() {
        Configuration configuration = getConfiguration();
        when(configuration.getVenmoState()).thenReturn("offline");
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        doNothing().when(fragment).sendAnalyticsEvent(anyString());
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "com.venmo";
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(Collections.singletonList(resolveInfo));
        Context context = mock(Context.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(fragment.getApplicationContext()).thenReturn(context);
        disableSignatureVerification();

        Venmo.authorize(fragment);

        verify(fragment).sendAnalyticsEvent("venmo.app-switch.started");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void performAppSwitch_sendsAnalyticsEventWhenUnavailable() {
        Configuration configuration = getConfiguration();
        when(configuration.getVenmoState()).thenReturn("off");
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        when(fragment.getHttpClient()).thenReturn(mock(BraintreeHttpClient.class));

        Venmo.authorize(fragment);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("venmo.selected");
        order.verify(fragment).sendAnalyticsEvent("venmo.app-switch.failed");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_postsPaymentMethodNonceOnSuccess()
            throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());
        when(fragment.getHttpClient()).thenReturn(
                new BraintreeHttpClient(TokenizationKey.fromString(TOKENIZATION_KEY)) {
                    @Override
                    public void get(String path, HttpResponseCallback callback) {
                        callback.success(stringFromFixture(
                                "payment_methods/get_payment_method_card_response.json"));
                    }
                });
        final CountDownLatch latch = new CountDownLatch(1);
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertEquals("123456-12345-12345-a-adfa", paymentMethodNonce.getNonce());
                assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
                latch.countDown();
            }
        });
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        latch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_sendsAnalyticsEventOnSuccess() throws InvalidArgumentException {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());
        when(fragment.getHttpClient()).thenReturn(
                new BraintreeHttpClient(TokenizationKey.fromString(TOKENIZATION_KEY)) {
                    @Override
                    public void get(String path, HttpResponseCallback callback) {
                        callback.success(
                                stringFromFixture(
                                        "payment_methods/get_payment_method_card_response.json"));
                    }
                });
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("venmo.app-switch.authorized");
        order.verify(fragment).sendAnalyticsEvent("venmo.app-switch.nonce-received");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_postsExceptionToListenerWhenNoNonceIsPresent()
            throws InterruptedException {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());
        final CountDownLatch latch = new CountDownLatch(1);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("No nonce present in response from Venmo app", error.getMessage());
                latch.countDown();
            }
        });

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, new Intent());

        latch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_postsExceptionToListener()
            throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient(
                TokenizationKey.fromString(TOKENIZATION_KEY)) {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.failure(new Exception("Nonce not found"));
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Nonce not found", error.getMessage());
                latch.countDown();
            }
        });
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        latch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());

        Venmo.onActivityResult(fragment, Activity.RESULT_CANCELED, new Intent());

        verify(fragment).sendAnalyticsEvent("venmo.app-switch.canceled");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_sendsAnalyticsEventMissingNonce() {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, new Intent());

        verify(fragment).sendAnalyticsEvent("venmo.app-switch.failed");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_sendsAnalyticsEventOnFailure() throws InvalidArgumentException {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());
        when(fragment.getHttpClient()).thenReturn(
                new BraintreeHttpClient(TokenizationKey.fromString(TOKENIZATION_KEY)) {
                    @Override
                    public void get(String path, HttpResponseCallback callback) {
                        callback.failure(new Exception());
                    }
                });
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "nonce");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("venmo.app-switch.authorized");
        order.verify(fragment).sendAnalyticsEvent("venmo.app-switch.failed");
    }

    @Test(timeout = 10000)
    @MediumTest
    public void onActivityResult_failsWhenUsingTokenizationKey() throws InterruptedException {
        BraintreeFragment fragment = getFragment(mActivity, TOKENIZATION_KEY);
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(CardNumber.VISA)
                .expirationDate("08/19");
        CardNonce cardNonce = tokenize(fragment, cardBuilder);
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, cardNonce.getNonce());
        final CountDownLatch latch = new CountDownLatch(1);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof AuthorizationException);
                assertEquals("Client key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                        error.getMessage());
                latch.countDown();
            }
        });

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        latch.await();
    }

    private Configuration getConfiguration() {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);

        return configuration;
    }
}
