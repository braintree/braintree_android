package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.UnionPayListener;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.models.UnionPayCardBuilder;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.BraintreeActivityTestRule;
import com.braintreepayments.testutils.CardNumber;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.CardNumber.UNIONPAY_CREDIT;
import static com.braintreepayments.testutils.CardNumber.UNIONPAY_DEBIT;
import static com.braintreepayments.testutils.CardNumber.UNIONPAY_ENROLLMENT_NOT_REQUIRED;
import static com.braintreepayments.testutils.CardNumber.UNIONPAY_SINGLE_STEP_SALE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class UnionPayTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private BraintreeFragment mBraintreeFragment;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws InvalidArgumentException {
        mCountDownLatch = new CountDownLatch(1);
        mBraintreeFragment = BraintreeFragmentTestUtils.getFragment(mActivityTestRule.getActivity(),
                new TestClientTokenBuilder().withUnionPay().build());
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_whenDebit_isDebitIsTrue() throws InterruptedException {
        mBraintreeFragment.addListener(new UnionPayListener() {
            @Override
            public void onCapabilitiesFetched(UnionPayCapabilities unionPayCapabilities) {
                assertTrue(unionPayCapabilities.isUnionPayEnrollmentRequired());
                assertTrue(unionPayCapabilities.supportsTwoStepAuthAndCapture());
                assertTrue(unionPayCapabilities.isDebit());
                assertTrue(unionPayCapabilities.isUnionPay());
                mCountDownLatch.countDown();
            }

            @Override
            public void onSmsCodeSent(String enrollmentId) {
                fail("Not expecting onSmsCodeSent");
            }
        });

        UnionPay.fetchCapabilities(mBraintreeFragment, UNIONPAY_DEBIT);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_whenCredit_isDebitIsFalse() throws InterruptedException {
        mBraintreeFragment.addListener(new UnionPayListener() {
            @Override
            public void onCapabilitiesFetched(UnionPayCapabilities unionPayCapabilities) {
                assertTrue(unionPayCapabilities.isUnionPayEnrollmentRequired());
                assertTrue(unionPayCapabilities.supportsTwoStepAuthAndCapture());
                assertFalse(unionPayCapabilities.isDebit());
                assertTrue(unionPayCapabilities.isUnionPay());
                mCountDownLatch.countDown();
            }

            @Override
            public void onSmsCodeSent(String enrollmentId) {
                fail("Not expecting onSmsCodeSent");
            }
        });

        UnionPay.fetchCapabilities(mBraintreeFragment, UNIONPAY_CREDIT);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_whenEnrollmentNotRequired_enrollmentRequiredIsFalse() throws InterruptedException {
        mBraintreeFragment.addListener(new UnionPayListener() {
            @Override
            public void onCapabilitiesFetched(UnionPayCapabilities unionPayCapabilities) {
                assertFalse(unionPayCapabilities.isUnionPayEnrollmentRequired());
                assertTrue(unionPayCapabilities.supportsTwoStepAuthAndCapture());
                assertTrue(unionPayCapabilities.isDebit());
                assertTrue(unionPayCapabilities.isUnionPay());
                mCountDownLatch.countDown();
            }

            @Override
            public void onSmsCodeSent(String enrollmentId) {
                fail("Not expecting onSmsCodeSent");
            }
        });

        UnionPay.fetchCapabilities(mBraintreeFragment, UNIONPAY_ENROLLMENT_NOT_REQUIRED);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_whenSingleStepSale_twoStepAuthAndCaptureIsFalse() throws InterruptedException {
        mBraintreeFragment.addListener(new UnionPayListener() {
            @Override
            public void onCapabilitiesFetched(UnionPayCapabilities unionPayCapabilities) {
                assertTrue(unionPayCapabilities.isUnionPayEnrollmentRequired());
                assertFalse(unionPayCapabilities.supportsTwoStepAuthAndCapture());
                assertTrue(unionPayCapabilities.isDebit());
                assertTrue(unionPayCapabilities.isUnionPay());
                mCountDownLatch.countDown();
            }

            @Override
            public void onSmsCodeSent(String enrollmentId) {
                fail("Not expecting onSmsCodeSent");
            }
        });

        UnionPay.fetchCapabilities(mBraintreeFragment, UNIONPAY_SINGLE_STEP_SALE);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void enroll_whenCredit_returnsEnrollmentId() throws InterruptedException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber(CardNumber.UNIONPAY_CREDIT)
                .expirationMonth("12")
                .expirationYear("2019")
                .mobileCountryCode("62")
                .mobilePhoneNumber("11111111111");

        mBraintreeFragment.addListener(new UnionPayListener() {
            @Override
            public void onCapabilitiesFetched(UnionPayCapabilities unionPayCapabilities) {
                fail("Not expecting onCapabilitiesFetched");
            }

            @Override
            public void onSmsCodeSent(String enrollmentId) {
                assertFalse(TextUtils.isEmpty(enrollmentId));
                mCountDownLatch.countDown();
            }
        });

        UnionPay.enroll(mBraintreeFragment, unionPayCardBuilder);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void enroll_whenEnrollmentNotRequired_returnsError() throws InterruptedException {
        UnionPayCardBuilder unionPayCardBuilder = new UnionPayCardBuilder()
                .cardNumber(UNIONPAY_ENROLLMENT_NOT_REQUIRED)
                .expirationMonth("12")
                .expirationYear("2019")
                .mobileCountryCode("62")
                .mobilePhoneNumber("11111111111");

        mBraintreeFragment.addListener(new UnionPayListener() {
            @Override
            public void onCapabilitiesFetched(UnionPayCapabilities capabilities) {
                fail("Not expecting onCapabilitiesFetched");
            }

            @Override
            public void onSmsCodeSent(String enrollmentId) {
                fail("Not expecting onSmsCodeSent");
            }
        });
        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ErrorWithResponse);
                assertEquals("UnionPay Enrollment is invalid", error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        UnionPay.enroll(mBraintreeFragment, unionPayCardBuilder);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_whenEnrollmentNotRequired_withExpirationMonthAndYear() throws InvalidArgumentException,
            InterruptedException {
        UnionPayCardBuilder cardBuilder = new UnionPayCardBuilder()
                .cardNumber(UNIONPAY_ENROLLMENT_NOT_REQUIRED)
                .expirationMonth("08")
                .expirationYear("20")
                .cvv("123");

        mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertEquals("85", ((CardNonce) paymentMethodNonce).getLastTwo());
                mCountDownLatch.countDown();
            }
        });

        UnionPay.tokenize(mBraintreeFragment, cardBuilder);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_whenEnrollmentNotRequired_withExpirationDate() throws InvalidArgumentException,
            InterruptedException {
        UnionPayCardBuilder cardBuilder = new UnionPayCardBuilder()
                .cardNumber(UNIONPAY_ENROLLMENT_NOT_REQUIRED)
                .expirationDate("08/20")
                .cvv("123");

        mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertEquals("85", ((CardNonce) paymentMethodNonce).getLastTwo());
                mCountDownLatch.countDown();
            }
        });

        UnionPay.tokenize(mBraintreeFragment, cardBuilder);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_unionPayCredit_withExpirationDate() throws InvalidArgumentException, InterruptedException {
        UnionPayCardBuilder cardBuilder = new UnionPayCardBuilder()
                .cardNumber(CardNumber.UNIONPAY_CREDIT)
                .expirationDate("08/20")
                .enrollmentId("3nr011m3nt")
                .smsCode("12345")
                .cvv("123");

        mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertEquals("32", ((CardNonce) paymentMethodNonce).getLastTwo());
                mCountDownLatch.countDown();
            }
        });

        UnionPay.tokenize(mBraintreeFragment, cardBuilder);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void tokenize_unionPayCredit_withExpirationMonthAndYear() throws InvalidArgumentException, InterruptedException {
        UnionPayCardBuilder cardBuilder = new UnionPayCardBuilder()
                .cardNumber(CardNumber.UNIONPAY_CREDIT)
                .expirationMonth("08")
                .expirationYear("20")
                .enrollmentId("3nr011m3nt")
                .smsCode("12345")
                .cvv("123");

        mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertEquals("32", ((CardNonce) paymentMethodNonce).getLastTwo());
                mCountDownLatch.countDown();
            }
        });

        UnionPay.tokenize(mBraintreeFragment, cardBuilder);

        mCountDownLatch.await();
    }
}
