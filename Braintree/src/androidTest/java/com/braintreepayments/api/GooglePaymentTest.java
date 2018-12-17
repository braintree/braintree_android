package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.TokenizationParametersListener;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestAndroidPayConfigurationBuilder;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.ShippingAddressRequirements;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.WalletConstants.CardNetwork;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithConfiguration;
import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_PAYMENT_DATA_REQUEST;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class GooglePaymentTest {

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

        GooglePayment.isReadyToPay(fragment, new BraintreeResponseListener<Boolean>() {
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
        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        verify(fragment).startActivityForResult(any(Intent.class), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));
    }

    @Test
    public void requestPayment_startsActivityWithOptionalValues_GooglePaymentV1() {
        BraintreeFragment fragment = getSetupFragment();
        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .allowPrepaidCards(true)
                .billingAddressFormat(1)
                .billingAddressRequired(true)
                .emailRequired(true)
                .phoneNumberRequired(true)
                .shippingAddressRequired(true)
                .shippingAddressRequirements(ShippingAddressRequirements.newBuilder().addAllowedCountryCode("USA").build())
                .uiRequired(true)
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));
        Intent intent = captor.getValue();
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);
        CardRequirements cardRequirements = paymentDataRequest.getCardRequirements();
        assertNotNull(cardRequirements);
        assertTrue(cardRequirements.allowPrepaidCards());
        assertEquals(1, cardRequirements.getBillingAddressFormat());
        assertTrue(cardRequirements.isBillingAddressRequired());
        assertTrue(paymentDataRequest.isEmailRequired());
        assertTrue(paymentDataRequest.isPhoneNumberRequired());
        assertTrue(paymentDataRequest.isShippingAddressRequired());
        assertNotNull(paymentDataRequest.getShippingAddressRequirements());
        assertNotNull(paymentDataRequest.getShippingAddressRequirements().getAllowedCountryCodes());
        assertTrue(paymentDataRequest.getShippingAddressRequirements().getAllowedCountryCodes().contains("USA"));
        assertTrue(paymentDataRequest.isUiRequired());
    }

    @Ignore("Requires Google-Payment@2+")
    @Test
    public void requestPayment_startsActivityWithOptionalValues_GooglePaymentV2() throws JSONException {
        BraintreeFragment fragment = getSetupFragment();
        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .allowPrepaidCards(true)
                .billingAddressFormat(1)
                .billingAddressRequired(true)
                .emailRequired(true)
                .phoneNumberRequired(true)
                .shippingAddressRequired(true)
                .shippingAddressRequirements(ShippingAddressRequirements.newBuilder().addAllowedCountryCode("USA").build())
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(BraintreeRequestCodes.GOOGLE_PAYMENT));
        Intent intent = captor.getValue();

        assertEquals(GooglePaymentActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        PaymentDataRequest paymentDataRequest = intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST);

        JSONObject paymentDataRequestJson = new JSONObject(paymentDataRequest.toJson());

        assertEquals(2, paymentDataRequestJson.get("apiVersion"));
        assertEquals(0, paymentDataRequestJson.get("apiVersionMinor"));

        assertEquals(true, paymentDataRequestJson.get("emailRequired"));
        assertEquals(true, paymentDataRequestJson.get("shippingAddressRequired"));

        JSONObject transactionInfoJson = paymentDataRequestJson.getJSONObject("transactionInfo");
        assertEquals("FINAL", transactionInfoJson.getString("totalPriceStatus"));
        assertEquals("1.00", transactionInfoJson.getString("totalPrice"));
        assertEquals("USD", transactionInfoJson.getString("currencyCode"));

        JSONArray allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods");
        JSONObject paypal = allowedPaymentMethods.getJSONObject(0);
        assertEquals("PAYPAL", paypal.getString("type"));

        JSONArray purchaseUnits = paypal.getJSONObject("parameters")
                .getJSONObject("purchase_context")
                .getJSONArray("purchase_units");
        assertEquals(1, purchaseUnits.length());

        JSONObject purchaseUnit = purchaseUnits.getJSONObject(0);
        assertEquals("paypal-client-id", purchaseUnit.getJSONObject("payee")
                .getString("client_id"));
        assertEquals("true", purchaseUnit.getString("recurring_payment"));

        JSONObject paypalTokenizationSpecification = paypal.getJSONObject("tokenizationSpecification");
        assertEquals("PAYMENT_GATEWAY", paypalTokenizationSpecification.getString("type"));

        JSONObject paypalTokenizationSpecificationParams = paypalTokenizationSpecification.getJSONObject("parameters");
        assertEquals("braintree", paypalTokenizationSpecificationParams.getString("gateway"));
        assertEquals("v1", paypalTokenizationSpecificationParams.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, paypalTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("android-pay-merchant-id", paypalTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + BuildConfig.VERSION_NAME+ "\",\"platform\":\"android\"}", paypalTokenizationSpecificationParams.getString("braintree:metadata"));
        assertFalse(paypalTokenizationSpecificationParams.has("braintree:clientKey"));
        assertEquals("paypal-client-id", paypalTokenizationSpecificationParams.getString("braintree:paypalClientId"));

        JSONObject card = allowedPaymentMethods.getJSONObject(1);
        assertEquals("CARD", card.getString("type"));

        JSONObject cardParams = card.getJSONObject("parameters");
        assertTrue(cardParams.getBoolean("billingAddressRequired"));
        assertTrue(cardParams.getBoolean("allowPrepaidCards"));

        assertEquals("PAN_ONLY", cardParams.getJSONArray("allowedAuthMethods").getString(0));
        assertEquals("CRYPTOGRAM_3DS", cardParams.getJSONArray("allowedAuthMethods").getString(1));

        assertEquals("VISA", cardParams.getJSONArray("allowedCardNetworks").getString(0));
        assertEquals("MASTERCARD", cardParams.getJSONArray("allowedCardNetworks").getString(1));
        assertEquals("AMEX", cardParams.getJSONArray("allowedCardNetworks").getString(2));
        assertEquals("DISCOVER", cardParams.getJSONArray("allowedCardNetworks").getString(3));

        JSONObject tokenizationSpecification = card.getJSONObject("tokenizationSpecification");
        assertEquals("PAYMENT_GATEWAY", tokenizationSpecification.getString("type"));

        JSONObject cardTokenizationSpecificationParams = tokenizationSpecification.getJSONObject("parameters");
        assertEquals("braintree", cardTokenizationSpecificationParams.getString("gateway"));
        assertEquals("v1", cardTokenizationSpecificationParams.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, cardTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("android-pay-merchant-id", cardTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + BuildConfig.VERSION_NAME+ "\",\"platform\":\"android\"}", cardTokenizationSpecificationParams.getString("braintree:metadata"));
        assertEquals("sandbox_tokenization_key", cardTokenizationSpecificationParams.getString("braintree:clientKey"));
    }

    @Test
    public void requestPayment_sendsAnalyticsEvent() {
        BraintreeFragment fragment = getSetupFragment();
        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build());

        GooglePayment.requestPayment(fragment, googlePaymentRequest);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("google-payment.selected");
        order.verify(fragment).sendAnalyticsEvent("google-payment.started");
    }

    @Test
    public void requestPayment_postsExceptionWhenTransactionInfoIsNull() throws Exception {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayment.requestPayment(fragment, null);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("google-payment.selected");
        order.verify(fragment).sendAnalyticsEvent("google-payment.failed");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayment.onActivityResult(fragment, Activity.RESULT_CANCELED, new Intent());

        verify(fragment).sendAnalyticsEvent("google-payment.canceled");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnNonOkOrCanceledResult() {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayment.onActivityResult(fragment, Activity.RESULT_FIRST_USER, new Intent());

        verify(fragment).sendAnalyticsEvent("google-payment.failed");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnOkResponse() {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayment.onActivityResult(fragment, Activity.RESULT_OK, new Intent());

        verify(fragment).sendAnalyticsEvent("google-payment.authorized");
    }

    @Test(timeout = 5000)
    public void getTokenizationParameters_returnsCorrectParameters() throws Exception {
        String config = mBaseConfiguration.withAnalytics().build();

        final BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(), TOKENIZATION_KEY, config);

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                Bundle tokenizationParameters = GooglePayment.getTokenizationParameters(fragment).getParameters();

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
                Bundle tokenizationParameters = GooglePayment.getTokenizationParameters(fragment).getParameters();

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
                Bundle tokenizationParameters = GooglePayment.getTokenizationParameters(fragment).getParameters();

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
                Collection<Integer> allowedCardNetworks = GooglePayment.getAllowedCardNetworks(fragment);

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

    @Test(timeout = 5000)
    public void getTokenizationParameters_returnsCorrectParametersInCallback() throws Exception {
        String config = mBaseConfiguration.androidPay(mBaseConfiguration.androidPay()
                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .build();
        final Configuration configuration = Configuration.fromJson(config);

        BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(), TOKENIZATION_KEY, config);

        GooglePayment.getTokenizationParameters(fragment, new TokenizationParametersListener() {
            @Override
            public void onResult(PaymentMethodTokenizationParameters parameters,
                    Collection<Integer> allowedCardNetworks) {
                assertEquals("braintree", parameters.getParameters().getString("gateway"));
                assertEquals(configuration.getMerchantId(),
                        parameters.getParameters().getString("braintree:merchantId"));
                assertEquals(configuration.getAndroidPay().getGoogleAuthorizationFingerprint(),
                        parameters.getParameters().getString("braintree:authorizationFingerprint"));
                assertEquals("v1",
                        parameters.getParameters().getString("braintree:apiVersion"));
                assertEquals(BuildConfig.VERSION_NAME,
                        parameters.getParameters().getString("braintree:sdkVersion"));

                try {
                    JSONObject metadata = new JSONObject(parameters.getParameters().getString("braintree:metadata"));
                    assertNotNull(metadata);
                    assertEquals(BuildConfig.VERSION_NAME, metadata.getString("version"));
                    assertNotNull(metadata.getString("sessionId"));
                    assertEquals("custom", metadata.getString("integration"));
                    assertEquals("android", metadata.get("platform"));
                } catch (JSONException e) {
                    fail("Failed to unpack json from tokenization parameters: " + e.getMessage());
                }

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
                .environment("sandbox")
                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(true)
                        .clientId("paypal-client-id"))
                .withAnalytics()
                .build();

        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();

        try {
            when(fragment.getAuthorization()).thenReturn(Authorization.fromString("sandbox_tokenization_key"));
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        }

        return fragment;
    }
}
