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

    Context context;
    MagnesSDK magnesSDK;
    PayPalInstallationIdentifier payPalInstallationIdentifier;

    @Before
    public void beforeEach() {
        magnesSDK = mock(MagnesSDK.class);
        payPalInstallationIdentifier = mock(PayPalInstallationIdentifier.class);
        context = mock(Context.class);
    }

    @Test
    public void getPayPalInstallationGUID_returnsInstallationIdentifier() {
        when(payPalInstallationIdentifier.getInstallationGUID(context)).thenReturn("sample-application-guid");
        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, payPalInstallationIdentifier);

        assertEquals("sample-application-guid", sut.getPayPalInstallationGUID(context));
    }

    @Test
    public void getClientMetaDataId_returnsEmptyStringWhenContextIsNull() {
        when(payPalInstallationIdentifier.getInstallationGUID(context)).thenReturn("sample-application-guid");
        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, payPalInstallationIdentifier);

        String result = sut.getClientMetadataId(null);
        assertEquals("", result);
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithDefaultSettings() throws InvalidInputException {
        when(payPalInstallationIdentifier.getInstallationGUID(context)).thenReturn("sample-application-guid");

        when(magnesSDK.collectAndSubmit(any(Context.class), (String) isNull(), ArgumentMatchers.<HashMap<String, String>>isNull()))
                .thenReturn(mock(MagnesResult.class));

        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, payPalInstallationIdentifier);
        sut.getClientMetadataId(context);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        assertEquals(MagnesSource.BRAINTREE.getVersion(), magnesSettings.getMagnesSource());
        assertFalse(magnesSettings.isDisableBeacon());
        assertEquals(Environment.LIVE, magnesSettings.getEnvironment());
        assertEquals("sample-application-guid", magnesSettings.getAppGuid());
    }

    @Test
    public void getClientMetadataId_configuresMagnesWithPayPalDataCollectorRequest() throws InvalidInputException {
        HashMap<String, String> additionalData = new HashMap<>();
        PayPalDataCollectorRequest payPalDataCollectorRequest = new PayPalDataCollectorRequest()
                .setApplicationGuid("application-guid")
                .setClientMetadataId("client-metadata-id")
                .setAdditionalData(additionalData)
                .setDisableBeacon(false);

        when(magnesSDK.collectAndSubmit(any(Context.class), eq("client-metadata-id"), same(additionalData)))
                .thenReturn(mock(MagnesResult.class));

        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, payPalInstallationIdentifier);
        sut.getClientMetadataId(context, payPalDataCollectorRequest);

        ArgumentCaptor<MagnesSettings> captor = ArgumentCaptor.forClass(MagnesSettings.class);
        verify(magnesSDK).setUp(captor.capture());

        MagnesSettings magnesSettings = captor.getValue();
        assertEquals(MagnesSource.BRAINTREE.getVersion(), magnesSettings.getMagnesSource());
        assertFalse(magnesSettings.isDisableBeacon());
        assertEquals(Environment.LIVE, magnesSettings.getEnvironment());
        assertEquals("application-guid", magnesSettings.getAppGuid());
    }

    @Test
    public void getClientMetadataId_forwardsClientMetadataIdFromMagnesResult() throws InvalidInputException {
        when(payPalInstallationIdentifier.getInstallationGUID(context)).thenReturn("sample-application-guid");

        MagnesResult magnesResult = mock(MagnesResult.class);
        when(magnesResult.getPaypalClientMetaDataId()).thenReturn("paypal-clientmetadata-id");

        when(magnesSDK.collectAndSubmit(any(Context.class), (String) isNull(), ArgumentMatchers.<HashMap<String, String>>isNull()))
                .thenReturn(magnesResult);

        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, payPalInstallationIdentifier);
        String result = sut.getClientMetadataId(context);

        assertEquals("paypal-clientmetadata-id", result);
    }

    @Test
    public void getClientMetadataId_returnsEmptyStringWhenMagnesInputInvalid() throws InvalidInputException {
        when(payPalInstallationIdentifier.getInstallationGUID(context)).thenReturn("sample-application-guid");

        when(magnesSDK.collectAndSubmit(any(Context.class), (String) isNull(), ArgumentMatchers.<HashMap<String, String>>isNull()))
                .thenThrow(new InvalidInputException("invalid input"));

        PayPalDataCollector sut = new PayPalDataCollector(magnesSDK, payPalInstallationIdentifier);
        String result = sut.getClientMetadataId(context);

        assertEquals("", result);
    }
}
