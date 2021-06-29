package com.braintreepayments.api;

import android.os.Bundle;

import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.PARTNER_SERVICE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class SamsungPartnerInfoBuilderTestJava {

    @Test
    public void build_setsSamsungServiceId() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getSamsungPayServiceId()).thenReturn("samsung-service-id");

        PartnerInfo partnerInfo = new SamsungPartnerInfoBuilder()
                .setConfiguration(configuration)
                .build();

        assertEquals("samsung-service-id", partnerInfo.getServiceId());
    }

    @Test
    public void build_setsData() {
        Configuration configuration = mock(Configuration.class);

        PartnerInfo partnerInfo = new SamsungPartnerInfoBuilder()
                .setConfiguration(configuration)
                .setIntegrationType("braintree-integration-type")
                .setSessionId("braintree-session-id")
                .build();

        Bundle data = partnerInfo.getData();
        assertEquals("INAPP_PAYMENT", data.getString(PARTNER_SERVICE_TYPE));

    }
}
