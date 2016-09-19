package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.TokenizationParametersListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AndroidPayCardNonce;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestAndroidPayConfigurationBuilder;
import com.google.android.gms.identity.intents.model.CountrySpecification;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.Address;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.InstrumentInfo;
import com.google.android.gms.wallet.PaymentMethodToken;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.ProxyCard;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.WalletConstants.CardNetwork;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.AndroidPayActivity.CHANGE_PAYMENT_METHOD;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_ALLOWED_CARD_NETWORKS;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_ALLOWED_COUNTRIES;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_CART;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_GOOGLE_TRANSACTION_ID;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_MERCHANT_NAME;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_PHONE_NUMBER_REQUIRED;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_REQUEST_TYPE;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_SHIPPING_ADDRESS_REQUIRED;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_TOKENIZATION_PARAMETERS;
import static com.braintreepayments.api.AndroidPayActivity.AUTHORIZE;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class AndroidPayTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private TestConfigurationBuilder mBaseConfiguration;

    @Before
    public void setup() {
        mBaseConfiguration = new TestConfigurationBuilder()
                .androidPay(new TestAndroidPayConfigurationBuilder()
                        .googleAuthorizationFingerprint("google-auth-fingerprint"))
                .merchantId("android-pay-merchant-id");
    }

    @Test(timeout = 1000)
    public void isReadyToPay_returnsFalseWhenAndroidPayIsNotEnabled()
            throws InvalidArgumentException, InterruptedException {
        Configuration configuration = new TestConfigurationBuilder()
                .androidPay(new TestAndroidPayConfigurationBuilder().enabled(false))
                .buildConfiguration();

        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), configuration);
        when(fragment.getAuthorization()).thenReturn(Authorization.fromString(TOKENIZATION_KEY));
        final CountDownLatch latch = new CountDownLatch(1);

        AndroidPay.isReadyToPay(fragment, new BraintreeResponseListener<Boolean>() {
            @Override
            public void onResponse(Boolean isReadyToPay) {
                assertFalse(isReadyToPay);
                latch.countDown();
            }
        });

        latch.await();
    }

    @Test(timeout = 1000)
    public void getTokenizationParameters_returnsCorrectParametersInCallback()
            throws InvalidArgumentException, InterruptedException {
        final Configuration configuration = mBaseConfiguration.androidPay(mBaseConfiguration.androidPay()
                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .buildConfiguration();

        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), configuration);
        when(fragment.getAuthorization()).thenReturn(Authorization.fromString(TOKENIZATION_KEY));
        when(fragment.getSessionId()).thenReturn("session-id");
        when(fragment.getIntegrationType()).thenReturn("custom");
        final CountDownLatch latch = new CountDownLatch(1);

        AndroidPay.getTokenizationParameters(fragment, new TokenizationParametersListener() {
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
                    assertEquals("session-id", metadata.getString("sessionId"));
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

                latch.countDown();
            }
        });

        latch.await();
    }

    @Test(timeout = 1000)
    public void getTokenizationParameters_returnsCorrectParameters() throws InvalidArgumentException {
        Configuration configuration = mBaseConfiguration.withAnalytics().buildConfiguration();

        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), configuration);
        when(fragment.getAuthorization()).thenReturn(Authorization.fromString("sandbox_abcdef_merchantIDHere"));

        Bundle tokenizationParameters = AndroidPay.getTokenizationParameters(fragment).getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(),
                tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(configuration.getAndroidPay().getGoogleAuthorizationFingerprint(),
                tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME,
                tokenizationParameters.getString("braintree:sdkVersion"));
    }

    @Test(timeout = 1000)
    public void getTokenizationParameters_doesNotIncludeATokenizationKeyWhenNotPresent()
            throws InvalidArgumentException {
        Configuration configuration = mBaseConfiguration.buildConfiguration();

        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), configuration);
        when(fragment.getAuthorization()).thenReturn(Authorization.fromString(stringFromFixture("client_token.json")));

        Bundle tokenizationParameters = AndroidPay.getTokenizationParameters(fragment).getParameters();

        assertNull(tokenizationParameters.getString("braintree:clientKey"));
    }

    @Test(timeout = 1000)
    public void getTokenizationParameters_includesATokenizationKeyWhenPresent()
            throws InvalidArgumentException, InterruptedException {
        Configuration configuration = mBaseConfiguration.withAnalytics().buildConfiguration();
        final BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), TOKENIZATION_KEY,
                configuration);

        Bundle tokenizationParameters = AndroidPay.getTokenizationParameters(fragment).getParameters();

        String actual = tokenizationParameters.getString("braintree:clientKey");
        String message = String.format("Expected [%s], but was [%s] from [%s]",
                TOKENIZATION_KEY, actual, tokenizationParameters.toString()
        );
        assertEquals(message, TOKENIZATION_KEY, actual);
    }

    @Test(timeout = 5000)
    public void getAllowedCardNetworks_returnsSupportedNetworks() {
        Configuration configuration = mBaseConfiguration.androidPay(mBaseConfiguration.androidPay()
                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .buildConfiguration();

        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), configuration);

        Collection<Integer> allowedCardNetworks = AndroidPay.getAllowedCardNetworks(fragment);

        assertEquals(4, allowedCardNetworks.size());
        assertTrue(allowedCardNetworks.contains(CardNetwork.VISA));
        assertTrue(allowedCardNetworks.contains(CardNetwork.MASTERCARD));
        assertTrue(allowedCardNetworks.contains(CardNetwork.AMEX));
        assertTrue(allowedCardNetworks.contains(CardNetwork.DISCOVER));
    }

    @Test
    public void requestAndroidPay_startsActivity() {
        BraintreeFragment fragment = getSetupFragment();
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        Cart cart = Cart.newBuilder().build();
        ArrayList<CountrySpecification> allowedCountries = new ArrayList<>();

        AndroidPay.requestAndroidPay(fragment, cart, true, true, allowedCountries);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(AndroidPay.ANDROID_PAY_REQUEST_CODE));
        Intent intent = captor.getValue();
        assertEquals(AndroidPayActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(AUTHORIZE, intent.getIntExtra(EXTRA_REQUEST_TYPE, -1));
        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("", intent.getStringExtra(EXTRA_MERCHANT_NAME));
        assertEquals(cart, intent.getParcelableExtra(EXTRA_CART));
        assertTrue(intent.getBooleanExtra(EXTRA_SHIPPING_ADDRESS_REQUIRED, false));
        assertTrue(intent.getBooleanExtra(EXTRA_PHONE_NUMBER_REQUIRED, false));
        assertEquals(allowedCountries, intent.getParcelableArrayListExtra(EXTRA_ALLOWED_COUNTRIES));
        assertNotNull(intent.getParcelableExtra(EXTRA_TOKENIZATION_PARAMETERS));
        assertNotNull(intent.getIntegerArrayListExtra(EXTRA_ALLOWED_CARD_NETWORKS));
    }

    @Test
    public void requestAndroidPay_sendsAnalyticsEvent() throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = getSetupFragment();
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());

        AndroidPay.requestAndroidPay(fragment, Cart.newBuilder().build(), false, false,
                new ArrayList<CountrySpecification>());

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("android-pay.selected");
        order.verify(fragment).sendAnalyticsEvent("android-pay.started");
    }

    @Test(timeout = 1000)
    public void requestAndroidPay_postsExceptionWhenCartIsNull() throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = getSetupFragment();

        AndroidPay.requestAndroidPay(fragment, null, false, false, new ArrayList<CountrySpecification>());

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("android-pay.selected");
        order.verify(fragment).sendAnalyticsEvent("android-pay.failed");
    }

    @Test(timeout = 5000)
    public void changePaymentMethod_startsActivity() {
        BraintreeFragment fragment = getSetupFragment();
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        AndroidPayCardNonce androidPayCardNonce = mock(AndroidPayCardNonce.class);
        when(androidPayCardNonce.getGoogleTransactionId()).thenReturn("google-transaction-id");

        AndroidPay.changePaymentMethod(fragment, androidPayCardNonce);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(fragment).startActivityForResult(captor.capture(), eq(AndroidPay.ANDROID_PAY_REQUEST_CODE));
        Intent intent = captor.getValue();
        assertEquals(AndroidPayActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(CHANGE_PAYMENT_METHOD, intent.getIntExtra(EXTRA_REQUEST_TYPE, -1));
        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.getIntExtra(EXTRA_ENVIRONMENT, -1));
        assertEquals("google-transaction-id", intent.getStringExtra(EXTRA_GOOGLE_TRANSACTION_ID));
    }

    @Test(timeout = 1000)
    public void changePaymentMethod_sendsAnalyticsEvent() {
        BraintreeFragment fragment = getSetupFragment();
        doNothing().when(fragment).startActivityForResult(any(Intent.class), anyInt());
        AndroidPayCardNonce androidPayCardNonce = mock(AndroidPayCardNonce.class);
        when(androidPayCardNonce.getGoogleTransactionId()).thenReturn("google-transaction-id");

        AndroidPay.changePaymentMethod(fragment, androidPayCardNonce);

        verify(fragment).sendAnalyticsEvent("android-pay.change-masked-wallet");
    }

    @Test(timeout = 1000)
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        BraintreeFragment fragment = getSetupFragment();

        AndroidPay.onActivityResult(fragment, Activity.RESULT_CANCELED, new Intent());

        verify(fragment).sendAnalyticsEvent("android-pay.canceled");
    }

    @Test(timeout = 5000)
    public void onActivityResult_sendsAnalyticsEventOnNonOkOrCanceledResult() {
        BraintreeFragment fragment = getSetupFragment();

        AndroidPay.onActivityResult(fragment, Activity.RESULT_FIRST_USER, new Intent());

        verify(fragment).sendAnalyticsEvent("android-pay.failed");
    }

    @Test(timeout = 1000)
    public void onActivityResult_sendsAnalyticsEventOnFullWalletResponse() throws InterruptedException {
        BraintreeFragment fragment = getSetupFragment();

        FullWallet wallet = createFullWallet();
        Intent intent = new Intent()
                .putExtra(WalletConstants.EXTRA_FULL_WALLET, wallet);

        AndroidPay.onActivityResult(fragment, Activity.RESULT_OK, intent);

        verify(fragment).sendAnalyticsEvent("android-pay.authorized");
    }

    private BraintreeFragment getSetupFragment() {
        Configuration configuration = mBaseConfiguration.androidPay(mBaseConfiguration.androidPay()
                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .withAnalytics()
                .buildConfiguration();

        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(), configuration);
        when(fragment.getHttpClient()).thenReturn(mock(BraintreeHttpClient.class));

        return fragment;
    }

    private FullWallet createFullWallet() {
        try {
            Class paymentMethodTokenClass = PaymentMethodToken.class;
            Class[] tokenParams = new Class[] { int.class, int.class, String.class };
            Constructor<PaymentMethodToken> tokenConstructor = paymentMethodTokenClass.getDeclaredConstructor(tokenParams);
            tokenConstructor.setAccessible(true);
            PaymentMethodToken token = tokenConstructor.newInstance(0, 0, "");

            Class fullWalletClass = FullWallet.class;
            Class[] walletParams = new Class[] { int.class, String.class, String.class, ProxyCard.class, String.class,
                    Address.class, Address.class, String[].class, UserAddress.class, UserAddress.class,
                    InstrumentInfo[].class, PaymentMethodToken.class };
            Constructor<FullWallet> walletConstructor = fullWalletClass.getDeclaredConstructor(walletParams);
            walletConstructor.setAccessible(true);
            return walletConstructor.newInstance(0, null, null, null, null, null, null, null, null, null, null, token);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }
}
