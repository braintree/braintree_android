package com.braintreepayments.api;

import android.app.Activity;
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
import com.braintreepayments.api.internal.HttpResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.testutils.FixturesHelper;
import com.braintreepayments.testutils.TestClientTokenBuilder;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;

import org.json.JSONException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.braintreepayments.testutils.CardNumber.VISA;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
                Class<? extends Exception> exceptionType) throws UnexpectedException {
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

    public void testThrowsUnexpectedExceptionWhenHttpRequestBlowsUp() throws UnexpectedException {
        BraintreeApi exceptionalClient = TestUtils.unexpectedExceptionThrowingApi(mContext,
                TestUtils.clientTokenFromFixture(mContext, "client_tokens/client_token.json"));
        new ApiTest(exceptionalClient, UnexpectedException.class).execute();
    }

    public void testThrowsServerErrorWhenServerReturns500() throws UnexpectedException {
        new ApiTest("", 500, ServerException.class).execute();
    }

    public void testThrowsServerErrorWhenServerReturnsMalformedResponse()
            throws UnexpectedException {
        new ApiTest("(╯°□°）╯︵ ┻━┻", 201, ServerException.class)
                .exceptionMessageShouldContain("Parsing server response")
                .execute();
    }

    public void testThrowsServerErrorWhenServerReturnsWellFormedButIncorrectResponse()
            throws UnexpectedException {
        String mockedResponse = FixturesHelper.stringFromFixture(mContext, "random_json.json");
        new ApiTest(mockedResponse, 201, ServerException.class)
                .exceptionMessageShouldContain("Parsing server response failed")
                .execute();
    }

    public void testThrowsDownForMaintenanceWhenServerIsDown() throws UnexpectedException {
        new ApiTest("", 503, DownForMaintenanceException.class).execute();
    }

    public void testThrowsUpgradeRequiredExceptionOn426() throws UnexpectedException {
        new ApiTest("", 426, UpgradeRequiredException.class).execute();
    }

    public void testThrowsAuthenticationExceptionOn401() throws UnexpectedException {
        new ApiTest("", 401, AuthenticationException.class).execute();
    }

    public void testThrowsAuthorizationExceptionOn403() throws UnexpectedException {
        new ApiTest("", 403, AuthorizationException.class).execute();
    }

    public void testThrowsErrorWithResponseOn422() throws UnexpectedException {
        String mockedResponse = FixturesHelper.stringFromFixture(mContext,
                "errors/error_response.json");
        new ApiTest(mockedResponse, 422, ErrorWithResponse.class)
                .exceptionMessageShouldContain("There was an error")
                .execute();
    }

    public void testThrowsUnexpectedExceptionWhenBuildingErrorFails() throws UnexpectedException {
        String mockedResponse = FixturesHelper.stringFromFixture(mContext, "random_json.json");
        new ApiTest(mockedResponse, 422, UnexpectedException.class)
                .exceptionMessageShouldContain("Parsing error response failed")
                .execute();
    }

    public void testThrowsUnknownExceptionOnUnrecognizedStatusCode() throws UnexpectedException {
        String mockedResponse = FixturesHelper.stringFromFixture(mContext,
                "errors/error_response.json");
        new ApiTest(mockedResponse, 418, UnexpectedException.class).execute();
    }

    public void testThrowsConfigurationExceptionOnBadPayPalConfiguration()
            throws ErrorWithResponse, BraintreeException {
        BraintreeApi braintreeApi = new BraintreeApi(mContext,
                TestUtils.clientTokenFromFixture(mContext, "client_tokens/client_token.json"));
        boolean exceptionHappened = false;

        try {
            braintreeApi.finishPayWithPayPal(null, PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID,
                    new Intent());
            fail("Configuration exception not thrown");
        } catch (ConfigurationException e) {
            exceptionHappened = true;
        }

        assertTrue("Expected ConfigurationException was not thrown", exceptionHappened);
    }

    public void testThrowsAuthorizationExceptionForARevokedClientToken() {
        BraintreeApi braintreeApi = new BraintreeApi(mContext,
                new TestClientTokenBuilder().withRevokedClientToken().build());
        new ApiTest(braintreeApi, AuthorizationException.class).execute();
    }

    public void testSendAnalyticsEventSendsAnalyticsIfEnabled() throws UnexpectedException {
        HttpRequest httpRequest = mock(HttpRequest.class);

        BraintreeApi braintreeApi = new BraintreeApi(mContext,
                TestUtils.clientTokenFromFixture(mContext, "client_tokens/client_token_analytics.json"),
                httpRequest);

        braintreeApi.sendAnalyticsEvent("very.important.analytics-payload", "TEST");

        verify(httpRequest, times(1)).post(matches("analytics_url"),
                contains("very.important.analytics-payload"));
    }

    public void testFinishPayWithVenmoReturnsANonce() {
        BraintreeApi braintreeApi = new BraintreeApi(mContext, new TestClientTokenBuilder().build());
        Intent intent = new Intent().putExtra(AppSwitch.EXTRA_PAYMENT_METHOD_NONCE, "payment method nonce");

        assertEquals("payment method nonce", braintreeApi.finishPayWithVenmo(Activity.RESULT_OK, intent));
    }

    public void testPayWithVenmoReturnsNullIfResultCodeNotOK() {
        BraintreeApi braintreeApi = new BraintreeApi(mContext, new TestClientTokenBuilder().build());

        assertNull(braintreeApi.finishPayWithVenmo(Activity.RESULT_CANCELED, new Intent()));
    }

    public void testGetPaymentMethodReturnsPaymentMethodFromNonce()
            throws ErrorWithResponse, BraintreeException, JSONException {
        BraintreeApi braintreeApi = new BraintreeApi(mContext, new TestClientTokenBuilder().build());
        Card card = braintreeApi.create(
                new CardBuilder().cardNumber(VISA).expirationDate("06/20"));

        Card cardFromShowNonce = (Card) braintreeApi.getPaymentMethod(card.getNonce());

        assertEquals(card.getLastTwo(), cardFromShowNonce.getLastTwo());
        assertEquals(card.getTypeLabel(), cardFromShowNonce.getTypeLabel());
    }

    public void testSendAnalyticsEventNoopsIfDisabled() throws UnexpectedException {
        HttpRequest httpRequest = mock(HttpRequest.class);
        BraintreeApi braintreeApi = new BraintreeApi(mContext,
                TestUtils.clientTokenFromFixture(mContext, "client_tokens/client_token.json"),
                httpRequest);

        braintreeApi.sendAnalyticsEvent("event", "TEST");

        verify(httpRequest, never()).post(anyString(), anyString());
    }

    public void testAnalyticsEventsAreSentToServer() {
        final AtomicInteger requestCount = new AtomicInteger(0);
        final AtomicInteger responseCode = new AtomicInteger(0);
        ClientToken token = ClientToken.getClientToken(new TestClientTokenBuilder().withAnalytics().build());
        HttpRequest request = new HttpRequest(token.getAuthorizationFingerprint()) {
            @Override
            public HttpResponse post (String url, String params) throws UnexpectedException {
                requestCount.incrementAndGet();
                HttpResponse response = super.post(url, params);
                responseCode.set(response.getResponseCode());
                return response;
            }
        };
        BraintreeApi braintreeApi = new BraintreeApi(mContext, token, request);

        braintreeApi.sendAnalyticsEvent("event", "TEST");
        assertEquals(1, requestCount.get());
        assertEquals(200, responseCode.get());

        braintreeApi.sendAnalyticsEvent("another-event", "TEST");
        assertEquals(2, requestCount.get());
        assertEquals(200, responseCode.get());
    }

}
