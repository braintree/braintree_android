package com.braintreepayments.api;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.Assertions.assertIsANonce;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class TokenizationClientTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesAPayPalAccountWithATokenizationKey() throws InterruptedException, JSONException, InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);

        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity);

        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);

        JSONObject urlResponseData = new JSONObject(Fixtures.PAYPAL_OTC_RESPONSE);
        PayPalAccountBuilder paypalAccountBuilder =
                new PayPalAccountBuilder().urlResponseData(urlResponseData);

        tokenizationClient.tokenize(paypalAccountBuilder, new TokenizeCallback() {
            @Override
            public void onResult(TokenizationResult tokenizationResult, Exception error) {
                assertIsANonce(tokenizationResult.getNonce());
                assertEquals("PayPal", tokenizationResult.getTypeLabel());
                latch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                fail(exception.getMessage());
            }
        });

        latch.await();
    }
}
