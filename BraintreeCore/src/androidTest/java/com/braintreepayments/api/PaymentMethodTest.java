package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.testutils.Fixtures;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.ExpirationDateHelper.validExpirationYear;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class PaymentMethodTest {

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

        Authorization authorization = Authorization.fromString(clientToken);
        final BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity, null);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("12")
                .expirationYear(validExpirationYear());

        tokenizationClient.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                sut.getPaymentMethodNonces(mActivity, new GetPaymentMethodNoncesCallback() {
                    @Override
                    public void onResult(@Nullable List<PaymentMethodNonce> paymentMethodNonces, @Nullable Exception error) {
                        assertNull(error);
                        assertNotNull(paymentMethodNonces);
                        assertEquals(1, paymentMethodNonces.size());

                        CardNonce cardNonce = (CardNonce) paymentMethodNonces.get(0);

                        assertIsANonce(cardNonce.getNonce());
                        assertEquals("11", cardNonce.getLastTwo());

                        sut.deletePaymentMethod(mActivity, cardNonce, new DeletePaymentMethodNonceCallback() {
                            @Override
                            public void onResult(@Nullable PaymentMethodNonce deletedNonce, @Nullable Exception error) {
                                assertNull(error);
                                CardNonce cardNonce = (CardNonce) deletedNonce;
                                assertEquals("11", cardNonce.getLastTwo());
                                latch.countDown();
                            }
                        });
                    }
                });
            }

            @Override
            public void failure(Exception exception) {
                fail(exception.getMessage());
            }
        });

        latch.await();
    }

    @Test(timeout = 10000)
    public void getPaymentMethodNonces_failsWithATokenizationKey() throws InterruptedException,
            InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);

        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        final BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity, null);
        TokenizationClient tokenizationClient = new TokenizationClient(braintreeClient);
        final PaymentMethodClient sut = new PaymentMethodClient(braintreeClient);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear(validExpirationYear());

        tokenizationClient.tokenize(cardBuilder, new PaymentMethodNonceCallback() {
            @Override
            public void success(final PaymentMethodNonce paymentMethodNonce) {
                sut.getPaymentMethodNonces(mActivity, new GetPaymentMethodNoncesCallback() {
                    @Override
                    public void onResult(@Nullable List<PaymentMethodNonce> paymentMethodNonces, @Nullable Exception error) {
                        assertNull(paymentMethodNonces);

                        assertTrue(error instanceof AuthorizationException);
                        assertEquals("Tokenization key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                                error.getMessage());
                        latch.countDown();
                    }
                });
            }

            @Override
            public void failure(Exception exception) {
                fail(exception.getMessage());
            }
        });

        latch.await();
    }
}
