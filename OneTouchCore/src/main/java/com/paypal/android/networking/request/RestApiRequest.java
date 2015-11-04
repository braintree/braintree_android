package com.paypal.android.networking.request;

import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;

import android.text.TextUtils;

public abstract class RestApiRequest extends ServerRequest {
    private static final String TAG = RestApiRequest.class.getSimpleName();


    protected static final String HEADER_CONTENT_TYPE = "Content-Type";
    protected static final String HEADER_ACCEPT = "Accept";
    protected static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    public RestApiRequest(ApiInfo apiInfo, ServerRequestEnvironment env,
                          CoreEnvironment coreEnv, String auth) {
        this(apiInfo, env, coreEnv, auth, null);
    }

	public RestApiRequest(ApiInfo apiInfo, ServerRequestEnvironment env,
            CoreEnvironment coreEnv, String auth,
            String urlSuffix) {
		super(apiInfo, env, coreEnv, urlSuffix);

        if(!TextUtils.isEmpty(auth)) {
            // not all requests have auth, such as FPTI
            putHeader(HEADER_AUTHORIZATION, auth);
        }
    }

}
