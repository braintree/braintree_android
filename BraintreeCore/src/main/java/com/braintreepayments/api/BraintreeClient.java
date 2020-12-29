package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.AnalyticsEvent;
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

    private final String sessionId;
    private final BraintreeGraphQLHttpClient graphQLHttpClient;

    private final ConfigurationManager configurationManager;
    private final BrowserSwitchClient browserSwitchClient;

    private ManifestValidator manifestValidator;

    private CrashReporter crashReporter;

    // TODO: re-factor BraintreeClient to capture an ApplicationContext reference to help streamline the API
    public static BraintreeClient newInstance(Authorization authorization, String returnUrlScheme) {
        return new BraintreeClient(authorization, returnUrlScheme);
    }

    BraintreeClient(Authorization authorization, String returnUrlScheme) {
        this(authorization, returnUrlScheme, new BraintreeHttpClient(authorization));
    }

    private BraintreeClient(Authorization authorization, String returnUrlScheme, BraintreeHttpClient httpClient) {
        this(authorization, returnUrlScheme, httpClient, new ConfigurationManager(httpClient));
    }

    @VisibleForTesting
    BraintreeClient(Authorization authorization, String returnUrlScheme, BraintreeHttpClient httpClient, ConfigurationManager configurationManager) {
        this.httpClient = httpClient;
        this.authorization = authorization;
        this.configurationManager = configurationManager;

        this.analyticsClient = AnalyticsClient.newInstance();
        this.sessionId = UUIDHelper.getFormattedUUID();
        this.graphQLHttpClient = new BraintreeGraphQLHttpClient(authorization);
        this.browserSwitchClient = BrowserSwitchClient.newInstance(returnUrlScheme);
        this.manifestValidator = new ManifestValidator();

        this.crashReporter = new CrashReporter(this);
        this.crashReporter.start();
    }

    public void getConfiguration(Context context, ConfigurationCallback callback) {
        configurationManager.loadConfiguration(context, authorization, callback);
    }

    public void sendAnalyticsEvent(final Context context, final String eventFragment) {
        getConfiguration(context, new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    final AnalyticsEvent event = new AnalyticsEvent(context, sessionId, getIntegrationType(context), eventFragment);
                    analyticsClient.sendEvent(event, configuration, context);
                }
            }
        });
    }

    // TODO: use Jetpack WorkManager to schedule Analytics events for upload periodically in the background
    public void flushAnalyticsEvents(final Context context) {
        getConfiguration(context, new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    analyticsClient.flushAnalyticsEvents(context, configuration, authorization, httpClient);
                }
            }
        });
    }

    public void sendGET(final String url, final Context context, final HttpResponseCallback responseCallback) {
        getConfiguration(context, new ConfigurationCallback() {
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

    public void sendPOST(final String url, final String data, final Context context, final HttpResponseCallback responseCallback) {
        getConfiguration(context, new ConfigurationCallback() {
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

    public String getIntegrationType(Context context) {
        return IntegrationType.get(context);
    }

    public void sendGraphQLPOST(final String payload, Context context, final HttpResponseCallback responseCallback) {
        getConfiguration(context, new ConfigurationCallback() {
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

    public boolean isUrlSchemeDeclaredInAndroidManifest(Context context, String urlScheme, Class klass) {
        return manifestValidator.isUrlSchemeDeclaredInAndroidManifest(context, urlScheme, klass);
    }

    public ActivityInfo getManifestActivityInfo(Context context, Class klass) {
        return manifestValidator.getActivityInfo(context, klass);
    }

    void reportCrash() {
        String analyticsUrl = analyticsClient.getLastKnownAnalyticsUrl();
        if (analyticsUrl != null) {
            final AnalyticsEvent event = new AnalyticsEvent(null, sessionId, "crash", "crash");
            httpClient.post(analyticsUrl, event.toString(), null, new HttpNoResponse());
        }
    }

    public Authorization getAuthorization() {
        return authorization;
    }
}
