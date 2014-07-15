package com.braintreepayments.api.internal;

import android.util.Log;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.squareup.okhttp.OkHttpClient;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

public class HttpRequest {

    public static boolean DEBUG = false;
    public static String TAG = "HttpRequest";

    public static enum HttpMethod {
        GET(false),
        POST(true),
        PUT(true),
        DELETE(false);

        private final boolean encodeParams;

        HttpMethod(boolean formEncodeParams) {
            this.encodeParams = formEncodeParams;
        }
    }

    private static final int STATUS_CODE_UNKNOWN = -1;
    private int mStatusCode = STATUS_CODE_UNKNOWN;

    private OkHttpClient mClient;
    private String mUrl;
    private HttpMethod mMethod;
    private List<NameValuePair> mParams;
    private String mRawBody;
    private List<NameValuePair> mHeaders;
    private String mResponseBody;

    public HttpRequest(OkHttpClient client, HttpMethod method, String url) {
        mClient = client;
        mMethod = method;
        mUrl = url;
        mParams = new LinkedList<NameValuePair>();
        mHeaders = new LinkedList<NameValuePair>();
        header("User-Agent", getUserAgent());
    }

    public HttpRequest header(String name, String value) {
        mHeaders.add(new BasicNameValuePair(name, value));
        return this;
    }

    public HttpRequest param(String key, String value) {
        return param(new BasicNameValuePair(key, value));
    }

    public HttpRequest param(NameValuePair param) {
        mParams.add(param);
        return this;
    }

    public HttpRequest rawBody(String rawBody) {
        mRawBody = rawBody;
        return this;
    }

    private static String urlWithQuery(String url, List<NameValuePair> params) {
        if (params == null || params.isEmpty()) {
            return url;
        } else {
            String query = URLEncodedUtils.format(params, HTTP.UTF_8);
            return url + "?" + query;
        }
    }

    public HttpRequest execute() throws UnexpectedException {
        try {
            HttpURLConnection connection;

            if (mMethod.encodeParams) {
                connection = mClient.open(new URL(mUrl));
            } else {
                connection = mClient.open(new URL(urlWithQuery(mUrl, mParams)));
            }

            try {
                connection.setRequestMethod(mMethod.toString());

                for (NameValuePair pair : mHeaders) {
                    connection.addRequestProperty(pair.getName(), pair.getValue());
                }

                if (mMethod.encodeParams) {
                    // this must happen *after* headers are added.
                    connection.setDoOutput(true);
                    DataOutputStream outputStream =
                            new DataOutputStream(connection.getOutputStream());
                    try {
                        outputStream.writeBytes(getRequestBody());
                    } finally {
                        outputStream.flush();
                        outputStream.close();
                    }
                }

                InputStream in = null;
                try {
                    mStatusCode = connection.getResponseCode();
                    if (mStatusCode == STATUS_CODE_UNKNOWN) {
                        throw new UnexpectedException("Status code unknown");
                    } else if (mStatusCode >= HTTP_OK && mStatusCode < 400) {
                        in = connection.getInputStream();
                    } else {
                        in = connection.getErrorStream();
                    }
                    if (in != null) {
                        byte[] response = readFully(in);
                        mResponseBody = new String(response, "UTF-8");
                    }
                } finally {
                    if (in != null) in.close();
                }
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new UnexpectedException(e.getMessage());
        }

        if (DEBUG) {
            Log.d(TAG, "HTTP request url: " + mUrl);
            Log.d(TAG, "HTTP request finished: Status_code = " + mStatusCode);
            Log.d(TAG, "HTTP request finished: Response = " + mResponseBody);
        }

        return this;
    }

    public String response() {
        return mResponseBody;
    }

    public int statusCode() {
        return mStatusCode;
    }

    private String getRequestBody() {
        if (mRawBody != null) {
            return mRawBody;
        }

        return URLEncodedUtils.format(mParams, HTTP.UTF_8);
    }

    protected String getUserAgent() {
        return "braintree/android/" + BuildConfig.VERSION_NAME;
    }

    /**
     * https://raw.github.com/square/okhttp/master/samples/guide/src/main/java/com/squareup/okhttp/guide/GetExample.java
     */
    private static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int count; (count = in.read(buffer)) != -1; ) {
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }

}