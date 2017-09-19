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
import com.braintreepayments.api.exceptions.GooglePaymentsException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentsCardNonce;
import com.braintreepayments.api.models.GooglePaymentsRequest;
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

import static com.braintreepayments.api.GooglePaymentsActivity.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePaymentsActivity.EXTRA_PAYMENT_DATA_REQUEST;

/**
 * Used to create and tokenize Google Payments payment methods.
 */
public class GooglePayments {

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
     * Launch a Google Payments request. This method will show the payment instrument chooser to the user.
     *
     * @param fragment The current {@link BraintreeFragment}.
     * @param request The {@link GooglePaymentsRequest} containing options for the transaction.
     */
    public static void requestPayment(final BraintreeFragment fragment, final @NonNull GooglePaymentsRequest request) {
        fragment.sendAnalyticsEvent("google-payments.selected");

        if (!validateManifest(fragment.getApplicationContext())) {
            fragment.postCallback(new BraintreeException("GooglePaymentsActivity was not found in the Android " +
                    "manifest, or did not have a theme of R.style.bt_transparent_activity"));
            fragment.sendAnalyticsEvent("google-payments.failed");
            return;
        }

        if (request == null || request.getTransactionInfo() == null) {
            fragment.postCallback(new BraintreeException("Cannot pass null TransactionInfo to requestPayment"));
            fragment.sendAnalyticsEvent("google-payments.failed");
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
                    paymentDataRequest.setEmailRequired(request.isPhoneNumberRequired());
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

                fragment.sendAnalyticsEvent("google-payments.started");

                Intent intent = new Intent(fragment.getApplicationContext(), GooglePaymentsActivity.class)
                        .putExtra(EXTRA_ENVIRONMENT, getEnvironment(configuration.getAndroidPay()))
                        .putExtra(EXTRA_PAYMENT_DATA_REQUEST, paymentDataRequest.build());
                fragment.startActivityForResult(intent, BraintreeRequestCodes.GOOGLE_PAYMENTS);
            }
        });
    }

    static void onActivityResult(BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            fragment.sendAnalyticsEvent("google-payments.authorized");

            try {
                fragment.postCallback(GooglePaymentsCardNonce.fromPaymentData(PaymentData.getFromIntent(data)));
                fragment.sendAnalyticsEvent("google-payments.nonce-received");
            } catch (JSONException | NullPointerException e) {
                fragment.sendAnalyticsEvent("google-payments.failed");

                try {
                    fragment.postCallback(ErrorWithResponse.fromJson(PaymentData.getFromIntent(data)
                            .getPaymentMethodToken().getToken()));
                } catch (JSONException | NullPointerException e1) {
                    fragment.postCallback(e1);
                }
            }
        } else if (resultCode == AutoResolveHelper.RESULT_ERROR) {
            fragment.sendAnalyticsEvent("google-payments.failed");

            fragment.postCallback(new GooglePaymentsException("An error was encountered during the Google Payments " +
                    "flow. See the status object in this exception for more details.",
                    AutoResolveHelper.getStatusFromIntent(data)));
        } else if (resultCode == Activity.RESULT_CANCELED) {
            fragment.sendAnalyticsEvent("google-payments.canceled");
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
            parameters.addParameter("braintree:clientKey", fragment.getAuthorization().toString());
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
        ActivityInfo activityInfo = ManifestValidator.getActivityInfo(context, GooglePaymentsActivity.class);
        return activityInfo != null && activityInfo.getThemeResource() == R.style.bt_transparent_activity;
    }
}
