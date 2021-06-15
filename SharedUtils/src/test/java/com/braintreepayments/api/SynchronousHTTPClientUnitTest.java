package com.braintreepayments.api;

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
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TLSSocketFactory.class })
public class SynchronousHTTPClientUnitTest {

    HTTPRequest httpRequest;
    BaseHTTPResponseParser httpResponseParser;
    SSLSocketFactory sslSocketFactory;

    @Before
    public void beforeEach() {
        mockStatic(TLSSocketFactory.class);
        httpRequest = mock(HTTPRequest.class);
        httpResponseParser = mock(BaseHTTPResponseParser.class);
        sslSocketFactory = mock(SSLSocketFactory.class);
    }

    @Test
    public void request_whenHttpRequestURLMalformed_throwsMalformedURLException() throws Exception {
        when(httpRequest.getPath()).thenReturn("");
        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getURL()).thenThrow(new MalformedURLException());

        final SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        assertThrows(MalformedURLException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                sut.request(httpRequest);
            }
        });
    }

    @Test
    public void request_whenPathIsNull_throwsIllegalArgumentException() throws Exception {
        when(httpRequest.getPath()).thenReturn(null);

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
        when(httpRequest.getPath()).thenReturn("sample/path");
        when(httpRequest.getMethod()).thenReturn("GET");

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
        when(httpRequest.getPath()).thenReturn("sample/path");

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
        when(httpRequest.getPath()).thenReturn("sample/path");

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
        when(httpRequest.getPath()).thenReturn("sample/path");

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
        when(httpRequest.getPath()).thenReturn("sample/path");
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
        when(httpRequest.getPath()).thenReturn("sample/path");
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
        when(httpRequest.getPath()).thenReturn("sample/path");

        Map<String, String> headers = new HashMap<>();
        headers.put("Sample-Header", "Sample Value");
        when(httpRequest.getHeaders()).thenReturn(headers);

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
        when(httpRequest.getPath()).thenReturn("sample/path");
        when(httpRequest.getMethod()).thenReturn("GET");

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
        when(httpRequest.getPath()).thenReturn("sample/path");
        when(httpRequest.getMethod()).thenReturn("GET");

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
        when(httpRequest.getPath()).thenReturn("sample/path");
        when(httpRequest.getMethod()).thenReturn("GET");

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
        when(httpRequest.getPath()).thenReturn("sample/path");
        when(httpRequest.getMethod()).thenReturn("POST");

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(httpRequest.getData()).thenReturn("test data");

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);
        verify(connection).setRequestProperty("Content-Type", "application/json");
    }

    @Test
    public void request_whenPost_writesAsciiCharactersToOutputStream() throws Exception {
        when(httpRequest.getPath()).thenReturn("sample/path");
        when(httpRequest.getMethod()).thenReturn("POST");

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        String data = "test data";
        when(httpRequest.getData()).thenReturn(data);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        OutputStream outputStream = mock(OutputStream.class);
        when(connection.getOutputStream()).thenReturn(outputStream);

        byte[] expectedBytes = data.getBytes(StandardCharsets.UTF_8);

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);

        verify(connection).setDoOutput(true);
        verify(outputStream).write(expectedBytes);
        verify(outputStream).flush();
        verify(outputStream).close();
    }

    @Test
    public void request_whenPost_writesUTF8CharactersToOutputStream() throws Exception {
        when(httpRequest.getPath()).thenReturn("sample/path");
        when(httpRequest.getMethod()).thenReturn("POST");

        URL url = mock(URL.class);
        when(httpRequest.getURL()).thenReturn(url);

        String data = "Bjärne Stroustrüp";
        when(httpRequest.getData()).thenReturn(data);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(url.openConnection()).thenReturn(connection);

        when(connection.getResponseCode()).thenReturn(200);
        when(httpResponseParser.parse(200, connection)).thenReturn("http_ok");

        OutputStream outputStream = mock(OutputStream.class);
        when(connection.getOutputStream()).thenReturn(outputStream);

        byte[] expectedBytes = data.getBytes(StandardCharsets.UTF_8);

        SynchronousHttpClient sut = new SynchronousHttpClient(sslSocketFactory, httpResponseParser);
        sut.request(httpRequest);

        verify(connection).setDoOutput(true);
        verify(outputStream).write(expectedBytes);
        verify(outputStream).flush();
        verify(outputStream).close();
    }
}

