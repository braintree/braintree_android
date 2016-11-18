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
import com.visa.checkout.VisaLibrary;
import com.visa.checkout.VisaMcomLibrary;
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

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "com.visa.*" })
@PrepareForTest({ VisaMcomLibrary.class, VisaPaymentInfo.class })
public class VisaCheckoutUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Configuration mConfigurationWithVisaCheckout;
    private BraintreeFragment mBraintreeFragment;

    @Before
    public void setup() throws JSONException {
        JSONObject configurationJSON = new JSONObject(stringFromFixture("configuration.json"));
        JSONObject visaConfiguration = new JSONObject(stringFromFixture("configuration/with_visaCheckout.json"));
        configurationJSON.put("visaCheckout", visaConfiguration.get("visaCheckout"));
        mConfigurationWithVisaCheckout = Configuration.fromJson(configurationJSON.toString());

        mBraintreeFragment = new MockFragmentBuilder()
                .configuration(mConfigurationWithVisaCheckout)
                .build();

        Activity activity = mock(Activity.class);
        when(mBraintreeFragment.getActivity()).thenReturn(activity);
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
    public void createVisaCheckoutLibrary_whenProduction_usesProductionConfig () throws Exception {
        spy(VisaMcomLibrary.class);
        doAnswer(new Answer<VisaMcomLibrary>() {
            @Override
            public VisaMcomLibrary answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(VisaMcomLibrary.class, "getLibrary", any(Activity.class), any(VisaEnvironmentConfig.class));

        JSONObject configurationJSON = new JSONObject(stringFromFixture("configuration.json"));
        JSONObject visaConfiguration = new JSONObject(stringFromFixture("configuration/with_visaCheckout.json"));
        configurationJSON.put("visaCheckout", visaConfiguration.get("visaCheckout"));
        configurationJSON.put("environment", "production");
        Configuration configuration = Configuration.fromJson(configurationJSON.toString());

        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Activity activity = mock(Activity.class);
        when(braintreeFragment.getActivity()).thenReturn(activity);

        VisaCheckout.createVisaCheckoutLibrary(braintreeFragment);

        ArgumentCaptor<VisaEnvironmentConfig> argumentCaptor = ArgumentCaptor.forClass(VisaEnvironmentConfig.class);

        PowerMockito.verifyStatic();
        VisaMcomLibrary.getLibrary(any(Context.class), argumentCaptor.capture());

        VisaEnvironmentConfig visaEnvironmentConfig = argumentCaptor.getValue();
        assertEquals(VisaEnvironmentConfig.PRODUCTION, visaEnvironmentConfig);
    }

    @Test
    public void createVisaCheckoutLibrary_setsVisaEnvironmentConfig()
            throws Exception {
        spy(VisaMcomLibrary.class);
        doAnswer(new Answer<VisaMcomLibrary>() {
            @Override
            public VisaMcomLibrary answer(InvocationOnMock invocation) throws Throwable {
                return mock(VisaMcomLibrary.class);
            }
        }).when(VisaMcomLibrary.class, "getLibrary", any(Activity.class), any(VisaEnvironmentConfig.class));

        JSONObject configurationJSON = new JSONObject(stringFromFixture("configuration.json"));
        JSONObject visaConfiguration = new JSONObject(stringFromFixture("configuration/with_visaCheckout.json"));
        configurationJSON.put("visaCheckout", visaConfiguration.get("visaCheckout"));
        Configuration configuration = Configuration.fromJson(configurationJSON.toString());

        BraintreeFragment braintreeFragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        Activity activity = mock(Activity.class);
        when(braintreeFragment.getActivity()).thenReturn(activity);

        VisaCheckout.createVisaCheckoutLibrary(braintreeFragment);

        ArgumentCaptor<VisaEnvironmentConfig> argumentCaptor = ArgumentCaptor.forClass(VisaEnvironmentConfig.class);

        verify(braintreeFragment).postVisaCheckoutLibraryCallback(any(VisaMcomLibrary.class));

        PowerMockito.verifyStatic();
        VisaMcomLibrary.getLibrary(any(Context.class), argumentCaptor.capture());

        VisaEnvironmentConfig visaEnvironmentConfig = argumentCaptor.getValue();
        assertEquals(VisaEnvironmentConfig.SANDBOX, visaEnvironmentConfig);
        assertEquals("gwApikey", visaEnvironmentConfig.getMerchantApiKey());
        assertEquals(BraintreeRequestCodes.VISA_CHECKOUT, visaEnvironmentConfig.getVisaCheckoutRequestCode());
    }

    @Test
    public void createVisaCheckoutLibrary_setsStaticEnvironmentConfigOnBraintreeVisaCheckoutResultActivity()
            throws Exception {
        spy(VisaMcomLibrary.class);
        doAnswer(new Answer<VisaMcomLibrary>() {
            @Override
            public VisaMcomLibrary answer(InvocationOnMock invocation) throws Throwable {
                return mock(VisaMcomLibrary.class);
            }
        }).when(VisaMcomLibrary.class, "getLibrary", any(Activity.class), any(VisaEnvironmentConfig.class));

        VisaCheckout.createVisaCheckoutLibrary(mBraintreeFragment);

        VisaEnvironmentConfig visaEnvironmentConfig = BraintreeVisaCheckoutResultActivity.sVisaEnvironmentConfig;
        assertEquals(VisaEnvironmentConfig.SANDBOX, visaEnvironmentConfig);
        assertEquals("gwApikey", visaEnvironmentConfig.getMerchantApiKey());
    }

    @Test
    public void authorize_whenVisaMerchantInfo_setsBraintreePropertiesOnVisaMerchantInfo() throws Exception {
        VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
        visaPaymentInfo.setDescription("merchantDescription");
        VisaUserInfo visaUserInfo = new VisaUserInfo();
        visaUserInfo.setFirstName("visaUserInfoFirstName");
        visaPaymentInfo.setVisaUserInfo(visaUserInfo);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<Integer> requestCodeCaptor = ArgumentCaptor.forClass(Integer.class);

        PowerMockito.doNothing().when(mBraintreeFragment).startActivityForResult(
                intentCaptor.capture(),
                requestCodeCaptor.capture()
        );

        VisaCheckout.authorize(mBraintreeFragment, null, visaPaymentInfo);

        VisaPaymentInfo actual = BraintreeVisaCheckoutResultActivity.sVisaPaymentInfo;

        assertEquals(BraintreeVisaCheckoutResultActivity.class.getName(),
                intentCaptor.getValue().getComponent().getClassName());
        assertEquals(BraintreeRequestCodes.VISA_CHECKOUT, requestCodeCaptor.getValue().intValue());
        assertEquals("gwApikey", actual.getVisaMerchantInfo().getMerchantApiKey());
        assertEquals("gwExternalClientId", actual.getExternalClientId());
        assertEquals("merchantDescription", actual.getDescription());
        assertEquals("visaUserInfoFirstName", actual.getVisaUserInfo().getFirstName());
    }

    @Test
    public void tokenize_whenSuccessful_postsVisaPaymentMethodNonce() {
        fail("Not implemented");
    }

    @Test
    public void tokenize_whenFailure_postsException() {
        fail("Not implemented");
    }

    @Test
    @PrepareForTest({ TokenizationClient.class })
    public void onActivityResult_whenComingBackSuccessfully_callsTokenize() throws JSONException {
        mockStatic(TokenizationClient.class);

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

        VisaPaymentSummary visaPaymentSummary = VisaPaymentSummary.CREATOR.createFromParcel(in);

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
    public void onActivityResult_whenCancelled_callsCancelCallback() {
        VisaCheckout.onActivityResult(mBraintreeFragment, Activity.RESULT_CANCELED, null);
        verify(mBraintreeFragment).postCancelCallback(eq(BraintreeRequestCodes.VISA_CHECKOUT));
    }

    @Test
    public void onActivityResult_whenFailure_postsException() {
        VisaCheckout.onActivityResult(mBraintreeFragment, -100, null);

        ArgumentCaptor exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(mBraintreeFragment).postCallback((Exception) exceptionCaptor.capture());

        assertEquals("Visa Checkout responded with resultCode=-100", ((BraintreeException)exceptionCaptor.getValue())
                .getMessage());
    }
}
