package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.TokenizationParametersListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestAndroidPayConfigurationBuilder;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.identity.intents.model.CountrySpecification;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWallet.Builder;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.WalletConstants.CardNetwork;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
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

        Bundle tokenizationParameters =
                AndroidPay.getTokenizationParameters(fragment).getParameters();

        String actual = tokenizationParameters.getString("braintree:clientKey");
        String message = String.format("Expected [%s], but was [%s] from [%s]",
                TOKENIZATION_KEY, actual, tokenizationParameters.toString()
        );
        assertEquals(message, TOKENIZATION_KEY, actual);
    }

    @Test(timeout = 1000)
    public void getAllowedCardNetworks_returnsSupportedNetworks() {
        Configuration configuration = mBaseConfiguration.androidPay(mBaseConfiguration.androidPay()
                .supportedNetworks(new String[]{"visa", "mastercard", "amex", "discover"}))
                .buildConfiguration();

        BraintreeFragment fragment =
                getMockFragment(mActivityTestRule.getActivity(), configuration);

        Collection<Integer> allowedCardNetworks = AndroidPay.getAllowedCardNetworks(fragment);

        assertEquals(4, allowedCardNetworks.size());
        assertTrue(allowedCardNetworks.contains(CardNetwork.VISA));
        assertTrue(allowedCardNetworks.contains(CardNetwork.MASTERCARD));
        assertTrue(allowedCardNetworks.contains(CardNetwork.AMEX));
        assertTrue(allowedCardNetworks.contains(CardNetwork.DISCOVER));
    }

    @Test(timeout = 5000)
    public void performMaskedWalletRequest_sendsAnalyticsEvent()
            throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = getSetupFragment();
        injectFakeGoogleApiClient(fragment);
        when(fragment.getAuthorization()).thenReturn(
                Authorization.fromString("sandbox_abcdef_merchantId"));

        AndroidPay.performMaskedWalletRequest(fragment, Cart.newBuilder().build(), false, false,
                Collections.<CountrySpecification>emptyList(), 0);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("android-pay.selected");
        order.verify(fragment).sendAnalyticsEvent("android-pay.started");
    }

    @Test(timeout = 1000)
    public void performMaskedWalletRequest_sendsFailedEventWhenCartIsNull()
            throws InterruptedException, InvalidArgumentException {
        BraintreeFragment fragment = getSetupFragment();
        injectFakeGoogleApiClient(fragment);
        when(fragment.getAuthorization()).thenReturn(
                Authorization.fromString("sandbox_abcdef_merchantId"));

        AndroidPay.performMaskedWalletRequest(fragment, null, false, false, Collections.<CountrySpecification>emptyList(), 0);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("android-pay.selected");
        order.verify(fragment).sendAnalyticsEvent("android-pay.failed");
    }

    @Test(timeout = 1000)
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        BraintreeFragment fragment = getSetupFragment();

        AndroidPay.onActivityResult(fragment, Cart.newBuilder().build(), Activity.RESULT_CANCELED, new Intent());

        verify(fragment).sendAnalyticsEvent("android-pay.canceled");
    }

    @Test(timeout = 5000)
    public void onActivityResult_sendsAnalyticsEventOnNonOkOrCanceledResult() {
        BraintreeFragment fragment = getSetupFragment();

        AndroidPay.onActivityResult(fragment, Cart.newBuilder().build(), Activity.RESULT_FIRST_USER,
                new Intent());

        verify(fragment).sendAnalyticsEvent("android-pay.failed");
    }

    @Test(timeout = 1000)
    public void onActivityResult_sendsAnalyticsEventOnMaskedWalletResponse()
            throws InterruptedException {
        final BraintreeFragment fragment = getSetupFragment();
        injectFakeGoogleApiClient(fragment);

        MaskedWallet wallet = createMaskedWallet();
        Intent intent = new Intent()
                .putExtra(WalletConstants.EXTRA_MASKED_WALLET, wallet);

        AndroidPay.onActivityResult(fragment, Cart.newBuilder().build(), Activity.RESULT_OK, intent);

        verify(fragment).sendAnalyticsEvent("android-pay.authorized");
    }

    /* helpers */
    private void injectFakeGoogleApiClient(final BraintreeFragment fragment) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fragment.mGoogleApiClient =
                        new GoogleApiClient.Builder(mActivityTestRule.getActivity())
                                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                                        .setEnvironment(AndroidPay
                                                .getEnvironment(configuration.getAndroidPay()))
                                        .setTheme(WalletConstants.THEME_LIGHT)
                                        .build())
                                .build();
                latch.countDown();
            }
        });

        latch.await();
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

    private MaskedWallet createMaskedWallet() {
        Class maskedWalletClass = MaskedWallet.class;
        try {
            Constructor<MaskedWallet> constructor =
                    maskedWalletClass.getDeclaredConstructor(new Class[0]);
            constructor.setAccessible(true);
            MaskedWallet wallet = constructor.newInstance(new Object[0]);

            Builder builder = wallet.newBuilderFrom(wallet);
            builder.setGoogleTransactionId("braintree-android-pay-test");
            return builder.build();
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
