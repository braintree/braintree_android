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

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.test.TestActivity;

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
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.internal.SignatureVerificationTestUtils.disableSignatureVerification;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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
        Configuration configuration = mock(Configuration.class);
        when(configuration.getVenmoState()).thenReturn("offline");
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
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
        when(fragment.getContext()).thenReturn(context);
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
        Configuration configuration = mock(Configuration.class);
        when(configuration.getVenmoState()).thenReturn("off");
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);

        Venmo.authorize(fragment);

        verify(fragment).sendAnalyticsEvent("add-venmo.start");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void performAppSwitch_sendsAnalyticsEventWhenUnavailable() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getVenmoState()).thenReturn("off");
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);

        Venmo.authorize(fragment);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("add-venmo.start");
        order.verify(fragment).sendAnalyticsEvent("add-venmo.unavailable");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_postsPaymentMethodOnSuccess() throws InterruptedException {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.success(stringFromFixture("payment_methods/get_payment_method_card_response.json"));
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        fragment.addListener(new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("123456-12345-12345-a-adfa", paymentMethod.getNonce());
                assertEquals("11", ((Card) paymentMethod).getLastTwo());
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
    public void onActivityResult_sendsAnalyticsEventOnSuccess() {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.success(stringFromFixture("payment_methods/get_payment_method_card_response.json"));
            }
        });
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        verify(fragment).sendAnalyticsEvent("venmo-app.success");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_postsExceptionToListenerWhenNoNonceIsPresent()
            throws InterruptedException {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        final CountDownLatch latch = new CountDownLatch(1);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertEquals("No nonce present in response from Venmo app", throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onRecoverableError(ErrorWithResponse error) {
            }
        });

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, new Intent());

        latch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_postsExceptionToListener() throws InterruptedException {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.failure(new Exception("Nonce not found"));
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertEquals("Nonce not found", throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onRecoverableError(ErrorWithResponse error) {

            }
        });
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        latch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_sendsAnalyticsEventMissingNonce() {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, new Intent());

        verify(fragment).sendAnalyticsEvent("venmo-app.fail.missing-nonce");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_sendsAnalyticsEventOnFailure() {
        BraintreeFragment fragment = getMockFragment(mActivity, mock(Configuration.class));
        when(fragment.getHttpClient()).thenReturn(new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.failure(new Exception());
            }
        });
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "nonce");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        verify(fragment).sendAnalyticsEvent("venmo-app.fail");
    }
}
