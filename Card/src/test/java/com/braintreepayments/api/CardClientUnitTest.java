package com.braintreepayments.api;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CardClientUnitTest {

    private Context context;
    private Card card;
    private CardTokenizeCallback cardTokenizeCallback;

    private BraintreeClient braintreeClient;
    private DataCollector dataCollector;
    private TokenizationClient tokenizationClient;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        card = mock(Card.class);
        cardTokenizeCallback = mock(CardTokenizeCallback.class);

        braintreeClient = mock(BraintreeClient.class);
        dataCollector = mock(DataCollector.class);
        tokenizationClient = mock(TokenizationClient.class);
    }

    @Test
    public void tokenize_callsListenerWithNonceOnSuccess() {
        CardClient sut = new CardClient(braintreeClient, tokenizationClient, dataCollector);
        sut.tokenize(context, card, cardTokenizeCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
            ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(same(card), callbackCaptor.capture());

        PaymentMethodNonceCallback callback = callbackCaptor.getValue();
        CardNonce cardNonce = mock(CardNonce.class);
        callback.success(cardNonce);

        verify(cardTokenizeCallback).onResult(cardNonce, null);
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnSuccess() {
        CardClient sut = new CardClient(braintreeClient, tokenizationClient, dataCollector);
        sut.tokenize(context, card, cardTokenizeCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(same(card), callbackCaptor.capture());

        PaymentMethodNonceCallback callback = callbackCaptor.getValue();
        callback.success(mock(CardNonce.class));

        verify(braintreeClient).sendAnalyticsEvent("card.nonce-received");
    }

    @Test
    public void tokenize_callsListenerWithErrorOnFailure() {
        CardClient sut = new CardClient(braintreeClient, tokenizationClient, dataCollector);
        sut.tokenize(context, card, cardTokenizeCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(same(card), callbackCaptor.capture());

        PaymentMethodNonceCallback callback = callbackCaptor.getValue();
        Exception error = new Exception("error");
        callback.failure(error);

        verify(cardTokenizeCallback).onResult(null, error);
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnFailure() {
        CardClient sut = new CardClient(braintreeClient, tokenizationClient, dataCollector);
        sut.tokenize(context, card, cardTokenizeCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(same(card), callbackCaptor.capture());

        PaymentMethodNonceCallback callback = callbackCaptor.getValue();
        callback.failure(new Exception("error"));

        verify(braintreeClient).sendAnalyticsEvent("card.nonce-failed");
    }
}