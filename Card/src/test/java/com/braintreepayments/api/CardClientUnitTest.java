package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CardClientUnitTest {

    private Context context;
    private Card card;
    private CardTokenizeCallback cardTokenizeCallback;

    private ApiClient apiClient;

    private Configuration graphQLEnabledConfig;
    private Configuration graphQLDisabledConfig;

    @Before
    public void beforeEach() throws JSONException {
        context = mock(Context.class);
        card = new Card();
        cardTokenizeCallback = mock(CardTokenizeCallback.class);

        apiClient = mock(ApiClient.class);

        graphQLEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
        graphQLDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
    }

    @Test
    public void tokenize_whenGraphQLEnabled_setsSessionIdOnCardBeforeTokenizing() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();
        when(braintreeClient.getSessionId()).thenReturn("session-id");

        CardClient sut = new CardClient(braintreeClient, apiClient);

        Card card = spy(new Card());
        sut.tokenize(card, cardTokenizeCallback);

        InOrder inOrder = Mockito.inOrder(card, apiClient);
        inOrder.verify(card).setSessionId("session-id");
        inOrder.verify(apiClient).tokenizeGraphQL(any(JSONObject.class), any(TokenizeCallback.class));
    }

    @Test
    public void tokenize_whenGraphQLEnabled_tokenizesWithGraphQL() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        apiClient = new MockApiClientBuilder()
                .tokenizeGraphQLSuccess(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD))
                .build();

        CardClient sut = new CardClient(braintreeClient, apiClient);

        sut.tokenize(card, cardTokenizeCallback);

        ArgumentCaptor<CardNonce> captor = ArgumentCaptor.forClass(CardNonce.class);
        verify(cardTokenizeCallback).onResult(captor.capture(), isNull());

        CardNonce cardNonce = captor.getValue();
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", cardNonce.getString());
    }

    @Test
    public void tokenize_whenGraphQLDisabled_tokenizesWithREST() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfig)
                .build();

        apiClient = new MockApiClientBuilder()
                .tokenizeRESTSuccess(new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))
                .build();

        CardClient sut = new CardClient(braintreeClient, apiClient);

        sut.tokenize(card, cardTokenizeCallback);

        ArgumentCaptor<CardNonce> captor = ArgumentCaptor.forClass(CardNonce.class);
        verify(cardTokenizeCallback).onResult(captor.capture(), isNull());

        CardNonce cardNonce = captor.getValue();
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getString());
    }

    @Test
    public void tokenize_whenGraphQLEnabled_sendsAnalyticsEventOnSuccess() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        apiClient = new MockApiClientBuilder()
                .tokenizeGraphQLSuccess(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD))
                .build();

        CardClient sut = new CardClient(braintreeClient, apiClient);
        sut.tokenize(card, cardTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent("card.nonce-received");
    }

    @Test
    public void tokenize_whenGraphQLDisabled_sendsAnalyticsEventOnSuccess() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfig)
                .build();

        apiClient = new MockApiClientBuilder()
                .tokenizeRESTSuccess(new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))
                .build();

        CardClient sut = new CardClient(braintreeClient, apiClient);
        sut.tokenize(card, cardTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent("card.nonce-received");
    }

    @Test
    public void tokenize_whenGraphQLEnabled_callsListenerWithErrorOnFailure() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        Exception error = new Exception();
        apiClient = new MockApiClientBuilder()
                .tokenizeGraphQLError(error)
                .build();

        CardClient sut = new CardClient(braintreeClient, apiClient);
        sut.tokenize(card, cardTokenizeCallback);

        verify(cardTokenizeCallback).onResult(null, error);
    }

    @Test
    public void tokenize_whenGraphQLDisabled_callsListenerWithErrorOnFailure() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfig)
                .build();

        Exception error = new Exception();
        apiClient = new MockApiClientBuilder()
                .tokenizeRESTError(error)
                .build();

        CardClient sut = new CardClient(braintreeClient, apiClient);
        sut.tokenize(card, cardTokenizeCallback);

        verify(cardTokenizeCallback).onResult(null, error);
    }

    @Test
    public void tokenize_whenGraphQLEnabled_sendsAnalyticsEventOnFailure() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        Exception error = new Exception();
        apiClient = new MockApiClientBuilder()
                .tokenizeGraphQLError(error)
                .build();

        CardClient sut = new CardClient(braintreeClient, apiClient);
        sut.tokenize(card, cardTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent("card.nonce-failed");
    }

    @Test
    public void tokenize_whenGraphQLDisabled_sendsAnalyticsEventOnFailure() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfig)
                .build();

        Exception error = new Exception();
        apiClient = new MockApiClientBuilder()
                .tokenizeRESTError(error)
                .build();

        CardClient sut = new CardClient(braintreeClient, apiClient);
        sut.tokenize(card, cardTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent("card.nonce-failed");
    }

    @Test
    public void tokenize_propagatesConfigurationFetchError() {
        Exception configError = new Exception("Configuration error.");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        CardClient sut = new CardClient(braintreeClient, apiClient);
        sut.tokenize(card, cardTokenizeCallback);

        verify(cardTokenizeCallback).onResult(null, configError);
    }
}