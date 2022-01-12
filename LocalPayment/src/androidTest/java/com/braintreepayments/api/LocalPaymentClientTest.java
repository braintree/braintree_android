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

@RunWith(AndroidJUnit4ClassRunner.class)
public class LocalPaymentClientTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> activityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private CountDownLatch countDownLatch;
    private BraintreeClient braintreeClient;

    @Before
    public void setUp() throws InvalidArgumentException {
        countDownLatch = new CountDownLatch(1);

        braintreeClient = new BraintreeClient(ApplicationProvider.getApplicationContext(), "sandbox_f252zhq7_hh4cpc39zq4rgjcg");
    }

    @Test(timeout = 10000)
    public void startPayment_callsListener_withApprovalUrl_andPaymentId() throws InterruptedException {
        PostalAddress address = new PostalAddress();
        address.setStreetAddress("836486 of 22321 Park Lake");
        address.setCountryCodeAlpha2("NL");
        address.setLocality("Den Haag");
        address.setPostalCode("2585 GJ");

        LocalPaymentRequest request = new LocalPaymentRequest();
        request.setPaymentType("ideal");
        request.setAmount("1.10");
        request.setAddress(address);
        request.setPhone("639847934");
        request.setEmail("jon@getbraintree.com");
        request.setGivenName("Jon");
        request.setSurname("Doe");
        request.setShippingAddressRequired(true);
        request.setCurrencyCode("EUR");

        LocalPaymentClient sut = new LocalPaymentClient(, braintreeClient);
        sut.startPayment(request, new LocalPaymentStartCallback() {
            @Override
            public void onResult(@Nullable LocalPaymentResult localPaymentResult, @Nullable Exception error) {
                assertNotNull(localPaymentResult.getApprovalUrl());
                assertNotNull(localPaymentResult.getPaymentId());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
}
