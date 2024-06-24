package com.braintreepayments.api.paypal;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;

import com.braintreepayments.api.core.ApiClient;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.ClientToken;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.core.TokenizationKey;
import com.braintreepayments.api.core.TokenizeCallback;
import com.braintreepayments.api.datacollector.DataCollector;
import com.braintreepayments.api.datacollector.DataCollectorInternalRequest;
import com.braintreepayments.api.sharedutils.HttpResponseCallback;
import com.braintreepayments.api.testutils.Fixtures;
import com.braintreepayments.api.testutils.MockApiClientBuilder;
import com.braintreepayments.api.testutils.MockBraintreeClientBuilder;

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

@RunWith(RobolectricTestRunner.class)
public class PayPalInternalClientUnitTest {

    private Context context;
    private Configuration configuration;

    private ClientToken clientToken;
    private TokenizationKey tokenizationKey;

    private DataCollector dataCollector;
    private ApiClient apiClient;

    PayPalInternalClientCallback payPalInternalClientCallback;

    @Before
    public void beforeEach() throws JSONException {
        context = mock(Context.class);
        clientToken = mock(ClientToken.class);
        tokenizationKey = mock(TokenizationKey.class);
        configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);

        dataCollector = mock(DataCollector.class);
        apiClient = mock(ApiClient.class);
        payPalInternalClientCallback = mock(PayPalInternalClientCallback.class);
    }

    @Test
    public void sendRequest_withPayPalVaultRequest_sendsAllParameters() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(clientToken)
                .appLinkReturnUri(Uri.parse("https://example.com"))
                .build();
        when(clientToken.getBearer()).thenReturn("client-token-bearer");

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PostalAddress shippingAddressOverride = new PostalAddress();
        shippingAddressOverride.setRecipientName("Brianna Tree");
        shippingAddressOverride.setStreetAddress("123 Fake St.");
        shippingAddressOverride.setExtendedAddress("Apt. v.0");
        shippingAddressOverride.setLocality("Oakland");
        shippingAddressOverride.setRegion("CA");
        shippingAddressOverride.setPostalCode("12345");
        shippingAddressOverride.setCountryCodeAlpha2("US");

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);
        payPalRequest.setBillingAgreementDescription("Billing Agreement Description");
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalRequest.setLandingPageType("sample-landing-page-type");
        payPalRequest.setDisplayName("sample-display-name");
        payPalRequest.setLocaleCode("US");
        payPalRequest.setShippingAddressRequired(true);
        payPalRequest.setShippingAddressEditable(true);
        payPalRequest.setShouldOfferCredit(true);
        payPalRequest.setShippingAddressOverride(shippingAddressOverride);

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq("/v1/paypal_hermes/setup_billing_agreement"),
                captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        JSONObject expected = new JSONObject()
                .put("authorization_fingerprint", "client-token-bearer")
                .put("return_url", "https://example.com://onetouch/v1/success")
                .put("cancel_url", "https://example.com://onetouch/v1/cancel")
                .put("offer_paypal_credit", true)
                .put("description", "Billing Agreement Description")
                .put("experience_profile", new JSONObject()
                        .put("no_shipping", false)
                        .put("landing_page_type", "sample-landing-page-type")
                        .put("brand_name", "sample-display-name")
                        .put("locale_code", "US")
                        .put("address_override", false))
                .put("shipping_address", new JSONObject()
                        .put("line1", "123 Fake St.")
                        .put("line2", "Apt. v.0")
                        .put("city", "Oakland")
                        .put("state", "CA")
                        .put("postal_code", "12345")
                        .put("country_code", "US")
                        .put("recipient_name", "Brianna Tree"))
                .put("merchant_account_id", "sample-merchant-account-id");

        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_sendsAllParameters() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(clientToken)
                .appLinkReturnUri(Uri.parse("https://example.com"))
                .build();
        when(clientToken.getBearer()).thenReturn("client-token-bearer");

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PostalAddress shippingAddressOverride = new PostalAddress();
        shippingAddressOverride.setRecipientName("Brianna Tree");
        shippingAddressOverride.setStreetAddress("123 Fake St.");
        shippingAddressOverride.setExtendedAddress("Apt. v.0");
        shippingAddressOverride.setLocality("Oakland");
        shippingAddressOverride.setRegion("CA");
        shippingAddressOverride.setPostalCode("12345");
        shippingAddressOverride.setCountryCodeAlpha2("US");

        PayPalLineItem item = new PayPalLineItem(PayPalLineItem.KIND_DEBIT, "Item 0", "1", "2");
        item.setDescription("A new item");
        item.setProductCode("abc-123");
        item.setUnitTaxAmount("1.50");
        item.setUrl("http://example.com");

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        payPalRequest.setCurrencyCode("USD");
        payPalRequest.setIntent("authorize");
        payPalRequest.setShouldRequestBillingAgreement(true);
        payPalRequest.setBillingAgreementDescription("Billing Agreement Description");
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalRequest.setLandingPageType("sample-landing-page-type");
        payPalRequest.setDisplayName("sample-display-name");
        payPalRequest.setLocaleCode("US");
        payPalRequest.setShippingAddressRequired(true);
        payPalRequest.setShippingAddressEditable(true);
        payPalRequest.setShouldOfferPayLater(true);
        payPalRequest.setLineItems(Collections.singletonList(item));
        payPalRequest.setShippingAddressOverride(shippingAddressOverride);

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq("/v1/paypal_hermes/create_payment_resource"),
                captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        JSONObject expected = new JSONObject()
                .put("amount", "1.00")
                .put("currency_iso_code", "USD")
                .put("intent", "authorize")
                .put("authorization_fingerprint", "client-token-bearer")
                .put("return_url", "https://example.com://onetouch/v1/success")
                .put("cancel_url", "https://example.com://onetouch/v1/cancel")
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
                        .put("landing_page_type", "sample-landing-page-type")
                        .put("brand_name", "sample-display-name")
                        .put("locale_code", "US")
                        .put("address_override", false))
                .put("line1", "123 Fake St.")
                .put("line2", "Apt. v.0")
                .put("city", "Oakland")
                .put("state", "CA")
                .put("postal_code", "12345")
                .put("country_code", "US")
                .put("recipient_name", "Brianna Tree")
                .put("merchant_account_id", "sample-merchant-account-id");

        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    public void sendRequest_withTokenizationKey_sendsClientKeyParam() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(tokenizationKey)
                .build();
        when(tokenizationKey.getBearer()).thenReturn("tokenization-key-bearer");

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("authorization_fingerprint"));
        assertEquals("tokenization-key-bearer", actual.get("client_key"));
    }

    @Test
    public void sendRequest_withEmptyDisplayName_fallsBackToPayPalConfigurationDisplayName()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(false);
        payPalRequest.setDisplayName("");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertEquals("paypal_merchant",
                ((JSONObject) actual.get("experience_profile")).get("brand_name"));
    }

    @Test
    public void sendRequest_withLocaleNotSpecified_omitsLocale() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);
        payPalRequest.setLocaleCode(null);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(((JSONObject) actual.get("experience_profile")).has("locale_code"));
    }

    @Test
    public void sendRequest_withMerchantAccountIdNotSpecified_omitsMerchantAccountId()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);
        payPalRequest.setMerchantAccountId(null);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("merchant_account_id"));
    }

    @Test
    public void sendRequest_withShippingAddressOverrideNotSpecified_sendsAddressOverrideFalse()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);
        payPalRequest.setShippingAddressOverride(null);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertEquals(false,
                ((JSONObject) actual.get("experience_profile")).get("address_override"));
    }

    @Test
    public void sendRequest_withShippingAddressSpecified_sendsAddressOverrideBasedOnShippingAdressEditability()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(clientToken)
                .build();
        when(clientToken.getBearer()).thenReturn("client-token-bearer");

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);
        payPalRequest.setShippingAddressEditable(false);
        payPalRequest.setShippingAddressOverride(new PostalAddress());

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq("/v1/paypal_hermes/setup_billing_agreement"),
                captor.capture(), any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertEquals(true, ((JSONObject) actual.get("experience_profile")).get("address_override"));
    }

    @Test
    public void sendRequest_withPayPalVaultRequest_omitsEmptyBillingAgreementDescription()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);
        payPalRequest.setBillingAgreementDescription("");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("description"));
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_fallsBackToPayPalConfigurationCurrencyCode()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_INR))
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertEquals("INR", actual.get("currency_iso_code"));
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_omitsEmptyLineItems() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        payPalRequest.setLineItems(new ArrayList<PayPalLineItem>());
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("line_items"));
    }

    @Test
    public void sendRequest_whenRiskCorrelationIdNotNull_setsClientMetadataIdToRiskCorrelationId() {
        when(dataCollector.getClientMetadataId(same(context), any(), same(configuration))).thenReturn("sample-client-metadata-id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_RESPONSE)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        payPalRequest.setRiskCorrelationId("risk-correlation-id");

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalPaymentAuthRequestParams> captor = ArgumentCaptor.forClass(
                PayPalPaymentAuthRequestParams.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), isNull());

        PayPalPaymentAuthRequestParams payPalPaymentAuthRequestParams = captor.getValue();
        assertEquals("risk-correlation-id", payPalPaymentAuthRequestParams.getClientMetadataId());
    }

    @Test
    public void sendRequest_whenRiskCorrelationIdNull_setsClientMetadataIdFromPayPalDataCollector() {
        when(dataCollector.getClientMetadataId(same(context), any(), any())).thenReturn("sample-client-metadata-id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_RESPONSE)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalPaymentAuthRequestParams> captor = ArgumentCaptor.forClass(
                PayPalPaymentAuthRequestParams.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), (Exception) isNull());

        PayPalPaymentAuthRequestParams payPalPaymentAuthRequestParams = captor.getValue();
        assertNull(payPalRequest.getRiskCorrelationId());
        assertEquals("sample-client-metadata-id", payPalPaymentAuthRequestParams.getClientMetadataId());
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_whenRequestBillingAgreementFalse_andBillingAgreementDescriptionSet_doesNotSettBillingAgreementDescription()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(tokenizationKey)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        payPalRequest.setShouldRequestBillingAgreement(false);
        payPalRequest.setBillingAgreementDescription("Billing agreement description");
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(anyString(), captor.capture(),
                any(HttpResponseCallback.class));

        String result = captor.getValue();
        JSONObject actual = new JSONObject(result);

        assertFalse(actual.has("request_billing_agreement"));
        assertFalse(actual.has("description"));
    }

    @Test
    public void sendRequest_withPayPalVaultRequest_callsBackPayPalResponseOnSuccess() {
        when(dataCollector.getClientMetadataId(context, configuration, true)).thenReturn("sample-client-metadata-id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(clientToken)
                .appLinkReturnUri(Uri.parse("https://example.com"))
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_RESPONSE_WITH_BA_TOKEN_PARAM)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalRequest.setRiskCorrelationId("sample-client-metadata-id");

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalPaymentAuthRequestParams> captor = ArgumentCaptor.forClass(
                PayPalPaymentAuthRequestParams.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), (Exception) isNull());

        String expectedUrl =
                "https://checkout.paypal.com/one-touch-login-sandbox/index.html?ba_token=fake-ba-token&action=create_payment_resource&amount=1.00&authorization_fingerprint=63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06%7Ccreated_at%3D2015-10-13T18%3A49%3A48.371382792%2B0000%26merchant_id%3Ddcpspy2brwdjr3qn%26public_key%3D9wwrzqk3vr3t4nc8&cancel_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fcancel&controller=client_api%2Fpaypal_hermes&currency_iso_code=USD&experience_profile%5Baddress_override%5D=false&experience_profile%5Bno_shipping%5D=false&merchant_id=dcpspy2brwdjr3qn&return_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fsuccess&offer_paypal_credit=true&version=1";
        PayPalPaymentAuthRequestParams payPalPaymentAuthRequestParams = captor.getValue();
        assertTrue(payPalPaymentAuthRequestParams.isBillingAgreement());
        assertEquals("sample-merchant-account-id", payPalPaymentAuthRequestParams.getMerchantAccountId());
        assertEquals("https://example.com://onetouch/v1/success", payPalPaymentAuthRequestParams.getSuccessUrl());
        assertEquals("fake-ba-token", payPalPaymentAuthRequestParams.getPairingId());
        assertEquals("sample-client-metadata-id", payPalPaymentAuthRequestParams.getClientMetadataId());
        assertEquals(expectedUrl, payPalPaymentAuthRequestParams.getApprovalUrl());
    }

    @Test
    public void sendRequest_withPayPalCheckoutRequest_callsBackPayPalResponseOnSuccess() {
        when(dataCollector.getClientMetadataId(context, configuration, true)).thenReturn("sample-client-metadata-id");

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_RESPONSE_WITH_TOKEN_PARAM)
                .appLinkReturnUri(Uri.parse("https://example.com"))
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        payPalRequest.setIntent("authorize");
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalRequest.setUserAction(PayPalCheckoutRequest.USER_ACTION_COMMIT);
        payPalRequest.setRiskCorrelationId("sample-client-metadata-id");

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<PayPalPaymentAuthRequestParams> captor = ArgumentCaptor.forClass(
                PayPalPaymentAuthRequestParams.class);
        verify(payPalInternalClientCallback).onResult(captor.capture(), (Exception) isNull());

        String expectedUrl =
                "https://checkout.paypal.com/one-touch-login-sandbox/index.html?token=fake-token&action=create_payment_resource&amount=1.00&authorization_fingerprint=63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06%7Ccreated_at%3D2015-10-13T18%3A49%3A48.371382792%2B0000%26merchant_id%3Ddcpspy2brwdjr3qn%26public_key%3D9wwrzqk3vr3t4nc8&cancel_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fcancel&controller=client_api%2Fpaypal_hermes&currency_iso_code=USD&experience_profile%5Baddress_override%5D=false&experience_profile%5Bno_shipping%5D=false&merchant_id=dcpspy2brwdjr3qn&return_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fsuccess&offer_paypal_credit=true&version=1";
        PayPalPaymentAuthRequestParams payPalPaymentAuthRequestParams = captor.getValue();
        assertFalse(payPalPaymentAuthRequestParams.isBillingAgreement());
        assertEquals("authorize", payPalPaymentAuthRequestParams.getIntent());
        assertEquals("sample-merchant-account-id", payPalPaymentAuthRequestParams.getMerchantAccountId());
        assertEquals("https://example.com://onetouch/v1/success", payPalPaymentAuthRequestParams.getSuccessUrl());
        assertEquals("fake-token", payPalPaymentAuthRequestParams.getPairingId());
        assertEquals("sample-client-metadata-id", payPalPaymentAuthRequestParams.getClientMetadataId());
        assertEquals(expectedUrl, payPalPaymentAuthRequestParams.getApprovalUrl());
    }

    @Test
    public void sendRequest_propagatesHttpErrors() {
        Exception httpError = new Exception("http error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(clientToken)
                .sendPOSTErrorResponse(httpError)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        verify(payPalInternalClientCallback).onResult(null, httpError);
    }

    @Test
    public void sendRequest_propagatesMalformedJSONResponseErrors() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(configuration)
                .authorizationSuccess(clientToken)
                .sendPOSTSuccessfulResponse("{bad:")
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        verify(payPalInternalClientCallback).onResult((PayPalPaymentAuthRequestParams) isNull(),
                any(JSONException.class));
    }

    @Test
    public void sendRequest_onConfigurationFailure_forwardsError() {
        Exception configurationError = new Exception("configuration error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(clientToken)
                .configurationError(configurationError)
                .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        verify(payPalInternalClientCallback).onResult(null, configurationError);
    }

    @Test
    public void tokenize_tokenizesWithApiClient() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalAccount payPalAccount = mock(PayPalAccount.class);
        PayPalInternalTokenizeCallback callback = mock(PayPalInternalTokenizeCallback.class);

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        sut.tokenize(payPalAccount, callback);

        verify(apiClient).tokenizeREST(same(payPalAccount), any(TokenizeCallback.class));
    }

    @Test
    public void tokenize_onTokenizeResult_returnsAccountNonceToCallback() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        ApiClient apiClient = new MockApiClientBuilder()
                .tokenizeRESTSuccess(
                        new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE))
                .build();
        PayPalAccount payPalAccount = mock(PayPalAccount.class);
        PayPalInternalTokenizeCallback callback = mock(PayPalInternalTokenizeCallback.class);

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        sut.tokenize(payPalAccount, callback);

        ArgumentCaptor<PayPalAccountNonce> captor =
                ArgumentCaptor.forClass(PayPalAccountNonce.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());

        PayPalAccountNonce expectedNonce = PayPalAccountNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE));
        PayPalAccountNonce result = captor.getValue();
        assertEquals(expectedNonce.getString(), result.getString());
    }

    @Test
    public void tokenize_onTokenizeError_returnsErrorToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        Exception error = new Exception("error");
        ApiClient apiClient = new MockApiClientBuilder()
                .tokenizeRESTError(error)
                .build();
        PayPalAccount payPalAccount = mock(PayPalAccount.class);
        PayPalInternalTokenizeCallback callback = mock(PayPalInternalTokenizeCallback.class);

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        sut.tokenize(payPalAccount, callback);

        verify(callback).onResult((PayPalAccountNonce) isNull(), same(error));
    }

    @Test
    public void payPalDataCollector_passes_correct_arguments_to_getClientMetadataId() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(configuration)
            .authorizationSuccess(clientToken)
            .returnUrlScheme("sample-scheme")
            .sendPOSTSuccessfulResponse(Fixtures.PAYPAL_HERMES_RESPONSE)
            .build();

        PayPalInternalClient sut = new PayPalInternalClient(braintreeClient, dataCollector, apiClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);
        payPalRequest.setIntent("authorize");
        payPalRequest.setMerchantAccountId("sample-merchant-account-id");

        sut.sendRequest(context, payPalRequest, payPalInternalClientCallback);

        ArgumentCaptor<DataCollectorInternalRequest> captor = ArgumentCaptor.forClass(
            DataCollectorInternalRequest.class);

        verify(dataCollector).getClientMetadataId(same(context), captor.capture(), same(configuration));

        assertTrue(captor.getValue().getHasUserLocationConsent());
    }
}