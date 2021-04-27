package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.Assertions.assertIsANonce;
import static com.braintreepayments.api.CardNumber.UNIONPAY_CREDIT;
import static com.braintreepayments.api.CardNumber.UNIONPAY_DEBIT;
import static com.braintreepayments.api.CardNumber.UNIONPAY_INTEGRATION_CREDIT;
import static com.braintreepayments.api.CardNumber.UNIONPAY_INTEGRATION_DEBIT;
import static com.braintreepayments.api.CardNumber.UNIONPAY_NOT_ACCEPTED;
import static com.braintreepayments.api.CardNumber.UNIONPAY_SINGLE_STEP_SALE;
import static com.braintreepayments.api.CardNumber.UNIONPAY_SMS_NOT_REQUIRED;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class UnionPayClientTest {

    private UnionPayClient unionPayClient;
    private CountDownLatch countDownLatch;

    @Before
    public void setUp() throws InvalidArgumentException {
        countDownLatch = new CountDownLatch(1);
        BraintreeClient braintreeClient =
                new BraintreeClient(ApplicationProvider.getApplicationContext(), new TestClientTokenBuilder().build());
        unionPayClient = new UnionPayClient(braintreeClient, null);
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_whenDebit_isDebitIsTrue() throws InterruptedException {
        unionPayClient.fetchCapabilities(UNIONPAY_DEBIT, new UnionPayFetchCapabilitiesCallback() {
            @Override
            public void onResult(UnionPayCapabilities capabilities, Exception error) {
                assertTrue(capabilities.supportsTwoStepAuthAndCapture());
                assertTrue(capabilities.isDebit());
                assertTrue(capabilities.isUnionPay());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_whenCredit_isDebitIsFalse() throws InterruptedException {
        unionPayClient.fetchCapabilities(UNIONPAY_CREDIT, new UnionPayFetchCapabilitiesCallback() {
            @Override
            public void onResult(UnionPayCapabilities capabilities, Exception error) {
                assertTrue(capabilities.supportsTwoStepAuthAndCapture());
                assertFalse(capabilities.isDebit());
                assertTrue(capabilities.isUnionPay());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_unionPayCredit_isSupported() throws InterruptedException {
        assertSupported(UNIONPAY_CREDIT, true);
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_unionPayDebit_isSupported() throws InterruptedException {
        assertSupported(UNIONPAY_DEBIT, true);
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_unionPaySingleStepSale_isSupported() throws InterruptedException {
        assertSupported(UNIONPAY_SINGLE_STEP_SALE, true);
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_unionPayIntegrationCredit_isSupported() throws InterruptedException {
        assertSupported(UNIONPAY_INTEGRATION_CREDIT, true);
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_unionPayIntegrationDebit_isSupported() throws InterruptedException {
        assertSupported(UNIONPAY_INTEGRATION_DEBIT, true);
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_unionPaySmsNotRequired_isNotSupported() throws InterruptedException {
        assertSupported(UNIONPAY_NOT_ACCEPTED, false);
    }

    @Test(timeout = 10000)
    public void fetchCapabilities_whenSingleStepSale_twoStepAuthAndCaptureIsFalse() throws InterruptedException {
        unionPayClient.fetchCapabilities(UNIONPAY_SINGLE_STEP_SALE, new UnionPayFetchCapabilitiesCallback() {
            @Override
            public void onResult(UnionPayCapabilities capabilities, Exception error) {
                assertTrue(capabilities.isSupported());
                assertFalse(capabilities.supportsTwoStepAuthAndCapture());
                assertTrue(capabilities.isDebit());
                assertTrue(capabilities.isUnionPay());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void enroll_whenIsUnionPay_returnsEnrollmentId() throws InterruptedException {
        String cardNumber = UNIONPAY_CREDIT;
        final UnionPayCard unionPayCard = new UnionPayCard();
        unionPayCard.setNumber(cardNumber);
        unionPayCard.setExpirationMonth("12");
        unionPayCard.setExpirationYear(ExpirationDateHelper.validExpirationYear());
        unionPayCard.setMobileCountryCode("62");
        unionPayCard.setMobilePhoneNumber("11111111111");

        unionPayClient.fetchCapabilities(cardNumber, new UnionPayFetchCapabilitiesCallback() {
            @Override
            public void onResult(UnionPayCapabilities capabilities, Exception error) {
                assertTrue(capabilities.isUnionPay());
                unionPayClient.enroll(unionPayCard, new UnionPayEnrollCallback() {
                    @Override
                    public void onResult(@Nullable UnionPayEnrollment enrollment, @Nullable Exception error) {
                        assertFalse(TextUtils.isEmpty(enrollment.getId()));
                        assertTrue(enrollment.isSmsCodeRequired());
                        countDownLatch.countDown();
                    }
                });
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void enroll_whenIsUnionPayFalse_willError() throws InterruptedException {
        String cardNumber = CardNumber.VISA;
        final UnionPayCard unionPayCard = new UnionPayCard();
        unionPayCard.setNumber(cardNumber);
        unionPayCard.setExpirationMonth("12");
        unionPayCard.setExpirationYear(ExpirationDateHelper.validExpirationYear());
        unionPayCard.setMobileCountryCode("62");
        unionPayCard.setMobilePhoneNumber("11111111111");

        unionPayClient.fetchCapabilities(cardNumber, new UnionPayFetchCapabilitiesCallback() {
            @Override
            public void onResult(UnionPayCapabilities capabilities, Exception error) {
                assertFalse(capabilities.isUnionPay());
                unionPayClient.enroll(unionPayCard, new UnionPayEnrollCallback() {
                    @Override
                    public void onResult(@Nullable UnionPayEnrollment enrollment, @Nullable Exception error) {
                        assertTrue(error instanceof ErrorWithResponse);
                        assertEquals("UnionPay Enrollment is invalid", error.getMessage());
                        countDownLatch.countDown();
                    }
                });
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void enroll_whenSmsCodeRequiredFalse_onSmsCodeSentReturnsFalse() throws InterruptedException {
        String cardNumber = UNIONPAY_SMS_NOT_REQUIRED;
        final UnionPayCard unionPayCard = new UnionPayCard();
        unionPayCard.setNumber(cardNumber);
        unionPayCard.setExpirationMonth("12");
        unionPayCard.setExpirationYear(ExpirationDateHelper.validExpirationYear());
        unionPayCard.setMobileCountryCode("62");
        unionPayCard.setMobilePhoneNumber("11111111111");

        unionPayClient.fetchCapabilities(cardNumber, new UnionPayFetchCapabilitiesCallback() {
            @Override
            public void onResult(UnionPayCapabilities capabilities, Exception error) {
                assertTrue(capabilities.isUnionPay());
                assertTrue(capabilities.isSupported());
                unionPayClient.enroll(unionPayCard, new UnionPayEnrollCallback() {
                    @Override
                    public void onResult(@Nullable UnionPayEnrollment enrollment, @Nullable Exception error) {
                        assertNull(error);
                        assertNotNull(enrollment.getId());
                        assertFalse(enrollment.isSmsCodeRequired());
                        countDownLatch.countDown();
                    }
                });
            }
        });

        countDownLatch.await();
    }

    @Ignore("Sample merchant account is not set up for Union Pay")
    @Test(timeout = 10000)
    public void tokenize_unionPayCredit_withExpirationDate() throws InterruptedException {
        final UnionPayCard unionPayCard = new UnionPayCard();
        unionPayCard.setNumber(CardNumber.UNIONPAY_CREDIT);
        unionPayCard.setExpirationDate("08/20");
        unionPayCard.setCvv("123");
        unionPayCard.setMobileCountryCode("62");
        unionPayCard.setMobilePhoneNumber("1111111111");

        unionPayClient.enroll(unionPayCard, new UnionPayEnrollCallback() {
            @Override
            public void onResult(@Nullable UnionPayEnrollment enrollment, @Nullable Exception error) {
                assertTrue(enrollment.isSmsCodeRequired());
                unionPayCard.setEnrollmentId(enrollment.getId());
                unionPayCard.setSmsCode("12345");

                unionPayClient.tokenize(unionPayCard, new UnionPayTokenizeCallback() {
                    @Override
                    public void onResult(CardNonce cardNonce, Exception error) {
                        assertIsANonce(cardNonce.getString());
                        assertEquals("32", cardNonce.getLastTwo());
                        countDownLatch.countDown();
                    }
                });
            }
        });

        countDownLatch.await();
    }

    @Ignore("Sample merchant account is not set up for Union Pay")
    @Test(timeout = 30000)
    public void tokenize_unionPayCredit_withExpirationMonthAndYear() throws InterruptedException {
        final UnionPayCard unionPayCard = new UnionPayCard();
        unionPayCard.setNumber(CardNumber.UNIONPAY_CREDIT);
        unionPayCard.setExpirationMonth("08");
        unionPayCard.setExpirationYear("20");
        unionPayCard.setCvv("123");
        unionPayCard.setMobileCountryCode("62");
        unionPayCard.setMobilePhoneNumber("1111111111");

        unionPayClient.enroll(unionPayCard, new UnionPayEnrollCallback() {
            @Override
            public void onResult(@Nullable UnionPayEnrollment enrollment, @Nullable Exception error) {
                assertTrue(enrollment.isSmsCodeRequired());
                unionPayCard.setEnrollmentId(enrollment.getId());
                unionPayCard.setSmsCode("12345");

                unionPayClient.tokenize(unionPayCard, new UnionPayTokenizeCallback() {
                    @Override
                    public void onResult(CardNonce cardNonce, Exception error) {
                        assertIsANonce(cardNonce.getString());
                        assertEquals("32", cardNonce.getLastTwo());
                        countDownLatch.countDown();
                    }
                });
            }
        });

        countDownLatch.await();
    }

    private void assertSupported(final String cardNumber, final boolean expected) throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        unionPayClient.fetchCapabilities(cardNumber, new UnionPayFetchCapabilitiesCallback() {

            @Override
            public void onResult(UnionPayCapabilities capabilities, Exception error) {
                boolean actual = capabilities.isSupported();
                String message = String.format("Expected %s to be supported? %b, but was %b", cardNumber, expected, actual);
                assertEquals(message, expected, actual);
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }
}
