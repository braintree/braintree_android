package com.braintreepayments.api;

import android.content.Context;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PayPalDataCollectorUnitTest {

    private Context context;
    private String sampleInstallationGUID;

    private MagnesSDK magnesSDK;
    private UUIDHelper uuidHelper;

    @Before
    public void beforeEach() {
        magnesSDK = mock(MagnesSDK.class);
        uuidHelper = mock(UUIDHelper.class);
        context = mock(Context.class);

        // this uuid has no actual meaning; magnes requires a valid guid for tests
        sampleInstallationGUID = "0665203b-16e4-4ce2-be98-d7d73ec32e8a";
    }

    @Test
    public void getPayPalInstallationGUID_returnsInstallationIdentifier() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, uuidHelper);

        assertEquals(sampleInstallationGUID, sut.getPayPalInstallationGUID(context));
    }

    @Test
    public void getClientMetaDataId_returnsEmptyStringWhenContextIsNull() {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);
        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, uuidHelper);

        String result = sut.getClientMetadataId(null);
        assertEquals("", result);
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithDefaultSettings() throws InvalidInputException {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        when(magnesSDK.collectAndSubmit(any(Context.class), (String) isNull(), ArgumentMatchers.<HashMap<String, String>>isNull()))
                .thenReturn(mock(MagnesResult.class));

        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, uuidHelper);
        sut.getClientMetadataId(context);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        assertEquals(MagnesSource.BRAINTREE.getVersion(), magnesSettings.getMagnesSource());
        assertFalse(magnesSettings.isDisableBeacon());
        assertEquals(Environment.LIVE, magnesSettings.getEnvironment());
        assertEquals(sampleInstallationGUID, magnesSettings.getAppGuid());
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithPayPalDataCollectorRequest() throws InvalidInputException {
        String applicationGUID = "07ea6967-1e4a-4aaa-942d-7f13db372b75";

        HashMap<String, String> additionalData = new HashMap<>();
        PayPalDataCollectorRequest payPalDataCollectorRequest = new PayPalDataCollectorRequest()
                .setApplicationGuid(applicationGUID)
                .setClientMetadataId("client-metadata-id")
                .setAdditionalData(additionalData)
                .setDisableBeacon(false);

        when(magnesSDK.collectAndSubmit(any(Context.class), eq("client-metadata-id"), same(additionalData)))
                .thenReturn(mock(MagnesResult.class));

        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, uuidHelper);
        sut.getClientMetadataId(context, payPalDataCollectorRequest);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        assertEquals(MagnesSource.BRAINTREE.getVersion(), magnesSettings.getMagnesSource());
        assertFalse(magnesSettings.isDisableBeacon());
        assertEquals(Environment.LIVE, magnesSettings.getEnvironment());
        assertEquals(applicationGUID, magnesSettings.getAppGuid());
    }

    @Test
    public void getClientMetadataId_forwardsClientMetadataIdFromMagnesResult() throws InvalidInputException {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        MagnesResult magnesResult = mock(MagnesResult.class);
        when(magnesResult.getPaypalClientMetaDataId()).thenReturn("paypal-clientmetadata-id");

        when(magnesSDK.collectAndSubmit(any(Context.class), (String) isNull(), ArgumentMatchers.<HashMap<String, String>>isNull()))
                .thenReturn(magnesResult);

        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, uuidHelper);
        String result = sut.getClientMetadataId(context);

        assertEquals("paypal-clientmetadata-id", result);
    }

    @Test
    public void getClientMetadataId_returnsEmptyStringWhenMagnesInputInvalid() throws InvalidInputException {
        when(uuidHelper.getInstallationGUID(context)).thenReturn(sampleInstallationGUID);

        when(magnesSDK.collectAndSubmit(any(Context.class), (String) isNull(), ArgumentMatchers.<HashMap<String, String>>isNull()))
                .thenThrow(new InvalidInputException("invalid input"));

        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, uuidHelper);
        String result = sut.getClientMetadataId(context);

        assertEquals("", result);
    }
}
