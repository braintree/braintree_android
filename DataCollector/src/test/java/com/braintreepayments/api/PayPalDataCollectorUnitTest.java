package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
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

        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(PayPalDataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);
        sut.getClientMetadataId(context, configuration);

        ArgumentCaptor<PayPalDataCollectorRequest> captor = ArgumentCaptor.forClass(PayPalDataCollectorRequest.class);
        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), captor.capture());

        PayPalDataCollectorRequest request = captor.getValue();
        assertEquals(sampleInstallationGUID, request.getApplicationGuid());
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithCustomRequestAndForwardsClientMetadataIdFromMagnesResult() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        PayPalDataCollectorRequest customRequest = new PayPalDataCollectorRequest();
        when(magnesInternalClient.getClientMetadataId(context, configuration, customRequest)).thenReturn("paypal-clientmetadata-id");

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);
        sut.getClientMetadataId(context, customRequest, configuration);

        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), same(customRequest));
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

    @Test
    public void collectDeviceData_configuresMagnesWithDefaultRequest() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(PayPalDataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        PayPalDataCollectorCallback callback = mock(PayPalDataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        ArgumentCaptor<PayPalDataCollectorRequest> captor = ArgumentCaptor.forClass(PayPalDataCollectorRequest.class);
        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), captor.capture());

        PayPalDataCollectorRequest request = captor.getValue();
        assertEquals(sampleInstallationGUID, request.getApplicationGuid());
    }

    @Test
    public void collectDeviceData_configuresMagnesWithClientId() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(PayPalDataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        PayPalDataCollectorCallback callback = mock(PayPalDataCollectorCallback.class);
        sut.collectDeviceData(context, "custom-client-metadata-id", callback);

        ArgumentCaptor<PayPalDataCollectorRequest> captor = ArgumentCaptor.forClass(PayPalDataCollectorRequest.class);
        verify(magnesInternalClient).getClientMetadataId(same(context), same(configuration), captor.capture());

        PayPalDataCollectorRequest request = captor.getValue();
        assertEquals(sampleInstallationGUID, request.getApplicationGuid());
        assertEquals("custom-client-metadata-id", request.getClientMetadataId());
    }

    @Test
    public void collectDeviceData_getsDeviceDataJSONWithCorrelationIdFromPayPal() throws Exception {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        when(magnesInternalClient.getClientMetadataId(same(context), same(configuration), any(PayPalDataCollectorRequest.class))).thenReturn("paypal-clientmetadata-id");

        PayPalDataCollector sut = new PayPalDataCollector(braintreeClient, magnesInternalClient, uuidHelper);

        PayPalDataCollectorCallback callback = mock(PayPalDataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        ArgumentCaptor<String> deviceDataCaptor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(deviceDataCaptor.capture(), (Exception) isNull());

        String deviceData = deviceDataCaptor.getValue();
        JSONObject json = new JSONObject(deviceData);
        assertEquals("paypal-clientmetadata-id", json.getString("correlation_id"));
    }
}
