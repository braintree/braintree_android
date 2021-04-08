package com.braintreepayments.api;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class BaseHttpResponseParserTest {

    @RunWith(Parameterized.class)
    public static class HttpSuccessTest {

        final private int responseCode;
        final private String contentEncoding;
        final private InputStream inputStream;

        final private String expectedResult;

        public HttpSuccessTest(int responseCode, String contentEncoding, InputStream inputStream, String expectedResult) {
            this.responseCode = responseCode;
            this.contentEncoding = contentEncoding;
            this.inputStream = inputStream;
            this.expectedResult = expectedResult;
        }

        @Parameterized.Parameters( name = "Parses Response Code {0} with Encoding {1}")
        public static Collection<Object[]> responseScenarios() throws IOException {
            return Arrays.asList(new Object[][] {
                { HTTP_OK, "gzip", createGzippedInputStream("200_ok_gzip"), "200_ok_gzip" },
                { HTTP_OK, null, createPlainTextInputStream("200_ok_plaintext"), "200_ok_plaintext" },
                { HTTP_CREATED, "gzip", createGzippedInputStream("201_created_gzip"), "201_created_gzip" },
                { HTTP_CREATED, null, createPlainTextInputStream("201_created_plaintext"), "201_created_plaintext" },
                { HTTP_ACCEPTED, "gzip", createGzippedInputStream("202_accepted_gzip"), "202_accepted_gzip" },
                { HTTP_ACCEPTED, null, createPlainTextInputStream("202_accepted_plaintext"), "202_accepted_plaintext" },
            });
        }

        @Test
        public void parse() throws Exception {
            HttpURLConnection connection = mock(HttpURLConnection.class);
            when(connection.getContentEncoding()).thenReturn(contentEncoding);
            when(connection.getInputStream()).thenReturn(inputStream);

            BaseHttpResponseParser sut = new BaseHttpResponseParser();
            assertEquals(expectedResult, sut.parse(responseCode, connection));
            verify(inputStream).close();
        }
    }

    @RunWith(Parameterized.class)
    public static class HttpErrorTest {

        final private int responseCode;
        final private String contentEncoding;
        final private InputStream errorStream;

        final private String expectedMessage;
        final private Class<Exception> expectedExceptionClass;

        public HttpErrorTest(int responseCode, String contentEncoding, InputStream errorStream, Class<Exception> expectedExceptionClass, String expectedMessage) {
            this.responseCode = responseCode;
            this.contentEncoding = contentEncoding;
            this.errorStream = errorStream;
            this.expectedMessage = expectedMessage;
            this.expectedExceptionClass = expectedExceptionClass;
        }

        @Parameterized.Parameters( name = "Parses Response Code {0} with Encoding {1}")
        public static Collection<Object[]> responseScenarios() throws IOException {
            return Arrays.asList(new Object[][] {
                { HTTP_BAD_REQUEST, "gzip", createGzippedInputStream("400_bad_request_gzip"), UnprocessableEntityException.class, "400_bad_request_gzip" },
                { HTTP_BAD_REQUEST, null, createPlainTextInputStream("400_bad_request_plaintext"), UnprocessableEntityException.class, "400_bad_request_plaintext" },
                { HTTP_UNAUTHORIZED, "gzip", createGzippedInputStream("401_unauthorized_gzip"), AuthenticationException.class, "401_unauthorized_gzip" },
                { HTTP_UNAUTHORIZED, null, createPlainTextInputStream("401_unauthorized_plaintext"), AuthenticationException.class, "401_unauthorized_plaintext" },
                { HTTP_FORBIDDEN, "gzip", createGzippedInputStream("403_forbidden_gzip"), AuthorizationException.class, "403_forbidden_gzip" },
                { HTTP_FORBIDDEN, null, createPlainTextInputStream("403_forbidden_plaintext"), AuthorizationException.class, "403_forbidden_plaintext" },
                { 422, "gzip", createGzippedInputStream("422_unprocessable_entity_gzip"), UnprocessableEntityException.class, "422_unprocessable_entity_gzip" },
                { 422, null, createPlainTextInputStream("422_unprocessable_entity_plaintext"), UnprocessableEntityException.class, "422_unprocessable_entity_plaintext" },
                { 426, "gzip", createGzippedInputStream("426_upgrade_required_gzip"), UpgradeRequiredException.class, "426_upgrade_required_gzip" },
                { 426, null, createPlainTextInputStream("426_upgrade_required_plaintext"), UpgradeRequiredException.class, "426_upgrade_required_plaintext" },
                { HTTP_INTERNAL_ERROR, "gzip", createGzippedInputStream("500_internal_server_error_gzip"), ServerException.class, "500_internal_server_error_gzip" },
                { HTTP_INTERNAL_ERROR, null, createPlainTextInputStream("500_internal_server_error_plaintext"), ServerException.class, "500_internal_server_error_plaintext" },
                { HTTP_UNAVAILABLE, "gzip", createGzippedInputStream("503_unavailable_gzip"), ServiceUnavailableException.class, "503_unavailable_gzip" },
                { HTTP_UNAVAILABLE, null, createPlainTextInputStream("503_unavailable_plaintext"), ServiceUnavailableException.class, "503_unavailable_plaintext" },
                { 418, "gzip", createGzippedInputStream("418_i'm_a_teapot_gzip"), UnexpectedException.class, "418_i'm_a_teapot_gzip" },
                { 418, null, createPlainTextInputStream("418_i'm_a_teapot_plaintext"), UnexpectedException.class, "418_i'm_a_teapot_plaintext" }
            });
        }

        @Test
        public void parse() throws IOException {
            final HttpURLConnection connection = mock(HttpURLConnection.class);
            when(connection.getContentEncoding()).thenReturn(contentEncoding);
            when(connection.getErrorStream()).thenReturn(errorStream);

            final BaseHttpResponseParser sut = new BaseHttpResponseParser();
            Exception exception = assertThrows(expectedExceptionClass, new ThrowingRunnable() {
                @Override
                public void run() throws Throwable {
                    sut.parse(responseCode, connection);
                }
            });

            assertEquals(expectedMessage, exception.getMessage());
            verify(errorStream).close();
        }
    }

    public static class HttpTooManyRequestsTest {

        @Test
        public void parse() {
            final HttpURLConnection connection = mock(HttpURLConnection.class);

            final BaseHttpResponseParser sut = new BaseHttpResponseParser();
            Exception exception = assertThrows(RateLimitException.class, new ThrowingRunnable() {
                @Override
                public void run() throws Throwable {
                    sut.parse(429, connection);
                }
            });

            String expectedMessage = "You are being rate-limited. Please try again in a few minutes.";
            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    private static InputStream createPlainTextInputStream(String input) {
        return spy(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    private static InputStream createGzippedInputStream(String input) throws IOException {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream(input.length());
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteOutputStream);
        gzipOutputStream.write(input.getBytes(StandardCharsets.UTF_8));
        gzipOutputStream.close();
        byte[] compressed = byteOutputStream.toByteArray();
        byteOutputStream.close();
        return spy(new ByteArrayInputStream(compressed));
    }
}