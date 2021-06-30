package com.braintreepayments.api;

import android.app.Application;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import com.paypal.checkout.PayPalCheckout;
import com.paypal.checkout.approve.Approval;
import com.paypal.checkout.approve.ApprovalData;
import com.paypal.checkout.approve.OnApprove;
import com.paypal.checkout.cancel.OnCancel;
import com.paypal.checkout.createorder.CreateOrder;
import com.paypal.checkout.createorder.CreateOrderActions;
import com.paypal.checkout.error.CorrelationIds;
import com.paypal.checkout.error.ErrorInfo;
import com.paypal.checkout.error.OnError;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest({PayPalCheckout.class, Uri.class})
@RunWith(RobolectricTestRunner.class)
public class PayPalNativeClientUnitTest {

    private FragmentActivity context;

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;
    private Configuration payPalNoClientIdConfig;

    private PayPalNativeTokenizeCallback payPalNativeTokenizeCallback;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void beforeEach() throws JSONException {
        context = mock(FragmentActivity.class);
        when(context.getPackageName()).thenReturn("paypalNative_packagename");
        when(context.getApplication()).thenReturn(mock(Application.class));

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
        payPalNoClientIdConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_NO_CLIENT_ID);


        payPalNativeTokenizeCallback = mock(PayPalNativeTokenizeCallback.class);
        PowerMockito.mockStatic(PayPalCheckout.class);
        PowerMockito.mockStatic(Uri.class);
    }

    @Test
    public void tokenizePayPalAccount_whenPayPalNotEnabled_throwsError() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalNativeTokenizeCallback).onResult(isNull(), errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.", errorCaptor.getValue().getMessage());
    }

    @Test
    public void tokenizePayPalAccount_whenNoClientId_throwsError() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalNoClientIdConfig)
                .build();

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalNativeTokenizeCallback).onResult(isNull(), errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("Invalid PayPal Client ID", errorCaptor.getValue().getMessage());
    }

    @Test
    public void tokenizePayPalAccount_internalClientHasNoPayPalResponse_throwsError() {
        final BraintreeException exception = new BraintreeException("error");
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().error(exception).build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalNativeTokenizeCallback).onResult(isNull(), errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("error", errorCaptor.getValue().getMessage());
    }

    @Test
    public void tokenizePayPalAccount_nativeCheckout_sendAnalytics() {
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.0");
        request.setShouldOfferPayLater(true);
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();


        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, request, payPalNativeTokenizeCallback);


        verify(braintreeClient).sendAnalyticsEvent("paypal.native.single-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal.native.single-payment.paylater.offered");
    }

    @Test
    public void tokenizePayPalAccount_whenUserCancels_throwErrorAndSendAnalytic() {
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.0");
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(new PayPalResponse(request)).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();


        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OnCancel callback = invocation.getArgument(2, OnCancel.class);
                callback.onCancel();
                return null;
            }
        }).when(PayPalCheckout.class);
        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalNativeTokenizeCallback).onResult(isNull(), errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof UserCanceledException);
        assertEquals("User canceled checkout", errorCaptor.getValue().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("paypal.native.client_cancel");
    }

    @Test
    public void tokenizePayPalAccount_whenNativeError_throwErrorAndSendAnalytic() {
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.0");
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(new PayPalResponse(request)).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();


        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final String errorReason = "error reason";
                OnError callback = invocation.getArgument(3, OnError.class);
                callback.onError(new ErrorInfo(new Exception(), errorReason, new CorrelationIds(), "orderId", "version"));

                ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
                verify(payPalNativeTokenizeCallback).onResult(isNull(), errorCaptor.capture());
                assertTrue(errorCaptor.getValue() instanceof BraintreeException);
                assertEquals(errorReason, errorCaptor.getValue().getMessage());
                verify(braintreeClient).sendAnalyticsEvent("paypal.native.error");
                return null;
            }
        }).when(PayPalCheckout.class);
        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);
    }

    @Test
    public void tokenizePayPalAccount_nativeCheckout_setCorrectPairingId() {
        CreateOrderActions createOrderActions = mock(CreateOrderActions.class);
        final String pairingId = "pairing_id";
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.0");
        PayPalResponse response = new PayPalResponse(request);
        response.pairingId(pairingId);

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(response).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();


        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                CreateOrder callback = invocation.getArgument(0, CreateOrder.class);
                callback.create(createOrderActions);
                return null;
            }
        }).when(PayPalCheckout.class);
        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        verify(createOrderActions).set(response.getPairingId());
    }

    @Test
    public void tokenizePayPalAccount_nativeCheckout_approvedAndTokenizesResponseOnSuccess() throws JSONException {
        final String pairingId = "pairing_id";
        final ApprovalData approvalData = new ApprovalData("SANDBOX-PAYER-ID", "EC-SANDBOX-EC-TOKEN", "SANDBOX-PAYMENT-ID");
        final Approval approval = mock(Approval.class);
        when(approval.getData()).thenReturn(approvalData);

        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.0");
        request.setMerchantAccountId("sample-merchant-account-id");
        PayPalResponse response = new PayPalResponse(request)
                .approvalUrl("https://example.com/approval?token=" + approvalData.getOrderId())
                .successUrl("https://example.com/success")
                .clientMetadataId("sample-client-metadata-id");
        response.pairingId(pairingId);

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().tokenizeRESTSuccess(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE))
                .build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(response).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .returnUrlScheme("sample-scheme")
                .build();


        String stringDeepLink = setUpUris(braintreeClient, approvalData, response);

        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OnApprove callback = invocation.getArgument(1, OnApprove.class);
               // when(approval.getData()).thenReturn(approvalData);
                callback.onApprove(approval);
                return null;
            }
        }).when(PayPalCheckout.class);

        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        verify(tokenizationClient).tokenizeREST(any(PayPalAccount.class), any(TokenizeCallback.class));
        verify(payPalNativeTokenizeCallback).onResult(any(PayPalAccountNonce.class), isNull());

        ArgumentCaptor<PayPalAccount> captor = ArgumentCaptor.forClass(PayPalAccount.class);
        verify(tokenizationClient).tokenizeREST(captor.capture(), any(TokenizeCallback.class));

        PayPalAccount payPalAccount = captor.getValue();
        JSONObject tokenizePayload = payPalAccount.buildJSON();
        assertEquals("sample-merchant-account-id", tokenizePayload.get("merchant_account_id"));

        JSONObject payPalTokenizePayload = tokenizePayload.getJSONObject("paypalAccount");

        JSONObject optionsJson = new JSONObject();
        optionsJson.put("validate", false);
        JSONObject expectedPayPalTokenizePayload = new JSONObject()
                .put("correlationId", "sample-client-metadata-id")
                .put("client", new JSONObject())
                .put("response", new JSONObject()
                        .put("webURL", stringDeepLink))
                .put("options", optionsJson)
                .put("intent", "authorize")
                .put("response_type", "web");

        JSONAssert.assertEquals(expectedPayPalTokenizePayload, payPalTokenizePayload, true);
    }

    @Test
    public void tokenizePayPalAccount_nativeCheckout_approvedAndReturnPayPalAccountNonce() throws JSONException {
        final String pairingId = "pairing_id";
        final ApprovalData approvalData = new ApprovalData("SANDBOX-PAYER-ID", "EC-SANDBOX-EC-TOKEN", "SANDBOX-PAYMENT-ID");
        final Approval approval = mock(Approval.class);
        when(approval.getData()).thenReturn(approvalData);

        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.0");
        request.setMerchantAccountId("sample-merchant-account-id");
        PayPalResponse response = new PayPalResponse(request)
                .approvalUrl("https://example.com/approval?token=" + approvalData.getOrderId())
                .successUrl("https://example.com/success")
                .clientMetadataId("sample-client-metadata-id");
        response.pairingId(pairingId);

        final PayPalAccountNonce expectedPayPalAccountNonce = PayPalAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE));
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().tokenizeRESTSuccess(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE))
                .build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(response).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .returnUrlScheme("sample-scheme")
                .build();

        setUpUris(braintreeClient, approvalData, response);

        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OnApprove callback = invocation.getArgument(1, OnApprove.class);
                callback.onApprove(approval);
                return null;
            }
        }).when(PayPalCheckout.class);

        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        verify(tokenizationClient).tokenizeREST(any(PayPalAccount.class), any(TokenizeCallback.class));
        verify(payPalNativeTokenizeCallback).onResult(any(PayPalAccountNonce.class), isNull());

        verify(braintreeClient).sendAnalyticsEvent("paypal.credit.accepted");

        ArgumentCaptor<PayPalAccountNonce> captor = ArgumentCaptor.forClass(PayPalAccountNonce.class);
        verify(payPalNativeTokenizeCallback).onResult(captor.capture(), isNull());

        PayPalAccountNonce receivedPayPalAccountNonce = captor.getValue();
        assertEquals(receivedPayPalAccountNonce.getAuthenticateUrl(), expectedPayPalAccountNonce.getAuthenticateUrl());
        assertEquals(receivedPayPalAccountNonce.getEmail(), expectedPayPalAccountNonce.getEmail());
    }

    @Test
    public void tokenizePayPalAccount_nativeCheckout_approvedAndTokenizeReturnError() throws JSONException {
        final String pairingId = "pairing_id";
        final ApprovalData approvalData = new ApprovalData("SANDBOX-PAYER-ID", "EC-SANDBOX-EC-TOKEN", "SANDBOX-PAYMENT-ID");
        final Approval approval = mock(Approval.class);
        when(approval.getData()).thenReturn(approvalData);

        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.0");
        request.setMerchantAccountId("sample-merchant-account-id");
        PayPalResponse response = new PayPalResponse(request)
                .approvalUrl("https://example.com/approval?token=" + approvalData.getOrderId())
                .successUrl("https://example.com/success")
                .clientMetadataId("sample-client-metadata-id");
        response.pairingId(pairingId);

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().tokenizeRESTError(new Exception("generic error"))
                .build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(response).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .returnUrlScheme("sample-scheme")
                .build();


        setUpUris(braintreeClient, approvalData, response);


        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OnApprove callback = invocation.getArgument(1, OnApprove.class);
                callback.onApprove(approval);
                return null;
            }
        }).when(PayPalCheckout.class);

        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        verify(tokenizationClient).tokenizeREST(any(PayPalAccount.class), any(TokenizeCallback.class));
        verify(payPalNativeTokenizeCallback).onResult(isNull(), any(Exception.class));

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalNativeTokenizeCallback).onResult(isNull(), captor.capture());

        Exception error = captor.getValue();
        assertEquals("generic error", error.getMessage());
    }

    @Test
    public void tokenizePayPalAccount_nativeCheckout_approvedAndUserCanceledURl() throws JSONException {
        final String pairingId = "pairing_id";
        final ApprovalData approvalData = new ApprovalData("SANDBOX-PAYER-ID", "EC-SANDBOX-EC-TOKEN", "SANDBOX-PAYMENT-ID");
        final Approval approval = mock(Approval.class);
        when(approval.getData()).thenReturn(approvalData);

        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.0");
        request.setMerchantAccountId("sample-merchant-account-id");
        PayPalResponse response = new PayPalResponse(request)
                .approvalUrl("https://example.com/approval?token=" + approvalData.getOrderId())
                .successUrl("https://example.com/cancel")
                .clientMetadataId("sample-client-metadata-id");
        response.pairingId(pairingId);

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().tokenizeRESTError(new Exception("generic error"))
                .build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(response).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .returnUrlScheme("sample-scheme")
                .build();

        Uri deepLinkUri = mock(Uri.class);
        Uri successfulUri = mock(Uri.class);

        String stringDeepLink = String.format(
                "%s://onetouch/v1/success?paymentId=%s&token=%s&PayerID=%s",
                braintreeClient.getReturnUrlScheme(),
                approvalData.getPaymentId(),
                approvalData.getOrderId(),
                approvalData.getPayerId());

        when(deepLinkUri.getLastPathSegment()).thenReturn("success");
        when(successfulUri.getLastPathSegment()).thenReturn("cancel");

        when(Uri.parse(stringDeepLink)).thenAnswer((Answer<Uri>) invocation -> deepLinkUri);
        when(Uri.parse(response.getSuccessUrl())).thenAnswer((Answer<Uri>) invocation -> successfulUri);


        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OnApprove callback = invocation.getArgument(1, OnApprove.class);
                callback.onApprove(approval);
                return null;
            }
        }).when(PayPalCheckout.class);

        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(payPalNativeTokenizeCallback).onResult(isNull(), captor.capture());

        Exception error = captor.getValue();
        assertEquals("User canceled.", error.getMessage());
    }

    @Test
    public void tokenizePayPalAccount_nativeCheckout_approvedAndInconsistentDataURL() throws JSONException {
        final String pairingId = "pairing_id";
        final ApprovalData approvalData = new ApprovalData("SANDBOX-PAYER-ID", "EC-SANDBOX-EC-TOKEN", "SANDBOX-PAYMENT-ID");
        final Approval approval = mock(Approval.class);
        when(approval.getData()).thenReturn(approvalData);

        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.0");
        request.setMerchantAccountId("sample-merchant-account-id");
        PayPalResponse response = new PayPalResponse(request)
                .approvalUrl("https://example.com/approval?token=" + approvalData.getOrderId())
                .successUrl("https://example.com/cancel")
                .clientMetadataId("sample-client-metadata-id");
        response.pairingId(pairingId);

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().tokenizeRESTError(new Exception("generic error"))
                .build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(response).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .returnUrlScheme("sample-scheme")
                .build();

        Uri deepLinkUri = mock(Uri.class);
        Uri successfulUri = mock(Uri.class);
        Uri approvalUri = mock(Uri.class);

        String stringDeepLink = String.format(
                "%s://onetouch/v1/success?paymentId=%s&token=%s&PayerID=%s",
                braintreeClient.getReturnUrlScheme(),
                approvalData.getPaymentId(),
                approvalData.getOrderId(),
                approvalData.getPayerId());

        when(deepLinkUri.getLastPathSegment()).thenReturn("success");
        when(deepLinkUri.getQueryParameter("token")).thenReturn("some data");
        when(deepLinkUri.toString()).thenReturn(stringDeepLink);
        when(successfulUri.getLastPathSegment()).thenReturn("success");
        when(approvalUri.getQueryParameter("token")).thenReturn(approvalData.getOrderId());


        when(Uri.parse(stringDeepLink)).thenAnswer((Answer<Uri>) invocation -> deepLinkUri);
        when(Uri.parse(response.getSuccessUrl())).thenAnswer((Answer<Uri>) invocation -> successfulUri);
        when(Uri.parse(response.getApprovalUrl())).thenAnswer((Answer<Uri>) invocation -> approvalUri);


        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OnApprove callback = invocation.getArgument(1, OnApprove.class);
                callback.onApprove(approval);
                return null;
            }
        }).when(PayPalCheckout.class);

        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeCheckoutRequest("1.0"), payPalNativeTokenizeCallback);

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(payPalNativeTokenizeCallback).onResult(isNull(), captor.capture());

        Exception error = captor.getValue();
        assertEquals("The response contained inconsistent data.", error.getMessage());
    }

    private String setUpUris(BraintreeClient braintreeClient, ApprovalData approvalData, PayPalResponse response) {
        Uri deepLinkUri = mock(Uri.class);
        Uri successfulUri = mock(Uri.class);
        Uri approvalUri = mock(Uri.class);

        String stringDeepLink = String.format(
                "%s://onetouch/v1/success?paymentId=%s&token=%s&PayerID=%s",
                braintreeClient.getReturnUrlScheme(),
                approvalData.getPaymentId(),
                approvalData.getOrderId(),
                approvalData.getPayerId());

        when(deepLinkUri.getLastPathSegment()).thenReturn("success");
        when(deepLinkUri.getQueryParameter("token")).thenReturn(approvalData.getOrderId());
        when(deepLinkUri.toString()).thenReturn(stringDeepLink);
        when(successfulUri.getLastPathSegment()).thenReturn("success");
        when(approvalUri.getQueryParameter("token")).thenReturn(approvalData.getOrderId());


        when(Uri.parse(stringDeepLink)).thenAnswer((Answer<Uri>) invocation -> deepLinkUri);
        when(Uri.parse(response.getSuccessUrl())).thenAnswer((Answer<Uri>) invocation -> successfulUri);
        when(Uri.parse(response.getApprovalUrl())).thenAnswer((Answer<Uri>) invocation -> approvalUri);

        return stringDeepLink;
    }

    @Test
    public void tokenizePayPalAccount_vaultCheckout_sendAnalytics() {
        PayPalNativeVaultRequest request = new PayPalNativeVaultRequest();
        request.setShouldOfferCredit(true);
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();


        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, request, payPalNativeTokenizeCallback);


        verify(braintreeClient).sendAnalyticsEvent("paypal.native.billing-agreement.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal.native.billing-agreement.credit.offered");
    }

    @Test
    public void tokenizePayPalAccount_vaultRequest_setBillingAgreementID() {
        final CreateOrderActions createOrderActions = mock(CreateOrderActions.class);
        final String pairingId = "pairing_id";
        PayPalNativeVaultRequest request = new PayPalNativeVaultRequest();
        PayPalResponse response = new PayPalResponse(request);
        response.pairingId(pairingId);

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(response).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();


        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                CreateOrder callback = invocation.getArgument(0, CreateOrder.class);
                callback.create(createOrderActions);
                return null;
            }
        }).when(PayPalCheckout.class);
        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeVaultRequest(), payPalNativeTokenizeCallback);

        verify(createOrderActions).setBillingAgreementId(response.getPairingId());
    }


    private String setUpVaultUris(BraintreeClient braintreeClient, ApprovalData approvalData, PayPalResponse response) {
        Uri deepLinkUri = mock(Uri.class);
        Uri successfulUri = mock(Uri.class);
        Uri approvalUri = mock(Uri.class);

        String stringDeepLink = String.format(
                "%s://onetouch/v1/success?token=%s&ba_token=%s",
                braintreeClient.getReturnUrlScheme(),
                approvalData.getOrderId(),
                response.getPairingId());

        when(deepLinkUri.getLastPathSegment()).thenReturn("success");
        when(deepLinkUri.getQueryParameter("ba_token")).thenReturn(response.getPairingId());
        when(deepLinkUri.toString()).thenReturn(stringDeepLink);
        when(successfulUri.getLastPathSegment()).thenReturn("success");
        when(approvalUri.getQueryParameter("ba_token")).thenReturn(response.getPairingId());


        when(Uri.parse(stringDeepLink)).thenAnswer((Answer<Uri>) invocation -> deepLinkUri);
        when(Uri.parse(response.getSuccessUrl())).thenAnswer((Answer<Uri>) invocation -> successfulUri);
        when(Uri.parse(response.getApprovalUrl())).thenAnswer((Answer<Uri>) invocation -> approvalUri);

        return stringDeepLink;
    }

    @Test
    public void tokenizePayPalAccount_vaultCheckout_approvedAndTokenizesResponseOnSuccess() throws JSONException {
        final String pairingId = "pairing_id";
        final ApprovalData approvalData = new ApprovalData("SANDBOX-PAYER-ID", "EC-SANDBOX-EC-TOKEN", "SANDBOX-PAYMENT-ID");
        final Approval approval = mock(Approval.class);
        when(approval.getData()).thenReturn(approvalData);

        PayPalNativeVaultRequest request = new PayPalNativeVaultRequest();
        request.setMerchantAccountId("sample-merchant-account-id");
        PayPalResponse response = new PayPalResponse(request)
                .approvalUrl("https://example.com/approval?ba_token=" + pairingId)
                .successUrl("https://example.com/success")
                .clientMetadataId("sample-client-metadata-id");
        response.pairingId(pairingId);

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().tokenizeRESTSuccess(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE))
                .build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().success(response).build();


        final BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .returnUrlScheme("sample-scheme")
                .build();


        String stringDeepLink = setUpVaultUris(braintreeClient, approvalData, response);

        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OnApprove callback = invocation.getArgument(1, OnApprove.class);
                callback.onApprove(approval);
                return null;
            }
        }).when(PayPalCheckout.class);

        PayPalCheckout.start(any(CreateOrder.class), any(OnApprove.class), any(OnCancel.class), any(OnError.class));

        PayPalNativeClient sut = new PayPalNativeClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.tokenizePayPalAccount(context, new PayPalNativeVaultRequest(), payPalNativeTokenizeCallback);

        verify(tokenizationClient).tokenizeREST(any(PayPalAccount.class), any(TokenizeCallback.class));
        verify(payPalNativeTokenizeCallback).onResult(any(PayPalAccountNonce.class), isNull());

        ArgumentCaptor<PayPalAccount> captor = ArgumentCaptor.forClass(PayPalAccount.class);
        verify(tokenizationClient).tokenizeREST(captor.capture(), any(TokenizeCallback.class));

        PayPalAccount payPalAccount = captor.getValue();
        JSONObject tokenizePayload = payPalAccount.buildJSON();
        assertEquals("sample-merchant-account-id", tokenizePayload.get("merchant_account_id"));

        JSONObject payPalTokenizePayload = tokenizePayload.getJSONObject("paypalAccount");

        JSONObject optionsJson = new JSONObject();
        optionsJson.put("validate", false);
        JSONObject expectedPayPalTokenizePayload = new JSONObject()
                .put("correlationId", "sample-client-metadata-id")
                .put("client", new JSONObject())
                .put("response", new JSONObject()
                        .put("webURL", stringDeepLink))
                .put("response_type", "web");

        JSONAssert.assertEquals(expectedPayPalTokenizePayload, payPalTokenizePayload, true);
    }
}
