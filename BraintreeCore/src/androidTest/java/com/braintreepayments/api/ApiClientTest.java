package com.braintreepayments.api;

import static com.braintreepayments.api.Assertions.assertIsANonce;
import static junit.framework.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4ClassRunner.class)
public class ApiClientTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test(timeout = 10000)
    @Ignore("It isn't clear what this test does or how it works, but we removed a lot of the PayPal OTC logic when refactoring for v4.")
    public void tokenize_tokenizesAPayPalAccountWithATokenizationKey() throws InterruptedException, JSONException {
        final CountDownLatch latch = new CountDownLatch(1);

        BraintreeClient braintreeClient = new BraintreeClient(context, Fixtures.TOKENIZATION_KEY);

        ApiClient apiClient = new ApiClient(braintreeClient);

        JSONObject urlResponseData = new JSONObject(Fixtures.PAYPAL_OTC_RESPONSE);
        PayPalAccount paypalAccount = new PayPalAccount();
        paypalAccount.setUrlResponseData(urlResponseData);

        apiClient.tokenizeREST(paypalAccount, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (exception != null) {
                    fail(exception.getMessage());
                }

                try {
                    PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJSON(tokenizationResponse);
                    assertIsANonce(payPalAccountNonce.getString());
                    latch.countDown();
                } catch (JSONException e) {
                    fail("This should not fail");
                }
            }
        });

        latch.await();
    }
}
