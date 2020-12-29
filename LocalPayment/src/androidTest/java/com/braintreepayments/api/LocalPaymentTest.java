package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.LocalPaymentRequest;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class LocalPaymentTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private LocalPayment localPayment;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws InvalidArgumentException {
        mCountDownLatch = new CountDownLatch(1);

        Authorization authorization = Authorization.fromString("sandbox_f252zhq7_hh4cpc39zq4rgjcg");
        BraintreeClient braintreeClient = new BraintreeClient(authorization, null);
        localPayment = new LocalPayment(null, braintreeClient);
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

        localPayment.startPayment(mActivityTestRule.getActivity(), request, new LocalPaymentStartCallback() {
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
