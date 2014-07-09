package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpRequest.HttpMethod;
import com.braintreepayments.api.internal.HttpRequestFactory;
import com.braintreepayments.api.models.CardBuilder;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.squareup.okhttp.OkHttpClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BraintreeApiTest extends AndroidTestCase {

    private Context mContext;

    @Override
    public void setUp() {
        mContext = getContext();
        TestUtils.setUp(mContext);
    }

    private static interface ApiCall {
        public void execute(BraintreeApi api) throws Exception;
    }

    private static final ApiCall CREATE = new ApiCall() {
        @Override
        public void execute(BraintreeApi api) throws Exception {
            api.create(new CardBuilder());
        }
    };

    private static final ApiCall LIST = new ApiCall() {
        @Override
        public void execute(BraintreeApi api) throws Exception {
            api.getPaymentMethods();
        }
    };

    private static final List<ApiCall> API_CALLs = Arrays.asList(CREATE, LIST);

    private class ApiTest {
        private final BraintreeApi api;
        private final Class<? extends Exception> exceptionType;
        private String exceptionMatcher;

        private ApiTest(String mockedResponse, int mockedResponseCode,
                Class<? extends Exception> exceptionType) {
            api = TestUtils.apiWithExpectedResponse(mContext,
                    TestUtils.clientTokenFromFixture(mContext, "client_tokens/client_token.json"),
                    mockedResponse, mockedResponseCode);
            this.exceptionType = exceptionType;
        }

        private ApiTest(BraintreeApi api, Class<? extends Exception> exceptionType) {
            this.api = api;
            this.exceptionType = exceptionType;
        }

        private ApiTest exceptionMessageShouldContain(String exceptionMatcher) {
            this.exceptionMatcher = exceptionMatcher;
            return this;
        }

        private void execute() {
            for (ApiCall apiCall : API_CALLs) {
                boolean exceptionOccurred = false;
                try {
                    apiCall.execute(api);
                } catch (Exception e) {
                    exceptionOccurred = true;
                    assertEquals(exceptionType, e.getClass());
                    if (exceptionMatcher != null) {
                        assertTrue(e.getMessage().contains(exceptionMatcher));
                    }
                }

                assertTrue("Expected an " + exceptionType.getSimpleName() +
                                " but none occurred during " + apiCall.getClass().getSimpleName(),
                        exceptionOccurred);
            }
        }
    }

    public void testThrowsUnexpectedExceptionWhenHttpRequestBlowsUp() {
        BraintreeApi exceptionalClient = TestUtils.unexpectedExceptionThrowingApi(mContext,
                TestUtils.clientTokenFromFixture(mContext, "client_tokens/client_token.json"));
        new ApiTest(exceptionalClient, UnexpectedException.class).execute();
    }

    public void testThrowsServerErrorWhenServerReturns500() {
        new ApiTest("", 500, ServerException.class).execute();
    }

    public void testThrowsServerErrorWhenServerReturnsMalformedResponse() {
        new ApiTest("(╯°□°）╯︵ ┻━┻", 201, ServerException.class)
                .exceptionMessageShouldContain("Parsing server response")
                .execute();
    }

    public void testThrowsServerErrorWhenServerReturnsWellFormedButIncorrectResponse() {
        String mockedResponse = FixturesHelper.stringFromFixture(mContext, "random_json.json");
        new ApiTest(mockedResponse, 201, ServerException.class)
                .exceptionMessageShouldContain("Parsing server response failed")
                .execute();
    }

    public void testThrowsDownForMaintenanceWhenServerIsDown() {
        new ApiTest("", 503, DownForMaintenanceException.class).execute();
    }

    public void testThrowsUpgradeRequiredExceptionOn426() {
        new ApiTest("", 426, UpgradeRequiredException.class).execute();
    }

    public void testThrowsAuthenticationExceptionOn401() {
        new ApiTest("", 401, AuthenticationException.class).execute();
    }

    public void testThrowsAuthorizationExceptionOn403() {
        new ApiTest("", 403, AuthorizationException.class).execute();
    }

    public void testThrowsErrorWithResponseOn422() {
        String mockedResponse = FixturesHelper.stringFromFixture(mContext,
                "errors/error_response.json");
        new ApiTest(mockedResponse, 422, ErrorWithResponse.class)
                .exceptionMessageShouldContain("There was an error")
                .execute();
    }

    public void testThrowsUnexpectedExceptionWhenBuildingErrorFails() {
        String mockedResponse = FixturesHelper.stringFromFixture(mContext, "random_json.json");
        new ApiTest(mockedResponse, 422, UnexpectedException.class)
                .exceptionMessageShouldContain("Parsing error response failed")
                .execute();
    }

    public void testThrowsUnknownExceptionOnUnrecognizedStatusCode() {
        String mockedResponse = FixturesHelper.stringFromFixture(mContext,
                "errors/error_response.json");
        new ApiTest(mockedResponse, 418, UnexpectedException.class).execute();
    }

    public void testThrowsConfigurationExceptionOnBadPayPalConfiguration()
            throws ErrorWithResponse, BraintreeException {
        BraintreeApi braintreeApi = new BraintreeApi(getContext(),
                TestUtils.clientTokenFromFixture(mContext, "client_tokens/client_token.json"));
        boolean exceptionHappened = false;

        try {
            braintreeApi.finishPayWithPayPal(PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID,
                    new Intent());
            fail("Configuration exception not thrown");
        } catch (ConfigurationException e) {
            exceptionHappened = true;
        }

        assertTrue("Expected ConfigurationException was not thrown", exceptionHappened);
    }

    public void testThrowsAuthorizationExceptionForARevokedClientToken() {
        BraintreeApi braintreeApi = new BraintreeApi(getContext(),
                new TestClientTokenBuilder().withRevokedClientToken().build());
        new ApiTest(braintreeApi, AuthorizationException.class).execute();
    }

    public void testSendAnalyticsEventSendsAnalyticsIfEnabled() throws UnexpectedException {
        HttpRequestFactory httpRequestFactory = mock(HttpRequestFactory.class);
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.header("Content-Type", "application/json")).thenReturn(httpRequest);
        when(httpRequest.rawBody(anyString())).thenReturn(httpRequest);
        when(httpRequestFactory.getRequest(eq(HttpMethod.POST), anyString())).thenReturn(
                httpRequest);

        BraintreeApi braintreeApi = new BraintreeApi(getContext(),
                TestUtils.clientTokenFromFixture(mContext, "client_tokens/client_token_analytics.json"),
                httpRequestFactory);

        braintreeApi.sendAnalyticsEvent("very.important.analytics-payload", "TEST");

        verify(httpRequestFactory, times(1)).getRequest(HttpMethod.POST,
                "analytics_url");
        verify(httpRequest, times(1)).rawBody(contains("very.important.analytics-payload"));
        verify(httpRequest, times(1)).execute();
    }

    public void testSendAnalyticsEventNoopsIfDisabled() {
        HttpRequestFactory httpRequestFactory = mock(HttpRequestFactory.class);
        BraintreeApi braintreeApi = new BraintreeApi(getContext(),
                TestUtils.clientTokenFromFixture(mContext, "client_tokens/client_token.json"),
                httpRequestFactory);

        braintreeApi.sendAnalyticsEvent("event", "TEST");

        verify(httpRequestFactory, never()).getRequest(any(HttpMethod.class), anyString());
    }

    public void testAnalyticsEventsAreSentToServer() {
        final AtomicInteger requestCount = new AtomicInteger(0);
        final AtomicInteger responseCode = new AtomicInteger(0);
        HttpRequestFactory requestFactory = new HttpRequestFactory(getContext()) {
            @Override
            public HttpRequest getRequest(HttpMethod method, String url) {
                return new HttpRequest(new OkHttpClient(), method, url) {
                    @Override
                    public HttpRequest execute() throws UnexpectedException {
                        HttpRequest request = super.execute();
                        requestCount.incrementAndGet();
                        responseCode.set(request.statusCode());
                        return request;
                    }
                };
            }
        };
        ClientToken token = ClientToken.getClientToken(new TestClientTokenBuilder().withAnalytics().build());
        BraintreeApi braintreeApi = new BraintreeApi(getContext(), token, requestFactory);

        braintreeApi.sendAnalyticsEvent("event", "TEST");
        assertEquals(1, requestCount.get());
        assertEquals(201, responseCode.get());

        braintreeApi.sendAnalyticsEvent("another-event", "TEST");
        assertEquals(2, requestCount.get());
        assertEquals(201, responseCode.get());
    }

}
