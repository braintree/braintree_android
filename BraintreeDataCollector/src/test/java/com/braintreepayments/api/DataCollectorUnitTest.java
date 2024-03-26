package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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

    private PayPalDataCollector payPalDataCollector;

    private Configuration configuration;
    private UUIDHelper uuidHelper;

    @Before
    public void beforeEach() throws JSONException {
        configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        context = ApplicationProvider.getApplicationContext();
        payPalDataCollector = mock(PayPalDataCollector.class);
        uuidHelper = mock(UUIDHelper.class);

        when(uuidHelper.getFormattedUUID()).thenReturn("sample-formatted-uuid");
    }

    @Test
    public void collectDeviceData_forwardsConfigurationFetchErrors() {
        Exception configError = new Exception("configuration error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        DataCollector sut = new DataCollector(
                braintreeClient, payPalDataCollector);

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        verify(callback).onResult(null, configError);
    }

    @Test
    public void collectDeviceData_getsDeviceDataJSONWithCorrelationIdFromPayPal() throws Exception {
        when(payPalDataCollector.getClientMetadataId(context, configuration, false)).thenReturn("sample_correlation_id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .build();

        DataCollector sut = new DataCollector(
                braintreeClient, payPalDataCollector);

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        ArgumentCaptor<String> deviceDataCaptor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(deviceDataCaptor.capture(), (Exception) isNull());

        String deviceData = deviceDataCaptor.getValue();
        JSONObject json = new JSONObject(deviceData);
        assertEquals("sample_correlation_id", json.getString("correlation_id"));
    }

    @Test
    public void collectDeviceData_without_DataCollectorRequest_sets_hasUserLocationConsent_to_false() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(configuration)
            .build();

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        DataCollector sut = new DataCollector(braintreeClient, payPalDataCollector);

        sut.collectDeviceData(context, callback);

        ArgumentCaptor<String> deviceDataCaptor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(deviceDataCaptor.capture(), (Exception) isNull());

        verify(payPalDataCollector).getClientMetadataId(context, configuration, false);
    }

    @Test
    public void collectDeviceData_with_DataCollectorRequest_sets_correct_values_for_getClientMetadataId() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(configuration)
            .build();

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        DataCollector sut = new DataCollector(braintreeClient, payPalDataCollector);

        DataCollectorRequest dataCollectorRequest = new DataCollectorRequest(true);

        sut.collectDeviceData(context, dataCollectorRequest, callback);

        ArgumentCaptor<String> deviceDataCaptor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(deviceDataCaptor.capture(), (Exception) isNull());

        verify(payPalDataCollector).getClientMetadataId(context, configuration, true);
    }
}