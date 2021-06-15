package com.braintreepayments.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

/**
 * Class that handles parsing http responses for {@link SynchronousHttpClient}.
 */
class BaseHTTPResponseParser implements HTTPResponseParser {

    private static final int HTTP_UNPROCESSABLE_ENTITY = 422;
    private static final int HTTP_UPGRADE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;


    /**
     * @param responseCode the response code returned when the http request was made.
     * @param connection the connection through which the http request was made.
     * @return the body of the http response.
     */
    public String parse(int responseCode, HttpURLConnection connection) throws Exception {
        String responseBody = parseBody(responseCode, connection);
        switch (responseCode) {
            case HTTP_OK: case HTTP_CREATED: case HTTP_ACCEPTED:
                return responseBody;
            case HTTP_BAD_REQUEST: case HTTP_UNPROCESSABLE_ENTITY:
                throw new UnprocessableEntityException(responseBody);
            case HTTP_UNAUTHORIZED:
                throw new AuthenticationException(responseBody);
            case HTTP_FORBIDDEN:
                throw new AuthorizationException(responseBody);
            case HTTP_UPGRADE_REQUIRED:
                throw new UpgradeRequiredException(responseBody);
            case HTTP_TOO_MANY_REQUESTS:
                throw new RateLimitException("You are being rate-limited. Please try again in a few minutes.");
            case HTTP_INTERNAL_ERROR:
                throw new ServerException(responseBody);
            case HTTP_UNAVAILABLE:
                throw new ServiceUnavailableException(responseBody);
            default:
                throw new UnexpectedException(responseBody);
        }
    }

    private String parseBody(int responseCode, HttpURLConnection connection) throws IOException {
        boolean gzip = "gzip".equals(connection.getContentEncoding());
        switch (responseCode) {
            case HTTP_OK: case HTTP_CREATED: case HTTP_ACCEPTED:
                return readStream(connection.getInputStream(), gzip);
            case HTTP_TOO_MANY_REQUESTS:
                return null;
            case HTTP_UNAUTHORIZED:
            case HTTP_FORBIDDEN:
            case HTTP_BAD_REQUEST:
            case HTTP_UNPROCESSABLE_ENTITY:
            case HTTP_UPGRADE_REQUIRED:
            case HTTP_INTERNAL_ERROR:
            case HTTP_UNAVAILABLE:
            default:
                return readStream(connection.getErrorStream(), gzip);
        }
    }

    private String readStream(InputStream in, boolean gzip) throws IOException {
        if (in == null) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (gzip) {
                in = new GZIPInputStream(in);
            }

            byte[] buffer = new byte[1024];
            for (int count; (count = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, count);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);

        } finally {
            try {
                in.close();
            } catch (IOException ignored) {}
        }
    }
}
