package com.braintreepayments.api;

import android.os.Parcel;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VisaCheckoutNonce;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.test.Assertions.assertIsANonce;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(Parameterized.class)
public class VisaCheckoutTest {

    private static final String CLIENT_TOKEN = new TestClientTokenBuilder()
            .withVisaCheckout()
            .build();

    @Parameters(name="{1}")
    public static Collection authorizationStrings() {
        return Arrays.asList(new String[][] {
                { CLIENT_TOKEN, "Client Token" },
                { TOKENIZATION_KEY, "Tokenization Key" }
        });
    }

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    public final String mAuthorization;

    private BraintreeFragment mBraintreeFragment;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws InvalidArgumentException {
        mCountDownLatch = new CountDownLatch(1);
        mBraintreeFragment = BraintreeFragment.newInstance(mActivityTestRule.getActivity(), mAuthorization);
        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                throw new RuntimeException("onError was not expected: ", error);
            }
        });
    }

    public VisaCheckoutTest(String authorization, String authorizationType) {
        mAuthorization = authorization;
    }

    @Test(timeout = 10000)
    public void tokenize_whenFailed_postsTokenizationException() throws InterruptedException {
        mBraintreeFragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ErrorWithResponse);
                assertEquals("Visa Checkout payment data decryption failed", error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        VisaCheckout.tokenize(mBraintreeFragment, malformedVisaPaymentSummary());
        mCountDownLatch.await();
    }

    @Test(timeout = 100000)
    public void tokenize_whenSuccess_postsVisaCheckoutNonce()
            throws InterruptedException {
        mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
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

        VisaCheckout.tokenize(mBraintreeFragment, sampleVisaPaymentSummary());
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
        in.writeString("-1");
        in.writeString("-1");
        in.writeString("-1");
        in.setDataPosition(0);

        return VisaPaymentSummary.CREATOR.createFromParcel(in);
    }

    private VisaPaymentSummary sampleVisaPaymentSummary() {
        JSONObject visaPaymentJson;
        try {
            visaPaymentJson = new JSONObject(stringFromFixture("response/visa_checkout_payment.json"));
        } catch (JSONException e) {
            throw new RuntimeException("Cannot convert visa_checkout_payment to JSON.", e);
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
