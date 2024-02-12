package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class VenmoApiUnitTest {

    private BraintreeClient braintreeClient;
    private ApiClient apiClient;

    @Before
    public void beforeEach() throws JSONException {
        braintreeClient = mock(BraintreeClient.class);
        apiClient = mock(ApiClient.class);
    }

    @Test
    public void createPaymentContext_createsPaymentContextViaGraphQL() throws JSONException {
        VenmoApi venmoAPI = new VenmoApi(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setShouldVault(false);
        request.setDisplayName("display-name");
        request.setCollectCustomerBillingAddress(true);
        request.setCollectCustomerShippingAddress(true);
        request.setTotalAmount("100");
        request.setSubTotalAmount("90");
        request.setTaxAmount("9.00");
        request.setShippingAmount("1");
        ArrayList<VenmoLineItem> lineItems = new ArrayList<>();
        lineItems.add(new VenmoLineItem(VenmoLineItem.KIND_DEBIT, "Some Item", 1, "1"));
        request.setLineItems(lineItems);

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

        JSONObject paysheetDetails = input.getJSONObject("paysheetDetails");
        assertEquals("true", paysheetDetails.getString("collectCustomerBillingAddress"));
        assertEquals("true", paysheetDetails.getString("collectCustomerShippingAddress"));
        JSONObject transactionDetails = paysheetDetails.getJSONObject("transactionDetails");
        assertEquals("1", transactionDetails.getString("shippingAmount"));
        assertEquals("9.00", transactionDetails.getString("taxAmount"));
        assertEquals("90", transactionDetails.getString("subTotalAmount"));
        assertEquals("100", transactionDetails.getString("totalAmount"));
        assertFalse(transactionDetails.has("discountAmount"));

        lineItems.get(0).setUnitTaxAmount("0");
        JSONArray expectedLineItems = new JSONArray().put(lineItems.get(0).toJson());
        assertEquals(expectedLineItems.toString(), transactionDetails.getString("lineItems"));
    }

    @Test
    public void createPaymentContext_whenTransactionAmountOptionsMissing() throws JSONException {
        VenmoApi venmoAPI = new VenmoApi(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setShouldVault(false);
        request.setCollectCustomerBillingAddress(true);

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
        JSONObject paysheetDetails = input.getJSONObject("paysheetDetails");
        assertEquals("true", paysheetDetails.getString("collectCustomerBillingAddress"));
        assertFalse(paysheetDetails.has("transactionDetails"));
        assertFalse(paysheetDetails.has("lineItems"));
    }

    @Test
    public void createPaymentContext_whenGraphQLPostSuccess_includesPaymentContextID_callsBackNull() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_CONTEXT_RESPONSE)
                .build();
        VenmoApi venmoAPI = new VenmoApi(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");

        VenmoApiCallback callback = mock(VenmoApiCallback.class);
        venmoAPI.createPaymentContext(request, request.getProfileId(), callback);

        verify(callback).onResult(anyString(), (Exception) isNull());
    }

    @Test
    public void createPaymentContext_whenGraphQLPostSuccess_missingPaymentContextID_callsBackError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_RESPONSE_WITHOUT_PAYMENT_CONTEXT_ID)
                .build();
        VenmoApi venmoAPI = new VenmoApi(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");

        VenmoApiCallback callback = mock(VenmoApiCallback.class);
        venmoAPI.createPaymentContext(request, request.getProfileId(), callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((String) isNull(), captor.capture());

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
        VenmoApi venmoAPI = new VenmoApi(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");

        VenmoApiCallback callback = mock(VenmoApiCallback.class);
        venmoAPI.createPaymentContext(request, request.getProfileId(), callback);

        verify(callback).onResult(null, error);
    }

    @Test
    public void createPaymentContext_withTotalAmountAndSetsFinalAmountToTrue() throws JSONException {
        VenmoApi venmoAPI = new VenmoApi(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setFinalAmount(true);
        request.setTotalAmount("5.99");

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
        assertEquals(true, input.getBoolean("isFinalAmount"));

        JSONObject paysheetDetails = input.getJSONObject("paysheetDetails");
        JSONObject transactionDetails = paysheetDetails.getJSONObject("transactionDetails");
        assertEquals("5.99", transactionDetails.getString("totalAmount"));
    }

    @Test
    public void createPaymentContext_withTotalAmountAndSetsFinalAmountToFalse() throws JSONException {
        VenmoApi venmoAPI = new VenmoApi(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setFinalAmount(false);
        request.setTotalAmount("5.99");

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
        assertEquals(false, input.getBoolean("isFinalAmount"));

        JSONObject paysheetDetails = input.getJSONObject("paysheetDetails");
        JSONObject transactionDetails = paysheetDetails.getJSONObject("transactionDetails");
        assertEquals("5.99", transactionDetails.getString("totalAmount"));
    }

    @Test
    public void createNonceFromPaymentContext_queriesGraphQLPaymentContext() throws JSONException {
        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);
        sut.createNonceFromPaymentContext("payment-context-id", mock(VenmoOnActivityResultCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));

        String payload = captor.getValue();
        JSONObject jsonPayload = new JSONObject(payload);
        String expectedQuery = "query PaymentContext($id: ID!) { node(id: $id) { ... on VenmoPaymentContext { paymentMethodId userName payerInfo { firstName lastName phoneNumber email externalId userName " +
                "shippingAddress { fullName addressLine1 addressLine2 adminArea1 adminArea2 postalCode countryCode } billingAddress { fullName addressLine1 addressLine2 adminArea1 adminArea2 postalCode countryCode } } } } }";
        assertEquals(expectedQuery, jsonPayload.get("query"));
        assertEquals("payment-context-id", jsonPayload.getJSONObject("variables").get("id"));
    }

    @Test
    public void createNonceFromPaymentContext_whenGraphQLPostSuccess_forwardsNonceToCallback() throws JSONException {
        String graphQLResponse = Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE;
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse(graphQLResponse)
                .build();

        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);

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

        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);

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

        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);

        VenmoOnActivityResultCallback callback = mock(VenmoOnActivityResultCallback.class);
        sut.createNonceFromPaymentContext("payment-context-id", callback);

        verify(callback).onResult((VenmoAccountNonce) isNull(), same(error));
    }

    @Test
    public void vaultVenmoAccountNonce_performsVaultRequest() throws JSONException {
        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);
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
                .tokenizeRESTSuccess(new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_WITH_NULL_PAYER_INFO_JSON))
                .build();
        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);

        VenmoOnActivityResultCallback callback = mock(VenmoOnActivityResultCallback.class);
        sut.vaultVenmoAccountNonce("nonce", callback);

        ArgumentCaptor<VenmoAccountNonce> captor = ArgumentCaptor.forClass(VenmoAccountNonce.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());

        VenmoAccountNonce nonce = captor.getValue();
        assertEquals("@sampleuser", nonce.getUsername());
    }

    @Test
    public void vaultVenmoAccountNonce_tokenizeRESTError_forwardsErrorToCallback() {
        Exception error = new Exception("error");
        ApiClient apiClient = new MockApiClientBuilder()
                .tokenizeRESTError(error)
                .build();
        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);

        VenmoOnActivityResultCallback callback = mock(VenmoOnActivityResultCallback.class);
        sut.vaultVenmoAccountNonce("nonce", callback);

        verify(callback).onResult((VenmoAccountNonce) isNull(), same(error));
    }
}
