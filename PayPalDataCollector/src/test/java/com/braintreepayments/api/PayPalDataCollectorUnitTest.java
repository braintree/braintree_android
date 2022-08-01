package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PayPalDataCollectorUnitTest {

    private Context context;
    private String sampleInstallationGUID;
    private Configuration configuration;

    private UUIDHelper uuidHelper;

    private BraintreeClient braintreeClient;
    private MagnesInternalClient magnesInternalClient;

    @Before
    public void beforeEach() throws JSONException {
        uuidHelper = mock(UUIDHelper.class);
        magnesInternalClient = mock(MagnesInternalClient.class);

        context = ApplicationProvider.getApplicationContext();

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
        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        assertEquals(sampleInstallationGUID, sut.getPayPalInstallationGUID(context));
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithDefaultRequest() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        when(configuration.getEnvironment()).thenReturn("production");

        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(PayPalDataCollectorRequest.class))).thenReturn("sample-client-id");

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);
        sut.getClientMetadataId(context, configuration);

        ArgumentCaptor<PayPalDataCollectorRequest> captor = ArgumentCaptor.forClass(PayPalDataCollectorRequest.class);
        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), captor.capture());

        PayPalDataCollectorRequest request = captor.getValue();
        assertEquals(sampleInstallationGUID, request.getApplicationGuid());
    }

    @Test
    public void getClientMetadataId_forwardsClientMetadataIdFromMagnesResult() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(PayPalDataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);
        String result = sut.getClientMetadataId(context, configuration);

        assertEquals("paypal-clientmetadata-id", result);
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithCustomRequestAndForwardsClientMetadataIdFromMagnesResult() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        when(configuration.getEnvironment()).thenReturn("production");

        PayPalDataCollectorRequest customRequest = new PayPalDataCollectorRequest();
        when(magnesInternalClient.getClientMetadataId(context, configuration, customRequest)).thenReturn("sample-client-id");

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);
        String result = sut.getClientMetadataId(context, customRequest, configuration);

        assertEquals("sample-client-id", result);
    }

    @Test
    public void collectDeviceData_forwardsConfigurationFetchErrors() {
        Exception configError = new Exception("configuration error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        PayPalDataCollectorCallback callback = mock(PayPalDataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        verify(callback).onResult(null, configError);
    }

//    @Test
//    public void collectDeviceData_getsDeviceDataJSONWithCorrelationIdFromPayPal() throws Exception {
//        when(payPalDataCollector.getClientMetadataId(context, kountDisabledConfiguration)).thenReturn("sample_correlation_id");
//
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
//                .configuration(kountDisabledConfiguration)
//                .build();
//
//        DataCollector sut = new DataCollector(
//                braintreeClient, payPalDataCollector, kountDataCollector, uuidHelper);
//
//        DataCollectorCallback callback = mock(DataCollectorCallback.class);
//        sut.collectDeviceData(context, callback);
//
//        ArgumentCaptor<String> deviceDataCaptor = ArgumentCaptor.forClass(String.class);
//        verify(callback).onResult(deviceDataCaptor.capture(), (Exception) isNull());
//
//        String deviceData = deviceDataCaptor.getValue();
//        JSONObject json = new JSONObject(deviceData);
//        junit.framework.Assert.assertEquals("sample_correlation_id", json.getString("correlation_id"));
//    }
}
