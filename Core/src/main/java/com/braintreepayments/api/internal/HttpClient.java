package com.braintreepayments.api.internal;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.braintreepayments.api.core.BuildConfig;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UnprocessableEntityException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

public class HttpClient<T extends HttpClient> {

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String UTF_8 = "UTF-8";

    private final ExecutorService mThreadPool;
    private final Handler mMainThreadHandler;

    private String mUserAgent;
    private SSLSocketFactory mSSLSocketFactory;
    private int mConnectTimeout;
    private int mReadTimeout;

    protected String mBaseUrl;

    public HttpClient() {
        mThreadPool =  Executors.newCachedThreadPool();
        mMainThreadHandler = new Handler(Looper.getMainLooper());
        mUserAgent = "braintree/core/" + BuildConfig.VERSION_NAME;
        mConnectTimeout = (int) TimeUnit.SECONDS.toMillis(30);
        mReadTimeout = (int) TimeUnit.SECONDS.toMillis(30);

        try {
            mSSLSocketFactory = new TLSSocketFactory();
        } catch (SSLException e) {
            mSSLSocketFactory = null;
        }
    }

    @SuppressWarnings("unchecked")
    public T setUserAgent(String userAgent) {
        mUserAgent = userAgent;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        mSSLSocketFactory = sslSocketFactory;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setBaseUrl(String baseUrl) {
        mBaseUrl = (baseUrl == null) ? "" : baseUrl;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setConnectTimeout(int timeout) {
        mConnectTimeout = timeout;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setReadTimeout(int timeout) {
        mReadTimeout = timeout;
        return (T) this;
    }

    /**
     * Make a HTTP GET request to using the base url and path provided. If the path is a full url,
     * it will be used instead of the previously provided base url.
     *
     * @param path The path or url to request from the server via GET
     * @param callback The {@link HttpResponseCallback} to receive the response or error.
     */
    public void get(final String path, final HttpResponseCallback callback) {
        if (path == null) {
            postCallbackOnMainThread(callback, new IllegalArgumentException("Path cannot be null"));
            return;
        }

        final String url;
        if (path.startsWith("http")) {
            url = path;
        } else {
            url = mBaseUrl + path;
        }

        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    connection = init(url);
                    connection.setRequestMethod(METHOD_GET);
                    postCallbackOnMainThread(callback, parseResponse(connection));
                } catch (Exception e) {
                    postCallbackOnMainThread(callback, e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    /**
     * Make a HTTP POST request using the base url and path provided. If the path is a full url,
     * it will be used instead of the previously provided url.
     *
     * @param path The path or url to request from the server via HTTP POST
     * @param data The body of the POST request
     * @param callback The {@link HttpResponseCallback} to receive the response or error.
     */
    public void post(final String path, final String data, final HttpResponseCallback callback) {
        if (path == null) {
            postCallbackOnMainThread(callback, new IllegalArgumentException("Path cannot be null"));
            return;
        }

        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    if (path.startsWith("http")) {
                        connection = init(path);
                    } else {
                        connection = init(mBaseUrl + path);
                    }

                    connection.setRequestMethod(METHOD_POST);
                    connection.setDoOutput(true);

                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.writeBytes(data);
                    out.flush();
                    out.close();

                    postCallbackOnMainThread(callback, parseResponse(connection));
                } catch (Exception e) {
                    postCallbackOnMainThread(callback, e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    protected HttpURLConnection init(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        if (connection instanceof HttpsURLConnection) {
            if (mSSLSocketFactory == null) {
                throw new SSLException("SSLSocketFactory was not set or failed to initialize");
            }

            ((HttpsURLConnection) connection).setSSLSocketFactory(mSSLSocketFactory);
        }

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", mUserAgent);
        connection.setRequestProperty("Accept-Language", Locale.getDefault().getLanguage());
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setConnectTimeout(mConnectTimeout);
        connection.setReadTimeout(mReadTimeout);

        return connection;
    }

    protected String parseResponse(HttpURLConnection connection) throws Exception {
        int responseCode = connection.getResponseCode();
        boolean gzip = "gzip".equals(connection.getContentEncoding());
        switch(responseCode) {
            case HTTP_OK: case HTTP_CREATED: case HTTP_ACCEPTED:
                return readStream(connection.getInputStream(), gzip);
            case HTTP_UNAUTHORIZED:
                throw new AuthenticationException(readStream(connection.getErrorStream(), gzip));
            case HTTP_FORBIDDEN:
                throw new AuthorizationException(readStream(connection.getErrorStream(), gzip));
            case 422: // HTTP_UNPROCESSABLE_ENTITY
                throw new UnprocessableEntityException(readStream(connection.getErrorStream(), gzip));
            case 426: // HTTP_UPGRADE_REQUIRED
                throw new UpgradeRequiredException(readStream(connection.getErrorStream(), gzip));
            case HTTP_INTERNAL_ERROR:
                throw new ServerException(readStream(connection.getErrorStream(), gzip));
            case HTTP_UNAVAILABLE:
                throw new DownForMaintenanceException(readStream(connection.getErrorStream(), gzip));
            default:
                throw new UnexpectedException(readStream(connection.getErrorStream(), gzip));
        }
    }

    void postCallbackOnMainThread(final HttpResponseCallback callback, final String response) {
        if (callback == null) {
            return;
        }

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.success(response);
            }
        });
    }

    void postCallbackOnMainThread(final HttpResponseCallback callback, final Exception exception) {
        if (callback == null) {
            return;
        }

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.failure(exception);
            }
        });
    }

    @Nullable
    private String readStream(InputStream in, boolean gzip) throws IOException {
        if (in == null) {
            return null;
        }

        if (gzip) {
            in = new GZIPInputStream(in);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int count; (count = in.read(buffer)) != -1; ) {
            out.write(buffer, 0, count);
        }
        return new String(out.toByteArray(), UTF_8);
    }
}
