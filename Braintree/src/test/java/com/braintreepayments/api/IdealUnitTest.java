package com.braintreepayments.api;

import android.app.Activity;
import android.net.Uri;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeApiHttpClient;
import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.IdealBank;
import com.braintreepayments.api.models.IdealRequest;
import com.braintreepayments.api.models.IdealResult;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestBraintreeApiConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestIdealConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import androidx.appcompat.app.AppCompatActivity;

import static com.braintreepayments.api.Ideal.IDEAL_RESULT_ID;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.json.*", "org.mockito.*", "org.robolectric.*", "android.*", "com.google.gms.*"})
@PrepareForTest({Ideal.class})
public class IdealUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private Configuration mConfiguration;
    private BraintreeFragment mBraintreeFragment;
    private BraintreeApiHttpClient mMockApiClient;

    @Before
    public void setUp() throws InvalidArgumentException {
        mConfiguration = new TestConfigurationBuilder()
                .assetsUrl("http://assets.example.com")
                .ideal(new TestIdealConfigurationBuilder()
                        .routeId("some-route-id")
                        .assetsUrl("http://assets.example.com"))
                .braintreeApi(new TestBraintreeApiConfigurationBuilder()
                        .accessToken("access-token")
                        .url("http://api.braintree.com"))
                .buildConfiguration();

        mBraintreeFragment = getMockFragment(stringFromFixture("client_token.json"), mConfiguration);

        mMockApiClient = mock(BraintreeApiHttpClient.class);
        when(mBraintreeFragment.getBraintreeApiHttpClient()).thenReturn(mMockApiClient);
    }

    private BraintreeFragment getMockFragment(String authorization, Configuration configuration) {
        try {
            return new MockFragmentBuilder()
                    .authorization(Authorization.fromString(authorization))
                    .configuration(configuration)
                    .build();
        } catch (InvalidArgumentException e) {
            fail(e.getMessage());
            return null;
        }
    }

    @Test
    public void fetchIssuingBanks_postsConfigurationExceptionWhenBraintreeApiNotEnabled()
            throws InvalidArgumentException, InterruptedException {
        Configuration configuration = new TestConfigurationBuilder()
                .ideal(new TestIdealConfigurationBuilder()
                        .routeId("some-route-id"))
                .buildConfiguration();

        BraintreeFragment fragment = getMockFragment(stringFromFixture("client_token.json"), configuration);

        Ideal.fetchIssuingBanks(fragment, new BraintreeResponseListener<List<IdealBank>>() {
            @Override
            public void onResponse(List<IdealBank> idealBanks) {
                fail("Success listener called");
            }
        });

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        Exception e = captor.getValue();

        assertEquals("Your access is restricted and cannot use this part of the Braintree API.", e.getMessage());
    }

    @Test
    public void fetchIssuingBanks_postsConfigurationExceptionWhenIdealNotEnabled() throws InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .braintreeApi(new TestBraintreeApiConfigurationBuilder()
                        .accessToken("access-token")
                        .url("http://api.braintree.com"))
                .buildConfiguration();

        BraintreeFragment fragment = getMockFragment(stringFromFixture("client_token.json"), configuration);

        Ideal.fetchIssuingBanks(fragment, new BraintreeResponseListener<List<IdealBank>>() {
            @Override
            public void onResponse(List<IdealBank> idealBanks) {
                fail("Success listener called");
            }
        });

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(fragment).postCallback(captor.capture());
        Exception e = captor.getValue();

        assertEquals("iDEAL is not enabled for this merchant.", e.getMessage());
    }

    @Test
    public void fetchIssuingBanks_postsCallbackToFragment() throws InvalidArgumentException {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.failure(new IOException());
                return null;
            }
        }).when(mMockApiClient).get(eq("/issuers/ideal"), any(HttpResponseCallback.class));

        Ideal.fetchIssuingBanks(mBraintreeFragment, new BraintreeResponseListener<List<IdealBank>>() {
            @Override
            public void onResponse(List<IdealBank> idealBanks) {
                fail("Success listener called");
            }
        });

        verify(mBraintreeFragment).postCallback(any(IOException.class));
    }

    @Test
    public void fetchIssuingBanks_success_sendsAnalyticsEvent() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.success(stringFromFixture("payment_methods/ideal_issuing_banks.json"));
                return null;
            }
        }).when(mMockApiClient).get(eq("/issuers/ideal"), any(HttpResponseCallback.class));

        Ideal.fetchIssuingBanks(mBraintreeFragment, new BraintreeResponseListener<List<IdealBank>>() {
            @Override
            public void onResponse(List<IdealBank> idealBanks) {
                verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.load.succeeded"));
            }
        });
    }

    @Test
    public void fetchIssuingBanks_failure_sendsAnalyticsEvent() {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.failure(new IOException());
                return null;
            }
        }).when(mMockApiClient).get(eq("/issuers/ideal"), any(HttpResponseCallback.class));

        Ideal.fetchIssuingBanks(mBraintreeFragment, null);

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.load.failed"));
    }

    @Test
    public void fetchIssuingBanks_postsErrorToListenerOnJsonParsingError() throws InvalidArgumentException {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.success("gibberish");
                return null;
            }
        }).when(mMockApiClient).get(eq("/issuers/ideal"), any(HttpResponseCallback.class));

        Ideal.fetchIssuingBanks(mBraintreeFragment, new BraintreeResponseListener<List<IdealBank>>() {
            @Override
            public void onResponse(List<IdealBank> idealBanks) {
                fail("Success listener called");
            }
        });

        verify(mBraintreeFragment).postCallback(any(JSONException.class));
    }

    @Test
    public void startPayment_startsBrowserWithProperRequestCode() throws InterruptedException, InvalidArgumentException {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.success(stringFromFixture("payment_methods/ideal_issuing_banks.json"));
                return null;
            }
        }).when(mMockApiClient).get(eq("/issuers/ideal"), any(HttpResponseCallback.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String requestBody = (String) invocation.getArguments()[1];

                JSONObject json = new JSONObject(requestBody);
                assertEquals("some-route-id", json.getString("route_id"));

                String expectedRedirectUrl = Uri.parse("http://assets.example.com/mobile/ideal-redirect/0.0.0/index.html?redirect_url=" + mBraintreeFragment.getReturnUrlScheme() + "://").toString();
                assertEquals(expectedRedirectUrl, json.getString("redirect_url"));

                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/pending_ideal_bank_payment.json"));
                return null;
            }
        }).when(mMockApiClient).post(eq("/ideal-payments"), any(String.class), any(HttpResponseCallback.class));

        Ideal.fetchIssuingBanks(mBraintreeFragment, new BraintreeResponseListener<List<IdealBank>>() {
            @Override
            public void onResponse(List<IdealBank> idealBanks) {
                IdealBank bank = idealBanks.get(0);

                IdealRequest builder = new IdealRequest()
                        .issuerId(bank.getId())
                        .amount("1.00")
                        .currency("EUR")
                        .orderId("abc-123");

                Ideal.startPayment(mBraintreeFragment, builder, null);

                verify(mBraintreeFragment).browserSwitch(eq(BraintreeRequestCodes.IDEAL), eq("http://approval.example.com/"));
                latch.countDown();
            }
        });

        latch.await();
    }

    @Test
    public void startPayment_success_sendsAnalyticsEvent() throws JSONException {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/pending_ideal_bank_payment.json"));
                return null;
            }
        }).when(mMockApiClient).post(eq("/ideal-payments"), any(String.class), any(HttpResponseCallback.class));

        List<IdealBank> banks = IdealBank.fromJson(mConfiguration, stringFromFixture("payment_methods/ideal_issuing_banks.json"));
        IdealRequest builder = new IdealRequest()
                .issuerId(banks.get(0).getId())
                .amount("1.00")
                .currency("EUR")
                .orderId("abc-123");
        Ideal.startPayment(mBraintreeFragment, builder, null);

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.start-payment.selected"));
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.webswitch.initiate.succeeded"));
    }

    @Test
    public void startPayment_failure_sendsAnalyticsEvent() throws JSONException, InvalidArgumentException {
        mConfiguration = new TestConfigurationBuilder()
                .assetsUrl("http://assets.example.com")
                .buildConfiguration();

        mBraintreeFragment = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("client_token.json")))
                .configuration(mConfiguration)
                .build();

        Ideal.startPayment(mBraintreeFragment, new IdealRequest(), null);

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.start-payment.selected"));
        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.start-payment.invalid-configuration"));
    }

    @Test
    public void startPayment_persistsIdealResultId() throws InvalidArgumentException, JSONException {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/pending_ideal_bank_payment.json"));
                return null;
            }
        }).when(mMockApiClient).post(eq("/ideal-payments"), any(String.class), any(HttpResponseCallback.class));

        List<IdealBank> banks = IdealBank.fromJson(mConfiguration, stringFromFixture("payment_methods/ideal_issuing_banks.json"));
        IdealRequest builder = new IdealRequest()
                .issuerId(banks.get(0).getId())
                .amount("1.00")
                .currency("EUR")
                .orderId("abc-123");
        Ideal.startPayment(mBraintreeFragment, builder, null);

        String idealResultId = getResultIdFromPrefs();
        assertEquals("ideal_payment_id", idealResultId);
    }

    @Test
    public void startPayment_callsResponseListenerWithPaymentId()
            throws InvalidArgumentException, JSONException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.success(stringFromFixture("payment_methods/pending_ideal_bank_payment.json"));
                return null;
            }
        }).when(mMockApiClient).post(eq("/ideal-payments"), any(String.class), any(HttpResponseCallback.class));

        List<IdealBank> banks = IdealBank.fromJson(mConfiguration, stringFromFixture("payment_methods/ideal_issuing_banks.json"));
        IdealRequest builder = new IdealRequest()
                .issuerId(banks.get(0).getId())
                .amount("1.00")
                .currency("EUR")
                .orderId("abc-123");

        Ideal.startPayment(mBraintreeFragment, builder, new BraintreeResponseListener<IdealResult>() {
            @Override
            public void onResponse(IdealResult idealResult) {
                assertEquals("ideal_payment_id", idealResult.getId());
                latch.countDown();
            }
        });

        latch.await();
    }

    @Test
    public void startPayment_callsExceptionListenerOnHttpError() throws InvalidArgumentException, JSONException {
        final Exception expectedException = new Exception();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[2];
                callback.failure(expectedException);
                return null;
            }
        }).when(mMockApiClient).post(eq("/ideal-payments"), any(String.class), any(HttpResponseCallback.class));

        List<IdealBank> banks = IdealBank.fromJson(mConfiguration, stringFromFixture("payment_methods/ideal_issuing_banks.json"));
        IdealRequest builder = new IdealRequest()
                .issuerId(banks.get(0).getId())
                .amount("1.00")
                .currency("EUR")
                .orderId("abc-123");
        Ideal.startPayment(mBraintreeFragment, builder, null);

        verify(mBraintreeFragment).postCallback(eq(expectedException));
    }

    @Test
    public void startPayment_callsExceptionListenerOnJsonParsingError()
            throws InvalidArgumentException, JSONException, InterruptedException {
        putResultIdInPrefs("ideal_payment_id");

        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.success("gibberish");
                latch.countDown();
                return null;
            }
        }).when(mMockApiClient).get(eq("/ideal-payments/ideal_payment_id/status"), any(HttpResponseCallback.class));

        Ideal.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_OK);

        latch.await();

        verify(mBraintreeFragment).postCallback(any(JSONException.class));
    }

    @Test
    public void onActivityResult_callsExceptionListenerOnNonCompletedOrPendingStatus()
            throws InvalidArgumentException, JSONException {
        putResultIdInPrefs("ideal_payment_id");

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.success(stringFromFixture("payment_methods/failed_ideal_bank_payment.json"));
                return null;
            }
        }).when(mMockApiClient).get(eq("/ideal-payments/ideal_payment_id/status"), any(HttpResponseCallback.class));

        Ideal.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_OK);

        ArgumentCaptor<IdealResult> captor = ArgumentCaptor.forClass(IdealResult.class);
        verify(mBraintreeFragment).postCallback(captor.capture());
        IdealResult actualBankResult = captor.getValue();
        assertEquals("ideal_payment_id", actualBankResult.getId());
        assertEquals("short_id", actualBankResult.getShortId());
        assertEquals("FAILED", actualBankResult.getStatus());
    }

    @Test
    public void onActivityResult_resultOk_sendsAnalyticsEvent() throws JSONException {
        putResultIdInPrefs("ideal_payment_id");

        Ideal.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_OK);

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.webswitch.succeeded"));
    }

    @Test
    public void onActivityResult_resultCanceled_sendsAnalyticsEvent() throws JSONException {
        putResultIdInPrefs("ideal_payment_id");

        Ideal.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_CANCELED);

        verify(mBraintreeFragment).sendAnalyticsEvent(eq("ideal.webswitch.canceled"));
    }

    @Test
    public void onActivityResult_returnsResultToFragment()
            throws InvalidArgumentException, JSONException, InterruptedException {
        putResultIdInPrefs("ideal_payment_id");

        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.success(stringFromFixture("payment_methods/completed_ideal_bank_payment.json"));
                latch.countDown();
                return null;
            }
        }).when(mMockApiClient).get(eq("/ideal-payments/ideal_payment_id/status"), any(HttpResponseCallback.class));

        Ideal.onActivityResult(mBraintreeFragment, AppCompatActivity.RESULT_OK);

        latch.await();

        ArgumentCaptor<IdealResult> captor = ArgumentCaptor.forClass(IdealResult.class);
        verify(mBraintreeFragment).postCallback(captor.capture());

        IdealResult capturedResult = captor.getValue();
        assertEquals("ideal_payment_id", capturedResult.getId());
        assertEquals("short_id", capturedResult.getShortId());
        assertEquals("COMPLETE", capturedResult.getStatus());
    }


    @Test(timeout = 10000)
    public void onActivityResult_checksPaymentStatusOnceBeforeInvokingCallback()
            throws InvalidArgumentException, JSONException, InterruptedException {
        Configuration configuration = new TestConfigurationBuilder()
                .ideal(new TestIdealConfigurationBuilder()
                        .routeId("some-route-id"))
                .braintreeApi(new TestBraintreeApiConfigurationBuilder()
                        .accessToken("access-token")
                        .url("http://api.braintree.com"))
                .buildConfiguration();

        final BraintreeFragment fragment = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("client_token.json")))
                .configuration(configuration)
                .build();

        BraintreeApiHttpClient apiHttpClient = mock(BraintreeApiHttpClient.class);
        when(fragment.getBraintreeApiHttpClient()).thenReturn(apiHttpClient);

        final String resultFixture = stringFromFixture("payment_methods/pending_ideal_bank_payment.json");

        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.success(resultFixture);
                latch.countDown();
                return null;
            }
        }).when(apiHttpClient).get(eq("/ideal-payments/ideal_payment_id/status"), any(HttpResponseCallback.class));

        IdealResult idealResult = IdealResult.fromJson(resultFixture);
        putResultIdInPrefs(idealResult.getId());
        Ideal.onActivityResult(fragment, AppCompatActivity.RESULT_OK);

        latch.await();
    }

    @Test(timeout = 5000)
    public void pollForCompletion_pollsUntilMaxRetryCountExceeded()
            throws InterruptedException, JSONException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .ideal(new TestIdealConfigurationBuilder()
                        .routeId("some-route-id"))
                .braintreeApi(new TestBraintreeApiConfigurationBuilder()
                        .accessToken("access-token")
                        .url("http://api.braintree.com"))
                .buildConfiguration();

        final BraintreeFragment fragment = new MockFragmentBuilder()
                .authorization(Authorization.fromString(stringFromFixture("client_token.json")))
                .configuration(configuration)
                .build();

        BraintreeApiHttpClient apiHttpClient = mock(BraintreeApiHttpClient.class);
        when(fragment.getBraintreeApiHttpClient()).thenReturn(apiHttpClient);

        final String resultFixture = stringFromFixture("payment_methods/pending_ideal_bank_payment.json");
        final CountDownLatch latch = new CountDownLatch(2);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpResponseCallback callback = (HttpResponseCallback) invocation.getArguments()[1];
                callback.success(resultFixture);
                latch.countDown();
                return null;
            }
        }).when(apiHttpClient).get(eq("/ideal-payments/ideal_payment_id/status"), any(HttpResponseCallback.class));

        IdealResult idealResult = IdealResult.fromJson(resultFixture);
        putResultIdInPrefs(idealResult.getId());

        Ideal.pollForCompletion(fragment, idealResult.getId(), 1, 1000);

        // Two retries = three calls to latch.countDown
        latch.await();
    }

    @Test(expected = InvalidArgumentException.class)
    public void pollForCompletion_throwsException_whenRetryCountInvalid_lowerBound() throws InvalidArgumentException {
        Ideal.pollForCompletion(mBraintreeFragment, "ideal-id", -1, 1000);
    }

    @Test(expected = InvalidArgumentException.class)
    public void pollForCompletion_throwsException_whenRetryCountInvalid_upperBound() throws InvalidArgumentException {
        Ideal.pollForCompletion(mBraintreeFragment, "ideal-id", 12, 1000);
    }

    @Test(expected = InvalidArgumentException.class)
    public void pollForCompletion_throwsException_whenDelayInvalid_lowerBound() throws InvalidArgumentException {
        Ideal.pollForCompletion(mBraintreeFragment, "ideal-id", 1, 999);
    }

    @Test(expected = InvalidArgumentException.class)
    public void pollForCompletion_throwsException_whenDelayInvalid_upperBound() throws InvalidArgumentException {
        Ideal.pollForCompletion(mBraintreeFragment, "ideal-id", 1, 10001);
    }

    private void putResultIdInPrefs(String resultId) {
        BraintreeSharedPreferences.putString(mBraintreeFragment.getApplicationContext(), IDEAL_RESULT_ID, resultId);
    }

    private String getResultIdFromPrefs() {
        return BraintreeSharedPreferences.getString(mBraintreeFragment.getApplicationContext(), IDEAL_RESULT_ID);
    }
}
