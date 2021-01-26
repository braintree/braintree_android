package com.braintreepayments.api;

import com.braintreepayments.api.HttpRequest;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import static org.junit.Assert.*;


@RunWith(Enclosed.class)
public class HttpRequestTest {

    public static class NonParameterizedHttpRequestTests {

        @Test
        public void getPath_returnsPath() {
            HttpRequest sut = HttpRequest.newInstance()
                    .path("sample/path");

            assertEquals("sample/path", sut.getPath());
        }

        @Test
        public void getData_returnsData() {
            HttpRequest sut = HttpRequest.newInstance()
                    .data("sample data");

            assertEquals("sample data", sut.getData());
        }

        @Test
        public void getMethod_returnsMethod() {
            HttpRequest sut = HttpRequest.newInstance()
                    .method("GET");

            assertEquals("GET", sut.getMethod());
        }

        @Test
        public void getHeaders_containsADefaultSetOfHeaders() {
            HttpRequest sut = HttpRequest.newInstance();

            assertEquals(2, sut.getHeaders().size());
            assertEquals("gzip", sut.getHeaders().get("Accept-Encoding"));
            assertEquals(Locale.getDefault().getLanguage(), sut.getHeaders().get("Accept-Language"));
        }

        @Test
        public void addHeaders_allowsForMoreHeadersToBeAddedToDefaultSet() {
            HttpRequest sut = HttpRequest.newInstance()
                    .addHeader("Header-0", "0")
                    .addHeader("Header-1", "1");

            assertEquals("0", sut.getHeaders().get("Header-0"));
            assertEquals("1", sut.getHeaders().get("Header-1"));
        }

        @Test
        public void getURL_whenPathStartsWithHttp_returnsPathWithNoModification() throws MalformedURLException, URISyntaxException {
            HttpRequest sut = HttpRequest.newInstance()
                    .path("https://anothersite.com/path");

            URL expectedURL = new URL("https://anothersite.com/path");
            assertEquals(expectedURL, sut.getURL());
        }

        @Test
        public void constructor_setsConnectTimeoutTo30SecondsByDefault() {
            HttpRequest sut = HttpRequest.newInstance();
            assertEquals(30000, sut.getConnectTimeout());
        }

        @Test
        public void constructor_setsReadTimeoutTo30SecondsByDefault() {
            HttpRequest sut = HttpRequest.newInstance();
            assertEquals(30000, sut.getReadTimeout());
        }

        @Test
        public void getURL_throwsMalformedURLExceptionIfBaseURLIsNull() {
            HttpRequest sut = HttpRequest.newInstance()
                    .baseUrl(null)
                    .path("sample/path");

            try {
                sut.getURL();
                fail("should throw an error");
            } catch (Exception e) {
                assertTrue(e instanceof MalformedURLException);
            }
        }

        @Test
        public void getURL_throwsMalformedURLExceptionIfBaseURLIsEmpty() {
            HttpRequest sut = HttpRequest.newInstance()
                    .baseUrl("")
                    .path("sample/path");

            try {
                sut.getURL();
                fail("should throw an error");
            } catch (Exception e) {
                assertTrue(e instanceof MalformedURLException);
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class GetURLTest {

        final private String baseUrl;
        final private String path;
        final private URL expectedURL;

        public GetURLTest(String baseUrl, String path, URL expectedURL) {
            this.baseUrl = baseUrl;
            this.path = path;
            this.expectedURL = expectedURL;
        }

        @Parameterized.Parameters(name = "Joins baseUrl: {0} and path: {1}")
        public static Collection<Object[]> urlScenarios() throws IOException {
            return Arrays.asList(new Object[][]{
                    {"https://www.example.com", "sample/path?param=1#fragment", new URL("https://www.example.com/sample/path?param=1#fragment")},
                    {"https://www.example.com/", "sample/path?param=1#fragment", new URL("https://www.example.com/sample/path?param=1#fragment")},
                    {"https://www.example.com", "/sample/path?param=1#fragment", new URL("https://www.example.com/sample/path?param=1#fragment")},
                    {"https://www.example.com/", "/sample/path?param=1#fragment", new URL("https://www.example.com/sample/path?param=1#fragment")},
                    {"https://www.example.com/existing/path", "sample/path?param=1#fragment", new URL("https://www.example.com/existing/path/sample/path?param=1#fragment")},
                    {"https://www.example.com/existing/path/", "sample/path?param=1#fragment", new URL("https://www.example.com/existing/path/sample/path?param=1#fragment")},
                    {"https://www.example.com/existing/path", "/sample/path?param=1#fragment", new URL("https://www.example.com/existing/path/sample/path?param=1#fragment")},
                    {"https://www.example.com/existing/path/", "/sample/path?param=1#fragment", new URL("https://www.example.com/existing/path/sample/path?param=1#fragment")},
                    {"https://www.example.com:123", "sample/path?param=1#fragment", new URL("https://www.example.com:123/sample/path?param=1#fragment")},
                    {"https://www.example.com:123/", "sample/path?param=1#fragment", new URL("https://www.example.com:123/sample/path?param=1#fragment")},
                    {"https://www.example.com:123", "/sample/path?param=1#fragment", new URL("https://www.example.com:123/sample/path?param=1#fragment")},
                    {"https://www.example.com:123/", "/sample/path?param=1#fragment", new URL("https://www.example.com:123/sample/path?param=1#fragment")},
                    {"https://www.example.com:123/existing/path", "sample/path?param=1#fragment", new URL("https://www.example.com:123/existing/path/sample/path?param=1#fragment")},
                    {"https://www.example.com:123/existing/path/", "sample/path?param=1#fragment", new URL("https://www.example.com:123/existing/path/sample/path?param=1#fragment")},
                    {"https://www.example.com:123/existing/path", "/sample/path?param=1#fragment", new URL("https://www.example.com:123/existing/path/sample/path?param=1#fragment")},
                    {"https://www.example.com:123/existing/path/", "/sample/path?param=1#fragment", new URL("https://www.example.com:123/existing/path/sample/path?param=1#fragment")},
            });
        }

        @Test
        public void getURL() throws Exception {
            HttpRequest sut = HttpRequest.newInstance()
                    .baseUrl(baseUrl)
                    .path(path);

            assertEquals(expectedURL, sut.getURL());
        }
    }
}