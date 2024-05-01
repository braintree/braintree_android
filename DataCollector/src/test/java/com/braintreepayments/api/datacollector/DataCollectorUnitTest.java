package com.braintreepayments.api.datacollector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.PayPalDataCollectorRequest;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.Fixtures;
import com.braintreepayments.api.MockBraintreeClientBuilder;
import com.braintreepayments.api.core.UUIDHelper;

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

    private PayPalDataCollectorRequest payPalDataCollectorRequest;
    private String riskCorrelationId = "risk_correlation_id";

    @Before
    public void beforeEach() throws JSONException {
        uuidHelper = mock(UUIDHelper.class);
        magnesInternalClient = mock(MagnesInternalClient.class);

        context = ApplicationProvider.getApplicationContext();

        payPalDataCollectorRequest = new PayPalDataCollectorRequest(true, riskCorrelationId);

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
        boolean hasUserLocationConsent = true;

        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(
                DataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        DataCollector sut = new DataCollector(braintreeClient, magnesInternalClient, uuidHelper);
        sut.getClientMetadataId(context, configuration);

        ArgumentCaptor<DataCollectorRequest> captor = ArgumentCaptor.forClass(DataCollectorRequest.class);
        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), captor.capture());

        DataCollectorRequest request = captor.getValue();

        assertEquals(sampleInstallationGUID, request.getApplicationGuid());
        assertEquals(hasUserLocationConsent, request.getHasUserLocationConsent());
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
        assertFalse(request.getHasUserLocationConsent());
    }

//    @Test
//    public void collectDeviceData_with_request_configuresMagnesWithDefaultRequest() {
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
//            .configuration(configuration)
//            .build();
//
//        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
//        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(PayPalDataCollectorInternalRequest.class))).thenReturn("paypal-clientmetadata-id");
//
//        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);
//
//        PayPalDataCollectorCallback callback = mock(PayPalDataCollectorCallback.class);
//        sut.collectDeviceData(context, payPalDataCollectorRequest, callback);
//
//        ArgumentCaptor<PayPalDataCollectorInternalRequest> captor = ArgumentCaptor.forClass(PayPalDataCollectorInternalRequest.class);
//        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), captor.capture());
//
//        PayPalDataCollectorInternalRequest request = captor.getValue();
//        assertEquals(sampleInstallationGUID, request.getApplicationGuid());
//        assertEquals(riskCorrelationId, request.getClientMetadataId());
//        assertTrue(request.getHasUserLocationConsent());
//    }

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
        assertFalse( request.getHasUserLocationConsent());
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
