package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.VisaCheckoutConfiguration;
import com.braintreepayments.api.models.VisaCheckoutNonce;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder;
import com.visa.checkout.VisaLibrary;
import com.visa.checkout.VisaMcomLibrary;
import com.visa.checkout.VisaMerchantInfo;
import com.visa.checkout.VisaMerchantInfo.AcceptedCardBrands;
import com.visa.checkout.VisaMerchantInfo.MerchantDataLevel;
import com.visa.checkout.VisaPaymentInfo;
import com.visa.checkout.VisaPaymentSummary;
import com.visa.checkout.VisaUserInfo;
import com.visa.checkout.utils.VisaEnvironmentConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "com.visa.*" })
@PrepareForTest({
        TokenizationClient.class, VisaMcomLibrary.class, VisaPaymentInfo.class, VisaCheckoutConfiguration.class })
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

        when(mBraintreeFragment.getActivity()).thenReturn(mock(Activity.class));
    }

    @Test
    public void createVisaCheckoutLibrary_throwsConfigurationExceptionWhenVisaCheckoutNotEnabled()
            throws JSONException {
        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .build();

        VisaCheckout.createVisaCheckoutLibrary(braintreeFragment);

        ArgumentCaptor<ConfigurationException> argumentCaptor = ArgumentCaptor.forClass(ConfigurationException.class);
        verify(braintreeFragment).postCallback(argumentCaptor.capture());

        ConfigurationException configurationException = argumentCaptor.getValue();
        assertEquals("Visa Checkout is not enabled.", configurationException.getMessage());
    }

    @Test
    public void createVisaCheckoutLibrary_whenProduction_usesProductionConfig() throws Exception {
        doNothingWhenAccessingVisaMcomLibrary();

        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .environment("production")
                        .visaCheckout(new TestVisaCheckoutConfigurationBuilder()
                                .apikey("gwApiKey")
                                .externalClientId("gwExternalClientId"))
                        .build())
                .build();

        when(braintreeFragment.getActivity()).thenReturn(mock(Activity.class));

        VisaCheckout.createVisaCheckoutLibrary(braintreeFragment);

        ArgumentCaptor<VisaEnvironmentConfig> argumentCaptor = ArgumentCaptor.forClass(VisaEnvironmentConfig.class);

        PowerMockito.verifyStatic();
        VisaMcomLibrary.getLibrary(any(Context.class), argumentCaptor.capture());

        assertEquals(VisaEnvironmentConfig.PRODUCTION, argumentCaptor.getValue());
    }

    @Test
    public void createVisaCheckoutLibrary_setsVisaEnvironmentConfig() throws Exception {
        doNothingWhenAccessingVisaMcomLibrary();

        when(mBraintreeFragment.getActivity()).thenReturn(mock(Activity.class));

        VisaCheckout.createVisaCheckoutLibrary(mBraintreeFragment);

        ArgumentCaptor<VisaEnvironmentConfig> argumentCaptor = ArgumentCaptor.forClass(VisaEnvironmentConfig.class);

        verify(mBraintreeFragment).postVisaCheckoutLibraryCallback(any(VisaMcomLibrary.class));

        PowerMockito.verifyStatic();
        VisaMcomLibrary.getLibrary(any(Context.class), argumentCaptor.capture());

        VisaEnvironmentConfig visaEnvironmentConfig = argumentCaptor.getValue();
        assertEquals(VisaEnvironmentConfig.SANDBOX, visaEnvironmentConfig);
        assertEquals("gwApikey", visaEnvironmentConfig.getMerchantApiKey());
        assertEquals(BraintreeRequestCodes.VISA_CHECKOUT, visaEnvironmentConfig.getVisaCheckoutRequestCode());
    }

    @Test
    public void createVisaCheckoutLibrary_whenVisaCheckoutSDKUnavailable_postsException () {
        stub(method(VisaCheckoutConfiguration.class, "isVisaCheckoutSDKAvailable")).toReturn(false);
        ArgumentCaptor<Exception> argumentCaptor = ArgumentCaptor.forClass(Exception.class);

        VisaCheckout.createVisaCheckoutLibrary(mBraintreeFragment);

        verify(mBraintreeFragment).postCallback(argumentCaptor.capture());
        assertEquals("Visa Checkout SDK is not available", argumentCaptor.getValue().getMessage());
    }

    @Test
    public void authorize_whenVisaMerchantInfo_setsBraintreePropertiesOnVisaMerchantInfo() throws Exception {
        VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
        visaPaymentInfo.setDescription("merchantDescription");
        VisaUserInfo visaUserInfo = new VisaUserInfo();
        visaUserInfo.setFirstName("visaUserInfoFirstName");
        visaPaymentInfo.setVisaUserInfo(visaUserInfo);
        List<AcceptedCardBrands> acceptedCardBrands = Arrays.asList(
                AcceptedCardBrands.ELECTRON
        );

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> requestCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        doNothing().when(mBraintreeFragment).startActivityForResult(intentCaptor.capture(),
                requestCodeCaptor.capture());

        VisaCheckout.authorize(mBraintreeFragment, new VisaPaymentInfo());

        VisaPaymentInfo actualVisaPaymentInfo = VisaCheckoutResultActivity.sVisaPaymentInfo;
        VisaEnvironmentConfig visaEnvironmentConfig = VisaCheckoutResultActivity.sVisaEnvironmentConfig;

        assertEquals(VisaEnvironmentConfig.SANDBOX, visaEnvironmentConfig);
        assertEquals("gwApikey", visaEnvironmentConfig.getMerchantApiKey());
        assertEquals(VisaCheckoutResultActivity.class.getName(),
                intentCaptor.getValue().getComponent().getClassName());
        assertEquals(BraintreeRequestCodes.VISA_CHECKOUT, requestCodeCaptor.getValue().intValue());
        assertEquals("gwApikey", actualVisaPaymentInfo.getVisaMerchantInfo().getMerchantApiKey());
        assertEquals("gwExternalClientId", actualVisaPaymentInfo.getExternalClientId());
        assertEquals("merchantDescription", actualVisaPaymentInfo.getDescription());
        assertEquals("visaUserInfoFirstName", actualVisaPaymentInfo.getVisaUserInfo().getFirstName());
        assertTrue(actualVisaPaymentInfo.getVisaMerchantInfo().getAcceptedCardBrands().containsAll(acceptedCardBrands));
    }

    @Test
    public void authorize_whenMerchantFilledOutVisaPaymentInfo_doesNotOverwriteApiKey() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> requestCodeCaptor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(mBraintreeFragment).startActivityForResult(intentCaptor.capture(),
                requestCodeCaptor.capture());

        VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
        VisaMerchantInfo visaMerchantInfo = new VisaMerchantInfo();
        visaMerchantInfo.setMerchantApiKey("merchantSetApiKey");
        visaPaymentInfo.setVisaMerchantInfo(visaMerchantInfo);

        VisaCheckout.authorize(mBraintreeFragment, visaPaymentInfo);

        assertEquals("merchantSetApiKey", VisaCheckoutResultActivity.sVisaPaymentInfo.getVisaMerchantInfo()
                .getMerchantApiKey());
    }

    @Test
    public void authorize_whenMerchantFilledOutVisaPaymentInfo_doesNotOverwriteExternalClientId() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> requestCodeCaptor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(mBraintreeFragment).startActivityForResult(intentCaptor.capture(),
                requestCodeCaptor.capture());

        VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
        visaPaymentInfo.setExternalClientId("merchantSetExternalClientId");

        VisaCheckout.authorize(mBraintreeFragment, visaPaymentInfo);

        assertEquals("merchantSetExternalClientId", VisaCheckoutResultActivity.sVisaPaymentInfo.getExternalClientId());
    }

    @Test
    public void authorize_whenMerchantFilledOutVisaPaymentInfo_doesOverwriteDataLevel() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> requestCodeCaptor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(mBraintreeFragment).startActivityForResult(intentCaptor.capture(),
                requestCodeCaptor.capture());

        VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
        visaPaymentInfo.setVisaMerchantInfo(new VisaMerchantInfo());
        visaPaymentInfo.getVisaMerchantInfo().setDataLevel(MerchantDataLevel.SUMMARY);

        VisaCheckout.authorize(mBraintreeFragment, visaPaymentInfo);

        assertEquals(MerchantDataLevel.FULL, VisaCheckoutResultActivity.sVisaPaymentInfo.getVisaMerchantInfo()
                .getDataLevel());
    }

    @Test
    public void authorize_whenMerchantFilledOutVisaPaymentInfo_doesNotOverwriteCardBrands() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> requestCodeCaptor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(mBraintreeFragment).startActivityForResult(intentCaptor.capture(),
                requestCodeCaptor.capture());

        VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
        VisaMerchantInfo visaMerchantInfo = new VisaMerchantInfo();
        visaMerchantInfo.setAcceptedCardBrands(Arrays.asList(AcceptedCardBrands.ELO));

        visaPaymentInfo.setVisaMerchantInfo(visaMerchantInfo);

        VisaCheckout.authorize(mBraintreeFragment, visaPaymentInfo);

        List<AcceptedCardBrands> acceptedCardBrands = VisaCheckoutResultActivity.sVisaPaymentInfo.getVisaMerchantInfo()
                .getAcceptedCardBrands();
        assertEquals(1, acceptedCardBrands.size());
        assertEquals(AcceptedCardBrands.ELO, acceptedCardBrands.get(0));
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
    public void onActivityResult_whenComingBackSuccessfully_callsTokenize() throws JSONException {
        mockStatic(TokenizationClient.class);

        VisaPaymentSummary visaPaymentSummary = sampleVisaPaymentSummary();

        Intent data = new Intent();
        data.putExtra(VisaLibrary.PAYMENT_SUMMARY, visaPaymentSummary);
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
        data.putExtra(VisaLibrary.PAYMENT_SUMMARY, visaPaymentSummary);
        VisaCheckout.onActivityResult(mBraintreeFragment, Activity.RESULT_OK, data);

        verifyStatic();
        TokenizationClient.tokenize(eq(mBraintreeFragment), any(PaymentMethodBuilder.class),
                any(PaymentMethodNonceCallback.class));

        verify(mBraintreeFragment).sendAnalyticsEvent("visacheckout.success");
    }

    @Test
    public void onActivityResult_whenCancelled_sendAnalyticsEvent() {
        VisaCheckout.onActivityResult(mBraintreeFragment, Activity.RESULT_CANCELED, null);
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("visacheckout.canceled"));
    }

    @Test
    public void onActivityResult_whenFailure_postsException() {
        VisaCheckout.onActivityResult(mBraintreeFragment, -100, null);

        ArgumentCaptor exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(mBraintreeFragment).postCallback((Exception) exceptionCaptor.capture());

        assertEquals("Visa Checkout responded with an invalid resultCode: -100",
                ((BraintreeException)exceptionCaptor.getValue()).getMessage());
    }

    @Test
    public void onActivityResult_whenFailure_sendAnalyticsevent() {
        VisaCheckout.onActivityResult(mBraintreeFragment, -100, null);
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("visacheckout.failed"));
    }

    private void doNothingWhenAccessingVisaMcomLibrary() throws Exception {
        spy(VisaMcomLibrary.class);
        doAnswer(new Answer<VisaMcomLibrary>() {
            @Override
            public VisaMcomLibrary answer(InvocationOnMock invocation) throws Throwable {
                return mock(VisaMcomLibrary.class);
            }
        }).when(VisaMcomLibrary.class, "getLibrary", any(Activity.class), any(VisaEnvironmentConfig.class));

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
