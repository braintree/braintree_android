package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.GoogleApiClientException;
import com.braintreepayments.api.exceptions.GoogleApiClientException.ErrorType;
import com.braintreepayments.api.exceptions.GooglePaymentException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.TokenizationParametersListener;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.MetadataBuilder;
import com.braintreepayments.api.models.TokenizationKey;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;

import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePaymentActivity.EXTRA_PAYMENT_DATA_REQUEST;

/**
 * Used to create and tokenize Google Payments payment methods. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/pay-with-google/overview">documentation</a>
 */
public class GooglePayment {

    private static final String VISA_NETWORK = "visa";
    private static final String MASTERCARD_NETWORK = "mastercard";
    private static final String AMEX_NETWORK = "amex";
    private static final String DISCOVER_NETWORK = "discover";

    /**
     * Before starting the Google Payments flow, use
     * {@link #isReadyToPay(BraintreeFragment, BraintreeResponseListener)} to check whether the
     * Google Payment API is supported and set up on the device. When the listener is called with
     * {@code true}, show the Google Payments button. When it is called with {@code false}, display other
     * checkout options.
     *
     * @param fragment {@link BraintreeFragment}
     * @param listener Instance of {@link BraintreeResponseListener<Boolean>} to receive the
     *                 isReadyToPay response.
     */
    public static void isReadyToPay(final BraintreeFragment fragment, final BraintreeResponseListener<Boolean> listener) {
        try {
            Class.forName(PaymentsClient.class.getName());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            listener.onResponse(false);
            return;
        }

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.getAndroidPay().isEnabled(fragment.getApplicationContext())) {
                    listener.onResponse(false);
                    return;
                }

                if (fragment.getActivity() == null) {
                    fragment.postCallback(new GoogleApiClientException(ErrorType.NotAttachedToActivity, 1));
                }

                PaymentsClient paymentsClient = Wallet.getPaymentsClient(fragment.getActivity(),
                        new Wallet.WalletOptions.Builder()
                                .setEnvironment(getEnvironment(configuration.getAndroidPay()))
                                .build());

                IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .build();

