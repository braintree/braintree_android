package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HttpRequest {

    private static final int THIRTY_SECONDS_MS = 30000;

    private String path;
    private String baseUrl;

    private byte[] data;
    private HttpMethod method;

    private final int readTimeout;
    private final int connectTimeout;

    private Map<String, String> headers;
    private final Map<String, String> additionalHeaders;

    static HttpRequest newInstance() {
        return new HttpRequest();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpRequest() {
        headers = null;
        additionalHeaders = new HashMap<>();
        baseUrl = "";

        readTimeout = THIRTY_SECONDS_MS;
        connectTimeout = THIRTY_SECONDS_MS;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpRequest path(String path) {
        this.path = path;
        return this;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpRequest baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpRequest data(String dataAsString) {
        this.data = dataAsString.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpRequest method(HttpMethod method) {
        this.method = method;
        return this;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpRequest addHeader(String name, String value) {
        additionalHeaders.put(name, value);
        return this;
    }

    String getPath() {
        return path;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public byte[] getData() {
        return data;
    }

    void dispose() {
        // overwrite data content with zeros
        if (data != null) {
            Arrays.fill(data, (byte) 0);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public HttpMethod getMethod() {
        return method;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Map<String, String> getHeaders() {
        if (headers == null) {
            headers = new HashMap<>();
            headers.put("Accept-Encoding", "gzip");
            headers.put("Accept-Language", Locale.getDefault().getLanguage());
            headers.putAll(additionalHeaders);
        }
        return Collections.unmodifiableMap(headers);
    }

    int getReadTimeout() {
        return readTimeout;
    }

    int getConnectTimeout() {
        return connectTimeout;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public URL getURL() throws MalformedURLException, URISyntaxException {
        URL url;
        if (path.startsWith("http")) {
            url = new URL(path);
        } else {
            URI baseURI = new URL(baseUrl).toURI();
            String newPath = join(baseURI.getPath(), path);
            URI newURI = baseURI.resolve(newPath).normalize();
            url = newURI.toURL();
        }
        return url;
    }

    private static String join(String path1, String path2) {
        File f1 = new File(path1);
        File f2 = new File(f1, path2);
        return f2.getPath();
    }
}
