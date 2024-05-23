package com.braintreepayments.api.localpayment;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.PostalAddress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4ClassRunner.class)
public class LocalPaymentClientTest {

    private CountDownLatch countDownLatch;
    private BraintreeClient braintreeClient;

    @Before
    public void setUp() {
        countDownLatch = new CountDownLatch(1);
        braintreeClient = new BraintreeClient(ApplicationProvider.getApplicationContext(), "sandbox_f252zhq7_hh4cpc39zq4rgjcg");
    }

    @Test(timeout = 10000)
    public void createPaymentAuthRequest_callsBack_withApprovalUrl_andPaymentId() throws InterruptedException {
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

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient);
        sut.createPaymentAuthRequest(request, (localPaymentAuthRequest) -> {
            assertTrue(localPaymentAuthRequest instanceof LocalPaymentAuthRequest.ReadyToLaunch);
            assertNotNull(((LocalPaymentAuthRequest.ReadyToLaunch) localPaymentAuthRequest).getRequestParams().getApprovalUrl());
            assertNotNull(((LocalPaymentAuthRequest.ReadyToLaunch) localPaymentAuthRequest).getRequestParams().getPaymentId());
            countDownLatch.countDown();
        });

        countDownLatch.await();
    }
}
