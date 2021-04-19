package com.braintreepayments.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodClientUnitTest {

    @Captor
    ArgumentCaptor<List<PaymentMethodNonce>> paymentMethodNoncesCaptor;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getPaymentMethodNonces_whenDefaultFirstIsFalse_sendsPaymentMethodsGETRequest() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .build();

        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);
        sut.getPaymentMethodNonces(false, mock(GetPaymentMethodNoncesCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGET(captor.capture(), any(HttpResponseCallback.class));

        String url = captor.getValue();
        assertEquals("/v1/payment_methods?default_first=false&session_id=sample-session-id", url);
    }

    @Test
    public void getPaymentMethodNonces_whenDefaultFirstIsTrue_sendsPaymentMethodsGETRequest() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .build();

        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);
        sut.getPaymentMethodNonces(true, mock(GetPaymentMethodNoncesCallback.class));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendGET(captor.capture(), any(HttpResponseCallback.class));

        String url = captor.getValue();
        assertEquals("/v1/payment_methods?default_first=true&session_id=sample-session-id", url);
    }

    @Test
    public void getPaymentMethodNonces_parsesCards() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .sendGETSuccessfulResponse(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_RESPONSE)
                .build();

        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(false, callback);

        verify(callback).onResult(paymentMethodNoncesCaptor.capture(), (Exception) isNull());

        List<PaymentMethodNonce> paymentMethodNonces = paymentMethodNoncesCaptor.getValue();
        assertEquals(PaymentMethodType.CARD, paymentMethodNonces.get(0).getType());
        assertEquals("ending in ••11", paymentMethodNonces.get(0).getDescription());
        assertEquals("123456-12345-12345-a-adfa", paymentMethodNonces.get(0).getString());
        assertTrue(paymentMethodNonces.get(0).isDefault());
    }

    @Test
    public void getPaymentMethodNonces_parsesPayPal() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .sendGETSuccessfulResponse(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_RESPONSE)
                .build();

        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(false, callback);

        verify(callback).onResult(paymentMethodNoncesCaptor.capture(), (Exception) isNull());

        List<PaymentMethodNonce> paymentMethodNonces = paymentMethodNoncesCaptor.getValue();
        assertEquals(PaymentMethodType.PAYPAL, paymentMethodNonces.get(1).getType());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", paymentMethodNonces.get(1).getString());
        assertEquals("with email paypalaccount@example.com", paymentMethodNonces.get(1).getDescription());
        assertFalse(paymentMethodNonces.get(1).isDefault());
    }

    @Test
    public void parsePaymentMethods_parsesVenmoAccountNonce() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .sendGETSuccessfulResponse(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_RESPONSE)
                .build();

        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(false, callback);

        verify(callback).onResult(paymentMethodNoncesCaptor.capture(), (Exception) isNull());

        List<PaymentMethodNonce> paymentMethodNonces = paymentMethodNoncesCaptor.getValue();
        assertEquals(PaymentMethodType.VENMO, paymentMethodNonces.get(2).getType());
        assertEquals("fake-venmo-nonce", paymentMethodNonces.get(2).getString());
        assertEquals("VenmoAccount", paymentMethodNonces.get(2).getDescription());
    }

    @Test
    public void parsePaymentMethods_doesNotParseGooglePayCardNonces() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .sendGETSuccessfulResponse(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_GOOGLE_PAY_RESPONSE)
                .build();

        PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        GetPaymentMethodNoncesCallback callback = mock(GetPaymentMethodNoncesCallback.class);
        sut.getPaymentMethodNonces(false, callback);

        verify(callback).onResult(paymentMethodNoncesCaptor.capture(), (Exception) isNull());

        List<PaymentMethodNonce> paymentMethodNonces = paymentMethodNoncesCaptor.getValue();
        assertEquals(0, paymentMethodNonces.size());
    }
}
