package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.test.AbstractBraintreeListener;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.BraintreeTestUtils.create;
import static com.braintreepayments.api.BraintreeTestUtils.getBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.httpClientWithExpectedError;
import static com.braintreepayments.testutils.CardNumber.AMEX;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class GetPaymentMethodsTest {

    @Before
    public void setUp() {
        BraintreeTestUtils.setUp(getTargetContext());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_returnsAnEmptyListIfEmpty() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Braintree braintree = getBraintree(getTargetContext(),
                new TestClientTokenBuilder().build());
        braintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                assertEquals(0, paymentMethods.size());
                latch.countDown();
            }
        });

        braintree.getPaymentMethods();

        latch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_returnsAListOfPaymentMethods()
            throws JSONException, ErrorWithResponse, BraintreeException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Braintree braintree = getBraintree(getTargetContext(),
                new TestClientTokenBuilder().withPayPal().build());
        braintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                assertEquals(3, paymentMethods.size());
                assertEquals("PayPal", paymentMethods.get(0).getTypeLabel());
                assertEquals("05", ((Card) paymentMethods.get(1)).getLastTwo());
                assertEquals("11", ((Card) paymentMethods.get(2)).getLastTwo());

                latch.countDown();
            }
        });

        create(braintree, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("01")
                .expirationYear("2017"));

        create(braintree, new CardBuilder()
                .cardNumber(AMEX)
                .expirationMonth("01")
                .expirationYear("2017"));

        create(braintree, new PayPalAccountBuilder().consentCode("fake_auth_code"));

        braintree.getPaymentMethods();

        latch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getPaymentMethods_throwsAnError() throws ErrorWithResponse,
            BraintreeException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Braintree braintree = getBraintree(getTargetContext(), new TestClientTokenBuilder().build());
        braintree.mHttpClient = httpClientWithExpectedError(new UnexpectedException("mock!"));

        braintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertTrue(throwable instanceof UnexpectedException);
                assertEquals("mock!", throwable.getMessage());
                latch.countDown();
            }
        });

        braintree.getPaymentMethods();

        latch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getPaymentMethods_throwsErrorWithResponse() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Braintree braintree = getBraintree(getTargetContext(), new TestClientTokenBuilder().build());
        braintree.mHttpClient = httpClientWithExpectedError(new ErrorWithResponse(422,
                stringFromFixture(getTargetContext(), "error_response.json")));
        braintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                assertEquals("There was an error", error.getMessage());
                latch.countDown();
            }
        });

        braintree.getPaymentMethods();

        latch.await();
    }
}
