package com.paypal.android.networking.request;

import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;
import com.paypal.android.networking.events.ErrorBase;
import com.paypal.android.networking.events.RequestError;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

@SuppressWarnings("nls")
public abstract class ServerRequest {
    private static final String TAG = ServerRequest.class.getSimpleName();

    private static long sNextSerialNumber = 1;

    /**
     * request header info generally setup by the ComputeRequest override and
     * used by the execute method
     */
    private final Map<String, String> mRequestHeaders;

    private final ServerRequestEnvironment mServerRequestEnvironment;
    private final CoreEnvironment mCoreEnvironment;

    private final ApiInfo mApiInfo;
    private final String mUrlSuffix;

    private final long mSerialNumber;

    private String mRequest; // String sent to the server
    private String mReply; // String received from the server

    /**
     * An mError from this request.
     */
    private ErrorBase mError;

    /**
     * The http status code from the response, or null if not provided or
     * processed yet.
     */
    private Integer httpStatusCode;

    /**
     * returned in PayPal-Debug-Id header
     */
    private String mPayPalDebugId;

    public ServerRequest(ApiInfo apiInfo, ServerRequestEnvironment env,
                         CoreEnvironment coreEnv) {
        this(apiInfo, env, coreEnv, null);
    }

    public ServerRequest(ApiInfo apiInfo, ServerRequestEnvironment env,
            CoreEnvironment coreEnv, String urlSuffix) {
        this.mSerialNumber = getNextSerialNumber();
        this.mApiInfo = apiInfo;
        this.mUrlSuffix = urlSuffix;
        this.mServerRequestEnvironment = env;
        this.mCoreEnvironment = coreEnv;
        this.mRequestHeaders = new LinkedHashMap<>();
    }

    public static long getNextSerialNumber() {
        return sNextSerialNumber++;
    }

    public CoreEnvironment getCoreEnvironment() {
        return mCoreEnvironment;
    }

    public String getComputedRequest() {
        return mRequest;
    }

    public void setComputedRequest(String s) {
        mRequest = s;
    }

    public String getServerReply() {
        return mReply;
    }

    public void setServerReply(String serverReply) {
        mReply = serverReply;
    }

    public void setPayPalDebugId(String id) {
        mPayPalDebugId = id;
    }

    /**
     * @return API this request implements
     */
    public final ApiInfo getApiInfo() {
        return mApiInfo;
    }

    public Map<String, String> getRequestHeaders() {
        return mRequestHeaders;
    }

    public String getPayPalDebugId() {
        return mPayPalDebugId;
    }

    /**
     * Adds a request header to the request.
     *
     * @param name
     * @param value
     */
    protected void putHeader(String name, String value) {
        mRequestHeaders.put(name, value);
    }

    /**
     * Returns the request string.
     *
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    public abstract String computeRequest() throws JSONException, UnsupportedEncodingException;

    /**
     * Execute something before this request is sent
     */
    public void preExecute(){
        // do nothing by default
    }

    /**
     * Execute something after this request is sent
     */
    public void postExecute(){
        // do nothing by default
    }

    /**
     * This method is called for 2xx http responses.
     *
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws NullPointerException
     * @throws JSONException
     */
    public abstract void parse() throws ParserConfigurationException, SAXException, JSONException;

    /**
     * Same as parse(), only called on non-2xx responses
     *
     * @throws JSONException
     */
    public abstract void parseError() throws JSONException;

    /**
     * Does this ServerRequest want to not call the remote server but use data
     * from a file in place of the actual server response
     *
     * @return a string that will be used to load the mock response. The
     * returned string must be a non-null file contained in the app's
     * assets/mock_responses directory.
     */
    public abstract String getMockResponse();

    /**
     * obtain the endpoint given the API/Server, if not found log an mError
     */
	public String getServerUrl(ApiInfo api) {
		String url = mServerRequestEnvironment.getUrl(api);
        if (url != null) {
            if (mUrlSuffix != null) {
                return url + mUrlSuffix;
            }
            return url;
        }

        // Don't return null, because there'll be an InvalidArgumentException down the line anyway.
        throw new RuntimeException("API " + api.toString() + " has no record for server "
				+ mServerRequestEnvironment.environmentName());
    }

    public boolean isTrackingRequest() {
        return false; //over-ride in tracking request
    }

    /**
     * Returns the nextValue of the server reply. Throws a JSONException if the
     * response is not valid json.
     *
     * @return
     * @throws JSONException
     */
    protected JSONObject getParsedJsonRootObject() throws JSONException {
        String serverReply = getServerReply();
        return getJsonObjectFromString(serverReply);
    }

    public static JSONObject getJsonObjectFromString(String serverReply) throws JSONException {
        Object nextValue = new JSONTokener(serverReply).nextValue();
        if (!(nextValue instanceof JSONObject)) {
            throw new JSONException("could not parse:" + serverReply + "\nnextValue:"
                    + nextValue);
        }
        JSONObject j = (JSONObject) nextValue;
        return j;
    }

    /**
     * Returns a logger friendly string of this server request.
     *
     * @return
     */
    public String toLogString() {
        return this.getClass().getSimpleName() + " SN:" + this.getSerialNumber();
    }

    public void setServerError(String errorCode, String shortMessage, String longMessage) {
        RequestError e = new RequestError(errorCode, shortMessage, longMessage);
        setError(e);
    }

    public long getSerialNumber() {
        return mSerialNumber;
    }

    public ErrorBase getError() {
        return mError;
    }

    public boolean isSuccess() {
        return getError() == null;
    }

    public void setError(ErrorBase e) {
        if (mError != null) {
            IllegalStateException ise = new IllegalStateException(
                    "Multiple exceptions reported");
            Log.e(TAG, "first mError=" + mError);
            Log.e(TAG, "second mError=" + e);
            Log.e(TAG, "", ise);
            throw ise;
        }
        mError = e;
    }

    public void setHttpStatusCode(Integer statusCode) {
        this.httpStatusCode = statusCode;
    }

    public Integer getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public ServerRequestEnvironment getServerRequestEnvironment() {
        return mServerRequestEnvironment;
    }
}
