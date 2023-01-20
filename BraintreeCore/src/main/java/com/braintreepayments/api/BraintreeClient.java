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

    private final AuthorizationLoader authorizationLoader;
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
    private final String braintreeDeepLinkReturnUrlScheme;

    private boolean launchesBrowserSwitchAsNewTask;

    /**
     * Create a new instance of {@link BraintreeClient} using a tokenization key or client token.
     *
     * @param context       Android Context
     * @param authorization The tokenization key or client token to use. If an invalid authorization is provided, a {@link BraintreeException} will be returned via callback.
     */
    public BraintreeClient(@NonNull Context context, @NonNull String authorization) {
        this(
                new BraintreeOptions(context, IntegrationType.CUSTOM)
                        .authorization(authorization)
        );
    }

    /**
     * Create a new instance of {@link BraintreeClient} using a {@link ClientTokenProvider}.
     *
     * @param context             Android Context
     * @param clientTokenProvider An implementation of {@link ClientTokenProvider} that {@link BraintreeClient} will use to fetch a client token on demand.
     */
    public BraintreeClient(@NonNull Context context, @NonNull ClientTokenProvider clientTokenProvider) {
        this(
                new BraintreeOptions(context, IntegrationType.CUSTOM)
                        .clientTokenProvider(clientTokenProvider)
        );
    }

    /**
     * Create a new instance of {@link BraintreeClient} using a tokenization key or client token and a custom url scheme.
     * <p>
     * This constructor should only be used for applications with multiple activities and multiple supported return url schemes.
     * This can be helpful for integrations using Drop-in and BraintreeClient to avoid deep linking collisions, since
     * Drop-in uses the same custom url scheme as the default BraintreeClient constructor.
     *
     * @param context         Android Context
     * @param authorization   The tokenization key or client token to use. If an invalid authorization is provided, a {@link BraintreeException} will be returned via callback.
     * @param returnUrlScheme A custom return url to use for browser and app switching
     */
    public BraintreeClient(@NonNull Context context, @NonNull String authorization, @NonNull String returnUrlScheme) {
        this(
                new BraintreeOptions(context, IntegrationType.CUSTOM)
                        .authorization(authorization)
                        .returnUrlScheme(returnUrlScheme)
        );
    }

    /**
     * Create a new instance of {@link BraintreeClient} using a {@link ClientTokenProvider} and a custom url scheme.
     * <p>
     * This constructor should only be used for applications with multiple activities and multiple supported return url schemes.
     * This can be helpful for integrations using Drop-in and BraintreeClient to avoid deep linking collisions, since
     * Drop-in uses the same custom url scheme as the default BraintreeClient constructor.
     *
     * @param context             Android Context
     * @param clientTokenProvider An implementation of {@link ClientTokenProvider} that {@link BraintreeClient} will use to fetch a client token on demand.
     * @param returnUrlScheme     A custom return url to use for browser and app switching
     */
    public BraintreeClient(@NonNull Context context, @NonNull ClientTokenProvider clientTokenProvider, @NonNull String returnUrlScheme) {
        this(
                new BraintreeOptions(context, IntegrationType.CUSTOM)
                        .clientTokenProvider(clientTokenProvider)
                        .returnUrlScheme(returnUrlScheme)
        );
    }

    BraintreeClient(@NonNull Context context, @NonNull ClientTokenProvider clientTokenProvider, @NonNull String sessionId, @NonNull @IntegrationType.Integration String integrationType) {
        this(
                new BraintreeOptions(context, integrationType)
                        .clientTokenProvider(clientTokenProvider)
                        .sessionId(sessionId)
        );
    }

    BraintreeClient(@NonNull Context context, @NonNull String authorization, @NonNull String sessionId, @NonNull @IntegrationType.Integration String integrationType) {
        this(
                new BraintreeOptions(context, integrationType)
                        .authorization(authorization)
                        .sessionId(sessionId)
        );
    }

    // NEXT MAJOR VERSION: Externalize BraintreeOptions allow additional allow parameters to grow
    // over time without having to make new constructors
    BraintreeClient(@NonNull BraintreeOptions options) {
        this(BraintreeClientParams.from(options));
    }

    @VisibleForTesting
    BraintreeClient(BraintreeClientParams params) {
        analyticsClient = params.getAnalyticsClient();
        applicationContext = params.getContext().getApplicationContext();
        authorizationLoader = params.getAuthorizationLoader();
        browserSwitchClient = params.getBrowserSwitchClient();
        configurationLoader = params.getConfigurationLoader();
        graphQLClient = params.getGraphQLClient();
        httpClient = params.getHttpClient();
        manifestValidator = params.getManifestValidator();

        sessionId = params.getSessionId();
        integrationType = params.getIntegrationType();
        returnUrlScheme = params.getReturnUrlScheme();

        // NEXT_MAJOR_VERSION: Formalize capitalization of URL, HTTP etc. via style guide and enforce
        // capitalization e.g. braintreeDeepLinkReturnURLScheme
        braintreeDeepLinkReturnUrlScheme = applicationContext
                .getApplicationContext()
                .getPackageName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree.deeplinkhandler";

        crashReporter = new CrashReporter(this);
        crashReporter.start();
    }

    /**
     * Retrieve Braintree configuration.
     *
     * @param callback {@link ConfigurationCallback}
     */
    public void getConfiguration(@NonNull final ConfigurationCallback callback) {
        getAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorizationResult(@Nullable final Authorization authorization, @Nullable Exception error) {
                if (authorization != null) {
                    configurationLoader.loadConfiguration(authorization, new ConfigurationLoaderCallback() {
                        @Override
                        public void onResult(@Nullable ConfigurationLoaderResult result, @Nullable Exception error) {
                            if (result != null) {
                                Configuration configuration = result.getConfiguration();
                                callback.onResult(configuration, null);

                                if (result.getLoadFromCacheError() != null) {
                                    sendAnalyticsEvent("configuration.cache.load.failed", configuration, authorization);
                                }
                                if (result.getSaveToCacheError() != null) {
                                    sendAnalyticsEvent("configuration.cache.save.failed", configuration, authorization);
                                }

                            } else {
                                callback.onResult(null, error);
                            }
                        }
                    });
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    void getAuthorization(@NonNull final AuthorizationCallback callback) {
        authorizationLoader.loadAuthorization(callback);
    }

    void sendAnalyticsEvent(final String eventName) {
        getAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorizationResult(@Nullable final Authorization authorization, @Nullable Exception error) {
                if (authorization != null) {
                    getConfiguration(new ConfigurationCallback() {
                        @Override
                        public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                            sendAnalyticsEvent(eventName, configuration, authorization);
                        }
                    });
                }
            }
        });
    }

    private void sendAnalyticsEvent(String eventName, Configuration configuration, Authorization authorization) {
        if (isAnalyticsEnabled(configuration)) {
            analyticsClient.sendEvent(configuration, eventName, sessionId, getIntegrationType(), authorization);
        }
    }

    void sendGET(final String url, final HttpResponseCallback responseCallback) {
        getAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorizationResult(@Nullable final Authorization authorization, @Nullable Exception authError) {
                if (authorization != null) {
                    getConfiguration(new ConfigurationCallback() {
                        @Override
                        public void onResult(@Nullable Configuration configuration, @Nullable Exception configError) {
                            if (configuration != null) {
                                httpClient.get(url, configuration, authorization, responseCallback);
                            } else {
                                responseCallback.onResult(null, configError);
                            }
                        }
                    });
                } else {
                    responseCallback.onResult(null, authError);
                }
            }
        });
    }

    void sendPOST(final String url, final String data, final HttpResponseCallback responseCallback) {
        getAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorizationResult(@Nullable final Authorization authorization, @Nullable Exception authError) {
                if (authorization != null) {
                    getConfiguration(new ConfigurationCallback() {
                        @Override
                        public void onResult(@Nullable Configuration configuration, @Nullable Exception configError) {
                            if (configuration != null) {
                                httpClient.post(url, data, configuration, authorization, responseCallback);
                            } else {
                                responseCallback.onResult(null, configError);
                            }
                        }
                    });
                } else {
                    responseCallback.onResult(null, authError);
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
        getAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorizationResult(@Nullable final Authorization authorization, @Nullable Exception authError) {
                if (authorization != null) {
                    getConfiguration(new ConfigurationCallback() {
                        @Override
                        public void onResult(@Nullable Configuration configuration, @Nullable Exception configError) {
                            if (configuration != null) {
                                graphQLClient.post(payload, configuration, authorization, responseCallback);
                            } else {
                                responseCallback.onResult(null, configError);
                            }
                        }
                    });
                } else {
                    responseCallback.onResult(null, authError);
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

    BrowserSwitchResult getBrowserSwitchResultFromCache(@NonNull Context context) {
        return browserSwitchClient.getResultFromCache(context);
    }

    BrowserSwitchResult deliverBrowserSwitchResultFromCache(@NonNull Context context) {
        return browserSwitchClient.deliverResultFromCache(context);
    }

    String getReturnUrlScheme() {
        if (launchesBrowserSwitchAsNewTask) {
            return braintreeDeepLinkReturnUrlScheme;
        }
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

    void reportCrash() {
        Authorization authorization = authorizationLoader.getAuthorizationFromCache();
        analyticsClient.reportCrash(applicationContext, sessionId, integrationType, authorization);
    }

    static boolean isAnalyticsEnabled(Configuration configuration) {
        return configuration != null && configuration.isAnalyticsEnabled();
    }

    Context getApplicationContext() {
        return applicationContext;
    }

    /**
     * For clients using a {@link ClientTokenProvider}, call this method to invalidate the existing,
     * cached client token. A new client token will be fetched by the SDK when it is needed.
     * <p>
     * For clients not using a {@link ClientTokenProvider}, this method does nothing.
     */
    public void invalidateClientToken() {
        authorizationLoader.invalidateClientToken();
    }

    boolean launchesBrowserSwitchAsNewTask() {
        return launchesBrowserSwitchAsNewTask;
    }

    /**
     * Set this property to true to allow the SDK to handle deep links on behalf of the host
     * application for browser switched flows.
     * <p>
     * For web payment flows, this means launching the browser in a task separate from the calling activity.
     * <p>
     * NOTE: When this property is set to true, all custom url schemes set in {@link BraintreeClient}
     * constructors will be ignored.
     *
     * @param launchesBrowserSwitchAsNewTask set to true to allow the SDK to capture deep links. This value is false by default.
     */
    public void launchesBrowserSwitchAsNewTask(boolean launchesBrowserSwitchAsNewTask) {
        this.launchesBrowserSwitchAsNewTask = launchesBrowserSwitchAsNewTask;
    }
}
