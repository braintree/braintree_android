package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import java.util.Locale;

/**
 * Core Braintree class that handles network requests.
 */
public class BraintreeClient {

    private final Authorization authorization;
    private final AnalyticsClient analyticsClient;
    private final BraintreeHttpClient httpClient;
    private final BraintreeGraphQLClient graphQLClient;
    private final BrowserSwitchClient browserSwitchClient;
    private final ConfigurationLoader configurationLoader;
    private final Context applicationContext;
    private final CrashReporter crashReporter;
    private final ManifestValidator manifestValidator;
    private final String sessionId;
    private final String integrationType;
    private final String returnUrlScheme;

    private static BraintreeClientParams createDefaultParams(Context context, String authString) {
        String returnUrlScheme = context
                .getApplicationContext()
                .getPackageName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree";
        return createDefaultParams(context, authString, returnUrlScheme, null, IntegrationType.CUSTOM);
    }

    private static BraintreeClientParams createDefaultParams(Context context, String authString, String returnUrlScheme) {
        return createDefaultParams(context, authString, returnUrlScheme, null, IntegrationType.CUSTOM);
    }

    private static BraintreeClientParams createDefaultParams(Context context, String authString, String sessionId, @IntegrationType.Integration String integrationType) {
        String returnUrlScheme = context
                .getApplicationContext()
                .getPackageName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree";
        return createDefaultParams(context, authString, returnUrlScheme, sessionId, integrationType);
    }

    private static BraintreeClientParams createDefaultParams(Context context, String authString, String returnUrlScheme, String sessionId, @IntegrationType.Integration String integrationType) {
        Authorization authorization = Authorization.fromString(authString);
        BraintreeHttpClient httpClient = new BraintreeHttpClient(authorization);
        return new BraintreeClientParams()
                .authorization(authorization)
                .context(context)
                .setIntegrationType(integrationType)
                .sessionId(sessionId)
                .httpClient(httpClient)
                .returnUrlScheme(returnUrlScheme)
                .graphQLClient(new BraintreeGraphQLClient(authorization))
                .analyticsClient(new AnalyticsClient(authorization))
                .browserSwitchClient(new BrowserSwitchClient())
                .manifestValidator(new ManifestValidator())
                .UUIDHelper(new UUIDHelper())
                .configurationLoader(new ConfigurationLoader(httpClient));
    }

    /**
     * Create a new instance of {@link BraintreeClient} using a tokenization key or client token.
     *
     * @param context       Android Context
     * @param authorization The tokenization key or client token to use. If an invalid authorization is provided, a {@link BraintreeException} will be returned via callback.
     */
    public BraintreeClient(@NonNull Context context, @NonNull String authorization) {
        this(createDefaultParams(context, authorization));
    }

    /**
     * Create a new instance of {@link BraintreeClient} using a tokenization key or client token and a custom url scheme.
     *
     * This constructor should only be used for applications with multiple activities and multiple supported return url schemes.
     * This can be helpful for integrations using Drop-in and BraintreeClient to avoid deep linking collisions, since
     * Drop-in uses the same custom url scheme as the default BraintreeClient constructor.
     *
     * @param context         Android Context
     * @param authorization   The tokenization key or client token to use. If an invalid authorization is provided, a {@link BraintreeException} will be returned via callback.
     * @param returnUrlScheme A custom return url to use for browser and app switching
     */
    public BraintreeClient(@NonNull Context context, @NonNull String authorization, @NonNull String returnUrlScheme) {
        this(createDefaultParams(context, authorization, returnUrlScheme));
    }

    BraintreeClient(@NonNull Context context, @NonNull String authorization, @NonNull String sessionId, @NonNull @IntegrationType.Integration String integrationType) {
        this(createDefaultParams(context, authorization, sessionId, integrationType));
    }

    @VisibleForTesting
    BraintreeClient(BraintreeClientParams params) {
        this.analyticsClient = params.getAnalyticsClient();
        this.applicationContext = params.getContext().getApplicationContext();
        this.authorization = params.getAuthorization();
        this.browserSwitchClient = params.getBrowserSwitchClient();
        this.configurationLoader = params.getConfigurationLoader();
        this.graphQLClient = params.getGraphQLClient();
        this.httpClient = params.getHttpClient();
        this.manifestValidator = params.getManifestValidator();

        String sessionId = params.getSessionId();
        if (sessionId == null) {
            sessionId = params.getUUIDHelper().getFormattedUUID();
        }
        this.sessionId = sessionId;
        this.integrationType = params.getIntegrationType();
        this.returnUrlScheme = params.getReturnUrlScheme();

        this.crashReporter = new CrashReporter(this);
        this.crashReporter.start();
    }

    /**
     * Retrieve Braintree configuration.
     *
     * @param callback {@link ConfigurationCallback}
     */
    public void getConfiguration(@NonNull ConfigurationCallback callback) {
        configurationLoader.loadConfiguration(applicationContext, authorization, callback);
    }

    void sendAnalyticsEvent(final String eventFragment) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (isAnalyticsEnabled(configuration)) {
                    final AnalyticsEvent event = new AnalyticsEvent(applicationContext, sessionId, getIntegrationType(), eventFragment);
                    analyticsClient.sendEvent(applicationContext, configuration, event);
                }
            }
        });
    }

    void sendGET(final String url, final HttpResponseCallback responseCallback) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    httpClient.get(url, configuration, responseCallback);
                } else {
                    responseCallback.onResult(null, error);
                }
            }
        });
    }

    void sendPOST(final String url, final String data, final HttpResponseCallback responseCallback) {
        getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    httpClient.post(url, data, configuration, responseCallback);
                } else {
                    responseCallback.onResult(null, error);
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
                    graphQLClient.post(payload, configuration, responseCallback);
                } else {
                    responseCallback.onResult(null, error);
                }
            }
        });
    }

    void startBrowserSwitch(FragmentActivity activity, BrowserSwitchOptions browserSwitchOptions) throws BrowserSwitchException {
        if (browserSwitchClient != null) {
            browserSwitchClient.start(activity, browserSwitchOptions);
        }
    }

    BrowserSwitchResult getBrowserSwitchResult(@NonNull FragmentActivity activity) {
        return browserSwitchClient.getResult(activity);
    }

    public BrowserSwitchResult deliverBrowserSwitchResult(@NonNull FragmentActivity activity) {
        return browserSwitchClient.deliverResult(activity);
    }

    String getReturnUrlScheme() {
        return returnUrlScheme;
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

    <T> ActivityInfo getManifestActivityInfo(Class<T> klass) {
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

    static boolean isAnalyticsEnabled(Configuration configuration) {
        return configuration != null && configuration.isAnalyticsEnabled();
    }

    Authorization getAuthorization() {
        return authorization;
    }

    Context getApplicationContext() {
        return applicationContext;
    }
}
