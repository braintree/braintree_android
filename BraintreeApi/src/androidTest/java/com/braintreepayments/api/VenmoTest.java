package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.models.VenmoConfiguration;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.BraintreeActivityTestRule;
import com.braintreepayments.testutils.MockContextForVenmo;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.internal.SignatureVerificationTestUtils.disableSignatureVerification;
import static junit.framework.Assert.assertEquals;
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
@SmallTest
public class VenmoTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test(timeout = 1000)
    public void packageIsCorrect() {
        assertEquals("com.venmo", Venmo.PACKAGE_NAME);
    }

    @Test(timeout = 1000)
    public void appSwitchActivityIsCorrect() {
        assertEquals("controller.SetupMerchantActivity", Venmo.APP_SWITCH_ACTIVITY);
    }

    @Test(timeout = 1000)
    public void certificateSubjectIsCorrect() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US",
                Venmo.CERTIFICATE_SUBJECT);
    }

    @Test(timeout = 1000)
    public void certificateIssuerIsCorrect() {
        assertEquals("CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US",
                Venmo.CERTIFICATE_ISSUER);
    }

    @Test(timeout = 1000)
    public void publicKeyHashCodeIsCorrect() {
        assertEquals(-129711843, Venmo.PUBLIC_KEY_HASH_CODE);
    }

    @Test(timeout = 1000)
    public void containsCorrectVenmoExtras() throws JSONException {
        Configuration configuration = mock(Configuration.class);
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        when(configuration.getEnvironment()).thenReturn("environment");

        Intent intent = Venmo.getLaunchIntent(configuration);

        assertEquals(new ComponentName("com.venmo", "com.venmo.controller.SetupMerchantActivity"),
                intent.getComponent());
        assertEquals("merchant_id", intent.getStringExtra(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", intent.getStringExtra(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals(BuildConfig.VERSION_NAME, intent.getStringExtra(Venmo.EXTRA_SDK_VERSION));
        assertEquals("environment", intent.getStringExtra(Venmo.EXTRA_ENVIRONMENT));
    }

    @Test(timeout = 1000)
    public void authorizeAccount_failsAndSendsExceptionWhenControlPanelNotEnabled()
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Configuration configuration = getConfiguration();
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Venmo is not enabled on the control panel.", error.getMessage());
                latch.countDown();
            }
        });
        Context mockContextForVenmo = new MockContextForVenmo()
                .venmoInstalled()
                .whitelistValue("true")
                .build();
        when(fragment.getApplicationContext()).thenReturn(mockContextForVenmo);
        when(venmoConfiguration.isVenmoWhitelisted(any(ContentResolver.class))).thenReturn(true);
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(configuration.getEnvironment()).thenReturn("environment");
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        doNothing().when(fragment).sendAnalyticsEvent(anyString());
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        disableSignatureVerification();

        Venmo.authorizeAccount(fragment, configuration);

        latch.await();
    }

    @Test(timeout = 1000)
    public void authorizeAccount_failsAndSendsExceptionWhenNotInstalled()
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Configuration configuration = getConfiguration();
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Venmo is not installed.", error.getMessage());
                latch.countDown();
            }
        });
        Context mockContextForVenmo = new MockContextForVenmo()
                .whitelistValue("true")
                .build();
        when(fragment.getApplicationContext()).thenReturn(mockContextForVenmo);
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        when(venmoConfiguration.isVenmoWhitelisted(any(ContentResolver.class))).thenReturn(true);
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(configuration.getEnvironment()).thenReturn("environment");
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        doNothing().when(fragment).sendAnalyticsEvent(anyString());
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        disableSignatureVerification();

        Venmo.authorizeAccount(fragment, configuration);

        latch.await();
    }

    @Test(timeout = 1000)
    public void authorizeAccount_failsAndSendsExceptionWhenNotWhitelisted()
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Configuration configuration = getConfiguration();
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertEquals("Venmo is not whitelisted.", error.getMessage());
                latch.countDown();
            }
        });
        Context mockContextForVenmo = new MockContextForVenmo()
                .venmoInstalled()
                .build();
        when(fragment.getApplicationContext()).thenReturn(mockContextForVenmo);
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(configuration.getEnvironment()).thenReturn("environment");
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        doNothing().when(fragment).sendAnalyticsEvent(anyString());
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        disableSignatureVerification();

        Venmo.authorizeAccount(fragment, configuration);

        latch.await();
    }

    @Test(timeout = 1000)
    public void performAppSwitch_appSwitchesWithVenmoLaunchIntent() {
        ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
        Configuration configuration = getConfiguration();
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);
        Context mockContextForVenmo = new MockContextForVenmo()
                .venmoInstalled()
                .whitelistValue("true")
                .build();
        when(fragment.getApplicationContext()).thenReturn(mockContextForVenmo);
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        when(venmoConfiguration.isVenmoWhitelisted(any(ContentResolver.class))).thenReturn(true);
        when(configuration.getMerchantId()).thenReturn("merchant_id");
        when(configuration.getEnvironment()).thenReturn("environment");
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        doNothing().when(fragment).sendAnalyticsEvent(anyString());
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        disableSignatureVerification();

        Venmo.authorizeAccount(fragment, configuration);

        verify(fragment).startActivityForResult(launchIntentCaptor.capture(),
                eq(Venmo.VENMO_REQUEST_CODE));
        Intent launchIntent = launchIntentCaptor.getValue();
        assertEquals("com.venmo/com.venmo.controller.SetupMerchantActivity",
                launchIntent.getComponent().flattenToString());
        Bundle extras = launchIntent.getExtras();
        assertEquals("merchant_id", extras.getString(Venmo.EXTRA_MERCHANT_ID));
        assertEquals("access-token", extras.getString(Venmo.EXTRA_ACCESS_TOKEN));
        assertEquals(BuildConfig.VERSION_NAME, extras.getString(Venmo.EXTRA_SDK_VERSION));
        assertEquals("environment", extras.getString(Venmo.EXTRA_ENVIRONMENT));
    }

    @Test(timeout = 1000)
    public void performAppSwitch_sendsAnalyticsEvent() {
        Configuration configuration = getConfiguration();
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);

        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        when(fragment.getHttpClient()).thenReturn(mock(BraintreeHttpClient.class));
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());

        Venmo.authorizeAccount(fragment);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
    }

    @Test(timeout = 1000)
    public void performAppSwitch_sendsAnalyticsEventWhenStarted() {
        Configuration configuration = getConfiguration();
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        MockContext mockContext = new MockContextForVenmo()
                .whitelistValue("true")
                .venmoInstalled()
                .build();
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);

        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        when(venmoConfiguration.isVenmoWhitelisted(any(ContentResolver.class))).thenReturn(true);
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        doNothing().when(fragment).sendAnalyticsEvent(anyString());
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        when(fragment.getApplicationContext()).thenReturn(mockContext);
        disableSignatureVerification();

        Venmo.authorizeAccount(fragment, configuration);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
        verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.started");
    }

    @Test(timeout = 1000)
    public void performAppSwitch_sendsAnalyticsEventWhenUnavailableAndPostException() {
        ArgumentCaptor<AppSwitchNotAvailableException> argumentCaptor =
                ArgumentCaptor.forClass(AppSwitchNotAvailableException.class);
        Configuration configuration = getConfiguration();
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        BraintreeFragment fragment = getMockFragment(mActivity, configuration);

        when(venmoConfiguration.isAccessTokenValid()).thenReturn(true);
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);

        Venmo.authorizeAccount(fragment, configuration);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("pay-with-venmo.selected");
        order.verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.failed");
        verify(fragment).postCallback(argumentCaptor.capture());
        assertEquals("Venmo is not installed.",
                argumentCaptor.getValue().getMessage());
    }

    @Test(timeout = 1000)
    public void onActivityResult_postsPaymentMethodNonceAndUsernameOnSuccess()
            throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());
        when(fragment.getHttpClient()).thenReturn(mock(BraintreeHttpClient.class));
        final CountDownLatch latch = new CountDownLatch(1);
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertTrue(paymentMethodNonce instanceof VenmoAccountNonce);
                VenmoAccountNonce venmoAccountNonce = (VenmoAccountNonce) paymentMethodNonce;
                assertEquals("123456-12345-12345-a-adfa", venmoAccountNonce.getNonce());
                assertEquals("username", venmoAccountNonce.getDescription());
                assertEquals("username", venmoAccountNonce.getUsername());
                latch.countDown();
            }
        });
        Intent intent = new Intent()
                .putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "123456-12345-12345-a-adfa")
                .putExtra(Venmo.EXTRA_USERNAME, "username");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        latch.await();
    }

    @Test(timeout = 1000)
    public void onActivityResult_sendsAnalyticsEventOnSuccess() throws InvalidArgumentException {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());
        when(fragment.getHttpClient()).thenReturn(mock(BraintreeHttpClient.class));
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa").putExtra(Venmo.EXTRA_USERNAME, "username");

        Venmo.onActivityResult(fragment, Activity.RESULT_OK, intent);

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.success");
    }

    @Test(timeout = 1000)
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        BraintreeFragment fragment = getMockFragment(mActivity, getConfiguration());
        when(fragment.getHttpClient()).thenReturn(mock(BraintreeHttpClient.class));

        Venmo.onActivityResult(fragment, Activity.RESULT_CANCELED, new Intent());

        verify(fragment).sendAnalyticsEvent("pay-with-venmo.app-switch.canceled");
    }

    @Test(timeout = 2000)
    public void isVenmoInstalled_returnsTrueWhenInstalled() {
        MockContext mockContext = new MockContextForVenmo()
                .whitelistValue("true")
                .venmoInstalled()
                .build();
        VenmoConfiguration venmoConfiguration = mock(VenmoConfiguration.class);
        when(venmoConfiguration.getAccessToken()).thenReturn("access-token");
        Configuration configuration = getConfiguration();
        when(configuration.getPayWithVenmo()).thenReturn(venmoConfiguration);
        BraintreeFragment braintreeFragment = getMockFragment(mActivity, configuration);
        when(braintreeFragment.getApplicationContext()).thenReturn(mockContext);

        disableSignatureVerification();

        assertTrue(Venmo.isVenmoInstalled(mockContext));
    }

    private Configuration getConfiguration() {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);

        return configuration;
    }
}
