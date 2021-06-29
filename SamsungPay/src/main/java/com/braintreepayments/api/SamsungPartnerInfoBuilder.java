package com.braintreepayments.api;

import android.os.Bundle;

import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;

import org.json.JSONException;
import org.json.JSONObject;

import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.PARTNER_SERVICE_TYPE;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.ServiceType.INAPP_PAYMENT;
import static com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager.EXTRA_KEY_TEST_MODE;

class SamsungPartnerInfoBuilder {

    private static final String BRAINTREE_TOKENIZATION_API_VERSION = "2018-10-01";

    private String sessionId;
    private String integrationType;
    private Configuration configuration;

    SamsungPartnerInfoBuilder() {}

    SamsungPartnerInfoBuilder setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    SamsungPartnerInfoBuilder setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    SamsungPartnerInfoBuilder setIntegrationType(String integrationType) {
        this.integrationType = integrationType;
        return this;
    }

    PartnerInfo build() throws JSONException {
        Bundle data = new Bundle();
        data.putString(PARTNER_SERVICE_TYPE, INAPP_PAYMENT.toString());

        data.putBoolean(EXTRA_KEY_TEST_MODE, false);

        JSONObject additionalData = new JSONObject();
        additionalData.put("braintreeTokenizationApiVersion", BRAINTREE_TOKENIZATION_API_VERSION);

        JSONObject clientSdkMetadata = new MetadataBuilder()
                .integration(integrationType)
                .sessionId(sessionId)
                .version()
                .build();
        additionalData.put("clientSdkMetadata", clientSdkMetadata);

        data.putString("additionalData", additionalData.toString());

        String serviceId = configuration.getSamsungPayServiceId();
        return new PartnerInfo(serviceId, data);
    }
}
