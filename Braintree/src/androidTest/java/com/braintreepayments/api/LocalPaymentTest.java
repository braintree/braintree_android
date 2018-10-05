package com.braintreepayments.api;

import android.app.Activity;
import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.BraintreeApiErrorResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreePaymentResultListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.BraintreePaymentResult;
import com.braintreepayments.api.models.IdealBank;
import com.braintreepayments.api.models.IdealRequest;
import com.braintreepayments.api.models.IdealResult;
import com.braintreepayments.api.models.LocalPaymentRequest;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.testutils.Assumptions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithAuthorization;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class LocalPaymentTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private BraintreeFragment mBraintreeFragment;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws InvalidArgumentException {
        Assumptions.assumeDeviceCanConnectToBraintreeApi();

        mCountDownLatch = new CountDownLatch(1);

        mBraintreeFragment = getFragmentWithAuthorization(mActivityTestRule.getActivity(),
                "sandbox_f252zhq7_hh4cpc39zq4rgjcg");
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
        LocalPayment.startPayment(mBraintreeFragment, request, new BraintreeResponseListener<LocalPaymentRequest>() {
            @Override
            public void onResponse(LocalPaymentRequest localPaymentRequest) {
                assertNotNull(localPaymentRequest.getApprovalUrl());
                assertNotNull(localPaymentRequest.getPaymentId());
                assertTrue(localPaymentRequest.getApprovalUrl().contains("integrationType=standalone"));
                assertTrue(localPaymentRequest.getApprovalUrl().contains("fundingSource=ideal"));
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }
}
