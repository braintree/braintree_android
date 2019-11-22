package com.braintreepayments.api;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.PaymentMethodDeleteException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.GraphQLConstants;
import com.braintreepayments.api.internal.GraphQLQueryHelper;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VenmoAccountNonce;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodUnitTest {

    private CardNonce mCardNonce;

    @Before
    public void setup() {
        mCardNonce = mock(CardNonce.class);

        when(mCardNonce.getNonce()).thenReturn("im-a-card-nonce");
    }

    @Test
    public void getPaymentMethodNonces_returnsAnEmptyListIfEmpty() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .successResponse(stringFromFixture("payment_methods/get_payment_methods_empty_response.json"))
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        ArgumentCaptor<List<PaymentMethodNonce>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(fragment).postCallback(captor.capture());
        assertEquals(0, captor.getValue().size());
    }

    @Test
    public void getPaymentMethodNonces_throwsAnError() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .errorResponse(new UnexpectedException("Error"))
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof UnexpectedException);
    }

    @Test
    public void getPaymentMethodNonces_sendsAnAnalyticsEventForParsingErrors() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .successResponse("{}")
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        verify(fragment).sendAnalyticsEvent("get-payment-methods.failed");
    }

    @Test
    public void getPaymentMethodNonces_sendsAnAnalyticsEventForErrors() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .errorResponse(new UnexpectedException("Error"))
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        verify(fragment).sendAnalyticsEvent("get-payment-methods.failed");
    }

    @Test
    public void getPaymentMethodNonces_fetchesPaymentMethods() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .successResponse(stringFromFixture("payment_methods/get_payment_methods_response.json"))
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        ArgumentCaptor<List<PaymentMethodNonce>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(fragment).postCallback(captor.capture());
        List<PaymentMethodNonce> paymentMethodNonces = captor.getValue();
        assertEquals(3, paymentMethodNonces.size());
        assertEquals("11", ((CardNonce) paymentMethodNonces.get(0)).getLastTwo());
        assertEquals("PayPal", paymentMethodNonces.get(1).getTypeLabel());
        assertEquals("happy-venmo-joe", ((VenmoAccountNonce) paymentMethodNonces.get(2)).getUsername());
    }

    @Test
    public void getPaymentMethodNonces_doesNotParseGooglePaymentMethods() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .successResponse(stringFromFixture("payment_methods/get_payment_methods_android_pay_response.json"))
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        ArgumentCaptor<List<PaymentMethodNonce>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(fragment).postCallback(captor.capture());
        List<PaymentMethodNonce> paymentMethodNonces = captor.getValue();
        assertEquals(0, paymentMethodNonces.size());
    }

    @Test
    public void getPaymentMethodNonces_sendsAnAnalyticsEventForSuccess() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .successResponse(stringFromFixture("payment_methods/get_payment_methods_response.json"))
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        verify(fragment).sendAnalyticsEvent("get-payment-methods.succeeded");
    }

    @Test
    public void getPaymentMethodNonces_includesDefaultFirstParamAndSessionIdInRequestPath() {
        BraintreeFragment fragment = new MockFragmentBuilder().build();
        when(fragment.getSessionId()).thenReturn("session-id");

        PaymentMethod.getPaymentMethodNonces(fragment, true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).get(captor.capture(), any(HttpResponseCallback.class));

        String requestUri = captor.getValue();
        assertTrue(requestUri.contains("default_first=true"));
        assertTrue(requestUri.contains("session_id=" + fragment.getSessionId()));
    }

    @Test
    public void deletePaymentMethodNonce_withTokenizationKey_throwsAnError() {
        BraintreeFragment fragment = new MockFragmentBuilder().build();

        PaymentMethod.deletePaymentMethod(fragment, mCardNonce);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("A client token with a customer id must be used to delete a payment method nonce.",
                captor.getValue().getMessage());
        verifyZeroInteractions(fragment.getGraphQLHttpClient());
    }

    @Test
    public void deletePaymentMethodNonce_throwsAnError()
            throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(stringFromFixture("client_token.json"));
        BraintreeFragment fragment = new MockFragmentBuilder()
                .authorization(authorization)
                .graphQLErrorResponse(new UnexpectedException("Error"))
                .build();

        PaymentMethod.deletePaymentMethod(fragment, mCardNonce);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        PaymentMethodDeleteException paymentMethodDeleteException = (PaymentMethodDeleteException)captor.getValue();
        PaymentMethodNonce paymentMethodNonce = paymentMethodDeleteException.getPaymentMethodNonce();
        assertEquals(mCardNonce, paymentMethodNonce);
    }

    @Test
    public void deletePaymentMethodNonce_sendAnAnalyticsEventForFailure()
            throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(stringFromFixture("client_token.json"));
        BraintreeFragment fragment = new MockFragmentBuilder()
                .authorization(authorization)
                .graphQLErrorResponse(new UnexpectedException("Error"))
                .build();

        PaymentMethod.deletePaymentMethod(fragment, mCardNonce);

        verify(fragment).sendAnalyticsEvent("delete-payment-methods.failed");
    }

    @Test
    public void deletePaymentMethodNonce_sendAnAnalyticsEventForSuccess()
            throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(stringFromFixture("client_token.json"));
        BraintreeFragment fragment = new MockFragmentBuilder()
                .authorization(authorization)
                .graphQLSuccessResponse("Success")
                .build();

        PaymentMethod.deletePaymentMethod(fragment, mCardNonce);

        verify(fragment).sendAnalyticsEvent("delete-payment-methods.succeeded");
    }

    @Test
    public void deletePaymentMethodNonce_sendNoncePostCallbackForSuccess()
            throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(stringFromFixture("client_token.json"));
        BraintreeFragment fragment = new MockFragmentBuilder()
                .authorization(authorization)
                .graphQLSuccessResponse("Success")
                .build();

        PaymentMethod.deletePaymentMethod(fragment, mCardNonce);

        verify(fragment).postPaymentMethodDeletedCallback(eq(mCardNonce));
    }

    @Test
    public void deletePaymentMethodNonce_postToGraphQL()
            throws Exception {
        Authorization authorization = Authorization.fromString(stringFromFixture("client_token.json"));
        BraintreeFragment fragment = new MockFragmentBuilder()
                .authorization(authorization)
                .graphQLSuccessResponse("Success")
                .sessionId("test-session-id")
                .integration("test-integration")
                .build();

        PaymentMethod.deletePaymentMethod(fragment, mCardNonce);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getGraphQLHttpClient()).post(captor.capture(), any(HttpResponseCallback.class));
        JSONObject graphQlRequest = new JSONObject(captor.getValue());

        assertEquals(GraphQLQueryHelper.getQuery(RuntimeEnvironment.application, R.raw.delete_payment_method_mutation),
                graphQlRequest.getString(GraphQLConstants.Keys.QUERY));

        JSONObject metadata = graphQlRequest.getJSONObject("clientSdkMetadata");

        assertEquals(mCardNonce.getNonce(), graphQlRequest.getJSONObject("variables")
                .getJSONObject("input").getString("singleUseTokenId"));

        assertEquals("DeletePaymentMethodFromSingleUseToken", graphQlRequest
                .getString(GraphQLConstants.Keys.OPERATION_NAME));

        assertEquals("test-integration", metadata.getString("integration"));
        assertEquals("test-session-id", metadata.getString("sessionId"));
        assertEquals("client", metadata.getString("source"));
    }
}
