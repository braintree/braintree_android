package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.exceptions.AndroidPayException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.TokenizationParametersListener;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.AndroidPayCardNonce;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.identity.intents.model.CountrySpecification;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;

import static com.braintreepayments.api.AndroidPayActivity.AUTHORIZE;
import static com.braintreepayments.api.AndroidPayActivity.CHANGE_PAYMENT_METHOD;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_ALLOWED_CARD_NETWORKS;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_ALLOWED_COUNTRIES;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_CART;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_ERROR;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_GOOGLE_TRANSACTION_ID;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_MERCHANT_NAME;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_PHONE_NUMBER_REQUIRED;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_REQUEST_TYPE;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_SHIPPING_ADDRESS_REQUIRED;
import static com.braintreepayments.api.AndroidPayActivity.EXTRA_TOKENIZATION_PARAMETERS;

/**
 * @deprecated Android Pay is deprecated, use {@link GooglePayment} instead. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/pay-with-google/overview">documentation</a>
 *
 * Used to create and tokenize Android Pay payment methods. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/android-pay/overview">documentation</a>
 */
@Deprecated
public class AndroidPay {

    /**
     * @deprecated Android Pay is deprecated, use {@link GooglePayment} instead. For more information see the
     * <a href="https://developers.braintreepayments.com/guides/pay-with-google/overview">documentation</a>
     *
     * Before starting the Android Pay flow, use
     * {@link #isReadyToPay(BraintreeFragment, BraintreeResponseListener)} to check whether the
     * user has the Android Pay app installed and is ready to pay. When the listener is called with
     * {@code true}, show the Android Pay button. When it is called with {@code false}, display other
     * checkout options along with text notifying the user to set up the Android Pay app.
     *
     * @param fragment {@link BraintreeFragment}
     * @param listener Instance of {@link BraintreeResponseListener<Boolean>} to receive the
     *                 isReadyToPay response.
     */
    @Deprecated
    public static void isReadyToPay(final BraintreeFragment fragment,
            final BraintreeResponseListener<Boolean> listener) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.getAndroidPay().isEnabled(fragment.getApplicationContext())) {
                    listener.onResponse(false);
                    return;
                }

                fragment.getGoogleApiClient(new BraintreeResponseListener<GoogleApiClient>() {
                    @Override
                    public void onResponse(GoogleApiClient googleApiClient) {
                        Wallet.Payments.isReadyToPay(googleApiClient).setResultCallback(
                                new ResultCallback<BooleanResult>() {
                                    @Override
                                    public void onResult(@NonNull BooleanResult booleanResult) {
                                        listener.onResponse(booleanResult.getStatus().isSuccess()
                                                && booleanResult.getValue());
                                    }
                                });
                    }
                });
            }
        });
    }

    /**
     * @deprecated Android Pay is deprecated, use {@link GooglePayment} instead. For more information see the
     * <a href="https://developers.braintreepayments.com/guides/pay-with-google/overview">documentation</a>
     *
     * Get Braintree specific tokenization parameters for Android Pay. Useful for existing Google
     * Wallet or Android Pay integrations, or when full control over the
     * {@link com.google.android.gms.wallet.MaskedWalletRequest} and
     * {@link com.google.android.gms.wallet.FullWalletRequest} is required.
     *
     * {@link PaymentMethodTokenizationParameters} should be supplied to the
     * {@link MaskedWalletRequest} via
     * {@link com.google.android.gms.wallet.MaskedWalletRequest.Builder#setPaymentMethodTokenizationParameters(PaymentMethodTokenizationParameters)}
     * and {@link Collection<Integer>} allowedCardNetworks should be supplied to the
     * {@link MaskedWalletRequest} via
     * {@link com.google.android.gms.wallet.MaskedWalletRequest.Builder#addAllowedCardNetworks(Collection)}.
     *
     * @param fragment {@link BraintreeFragment}
     * @param listener Instance of {@link TokenizationParametersListener} to receive the
     *                 {@link PaymentMethodTokenizationParameters}.
     */
    @Deprecated
    public static void getTokenizationParameters(final BraintreeFragment fragment,
            final TokenizationParametersListener listener) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                listener.onResult(GooglePayment.getTokenizationParameters(fragment),
                        GooglePayment.getAllowedCardNetworks(fragment));
            }
        });
    }

    /**
     * @deprecated Android Pay is deprecated, use {@link GooglePayment} instead. For more information see the
     * <a href="https://developers.braintreepayments.com/guides/pay-with-google/overview">documentation</a>
     *
     * Launch an Android Pay masked wallet request. This method will show the payment instrument
     * chooser to the user.
     *
     * @param fragment The current {@link BraintreeFragment}.
     * @param cart The cart representation with price, currency code, and optionally items.
     * @param shippingAddressRequired {@code true} if this request requires a shipping address, {@code false} otherwise.
     * @param phoneNumberRequired {@code true} if this request requires a phone number, {@code false} otherwise.
     * @param allowedCountries ISO 3166-2 country codes that shipping is allowed to.
     */
    @Deprecated
    public static void requestAndroidPay(final BraintreeFragment fragment, final @NonNull Cart cart,
            final boolean shippingAddressRequired, final boolean phoneNumberRequired,
            final ArrayList<CountrySpecification> allowedCountries) {
        fragment.sendAnalyticsEvent("android-pay.selected");

        if (!validateManifest(fragment.getApplicationContext())) {
            fragment.postCallback(new BraintreeException("AndroidPayActivity was not found in the Android manifest, " +
                    "or did not have a theme of R.style.bt_transparent_activity"));
            fragment.sendAnalyticsEvent("android-pay.failed");
            return;
        }

        if (cart == null) {
            fragment.postCallback(new BraintreeException("Cannot pass null cart to performMaskedWalletRequest"));
            fragment.sendAnalyticsEvent("android-pay.failed");
            return;
        }

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fragment.sendAnalyticsEvent("android-pay.started");

                Intent intent = new Intent(fragment.getApplicationContext(), AndroidPayActivity.class)
                        .putExtra(EXTRA_ENVIRONMENT, GooglePayment.getEnvironment(configuration.getAndroidPay()))
                        .putExtra(EXTRA_MERCHANT_NAME, configuration.getAndroidPay().getDisplayName())
                        .putExtra(EXTRA_CART, cart)
                        .putExtra(EXTRA_TOKENIZATION_PARAMETERS, GooglePayment.getTokenizationParameters(fragment))
                        .putIntegerArrayListExtra(EXTRA_ALLOWED_CARD_NETWORKS, GooglePayment.getAllowedCardNetworks(fragment))
                        .putExtra(EXTRA_SHIPPING_ADDRESS_REQUIRED, shippingAddressRequired)
                        .putExtra(EXTRA_PHONE_NUMBER_REQUIRED, phoneNumberRequired)
                        .putParcelableArrayListExtra(EXTRA_ALLOWED_COUNTRIES, allowedCountries)
                        .putExtra(EXTRA_REQUEST_TYPE, AUTHORIZE);
                fragment.startActivityForResult(intent, BraintreeRequestCodes.ANDROID_PAY);
            }
        });
    }

    /**
     * @deprecated Android Pay is deprecated, use {@link GooglePayment} instead. For more information see the
     * <a href="https://developers.braintreepayments.com/guides/pay-with-google/overview">documentation</a>
     *
     * Performs a change masked wallet request. This will allow the user to change the backing card and other information
     * associated with the payment method.
     *
     * @param fragment The current {@link BraintreeFragment}.
     * @param androidPayCardNonce the {@link AndroidPayCardNonce} to update.
     */
    @Deprecated
    public static void changePaymentMethod(final BraintreeFragment fragment,
            final AndroidPayCardNonce androidPayCardNonce) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fragment.sendAnalyticsEvent("android-pay.change-masked-wallet");

                Intent intent = new Intent(fragment.getApplicationContext(), AndroidPayActivity.class)
                        .putExtra(EXTRA_ENVIRONMENT, GooglePayment.getEnvironment(configuration.getAndroidPay()))
                        .putExtra(EXTRA_GOOGLE_TRANSACTION_ID, androidPayCardNonce.getGoogleTransactionId())
                        .putExtra(EXTRA_CART, androidPayCardNonce.getCart())
                        .putExtra(EXTRA_REQUEST_TYPE, CHANGE_PAYMENT_METHOD);
                fragment.startActivityForResult(intent, BraintreeRequestCodes.ANDROID_PAY);
            }
        });
    }

    /**
     * @deprecated Use {@link #tokenize(BraintreeFragment, FullWallet, Cart)} instead.
     */
    @Deprecated
    public static void tokenize(BraintreeFragment fragment, FullWallet wallet) {
        tokenize(fragment, wallet, null);
    }

    /**
     * @deprecated Android Pay is deprecated, use {@link GooglePayment} instead. For more information see the
     * <a href="https://developers.braintreepayments.com/guides/pay-with-google/overview">documentation</a>
     *
     * Call this method when you've received a successful FullWallet request in your activity's
     * {@link Activity#onActivityResult(int, int, Intent)} to get an {@link AndroidPayCardNonce} from a
     * {@link FullWallet}.
     *
     * @param fragment An instance of {@link BraintreeFragment}.
     * @param wallet a {@link FullWallet} from the Intent in {@link Activity#onActivityResult(int, int, Intent)}.
     * @param cart the {@link Cart} used when creating the {@link FullWallet}.
     */
    @Deprecated
    public static void tokenize(BraintreeFragment fragment, FullWallet wallet, Cart cart) {
        try {
            fragment.postCallback(AndroidPayCardNonce.fromFullWallet(wallet, cart));
            fragment.sendAnalyticsEvent("android-pay.nonce-received");
        } catch (JSONException e) {
            fragment.sendAnalyticsEvent("android-pay.failed");

            try {
                fragment.postCallback(ErrorWithResponse.fromJson(wallet.getPaymentMethodToken().getToken()));
            } catch (JSONException e1) {
                fragment.postCallback(e1);
            }
        }
    }

    static void onActivityResult(BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (data.hasExtra(WalletConstants.EXTRA_FULL_WALLET)) {
                fragment.sendAnalyticsEvent("android-pay.authorized");
                tokenize(fragment, (FullWallet) data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET),
                        (Cart) data.getParcelableExtra(EXTRA_CART));
            }
        } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            fragment.sendAnalyticsEvent("android-pay.canceled");
        } else {
            if (data != null) {
                if (data.hasExtra(EXTRA_ERROR)) {
                    fragment.postCallback(new AndroidPayException(data.getStringExtra(EXTRA_ERROR)));
                } else {
                    fragment.postCallback(new AndroidPayException("Android Pay error code: " +
                            data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1) +
                            " see https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants " +
                            "for more details"));
                }
            }

            fragment.sendAnalyticsEvent("android-pay.failed");
        }
    }

    private static boolean validateManifest(Context context) {
        ActivityInfo activityInfo = ManifestValidator.getActivityInfo(context, AndroidPayActivity.class);
        return activityInfo != null && activityInfo.getThemeResource() == R.style.bt_transparent_activity;
    }
}
