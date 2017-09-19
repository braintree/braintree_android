package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentsRequest;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestAndroidPayConfigurationBuilder;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.WalletConstants.CardNetwork;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithConfiguration;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragmentWithConfiguration;
import static com.braintreepayments.api.GooglePaymentsActivity.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePaymentsActivity.EXTRA_PAYMENT_DATA_REQUEST;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class GooglePaymentsTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private CountDownLatch mLatch;
    private TestConfigurationBuilder mBaseConfiguration;

    @Before
    public void setup() {
        mLatch = new CountDownLatch(1);
        mBaseConfiguration = new TestConfigurationBuilder()
                .androidPay(new TestAndroidPayConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint"))
                .merchantId("android-pay-merchant-id");
    }

    @Test(timeout = 5000)
    public void isReadyToPay_returnsFalseWhenAndroidPayIsNotEnabled() throws Exception {
        String configuration = new TestConfigurationBuilder()
                .androidPay(new TestAndroidPayConfigurationBuilder().enabled(false))
                .build();

        BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(), TOKENIZATION_KEY, configuration);

        GooglePayments.isReadyToPay(fragment, new BraintreeResponseListener<Boolean>() {
            @Override
            public void onResponse(Boolean isReadyToPay) {
                assertFalse(isReadyToPay);
                mLatch.countDown();
            }
        });

        mLatch.await();
    }

    @Test
    public void requestPayment_startsActivity() {
        BraintreeFragment fragment = getSetupFragment();
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        GooglePaymentsRequest googlePaymentsRequest = new GooglePaymentsRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePayments.requestPayment(fragment, googlePaymentsRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENTS));
        Intent intent = captor.getValue();
        assertEquals(GooglePaymentsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        assertEquals(googlePaymentsRequest.getTransactionInfo(), paymentDataRequest.getTransactionInfo());
        assertEquals(2, paymentDataRequest.getAllowedPaymentMethods().size());
        assertTrue(paymentDataRequest.getAllowedPaymentMethods().contains(WalletConstants.PAYMENT_METHOD_CARD));
        assertTrue(paymentDataRequest.getAllowedPaymentMethods().contains(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD));
        List<Integer> allowedCardNetworks = paymentDataRequest.getCardRequirements().getAllowedCardNetworks();
        assertEquals(4, allowedCardNetworks.size());
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_VISA));
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_DISCOVER));
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_MASTERCARD));
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_AMEX));
        assertEquals(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY,
                paymentDataRequest.getPaymentMethodTokenizationParameters().getPaymentMethodTokenizationType());

        Bundle expectedParameters = GooglePayments.getTokenizationParameters(fragment)
                .getParameters();
        Bundle actualParameters = paymentDataRequest.getPaymentMethodTokenizationParameters().getParameters();
        assertEquals(expectedParameters.getString("gateway"), actualParameters.getString("gateway"));
        assertEquals(expectedParameters.getString("braintree:merchantId"), actualParameters.getString("braintree:merchantId"));
        assertEquals(expectedParameters.getString("braintree:authorizationFingerprint"),
                actualParameters.getString("braintree:authorizationFingerprint"));
        assertEquals(expectedParameters.getString("braintree:apiVersion"), actualParameters.getString("braintree:apiVersion"));
        assertEquals(expectedParameters.getString("braintree:sdkVersion"), actualParameters.getString("braintree:sdkVersion"));
        assertEquals(expectedParameters.getString("braintree:metadata"), actualParameters.getString("braintree:metadata"));
    }

    @Test
    public void requestPayment_sendsAnalyticsEvent() throws Exception {
        BraintreeFragment fragment = getSetupFragment();
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        GooglePaymentsRequest googlePaymentsRequest = new GooglePaymentsRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePayments.requestPayment(fragment, googlePaymentsRequest);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("google-payments.selected");
        order.verify(fragment).sendAnalyticsEvent("google-payments.started");
    }

    @Test
    public void requestPayment_postsExceptionWhenTransactionInfoIsNull() throws Exception {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayments.requestPayment(fragment, null);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("google-payments.selected");
        order.verify(fragment).sendAnalyticsEvent("google-payments.failed");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayments.onActivityResult(fragment, Activity.RESULT_CANCELED, new Intent());

        verify(fragment).sendAnalyticsEvent("google-payments.canceled");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnNonOkOrCanceledResult() {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayments.onActivityResult(fragment, Activity.RESULT_FIRST_USER, new Intent());

        verify(fragment).sendAnalyticsEvent("google-payments.failed");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnOkResponse() throws InterruptedException {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayments.onActivityResult(fragment, Activity.RESULT_OK, new Intent());

        verify(fragment).sendAnalyticsEvent("google-payments.authorized");
    }

    @Test(timeout = 5000)
    public void getTokenizationParameters_returnsCorrectParameters() throws Exception {
        String config = mBaseConfiguration.withAnalytics().build();

        final BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(), TOKENIZATION_KEY, config);

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                Bundle tokenizationParameters = GooglePayments.getTokenizationParameters(fragment).getParameters();

                assertEquals("braintree", tokenizationParameters.getString("gateway"));
                assertEquals(configuration.getMerchantId(), tokenizationParameters.getString("braintree:merchantId"));
                assertEquals(configuration.getAndroidPay().getGoogleAuthorizationFingerprint(),
                        tokenizationParameters.getString("braintree:authorizationFingerprint"));
                assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
                assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));

                mLatch.countDown();
            }
        });

        mLatch.await();
    }

    @Test(timeout = 5000)
    public void getTokenizationParameters_doesNotIncludeATokenizationKeyWhenNotPresent() throws Exception {
        final BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(),
                stringFromFixture("client_token.json"), mBaseConfiguration.build());

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                Bundle tokenizationParameters = GooglePayments.getTokenizationParameters(fragment).getParameters();

                assertNull(tokenizationParameters.getString("braintree:clientKey"));

                mLatch.countDown();
            }
        });

        mLatch.countDown();
    }

    @Test(timeout = 5000)
    public void getTokenizationParameters_includesATokenizationKeyWhenPresent() throws Exception {
        final BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(), TOKENIZATION_KEY,
                mBaseConfiguration.withAnalytics().build());

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                Bundle tokenizationParameters = GooglePayments.getTokenizationParameters(fragment).getParameters();

                assertEquals(TOKENIZATION_KEY,  tokenizationParameters.getString("braintree:clientKey"));

                mLatch.countDown();
            }
        });

        mLatch.await();
    }

    @Test(timeout = 5000)
    public void getAllowedCardNetworks_returnsSupportedNetworks() throws InterruptedException {
        String configuration = mBaseConfiguration.androidPay(mBaseConfiguration.androidPay()
                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .build();

        final BraintreeFragment fragment = getFragmentWithConfiguration(mActivityTestRule.getActivity(), configuration);

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                Collection<Integer> allowedCardNetworks = GooglePayments.getAllowedCardNetworks(fragment);

                assertEquals(4, allowedCardNetworks.size());
                assertTrue(allowedCardNetworks.contains(CardNetwork.VISA));
                assertTrue(allowedCardNetworks.contains(CardNetwork.MASTERCARD));
                assertTrue(allowedCardNetworks.contains(CardNetwork.AMEX));
                assertTrue(allowedCardNetworks.contains(CardNetwork.DISCOVER));

                mLatch.countDown();
            }
        });

        mLatch.await();
    }

    private BraintreeFragment getSetupFragment() {
        String configuration = mBaseConfiguration.androidPay(mBaseConfiguration.androidPay()
                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .build();

        BraintreeFragment fragment = getMockFragmentWithConfiguration(mActivityTestRule.getActivity(), configuration);
        when(fragment.getHttpClient()).thenReturn(mock(BraintreeHttpClient.class));

        return fragment;
    }
}
