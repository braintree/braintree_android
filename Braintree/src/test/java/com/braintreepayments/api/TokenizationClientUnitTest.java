package com.braintreepayments.api;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.UnionPayCardBuilder;
import com.braintreepayments.api.models.VenmoAccountBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class TokenizationClientUnitTest {

    @Test
    public void tokenize_includesSessionIdInRequest() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder().build();
        when(fragment.getSessionId()).thenReturn("session-id");

        TokenizationClient.tokenize(fragment, new CardBuilder(), null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));
        JSONObject data = new JSONObject(captor.getValue()).getJSONObject("_meta");
        assertEquals("session-id", data.getString("sessionId"));
    }

    @Test
    public void tokenize_withGraphQLEnabledButApiBelowLollipop_tokenizesCardsWithRest() throws BraintreeException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .graphQL()
                        .build())
                .build();
        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient.tokenize(fragment, cardBuilder, null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verifyZeroInteractions(fragment.getGraphQLHttpClient());
        verify(fragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));
        assertEquals(cardBuilder.build(), captor.getValue());
    }

    @Config(sdk = 21)
    @Test
    public void tokenize_tokenizesCardsWithGraphQLWhenEnabled() throws BraintreeException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .graphQL()
                        .build())
                .build();
        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient.tokenize(fragment, cardBuilder, null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verifyZeroInteractions(fragment.getHttpClient());
        verify(fragment.getGraphQLHttpClient()).post(captor.capture(), any(HttpResponseCallback.class));
        assertEquals(cardBuilder.buildGraphQL(fragment.getApplicationContext(), fragment.getAuthorization()),
                captor.getValue());
    }

    @Config(sdk = 21)
    @Test
    public void tokenize_sendGraphQLAnalyticsEventWhenEnabled() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .graphQL()
                        .build())
                .build();
        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient.tokenize(fragment, cardBuilder, null);

        verify(fragment).sendAnalyticsEvent("card.graphql.tokenization.started");
    }

    @Test
    public void tokenize_tokenizesCardsWithRestWhenGraphQLIsDisabled() {
        BraintreeFragment fragment = new MockFragmentBuilder().build();
        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient.tokenize(fragment, cardBuilder, null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verifyZeroInteractions(fragment.getGraphQLHttpClient());
        verify(fragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));
        assertEquals(cardBuilder.build(), captor.getValue());
    }

    @Test
    public void tokenize_tokenizesNonCardPaymentMethodsWithRestWhenGraphQLIsEnabled() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .graphQL()
                        .build())
                .build();

        TokenizationClient.tokenize(fragment, new PayPalAccountBuilder(), null);
        TokenizationClient.tokenize(fragment, new UnionPayCardBuilder(), null);
        TokenizationClient.tokenize(fragment, new VenmoAccountBuilder(), null);

        verifyZeroInteractions(fragment.getGraphQLHttpClient());
    }

    @Config(sdk = 21)
    @Test
    public void tokenize_sendGraphQLAnalyticsEventOnSuccess() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .graphQL()
                        .build())
                .graphQLSuccessResponse(stringFromFixture("response/graphql/credit_card.json"))
                .build();
        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient.tokenize(fragment, cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {}

            @Override
            public void failure(Exception exception) {}
        });

        verify(fragment).sendAnalyticsEvent("card.graphql.tokenization.success");
    }

    @Config(sdk = 21)
    @Test
    public void tokenize_sendGraphQLAnalyticsEventOnFailure() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .graphQL()
                        .build())
                .graphQLErrorResponse(ErrorWithResponse.fromGraphQLJson(stringFromFixture("errors/graphql/credit_card_error.json")))
                .build();
        CardBuilder cardBuilder = new CardBuilder();

        TokenizationClient.tokenize(fragment, cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {}

            @Override
            public void failure(Exception exception) {}
        });

        verify(fragment).sendAnalyticsEvent("card.graphql.tokenization.failure");
    }

    @Test
    public void versionedPath_returnsv1Path() {
        assertEquals("/v1/test/path", TokenizationClient.versionedPath("test/path"));
    }
}
