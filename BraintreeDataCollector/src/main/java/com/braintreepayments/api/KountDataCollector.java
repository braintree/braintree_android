package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.models.Configuration;
import com.kount.api.DataCollector;

public class KountDataCollector {

    private final BraintreeClient braintreeClient;
    private final DataCollector kountDataCollector;

    KountDataCollector(BraintreeClient braintreeClient) {
        this(braintreeClient, DataCollector.getInstance());
    }

    @VisibleForTesting
    KountDataCollector(BraintreeClient braintreeClient, DataCollector kountDataCollector) {
        this.braintreeClient = braintreeClient;
        this.kountDataCollector = kountDataCollector;
    }

    void startDataCollection(@NonNull Context context, @NonNull final String merchantId, @NonNull final String deviceSessionId, @NonNull final KountDataCollectorCallback callback) {
        braintreeClient.sendAnalyticsEvent("data-collector.kount.started");

        try {
            Class.forName(com.kount.api.DataCollector.class.getName());
        } catch (ClassNotFoundException | NoClassDefFoundError | NumberFormatException e) {
            braintreeClient.sendAnalyticsEvent("data-collector.kount.failed");

            Exception startError = new BraintreeException("Kount session failed to start.");
            callback.onResult(null, startError);
        }

        final Context applicationContext = context.getApplicationContext();
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {

                    // configure kount
                    kountDataCollector.setContext(applicationContext);
                    kountDataCollector.setMerchantID(Integer.parseInt(merchantId));
                    kountDataCollector.setLocationCollectorConfig(com.kount.api.DataCollector.LocationConfig.COLLECT);
                    kountDataCollector.setEnvironment(getDeviceCollectorEnvironment(configuration.getEnvironment()));

                    kountDataCollector.collectForSession(deviceSessionId, new DataCollector.CompletionHandler() {
                        @Override
                        public void completed(String kountSessionId) {
                            braintreeClient.sendAnalyticsEvent("data-collector.kount.succeeded");
                            callback.onResult(kountSessionId, null);
                        }

                        @Override
                        public void failed(String kountSessionId, DataCollector.Error error) {
                            braintreeClient.sendAnalyticsEvent("data-collector.kount.failed");
                            callback.onResult(kountSessionId, null);
                        }
                    });

                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    @VisibleForTesting
    static int getDeviceCollectorEnvironment(String environment) {
        if ("production".equalsIgnoreCase(environment)) {
            return com.kount.api.DataCollector.ENVIRONMENT_PRODUCTION;
        }
        return com.kount.api.DataCollector.ENVIRONMENT_TEST;
    }
}
