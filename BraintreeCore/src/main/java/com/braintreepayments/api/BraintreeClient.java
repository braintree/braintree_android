package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import java.util.Locale;

public class BraintreeClient {

    private final Authorization authorization;
    private final AnalyticsClient analyticsClient;
    private final BraintreeHttpClient httpClient;
    private final BraintreeGraphQLHttpClient graphQLHttpClient;
    private final BrowserSwitchClient browserSwitchClient;
    private final ConfigurationManager configurationManager;
    private final Context applicationContext;
    private final CrashReporter crashReporter;
    private final ManifestValidator manifestValidator;
    private final String sessionId;
    private final String integrationType;

    private static BraintreeClientParams createDefaultParams(Authorization authorization, Context context) {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(authorization);
        return new BraintreeClientParams()
                .authorization(authorization)
                .context(context)
                .setIntegrationType(IntegrationType.get(context))
                .sessionId(UUIDHelper.getFormattedUUID())
                .httpClient(httpClient)
                .graphQLHttpClient(new BraintreeGraphQLHttpClient(authorization))
                .analyticsClient(new AnalyticsClient(authorization))
                .browserSwitchClient(new BrowserSwitchClient())
                .manifestValidator(new ManifestValidator())
                .configurationManager(new ConfigurationManager(httpClient));
    }

    public BraintreeClient(Authorization authorization, Context context) {
        this(createDefaultParams(authorization, context));
    }

    @VisibleForTesting
    BraintreeClient(BraintreeClientParams params) {
        this.analyticsClient = params.getAnalyticsClient();
        this.applicationContext = params.getContext().getApplicationContext();
        this.authorization = params.getAuthorization();
        this.browserSwitchClient = params.getBrowserSwitchClient();
        this.configurationManager = params.getConfigurationManager();
        this.graphQLHttpClient = params.getGraphQLHttpClient();
        this.httpClient = params.getHttpClient();
        this.manifestValidator = params.getManifestValidator();
        this.sessionId = params.getSessionId();
        this.integrationType = params.getIntegrationType();

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
        return integrationType;
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

    public BrowserSwitchResult deliverBrowserSwitchResult(FragmentActivity activity) {
        return browserSwitchClient.deliverResult(activity);
    }

    String getReturnUrlScheme() {
        return applicationContext.getPackageName().toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree";
    }

    public boolean canPerformBrowserSwitch(FragmentActivity activity, @BraintreeRequestCodes int requestCode) {
        // url used to see if the application is able to open an https url e.g. web browser
        Uri url = Uri.parse("https://braintreepayments.com");
        String returnUrlScheme = getReturnUrlScheme();
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .url(url)
                .returnUrlScheme(returnUrlScheme)
                .requestCode(requestCode);
        boolean result = true;
        try {
            browserSwitchClient.assertCanPerformBrowserSwitch(activity, browserSwitchOptions);
        } catch (BrowserSwitchException e) {
            result = false;
        }
        return result;
    }

    public <T> boolean isUrlSchemeDeclaredInAndroidManifest(String urlScheme, Class<T> klass) {
        return manifestValidator.isUrlSchemeDeclaredInAndroidManifest(applicationContext, urlScheme, klass);
    }

    public ActivityInfo getManifestActivityInfo(Class klass) {
        return manifestValidator.getActivityInfo(applicationContext, klass);
    }

    // TODO: Remove application context dependency from AnalyticsEvent and unit test
    void reportCrash() {
        String analyticsUrl = analyticsClient.getLastKnownAnalyticsUrl();
        if (analyticsUrl != null) {
            final AnalyticsEvent event = new AnalyticsEvent(applicationContext, sessionId, "crash", "crash");
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
