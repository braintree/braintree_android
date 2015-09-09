package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.ClientKey;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestActivity;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWallet.Builder;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.WalletConstants.CardNetwork;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static com.braintreepayments.testutils.TestClientKey.CLIENT_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class AndroidPayTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    @Test(timeout = 1000)
    @SmallTest
    public void getTokenizationParameters_returnsCorrectParameters() {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint())
                .thenReturn("google-auth-fingerprint");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);
        BraintreeFragment fragment =
                getMockFragment(mActivityTestRule.getActivity(), configuration);

        Bundle tokenizationParameters =
                AndroidPay.getTokenizationParameters(fragment).getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(),
                tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(androidPayConfiguration.getGoogleAuthorizationFingerprint(),
                tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME,
                tokenizationParameters.getString("braintree:sdkVersion"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getTokenizationParameters_doesNotIncludeAClientKeyWhenNotPresent() {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint())
                .thenReturn("google-auth-fingerprint");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);
        BraintreeFragment fragment =
                getMockFragment(mActivityTestRule.getActivity(), configuration);

        Bundle tokenizationParameters =
                AndroidPay.getTokenizationParameters(fragment).getParameters();

        assertNull(tokenizationParameters.getString("braintree:clientKey"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getTokenizationParameters_includesAClientKeyWhenPresent()
            throws InvalidArgumentException, InterruptedException {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint()).thenReturn(
                "google-auth-fingerprint");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);
        final BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(),
                CLIENT_KEY,
                configuration);
        when(fragment.getClientKey()).thenReturn(ClientKey.fromString(CLIENT_KEY));

        Bundle tokenizationParameters =
                AndroidPay.getTokenizationParameters(fragment).getParameters();

        String actual = tokenizationParameters.getString("braintree:clientKey");
        String message = String.format("Expected [%s], but was [%s] from [%s]",
                CLIENT_KEY, actual, tokenizationParameters.toString()
        );
        assertEquals(message, CLIENT_KEY, actual);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getAllowedCardNetworks_returnsSupportedNetworks() {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint()).thenReturn(
                "google-auth-fingerprint");
        when(androidPayConfiguration.getSupportedNetworks())
                .thenReturn(new String[]{"visa", "mastercard", "amex", "discover"});
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);
        BraintreeFragment fragment =
                getMockFragment(mActivityTestRule.getActivity(), configuration);

        Collection<Integer> allowedCardNetworks = AndroidPay.getAllowedCardNetworks(fragment);

        assertEquals(4, allowedCardNetworks.size());
        assertTrue(allowedCardNetworks.contains(CardNetwork.VISA));
        assertTrue(allowedCardNetworks.contains(CardNetwork.MASTERCARD));
        assertTrue(allowedCardNetworks.contains(CardNetwork.AMEX));
        assertTrue(allowedCardNetworks.contains(CardNetwork.DISCOVER));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void performMaskedWalletRequest_sendsAnalyticsEvent() throws InterruptedException {
        BraintreeFragment fragment = getSetupFragment();
        injectFakeGoogleApiClient(fragment);

        AndroidPay.performMaskedWalletRequest(fragment, null, true, false, false, 0);

        InOrder order = inOrder(fragment);
        order.verify(fragment).sendAnalyticsEvent("android-pay.selected");
        order.verify(fragment).sendAnalyticsEvent("android-pay.started");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void performMaskedWalletRequest_sendsAnalyticsEventForCartAndBillingAgreement() {
        BraintreeFragment fragment = getSetupFragment();

        AndroidPay.performMaskedWalletRequest(fragment, Cart.newBuilder().build(), true, false,
                false, 0);

        verify(fragment).sendAnalyticsEvent("android-pay.failed");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void performMaskedWalletRequest_sendsAnalyticsEventForNoCartOrBillingAgreement() {
        BraintreeFragment fragment = getSetupFragment();

        AndroidPay.performMaskedWalletRequest(fragment, null, false, false, false, 0);

        verify(fragment).sendAnalyticsEvent("android-pay.failed");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_sendsAnalyticsEventOnCancel() {
        BraintreeFragment fragment = getSetupFragment();

        AndroidPay.onActivityResult(fragment, null, false, Activity.RESULT_CANCELED, new Intent());

        verify(fragment).sendAnalyticsEvent("android-pay.canceled");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_sendsAnalyticsEventOnNonOkOrCanceledResult() {
        BraintreeFragment fragment = getSetupFragment();

        AndroidPay.onActivityResult(fragment, null, false, Activity.RESULT_FIRST_USER,
                new Intent());

        verify(fragment).sendAnalyticsEvent("android-pay.failed");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_sendsAnalyticsEventOnMaskedWalletResponse()
            throws InterruptedException {
        final BraintreeFragment fragment = getSetupFragment();
        injectFakeGoogleApiClient(fragment);

        MaskedWallet wallet = createMaskedWallet();
        Intent intent = new Intent()
                .putExtra(WalletConstants.EXTRA_MASKED_WALLET, wallet);

        AndroidPay.onActivityResult(fragment, null, false, Activity.RESULT_OK, intent);

        verify(fragment).sendAnalyticsEvent("android-pay.authorized");
    }

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

    /* helpers */
    private BraintreeFragment getSetupFragment() {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint()).thenReturn(
                "google-auth-fingerprint");
        when(androidPayConfiguration.getSupportedNetworks())
                .thenReturn(new String[]{"visa", "mastercard", "amex", "discover"});
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);

        BraintreeFragment fragment =
                getMockFragment(mActivityTestRule.getActivity(), configuration);
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
