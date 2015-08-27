package com.braintreepayments.api;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.TestClientKey.CLIENT_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class CardTokenizerTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private BraintreeFragment mBraintreeFragment;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws InvalidArgumentException {
        mBraintreeFragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(),
                new TestClientTokenBuilder().build());
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_tokenizesACard() throws InvalidArgumentException, InterruptedException {
        mBraintreeFragment.addListener(new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("11", ((Card) paymentMethod).getLastTwo());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        CardTokenizer.tokenize(mBraintreeFragment, cardBuilder);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_tokenizesACardWithAClientKey() throws InvalidArgumentException, InterruptedException {
        mBraintreeFragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(), CLIENT_KEY);
        mBraintreeFragment.addListener(new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("11", ((Card) paymentMethod).getLastTwo());
                mCountDownLatch.countDown();
            }
        });
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("08/20");

        CardTokenizer.tokenize(mBraintreeFragment, cardBuilder);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_callsListenerWithErrorOnFailure() throws InterruptedException {
        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {}

            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                assertEquals(422, error.getStatusCode());
                assertNotNull(error.getFieldErrors());
                assertEquals("Credit card is invalid", error.getMessage());
                assertEquals(1, error.getFieldErrors().size());
                assertEquals(3, error.errorFor("creditCard").getFieldErrors().size());
                assertEquals("Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code",
                        error.errorFor("creditCard").errorFor("base").getMessage());
                assertEquals("Expiration year is invalid",
                        error.errorFor("creditCard").errorFor("expirationYear").getMessage());
                assertEquals("Credit card number is required",
                        error.errorFor("creditCard").errorFor("number").getMessage());
                assertNull(error.errorFor("creditCard").errorFor("expirationMonth"));
                mCountDownLatch.countDown();
            }
        });

        CardTokenizer.tokenize(mBraintreeFragment, new CardBuilder().expirationMonth("01"));

        mCountDownLatch.await();
    }
}
