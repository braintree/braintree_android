package com.braintreepayments.api;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

/**
 * This class performs an http request on the calling thread. The external caller is
 * responsible for thread scheduling to ensure that this is not called on the main thread.
 */
class SynchronousHttpClient {

    private SSLSocketFactory socketFactory;
    private final HttpResponseParser parser;

    SynchronousHttpClient(SSLSocketFactory socketFactory, HttpResponseParser parser) {
        this.parser = parser;
        if (socketFactory != null) {
            this.socketFactory = socketFactory;
        } else {
            try {
                this.socketFactory = TLSSocketFactory.newInstance();
            } catch (SSLException e) {
                this.socketFactory = null;
            }
        }
    }

    void setSSLSocketFactory(SSLSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    String request(HttpRequest httpRequest) throws Exception {
        if (httpRequest.getPath() == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        URL url = httpRequest.getURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            if (socketFactory == null) {
                throw new SSLException("SSLSocketFactory was not set or failed to initialize");
            }
            ((HttpsURLConnection) connection).setSSLSocketFactory(socketFactory);
        }

        String requestMethod = httpRequest.getMethod();
        connection.setRequestMethod(requestMethod);

        connection.setReadTimeout(httpRequest.getReadTimeout());
        connection.setConnectTimeout(httpRequest.getConnectTimeout());

        // apply request headers
        Map<String, String> headers = httpRequest.getHeaders();
        for (Map.Entry<String,String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        if (requestMethod != null && requestMethod.equals("POST")) {
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(httpRequest.getData());
            outputStream.flush();
            outputStream.close();

            httpRequest.dispose();
        }

        int responseCode = connection.getResponseCode();
        try {
            return parser.parse(responseCode, connection);
        } finally {
            connection.disconnect();
        }
    }
}
