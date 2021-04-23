package com.braintreepayments.api;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.lang.ref.WeakReference;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricTestRunner.class)
public class TokenizationClientUnitTest {

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
    public void tokenize_whenBraintreeClientReferenceIsNull_doesNothing() {
        Card card = mock(Card.class);
        TokenizeCallback callback = mock(TokenizeCallback.class);

        TokenizationClient sut = new TokenizationClient(new WeakReference<BraintreeClient>(null));
        sut.tokenize(card, callback);

        verifyZeroInteractions(card);
        verifyZeroInteractions(callback);
    }

    @Test
    public void tokenize_includesSessionIdInRequest() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfig)
                .sessionId("session-id")
                .build();

        TokenizationClient sut = new TokenizationClient(braintreeClient);

        sut.tokenize(new Card(), null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));
        JSONObject data = new JSONObject(captor.getValue()).getJSONObject("_meta");
        assertEquals("session-id", data.getString("sessionId"));
    }

    @Test
    public void tokenize_tokenizesCardsWithGraphQLWhenEnabled() throws BraintreeException, InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .authorization(authorization)
                .build();

        TokenizationClient sut = new TokenizationClient(braintreeClient);
        Card card = new Card();

        sut.tokenize(card, null);

        verify(braintreeClient, never()).sendPOST(anyString(), anyString(), any(HttpResponseCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));
        assertEquals(card.buildGraphQLTokenizationJSON().toString(), captor.getValue());
    }

    @Test
    public void tokenize_sendGraphQLAnalyticsEventWhenEnabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        Card card = new Card();

        TokenizationClient sut = new TokenizationClient(braintreeClient);
        sut.tokenize(card, null);

        verify(braintreeClient).sendAnalyticsEvent("card.graphql.tokenization.started");
    }

    @Test
    public void tokenize_tokenizesCardsWithRestWhenGraphQLIsDisabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfig)
                .build();

        Card card = new Card();

        TokenizationClient sut = new TokenizationClient(braintreeClient);
        sut.tokenize(card, null);

        verify(braintreeClient, never()).sendGraphQLPOST(anyString(), any(HttpResponseCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        assertEquals(card.buildTokenizationJSON().toString(), captor.getValue());
    }

    @Test
    public void tokenize_tokenizesNonCardPaymentMethodsWithRestWhenGraphQLIsEnabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        TokenizationClient sut = new TokenizationClient(braintreeClient);

        sut.tokenize(new PayPalAccount(), null);
        sut.tokenize(new UnionPayCard(), null);
        sut.tokenize(new VenmoAccount(), null);

        verify(braintreeClient, never()).sendGraphQLPOST(anyString(), any(HttpResponseCallback.class));
    }

    @Test
    public void tokenize_sendGraphQLAnalyticsEventOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .sendGraphQLPOSTSuccessfulResponse(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD)
                .build();

        Card card = new Card();

        TokenizationClient sut = new TokenizationClient(braintreeClient);
        sut.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {

            }
        });

        verify(braintreeClient).sendAnalyticsEvent("card.graphql.tokenization.success");
    }

    @Test
    public void tokenize_sendGraphQLAnalyticsEventOnFailure() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .sendGraphQLPOSTErrorResponse(ErrorWithResponse.fromGraphQLJson(Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_ERROR))
                .build();

        Card card = new Card();

        TokenizationClient sut = new TokenizationClient(braintreeClient);
        sut.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {

            }
        });

        verify(braintreeClient).sendAnalyticsEvent("card.graphql.tokenization.failure");
    }

    @Test
    public void tokenize_propagatesConfigurationFetchError() {
        Exception configError = new Exception("Configuration error.");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        TokenizationClient sut = new TokenizationClient(braintreeClient);

        Card card = new Card();
        TokenizeCallback callback = mock(TokenizeCallback.class);

        sut.tokenize(card, callback);
        verify(callback).onResult(null, configError);
    }

    @Test
    public void versionedPath_returnsv1Path() {
        assertEquals("/v1/test/path", TokenizationClient.versionedPath("test/path"));
    }
}
