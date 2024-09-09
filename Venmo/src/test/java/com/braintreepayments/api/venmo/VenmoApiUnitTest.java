package com.braintreepayments.api.venmo;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.braintreepayments.api.testutils.Fixtures;
import com.braintreepayments.api.testutils.MockApiClientBuilder;
import com.braintreepayments.api.testutils.MockBraintreeClientBuilder;
import com.braintreepayments.api.core.ApiClient;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.TokenizeCallback;
import com.braintreepayments.api.sharedutils.HttpResponseCallback;

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
    public void beforeEach() {
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

        venmoAPI.createPaymentContext(request, request.getProfileId(),
                mock(VenmoApiCallback.class));

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));

        JSONObject graphQLJSON = captor.getValue();

        JSONObject variables = graphQLJSON.getJSONObject("variables");
        JSONObject input = variables.getJSONObject("input");
        assertEquals("SINGLE_USE", input.getString("paymentMethodUsage"));
        assertEquals("sample-venmo-merchant", input.getString("merchantProfileId"));
        assertEquals("MOBILE_APP", input.getString("customerClient"));
        assertEquals("CONTINUE", input.getString("intent"));
        assertEquals("display-name", input.getString("displayName"));

        JSONObject metadata = graphQLJSON.getJSONObject("clientSdkMetadata");
        assertEquals(com.braintreepayments.api.core.BuildConfig.VERSION_NAME, metadata.getString("version"));
        assertEquals("android", metadata.getString("platform"));

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

        venmoAPI.createPaymentContext(request, request.getProfileId(),
                mock(VenmoApiCallback.class));

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));

        JSONObject graphQLJSON = captor.getValue();

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
                .sendGraphQLPOSTSuccessfulResponse(
                        Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_CONTEXT_RESPONSE)
                .build();
        VenmoApi venmoAPI = new VenmoApi(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");

        VenmoApiCallback callback = mock(VenmoApiCallback.class);
        venmoAPI.createPaymentContext(request, request.getProfileId(), callback);

        verify(callback).onResult(anyString(), isNull());
    }

    @Test
    public void createPaymentContext_whenGraphQLPostSuccess_missingPaymentContextID_callsBackError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse(
                        Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_RESPONSE_WITHOUT_PAYMENT_CONTEXT_ID)
                .build();
        VenmoApi venmoAPI = new VenmoApi(braintreeClient, apiClient);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");

        VenmoApiCallback callback = mock(VenmoApiCallback.class);
        venmoAPI.createPaymentContext(request, request.getProfileId(), callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult(isNull(), captor.capture());

        Exception error = captor.getValue();
        assertTrue(error instanceof BraintreeException);
        assertEquals("Failed to fetch a Venmo paymentContextId while constructing the requestURL.",
                error.getMessage());
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

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));

        JSONObject graphQLJSON = captor.getValue();

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

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));

        JSONObject graphQLJSON = captor.getValue();

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
        sut.createNonceFromPaymentContext("payment-context-id", mock(VenmoInternalCallback.class));

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(braintreeClient).sendGraphQLPOST(captor.capture(), any(HttpResponseCallback.class));

        JSONObject jsonPayload = captor.getValue();
        assertEquals("payment-context-id", jsonPayload.getJSONObject("variables").get("id"));
    }

    @Test
    public void createNonceFromPaymentContext_whenGraphQLPostSuccess_forwardsNonceToCallback() {
        String graphQLResponse = Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE;
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse(graphQLResponse)
                .build();

        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);

        VenmoInternalCallback callback = mock(VenmoInternalCallback.class);
        sut.createNonceFromPaymentContext("payment-context-id", callback);

        ArgumentCaptor<VenmoAccountNonce> captor = ArgumentCaptor.forClass(VenmoAccountNonce.class);
        verify(callback).onResult(captor.capture(), isNull());
        assertEquals("@somebody", captor.getValue().getUsername());
    }

    @Test
    public void createNonceFromPaymentContext_whenGraphQLPostResponseMalformed_callsBackError() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTSuccessfulResponse("not-json")
                .build();

        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);

        VenmoInternalCallback callback = mock(VenmoInternalCallback.class);
        sut.createNonceFromPaymentContext("payment-context-id", callback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult(isNull(), captor.capture());
        assertTrue(captor.getValue() instanceof JSONException);
    }

    @Test
    public void createNonceFromPaymentContext_whenGraphQLPostError_forwardsErrorToCallback() {
        Exception error = new Exception("error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendGraphQLPOSTErrorResponse(error)
                .build();

        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);

        VenmoInternalCallback callback = mock(VenmoInternalCallback.class);
        sut.createNonceFromPaymentContext("payment-context-id", callback);

        verify(callback).onResult(isNull(), same(error));
    }

    @Test
    public void vaultVenmoAccountNonce_performsVaultRequest() throws JSONException {
        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);
        sut.vaultVenmoAccountNonce("nonce", mock(VenmoInternalCallback.class));

        ArgumentCaptor<VenmoAccount> accountBuilderCaptor =
                ArgumentCaptor.forClass(VenmoAccount.class);
        verify(apiClient).tokenizeREST(accountBuilderCaptor.capture(), any(TokenizeCallback.class));

        VenmoAccount venmoAccount = accountBuilderCaptor.getValue();
        JSONObject venmoJSON = venmoAccount.buildJSON();
        assertEquals("nonce", venmoJSON.getJSONObject("venmoAccount").getString("nonce"));
    }

    @Test
    public void vaultVenmoAccountNonce_tokenizeRESTSuccess_callsBackNonce() throws JSONException {
        ApiClient apiClient = new MockApiClientBuilder()
                .tokenizeRESTSuccess(new JSONObject(
                        Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_WITH_NULL_PAYER_INFO_JSON))
                .build();
        VenmoApi sut = new VenmoApi(braintreeClient, apiClient);

        VenmoInternalCallback callback = mock(VenmoInternalCallback.class);
        sut.vaultVenmoAccountNonce("nonce", callback);

        ArgumentCaptor<VenmoAccountNonce> captor = ArgumentCaptor.forClass(VenmoAccountNonce.class);
        verify(callback).onResult(captor.capture(), isNull());

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

        VenmoInternalCallback callback = mock(VenmoInternalCallback.class);
        sut.vaultVenmoAccountNonce("nonce", callback);

        verify(callback).onResult(isNull(), same(error));
    }
}
