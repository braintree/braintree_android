package com.braintreepayments.api;

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
public class HTTPRequestTest {

    public static class NonParameterizedHTTPRequestTests {

        @Test
        public void getPath_returnsPath() {
            HTTPRequest sut = HTTPRequest.newInstance()
                    .path("sample/path");

            assertEquals("sample/path", sut.getPath());
        }

        @Test
        public void getData_returnsData() {
            HTTPRequest sut = HTTPRequest.newInstance()
                    .data("sample data");

            assertEquals("sample data", sut.getData());
        }

        @Test
        public void getMethod_returnsMethod() {
            HTTPRequest sut = HTTPRequest.newInstance()
                    .method("GET");

            assertEquals("GET", sut.getMethod());
        }

        @Test
        public void getHeaders_containsADefaultSetOfHeaders() {
            HTTPRequest sut = HTTPRequest.newInstance();

            assertEquals(2, sut.getHeaders().size());
            assertEquals("gzip", sut.getHeaders().get("Accept-Encoding"));
            assertEquals(Locale.getDefault().getLanguage(), sut.getHeaders().get("Accept-Language"));
        }

        @Test
        public void addHeaders_allowsForMoreHeadersToBeAddedToDefaultSet() {
            HTTPRequest sut = HTTPRequest.newInstance()
                    .addHeader("Header-0", "0")
                    .addHeader("Header-1", "1");

            assertEquals("0", sut.getHeaders().get("Header-0"));
            assertEquals("1", sut.getHeaders().get("Header-1"));
        }

        @Test
        public void getURL_whenPathStartsWithHttp_returnsPathWithNoModification() throws MalformedURLException, URISyntaxException {
            HTTPRequest sut = HTTPRequest.newInstance()
                    .path("https://anothersite.com/path");

            URL expectedURL = new URL("https://anothersite.com/path");
            assertEquals(expectedURL, sut.getURL());
        }

        @Test
        public void constructor_setsConnectTimeoutTo30SecondsByDefault() {
            HTTPRequest sut = HTTPRequest.newInstance();
            assertEquals(30000, sut.getConnectTimeout());
        }

        @Test
        public void constructor_setsReadTimeoutTo30SecondsByDefault() {
            HTTPRequest sut = HTTPRequest.newInstance();
            assertEquals(30000, sut.getReadTimeout());
        }

        @Test
        public void getURL_throwsMalformedURLExceptionIfBaseURLIsNull() {
            HTTPRequest sut = HTTPRequest.newInstance()
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
            HTTPRequest sut = HTTPRequest.newInstance()
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

        private final String baseUrl;
        private final String path;
        private final URL expectedURL;

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
            HTTPRequest sut = HTTPRequest.newInstance()
                    .baseUrl(baseUrl)
                    .path(path);

            assertEquals(expectedURL, sut.getURL());
        }
    }
}