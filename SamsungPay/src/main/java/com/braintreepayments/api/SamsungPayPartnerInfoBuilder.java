package com.braintreepayments.api;

import android.os.Bundle;

import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;

import org.json.JSONException;
import org.json.JSONObject;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.PARTNER_SERVICE_TYPE;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.ServiceType.INAPP_PAYMENT;
import static com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager.EXTRA_KEY_TEST_MODE;

class SamsungPayPartnerInfoBuilder {

    private static final String API_VERSION_KEY = "braintreeTokenizationApiVersion";
    private static final String CLIENT_SDK_METADATA_KEY = "clientSdkMetadata";

    private static final String API_VERSION = "2018-10-01";

    private String sessionId;
    private String integrationType;
    private Configuration configuration;

    SamsungPayPartnerInfoBuilder() {
    }

    SamsungPayPartnerInfoBuilder setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    SamsungPayPartnerInfoBuilder setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    SamsungPayPartnerInfoBuilder setIntegrationType(String integrationType) {
        this.integrationType = integrationType;
        return this;
    }

    PartnerInfo build() {
        Bundle data = new Bundle();
        data.putString(PARTNER_SERVICE_TYPE, INAPP_PAYMENT.toString());

        boolean isTestEnvironment = false;
        String samsungPayEnvironment = configuration.getSamsungPayEnvironment();
        if (samsungPayEnvironment != null) {
            isTestEnvironment = samsungPayEnvironment.equalsIgnoreCase("SANDBOX");
        }
        data.putBoolean(EXTRA_KEY_TEST_MODE, isTestEnvironment);

        JSONObject clientSdkMetadata = new MetadataBuilder()
                .integration(integrationType)
                .sessionId(sessionId)
                .version()
                .build();

        JSONObject additionalData = new JSONObject();
        try {
            additionalData.put(API_VERSION_KEY, API_VERSION);
            additionalData.put(CLIENT_SDK_METADATA_KEY, clientSdkMetadata);
        } catch (JSONException ignored) {
        }
        data.putString("additionalData", additionalData.toString());

        String serviceId = configuration.getSamsungPayServiceId();
        return new PartnerInfo(serviceId, data);
    }
}
