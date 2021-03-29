package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import java.util.Locale;

/**
 * Core Braintree class that handles network requests.
 */
public class BraintreeClient {

    private final String authString;
    private final AnalyticsClient analyticsClient;
    private final BraintreeHttpClient httpClient;
    private final BraintreeGraphQLHttpClient graphQLHttpClient;
    private final BrowserSwitchClient browserSwitchClient;
    private final ConfigurationLoader configurationLoader;
    private final Context applicationContext;
    private final CrashReporter crashReporter;
    private final ManifestValidator manifestValidator;
    private final String sessionId;
    private final String integrationType;
    private final AuthorizationParser authorizationParser;
    private Authorization authorization;

    private static BraintreeClientParams createDefaultParams(String authorization, Context context) {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();
        return new BraintreeClientParams()
                .authorization(authorization)
                .context(context)
                .setIntegrationType(IntegrationType.get(context))
                .sessionId(UUIDHelper.getFormattedUUID())
                .httpClient(httpClient)
                .graphQLHttpClient(new BraintreeGraphQLHttpClient())
                .analyticsClient(new AnalyticsClient())
                .browserSwitchClient(new BrowserSwitchClient())
                .manifestValidator(new ManifestValidator())
                .configurationLoader(new ConfigurationLoader(httpClient))
                .authorizationParser(new AuthorizationParser());
    }

    public BraintreeClient(String authString, Context context) {
        this(createDefaultParams(authString, context));
    }

    @VisibleForTesting
    BraintreeClient(BraintreeClientParams params) {
        this.analyticsClient = params.getAnalyticsClient();
        this.applicationContext = params.getContext().getApplicationContext();
        this.authString = params.getAuthorization();
        this.browserSwitchClient = params.getBrowserSwitchClient();
        this.configurationLoader = params.getConfigurationLoader();
        this.graphQLHttpClient = params.getGraphQLHttpClient();
        this.httpClient = params.getHttpClient();
        this.manifestValidator = params.getManifestValidator();
        this.sessionId = params.getSessionId();
        this.integrationType = params.getIntegrationType();
        this.authorizationParser = params.getAuthorizationParser();

        this.crashReporter = new CrashReporter(this);
        this.crashReporter.start();
    }

    /**
     * Retrieve Braintree configuration.
     * @param callback {@link ConfigurationCallback}
     */
    public void getConfiguration(ConfigurationCallback callback) {
        if (authorization == null) {
            try {
                authorization = authorizationParser.parse(authString);
            } catch (InvalidArgumentException e) {
                callback.onResult(null, new InvalidArgumentException("Tokenization Key or client token was invalid."));
            }
        }
        configurationLoader.loadConfiguration(applicationContext, authorization, callback);
    }

    void sendAnalyticsEvent(final String eventFragment) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (isAnalyticsEnabled(configuration)) {
                    final AnalyticsEvent event = new AnalyticsEvent(applicationContext, sessionId, getIntegrationType(), eventFragment);
                    analyticsClient.sendEvent(authorization, event, configuration, applicationContext);
                }
            }
        });
    }

    void sendGET(final String url, final HttpResponseCallback responseCallback) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    httpClient.get(url, configuration, responseCallback, authorization);
                } else {
                    responseCallback.failure(error);
                }
            }
        });
    }

    void sendPOST(final String url, final String data, final HttpResponseCallback responseCallback) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    httpClient.post(url, data, configuration, responseCallback, authorization);
                } else {
                    responseCallback.failure(error);
                }
            }
        });
    }

    String getSessionId() {
        return sessionId;
    }

    String getIntegrationType() {
        return integrationType;
    }

    void sendGraphQLPOST(final String payload, final HttpResponseCallback responseCallback) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    graphQLHttpClient.post(payload, configuration, responseCallback, authorization);
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
        if (applicationContext != null) {
            return applicationContext.getPackageName().toLowerCase(Locale.ROOT)
                    .replace("_", "") + ".braintree";
        }
        return null;
    }

    boolean canPerformBrowserSwitch(FragmentActivity activity, @BraintreeRequestCodes int requestCode) {
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

    <T> boolean isUrlSchemeDeclaredInAndroidManifest(String urlScheme, Class<T> klass) {
        return manifestValidator.isUrlSchemeDeclaredInAndroidManifest(applicationContext, urlScheme, klass);
    }

    ActivityInfo getManifestActivityInfo(Class klass) {
        return manifestValidator.getActivityInfo(applicationContext, klass);
    }

    // TODO: Remove application context dependency from AnalyticsEvent and unit test
    void reportCrash() {
        String analyticsUrl = analyticsClient.getLastKnownAnalyticsUrl();
        if (analyticsUrl != null && authorization != null) {
            final AnalyticsEvent event = new AnalyticsEvent(applicationContext, sessionId, "crash", "crash");
            httpClient.post(analyticsUrl, event.toString(), null, new HttpNoResponse(), authorization);
        }
    }

    static boolean isAnalyticsEnabled(Configuration configuration) {
        return configuration != null && configuration.isAnalyticsEnabled();
    }

    // TODO: Investigate how to get rid of this accessor
    Authorization getAuthorization() {
        if (authorization == null) {
            try {
                authorization = Authorization.fromString(authString);
            } catch (InvalidArgumentException ignored) { }
        }
        return authorization;
    }
}
