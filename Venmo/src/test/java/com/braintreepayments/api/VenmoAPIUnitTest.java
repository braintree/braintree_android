package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VenmoAPIUnitTest {

    private BraintreeClient braintreeClient;
    private ApiClient apiClient;

    @Before
    public void beforeEach() throws JSONException {
        braintreeClient = mock(BraintreeClient.class);
        apiClient = mock(ApiClient.class);
    }

    @Test
    public void createPaymentContext_createsPaymentContextViaGraphQL() throws JSONException {
        VenmoAPI venmoAPI = new VenmoAPI(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setShouldVault(false);
        request.setDisplayName("display-name");

        venmoAPI.createPaymentContext(request, request.getProfileId(), mock(VenmoApiCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));

        String graphQLBody = captor.getValue();
        JSONObject graphQLJSON = new JSONObject(graphQLBody);
        String expectedQuery = "mutation CreateVenmoPaymentContext($input: CreateVenmoPaymentContextInput!) { createVenmoPaymentContext(input: $input) { venmoPaymentContext { id } } }";
        assertEquals(expectedQuery, graphQLJSON.getString("query"));

        JSONObject variables = graphQLJSON.getJSONObject("variables");
        JSONObject input = variables.getJSONObject("input");
        assertEquals("SINGLE_USE", input.getString("paymentMethodUsage"));
        assertEquals("sample-venmo-merchant", input.getString("merchantProfileId"));
        assertEquals("MOBILE_APP", input.getString("customerClient"));
        assertEquals("CONTINUE", input.getString("intent"));
        assertEquals("display-name", input.getString("displayName"));
    }

    @Test
    public void createPaymentContext_whenGraphQLPostSuccess_includesPaymentContextID_callsBackNull() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_CONTEXT_RESPONSE)
                .build();
        VenmoAPI venmoAPI = new VenmoAPI(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");

        VenmoApiCallback callback = mock(VenmoApiCallback.class);
        venmoAPI.createPaymentContext(request, request.getProfileId(), callback);

        verify(callback).onResult((Exception) isNull());
    }

    @Test
    public void createPaymentContext_whenGraphQLPostSuccess_missingPaymentContextID_callsBackError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_RESPONSE_WITHOUT_PAYMENT_CONTEXT_ID)
                .build();
        VenmoAPI venmoAPI = new VenmoAPI(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");

        VenmoApiCallback callback = mock(VenmoApiCallback.class);
        venmoAPI.createPaymentContext(request, request.getProfileId(), callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult(captor.capture());

        Exception error = captor.getValue();
        assertTrue(error instanceof BraintreeException);
        assertEquals("Failed to fetch a Venmo paymentContextId while constructing the requestURL.", error.getMessage());
    }

    @Test
    public void createPaymentContext_whenGraphQLPostError_forwardsErrorToCallback() {
        Exception error = new Exception("error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTErrorResponse(error)
                .build();
        VenmoAPI venmoAPI = new VenmoAPI(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");

        VenmoApiCallback callback = mock(VenmoApiCallback.class);
        venmoAPI.createPaymentContext(request, request.getProfileId(), callback);

        verify(callback).onResult(error);
    }

    @Test
    public void createNonceFromPaymentContext_queriesGraphQLPaymentContext() throws JSONException {
        VenmoAPI sut = new VenmoAPI(braintreeClient, apiClient);
        sut.createNonceFromPaymentContext("payment-context-id", mock(VenmoOnActivityResultCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));

        String payload = captor.getValue();
        JSONObject jsonPayload = new JSONObject(payload);
        String expectedQuery = "query PaymentContext($id: ID!) { node(id: $id) { ... on VenmoPaymentContext { paymentMethodId userName payerInfo { firstName lastName phoneNumber email externalId userName } } } }";
        assertEquals(expectedQuery, jsonPayload.get("query"));
        assertEquals("payment-context-id", jsonPayload.getJSONObject("variables").get("id"));
    }

    @Test
    public void createNonceFromPaymentContext_whenGraphQLPostSuccess_forwardsNonceToCallback() throws JSONException {
        String graphQLResponse = Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE;
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse(graphQLResponse)
                .build();

        VenmoAPI sut = new VenmoAPI(braintreeClient, apiClient);

        VenmoOnActivityResultCallback callback = mock(VenmoOnActivityResultCallback.class);
        sut.createNonceFromPaymentContext("payment-context-id", callback);

        ArgumentCaptor<VenmoAccountNonce> captor = ArgumentCaptor.forClass(VenmoAccountNonce.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());
        assertEquals("@somebody", captor.getValue().getUsername());
    }

    @Test
    public void createNonceFromPaymentContext_whenGraphQLPostResponseMalformed_callsBackError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse("not-json")
                .build();

        VenmoAPI sut = new VenmoAPI(braintreeClient, apiClient);

        VenmoOnActivityResultCallback callback = mock(VenmoOnActivityResultCallback.class);
        sut.createNonceFromPaymentContext("payment-context-id", callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((VenmoAccountNonce) isNull(), captor.capture());
        assertTrue(captor.getValue() instanceof JSONException);
    }

    @Test
    public void createNonceFromPaymentContext_whenGraphQLPostError_forwardsErrorToCallback() {
        Exception error = new Exception("error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTErrorResponse(error)
                .build();

        VenmoAPI sut = new VenmoAPI(braintreeClient, apiClient);

        VenmoOnActivityResultCallback callback = mock(VenmoOnActivityResultCallback.class);
        sut.createNonceFromPaymentContext("payment-context-id", callback);

        verify(callback).onResult((VenmoAccountNonce) isNull(), same(error));
    }

    @Test
    public void vaultVenmoAccountNonce_performsVaultRequest() throws JSONException {
        VenmoAPI sut = new VenmoAPI(braintreeClient, apiClient);
        sut.vaultVenmoAccountNonce("nonce", mock(VenmoOnActivityResultCallback.class));

        ArgumentCaptor<VenmoAccount> accountBuilderCaptor = ArgumentCaptor.forClass(VenmoAccount.class);
        verify(apiClient).tokenizeREST(accountBuilderCaptor.capture(), any(TokenizeCallback.class));

        VenmoAccount venmoAccount = accountBuilderCaptor.getValue();
        JSONObject venmoJSON = venmoAccount.buildJSON();
        assertEquals("nonce", venmoJSON.getJSONObject("venmoAccount").getString("nonce"));
    }

    @Test
    public void vaultVenmoAccountNonce_tokenizeRESTSuccess_callsBackNonce() throws JSONException {
        ApiClient apiClient = new MockApiClientBuilder()
                .tokenizeGraphQLSuccess(new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON))
                .build();
        VenmoAPI sut = new VenmoAPI(braintreeClient, apiClient);

        VenmoOnActivityResultCallback callback = mock(VenmoOnActivityResultCallback.class);
        sut.vaultVenmoAccountNonce("nonce", callback);

        verify(callback).onResult(VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON)), null);
    }

    @Test
    public void vaultVenmoAccountNonce_tokenizeRESTSuccess_responseMalformed_callsBackError() {

    }

    @Test
    public void vaultVenmoAccountNonce_tokenizeRESTError_forwardsErrorToCallback() {


    }
}
