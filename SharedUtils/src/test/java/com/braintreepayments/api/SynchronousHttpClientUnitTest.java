package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TLSSocketFactory.class })
public class SynchronousHttpClientUnitTest {

    HttpRequest httpRequest;
    BaseHttpResponseParser httpResponseParser;
    SSLSocketFactory sslSocketFactory;

    @Before
    public void beforeEach() {
        mockStatic(TLSSocketFactory.class);
        httpRequest = mock(HttpRequest.class);
        httpResponseParser = mock(BaseHttpResponseParser.class);
        sslSocketFactory = mock(SSLSocketFactory.class);
    }

    @Test
    public void request_whenHttpRequestURLMalformed_throwsMalformedURLException() {
        final HttpRequest httpRequest = new HttpRequest()
                .path("")
                .method("GET")
                .baseUrl("/:/");

        final SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        assertThrows(MalformedURLException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                sut.request(httpRequest);
            }
        });
    }

    @Test
    public void request_whenPathIsNull_throwsIllegalArgumentException() {
        final HttpRequest httpRequest = new HttpRequest()
                .path(null);

        final SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                sut.request(httpRequest);
            }
        });

        assertEquals("Path cannot be null", exception.getMessage());
    }

    @Test
    public void request_setsRequestMethod() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);
        verify(connection).setRequestMethod("GET");
    }

    @Test
    public void request_whenConnectionIsHttps_usesDefaultSSLSocketFactoryWhenNoFactoryIsSet() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        TLSSocketFactory defaultSocketFactory = mock(TLSSocketFactory.class);
        when(TLSSocketFactory.newInstance()).thenReturn(defaultSocketFactory);

        SynchronousHttpClient sut = new SynchronousHttpClient(null, httpResponseParser);
        sut.request(httpRequest);
        verify(connection).setSSLSocketFactory(defaultSocketFactory);
    }

    @Test
    public void request_whenConnectionIsHttps_setsSSLSocketFactory() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);
        verify(connection).setSSLSocketFactory(sslSocketFactory);
    }

    @Test
    public void request_whenConnectionIsHttps_andSSLSocketFactoryIsNull_throwsSSLException() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        TLSSocketFactory defaultSocketFactory = mock(TLSSocketFactory.class);
        when(TLSSocketFactory.newInstance()).thenReturn(defaultSocketFactory);

        final SynchronousHttpClient sut = new SynchronousHttpClient(null, httpResponseParser);
        sut.setSSLSocketFactory(null);
        SSLException exception = assertThrows(SSLException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                sut.request(httpRequest);
            }
        });

        assertEquals(exception.getMessage(), "SSLSocketFactory was not set or failed to initialize");
    }

    @Test
    public void request_setsHttpReadTimeout() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com"));

        when(httpRequest.getReadTimeout()).thenReturn(123);

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);
        verify(connection).setReadTimeout(123);
    }

    @Test
    public void request_setsHttpConnectionTimeout() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com"));

        when(httpRequest.getConnectTimeout()).thenReturn(456);

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);
        verify(connection).setConnectTimeout(456);
    }

    @Test
    public void request_setsHttpHeaders() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com")
                .addHeader("Sample-Header", "Sample Value"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);
        verify(connection).setRequestProperty("Sample-Header", "Sample Value");
    }

    @Test
    public void request_parsesResponseAndReturnsHttpBody() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        String result = sut.request(httpRequest);
        assertEquals("http_ok", result);
    }

    @Test
    public void request_onSuccess_closesUrlConnection() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);
        verify(connection).disconnect();
    }

    @Test
    public void request_onHttpResponseParserException_propagatesExceptionAndClosesUrlConnection() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("GET")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenThrow(new Exception("error"));

        final SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        assertThrows(Exception.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                sut.request(httpRequest);
            }
        });
        verify(connection).disconnect();
    }

    @Test
    public void request_whenPost_addsContentTypeHeader() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("POST")
                .data("test data")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);
        verify(connection).setRequestProperty("Content-Type", "application/json");
    }

    @Test
    public void request_whenPost_writesAsciiCharactersToOutputStream() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("POST")
                .data("test data")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        OutputStream outputStream = mock(OutputStream.class);
        when(connection.getOutputStream()).thenReturn(outputStream);

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);

        verify(connection).setDoOutput(true);
        verify(outputStream).write(toByteArray("test data"));
        verify(outputStream).flush();
        verify(outputStream).close();
        verify(httpRequest).dispose();
    }

    @Test
    public void request_whenPost_writesUTF8CharactersToOutputStream() throws Exception {
        final HttpRequest httpRequest = spy(new HttpRequest()
                .path("sample/path")
                .method("POST")
                .data("Bjärne Stroustrüp")
                .baseUrl("https://www.sample.com"));

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        OutputStream outputStream = mock(OutputStream.class);
        when(connection.getOutputStream()).thenReturn(outputStream);

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);

        verify(connection).setDoOutput(true);
        verify(outputStream).write(toByteArray("Bjärne Stroustrüp"));
        verify(outputStream).flush();
        verify(outputStream).close();
        verify(httpRequest).dispose();
    }

    private static byte[] toByteArray(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }
}

