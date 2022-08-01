package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;

import lib.android.paypal.com.magnessdk.Environment;
import lib.android.paypal.com.magnessdk.InvalidInputException;
import lib.android.paypal.com.magnessdk.MagnesResult;
import lib.android.paypal.com.magnessdk.MagnesSDK;
import lib.android.paypal.com.magnessdk.MagnesSettings;
import lib.android.paypal.com.magnessdk.MagnesSource;

@RunWith(RobolectricTestRunner.class)
public class MagnesInternalClientUnitTest {

    private Context context;

    private Configuration prodConfiguration;
    private Configuration sandboxConfiguration;

    private MagnesSDK magnesSDK;
    private String validApplicationGUID;

    private MagnesResult magnesResult;

    private HashMap<String, String> additionalData;
    private PayPalDataCollectorRequest payPalDataCollectorRequest;

    @Before
    public void beforeEach() {
        magnesSDK = mock(MagnesSDK.class);
        context = ApplicationProvider.getApplicationContext();

        prodConfiguration = mock(Configuration.class);
        when(prodConfiguration.getEnvironment()).thenReturn("production");

        sandboxConfiguration = mock(Configuration.class);
        when(sandboxConfiguration.getEnvironment()).thenReturn("sandbox");

        // NOTE: this uuid has no actual meaning; Magnes requires a valid guid for tests
        validApplicationGUID = "0665203b-16e4-4ce2-be98-d7d73ec32e8a";

        magnesResult = mock(MagnesResult.class);
        when(magnesResult.getPaypalClientMetaDataId()).thenReturn("magnes-client-metadata-id");

        additionalData = new HashMap<>();

        payPalDataCollectorRequest = new PayPalDataCollectorRequest()
                .setClientMetadataId("sample-client-metadata-id")
                .setDisableBeacon(true)
                .setAdditionalData(additionalData)
                .setApplicationGuid(validApplicationGUID);
    }

    @Test
    public void getClientMetaDataId_returnsEmptyStringWhenContextIsNull() {
        MagnesInternalClient sut = new MagnesInternalClient(magnesSDK);
        String result = sut.getClientMetadataId(null, sandboxConfiguration, payPalDataCollectorRequest);
        assertEquals("", result);
    }

    @Test
    public void getClientMetaDataId_configuresMagnesSourceAsBraintree() throws InvalidInputException {
        when(magnesSDK.collectAndSubmit(context, "sample-client-metadata-id", additionalData)).thenReturn(magnesResult);

        MagnesInternalClient sut = new MagnesInternalClient(magnesSDK);
        sut.getClientMetadataId(context, sandboxConfiguration, payPalDataCollectorRequest);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        assertEquals(MagnesSource.BRAINTREE.getVersion(), magnesSettings.getMagnesSource());
    }

    @Test
    public void getClientMetaDataId_whenBraintreeEnvironmentIsSandbox_configuresMagnesEnvironmentToSandbox() throws InvalidInputException {
        when(magnesSDK.collectAndSubmit(context, "sample-client-metadata-id", additionalData)).thenReturn(magnesResult);

        MagnesInternalClient sut = new MagnesInternalClient(magnesSDK);
        sut.getClientMetadataId(context, sandboxConfiguration, payPalDataCollectorRequest);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        assertEquals(Environment.SANDBOX, magnesSettings.getEnvironment());
    }

    @Test
    public void getClientMetaDataId_whenBraintreeEnvironmentIsProd_configuresMagnesEnvironmentToLive() throws InvalidInputException {
        when(magnesSDK.collectAndSubmit(context, "sample-client-metadata-id", additionalData)).thenReturn(magnesResult);

        MagnesInternalClient sut = new MagnesInternalClient(magnesSDK);
        sut.getClientMetadataId(context, prodConfiguration, payPalDataCollectorRequest);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        assertEquals(Environment.LIVE, magnesSettings.getEnvironment());
    }

    @Test
    public void getClientMetaDataId_forwardsDisableBeaconOptionToMagnes() throws InvalidInputException {
        when(magnesSDK.collectAndSubmit(context, "sample-client-metadata-id", additionalData)).thenReturn(magnesResult);

        MagnesInternalClient sut = new MagnesInternalClient(magnesSDK);
        sut.getClientMetadataId(context, prodConfiguration, payPalDataCollectorRequest);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        assertTrue(magnesSettings.isDisableBeacon());
    }

    @Test
    public void getClientMetaDataId_forwardsApplicationGUIDOptionToMagnes() throws InvalidInputException {
        when(magnesSDK.collectAndSubmit(context, "sample-client-metadata-id", additionalData)).thenReturn(magnesResult);

        MagnesInternalClient sut = new MagnesInternalClient(magnesSDK);
        sut.getClientMetadataId(context, prodConfiguration, payPalDataCollectorRequest);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        assertEquals(validApplicationGUID, magnesSettings.getAppGuid());
    }

    @Test
    public void getClientMetaDataId_returnsAnEmptyStringWhenApplicationGUIDIsInvalid() {
        PayPalDataCollectorRequest requestWithInvalidGUID = new PayPalDataCollectorRequest()
                .setApplicationGuid("invalid guid");

        MagnesInternalClient sut = new MagnesInternalClient(magnesSDK);
        String result = sut.getClientMetadataId(context, prodConfiguration, requestWithInvalidGUID);

        assertEquals("", result);
    }

    @Test
    public void getClientMetaDataId_forwardsClientMetadataIdFromMagnesStart() throws InvalidInputException {
        when(magnesSDK.collectAndSubmit(context, "sample-client-metadata-id", additionalData)).thenReturn(magnesResult);

        MagnesInternalClient sut = new MagnesInternalClient(magnesSDK);
        String result = sut.getClientMetadataId(context, prodConfiguration, payPalDataCollectorRequest);

        assertEquals("magnes-client-metadata-id", result);
    }

    @Test
    public void getClientMetaDataId_returnsAnEmptyStringWhenCollectAndSubmitThrows() throws InvalidInputException {

        when(magnesSDK.collectAndSubmit(context, "sample-client-metadata-id", additionalData)).thenThrow(new InvalidInputException("invalid input"));

        MagnesInternalClient sut = new MagnesInternalClient(magnesSDK);
        String result = sut.getClientMetadataId(context, prodConfiguration, payPalDataCollectorRequest);

        assertEquals("", result);
    }
}