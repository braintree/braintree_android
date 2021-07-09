package com.braintreepayments.api;

import android.os.Bundle;

import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.PARTNER_SERVICE_TYPE;
import static com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager.EXTRA_KEY_TEST_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class SamsungPayPartnerInfoBuilderUnitTest {

    @Test
    public void build_setsSamsungServiceId() throws JSONException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getSamsungPayServiceId()).thenReturn("samsung-service-id");

        PartnerInfo partnerInfo = new SamsungPayPartnerInfoBuilder()
                .setConfiguration(configuration)
                .build();

        assertEquals("samsung-service-id", partnerInfo.getServiceId());
    }

    @Test
    public void build_setsData() throws JSONException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getSamsungPayEnvironment()).thenReturn("PRODUCTION");

        PartnerInfo partnerInfo = new SamsungPayPartnerInfoBuilder()
                .setConfiguration(configuration)
                .setIntegrationType("braintree-integration-type")
                .setSessionId("braintree-session-id")
                .build();

        Bundle data = partnerInfo.getData();
        assertEquals("INAPP_PAYMENT", data.getString(PARTNER_SERVICE_TYPE));
        assertFalse(data.getBoolean(EXTRA_KEY_TEST_MODE));

        JSONObject expectedAdditionalData = new JSONObject()
                .put("braintreeTokenizationApiVersion", "2018-10-01")
                .put("clientSdkMetadata", new JSONObject()
                        .put("integration", "braintree-integration-type")
                        .put("platform", "android")
                        .put("sessionId", "braintree-session-id")
                        .put("version", BuildConfig.VERSION_NAME)
                );

        String actualAdditionalData = data.getString("additionalData");
        JSONAssert.assertEquals(expectedAdditionalData, new JSONObject(actualAdditionalData), true);
    }

    @Test
    public void build_whenEnvironmentIsSANDBOX_setsTestModeDataParamToTrue() throws JSONException {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getSamsungPayEnvironment()).thenReturn("SANDBOX");

        PartnerInfo partnerInfo = new SamsungPayPartnerInfoBuilder()
                .setConfiguration(configuration)
                .setIntegrationType("braintree-integration-type")
                .setSessionId("braintree-session-id")
                .build();

        Bundle data = partnerInfo.getData();
        assertTrue(data.getBoolean(EXTRA_KEY_TEST_MODE));
    }
}
