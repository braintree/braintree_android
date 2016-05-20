package com.braintreepayments.api.internal;

import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.TestConfigurationStringBuilder;

import org.json.JSONException;

public class TestConfigurationBuilder extends TestConfigurationStringBuilder {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T build(Class<T> clazz) {
        if (clazz.equals(Configuration.class)) {
            try {
                return (T) Configuration.fromJson(build());
            } catch (JSONException ignored) {
                return null;
            }
        } else {
            return super.build(clazz);
        }
    }
}
