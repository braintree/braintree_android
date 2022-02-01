package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ThreeDSecureAPIUnitTest {

    private ThreeDSecureAPI sut;

    @Test
    public void performLookup_sendsGraphQLPost() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureRequest threeDSecureRequest = mock(ThreeDSecureRequest.class);
        String mockData = "{\"mock\":\"json\"}";
        when(threeDSecureRequest.build("cardinal-session-id")).thenReturn(mockData);

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.performLookup(threeDSecureRequest, "cardinal-session-id", callback);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(urlCaptor.capture(), dataCaptor.capture(), any(HttpResponseCallback.class));

        String url = urlCaptor.getValue();
        assertEquals("/v1/payment_methods/null/three_d_secure/lookup", url);

        String data = dataCaptor.getValue();
        assertSame(mockData, data);
    }
}