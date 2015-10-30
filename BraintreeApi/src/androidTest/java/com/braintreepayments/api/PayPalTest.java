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
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.PayPalAccount;
import com.braintreepayments.api.models.PayPalRequest;
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

import java.util.concurrent.CountDownLatch;

import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.verifyAnalyticsEvent;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class PayPalTest implements IntentCallback {

    @Rule
    public final IntentsTestRule<TestActivity> mActivityTestRule =
            new IntentsTestRule<>(TestActivity.class);

    private Activity mActivity;
    private CountDownLatch mLatch;
    private String mResponseUrl;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mLatch = new CountDownLatch(1);
        mResponseUrl = null;
    }

    @Test(timeout = 10000)
    @MediumTest
    public void authorizeAccount_startsBrowser() {
        Looper.prepare();
        final BraintreeFragment fragment = getMockFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));

        PayPal.authorizeAccount(fragment);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivity(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        Uri data = intent.getData();
        assertEquals("checkout.paypal.com", data.getHost());
        assertEquals("/one-touch-login/", data.getPath());
        assertNotNull(data.getQueryParameter("payload"));
        assertNotNull(data.getQueryParameter("payloadEnc"));
        assertNotNull(data.getQueryParameter("payloadEnc"));
        assertTrue(data.getQueryParameter("x-success").contains("success"));
        assertTrue(data.getQueryParameter("x-cancel").contains("cancel"));
    }

    @Test(timeout = 10000)
    @MediumTest
    public void authorizeAccount_authorizesAccount() throws InterruptedException,
            InvalidArgumentException {
        Looper.prepare();
        final BraintreeFragment fragment = getFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));
        fragment.addListener(new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertTrue(paymentMethod instanceof PayPalAccount);
                mLatch.countDown();
            }
        });
        setBrowserSwitchResponse(
                "onetouch/v1/success?payloadEnc=mockPayloadEnc&payload=mockPayload&x-source=com.braintree.browserswitch");

        PayPal.authorizeAccount(fragment);

        mLatch.await();
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
        fragment.addListener(new PaymentMethodCreatedListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertTrue(paymentMethod instanceof PayPalAccount);
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
    public void authorizeAccount_sendsAnalyticsEvent()
            throws JSONException, InvalidArgumentException {
        Looper.prepare();
        BraintreeFragment fragment = getMockFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_offline_paypal.json"));

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
        final BraintreeFragment fragment = getMockFragment(mActivity,
                stringFromFixture("client_token.json"),
                stringFromFixture("configuration_with_analytics.json"));
        fragment.addListener(new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                assertTrue(error instanceof ConfigurationException);
                assertEquals("PayPal is disabled or configuration is invalid",
                        error.getMessage());
                mLatch.countDown();
            }
        });

        PayPal.authorizeAccount(fragment);

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

    /** helpers */
    private void setBrowserSwitchResponse(final String responseUrl) {
        intending(hasExtraWithKey(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH))
                .respondWith(new ActivityResult(Activity.RESULT_FIRST_USER, null));

        mResponseUrl = "com.braintreepayments.api.test.braintree://" + responseUrl;

        IntentMonitorRegistry.getInstance().addIntentCallback(this);
    }

    @Override
    public void onIntentSent(Intent intent) {
        if (intent.hasExtra(BraintreeBrowserSwitchActivity.EXTRA_BROWSER_SWITCH) &&
                mResponseUrl != null) {
            mActivity.startActivity(new Intent().setData(Uri.parse(mResponseUrl)));
        }
    }
}
