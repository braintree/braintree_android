package com.braintreepayments.api;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.FlakyTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PayPalCheckout;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;

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

import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.verifyAnalyticsEvent;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class PayPalTest {

    @Rule
    public final IntentsTestRule<TestActivity> mActivityTestRule =
            new IntentsTestRule<>(TestActivity.class);

    private Activity mActivity;
    private CountDownLatch mLatch;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mLatch = new CountDownLatch(1);
    }

    @Test(timeout = 1000)
    @SmallTest
    @FlakyTest
    public void authorizeAccount_startsPayPal() throws JSONException, InterruptedException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));
        final ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
        final BraintreeFragment fragment = getMockFragment(mActivity, configuration);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doAnswer(new Answer() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        verify(fragment).startActivity(launchIntentCaptor.capture());
                        assertEquals(PayPal.PAYPAL_REQUEST_CODE,
                                launchIntentCaptor.getValue().getIntExtra(
                                        BraintreeBrowserSwitchActivity.EXTRA_REQUEST_CODE,
                                        Integer.MAX_VALUE));
                        mLatch.countDown();
                        return null;
                    }
                }).when(fragment).startActivity(any(Intent.class));

                // TODO: sometimes getActivity() returns null, and I don't know why
                if (fragment.getActivity() != null) {
                    PayPal.authorizeAccount(fragment);
                }
            }
        });
        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_withNoAmountPostsException() throws InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = BraintreeFragmentTestUtils.getFragment(mActivity,
                authString, configString);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof BraintreeException);
                assertEquals("An amount MUST be specified for the Single Payment flow.",
                        error.getMessage());
                mLatch.countDown();
            }
        });

        PayPal.checkout(fragment, new PayPalCheckout());
        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void requestBillingAgreement_withAmountPostsException() throws InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = BraintreeFragmentTestUtils.getFragment(mActivity,
                authString, configString);
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof BraintreeException);
                assertEquals("There must be no amount specified for the Billing Agreement flow",
                        error.getMessage());
                mLatch.countDown();
            }
        });

        PayPal.requestBillingAgreement(fragment, new PayPalCheckout("1"));
        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void checkout_successfulResultDoesNotCallCancelListener()
            throws InvalidArgumentException, InterruptedException {
        Looper.prepare();
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment =
                BraintreeFragmentTestUtils.getFragment(mActivity, authString, configString);
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

        fragment.addListener(new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertTrue(paymentMethod instanceof PayPalAccount);
                mLatch.countDown();
            }
        });

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail(error.getMessage());
            }
        });

        Intent returnIntent = new Intent(Intent.ACTION_VIEW);
        returnIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        returnIntent.setComponent(new ComponentName("com.braintreepayments.demo",
                "com.braintreepayments.api.BraintreeBrowserSwitchActivity"));
        returnIntent.setData(Uri.parse(
                "com.braintreepayments.demo.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"));
        returnIntent.setFlags(272629760);

        ActivityResult result = new ActivityResult(Activity.RESULT_CANCELED, returnIntent);
        intending(allOf(hasAction(Intent.ACTION_VIEW))).respondWith(result);

        PayPal.checkout(fragment, new PayPalCheckout("1"));

        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void checkout_cancelUrlTriggersCancelListener()
            throws JSONException, InterruptedException, InvalidArgumentException {
        Looper.prepare();
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = BraintreeFragmentTestUtils.getFragment(mActivity,
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

        Intent returnIntent = new Intent();
        returnIntent.setData(
                Uri.parse("http://paypal.com/do/the/thing/canceled?token=canceled-token"));
        ActivityResult result = new ActivityResult(Activity.RESULT_OK, returnIntent);
        intending(allOf(hasAction(Intent.ACTION_VIEW))).respondWith(result);

        PayPal.checkout(fragment, new PayPalCheckout("1"));
        mLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void checkout_resultCanceledTriggersCancelListener()
            throws JSONException, InterruptedException, InvalidArgumentException {
        Looper.prepare();
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = BraintreeFragmentTestUtils.getFragment(mActivity,
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

        ActivityResult result = new ActivityResult(Activity.RESULT_CANCELED, new Intent());
        intending(allOf(hasAction(Intent.ACTION_VIEW))).respondWith(result);

        PayPal.checkout(fragment, new PayPalCheckout("1"));
        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void authorizeAccount_sendsAnalyticsEvent()
            throws JSONException, InvalidArgumentException {
        Looper.prepare();
        BraintreeFragment fragment = getMockFragment(mActivity,
                Configuration
                        .fromJson(stringFromFixture("configuration_with_offline_paypal.json")));

        // TODO: sometimes getActivity() returns null, and I don't know why
        if (fragment.getActivity() != null) {
            PayPal.authorizeAccount(fragment);
        }
        verifyAnalyticsEvent(fragment, "paypal.selected");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_postConfigurationExceptionWhenInvalid()
            throws JSONException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Configuration configuration =
                Configuration.fromJson(stringFromFixture("configuration_with_analytics.json"));
        final BraintreeFragment fragment = getMockFragment(mActivity, configuration);

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ConfigurationException);
                assertEquals("PayPal is disabled or configuration is invalid",
                        error.getMessage());
                latch.countDown();
            }
        });

        PayPal.authorizeAccount(fragment);
        latch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_defaultPostParamsIncludeCorrectValues()
            throws InvalidArgumentException, InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = BraintreeFragmentTestUtils.getFragment(mActivity,
                authString, configString);

        fragment.mHttpClient = new BraintreeHttpClient(Authorization.fromString(authString)) {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                if (path.contains("/paypal_hermes/create_payment_resource")) {
                    try {
                        JSONObject json = new JSONObject(data);
                        assertEquals("1", json.get("amount"));
                        JSONObject experienceProfileJson = json.getJSONObject("experience_profile");
                        assertEquals(false, experienceProfileJson.get("no_shipping"));
                        assertEquals(false, experienceProfileJson.get("address_override"));
                        mLatch.countDown();
                    } catch (JSONException e) {
                    }
                }
            }
        };

        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                fail(error.getMessage());
            }
        });

        PayPal.checkout(fragment, new PayPalCheckout("1"));

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_postParamsIncludeNoShipping()
            throws InvalidArgumentException, InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment =
                BraintreeFragmentTestUtils.getFragment(mActivity, authString, configString);

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
                    } catch (JSONException e) {
                    }
                }
            }
        };

        PayPal.checkout(fragment, new PayPalCheckout("1").shippingAddressRequired(true));

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_postParamsIncludeAddressAndAddressOverride()
            throws InvalidArgumentException, InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment =
                BraintreeFragmentTestUtils.getFragment(mActivity, authString, configString);

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
                    } catch (JSONException e) {
                    }
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

        PayPalCheckout checkout = new PayPalCheckout("3.43").shippingAddressOverride(address);
        PayPal.checkout(fragment, checkout);

        mLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void checkout_postParamsIncludeNoShippingAndAddressAndAddressOverride()
            throws InvalidArgumentException, InterruptedException {
        String configString = stringFromFixture("configuration_with_offline_paypal.json");
        String authString = stringFromFixture("client_token.json");
        final BraintreeFragment fragment = BraintreeFragmentTestUtils.getFragment(mActivity,
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
                    } catch (JSONException e) {
                    }
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
        PayPalCheckout checkout =
                new PayPalCheckout("3.43").shippingAddressRequired(true).shippingAddressOverride(
                        address);
        PayPal.checkout(fragment, checkout);

        mLatch.await();
    }
}
