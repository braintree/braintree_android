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

    private static BraintreeClientParams createDefaultParams(Context context, String authString, ClientTokenProvider clientTokenProvider) {
        String returnUrlScheme = context
                .getApplicationContext()
                .getPackageName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree";

        String braintreeReturnUrlScheme = context
                .getApplicationContext()
                .getPackageName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree.deeplinkhandler";
        return createDefaultParams(context, authString, clientTokenProvider, returnUrlScheme, null, IntegrationType.CUSTOM, braintreeReturnUrlScheme);
    }

    private static BraintreeClientParams createDefaultParams(Context context, String authString, ClientTokenProvider clientTokenProvider, String returnUrlScheme) {
        return createDefaultParams(context, authString, clientTokenProvider, returnUrlScheme, null, IntegrationType.CUSTOM, null);
    }

    private static BraintreeClientParams createDefaultParams(Context context, String authString, ClientTokenProvider clientTokenProvider, String sessionId, @IntegrationType.Integration String integrationType) {
        String returnUrlScheme = context
                .getApplicationContext()
                .getPackageName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree";

        String braintreeReturnUrlScheme = context
                .getApplicationContext()
                .getPackageName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree.deeplinkhandler";

        return createDefaultParams(context, authString, clientTokenProvider, returnUrlScheme, sessionId, integrationType, braintreeReturnUrlScheme);
    }

    private static BraintreeClientParams createDefaultParams(Context context, String initialAuthString, ClientTokenProvider clientTokenProvider, String returnUrlScheme, String sessionId, @IntegrationType.Integration String integrationType, String braintreeReturnURLScheme) {
        AuthorizationLoader authorizationLoader =
                new AuthorizationLoader(initialAuthString, clientTokenProvider);

        BraintreeHttpClient httpClient = new BraintreeHttpClient();
        return new BraintreeClientParams()
                .authorizationLoader(authorizationLoader)
                .context(context)
                .setIntegrationType(integrationType)
                .sessionId(sessionId)
                .httpClient(httpClient)
                .returnUrlScheme(returnUrlScheme)
                .braintreeDeepLinkReturnUrlScheme(braintreeReturnURLScheme)
                .graphQLClient(new BraintreeGraphQLClient())
                .analyticsClient(new AnalyticsClient(context))
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
        this(createDefaultParams(context, authorization, null));
    }

    /**
     * Create a new instance of {@link BraintreeClient} using a {@link ClientTokenProvider}.
     *
     * @param context             Android Context
     * @param clientTokenProvider An implementation of {@link ClientTokenProvider} that {@link BraintreeClient} will use to fetch a client token on demand.
     */
    public BraintreeClient(@NonNull Context context, @NonNull ClientTokenProvider clientTokenProvider) {
        this(createDefaultParams(context, null, clientTokenProvider));
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
        this(createDefaultParams(context, authorization, null, returnUrlScheme));
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
        this(createDefaultParams(context, null, clientTokenProvider, returnUrlScheme));
    }

    BraintreeClient(@NonNull Context context, @NonNull ClientTokenProvider clientTokenProvider, @NonNull String sessionId, @NonNull @IntegrationType.Integration String integrationType) {
        this(createDefaultParams(context, null, clientTokenProvider, sessionId, integrationType));
    }

    BraintreeClient(@NonNull Context context, @NonNull String authorization, @NonNull String sessionId, @NonNull @IntegrationType.Integration String integrationType) {
        this(createDefaultParams(context, authorization, null, sessionId, integrationType));
    }

    @VisibleForTesting
    BraintreeClient(BraintreeClientParams params) {
        this.analyticsClient = params.getAnalyticsClient();
        this.applicationContext = params.getContext().getApplicationContext();
        this.authorizationLoader = params.getAuthorizationLoader();
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
        this.braintreeDeepLinkReturnUrlScheme = params.getBraintreeDeepLinkReturnUrlScheme();

        this.crashReporter = new CrashReporter(this);
        this.crashReporter.start();
    }

    /**
     * Retrieve Braintree configuration.
     *
     * @param callback {@link ConfigurationCallback}
     */
    public void getConfiguration(@NonNull final ConfigurationCallback callback) {
        getAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorizationResult(@Nullable Authorization authorization, @Nullable Exception error) {
                if (authorization != null) {
                    configurationLoader.loadConfiguration(applicationContext, authorization, new ConfigurationLoaderCallback() {
                        @Override
                        public void onResult(@Nullable ConfigurationLoaderResult result, @Nullable Exception error) {
                            if (result != null) {
                                callback.onResult(result.getConfiguration(), null);
                                if (result.getLoadFromCacheError() != null) {
                                    sendAnalyticsEvent("config cache loading failed");
                                }
                                if (result.getSaveToCacheError() != null) {
                                    sendAnalyticsEvent("config cache saving failed");
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
                            if (isAnalyticsEnabled(configuration)) {
                                analyticsClient.sendEvent(configuration, eventName, sessionId, getIntegrationType(), authorization);
                            }
                        }
                    });
                }
            }
        });
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
     *
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
     *
     * For web payment flows, this means launching the browser in a task separate from the calling activity.
     *
     * NOTE: When this property is set to true, all custom url schemes set in {@link BraintreeClient}
     * constructors will be ignored.
     *
     * @param launchesBrowserSwitchAsNewTask set to true to allow the SDK to capture deep links. This value is false by default.
     */
    public void launchesBrowserSwitchAsNewTask(boolean launchesBrowserSwitchAsNewTask) {
        this.launchesBrowserSwitchAsNewTask = launchesBrowserSwitchAsNewTask;
    }
}
