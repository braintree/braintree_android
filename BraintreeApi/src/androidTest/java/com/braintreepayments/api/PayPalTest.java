package com.braintreepayments.api;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.intent.IntentCallback;
import android.support.test.runner.intent.IntentMonitorRegistry;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.BundleMatchers.hasEntry;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasHost;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasParamWithName;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasParamWithValue;
import static android.support.test.espresso.intent.matcher.UriMatchers.hasPath;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.verifyAnalyticsEvent;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;

@RunWith(AndroidJUnit4.class)
public class PayPalTest {

    @Rule
    public final IntentsTestRule<TestActivity> mActivityTestRule =
            new IntentsTestRule<>(TestActivity.class);

    private Activity mActivity;
    private CountDownLatch mLatch;
    private IntentCallback mIntentCallback;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mLatch = new CountDownLatch(1);
    }

    @After
    public void tearDown() {
        if (mIntentCallback != null) {
            IntentMonitorRegistry.getInstance().removeIntentCallback(mIntentCallback);
        }
    }

    @Test(timeout = 10000)
    @MediumTest
    public void authorizeAccount_startsBrowser() {
        Looper.prepare();
        final BraintreeFragment fragment = getFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));

        PayPal.authorizeAccount(fragment);

        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(hasHost("checkout.paypal.com")),
                hasData(hasPath("/one-touch-login/")),
                hasData(hasParamWithName("payload")),
                hasData(hasParamWithName("payloadEnc")),
                hasData(hasParamWithValue("x-success", "com.braintreepayments.api.test.braintree://onetouch/v1/success")),
                hasData(hasParamWithValue("x-cancel", "com.braintreepayments.api.test.braintree://onetouch/v1/cancel")),
                hasExtras(allOf(hasEntry(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH, true)))));
    }

    //@Test(timeout = 1000)
    @SmallTest
    public void authorizeAccount_sendsAnalyticsEvent()
            throws JSONException, InvalidArgumentException {
        Looper.prepare();
        BraintreeFragment fragment = getMockFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));

        PayPal.authorizeAccount(fragment);

        verifyAnalyticsEvent(fragment, "paypal.selected");
    }

    //@Test(timeout = 10000)
    @MediumTest
    public void authorizeAccount_isSuccessful() throws InterruptedException,
            InvalidArgumentException {
        Looper.prepare();
        final BraintreeFragment fragment = getFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
                mLatch.countDown();
            }
        });
        setBrowserSwitchResponse(
                "onetouch/v1/success?payloadEnc=3vsdKACxHUPPfEFpfI9DeJcPw4f%2Bj9Rp5fJjf%2B9h%2FN6GjoBaRQkIa9oUV2Vtm1I%2FiZqjZqd%2FXWQ56sts0iyl7eAVCfEvXHlpfrBg5e89JDINUUSAGAhTYmJWvoNm5YGxkSXmefLHhdvao8bIHZ26ExNL25oKS9E7RWgBtwOx%2BzChE3u0klAlgSN027ex7GSezjk5CsXMrns7%2BmcebLObQoZb3C1XjKik2m4HhSwXSdR5ygRkaRSVO5e1PVz0oiUBxpzGiubNb9aPrRtWvx%2FRwq3RSHNUIa4LuslgrxVx2WIa0isNKR3bBwzFcYClLbS6065Cs60Desg0BZSrudkwgSNJDwKnIzJM8FC1m4Xd2ASd63XnMzBh1RzbouAXqsrdIJFFVcVTrU4yO6mWTFqklw%3D%3D&payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6IjRhMDcwYjhmLTgyMDQtNDczMC05Y2M0LWZiNWQ3ZjE3YWY3OCIsInJlc3BvbnNlX3R5cGUiOiJjb2RlIiwiZW52aXJvbm1lbnQiOiJtb2NrIiwiZXJyb3IiOm51bGx9&x-source=com.braintree.browserswitch");

        PayPal.authorizeAccount(fragment);

        mLatch.await();
    }

    //@Test(timeout = 10000)
    @MediumTest
    public void authorizeAccount_doesNotCallCancelListenerWhenSuccessful() throws InterruptedException,
            InvalidArgumentException {
        Looper.prepare();
        final BraintreeFragment fragment = getFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
                mLatch.countDown();
            }
        });
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                fail("Cancel listener was called");
            }
        });
        setBrowserSwitchResponse(
                "onetouch/v1/success?payloadEnc=3vsdKACxHUPPfEFpfI9DeJcPw4f%2Bj9Rp5fJjf%2B9h%2FN6GjoBaRQkIa9oUV2Vtm1I%2FiZqjZqd%2FXWQ56sts0iyl7eAVCfEvXHlpfrBg5e89JDINUUSAGAhTYmJWvoNm5YGxkSXmefLHhdvao8bIHZ26ExNL25oKS9E7RWgBtwOx%2BzChE3u0klAlgSN027ex7GSezjk5CsXMrns7%2BmcebLObQoZb3C1XjKik2m4HhSwXSdR5ygRkaRSVO5e1PVz0oiUBxpzGiubNb9aPrRtWvx%2FRwq3RSHNUIa4LuslgrxVx2WIa0isNKR3bBwzFcYClLbS6065Cs60Desg0BZSrudkwgSNJDwKnIzJM8FC1m4Xd2ASd63XnMzBh1RzbouAXqsrdIJFFVcVTrU4yO6mWTFqklw%3D%3D&payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6IjRhMDcwYjhmLTgyMDQtNDczMC05Y2M0LWZiNWQ3ZjE3YWY3OCIsInJlc3BvbnNlX3R5cGUiOiJjb2RlIiwiZW52aXJvbm1lbnQiOiJtb2NrIiwiZXJyb3IiOm51bGx9&x-source=com.braintree.browserswitch");

        PayPal.authorizeAccount(fragment);

        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void authorizeAccount_callsCancelListenerWhenCanceled() throws InterruptedException {
        Looper.prepare();
        final BraintreeFragment fragment = getFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(PayPal.PAYPAL_REQUEST_CODE, requestCode);
                mLatch.countDown();
            }
        });
        setBrowserSwitchResponse("onetouch/v1/cancel");

        PayPal.authorizeAccount(fragment);

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void requestBillingAgreement_postsExceptionWhenAmountIsIncluded()
            throws InterruptedException {
        final BraintreeFragment fragment = getFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof BraintreeException);
                assertEquals("There must be no amount specified for the Billing Agreement flow",
                        error.getMessage());
                mLatch.countDown();
            }
        });

        PayPal.requestBillingAgreement(fragment, new PayPalRequest("1"));

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void requestBillingAgreement_startsBrowser() throws InvalidArgumentException {
        Looper.prepare();
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity, authString,
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/setup_billing_agreement")) {
                    callback.success(stringFromFixture("paypal_hermes_billing_agreement_response.json"));
                }
            }
        };

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(hasHost("checkout.paypal.com")),
                hasData(hasPath("/one-touch-login-sandbox/index.html")),
                hasData(hasParamWithValue("action", "create_payment_resource")),
                hasData(not(hasParamWithName("amount"))),
                hasData(hasParamWithValue("authorization_fingerprint", "63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06|created_at=2015-10-13T18:49:48.371382792+0000&merchant_id=dcpspy2brwdjr3qn&public_key=9wwrzqk3vr3t4nc8")),
                hasData(not(hasParamWithName("currency_iso_code"))),
                hasData(hasParamWithValue("experience_profile[address_override]", "false")),
                hasData(hasParamWithValue("experience_profile[no_shipping]", "false")),
                hasData(hasParamWithValue("merchant_id", "dcpspy2brwdjr3qn")),
                hasData(hasParamWithValue("return_url", "com.braintreepayments.api.test.braintree://onetouch/v1/success")),
                hasData(hasParamWithValue("cancel_url", "com.braintreepayments.api.test.braintree://onetouch/v1/cancel")),
                hasExtras(allOf(hasEntry(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH, true)))));
    }

    @Test(timeout = 10000)
    @MediumTest
    public void requestBillingAgreement_isSuccessful() throws InvalidArgumentException,
            InterruptedException {
        Looper.prepare();
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity, authString,
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/setup_billing_agreement")) {
                    callback.success(stringFromFixture("paypal_hermes_response.json"));
                } else {
                    callback.success(stringFromFixture("payment_methods/paypal_account.json"));
                }
            }
        };
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
                mLatch.countDown();
            }
        });

        setBrowserSwitchResponse(
                "onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN");

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void requestBillingAgreement_doesNotCallCancelListenerWhenSuccessful() throws InvalidArgumentException,
            InterruptedException {
        Looper.prepare();
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity, authString,
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/setup_billing_agreement")) {
                    callback.success(stringFromFixture("paypal_hermes_response.json"));
                } else {
                    callback.success(stringFromFixture("payment_methods/paypal_account.json"));
                }
            }
        };
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                fail("Cancel listener called with code: " + requestCode);
            }
        });
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
                mLatch.countDown();
            }
        });

        setBrowserSwitchResponse(
                "onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN");

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void requestBillingAgreement_cancelUrlTriggersCancelListener()
            throws JSONException, InterruptedException, InvalidArgumentException {
        Looper.prepare();
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity,
                authString, configString);
        fragment.mHttpClient = new BraintreeHttpClient(
                ClientToken.fromString(new TestClientTokenBuilder().build())) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                callback.success(stringFromFixture("paypal_hermes_response.json"));
            }
        };
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(PayPal.PAYPAL_REQUEST_CODE, requestCode);
                mLatch.countDown();
            }
        });
        setBrowserSwitchResponse("onetouch/v1/cancel");

        PayPal.requestBillingAgreement(fragment, new PayPalRequest());

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_startsBrowser() throws InvalidArgumentException {
        Looper.prepare();
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity, authString,
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/create_payment_resource")) {
                    callback.success(stringFromFixture("paypal_hermes_response.json"));
                }
            }
        };

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1.00"));

        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(hasHost("checkout.paypal.com")),
                hasData(hasPath("/one-touch-login-sandbox/index.html")),
                hasData(hasParamWithValue("action", "create_payment_resource")),
                hasData(hasParamWithValue("amount", "1.00")),
                hasData(hasParamWithValue("authorization_fingerprint",
                        "63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06|created_at=2015-10-13T18:49:48.371382792+0000&merchant_id=dcpspy2brwdjr3qn&public_key=9wwrzqk3vr3t4nc8")),
                hasData(hasParamWithValue("currency_iso_code", "USD")),
                hasData(hasParamWithValue("experience_profile[address_override]", "false")),
                hasData(hasParamWithValue("experience_profile[no_shipping]", "false")),
                hasData(hasParamWithValue("merchant_id", "dcpspy2brwdjr3qn")),
                hasData(hasParamWithValue("return_url",
                        "com.braintreepayments.api.test.braintree://onetouch/v1/success")),
                hasData(hasParamWithValue("cancel_url",
                        "com.braintreepayments.api.test.braintree://onetouch/v1/cancel")),
                hasExtras(allOf(hasEntry(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH,
                        true)))));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_postsExceptionWhenNoAmountIsSet() throws InterruptedException {
        final BraintreeFragment fragment = getFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof BraintreeException);
                assertEquals("An amount must be specified for the Single Payment flow.",
                        error.getMessage());
                mLatch.countDown();
            }
        });

        PayPal.requestOneTimePayment(fragment, new PayPalRequest());

        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void checkout_isSuccessful() throws InvalidArgumentException,
            InterruptedException {
        Looper.prepare();
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity, authString,
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/create_payment_resource")) {
                    callback.success(stringFromFixture("paypal_hermes_response.json"));
                } else {
                    callback.success(stringFromFixture("payment_methods/paypal_account.json"));
                }
            }
        };
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
                mLatch.countDown();
            }
        });

        setBrowserSwitchResponse(
                "onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN");

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void checkout_doesNotCallCancelListenerWhenSuccessful() throws InvalidArgumentException,
            InterruptedException {
        Looper.prepare();
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity, authString,
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/create_payment_resource")) {
                    callback.success(stringFromFixture("paypal_hermes_response.json"));
                } else {
                    callback.success(stringFromFixture("payment_methods/paypal_account.json"));
                }
            }
        };
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                fail("Cancel listener called with code: " + requestCode);
            }
        });
        fragment.addListener(new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
                mLatch.countDown();
            }
        });

        setBrowserSwitchResponse(
                "onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN");

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void checkout_cancelUrlTriggersCancelListener()
            throws JSONException, InterruptedException, InvalidArgumentException {
        Looper.prepare();
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity,
                authString, configString);
        fragment.mHttpClient = new BraintreeHttpClient(
                ClientToken.fromString(new TestClientTokenBuilder().build())) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                callback.success(stringFromFixture("paypal_hermes_response.json"));
            }
        };
        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                assertEquals(PayPal.PAYPAL_REQUEST_CODE, requestCode);
                mLatch.countDown();
            }
        });
        setBrowserSwitchResponse("onetouch/v1/cancel");

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_defaultPostParamsIncludeCorrectValues()
            throws InvalidArgumentException, InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity,
                authString, configString);
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/create_payment_resource")) {
                    try {
                        JSONObject json = new JSONObject(data);
                        assertEquals("1", json.get("amount"));
                        JSONObject experienceProfileJson = json.getJSONObject("experience_profile");
                        assertEquals(true, experienceProfileJson.get("no_shipping"));
                        assertEquals(false, experienceProfileJson.get("address_override"));

                        mLatch.countDown();
                    } catch (JSONException ignored) {}
                }
            }
        };
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail(error.getMessage());
            }
        });

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1"));

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_postParamsIncludeNoShipping()
            throws InvalidArgumentException, InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment =
                getFragment(mActivity, authString, configString);
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/create_payment_resource")) {
                    try {
                        JSONObject json = new JSONObject(data);
                        assertEquals("1", json.get("amount"));
                        JSONObject experienceProfileJson = json.getJSONObject("experience_profile");
                        assertEquals(true, experienceProfileJson.get("no_shipping"));
                        assertEquals(false, experienceProfileJson.get("address_override"));
                        mLatch.countDown();
                    } catch (JSONException ignored) {}
                }
            }
        };

        PayPal.requestOneTimePayment(fragment, new PayPalRequest("1").shippingAddressRequired(false));

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_postParamsIncludeAddressAndAddressOverride()
            throws InvalidArgumentException, InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment =
                getFragment(mActivity, authString, configString);
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/create_payment_resource")) {
                    try {
                        JSONObject json = new JSONObject(data);
                        assertEquals("3.43", json.get("amount"));
                        assertEquals("123 Fake St.", json.get("line1"));
                        assertEquals("Apt. v.0", json.get("line2"));
                        assertEquals("Oakland", json.get("city"));
                        assertEquals("CA", json.get("state"));
                        assertEquals("12345", json.get("postal_code"));
                        assertEquals("US", json.get("country_code"));
                        JSONObject experienceProfileJson = json.getJSONObject("experience_profile");
                        assertEquals(false, experienceProfileJson.get("no_shipping"));
                        assertEquals(true, experienceProfileJson.get("address_override"));

                        mLatch.countDown();
                    } catch (JSONException ignored) {}
                }
            }
        };

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");

        PayPalRequest request = new PayPalRequest("3.43")
                .shippingAddressRequired(true)
                .shippingAddressOverride(address);
        PayPal.requestOneTimePayment(fragment, request);

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_postParamsIncludeNoShippingAndAddressAndAddressOverride()
            throws InvalidArgumentException, InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = getFragment(mActivity,
                authString, configString);
        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/create_payment_resource")) {
                    try {
                        JSONObject json = new JSONObject(data);
                        assertEquals("3.43", json.get("amount"));
                        assertEquals("123 Fake St.", json.get("line1"));
                        assertEquals("Apt. v.0", json.get("line2"));
                        assertEquals("Oakland", json.get("city"));
                        assertEquals("CA", json.get("state"));
                        assertEquals("12345", json.get("postal_code"));
                        assertEquals("US", json.get("country_code"));
                        JSONObject experienceProfileJson = json.getJSONObject("experience_profile");
                        assertEquals(true, experienceProfileJson.get("no_shipping"));
                        assertEquals(true, experienceProfileJson.get("address_override"));
                        mLatch.countDown();
                    } catch (JSONException ignored) {}
                }
            }
        };

        PostalAddress address = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. v.0")
                .locality("Oakland")
                .region("CA")
                .postalCode("12345")
                .countryCodeAlpha2("US");
        PayPalRequest request = new PayPalRequest("3.43")
                .shippingAddressRequired(false)
                .shippingAddressOverride(address);
        PayPal.requestOneTimePayment(fragment, request);

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_postConfigurationExceptionWhenInvalid()
            throws JSONException, InterruptedException {
        final BraintreeFragment fragment = getMockFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_analytics.json"));
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ConfigurationException);
                assertEquals("PayPal is not enabled",
                        error.getMessage());
                mLatch.countDown();
            }
        });

        PayPal.authorizeAccount(fragment);

        mLatch.await();
    }

    /** helpers */
    private void setBrowserSwitchResponse(final String responseUrl) {
        intending(hasExtraWithKey(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH))
                .respondWith(new ActivityResult(Activity.RESULT_FIRST_USER, null));

        mIntentCallback = new IntentCallback() {
            @Override
            public void onIntentSent(Intent intent) {
                if (intent.hasExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH)) {
                    Intent responseIntent = new Intent()
                            .setData(Uri.parse(
                                    "com.braintreepayments.api.test.braintree://" + responseUrl));
                    mActivity.startActivity(responseIntent);
                }
            }
        };
        IntentMonitorRegistry.getInstance().addIntentCallback(mIntentCallback);
    }
}
