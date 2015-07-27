package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.test.AbstractBraintreeListener;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.BraintreeTestUtils.getBraintree;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class AddPaypalAccountTest {

    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() {
        BraintreeTestUtils.setUp(getTargetContext());
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void create_acceptsPayPalAccount() throws InterruptedException {
        Braintree braintree = getBraintree(getTargetContext(),
                new TestClientTokenBuilder().withPayPal().build());
        braintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertNotNull(paymentMethod.getNonce());
                assertEquals("PayPal", paymentMethod.getTypeLabel());
                mCountDownLatch.countDown();
            }
        });

        braintree.create(new PayPalAccountBuilder().consentCode("test-authorization-code"));

        mCountDownLatch.await();
    }
}
