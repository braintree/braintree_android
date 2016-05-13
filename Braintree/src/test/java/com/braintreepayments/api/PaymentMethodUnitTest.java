package com.braintreepayments.api;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.AndroidPayCardNonce;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricGradleTestRunner;

import java.util.List;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class PaymentMethodUnitTest {

    @Test
    public void getPaymentMethodNonces_returnsAnEmptyListIfEmpty() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mock(Configuration.class))
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
                .configuration(mock(Configuration.class))
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
                .configuration(mock(Configuration.class))
                .successResponse("{}")
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        verify(fragment).sendAnalyticsEvent("get-payment-methods.failed");
    }

    @Test
    public void getPaymentMethodNonces_sendsAnAnalyticsEventForErrors() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mock(Configuration.class))
                .errorResponse(new UnexpectedException("Error"))
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        verify(fragment).sendAnalyticsEvent("get-payment-methods.failed");
    }

    @Test
    public void getPaymentMethodNonces_fetchesPaymentMethods() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mock(Configuration.class))
                .successResponse(stringFromFixture("payment_methods/get_payment_methods_response.json"))
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        ArgumentCaptor<List<PaymentMethodNonce>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(fragment).postCallback(captor.capture());
        List<PaymentMethodNonce> paymentMethodNonces = captor.getValue();
        assertEquals(3, paymentMethodNonces.size());
        assertEquals("11", ((CardNonce) paymentMethodNonces.get(0)).getLastTwo());
        assertEquals("PayPal", paymentMethodNonces.get(1).getTypeLabel());
        assertEquals("11", ((AndroidPayCardNonce) paymentMethodNonces.get(2)).getLastTwo());
    }

    @Test
    public void getPaymentMethodNonces_sendsAnAnalyticsEventForSuccess() {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mock(Configuration.class))
                .successResponse(stringFromFixture("payment_methods/get_payment_methods_response.json"))
                .build();

        PaymentMethod.getPaymentMethodNonces(fragment);

        verify(fragment).sendAnalyticsEvent("get-payment-methods.succeeded");
    }

    @Test
    public void getPaymentMethodNonces_includesDefaultFirstParamInRequestPath()
            throws InvalidArgumentException, InterruptedException {
        BraintreeFragment fragment = new MockFragmentBuilder().configuration(mock(Configuration.class))
                .build();
        when(fragment.getSessionId()).thenReturn("session-id");

        PaymentMethod.getPaymentMethodNonces(fragment, true);

        verify(fragment.getHttpClient()).get(contains("default_first=true"), any(HttpResponseCallback.class));
    }
}
