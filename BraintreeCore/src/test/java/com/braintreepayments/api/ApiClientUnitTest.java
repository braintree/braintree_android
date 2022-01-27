package com.braintreepayments.api;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.lang.ref.WeakReference;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricTestRunner.class)
public class ApiClientUnitTest {

    Context context;

    private Configuration graphQLEnabledConfig;
    private Configuration graphQLDisabledConfig;

    @Before
    public void beforeEach() throws JSONException {
        context = ApplicationProvider.getApplicationContext();

        graphQLEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
        graphQLDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
    }

    @Test
    public void tokenizeREST_whenBraintreeClientReferenceIsNull_doesNothing() {
        Card card = mock(Card.class);
        TokenizeCallback callback = mock(TokenizeCallback.class);

        ApiClient sut = new ApiClient(new WeakReference<BraintreeClient>(null));
        sut.tokenizeREST(card, callback);

        verifyZeroInteractions(card);
        verifyZeroInteractions(callback);
    }

    @Test
    public void tokenizeREST_setsSessionIdBeforeTokenizing() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfig)
                .sessionId("session-id")
                .build();

        ApiClient sut = new ApiClient(braintreeClient);

        Card card = spy(new Card());
        sut.tokenizeREST(card, null);

        InOrder inOrder = Mockito.inOrder(card, braintreeClient);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        inOrder.verify(card).setSessionId("session-id");
        inOrder.verify(braintreeClient).sendPOST(anyString(), bodyCaptor.capture(), any(HttpResponseCallback.class));

        JSONObject data = new JSONObject(bodyCaptor.getValue()).getJSONObject("_meta");
        assertEquals("session-id", data.getString("sessionId"));
    }

    @Test
    public void tokenizeGraphQL_tokenizesCardsWithGraphQL() throws BraintreeException, InvalidArgumentException, JSONException {
        Authorization authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .authorizationSuccess(authorization)
                .build();

        ApiClient sut = new ApiClient(braintreeClient);
        Card card = new Card();

        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), null);

        verify(braintreeClient, never()).sendPOST(anyString(), anyString(), any(HttpResponseCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));
        assertEquals(card.buildJSONForGraphQL().toString(), captor.getValue());
    }

    @Test
    public void tokenizeGraphQL_sendGraphQLAnalyticsEventWhenEnabled() throws BraintreeException, JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        Card card = new Card();

        ApiClient sut = new ApiClient(braintreeClient);
        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), null);

        verify(braintreeClient).sendAnalyticsEvent("card.graphql.tokenization.started");
    }

    @Test
    public void tokenizeREST_tokenizesPaymentMethodsWithREST() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        ApiClient sut = new ApiClient(braintreeClient);

        sut.tokenizeREST(new PayPalAccount(), null);
        sut.tokenizeREST(new UnionPayCard(), null);
        sut.tokenizeREST(new VenmoAccount(), null);

        verify(braintreeClient, never()).sendGraphQLPOST(anyString(), any(HttpResponseCallback.class));
    }

    @Test
    public void tokenizeGraphQL_sendGraphQLAnalyticsEventOnSuccess() throws BraintreeException, JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .sendGraphQLPOSTSuccessfulResponse(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD)
                .build();

        Card card = new Card();

        ApiClient sut = new ApiClient(braintreeClient);
        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {

            }
        });

        verify(braintreeClient).sendAnalyticsEvent("card.graphql.tokenization.success");
    }

    @Test
    public void tokenizeGraphQL_sendGraphQLAnalyticsEventOnFailure() throws BraintreeException, JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .sendGraphQLPOSTErrorResponse(ErrorWithResponse.fromGraphQLJson(Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_ERROR))
                .build();

        Card card = new Card();

        ApiClient sut = new ApiClient(braintreeClient);
        sut.tokenizeGraphQL(card.buildJSONForGraphQL(), new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {

            }
        });

        verify(braintreeClient).sendAnalyticsEvent("card.graphql.tokenization.failure");
    }

    @Test
    public void versionedPath_returnsv1Path() {
        assertEquals("/v1/test/path", ApiClient.versionedPath("test/path"));
    }
}
