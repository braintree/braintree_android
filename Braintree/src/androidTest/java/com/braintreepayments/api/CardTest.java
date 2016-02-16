package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.BraintreeActivityTestRule;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class CardTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    @Test(timeout = 10000)
    public void tokenize_tokenizesACard() throws InvalidArgumentException, InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(new TestClientTokenBuilder().build(), cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateTrue() throws InvalidArgumentException, InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(true);

        assertTokenizationSuccessful(new TestClientTokenBuilder().build(), cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithValidateFalse() throws InvalidArgumentException, InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(false);

        assertTokenizationSuccessful(new TestClientTokenBuilder().build(), cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKey() throws InvalidArgumentException, InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        assertTokenizationSuccessful(TOKENIZATION_KEY, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_tokenizesACardWithATokenizationKeyAndValidateFalse() throws InvalidArgumentException, InterruptedException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(false);

        assertTokenizationSuccessful(TOKENIZATION_KEY, cardBuilder);
    }

    @Test(timeout = 10000)
    public void tokenize_failsWithTokenizationKeyAndValidateTrue() throws InterruptedException,
            InvalidArgumentException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(), TOKENIZATION_KEY);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof AuthorizationException);
                assertEquals("Tokenization key authorization not allowed for this endpoint. Please use an authentication method with upgraded permissions",
                        error.getMessage());
                countDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20")
                .validate(true);

        Card.tokenize(fragment, cardBuilder);

        countDownLatch.await();
    }

    private void assertTokenizationSuccessful(String authorization, CardBuilder cardBuilder)
            throws InterruptedException, InvalidArgumentException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        BraintreeFragment fragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(), authorization);
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
                countDownLatch.countDown();
            }
        });

        Card.tokenize(fragment, cardBuilder);

        countDownLatch.await();
    }
}
