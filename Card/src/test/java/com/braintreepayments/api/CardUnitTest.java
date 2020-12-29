package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CardUnitTest {

    private Context context;
    private CardBuilder cardBuilder;
    private CardTokenizeCallback cardTokenizeCallback;

    private BraintreeClient braintreeClient;
    private DataCollector dataCollector;
    private TokenizationClient tokenizationClient;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        cardBuilder = mock(CardBuilder.class);
        cardTokenizeCallback = mock(CardTokenizeCallback.class);

        braintreeClient = mock(BraintreeClient.class);
        dataCollector = mock(DataCollector.class);
        tokenizationClient = mock(TokenizationClient.class);
    }

    @Test
    public void tokenize_callsListenerWithNonceOnSuccess() {
        Card sut = new Card(braintreeClient, tokenizationClient, dataCollector);
        sut.tokenize(context, cardBuilder, cardTokenizeCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
            ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(same(context), same(cardBuilder), callbackCaptor.capture());

        PaymentMethodNonceCallback callback = callbackCaptor.getValue();
        PaymentMethodNonce paymentMethodNonce = mock(PaymentMethodNonce.class);
        callback.success(paymentMethodNonce);

        verify(cardTokenizeCallback).onResult(paymentMethodNonce, null);
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnSuccess() {
        Card sut = new Card(braintreeClient, tokenizationClient, dataCollector);
        sut.tokenize(context, cardBuilder, cardTokenizeCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(same(context), same(cardBuilder), callbackCaptor.capture());

        PaymentMethodNonceCallback callback = callbackCaptor.getValue();
        callback.success(mock(PaymentMethodNonce.class));

        verify(braintreeClient).sendAnalyticsEvent(context, "card.nonce-received");
    }

    @Test
    public void tokenize_callsListenerWithErrorOnFailure() {
        Card sut = new Card(braintreeClient, tokenizationClient, dataCollector);
        sut.tokenize(context, cardBuilder, cardTokenizeCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(same(context), same(cardBuilder), callbackCaptor.capture());

        PaymentMethodNonceCallback callback = callbackCaptor.getValue();
        Exception error = new Exception("error");
        callback.failure(error);

        verify(cardTokenizeCallback).onResult(null, error);
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnFailure() {
        Card sut = new Card(braintreeClient, tokenizationClient, dataCollector);
        sut.tokenize(context, cardBuilder, cardTokenizeCallback);

        ArgumentCaptor<PaymentMethodNonceCallback> callbackCaptor =
                ArgumentCaptor.forClass(PaymentMethodNonceCallback.class);
        verify(tokenizationClient).tokenize(same(context), same(cardBuilder), callbackCaptor.capture());

        PaymentMethodNonceCallback callback = callbackCaptor.getValue();
        callback.failure(new Exception("error"));

        verify(braintreeClient).sendAnalyticsEvent(context, "card.nonce-failed");
    }
}