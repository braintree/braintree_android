package com.braintreepayments.api;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.CardBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class TokenizationClientUnitTest {

    @Test
    public void tokenize_includesSessionIdInRequest() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder().build();
        when(fragment.getSessionId()).thenReturn("session-id");

        TokenizationClient.tokenize(fragment, new CardBuilder(), null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(fragment.getHttpClient()).post(anyString(), captor.capture(), any(HttpResponseCallback.class));
        JSONObject data = new JSONObject(captor.getValue()).getJSONObject("_meta");
        assertEquals("session-id", data.getString("sessionId"));
    }
}
