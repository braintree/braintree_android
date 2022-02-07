package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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
    public void vaultVenmoAccountNonce_performsVaultRequest() throws JSONException {
        VenmoAPI sut = new VenmoAPI(braintreeClient, apiClient);
        sut.vaultVenmoAccountNonce("nonce", mock(VenmoOnActivityResultCallback.class));

        ArgumentCaptor<VenmoAccount> accountBuilderCaptor = ArgumentCaptor.forClass(VenmoAccount.class);
        verify(apiClient).tokenizeREST(accountBuilderCaptor.capture(), any(TokenizeCallback.class));

        VenmoAccount venmoAccount = accountBuilderCaptor.getValue();
        JSONObject venmoJSON = venmoAccount.buildJSON();
        assertEquals("nonce", venmoJSON.getJSONObject("venmoAccount").getString("nonce"));
    }

}
