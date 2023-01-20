package com.braintreepayments.api;

import android.content.Context;

import java.util.Locale;

class BraintreeClientParams {

    private AuthorizationLoader authorizationLoader;
    private AnalyticsClient analyticsClient;
    private BraintreeHttpClient httpClient;
    private Context context;

    private String sessionId;
    private String integrationType;
    private BraintreeGraphQLClient graphQLClient;

    private String returnUrlScheme;
    private ConfigurationLoader configurationLoader;
    private BrowserSwitchClient browserSwitchClient;
    private ManifestValidator manifestValidator;
    private UUIDHelper uuidHelper;

    private static String createDefaultReturnUrlScheme(Context context) {
        return context
                .getApplicationContext()
                .getPackageName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree";
    }

    private static String createBraintreeReturnUrlScheme(Context context) {
        return context
                .getApplicationContext()
                .getPackageName()
                .toLowerCase(Locale.ROOT)
                .replace("_", "") + ".braintree.deeplinkhandler";
    }

    static BraintreeClientParams from(BraintreeOptions options) {

        Context context = options.getContext();
        AuthorizationLoader authorizationLoader =
            new AuthorizationLoader(options.getAuthorization(), options.getClientTokenProvider());

        String sessionId = options.getSessionId();
        if (sessionId == null) {
            UUIDHelper uuidHelper = new UUIDHelper();
            sessionId = uuidHelper.getFormattedUUID();
        }

        String returnUrlScheme = options.getReturnUrlScheme();
        if (returnUrlScheme == null) {
            returnUrlScheme = createDefaultReturnUrlScheme(context);
        }

        BraintreeHttpClient httpClient = new BraintreeHttpClient();
        return new BraintreeClientParams()
                .authorizationLoader(authorizationLoader)
                .context(context)
                .setIntegrationType(options.getIntegrationType())
                .sessionId(sessionId)
                .httpClient(httpClient)
                .returnUrlScheme(returnUrlScheme)
                .graphQLClient(new BraintreeGraphQLClient())
                .analyticsClient(new AnalyticsClient(context))
                .browserSwitchClient(new BrowserSwitchClient())
                .manifestValidator(new ManifestValidator())
                .UUIDHelper(new UUIDHelper())
                .configurationLoader(new ConfigurationLoader(context, httpClient));
    }

    AuthorizationLoader getAuthorizationLoader() {
        return authorizationLoader;
    }

    BraintreeClientParams authorizationLoader(AuthorizationLoader authorizationLoader) {
        this.authorizationLoader = authorizationLoader;
        return this;
    }

    AnalyticsClient getAnalyticsClient() {
        return analyticsClient;
    }

    BraintreeClientParams analyticsClient(AnalyticsClient analyticsClient) {
        this.analyticsClient = analyticsClient;
        return this;
    }

    BraintreeHttpClient getHttpClient() {
        return httpClient;
    }

    BraintreeClientParams httpClient(BraintreeHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    Context getContext() {
        return context;
    }

    BraintreeClientParams context(Context context) {
        this.context = context;
        return this;
    }

    String getSessionId() {
        return sessionId;
    }

    BraintreeClientParams sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    BraintreeGraphQLClient getGraphQLClient() {
        return graphQLClient;
    }

    BraintreeClientParams graphQLClient(BraintreeGraphQLClient graphQLClient) {
        this.graphQLClient = graphQLClient;
        return this;
    }

    ConfigurationLoader getConfigurationLoader() {
        return configurationLoader;
    }

    BraintreeClientParams configurationLoader(ConfigurationLoader configurationLoader) {
        this.configurationLoader = configurationLoader;
        return this;
    }

    BrowserSwitchClient getBrowserSwitchClient() {
        return browserSwitchClient;
    }

    BraintreeClientParams browserSwitchClient(BrowserSwitchClient browserSwitchClient) {
        this.browserSwitchClient = browserSwitchClient;
        return this;
    }

    ManifestValidator getManifestValidator() {
        return manifestValidator;
    }

    BraintreeClientParams manifestValidator(ManifestValidator manifestValidator) {
        this.manifestValidator = manifestValidator;
        return this;
    }

    UUIDHelper getUUIDHelper() {
        return uuidHelper;
    }

    BraintreeClientParams UUIDHelper(UUIDHelper uuidHelper) {
        this.uuidHelper = uuidHelper;
        return this;
    }

    String getIntegrationType() {
        return integrationType;
    }

    BraintreeClientParams setIntegrationType(String integrationType) {
        this.integrationType = integrationType;
        return this;
    }

    String getReturnUrlScheme() {
        return returnUrlScheme;
    }

    BraintreeClientParams returnUrlScheme(String returnUrlScheme) {
        this.returnUrlScheme = returnUrlScheme;
        return this;
    }
}
