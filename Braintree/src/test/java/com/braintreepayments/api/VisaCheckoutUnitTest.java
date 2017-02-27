package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.VisaCheckoutConfiguration;
import com.braintreepayments.api.models.VisaCheckoutNonce;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder;
import com.visa.checkout.Environment;
import com.visa.checkout.Profile;
import com.visa.checkout.Profile.CardBrand;
import com.visa.checkout.Profile.DataLevel;
import com.visa.checkout.Profile.ProfileBuilder;
import com.visa.checkout.PurchaseInfo.PurchaseInfoBuilder;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.models.BraintreeRequestCodes.VISA_CHECKOUT;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "com.visa.*" })
@PrepareForTest({ TokenizationClient.class, VisaCheckoutConfiguration.class })
public class VisaCheckoutUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Configuration mConfigurationWithVisaCheckout;
    private BraintreeFragment mBraintreeFragment;

    @Before
    public void setup() throws JSONException {
        JSONObject visaConfiguration = new JSONObject(stringFromFixture("configuration/with_visa_checkout.json"));
        mConfigurationWithVisaCheckout = Configuration.fromJson(visaConfiguration.toString());

        mBraintreeFragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithVisaCheckout)
                .build();

        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                throw new RuntimeException(error);
            }
        });

        when(mBraintreeFragment.getActivity()).thenReturn(mock(Activity.class));
    }

    @Test
    public void createProfileBuilder_whenNotEnabled_throwsConfigurationException()
            throws JSONException {
        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .build();

        VisaCheckout.createProfileBuilder(braintreeFragment, null);

        ArgumentCaptor<ConfigurationException> argumentCaptor = ArgumentCaptor.forClass(ConfigurationException.class);
        verify(braintreeFragment, times(1)).postCallback(argumentCaptor.capture());

        ConfigurationException configurationException = argumentCaptor.getValue();
        assertEquals("Visa Checkout is not enabled.", configurationException.getMessage());
    }

    @Test
    public void createProfileBuilder_whenProduction_usesProductionConfig() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);
        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .environment("production")
                        .visaCheckout(new TestVisaCheckoutConfigurationBuilder()
                                .apikey("gwApiKey")
                                .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                                .externalClientId("gwExternalClientId"))
                        .build())
                .build();

        VisaCheckout.createProfileBuilder(braintreeFragment, new BraintreeResponseListener<ProfileBuilder>() {
            @Override
            public void onResponse(ProfileBuilder profileBuilder) {
                List<String> expectedCardBrands = Arrays.asList(new String[] { CardBrand.VISA, CardBrand.MASTERCARD });
                Profile profile = profileBuilder.build();
                assertEquals(Environment.PRODUCTION, profile.getEnvironment());
                assertEquals("gwApiKey", profile.getApiKey());
                assertEquals("gwExternalClientId", profile.getExternalClientId());
                assertEquals(DataLevel.FULL, profile.getDataLevel());
                assertTrue(expectedCardBrands.containsAll(Arrays.asList(profile.getAcceptedCardBrands())));

                lock.countDown();
            }
        });

        lock.await();
    }

    @Test
    public void createProfileBuilder_whenNotProduction_usesSandboxConfig() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);
        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .environment("environment")
                        .visaCheckout(new TestVisaCheckoutConfigurationBuilder()
                                .apikey("gwApiKey")
                                .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                                .externalClientId("gwExternalClientId"))
                        .build())
                .build();

        VisaCheckout.createProfileBuilder(braintreeFragment, new BraintreeResponseListener<ProfileBuilder>() {
            @Override
            public void onResponse(ProfileBuilder profileBuilder) {
                List<String> expectedCardBrands = Arrays.asList(new String[] { CardBrand.VISA, CardBrand.MASTERCARD });
                Profile profile = profileBuilder.build();
                assertEquals(Environment.SANDBOX, profile.getEnvironment());
                assertEquals("gwApiKey", profile.getApiKey());
                assertEquals("gwExternalClientId", profile.getExternalClientId());
                assertEquals(DataLevel.FULL, profile.getDataLevel());
                assertTrue(expectedCardBrands.containsAll(Arrays.asList(profile.getAcceptedCardBrands())));

                lock.countDown();
            }
        });

        lock.await();
    }

    @Test(timeout = 10000)
    public void authorize_startsVisaCheckoutActivity() throws InterruptedException {
        doNothing().when(mBraintreeFragment).startActivityForResult(any(Intent.class), anyInt());

        VisaCheckout.authorize(mBraintreeFragment, new PurchaseInfoBuilder(new BigDecimal("1.00"), "test"));

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(mBraintreeFragment).startActivityForResult(intentCaptor.capture(),
                eq(VISA_CHECKOUT));

        assertEquals("com.visa.checkout.VisaActivity", intentCaptor.getValue().getComponent().getClassName());
    }

    @Test
    public void tokenize_whenSuccessful_postsVisaPaymentMethodNonce() throws Exception {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PaymentMethodNonceCallback paymentMethodNonceCallback = (PaymentMethodNonceCallback)invocation
                        .getArguments()[2];

                paymentMethodNonceCallback.success(VisaCheckoutNonce.fromJson(
                        stringFromFixture("payment_methods/visa_checkout_response.json")));
                return null;
            }
        }).when(TokenizationClient.class, "tokenize", any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
        ArgumentCaptor paymentMethodNonceCaptor = ArgumentCaptor.forClass(VisaCheckoutNonce.class);

        VisaCheckout.tokenize(mBraintreeFragment, sampleVisaPaymentSummary());

        verify(mBraintreeFragment).postCallback((VisaCheckoutNonce) paymentMethodNonceCaptor.capture());
        assertEquals("123456-12345-12345-a-adfa", ((VisaCheckoutNonce) paymentMethodNonceCaptor.getValue())
                .getNonce());
    }

    @Test
    public void tokenize_whenSuccessful_sendsAnalyticEvent() throws Exception {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PaymentMethodNonceCallback paymentMethodNonceCallback = (PaymentMethodNonceCallback)invocation
                        .getArguments()[2];

                paymentMethodNonceCallback.success(VisaCheckoutNonce.fromJson(
                        stringFromFixture("payment_methods/visa_checkout_response.json")));
                return null;
            }
        }).when(TokenizationClient.class, "tokenize", any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
        ArgumentCaptor paymentMethodNonceCaptor = ArgumentCaptor.forClass(VisaCheckoutNonce.class);

        VisaCheckout.tokenize(mBraintreeFragment, sampleVisaPaymentSummary());

        verify(mBraintreeFragment).postCallback((VisaCheckoutNonce) paymentMethodNonceCaptor.capture());
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("visacheckout.nonce-recieved"));
    }

    @Test
    public void tokenize_whenFailure_postsException() throws Exception {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PaymentMethodNonceCallback paymentMethodNonceCallback = (PaymentMethodNonceCallback)invocation
                        .getArguments()[2];

                paymentMethodNonceCallback.failure(new Exception("Mock Failure"));
                return null;
            }
        }).when(TokenizationClient.class, "tokenize", any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
        ArgumentCaptor exceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        VisaCheckout.tokenize(mBraintreeFragment, sampleVisaPaymentSummary());

        verify(mBraintreeFragment).postCallback((Exception) exceptionCaptor.capture());
        assertEquals("Mock Failure", ((Exception)exceptionCaptor.getValue()).getMessage());
    }

    @Test
    public void tokenize_whenFailure_sendsAnalyticEvent() throws Exception {
        mockStatic(TokenizationClient.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PaymentMethodNonceCallback paymentMethodNonceCallback = (PaymentMethodNonceCallback)invocation
                        .getArguments()[2];

                paymentMethodNonceCallback.failure(new Exception("Mock Failure"));
                return null;
            }
        }).when(TokenizationClient.class, "tokenize", any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));
        ArgumentCaptor exceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        VisaCheckout.tokenize(mBraintreeFragment, sampleVisaPaymentSummary());

        verify(mBraintreeFragment).postCallback((Exception) exceptionCaptor.capture());
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("visacheckout.nonce-failed"));
    }

    @Test
    public void onActivityResult_onVisaCheckout_whenCanceled_doesNotPostCancel() {
        VisaCheckout.onActivityResult(mBraintreeFragment, Activity.RESULT_CANCELED, new Intent());

        verify(mBraintreeFragment, times(0)).postCancelCallback(VISA_CHECKOUT);
    }

    @Test
    public void onActivityResult_whenOk_callsTokenize() throws JSONException {
        mockStatic(TokenizationClient.class);

        VisaPaymentSummary visaPaymentSummary = sampleVisaPaymentSummary();

        Intent data = new Intent();
        data.putExtra(VisaCheckoutSdk.INTENT_PAYMENT_SUMMARY, visaPaymentSummary);
        VisaCheckout.onActivityResult(mBraintreeFragment, Activity.RESULT_OK, data);

        ArgumentCaptor<PaymentMethodBuilder> paymentMethodBuilderArgumentCaptor = ArgumentCaptor.forClass(
                PaymentMethodBuilder.class);

        verifyStatic();
        TokenizationClient.tokenize(eq(mBraintreeFragment), paymentMethodBuilderArgumentCaptor.capture(),
                any(PaymentMethodNonceCallback.class));

        JSONObject visaCheckoutCard = new JSONObject(paymentMethodBuilderArgumentCaptor.getValue().build())
                .getJSONObject("visaCheckoutCard");

        assertEquals("stubbedEncPaymentData", visaCheckoutCard.getString("encryptedPaymentData"));
        assertEquals("stubbedEncKey", visaCheckoutCard.getString("encryptedKey"));
        assertEquals("stubbedCallId", visaCheckoutCard.getString("callId"));
    }

    @Test
    public void onActivityResult_whenOk_sendAnalyticsEvent() {
        mockStatic(TokenizationClient.class);

        VisaPaymentSummary visaPaymentSummary = sampleVisaPaymentSummary();

        Intent data = new Intent();
        data.putExtra(VisaCheckoutSdk.INTENT_PAYMENT_SUMMARY, visaPaymentSummary);
        VisaCheckout.onActivityResult(mBraintreeFragment, Activity.RESULT_OK, data);

        verifyStatic();
        TokenizationClient.tokenize(eq(mBraintreeFragment), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));

        verify(mBraintreeFragment).sendAnalyticsEvent("visacheckout.success");
    }

    @Test
    public void onActivityResult_whenCanceled_sendAnalyticsEvent() {
        VisaCheckout.onActivityResult(mBraintreeFragment, Activity.RESULT_CANCELED, null);
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("visacheckout.canceled"));
    }

    @Test
    public void onActivityResult_whenFailed_postsException() {
        VisaCheckout.onActivityResult(mBraintreeFragment, -100, null);

        ArgumentCaptor exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(mBraintreeFragment).postCallback((Exception) exceptionCaptor.capture());

        assertEquals("Visa Checkout responded with an invalid resultCode: -100",
                ((BraintreeException)exceptionCaptor.getValue()).getMessage());
    }

    @Test
    public void onActivityResult_whenFailed_sendAnalyticsEvent() {
        VisaCheckout.onActivityResult(mBraintreeFragment, -100, null);
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("visacheckout.failed"));
    }

    private VisaPaymentSummary sampleVisaPaymentSummary() {
        Parcel in = Parcel.obtain();
        in.writeLong(1);
        in.writeString("US");
        in.writeString("90210");
        in.writeString("1234");
        in.writeString("VISA");
        in.writeString("Credit");
        in.writeString("stubbedEncPaymentData");
        in.writeString("stubbedEncKey");
        in.writeString("stubbedCallId");
        in.setDataPosition(0);

        return VisaPaymentSummary.CREATOR.createFromParcel(in);
    }
}
