package com.braintreepayments.api;

import com.visa.checkout.Profile;
import com.visa.checkout.Profile.CardBrand;
import com.visa.checkout.Profile.ProfileBuilder;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*", "com.visa.checkout.Profile", "com.visa.checkout.Profile.ProfileBuilder"})
@PrepareForTest({ VisaPaymentSummary.class, VisaCheckoutSdk.class })
// TODO: Investigate Robolectric / PowerMock Combination test failures
@Ignore("These tests are failing because of VisaCheckout classes marked 'final'. Using Braintree wrapped types for Visa lib may help here in the future.")
public class VisaCheckoutClientUnitTest {

    @Rule
    public PowerMockRule powerMockRule = new PowerMockRule();

    private Configuration configurationWithVisaCheckout;
    private VisaPaymentSummary visaPaymentSummary;

    @Before
    public void setup() throws Exception {
        configurationWithVisaCheckout = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT);

        visaPaymentSummary = PowerMockito.mock(VisaPaymentSummary.class);
        when(visaPaymentSummary.getCallId()).thenReturn("stubbedCallId");
        when(visaPaymentSummary.getEncKey()).thenReturn("stubbedEncKey");
        when(visaPaymentSummary.getEncPaymentData()).thenReturn("stubbedEncPaymentData");
        PowerMockito.whenNew(VisaPaymentSummary.class).withAnyArguments().thenReturn(visaPaymentSummary);
    }

    @Test
    public void createProfileBuilder_whenNotEnabled_throwsConfigurationException() {
        APIClient apiClient = new MockAPIClientBuilder().build();

        Configuration configuration = TestConfigurationBuilder.basicConfig();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, apiClient);

        VisaCheckoutCreateProfileBuilderCallback listener = mock(VisaCheckoutCreateProfileBuilderCallback.class);
        sut.createProfileBuilder(listener);

        ArgumentCaptor<ConfigurationException> captor = ArgumentCaptor.forClass(ConfigurationException.class);
        verify(listener, times(1)).onResult((ProfileBuilder) isNull(), captor.capture());

        ConfigurationException configurationException = captor.getValue();
        assertEquals("Visa Checkout is not enabled.", configurationException.getMessage());
    }

    @Test
    public void createProfileBuilder_whenProduction_usesProductionConfig() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        APIClient apiClient = new MockAPIClientBuilder().build();
        String configString = new TestConfigurationBuilder()
                .environment("production")
                .visaCheckout(new TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder()
                        .apikey("gwApiKey")
                        .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                        .externalClientId("gwExternalClientId"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, apiClient);

        sut.createProfileBuilder(new VisaCheckoutCreateProfileBuilderCallback() {
            @Override
            public void onResult(ProfileBuilder profileBuilder, Exception error) {
                List<String> expectedCardBrands = Arrays.asList(CardBrand.VISA, CardBrand.MASTERCARD);
                Profile profile = profileBuilder.build();
                assertNotNull(profile);
                lock.countDown();
            }
        });

        lock.await();
    }

    @Test
    public void createProfileBuilder_whenNotProduction_usesSandboxConfig() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        APIClient apiClient = new MockAPIClientBuilder().build();
        String configString = new TestConfigurationBuilder()
                .environment("environment")
                .visaCheckout(new TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder()
                        .apikey("gwApiKey")
                        .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                        .externalClientId("gwExternalClientId"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(configString))
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, apiClient);

        sut.createProfileBuilder(new VisaCheckoutCreateProfileBuilderCallback() {
            @Override
            public void onResult(ProfileBuilder profileBuilder, Exception error) {
                List<String> expectedCardBrands = Arrays.asList(CardBrand.VISA, CardBrand.MASTERCARD);
                Profile profile = profileBuilder.build();
                assertNotNull(profile);
                lock.countDown();
            }
        });

        lock.await();
    }

    @Test
    public void tokenize_whenSuccessful_postsVisaPaymentMethodNonce() throws JSONException {
        APIClient apiClient = new MockAPIClientBuilder()
                .tokenizeRESTSuccess(new JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configurationWithVisaCheckout)
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, apiClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(visaPaymentSummary, listener);

        verify(listener).onResult(any(VisaCheckoutNonce.class), (Exception) isNull());
    }

    @Test
    public void tokenize_whenSuccessful_sendsAnalyticEvent() throws JSONException {
        APIClient apiClient = new MockAPIClientBuilder()
                .tokenizeRESTSuccess(new JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configurationWithVisaCheckout)
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, apiClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(visaPaymentSummary, listener);

        verify(braintreeClient).sendAnalyticsEvent("visacheckout.tokenize.succeeded");
    }

    @Test
    public void tokenize_whenFailure_postsException() {
        Exception tokenizeError = new Exception("Mock Failure");
        APIClient apiClient = new MockAPIClientBuilder()
                .tokenizeRESTError(tokenizeError)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configurationWithVisaCheckout)
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, apiClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(visaPaymentSummary, listener);

        verify(listener).onResult(null, tokenizeError);
    }

    @Test
    public void tokenize_whenFailure_sendsAnalyticEvent() {
        Exception tokenizeError = new Exception("Mock Failure");
        APIClient apiClient = new MockAPIClientBuilder()
                .tokenizeRESTError(tokenizeError)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configurationWithVisaCheckout)
                .build();
        VisaCheckoutClient sut = new VisaCheckoutClient(braintreeClient, apiClient);

        VisaCheckoutTokenizeCallback listener = mock(VisaCheckoutTokenizeCallback.class);
        sut.tokenize(visaPaymentSummary, listener);

        verify(braintreeClient).sendAnalyticsEvent("visacheckout.tokenize.failed");
    }
}
