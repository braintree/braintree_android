package com.braintreepayments.api;

import android.os.Bundle;

import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;

public class SamsungPartnerInfoBuilder {

    private Configuration configuration;

    SamsungPartnerInfoBuilder() {}

    SamsungPartnerInfoBuilder setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    PartnerInfo build() {
        Bundle bundle = new Bundle();

        String serviceId = configuration.getSamsungPayServiceId();
        return new PartnerInfo(serviceId, bundle);
    }
}
