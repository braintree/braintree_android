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
        CardBuilder cardBuilder = mock(CardBuilder.class);
        PaymentMethodNonceCallback callback = mock(PaymentMethodNonceCallback.class);

        TokenizationClient sut = new TokenizationClient(new WeakReference<BraintreeClient>(null));
        sut.tokenize(cardBuilder, callback);

        verifyZeroInteractions(cardBuilder);
        verifyZeroInteractions(callback);
    }

    @Test
    public void tokenize_includesSessionIdInRequest() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfig)
                .sessionId("session-id")
                .build();

        TokenizationClient sut = new TokenizationClient(braintreeClient);

        sut.tokenize(new CardBuilder(), null);

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
        CardBuilder cardBuilder = new CardBuilder();

        sut.tokenize(cardBuilder, null);

        verify(braintreeClient, never()).sendPOST(anyString(), anyString(), any(HttpResponseCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));
        assertEquals(cardBuilder.buildGraphQL(authorization),
                captor.getValue());
    }

    @Test
    public void tokenize_sendGraphQLAnalyticsEventWhenEnabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient sut = new TokenizationClient(braintreeClient);
        sut.tokenize(cardBuilder, null);

        verify(braintreeClient).sendAnalyticsEvent("card.graphql.tokenization.started");
    }

    @Test
    public void tokenize_tokenizesCardsWithRestWhenGraphQLIsDisabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLDisabledConfig)
                .build();

        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient sut = new TokenizationClient(braintreeClient);
        sut.tokenize(cardBuilder, null);

        verify(braintreeClient, never()).sendGraphQLPOST(anyString(), any(HttpResponseCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        assertEquals(cardBuilder.build(), captor.getValue());
    }

    @Test
    public void tokenize_tokenizesNonCardPaymentMethodsWithRestWhenGraphQLIsEnabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .build();

        TokenizationClient sut = new TokenizationClient(braintreeClient);

        sut.tokenize(new PayPalAccountBuilder(), null);
        sut.tokenize(new UnionPayCardBuilder(), null);
        sut.tokenize(new VenmoAccountBuilder(), null);

        verify(braintreeClient, never()).sendGraphQLPOST(anyString(), any(HttpResponseCallback.class));
    }

    @Test
    public void tokenize_sendGraphQLAnalyticsEventOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .sendGraphQLPOSTSuccessfulResponse(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD)
                .build();

        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient sut = new TokenizationClient(braintreeClient);
        sut.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {}

            @Override
            public void failure(Exception exception) {}
        });

        verify(braintreeClient).sendAnalyticsEvent("card.graphql.tokenization.success");
    }

    @Test
    public void tokenize_sendGraphQLAnalyticsEventOnFailure() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(graphQLEnabledConfig)
                .sendGraphQLPOSTErrorResponse(ErrorWithResponse.fromGraphQLJson(Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_ERROR))
                .build();

        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient sut = new TokenizationClient(braintreeClient);
        sut.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {}

            @Override
            public void failure(Exception exception) {}
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

        CardBuilder cardBuilder = new CardBuilder();
        PaymentMethodNonceCallback callback = mock(PaymentMethodNonceCallback.class);

        sut.tokenize(cardBuilder, callback);
        verify(callback).failure(configError);
    }

    @Test
    public void versionedPath_returnsv1Path() {
        assertEquals("/v1/test/path", TokenizationClient.versionedPath("test/path"));
    }
}
