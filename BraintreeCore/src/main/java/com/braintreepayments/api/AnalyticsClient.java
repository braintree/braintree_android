package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import com.braintreepayments.api.internal.AnalyticsDatabase;
import com.braintreepayments.api.internal.AnalyticsEvent;
import com.braintreepayments.api.internal.AnalyticsIntentService;
import com.braintreepayments.api.internal.AnalyticsSender;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;

public class AnalyticsClient {

    public static AnalyticsClient newInstance() {
        return new AnalyticsClient();
    }

    private String lastKnownAnalyticsUrl;

    private AnalyticsClient() {}

    void sendEvent(AnalyticsEvent event, Configuration configuration, Context context) {
        if (configuration.getAnalytics().isEnabled()) {
            lastKnownAnalyticsUrl = configuration.getAnalytics().getUrl();

            Context applicationContext = context.getApplicationContext();
            AnalyticsDatabase db = AnalyticsDatabase.getInstance(applicationContext);
            db.addEvent(event);
        }
    }

    void flushAnalyticsEvents(Context context, Configuration configuration, Authorization authorization, BraintreeHttpClient httpClient) {
        if (configuration != null && configuration.toJson() != null && configuration.getAnalytics().isEnabled()) {

            Intent intent = new Intent(context, AnalyticsIntentService.class)
                    .putExtra(AnalyticsIntentService.EXTRA_AUTHORIZATION, authorization.toString())
                    .putExtra(AnalyticsIntentService.EXTRA_CONFIGURATION, configuration.toJson());

            Context applicationContext = context.getApplicationContext();
            try {
                applicationContext.startService(intent);
            } catch (RuntimeException e) {
                AnalyticsSender.send(context.getApplicationContext(), authorization, configuration, httpClient,
                        configuration.getAnalytics().getUrl(), false);
            }
        }
    }

    public String getLastKnownAnalyticsUrl() {
        return lastKnownAnalyticsUrl;
    }
}
