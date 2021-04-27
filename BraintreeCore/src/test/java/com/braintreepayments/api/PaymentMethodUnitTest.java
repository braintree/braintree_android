package com.braintreepayments.api;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static com.braintreepayments.api.FixturesHelper.base64Encode;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodUnitTest {

    private Context context;
    private CardNonce cardNonce;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        cardNonce = mock(CardNonce.class);

        when(cardNonce.getString()).thenReturn("im-a-card-nonce");
    }

    @Test
    public void getPaymentMethodNonces_returnsAnEmptyListIfEmpty() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_EMPTY_RESPONSE)
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(callback);

        ArgumentCaptor<List<PaymentMethodNonce>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());

        assertEquals(0, captor.getValue().size());
    }

    @Test
    public void getPaymentMethodNonces_throwsAnError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETErrorResponse(new UnexpectedException("Error"))
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((List<PaymentMethodNonce>) isNull(), captor.capture());
        assertTrue(captor.getValue() instanceof UnexpectedException);
    }

    @Test
    public void getPaymentMethodNonces_sendsAnAnalyticsEventForParsingErrors() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse("{}")
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(callback);

        verify(braintreeClient).sendAnalyticsEvent("get-payment-methods.failed");
    }

    @Test
    public void getPaymentMethodNonces_sendsAnAnalyticsEventForErrors() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETErrorResponse(new UnexpectedException("Error"))
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(callback);

        verify(braintreeClient).sendAnalyticsEvent("get-payment-methods.failed");
    }

    @Test
    public void getPaymentMethodNonces_fetchesPaymentMethods() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_RESPONSE)
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(callback);

        ArgumentCaptor<List<PaymentMethodNonce>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());

        List<PaymentMethodNonce> paymentMethodNonces = captor.getValue();
        assertEquals(3, paymentMethodNonces.size());
        assertEquals(PaymentMethodType.CARD, paymentMethodNonces.get(0).getType());
        assertEquals(PaymentMethodType.PAYPAL, paymentMethodNonces.get(1).getType());
        assertEquals(PaymentMethodType.VENMO, paymentMethodNonces.get(2).getType());
    }

    @Test
    public void getPaymentMethodNonces_doesNotParseGooglePayMethods() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_GOOGLE_PAY_RESPONSE)
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(callback);

        ArgumentCaptor<List<PaymentMethodNonce>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());

        List<PaymentMethodNonce> paymentMethodNonces = captor.getValue();
        assertEquals(0, paymentMethodNonces.size());
    }

    @Test
    public void getPaymentMethodNonces_sendsAnAnalyticsEventForSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGETSuccessfulResponse(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_RESPONSE)
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(callback);

        verify(braintreeClient).sendAnalyticsEvent("get-payment-methods.succeeded");
    }

    @Test
    public void getPaymentMethodNonces_includesDefaultFirstParamAndSessionIdInRequestPath() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(true, callback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGET(captor.capture(), any(HttpResponseCallback.class));

        String requestUri = captor.getValue();
        assertTrue(requestUri.contains("default_first=true"));
        assertTrue(requestUri.contains("session_id=session-id"));
    }

    @Test
    public void deletePaymentMethodNonce_withTokenizationKey_throwsAnError() throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        DeletePaymentMethodNonceCallback callback = mock(DeletePaymentMethodNonceCallback.class);
        sut.deletePaymentMethod(context, cardNonce, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("A client token with a customer id must be used to delete a payment method nonce.",
                captor.getValue().getMessage());

        verify(braintreeClient, never()).sendGraphQLPOST(anyString(), any(HttpResponseCallback.class));
    }

    @Test
    public void deletePaymentMethodNonce_throwsAnError()
            throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN)))
                .sendGraphQLPOSTErrorResponse(new UnexpectedException("Error"))
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        DeletePaymentMethodNonceCallback callback = mock(DeletePaymentMethodNonceCallback.class);
        sut.deletePaymentMethod(context, cardNonce, callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        PaymentMethodDeleteException paymentMethodDeleteException = (PaymentMethodDeleteException)captor.getValue();
        PaymentMethodNonce paymentMethodNonce = paymentMethodDeleteException.getPaymentMethodNonce();
        assertEquals(cardNonce, paymentMethodNonce);
    }

    @Test
    public void deletePaymentMethodNonce_sendAnAnalyticsEventForFailure()
            throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN)))
                .sendGraphQLPOSTErrorResponse(new UnexpectedException("Error"))
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        DeletePaymentMethodNonceCallback callback = mock(DeletePaymentMethodNonceCallback.class);
        sut.deletePaymentMethod(context, cardNonce, callback);

        verify(braintreeClient).sendAnalyticsEvent("delete-payment-methods.failed");
    }

    @Test
    public void deletePaymentMethodNonce_sendAnAnalyticsEventForSuccess()
            throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN)))
                .sendGraphQLPOSTSuccessfulResponse("Success")
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        DeletePaymentMethodNonceCallback callback = mock(DeletePaymentMethodNonceCallback.class);
        sut.deletePaymentMethod(context, cardNonce, callback);

        verify(braintreeClient).sendAnalyticsEvent("delete-payment-methods.succeeded");
    }

    @Test
    public void deletePaymentMethodNonce_sendNoncePostCallbackForSuccess()
            throws InvalidArgumentException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN)))
                .sendGraphQLPOSTSuccessfulResponse("Success")
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        DeletePaymentMethodNonceCallback callback = mock(DeletePaymentMethodNonceCallback.class);
        sut.deletePaymentMethod(context, cardNonce, callback);

        verify(callback).onResult(cardNonce, null);
    }

    @Test
    public void deletePaymentMethodNonce_postToGraphQL()
            throws Exception {
        Authorization authorization = Authorization
                .fromString(base64Encode(Fixtures.CLIENT_TOKEN));

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorization(authorization)
                .sendGraphQLPOSTSuccessfulResponse("Success")
                .sessionId("test-session-id")
                .integration("test-integration")
                .build();
        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        DeletePaymentMethodNonceCallback callback = mock(DeletePaymentMethodNonceCallback.class);
        sut.deletePaymentMethod(context, cardNonce, callback);

        verify(braintreeClient).getIntegrationType();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));

        JSONObject graphQlRequest = new JSONObject(captor.getValue());

        String expectedGraphQLQuery = GraphQLQueryHelper.getQuery(
                ApplicationProvider.getApplicationContext(), R.raw.delete_payment_method_mutation);
        assertEquals(expectedGraphQLQuery, graphQlRequest.getString(GraphQLConstants.Keys.QUERY));

        JSONObject metadata = graphQlRequest.getJSONObject("clientSdkMetadata");

        assertEquals(cardNonce.getString(), graphQlRequest.getJSONObject("variables")
                .getJSONObject("input").getString("singleUseTokenId"));

        assertEquals("DeletePaymentMethodFromSingleUseToken", graphQlRequest
                .getString(GraphQLConstants.Keys.OPERATION_NAME));

        assertEquals("test-integration", metadata.getString("integration"));
        assertEquals("test-session-id", metadata.getString("sessionId"));
        assertEquals("client", metadata.getString("source"));
    }
}
