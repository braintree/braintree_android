package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.TestConfigurationBuilder.TestKountConfigurationBuilder;

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
    private KountDataCollector kountDataCollector;

    private Configuration kountEnabledConfiguration;
    private Configuration kountDisabledConfiguration;
    private UUIDHelper uuidHelper;

    @Before
    public void beforeEach() throws JSONException {
        kountEnabledConfiguration = new TestConfigurationBuilder()
                .kount(new TestKountConfigurationBuilder()
                        .kountMerchantId("600000"))
                .buildConfiguration();
        kountDisabledConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        context = ApplicationProvider.getApplicationContext();
        payPalDataCollector = mock(PayPalDataCollector.class);
        kountDataCollector = mock(KountDataCollector.class);
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
                braintreeClient, payPalDataCollector, kountDataCollector, uuidHelper);

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        verify(callback).onResult(null, configError);
    }

    @Test
    public void collectDeviceData_withKountEnabled_getsDeviceDataJSONWithDeviceSessionId() throws Exception {
        when(payPalDataCollector.getClientMetadataId(context, kountEnabledConfiguration)).thenReturn("sample_correlation_id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(kountEnabledConfiguration)
                .build();

        DataCollector sut = new DataCollector(
                braintreeClient, payPalDataCollector, kountDataCollector, uuidHelper);

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        ArgumentCaptor<KountDataCollectorCallback> kountCaptor =
            ArgumentCaptor.forClass(KountDataCollectorCallback.class);
        verify(kountDataCollector).startDataCollection(any(Context.class), eq("600000"), eq("sample-formatted-uuid"), kountCaptor.capture());

        KountDataCollectorCallback kountCallback = kountCaptor.getValue();
        kountCallback.onResult("kount_session_id", null);

        ArgumentCaptor<String> deviceDataCaptor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(deviceDataCaptor.capture(), (Exception) isNull());

        String deviceData = deviceDataCaptor.getValue();
        JSONObject json = new JSONObject(deviceData);
        assertEquals("600000", json.getString("fraud_merchant_id"));
        assertEquals("sample_correlation_id", json.getString("correlation_id"));
    }

    @Test
    public void collectDeviceData_withKountDisabled_getsDeviceDataJSONWithCorrelationIdFromPayPal() throws Exception {
        when(payPalDataCollector.getClientMetadataId(context, kountDisabledConfiguration)).thenReturn("sample_correlation_id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(kountDisabledConfiguration)
                .build();

        DataCollector sut = new DataCollector(
                braintreeClient, payPalDataCollector, kountDataCollector, uuidHelper);

        DataCollectorCallback callback = mock(DataCollectorCallback.class);
        sut.collectDeviceData(context, callback);

        ArgumentCaptor<String> deviceDataCaptor = ArgumentCaptor.forClass(String.class);
        verify(callback).onResult(deviceDataCaptor.capture(), (Exception) isNull());

        String deviceData = deviceDataCaptor.getValue();
        JSONObject json = new JSONObject(deviceData);
        assertEquals("sample_correlation_id", json.getString("correlation_id"));
    }
}