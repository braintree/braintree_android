package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class LocalPaymentClientTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private CountDownLatch mCountDownLatch;
    private BraintreeClient braintreeClient;

    @Before
    public void setUp() {
        mCountDownLatch = new CountDownLatch(1);

        braintreeClient = new BraintreeClient("sandbox_f252zhq7_hh4cpc39zq4rgjcg", ApplicationProvider.getApplicationContext());
    }

    @Test(timeout = 10000)
    public void startPayment_callsListener_withApprovalUrl_andPaymentId() throws InterruptedException {
        PostalAddress address = new PostalAddress()
                .streetAddress("836486 of 22321 Park Lake")
                .countryCodeAlpha2("NL")
                .locality("Den Haag")
                .postalCode("2585 GJ");
        LocalPaymentRequest request = new LocalPaymentRequest()
                .paymentType("ideal")
                .amount("1.10")
                .address(address)
                .phone("639847934")
                .email("jon@getbraintree.com")
                .givenName("Jon")
                .surname("Doe")
                .shippingAddressRequired(true)
                .currencyCode("EUR");

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient);
        sut.startPayment(request, new LocalPaymentStartCallback() {
            @Override
            public void onResult(@Nullable LocalPaymentTransaction transaction, @Nullable Exception error) {
                assertNotNull(transaction.getApprovalUrl());
                assertNotNull(transaction.getPaymentId());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }
}
