package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;

import com.braintreepayments.api.annotations.Beta;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.AndroidPayCard;
import com.braintreepayments.api.models.Configuration;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;

/**
 * Class containing Android Pay specific logic.
 */
@Beta
public class AndroidPay {

    protected static final int ANDROID_PAY_MASKED_WALLET_REQUEST_CODE = 13489;
    protected static final int ANDROID_PAY_FULL_WALLET_REQUEST_CODE = 13590;

    /**
     * Get Braintree specific tokenization parameters for Android Pay. Useful for existing Google Wallet
     * or Android Pay integrations, or when full control over the {@link com.google.android.gms.wallet.MaskedWalletRequest}
     * and {@link com.google.android.gms.wallet.FullWalletRequest} is required.
     *
     * These parameters should be supplied to the
     * {@link com.google.android.gms.wallet.MaskedWalletRequest} via
     * {@link com.google.android.gms.wallet.MaskedWalletRequest.Builder#setPaymentMethodTokenizationParameters(PaymentMethodTokenizationParameters)}.
     *
     * @return the {@link PaymentMethodTokenizationParameters}
     */
    @Beta
    public static PaymentMethodTokenizationParameters getTokenizationParameters(BraintreeFragment fragment) {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.PAYMENT_GATEWAY)
                .addParameter("gateway", "braintree")
                .addParameter("braintree:merchantId", fragment.getConfiguration().getMerchantId())
                .addParameter("braintree:authorizationFingerprint",
                        fragment.getConfiguration().getAndroidPay().getGoogleAuthorizationFingerprint())
                .addParameter("braintree:apiVersion", "v1")
                .addParameter("braintree:sdkVersion", BuildConfig.VERSION_NAME)
                .build();
    }

    /**
     * Call this method when you've received a successful FullWallet request in your activity's
     * {@link Activity#onActivityResult(int, int, Intent)} to get an {@link AndroidPayCard} from a
     * {@link FullWallet}.
     *
     * @param fragment An instance of {@link BraintreeFragment}.
     * @param wallet a {@link FullWallet} from the Intent in
     *          {@link Activity#onActivityResult(int, int, Intent)}
     */
    @Beta
    public static void tokenize(BraintreeFragment fragment, FullWallet wallet) {
        try {
            fragment.postCallback(AndroidPayCard.fromJson(wallet.getPaymentMethodToken().getToken()));
        } catch (JSONException e) {
            fragment.postCallback(e);
        }
    }

    /**
     * Launch an Android Pay masked wallet request. This method will show the payment instrument
     * chooser to the user.
     *
     * @param fragment The current {@link BraintreeFragment}.
     * @param cart The cart representation with price and optionally items.
     * @param isBillingAgreement {@code true} if this request is for a billing agreement, {@code false} otherwise.
     * @param shippingAddressRequired {@code true} if this request requires a shipping address, {@code false} otherwise.
     * @param phoneNumberRequired {@code true} if this request requires a phone number, {@code false} otherwise.
     * @param requestCode The requestCode to use with {@link Activity#startActivityForResult(Intent, int)}
     */
    protected static void performMaskedWalletRequest(BraintreeFragment fragment, Cart cart,
            boolean isBillingAgreement, boolean shippingAddressRequired,
            boolean phoneNumberRequired, int requestCode) {
        if (isBillingAgreement && cart != null) {
            fragment.postCallback(new InvalidArgumentException(
                    "The cart must be null when isBillingAgreement is true"));
            return;
        } else if (!isBillingAgreement && cart == null) {
            fragment.postCallback(new InvalidArgumentException(
                    "Cart cannot be null unless isBillingAgreement is true"));
            return;
        }

        MaskedWalletRequest.Builder maskedWalletRequestBuilder = MaskedWalletRequest.newBuilder()
                .setMerchantName(getMerchantName(fragment.getConfiguration()))
                .setCurrencyCode("USD")
                .setCart(cart)
                .setIsBillingAgreement(isBillingAgreement)
                .setShippingAddressRequired(shippingAddressRequired)
                .setPhoneNumberRequired(phoneNumberRequired)
                .setPaymentMethodTokenizationParameters(getTokenizationParameters(fragment));

        if (cart != null) {
            maskedWalletRequestBuilder.setEstimatedTotalPrice(cart.getTotalPrice());
        }

        Wallet.Payments.loadMaskedWallet(fragment.getGoogleApiClient(),
                maskedWalletRequestBuilder.build(), requestCode);
    }

    /**
     * Perform a full wallet request. This can only be done after a masked wallet request has been
     * made.
     *
     * @param fragment The current {@link BraintreeFragment} through which the callbacks should
     *          be forwarded
     * @param googleTransactionId The transaction id from the {@link MaskedWallet}.
     */
    protected static void performFullWalletRequest(BraintreeFragment fragment,
            Cart cart, boolean isBillingAgreement, String googleTransactionId) {
        FullWalletRequest.Builder fullWalletRequestBuilder = FullWalletRequest.newBuilder()
                .setGoogleTransactionId(googleTransactionId);

        if (!isBillingAgreement) {
            fullWalletRequestBuilder.setCart(cart);
        }

        Wallet.Payments.loadFullWallet(fragment.getGoogleApiClient(), fullWalletRequestBuilder.build(),
                ANDROID_PAY_FULL_WALLET_REQUEST_CODE);
    }

    protected static void onActivityResult(BraintreeFragment fragment, Cart cart,
            boolean isBillingAgreement, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (data.hasExtra(WalletConstants.EXTRA_MASKED_WALLET)) {
                String googleTransactionId =
                        ((MaskedWallet) data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET))
                        .getGoogleTransactionId();
                performFullWalletRequest(fragment, cart, isBillingAgreement, googleTransactionId);
            } else if (data.hasExtra(WalletConstants.EXTRA_FULL_WALLET)) {
                tokenize(fragment,
                        (FullWallet) data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET));
            }
        }
    }

    protected static int getEnvironment(Configuration configuration) {
        if("production".equals(configuration.getAndroidPay().getEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_SANDBOX;
        }
    }

    private static String getMerchantName(Configuration configuration) {
        if (configuration.getAndroidPay().getDisplayName() != null) {
            return configuration.getAndroidPay().getDisplayName();
        } else {
            return "";
        }
    }
}
