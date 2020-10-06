package com.braintreepayments.api;

import android.content.Intent;
import android.os.Bundle;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

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
import com.braintreepayments.testutils.TestConfigurationBuilder.TestGooglePaymentConfigurationBuilder;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragment;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithConfiguration;
import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_PAYMENT_DATA_REQUEST;
import static com.braintreepayments.testutils.FixturesHelper.base64EncodedClientTokenFromFixture;
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

@RunWith(AndroidJUnit4ClassRunner.class)
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
                .googlePayment(new TestGooglePaymentConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint"))
                .merchantId("android-pay-merchant-id");
    }

    @Test(timeout = 5000)
    public void isReadyToPay_returnsFalseWhenGooglePaymentIsNotEnabled() throws Exception {
        String configuration = new TestConfigurationBuilder()
                .googlePayment(new TestGooglePaymentConfigurationBuilder().enabled(false))
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
    public void requestPayment_startsActivityWithOptionalValues() throws JSONException {
        BraintreeFragment fragment = getSetupFragment();
        String googlePaymentModuleVersion = com.braintreepayments.api.googlepayment.BuildConfig.VERSION_NAME;
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
        assertEquals("paypal-client-id-for-google-payment", purchaseUnit.getJSONObject("payee")
                .getString("client_id"));
        assertEquals("true", purchaseUnit.getString("recurring_payment"));

        JSONObject paypalTokenizationSpecification = paypal.getJSONObject("tokenizationSpecification");
        assertEquals("PAYMENT_GATEWAY", paypalTokenizationSpecification.getString("type"));

        JSONObject paypalTokenizationSpecificationParams = paypalTokenizationSpecification.getJSONObject("parameters");
        assertEquals("braintree", paypalTokenizationSpecificationParams.getString("gateway"));
        assertEquals("v1", paypalTokenizationSpecificationParams.getString("braintree:apiVersion"));
        assertEquals(googlePaymentModuleVersion, paypalTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("android-pay-merchant-id", paypalTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + googlePaymentModuleVersion + "\",\"platform\":\"android\"}", paypalTokenizationSpecificationParams.getString("braintree:metadata"));
        assertFalse(paypalTokenizationSpecificationParams.has("braintree:clientKey"));
        assertEquals("paypal-client-id-for-google-payment", paypalTokenizationSpecificationParams.getString("braintree:paypalClientId"));

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
        assertEquals(googlePaymentModuleVersion, cardTokenizationSpecificationParams.getString("braintree:sdkVersion"));
        assertEquals("android-pay-merchant-id", cardTokenizationSpecificationParams.getString("braintree:merchantId"));
        assertEquals("{\"source\":\"client\",\"version\":\"" + googlePaymentModuleVersion + "\",\"platform\":\"android\"}", cardTokenizationSpecificationParams.getString("braintree:metadata"));
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
    public void requestPayment_postsExceptionWhenTransactionInfoIsNull() {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayment.requestPayment(fragment, null);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("google-payment.selected");
        order.verify(fragment).sendAnalyticsEvent("google-payment.failed");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayment.onActivityResult(fragment, RESULT_CANCELED, new Intent());

        verify(fragment).sendAnalyticsEvent("google-payment.canceled");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnNonOkOrCanceledResult() {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayment.onActivityResult(fragment, RESULT_FIRST_USER, new Intent());

        verify(fragment).sendAnalyticsEvent("google-payment.failed");
    }

    @Test
    public void onActivityResult_sendsAnalyticsEventOnOkResponse() {
        BraintreeFragment fragment = getSetupFragment();

        GooglePayment.onActivityResult(fragment, RESULT_OK, new Intent());

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
                assertEquals(configuration.getGooglePayment().getGoogleAuthorizationFingerprint(),
                        tokenizationParameters.getString("braintree:authorizationFingerprint"));
                assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
                assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));

                mLatch.countDown();
            }
        });

        mLatch.await();
    }

    @Test(timeout = 5000)
    public void getTokenizationParameters_doesNotIncludeATokenizationKeyWhenNotPresent() {
        final BraintreeFragment fragment = getFragment(mActivityTestRule.getActivity(),
                base64EncodedClientTokenFromFixture("client_token.json"), mBaseConfiguration.build());

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
        String configuration = mBaseConfiguration.googlePayment(mBaseConfiguration.googlePayment()
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
        String config = mBaseConfiguration.googlePayment(mBaseConfiguration.googlePayment()
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
                assertEquals(configuration.getGooglePayment().getGoogleAuthorizationFingerprint(),
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
        String configuration = mBaseConfiguration.googlePayment(mBaseConfiguration.googlePayment()
                .environment("sandbox")
                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .googlePayment(new TestGooglePaymentConfigurationBuilder()
                        .environment("sandbox")
                        .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"})
                        .paypalClientId("paypal-client-id-for-google-payment")
                        .enabled(true))
                .paypal(new TestConfigurationBuilder.TestPayPalConfigurationBuilder(true)
                        .clientId("paypal-client-id-for-paypal"))
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
