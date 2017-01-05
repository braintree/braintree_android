package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.VisaCheckoutListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VisaCheckoutPaymentMethodNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
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
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class VisaCheckoutTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private BraintreeFragment mBraintreeFragment;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws InvalidArgumentException {
        mCountDownLatch = new CountDownLatch(1);
        mBraintreeFragment = BraintreeFragmentTestUtils.getFragment(mActivityTestRule.getActivity(),
                new TestClientTokenBuilder().withVisaCheckout().build(), null);
    }

    @Test(timeout = 10000)
    public void createVisaCheckoutLibrary_whenSuccessful_returnsVisaMComLibrary() throws InterruptedException {
        mBraintreeFragment.addListener(new VisaCheckoutListener() {
            @Override
            public void onVisaCheckoutLibraryCreated(VisaMcomLibrary visaMcomLibrary) {
                assertNotNull(visaMcomLibrary);
                mCountDownLatch.countDown();
            }
        });

        VisaCheckout.createVisaCheckoutLibrary(mBraintreeFragment);
        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void authorize_whenActivityCancels_postsCancel() throws InterruptedException {
        mBraintreeFragment = spy(mBraintreeFragment);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                mBraintreeFragment.onActivityResult(BraintreeRequestCodes.VISA_CHECKOUT,
                        Activity.RESULT_CANCELED, new Intent());
                return null;
            }
        }).when(mBraintreeFragment).startActivityForResult(any(Intent.class), eq(BraintreeRequestCodes.VISA_CHECKOUT));

        mBraintreeFragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(BraintreeRequestCodes.VISA_CHECKOUT, requestCode);
                mCountDownLatch.countDown();
            }
        });

        VisaCheckout.authorize(mBraintreeFragment, new VisaPaymentInfo());
        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void authorize_whenActivityResultOkAndTokenizationFails_postsTokenizationException()
            throws InterruptedException {
        mBraintreeFragment = spy(mBraintreeFragment);
        BraintreeHttpClient httpClient = spy(mBraintreeFragment.getHttpClient());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback httpResponseCallback = (HttpResponseCallback) invocation.getArguments()[2];
                httpResponseCallback.failure(new BraintreeException("Tokenization Failure"));
                return null;
            }
        }).when(httpClient).post(contains("visa_checkout_cards"), anyString(), any(HttpResponseCallback.class));
        when(mBraintreeFragment.getHttpClient()).thenReturn(httpClient);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent data = new Intent();
                data.putExtra(VisaLibrary.PAYMENT_SUMMARY, sampleVisaPaymentSummary());
                mBraintreeFragment.onActivityResult(BraintreeRequestCodes.VISA_CHECKOUT,
                        Activity.RESULT_OK, data);
                return null;
            }
        }).when(mBraintreeFragment).startActivityForResult(any(Intent.class), eq(BraintreeRequestCodes.VISA_CHECKOUT));

        mBraintreeFragment.addListener(new VisaCheckoutListener() {
            @Override
            public void onVisaCheckoutLibraryCreated(VisaMcomLibrary visaMcomLibrary) {
                VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
                VisaCheckout.authorize(mBraintreeFragment, visaPaymentInfo);
            }
        });

        VisaCheckout.createVisaCheckoutLibrary(mBraintreeFragment);
        mCountDownLatch.await();

        ArgumentCaptor exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(mBraintreeFragment).postCallback((Exception) exceptionCaptor.capture());

        BraintreeException error = (BraintreeException) exceptionCaptor.getValue();
        assertTrue(error instanceof BraintreeException);
        assertEquals("Tokenization Failure", error.getMessage());
    }

    @Test(timeout = 10000)
    public void authorize_whenActivityResultOkAndTokenizationSuccess_postsVisaCheckoutPaymentMethodNonce()
            throws InterruptedException {
        mBraintreeFragment = spy(mBraintreeFragment);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Intent data = new Intent();
                data.putExtra(VisaLibrary.PAYMENT_SUMMARY, sampleVisaPaymentSummary());
                mBraintreeFragment.onActivityResult(BraintreeRequestCodes.VISA_CHECKOUT,
                        Activity.RESULT_OK, data);
                return null;
            }
        }).when(mBraintreeFragment).startActivityForResult(any(Intent.class), eq(BraintreeRequestCodes.VISA_CHECKOUT));

        mBraintreeFragment.addListener(new VisaCheckoutListener() {
            @Override
            public void onVisaCheckoutLibraryCreated(VisaMcomLibrary visaMcomLibrary) {
                VisaPaymentInfo visaPaymentInfo = new VisaPaymentInfo();
                VisaCheckout.authorize(mBraintreeFragment, visaPaymentInfo);
            }
        });

        mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                VisaCheckoutPaymentMethodNonce visaCheckoutPayment = (VisaCheckoutPaymentMethodNonce) paymentMethodNonce;
                assertIsANonce(visaCheckoutPayment.getNonce());
                assertEquals("44", visaCheckoutPayment.getLastTwo());
                assertEquals("MasterCard", visaCheckoutPayment.getCardType());
                assertEquals("US", visaCheckoutPayment.getShippingAddress().getCountryCode());
                assertEquals("BT", visaCheckoutPayment.getUserData().getUserFirstName());
                mCountDownLatch.countDown();
            }
        });

        VisaCheckout.createVisaCheckoutLibrary(mBraintreeFragment);
        mCountDownLatch.await();
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
