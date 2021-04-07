package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.Assertions.assertIsANonce;
import static com.braintreepayments.api.CardNumber.VISA;
import static com.braintreepayments.api.ExpirationDateHelper.validExpirationYear;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PaymentMethodClientTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
    }

    // TODO: investigate
    @Ignore("This test is passing when run individually, but not when run with other tests.")
    @Test(timeout = 10000)
    public void getPaymentMethodNonces_andDeletePaymentMethod_returnsCardNonce() throws InterruptedException, InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String clientToken = new TestClientTokenBuilder().withCustomerId().build();

        final BraintreeClient braintreeClient = new BraintreeClient(mActivity, clientToken);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationMonth("12");
        card.setExpirationYear(validExpirationYear());

        tokenizationClient.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(String tokenizationResponse, Exception exception) {
                if (exception != null) {
                    fail(exception.getMessage());
                }

                sut.getPaymentMethodNonces(new GetPaymentMethodNoncesCallback() {
                    @Override
                    public void onResult(@Nullable List<BraintreeNonce> paymentMethodNonces, @Nullable Exception error) {
                        assertNull(error);
                        assertNotNull(paymentMethodNonces);
                        assertEquals(1, paymentMethodNonces.size());

                        PaymentMethodNonce paymentMethodNonce = paymentMethodNonces.get(0);

                        assertIsANonce(paymentMethodNonce.getString());
                        assertEquals("Visa", paymentMethodNonce.getTypeLabel());

                        sut.deletePaymentMethod(mActivity, paymentMethodNonce, new DeletePaymentMethodNonceCallback() {
                            @Override
                            public void onResult(@Nullable PaymentMethodNonce deletedNonce, @Nullable Exception error) {
                                assertNull(error);
                                assertEquals("Visa", deletedNonce.getTypeLabel());
                                latch.countDown();
                            }
                        });
                    }
                });
            }
        });

        latch.await();
    }

    @Test(timeout = 10000)
    public void getPaymentMethodNonces_failsWithATokenizationKey() throws InterruptedException,
            InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);

        final BraintreeClient braintreeClient = new BraintreeClient(mActivity, Fixtures.TOKENIZATION_KEY);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationMonth("04");
        card.setExpirationYear(validExpirationYear());

        tokenizationClient.tokenize(card, new TokenizeCallback() {
            @Override
            public void onResult(String tokenizationResponse, Exception exception) {
                if (exception != null) {
                    fail(exception.getMessage());
                }

                sut.getPaymentMethodNonces(new GetPaymentMethodNoncesCallback() {
                    @Override
                    public void onResult(@Nullable List<BraintreeNonce> paymentMethodNonces, @Nullable Exception error) {
                        assertNull(paymentMethodNonces);

                        assertTrue(error instanceof AuthorizationException);
                        assertEquals("Tokenization key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                                error.getMessage());
                        latch.countDown();
                    }
                });
            }
        });

        latch.await();
    }
}
