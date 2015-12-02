package com.paypal.android.networking.request;

import com.paypal.android.sdk.onetouch.core.base.ContextInspector;

/**
 * Interface for requests that need to talk to the configuration, or perform callbacks.
 */
public interface ServerRequestEnvironment {
    /**
     * Sends the ServerRequest to a handler to be processed on the Looper thread.
     *
     * @param request
     */
    void completeServerRequest(ServerRequest request);

    /**
     * Return the url configured for the input API. If the api is unspecified, return null.
     *
     * @param api
     * @return
     */
    String getUrl(ApiInfo api);

    /**
     * Returns the current environment name
     *
     * @return
     */
    String environmentName();

    /**
     * Returns the current environment base url
     *
     * @return String
     */
    String environmentBaseUrl();

    /**
     * Returns the context inspector (duh)
     *
     * @return
     */
    ContextInspector getContextInspector();
}
