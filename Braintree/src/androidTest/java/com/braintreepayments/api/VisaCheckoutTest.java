package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.VisaCheckoutListener;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VisaCheckoutNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder;
import com.visa.checkout.VisaLibrary;
import com.visa.checkout.VisaMcomLibrary;
import com.visa.checkout.VisaPaymentInfo;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragmentWithAuthorization;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragmentWithConfiguration;
import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;

@RunWith(AndroidJUnit4.class)
public class VisaCheckoutTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private CountDownLatch mCountDownLatch;
    private String mVisaCheckoutConfiguration;

    @Before
    public void setUp() throws InvalidArgumentException {
        mVisaCheckoutConfiguration = new TestConfigurationBuilder()
                .visaCheckout(new TestVisaCheckoutConfigurationBuilder()
                        .apikey("apikey")
                        .externalClientId("externalClientId"))
                .build();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void createVisaCheckoutLibrary_whenSuccessful_returnsVisaMComLibrary() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = getMockFragmentWithConfiguration(mActivityTestRule.getActivity(),
                mVisaCheckoutConfiguration);
        fragment.addListener(new VisaCheckoutListener() {
            @Override
            public void onVisaCheckoutLibraryCreated(VisaMcomLibrary visaMcomLibrary) {
                assertNotNull(visaMcomLibrary);
                latch.countDown();
            }
        });

        VisaCheckout.createVisaCheckoutLibrary(fragment);
        latch.await();
    }

    @Test(timeout = 10000)
    public void authorize_whenActivityCancels_postsCancel() throws InterruptedException {
        final BraintreeFragment fragment = getMockFragmentWithConfiguration(mActivityTestRule.getActivity(),
                mVisaCheckoutConfiguration);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                fragment.onActivityResult(BraintreeRequestCodes.VISA_CHECKOUT,
                        Activity.RESULT_CANCELED, new Intent());
                return null;
            }
        }).when(fragment).startActivityForResult(any(Intent.class), eq(BraintreeRequestCodes.VISA_CHECKOUT));

        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(BraintreeRequestCodes.VISA_CHECKOUT, requestCode);
                mCountDownLatch.countDown();
            }
        });

        VisaCheckout.authorize(fragment, new VisaPaymentInfo());
        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void onActivityResult_whenActivityResultOkAndTokenizationFails_postsTokenizationException()
            throws InterruptedException {
        final BraintreeFragment fragment = getMockFragmentWithAuthorization(mActivityTestRule.getActivity(),
                new TestClientTokenBuilder().withVisaCheckout().build());

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ErrorWithResponse);
                assertEquals("Visa Checkout payment data decryption failed", error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        VisaCheckout.onActivityResult(fragment, Activity.RESULT_OK, new Intent()
                .putExtra(VisaLibrary.PAYMENT_SUMMARY, malformedVisaPaymentSummary()));
        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void onActivityResult_whenActivityResultOkAndTokenizationSuccess_postsVisaCheckoutNonce()
            throws InterruptedException {
        final BraintreeFragment fragment = getMockFragmentWithAuthorization(mActivityTestRule.getActivity(),
                new TestClientTokenBuilder().withVisaCheckout().build());

        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                VisaCheckoutNonce visaCheckoutPayment = (VisaCheckoutNonce) paymentMethodNonce;
                assertIsANonce(visaCheckoutPayment.getNonce());
                assertEquals("44", visaCheckoutPayment.getLastTwo());
                assertEquals("MasterCard", visaCheckoutPayment.getCardType());
                assertEquals("US", visaCheckoutPayment.getShippingAddress().getCountryCode());
                assertEquals("BT", visaCheckoutPayment.getUserData().getUserFirstName());
                mCountDownLatch.countDown();
            }
        });

        VisaCheckout.onActivityResult(fragment, Activity.RESULT_OK, new Intent().putExtra(VisaLibrary.PAYMENT_SUMMARY,
                sampleVisaPaymentSummary()));
        mCountDownLatch.await();
    }

    private VisaPaymentSummary malformedVisaPaymentSummary() {
        Parcel in = Parcel.obtain();
        in.writeLong(1);
        in.writeString("US");
        in.writeString("90210");
        in.writeString("1234");
        in.writeString("VISA");
        in.writeString("Credit");
        in.writeString("encPaymentData");
        in.writeString("encKey");
        in.writeString("callid");
        in.setDataPosition(0);

        return VisaPaymentSummary.CREATOR.createFromParcel(in);
    }

    private VisaPaymentSummary sampleVisaPaymentSummary() {
        JSONObject visaPaymentJson;
        try {
            visaPaymentJson = new JSONObject(stringFromFixture("response/visa_checkout_payment.json"));
        } catch (JSONException e) {
            throw new RuntimeException("Cannot convert visa_checkout_payment to JSON.");
        }

        Parcel in = Parcel.obtain();
        in.writeLong(1);
        in.writeString("US");
        in.writeString("90210");
        in.writeString("1234");
        in.writeString("VISA");
        in.writeString("Credit");
        in.writeString(Json.optString(visaPaymentJson, "encPaymentData", ""));
        in.writeString(Json.optString(visaPaymentJson, "encKey", ""));
        in.writeString(Json.optString(visaPaymentJson, "callid", ""));
        in.setDataPosition(0);

        return VisaPaymentSummary.CREATOR.createFromParcel(in);
    }
}
