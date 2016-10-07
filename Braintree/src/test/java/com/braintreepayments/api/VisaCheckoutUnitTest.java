package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.Configuration;
import com.visa.checkout.VisaMcomLibrary;
import com.visa.checkout.VisaMerchantInfo.MerchantDataLevel;
import com.visa.checkout.VisaPaymentInfo;
import com.visa.checkout.utils.VisaEnvironmentConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricGradleTestRunner.class)
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
        assertEquals(VisaCheckout.VISA_CHECKOUT_REQUEST_CODE, visaEnvironmentConfig.getVisaCheckoutRequestCode());
    }

    @Test
    public void authorize_whenVisaMerchantInfo_setsBraintreePropertiesOnVisaMerchantInfo() {
        VisaMcomLibrary visaMcomLibrary = PowerMockito.mock(VisaMcomLibrary.class);
        ArgumentCaptor<VisaPaymentInfo> argumentCaptor = ArgumentCaptor.forClass(VisaPaymentInfo.class);
        PowerMockito.doNothing().when(visaMcomLibrary).checkoutWithPayment(
                argumentCaptor.capture(),
                Mockito.any(Integer.class)
        );

        VisaCheckout.authorize(mBraintreeFragment, visaMcomLibrary, new VisaPaymentInfo());

        VisaPaymentInfo visaPaymentInfo = argumentCaptor.getValue();

        assertEquals("gwExternalClientId", visaPaymentInfo.getExternalClientId());
        assertEquals("gwApikey", visaPaymentInfo.getVisaMerchantInfo().getMerchantApiKey());
        assertEquals(MerchantDataLevel.FULL, visaPaymentInfo.getVisaMerchantInfo().getDataLevel());
    }
}
