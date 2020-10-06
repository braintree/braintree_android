package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PreferredPaymentMethodsListener;
import com.braintreepayments.api.internal.SignatureVerificationUnitTestUtils;
import com.braintreepayments.api.models.PreferredPaymentMethodsResult;
import com.braintreepayments.api.test.VenmoInstalledContextFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
public class PreferredPaymentMethodsTest {

    private CountDownLatch mCountDownLatch;

    private Context mockContext = mock(Context.class);
    private PackageManager mockPackageManager = mock(PackageManager.class);

    @Before
    public void setUp() throws Exception {
        mCountDownLatch = new CountDownLatch(1);

        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);

        // by default, simulate PayPal and Venmo apps not installed
        when(mockPackageManager.getApplicationInfo("com.paypal.android.p2pmobile", 0))
                .thenThrow(new PackageManager.NameNotFoundException());
        when(mockPackageManager.getApplicationInfo("com.venmo", 0))
                .thenThrow(new PackageManager.NameNotFoundException());
    }

    @Test
    public void fetchPreferredPaymentMethods_whenPayPalAppIsInstalled_callsListenerWithTrue() throws Exception {
        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .context(mockContext)
                .build();

        // simulate PayPal app installed
        reset(mockPackageManager);
        when(mockPackageManager.getApplicationInfo("com.paypal.android.p2pmobile", 0)).thenReturn(new ApplicationInfo());

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertTrue(preferredPaymentMethodsResult.isPayPalPreferred());
                verify(mockFragment).sendAnalyticsEvent("preferred-payment-methods.paypal.app-installed.true");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenVenmoAppIsInstalled_callsListenerWithTrue() throws Exception {
        SignatureVerificationUnitTestUtils.disableSignatureVerification();

        // simulate Venmo app installed
        mockContext = VenmoInstalledContextFactory.venmoInstalledContext(true);

        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .context(mockContext)
                .build();

        when(mockFragment.getGraphQLHttpClient()).thenReturn(null);

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertTrue(preferredPaymentMethodsResult.isVenmoPreferred());
                verify(mockFragment).sendAnalyticsEvent("preferred-payment-methods.venmo.app-installed.true");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenVenmoAppIsNotInstalled_callsListenerWithFalseForVenmo() throws InterruptedException {
        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .context(mockContext)
                .build();

        when(mockFragment.getGraphQLHttpClient()).thenReturn(null);

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isVenmoPreferred());
                verify(mockFragment).sendAnalyticsEvent("preferred-payment-methods.venmo.app-installed.false");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_sendsQueryToGraphQL() throws InterruptedException {
        String response = "{\"data\": {\"preferredPaymentMethods\": {\"paypal\": true}}}";
        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .graphQLSuccessResponse(response)
                .context(mockContext)
                .build();

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                verify(mockFragment.getGraphQLHttpClient()).post(captor.capture(), any(HttpResponseCallback.class));
                String expectedQuery = "{ \"query\": \"query PreferredPaymentMethods { preferredPaymentMethods { paypalPreferred } }\" }";
                assertEquals(expectedQuery, captor.getValue());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenGraphQLIsNotEnabled_andPayPalAppNotInstalled_callsListenerWithFalseForPayPal() throws InterruptedException {
        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .context(mockContext)
                .build();

        when(mockFragment.getGraphQLHttpClient()).thenReturn(null);

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred());
                assertFalse(preferredPaymentMethodsResult.isVenmoPreferred());
                verify(mockFragment).sendAnalyticsEvent("preferred-payment-methods.api-disabled");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenApiDetectsPayPalPreferred_callsListenerWithTrueForPayPal() throws InterruptedException {
        String response = "{\"data\": {\"preferredPaymentMethods\": {\"paypalPreferred\": true}}}";
        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .graphQLSuccessResponse(response)
                .context(mockContext)
                .build();

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertTrue(preferredPaymentMethodsResult.isPayPalPreferred());
                verify(mockFragment).sendAnalyticsEvent("preferred-payment-methods.paypal.api-detected.true");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenApiDetectsPayPalNotPreferred_callsListenerWithFalseForPayPal() throws InterruptedException {
        String response = "{\"data\": {\"preferredPaymentMethods\": {\"paypalPreferred\": false}}}";
        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .graphQLSuccessResponse(response)
                .context(mockContext)
                .build();

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred());
                verify(mockFragment).sendAnalyticsEvent("preferred-payment-methods.paypal.api-detected.false");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenGraphQLReturnsError_callsListenerWithFalseForPayPal() throws InterruptedException {
        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .graphQLErrorResponse(new Exception())
                .context(mockContext)
                .build();

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred());
                verify(mockFragment).sendAnalyticsEvent("preferred-payment-methods.api-error");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }
}