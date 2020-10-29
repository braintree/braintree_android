package com.braintreepayments.api;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.Fixtures;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithAuthorization;
import static com.braintreepayments.testutils.Assertions.assertIsANonce;
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
    public void tokenize_tokenizesAPayPalAccountWithATokenizationKey() throws InterruptedException, JSONException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = getFragmentWithAuthorization(mActivity, Fixtures.TOKENIZATION_KEY);

        JSONObject otcJson = new JSONObject(Fixtures.PAYPAL_OTC_RESPONSE);
        PayPalAccountBuilder paypalAccountBuilder =
                new PayPalAccountBuilder().oneTouchCoreData(otcJson);

        TokenizationClient.tokenize(fragment, paypalAccountBuilder,
                new PaymentMethodNonceCallback() {
                    @Override
                    public void success(PaymentMethodNonce paymentMethodNonce) {
                        assertIsANonce(paymentMethodNonce.getNonce());
                        assertEquals("PayPal", paymentMethodNonce.getTypeLabel());
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