                paymentsClient.isReadyToPay(request).addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        try {
                            listener.onResponse(task.getResult(ApiException.class));
                        } catch (ApiException e) {
                            listener.onResponse(false);
                        }
                    }
                });
            }
        });
    }

    /**
     * Get Braintree specific tokenization parameters for a Google Payment. Useful for when full control over the
     * {@link PaymentDataRequest} is required.
     *
     * {@link PaymentMethodTokenizationParameters} should be supplied to the
     * {@link PaymentDataRequest} via
     * {@link PaymentDataRequest.Builder#setPaymentMethodTokenizationParameters(PaymentMethodTokenizationParameters)}
     * and {@link Collection<Integer>} allowedCardNetworks should be supplied to the
     * {@link CardRequirements} via
     * {@link CardRequirements.Builder#addAllowedCardNetworks(Collection)}}.
     *
     * @param fragment {@link BraintreeFragment}
     * @param listener Instance of {@link TokenizationParametersListener} to receive the
     *                 {@link PaymentMethodTokenizationParameters}.
     */
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
     * Launch a Google Payments request. This method will show the payment instrument chooser to the user.
     *
     * @param fragment The current {@link BraintreeFragment}.
     * @param request The {@link GooglePaymentRequest} containing options for the transaction.
     */
    public static void requestPayment(final BraintreeFragment fragment, final @NonNull GooglePaymentRequest request) {
        fragment.sendAnalyticsEvent("google-payment.selected");

        if (!validateManifest(fragment.getApplicationContext())) {
            fragment.postCallback(new BraintreeException("GooglePaymentActivity was not found in the Android " +
                    "manifest, or did not have a theme of R.style.bt_transparent_activity"));
            fragment.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        if (request == null || request.getTransactionInfo() == null) {
            fragment.postCallback(new BraintreeException("Cannot pass null TransactionInfo to requestPayment"));
            fragment.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                PaymentDataRequest.Builder paymentDataRequest = PaymentDataRequest.newBuilder()
                        .setTransactionInfo(request.getTransactionInfo())
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .setPaymentMethodTokenizationParameters(getTokenizationParameters(fragment));

                CardRequirements.Builder cardRequirements = CardRequirements.newBuilder()
                        .addAllowedCardNetworks(getAllowedCardNetworks(fragment));
                if (request.getAllowPrepaidCards() != null) {
                    cardRequirements.setAllowPrepaidCards(request.getAllowPrepaidCards());
                }

                if (request.getBillingAddressFormat() != null) {
                    cardRequirements.setBillingAddressFormat(request.getBillingAddressFormat());
                }

                if (request.isBillingAddressRequired() != null) {
                    cardRequirements.setBillingAddressRequired(request.isBillingAddressRequired());
                }

                paymentDataRequest.setCardRequirements(cardRequirements.build());

                if (request.isEmailRequired() != null) {
                    paymentDataRequest.setEmailRequired(request.isEmailRequired());
                }

                if (request.isPhoneNumberRequired() != null) {
                    paymentDataRequest.setPhoneNumberRequired(request.isPhoneNumberRequired());
                }

                if (request.isShippingAddressRequired() != null) {
                    paymentDataRequest.setShippingAddressRequired(request.isShippingAddressRequired());
                }

                if (request.getShippingAddressRequirements() != null) {
                    paymentDataRequest.setShippingAddressRequirements(request.getShippingAddressRequirements());
                }

                if (request.isUiRequired() != null) {
                    paymentDataRequest.setUiRequired(request.isUiRequired());
                }

                fragment.sendAnalyticsEvent("google-payment.started");

                Intent intent = new Intent(fragment.getApplicationContext(), GooglePaymentActivity.class)
                        .putExtra(EXTRA_ENVIRONMENT, getEnvironment(configuration.getAndroidPay()))
                        .putExtra(EXTRA_PAYMENT_DATA_REQUEST, paymentDataRequest.build());
                fragment.startActivityForResult(intent, BraintreeRequestCodes.GOOGLE_PAYMENT);
            }
        });
    }

    /**
     * Call this method when you've received a successful {@link PaymentData} response in your activity's
     * {@link Activity#onActivityResult(int, int, Intent)} to get a {@link GooglePaymentCardNonce}.
     *
     * @param fragment An instance of {@link BraintreeFragment}.
     * @param paymentData {@link PaymentData} from the Intent in {@link Activity#onActivityResult(int, int, Intent)}.
     */
    public static void tokenize(BraintreeFragment fragment, PaymentData paymentData) {
        try {
            fragment.postCallback(GooglePaymentCardNonce.fromPaymentData(paymentData));
            fragment.sendAnalyticsEvent("google-payment.nonce-received");
        } catch (JSONException | NullPointerException e) {
            fragment.sendAnalyticsEvent("google-payment.failed");

            try {
                fragment.postCallback(ErrorWithResponse.fromJson(paymentData.getPaymentMethodToken().getToken()));
            } catch (JSONException | NullPointerException e1) {
                fragment.postCallback(e1);
            }
        }
    }

    static void onActivityResult(BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            fragment.sendAnalyticsEvent("google-payment.authorized");
            tokenize(fragment, PaymentData.getFromIntent(data));
        } else if (resultCode == AutoResolveHelper.RESULT_ERROR) {
            fragment.sendAnalyticsEvent("google-payment.failed");

            fragment.postCallback(new GooglePaymentException("An error was encountered during the Google Payments " +
                    "flow. See the status object in this exception for more details.",
                    AutoResolveHelper.getStatusFromIntent(data)));
        } else if (resultCode == Activity.RESULT_CANCELED) {
            fragment.sendAnalyticsEvent("google-payment.canceled");
        }
    }

    static int getEnvironment(AndroidPayConfiguration configuration) {
        if ("production".equals(configuration.getEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }

    static PaymentMethodTokenizationParameters getTokenizationParameters(BraintreeFragment fragment) {
        PaymentMethodTokenizationParameters.Builder parameters = PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "braintree")
                .addParameter("braintree:merchantId", fragment.getConfiguration().getMerchantId())
                .addParameter("braintree:authorizationFingerprint",
                        fragment.getConfiguration().getAndroidPay().getGoogleAuthorizationFingerprint())
                .addParameter("braintree:apiVersion", "v1")
                .addParameter("braintree:sdkVersion", BuildConfig.VERSION_NAME)
                .addParameter("braintree:metadata", new MetadataBuilder()
                        .integration(fragment.getIntegrationType())
                        .sessionId(fragment.getSessionId())
                        .version()
                        .toString());

        if (fragment.getAuthorization() instanceof TokenizationKey) {
            parameters.addParameter("braintree:clientKey", fragment.getAuthorization().getBearer());
        }

        return parameters.build();
    }

    static ArrayList<Integer> getAllowedCardNetworks(BraintreeFragment fragment) {
        ArrayList<Integer> allowedNetworks = new ArrayList<>();
        for (String network : fragment.getConfiguration().getAndroidPay().getSupportedNetworks()) {
            switch (network) {
                case VISA_NETWORK:
                    allowedNetworks.add(WalletConstants.CARD_NETWORK_VISA);
                    break;
                case MASTERCARD_NETWORK:
                    allowedNetworks.add(WalletConstants.CARD_NETWORK_MASTERCARD);
                    break;
                case AMEX_NETWORK:
                    allowedNetworks.add(WalletConstants.CARD_NETWORK_AMEX);
                    break;
                case DISCOVER_NETWORK:
                    allowedNetworks.add(WalletConstants.CARD_NETWORK_DISCOVER);
                    break;
            }
        }

        return allowedNetworks;
    }

    private static boolean validateManifest(Context context) {
        ActivityInfo activityInfo = ManifestValidator.getActivityInfo(context, GooglePaymentActivity.class);
        return activityInfo != null && activityInfo.getThemeResource() == R.style.bt_transparent_activity;
    }
}
