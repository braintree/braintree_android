package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeGraphQLHttpClient;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.internal.HttpNoResponse;
import com.braintreepayments.api.internal.IntegrationType;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.internal.UUIDHelper;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;

// TODO: unit test when API is finalized
public class BraintreeClient {

    private final Authorization authorization;
    private final AnalyticsClient analyticsClient;
    private final BraintreeHttpClient httpClient;
    private final Context applicationContext;

    private final String sessionId;
    private final BraintreeGraphQLHttpClient graphQLHttpClient;

    private final ConfigurationManager configurationManager;
    private final BrowserSwitchClient browserSwitchClient;

    private ManifestValidator manifestValidator;

    private CrashReporter crashReporter;

    public BraintreeClient(Authorization authorization, Context context, String returnUrlScheme) {
        this(authorization, context, returnUrlScheme, new BraintreeHttpClient(authorization), new BraintreeGraphQLHttpClient(authorization));
    }

    private BraintreeClient(Authorization authorization, Context context, String returnUrlScheme, BraintreeHttpClient httpClient, BraintreeGraphQLHttpClient graphQLHttpClient) {
        this(authorization, context, returnUrlScheme, httpClient, graphQLHttpClient, new ConfigurationManager(httpClient), new AnalyticsClient(authorization), UUIDHelper.getFormattedUUID());
    }

    @VisibleForTesting
    BraintreeClient(Authorization authorization, Context context, String returnUrlScheme, BraintreeHttpClient httpClient, BraintreeGraphQLHttpClient graphQLHttpClient, ConfigurationManager configurationManager, AnalyticsClient analyticsClient, String sessionId) {
        this.httpClient = httpClient;
        this.authorization = authorization;
        this.applicationContext = context.getApplicationContext();
        this.configurationManager = configurationManager;

        this.analyticsClient = analyticsClient;
        this.sessionId = sessionId;
        this.graphQLHttpClient = graphQLHttpClient;
        this.browserSwitchClient = BrowserSwitchClient.newInstance(returnUrlScheme);
        this.manifestValidator = new ManifestValidator();

        this.crashReporter = new CrashReporter(this);
        this.crashReporter.start();
    }

    public void getConfiguration(ConfigurationCallback callback) {
        configurationManager.loadConfiguration(applicationContext, authorization, callback);
    }

    public void sendAnalyticsEvent(final String eventFragment) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (isAnalyticsEnabled(configuration)) {
                    final AnalyticsEvent event = new AnalyticsEvent(applicationContext, sessionId, getIntegrationType(), eventFragment);
                    analyticsClient.sendEvent(event, configuration, applicationContext);
                }
            }
        });
    }

    public void sendGET(final String url, final HttpResponseCallback responseCallback) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    httpClient.get(url, configuration, responseCallback);
                } else {
                    responseCallback.failure(error);
                }
            }
        });
    }

    public void sendPOST(final String url, final String data, final HttpResponseCallback responseCallback) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    httpClient.post(url, data, configuration, responseCallback);
                } else {
                    responseCallback.failure(error);
                }
            }
        });
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getIntegrationType() {
        return IntegrationType.get(applicationContext);
    }

    public void sendGraphQLPOST(final String payload, final HttpResponseCallback responseCallback) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    graphQLHttpClient.post(payload, configuration, responseCallback);
                } else {
                    responseCallback.failure(error);
                }
            }
        });
    }

    void startBrowserSwitch(FragmentActivity activity, BrowserSwitchOptions browserSwitchOptions) throws BrowserSwitchException {
        if (browserSwitchClient != null) {
            browserSwitchClient.start(activity, browserSwitchOptions);
        }
    }

    void deliverBrowserSwitchResult(FragmentActivity activity) {
        if (browserSwitchClient != null) {
            browserSwitchClient.deliverResult(activity);
        }
    }

    public boolean canPerformBrowserSwitch(FragmentActivity activity, @BraintreeRequestCodes int requestCode) {
        String url = String.format("%s://test", browserSwitchClient.getReturnUrlScheme());
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .url(Uri.parse(url))
                .requestCode(requestCode);
        boolean result = true;
        try {
            browserSwitchClient.assertCanPerformBrowserSwitch(activity, browserSwitchOptions);
        } catch (BrowserSwitchException e) {
            result = false;
        }
        return result;
    }

    public boolean isUrlSchemeDeclaredInAndroidManifest(String urlScheme, Class klass) {
        return manifestValidator.isUrlSchemeDeclaredInAndroidManifest(applicationContext, urlScheme, klass);
    }

    void reportCrash() {
        String analyticsUrl = analyticsClient.getLastKnownAnalyticsUrl();
        if (analyticsUrl != null) {
            final AnalyticsEvent event = new AnalyticsEvent(null, sessionId, "crash", "crash");
            httpClient.post(analyticsUrl, event.toString(), null, new HttpNoResponse());
        }
    }

    private static boolean isAnalyticsEnabled(Configuration configuration) {
        return configuration != null && configuration.getAnalytics() != null && configuration.getAnalytics().isEnabled();
    }

    public Authorization getAuthorization() {
        return authorization;
    }
}
