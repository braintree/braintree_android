package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;

import lib.android.paypal.com.magnessdk.Environment;
import lib.android.paypal.com.magnessdk.InvalidInputException;
import lib.android.paypal.com.magnessdk.MagnesResult;
import lib.android.paypal.com.magnessdk.MagnesSDK;
import lib.android.paypal.com.magnessdk.MagnesSettings;
import lib.android.paypal.com.magnessdk.MagnesSource;

@RunWith(RobolectricTestRunner.class)
public class PayPalDataCollectorUnitTest {

    private Context context;
    private String sampleInstallationGUID;
    private Configuration configuration;

    private MagnesSDK magnesSDK;
    private UUIDHelper uuidHelper;
    private BraintreeClient braintreeClient;

    @Before
    public void beforeEach() throws JSONException {
        magnesSDK = mock(MagnesSDK.class);
        uuidHelper = mock(UUIDHelper.class);
        context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(mock(Context.class));
        configuration = mock(Configuration.class);
        when(configuration.getEnvironment()).thenReturn("sandbox");

        // this uuid has no actual meaning; magnes requires a valid guid for tests
        sampleInstallationGUID = "0665203b-16e4-4ce2-be98-d7d73ec32e8a";

        braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN))
                .build();
    }

    @Test
    public void getPayPalInstallationGUID_returnsInstallationIdentifier() {

        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesSDK, uuidHelper);

        assertEquals(sampleInstallationGUID, sut.getPayPalInstallationGUID(context));
    }

    @Test
    public void getClientMetaDataId_returnsEmptyStringWhenContextIsNull() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesSDK, uuidHelper);

        String result = sut.getClientMetadataId(null, configuration);
        assertEquals("", result);
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithDefaultSettings() throws InvalidInputException {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        when(configuration.getEnvironment()).thenReturn("production");

        when(magnesSDK.collectAndSubmit(ArgumentMatchers.any(Context.class), (String) ArgumentMatchers.isNull(), ArgumentMatchers.<HashMap<String, String>>isNull()))
                .thenReturn(mock(MagnesResult.class));

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesSDK, uuidHelper);
        sut.getClientMetadataId(context, configuration);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        Assert.assertEquals(MagnesSource.BRAINTREE.getVersion(), magnesSettings.getMagnesSource());
        assertFalse(magnesSettings.isDisableBeacon());
        Assert.assertEquals(Environment.LIVE, magnesSettings.getEnvironment());
        Assert.assertEquals(sampleInstallationGUID, magnesSettings.getAppGuid());
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithPayPalDataCollectorRequest() throws InvalidInputException {
        when(configuration.getEnvironment()).thenReturn("production");
        String applicationGUID = "07ea6967-1e4a-4aaa-942d-7f13db372b75";

        HashMap<String, String> additionalData = new HashMap<>();
        PayPalDataCollectorRequest payPalDataCollectorRequest = new PayPalDataCollectorRequest()
                .setApplicationGuid(applicationGUID)
                .setClientMetadataId("client-metadata-id")
                .setAdditionalData(additionalData)
                .setDisableBeacon(false);

        when(magnesSDK.collectAndSubmit(ArgumentMatchers.any(Context.class), ArgumentMatchers.eq("client-metadata-id"), ArgumentMatchers.same(additionalData)))
                .thenReturn(mock(MagnesResult.class));

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesSDK, uuidHelper);
        sut.getClientMetadataId(context, payPalDataCollectorRequest, configuration);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        Assert.assertEquals(MagnesSource.BRAINTREE.getVersion(), magnesSettings.getMagnesSource());
        assertFalse(magnesSettings.isDisableBeacon());
        Assert.assertEquals(Environment.LIVE, magnesSettings.getEnvironment());
        Assert.assertEquals(applicationGUID, magnesSettings.getAppGuid());
    }

    @Test
    public void getClientMetadataId_forwardsClientMetadataIdFromMagnesResult() throws InvalidInputException {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        MagnesResult magnesResult = mock(MagnesResult.class);
        when(magnesResult.getPaypalClientMetaDataId()).thenReturn("paypal-clientmetadata-id");

        when(magnesSDK.collectAndSubmit(ArgumentMatchers.any(Context.class), (String) ArgumentMatchers.isNull(), ArgumentMatchers.<HashMap<String, String>>isNull()))
                .thenReturn(magnesResult);

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesSDK, uuidHelper);
        String result = sut.getClientMetadataId(context, configuration);

        assertEquals("paypal-clientmetadata-id", result);
    }

    @Test
    public void getClientMetadataId_returnsEmptyStringWhenMagnesInputInvalid() throws InvalidInputException {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        when(magnesSDK.collectAndSubmit(ArgumentMatchers.any(Context.class), (String) ArgumentMatchers.isNull(), ArgumentMatchers.<HashMap<String, String>>isNull()))
                .thenThrow(new InvalidInputException("invalid input"));

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesSDK, uuidHelper);
        String result = sut.getClientMetadataId(context, configuration);

        assertEquals("", result);
    }

    @Test
    public void getClientMetadataId_whenConfigurationEnvironmentSandbox_setsMagnesEnvironmentSandbox() throws InvalidInputException {
        when(configuration.getEnvironment()).thenReturn("sandbox");
        String applicationGUID = "07ea6967-1e4a-4aaa-942d-7f13db372b75";

        HashMap<String, String> additionalData = new HashMap<>();
        PayPalDataCollectorRequest payPalDataCollectorRequest = new PayPalDataCollectorRequest()
                .setApplicationGuid(applicationGUID)
                .setClientMetadataId("client-metadata-id")
                .setAdditionalData(additionalData)
                .setDisableBeacon(false);

        when(magnesSDK.collectAndSubmit(ArgumentMatchers.any(Context.class), ArgumentMatchers.eq("client-metadata-id"), ArgumentMatchers.same(additionalData)))
                .thenReturn(mock(MagnesResult.class));

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesSDK, uuidHelper);
        sut.getClientMetadataId(context, payPalDataCollectorRequest, configuration);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        Assert.assertEquals(Environment.SANDBOX, magnesSettings.getEnvironment());
    }

    @Test
    public void getClientMetadataId_whenConfigurationEnvironmentNotSandbox_setsMagnesEnvironmentLive() throws InvalidInputException {
        when(configuration.getEnvironment()).thenReturn("production");
        String applicationGUID = "07ea6967-1e4a-4aaa-942d-7f13db372b75";

        HashMap<String, String> additionalData = new HashMap<>();
        PayPalDataCollectorRequest payPalDataCollectorRequest = new PayPalDataCollectorRequest()
                .setApplicationGuid(applicationGUID)
                .setClientMetadataId("client-metadata-id")
                .setAdditionalData(additionalData)
                .setDisableBeacon(false);

        when(magnesSDK.collectAndSubmit(ArgumentMatchers.any(Context.class), ArgumentMatchers.eq("client-metadata-id"), ArgumentMatchers.same(additionalData)))
                .thenReturn(mock(MagnesResult.class));

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesSDK, uuidHelper);
        sut.getClientMetadataId(context, payPalDataCollectorRequest, configuration);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        Assert.assertEquals(Environment.LIVE, magnesSettings.getEnvironment());
    }

    @Test
    public void collectDeviceData_forwardsConfigurationFetchErrors() {
        Exception configError = new Exception("configuration error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesSDK, uuidHelper);

        PayPalDataCollectorCallback callback = mock(PayPalDataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        verify(callback).onResult(null, configError);
    }
}
