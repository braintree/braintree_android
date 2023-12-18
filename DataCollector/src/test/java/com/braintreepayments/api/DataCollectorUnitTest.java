package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DataCollectorUnitTest {

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

        // this uuid has no actual meaning; magnes requires a valid guid for tests
        sampleInstallationGUID = "0665203b-16e4-4ce2-be98-d7d73ec32e8a";

        braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN))
                .build();
    }

    @Test
    public void getPayPalInstallationGUID_returnsInstallationIdentifier() {

        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        DataCollector sut = new DataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        assertEquals(sampleInstallationGUID, sut.getPayPalInstallationGUID(context));
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithDefaultRequest() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(
                DataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        DataCollector sut = new DataCollector(braintreeClient, magnesInternalClient, uuidHelper);
        sut.getClientMetadataId(context, configuration);

        ArgumentCaptor<DataCollectorRequest> captor = ArgumentCaptor.forClass(DataCollectorRequest.class);
        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), captor.capture());

        DataCollectorRequest request = captor.getValue();
        assertEquals(sampleInstallationGUID, request.getApplicationGuid());
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithCustomRequestAndForwardsClientMetadataIdFromMagnesResult() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        DataCollectorRequest customRequest = new DataCollectorRequest();
        when(magnesInternalClient.getClientMetadataId(context, configuration, customRequest)).thenReturn("paypal-clientmetadata-id");

        DataCollector sut = new DataCollector(braintreeClient, magnesInternalClient, uuidHelper);
        sut.getClientMetadataId(context, customRequest, configuration);

        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), same(customRequest));
    }

    @Test
    public void getClientMetadataId_forwardsClientMetadataIdFromMagnesResult() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(
                DataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        DataCollector sut = new DataCollector(braintreeClient, magnesInternalClient, uuidHelper);
        String result = sut.getClientMetadataId(context, configuration);

        assertEquals("paypal-clientmetadata-id", result);
    }

    @Test
    public void collectDeviceData_forwardsConfigurationFetchErrors() {
        Exception configError = new Exception("configuration error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        DataCollector sut = new DataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        ArgumentCaptor<DataCollectorResult> deviceDataCaptor = ArgumentCaptor.forClass(DataCollectorResult.class);
        verify(callback).onDataCollectorResult(deviceDataCaptor.capture());

        DataCollectorResult result = deviceDataCaptor.getValue();
        assertTrue(result instanceof DataCollectorResult.Failure);
        assertEquals(configError, ((DataCollectorResult.Failure) result).getError());
    }

    @Test
    public void collectDeviceData_configuresMagnesWithDefaultRequest() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(
                DataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        DataCollector sut = new DataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        ArgumentCaptor<DataCollectorRequest> captor = ArgumentCaptor.forClass(DataCollectorRequest.class);
        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), captor.capture());

        DataCollectorRequest request = captor.getValue();
        assertEquals(sampleInstallationGUID, request.getApplicationGuid());
    }

    @Test
    public void collectDeviceData_configuresMagnesWithClientId() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(
                DataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        DataCollector sut = new DataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        sut.collectDeviceData(context, "custom-client-metadata-id", callback);

        ArgumentCaptor<DataCollectorRequest> captor = ArgumentCaptor.forClass(DataCollectorRequest.class);
        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), captor.capture());

        DataCollectorRequest request = captor.getValue();
        assertEquals(sampleInstallationGUID, request.getApplicationGuid());
        assertEquals("custom-client-metadata-id", request.getClientMetadataId());
    }

    @Test
    public void collectDeviceData_getsDeviceDataJSONWithCorrelationIdFromPayPal() throws Exception {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(
                DataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        DataCollector sut = new DataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        ArgumentCaptor<DataCollectorResult> deviceDataCaptor = ArgumentCaptor.forClass(DataCollectorResult.class);
        verify(callback).onDataCollectorResult(deviceDataCaptor.capture());

        DataCollectorResult result = deviceDataCaptor.getValue();
        assertTrue(result instanceof DataCollectorResult.Success);
        String deviceData = ((DataCollectorResult.Success) result).getDeviceData();
        JSONObject json = new JSONObject(deviceData);
        assertEquals("paypal-clientmetadata-id", json.getString("correlation_id"));
    }
}
