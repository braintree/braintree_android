package com.braintreepayments.api;

import static junit.framework.TestCase.assertNull;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PayPalNativeCheckoutInternalClientUnitTest {

    private Context context;
    private Configuration configuration;

    private ClientToken clientToken;
    private TokenizationKey tokenizationKey;

    private PayPalDataCollector payPalDataCollector;
    private ApiClient apiClient;

    PayPalNativeCheckoutInternalClient.PayPalNativeCheckoutInternalClientCallback payPalInternalClientCallback;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        configuration = mock(Configuration.class);
        clientToken = mock(ClientToken.class);
        tokenizationKey = mock(TokenizationKey.class);

        payPalDataCollector = mock(PayPalDataCollector.class);
        apiClient = mock(ApiClient.class);
        payPalInternalClientCallback = mock(PayPalNativeCheckoutInternalClient.PayPalNativeCheckoutInternalClientCallback.class);
    }

    @Test
    public void sendRequest_withPayPalVaultRequest_sendsAllParameters() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .returnUrlScheme("sample-scheme")
                .build();
        when(clientToken.getBearer()).thenReturn("client-token-bearer");

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();
        payPalRequest.setBillingAgreementDescription("Billing Agreement Description");
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalRequest.setDisplayName("sample-display-name");
        payPalRequest.setLocaleCode("US");
        payPalRequest.setShippingAddressRequired(true);
        payPalRequest.setShippingAddressEditable(true);
        payPalRequest.setShouldOfferCredit(true);

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq("/v1/paypal_hermes/setup_billing_agreement"), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        JSONObject expected = new JSONObject()
                .put("authorization_fingerprint", "client-token-bearer")
                .put("return_url", "sample-scheme://onetouch/v1/success")
                .put("cancel_url", "sample-scheme://onetouch/v1/cancel")
                .put("offer_paypal_credit", true)
                .put("description", "Billing Agreement Description")
                .put("experience_profile", new JSONObject()
                        .put("no_shipping", false)
                        .put("brand_name", "sample-display-name")
                        .put("locale_code", "US"))
                .put("merchant_account_id", "sample-merchant-account-id");

        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_sendsAllParameters() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .returnUrlScheme("sample-scheme")
                .build();
        when(clientToken.getBearer()).thenReturn("client-token-bearer");

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutLineItem item = new PayPalNativeCheckoutLineItem(PayPalNativeCheckoutLineItem.KIND_DEBIT, "Item 0", "1", "2");
        item.setDescription("A new item");
        item.setProductCode("abc-123");
        item.setUnitTaxAmount("1.50");
        item.setUrl("http://example.com");

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalRequest.setCurrencyCode("USD");
        payPalRequest.setIntent("authorize");
        payPalRequest.setShouldRequestBillingAgreement(true);
        payPalRequest.setBillingAgreementDescription("Billing Agreement Description");
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalRequest.setDisplayName("sample-display-name");
        payPalRequest.setLocaleCode("US");
        payPalRequest.setShippingAddressRequired(true);
        payPalRequest.setShippingAddressEditable(true);
        payPalRequest.setShouldOfferPayLater(true);
        payPalRequest.setLineItems(Collections.singletonList(item));

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq("/v1/paypal_hermes/create_payment_resource"), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        JSONObject expected = new JSONObject()
                .put("amount", "1.00")
                .put("currency_iso_code", "USD")
                .put("intent", "authorize")
                .put("authorization_fingerprint", "client-token-bearer")
                .put("return_url", "sample-scheme://onetouch/v1/success")
                .put("cancel_url", "sample-scheme://onetouch/v1/cancel")
                .put("offer_pay_later", true)
                .put("request_billing_agreement", true)
                .put("billing_agreement_details", new JSONObject()
                        .put("description", "Billing Agreement Description"))
                .put("line_items", new JSONArray()
                        .put(new JSONObject()
                                .put("kind", "debit")
                                .put("name", "Item 0")
                                .put("quantity", "1")
                                .put("unit_amount", "2")
                                .put("description", "A new item")
                                .put("product_code", "abc-123")
                                .put("unit_tax_amount", "1.50")
                                .put("url", "http://example.com")))
                .put("experience_profile", new JSONObject()
                        .put("no_shipping", false)
                        .put("brand_name", "sample-display-name")
                        .put("locale_code", "US"))
                .put("merchant_account_id", "sample-merchant-account-id");

        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    public void sendRequest_withTokenizationKey_sendsClientKeyParam() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(tokenizationKey)
                .build();
        when(tokenizationKey.getBearer()).thenReturn("tokenization-key-bearer");

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("authorization_fingerprint"));
        assertEquals("tokenization-key-bearer", actual.get("client_key"));
    }

    @Test
    public void sendRequest_withEmptyDisplayName_fallsBackToPayPalConfigurationDisplayName() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();
        payPalRequest.setDisplayName("");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertEquals("paypal_merchant", ((JSONObject) actual.get("experience_profile")).get("brand_name"));
    }

    @Test
    public void sendRequest_withLocaleNotSpecified_omitsLocale() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();
        payPalRequest.setLocaleCode(null);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(((JSONObject) actual.get("experience_profile")).has("locale_code"));
    }

    @Test
    public void sendRequest_withMerchantAccountIdNotSpecified_omitsMerchantAccountId() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();
        payPalRequest.setMerchantAccountId(null);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("merchant_account_id"));
    }

    @Test
    public void sendRequest_withPayPalVaultRequest_omitsEmptyBillingAgreementDescription() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();
        payPalRequest.setBillingAgreementDescription("");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("description"));
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_fallsBackToPayPalConfigurationCurrencyCode() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_INR))
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertEquals("INR", actual.get("currency_iso_code"));
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_omitsEmptyLineItems() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalRequest.setLineItems(new ArrayList<PayPalNativeCheckoutLineItem>());
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("line_items"));
    }

    @Test
    public void sendRequest_whenRiskCorrelationIdNotNull_setsClientMetadataIdToRiskCorrelationId() throws Exception {
        when(payPalDataCollector.getClientMetadataId(context, configuration, null)).thenReturn("sample-client-metadata-id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_RESPONSE)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalRequest.setRiskCorrelationId("risk-correlation-id");

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalNativeCheckoutResponse> captor = ArgumentCaptor.forClass(PayPalNativeCheckoutResponse.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), isNull());

        PayPalNativeCheckoutResponse payPalResponse = captor.getValue();
        assertEquals("risk-correlation-id", payPalResponse.getClientMetadataId());
    }

    @Test
    public void sendRequest_whenRiskCorrelationIdNull_setsClientMetadataIdFromPayPalDataCollector() throws Exception {
        when(payPalDataCollector.getClientMetadataId(any(), any(), (String) isNull())).thenReturn("sample-client-metadata-id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_RESPONSE)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalNativeCheckoutResponse> captor = ArgumentCaptor.forClass(PayPalNativeCheckoutResponse.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), isNull());

        PayPalNativeCheckoutResponse payPalResponse = captor.getValue();
        assertNull(payPalRequest.getRiskCorrelationId());
        assertEquals("sample-client-metadata-id", payPalResponse.getClientMetadataId());
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_whenRequestBillingAgreementFalse_andBillingAgreementDescriptionSet_doesNotSettBillingAgreementDescription() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalRequest.setShouldRequestBillingAgreement(false);
        payPalRequest.setBillingAgreementDescription("Billing agreement description");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("request_billing_agreement"));
        assertFalse(actual.has("description"));
    }

    @Test
    public void sendRequest_withPayPalVaultRequest_callsBackPayPalResponseOnSuccess() throws Exception {
        when(payPalDataCollector.getClientMetadataId(any(), any(), (String) isNull())).thenReturn("sample-client-metadata-id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .returnUrlScheme("sample-scheme")
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_BILLING_AGREEMENT_RESPONSE)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalNativeCheckoutResponse> captor = ArgumentCaptor.forClass(PayPalNativeCheckoutResponse.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), isNull());

        String expectedUrl =
                "https://checkout.paypal.com/one-touch-login-sandbox/index.html?action=create_payment_resource\u0026authorization_fingerprint=63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06%7Ccreated_at%3D2015-10-13T18%3A49%3A48.371382792%2B0000%26merchant_id%3Ddcpspy2brwdjr3qn%26public_key%3D9wwrzqk3vr3t4nc8\u0026cancel_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fcancel\u0026controller=client_api%2Fpaypal_hermes\u0026experience_profile%5Baddress_override%5D=false\u0026experience_profile%5Bno_shipping%5D=false\u0026merchant_id=dcpspy2brwdjr3qn\u0026return_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fsuccess\u0026ba_token=EC-HERMES-SANDBOX-EC-TOKEN\u0026offer_paypal_credit=true\u0026version=1\u0026useraction=";

        PayPalNativeCheckoutResponse payPalResponse = captor.getValue();
        assertTrue(payPalResponse.isBillingAgreement());
        assertEquals("sample-merchant-account-id", payPalResponse.getMerchantAccountId());
        assertEquals("EC-HERMES-SANDBOX-EC-TOKEN", payPalResponse.getPairingId());
        assertEquals("sample-client-metadata-id", payPalResponse.getClientMetadataId());
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_callsBackPayPalResponseOnSuccess() throws Exception {
        when(payPalDataCollector.getClientMetadataId(any(), any(), (String) isNull())).thenReturn("sample-client-metadata-id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_RESPONSE)
                .returnUrlScheme("sample-scheme")
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalRequest.setIntent("authorize");
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalRequest.setUserAction(PayPalNativeCheckoutRequest.USER_ACTION_COMMIT);

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalNativeCheckoutResponse> captor = ArgumentCaptor.forClass(PayPalNativeCheckoutResponse.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), isNull());

        PayPalNativeCheckoutResponse payPalResponse = captor.getValue();
        assertFalse(payPalResponse.isBillingAgreement());
        assertEquals("authorize", payPalResponse.getIntent());
        assertEquals("sample-merchant-account-id", payPalResponse.getMerchantAccountId());
        assertEquals("EC-HERMES-SANDBOX-EC-TOKEN", payPalResponse.getPairingId());
        assertEquals("sample-client-metadata-id", payPalResponse.getClientMetadataId());
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_setsApprovalUrlUserActionToEmptyStringOnDefault() throws Exception {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_RESPONSE)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalRequest.setIntent("authorize");
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalNativeCheckoutResponse> captor = ArgumentCaptor.forClass(PayPalNativeCheckoutResponse.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), isNull());
    }

    @Test
    public void sendRequest_withPayPalVaultRequest_setsApprovalUrlUserActionToEmptyStringOnDefault() throws Exception {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_BILLING_AGREEMENT_RESPONSE)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalNativeCheckoutResponse> captor = ArgumentCaptor.forClass(PayPalNativeCheckoutResponse.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), isNull());
    }

    @Test
    public void sendRequest_propagatesHttpErrors() throws Exception {
        Exception httpError = new Exception("http error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .sendPOSTErrorResponse(httpError)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        verify(payPalInternalClientCallback).onResult(null, httpError);
    }

    @Test
    public void sendRequest_propagatesMalformedJSONResponseErrors() throws Exception {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL))
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse("{bad:")
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        verify(payPalInternalClientCallback).onResult(isNull(), any(JSONException.class));
    }

    @Test
    public void sendRequest_onAuthorizationFailure_forwardsError() throws Exception {
        Exception authError = new Exception("authorization error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationError(authError)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        verify(payPalInternalClientCallback).onResult(null, authError);
    }

    @Test
    public void sendRequest_onConfigurationFailure_forwardsError() throws Exception {
        Exception configurationError = new Exception("configuration error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(clientToken)
                .configurationError(configurationError)
                .build();

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        verify(payPalInternalClientCallback).onResult(null, configurationError);
    }

    @Test
    public void tokenize_tokenizesWithApiClient() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalNativeCheckoutAccount payPalAccount = mock(PayPalNativeCheckoutAccount.class);
        PayPalNativeCheckoutResultCallback callback = mock(PayPalNativeCheckoutResultCallback.class);

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        sut.tokenize(payPalAccount, callback);

        verify(apiClient).tokenizeREST(same(payPalAccount), any(TokenizeCallback.class));
    }

    @Test
    public void tokenize_onTokenizeResult_returnsAccountNonceToCallback() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ApiClient apiClient = new MockApiClientBuilder()
                .tokenizeRESTSuccess(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE))
                .build();
        PayPalNativeCheckoutAccount payPalAccount = mock(PayPalNativeCheckoutAccount.class);
        PayPalNativeCheckoutResultCallback callback = mock(PayPalNativeCheckoutResultCallback.class);

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        sut.tokenize(payPalAccount, callback);

        ArgumentCaptor<PayPalNativeCheckoutAccountNonce> captor = ArgumentCaptor.forClass(PayPalNativeCheckoutAccountNonce.class);
        verify(callback).onResult(captor.capture(), isNull());

        PayPalNativeCheckoutAccountNonce expectedNonce = PayPalNativeCheckoutAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE));
        PayPalNativeCheckoutAccountNonce result = captor.getValue();
        assertEquals(expectedNonce.getString(), result.getString());
    }

    @Test
    public void tokenize_onTokenizeError_returnsErrorToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        Exception error = new Exception("error");
        ApiClient apiClient = new MockApiClientBuilder()
                .tokenizeRESTError(error)
                .build();
        PayPalNativeCheckoutAccount payPalAccount = mock(PayPalNativeCheckoutAccount.class);
        PayPalNativeCheckoutResultCallback callback = mock(PayPalNativeCheckoutResultCallback.class);

        PayPalNativeCheckoutInternalClient sut = new PayPalNativeCheckoutInternalClient(braintreeClient, payPalDataCollector, apiClient);

        sut.tokenize(payPalAccount, callback);

        verify(callback).onResult(isNull(), same(error));
    }
}