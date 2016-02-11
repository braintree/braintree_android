package com.braintreepayments.api;

import android.app.Activity;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.BraintreeActivityTestRule;
import com.braintreepayments.testutils.FixturesHelper;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.tokenize;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class TokenizationClientTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test(timeout = 10000)
    public void getPaymentMethodNonces_getsPaymentMethodsFromServer() throws InterruptedException,
            InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String clientToken = new TestClientTokenBuilder().build();
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, clientToken);
        getInstrumentation().waitForIdleSync();
        fragment.addListener(new PaymentMethodNoncesUpdatedListener() {
            @Override
            public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {
                assertEquals(1, paymentMethodNonces.size());
                assertEquals("11", ((CardNonce) paymentMethodNonces.get(0)).getLastTwo());
                latch.countDown();
            }
        });
        tokenize(fragment, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));

        TokenizationClient.getPaymentMethodNonces(fragment);

        latch.await();
    }

    @Test(timeout = 10000)
    public void getPaymentMethodNonces_failsWithATokenizationKey() throws InterruptedException,
            InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivity, TOKENIZATION_KEY);
        getInstrumentation().waitForIdleSync();
        fragment.addListener(new PaymentMethodNoncesUpdatedListener() {
            @Override
            public void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces) {
                fail("getPaymentMethodNonces succeeded");
            }
        });
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof AuthorizationException);
                assertEquals("Tokenization key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                        error.getMessage());
                latch.countDown();
            }
        });
        tokenize(fragment, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));

        TokenizationClient.getPaymentMethodNonces(fragment);

        latch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesAPayPalAccountWithATokenizationKey() throws InterruptedException, JSONException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = getFragment(mActivity, TOKENIZATION_KEY);

        JSONObject otcJson = new JSONObject(FixturesHelper.stringFromFixture("paypal_otc_response.json"));
        PayPalAccountBuilder paypalAccountBuilder =
                new PayPalAccountBuilder().oneTouchCoreData(otcJson);

        TokenizationClient.tokenize(fragment, paypalAccountBuilder,
                new PaymentMethodNonceCallback() {
                    @Override
                    public void success(PaymentMethodNonce paymentMethodNonce) {
                        assertNotNull(paymentMethodNonce.getNonce());
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
