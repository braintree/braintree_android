package com.braintreepayments.api;

import android.content.Context;

import org.json.JSONException;
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
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
public class PreferredPaymentMethodsTest {

    private CountDownLatch mCountDownLatch;
    private Context context;
    private Context applicationContext;

    private Configuration graphQLEnabledConfiguration;
    private Configuration graphQLDisabledConfiguration;

    private DeviceInspector deviceInspector;

    @Before
    public void setUp() throws JSONException {
        mCountDownLatch = new CountDownLatch(1);

        context = mock(Context.class);
        applicationContext = mock(Context.class);
        deviceInspector = mock(DeviceInspector.class);

        graphQLEnabledConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
        graphQLDisabledConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        when(context.getApplicationContext()).thenReturn(applicationContext);
    }

    @Test
    public void fetchPreferredPaymentMethods_whenPayPalAppIsInstalled_callsListenerWithTrue() throws Exception {
        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfiguration)
                .build();

        when(deviceInspector.isPayPalInstalled(applicationContext)).thenReturn(true);

        PreferredPaymentMethods sut = new PreferredPaymentMethods(braintreeClient, deviceInspector);
        sut.fetchPreferredPaymentMethods(context, new PreferredPaymentMethodsCallback() {
            @Override
            public void onResult(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertTrue(preferredPaymentMethodsResult.isPayPalPreferred());
                verify(braintreeClient).sendAnalyticsEvent("preferred-payment-methods.paypal.app-installed.true");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenVenmoAppIsInstalled_callsListenerWithTrue() throws Exception {
        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfiguration)
                .build();

        when(deviceInspector.isVenmoInstalled(applicationContext)).thenReturn(true);

        PreferredPaymentMethods sut = new PreferredPaymentMethods(braintreeClient, deviceInspector);
        sut.fetchPreferredPaymentMethods(context, new PreferredPaymentMethodsCallback() {
            @Override
            public void onResult(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertTrue(preferredPaymentMethodsResult.isVenmoPreferred());
                verify(braintreeClient).sendAnalyticsEvent("preferred-payment-methods.venmo.app-installed.true");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenVenmoAppIsNotInstalled_callsListenerWithFalseForVenmo() throws InterruptedException {
        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfiguration)
                .build();

        PreferredPaymentMethods sut = new PreferredPaymentMethods(braintreeClient, deviceInspector);
        sut.fetchPreferredPaymentMethods(context, new PreferredPaymentMethodsCallback() {
            @Override
            public void onResult(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isVenmoPreferred());
                verify(braintreeClient).sendAnalyticsEvent("preferred-payment-methods.venmo.app-installed.false");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_sendsQueryToGraphQL() throws InterruptedException {
        String response = "{\"data\": {\"preferredPaymentMethods\": {\"paypal\": true}}}";
        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfiguration)
                .sendGraphQLPOSTSuccessfulResponse(response)
                .build();

        PreferredPaymentMethods sut = new PreferredPaymentMethods(braintreeClient, deviceInspector);
        sut.fetchPreferredPaymentMethods(context, new PreferredPaymentMethodsCallback() {
            @Override
            public void onResult(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

                verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));
                String expectedQuery = "{ \"query\": \"query PreferredPaymentMethods { preferredPaymentMethods { paypalPreferred } }\" }";
                assertEquals(expectedQuery, captor.getValue());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenGraphQLIsNotEnabled_andPayPalAppNotInstalled_callsListenerWithFalseForPayPal() throws InterruptedException {
        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfiguration)
                .build();

        PreferredPaymentMethods sut = new PreferredPaymentMethods(braintreeClient, deviceInspector);
        sut.fetchPreferredPaymentMethods(context, new PreferredPaymentMethodsCallback() {
            @Override
            public void onResult(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred());
                assertFalse(preferredPaymentMethodsResult.isVenmoPreferred());
                verify(braintreeClient).sendAnalyticsEvent("preferred-payment-methods.api-disabled");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenApiDetectsPayPalPreferred_callsListenerWithTrueForPayPal() throws InterruptedException {
        String response = "{\"data\": {\"preferredPaymentMethods\": {\"paypalPreferred\": true}}}";
        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfiguration)
                .sendGraphQLPOSTSuccessfulResponse(response)
                .build();

        PreferredPaymentMethods sut = new PreferredPaymentMethods(braintreeClient, deviceInspector);
        sut.fetchPreferredPaymentMethods(context, new PreferredPaymentMethodsCallback() {
            @Override
            public void onResult(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertTrue(preferredPaymentMethodsResult.isPayPalPreferred());
                verify(braintreeClient).sendAnalyticsEvent("preferred-payment-methods.paypal.api-detected.true");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenApiDetectsPayPalNotPreferred_callsListenerWithFalseForPayPal() throws InterruptedException {
        String response = "{\"data\": {\"preferredPaymentMethods\": {\"paypalPreferred\": false}}}";
        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfiguration)
                .sendGraphQLPOSTSuccessfulResponse(response)
                .build();

        PreferredPaymentMethods sut = new PreferredPaymentMethods(braintreeClient, deviceInspector);
        sut.fetchPreferredPaymentMethods(context, new PreferredPaymentMethodsCallback() {
            @Override
            public void onResult(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred());
                verify(braintreeClient).sendAnalyticsEvent("preferred-payment-methods.paypal.api-detected.false");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test
    public void fetchPreferredPaymentMethods_whenGraphQLReturnsError_callsListenerWithFalseForPayPal() throws InterruptedException {
        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfiguration)
                .sendGraphQLPOSTErrorResponse(new Exception())
                .build();

        PreferredPaymentMethods sut = new PreferredPaymentMethods(braintreeClient, deviceInspector);
        sut.fetchPreferredPaymentMethods(context, new PreferredPaymentMethodsCallback() {
            @Override
            public void onResult(PreferredPaymentMethodsResult preferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred());
                verify(braintreeClient).sendAnalyticsEvent("preferred-payment-methods.api-error");
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }
}