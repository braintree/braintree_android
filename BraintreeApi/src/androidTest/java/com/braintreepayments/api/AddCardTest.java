package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.test.AbstractBraintreeListener;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.BraintreeTestUtils.getBraintree;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class AddCardTest {

    private Braintree mBraintree;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws JSONException, InterruptedException {
        BraintreeTestUtils.setUp(getTargetContext());
        mBraintree = getBraintree(getTargetContext(), new TestClientTokenBuilder().build());
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void canAddCard() throws InterruptedException {
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertNotNull(paymentMethod.getNonce());
                mCountDownLatch.countDown();
            }
        });

        mBraintree.create(new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("01")
                .expirationYear("2017"));

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void canTokenizeInvalidCard() throws InterruptedException {
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertNotNull(paymentMethodNonce);
                mCountDownLatch.countDown();
            }
        });

        mBraintree.tokenize(new CardBuilder().cardNumber("This is a credit card"));

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void throwsErrorOnServerFailure() throws InterruptedException {
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                assertEquals(422, error.getStatusCode());
                assertNotNull(error.getFieldErrors());
                assertEquals("Credit card is invalid", error.getMessage());
                assertEquals(1, error.getFieldErrors().size());
                assertEquals(3, error.errorFor("creditCard").getFieldErrors().size());
                assertEquals(
                        "Credit card must include number, payment_method_nonce, or venmo_sdk_payment_method_code",
                        error.errorFor("creditCard").errorFor("base").getMessage());
                assertEquals("Expiration year is invalid",
                        error.errorFor("creditCard").errorFor("expirationYear").getMessage());
                assertEquals("Credit card number is required",
                        error.errorFor("creditCard").errorFor("number").getMessage());
                assertNull(error.errorFor("creditCard").errorFor("expirationMonth"));
                mCountDownLatch.countDown();
            }
        });

        mBraintree.create(new CardBuilder().expirationMonth("01"));

        mCountDownLatch.await();
    }
}