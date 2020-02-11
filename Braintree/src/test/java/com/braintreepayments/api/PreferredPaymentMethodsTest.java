package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PreferredPaymentMethodsListener;
import com.braintreepayments.api.models.PreferredPaymentMethodsResult;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

public class PreferredPaymentMethodsTest {

    private CountDownLatch mCountDownLatch;

    private Context mockContext = mock(Context.class);
    private PackageManager mockPackageManager = mock(PackageManager.class);

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        mCountDownLatch = new CountDownLatch(1);

        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);

        // by default, simulate PayPal app not installed
        when(mockPackageManager.getApplicationInfo("com.paypal.android.p2pmobile", 0))
                .thenThrow(new PackageManager.NameNotFoundException());
    }

    @Test
    public void fetchPreferredPaymentMethods_whenPayPalAppInstalledOnDevice_callsListenerWithTrue() throws Exception {
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
    public void fetchPreferredPaymentMethods_sendsQueryToGraphQL() throws InterruptedException {
        String response = "{\"data\": {\"clientConfiguration\": {\"paypal\": {\"preferredPaymentMethod\": true}}}}";
        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .graphQLSuccessResponse(response)
                .context(mockContext)
                .build();

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                verify(mockFragment.getGraphQLHttpClient()).post(captor.capture(), any(HttpResponseCallback.class));
                String expectedQuery = "{ \"query\": \"query ClientConfiguration { clientConfiguration { paypal { preferredPaymentMethod } } }\" }";
                assertEquals(expectedQuery, captor.getValue());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenGraphQLIsNotEnabled_callsListenerWithFalse() throws InterruptedException {
        final BraintreeFragment mockFragment = new MockFragmentBuilder()
                .context(mockContext)
                .build();

        when(mockFragment.getGraphQLHttpClient()).thenReturn(null);

        PreferredPaymentMethods.fetchPreferredPaymentMethods(mockFragment, new PreferredPaymentMethodsListener() {
            @Override
            public void onPreferredPaymentMethodsFetched(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred());
                verify(mockFragment).sendAnalyticsEvent("preferred-payment-methods.api-disabled");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenPayPalIsPreferred_callsListenerWithTrue() throws InterruptedException {
        String response = "{\"data\": {\"clientConfiguration\": {\"paypal\": {\"preferredPaymentMethod\": true}}}}";
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
    public void fetchPreferredPaymentMethods_whenPayPalIsNotPreferred_callsListenerWithFalse() throws InterruptedException {
        String response = "{\"data\": {\"clientConfiguration\": {\"paypal\": {\"preferredPaymentMethod\": false}}}}";
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
    public void fetchPreferredPaymentMethods_whenGraphQLReturnsError_callsListenerWithFalse() throws InterruptedException {
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