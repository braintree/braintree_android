package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.googlepayment.R;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Used to create and tokenize Google Payments payment methods. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/pay-with-google/overview">documentation</a>
 */
public class GooglePaymentClient {

    protected static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.EXTRA_ENVIRONMENT";
    protected static final String EXTRA_PAYMENT_DATA_REQUEST = "com.braintreepayments.api.EXTRA_PAYMENT_DATA_REQUEST";

    private static final String VISA_NETWORK = "visa";
    private static final String MASTERCARD_NETWORK = "mastercard";
    private static final String AMEX_NETWORK = "amex";
    private static final String DISCOVER_NETWORK = "discover";

    private static final String CARD_PAYMENT_TYPE = "CARD";
    private static final String PAYPAL_PAYMENT_TYPE = "PAYPAL";

    private BraintreeClient braintreeClient;

    public GooglePaymentClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    /**
     * Before starting the Google Payments flow, use this method to check whether the
     * Google Payment API is supported and set up on the device. When the callback is called with
     * {@code true}, show the Google Payments button. When it is called with {@code false}, display other
     * checkout options.
     *
     * @param activity {@link FragmentActivity}
     * @param callback Instance of {@link GooglePaymentIsReadyToPayCallback} to receive the
     *                 isReadyToPay result.
     */
    public void isReadyToPay(final FragmentActivity activity, final GooglePaymentIsReadyToPayCallback callback) {
        isReadyToPay(activity, null, callback);
    }

    /**
     * Before starting the Google Payments flow, use this method to check whether the
     * Google Payment API is supported and set up on the device. When the callback is called with
     * {@code true}, show the Google Payments button. When it is called with {@code false}, display other
     * checkout options.
     *
     * @param activity {@link FragmentActivity}
     * @param request  {@link ReadyForGooglePaymentRequest}
     * @param callback Instance of {@link GooglePaymentIsReadyToPayCallback} to receive the
     *                 isReadyToPay result.
     */
    public void isReadyToPay(final FragmentActivity activity, final ReadyForGooglePaymentRequest request, final GooglePaymentIsReadyToPayCallback callback) {

        try {
            Class.forName(PaymentsClient.class.getName());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            callback.onResult(false, null);
            return;
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception e) {
                if (!configuration.getGooglePayment().isEnabled()) {
                    callback.onResult(false, null);
                    return;
                }

                if (activity == null) {
                    callback.onResult(false, new GoogleApiClientException(GoogleApiClientException.ErrorType.NotAttachedToActivity, 1));
                    return;
                }

                PaymentsClient paymentsClient = Wallet.getPaymentsClient(activity,
                        new Wallet.WalletOptions.Builder()
                                .setEnvironment(getEnvironment(configuration.getGooglePayment()))
                                .build());

                JSONObject json = new JSONObject();
                JSONArray allowedCardNetworks = buildCardNetworks(configuration);

                try {
                    json
                            .put("apiVersion", 2)
                            .put("apiVersionMinor", 0)
                            .put("allowedPaymentMethods", new JSONArray()
                                    .put(new JSONObject()
                                            .put("type", "CARD")
                                            .put("parameters", new JSONObject()
                                                    .put("allowedAuthMethods", new JSONArray()
                                                            .put("PAN_ONLY")
                                                            .put("CRYPTOGRAM_3DS"))
                                                    .put("allowedCardNetworks", allowedCardNetworks))));

                    if (request != null) {
                        json.put("existingPaymentMethodRequired", request.isExistingPaymentMethodRequired());
                    }

                } catch (JSONException ignored) {
                }
                IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(json.toString());

                paymentsClient.isReadyToPay(request).addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        try {
                            callback.onResult(task.getResult(ApiException.class), null);
                        } catch (ApiException e) {
                            callback.onResult(false, e);
                        }
                    }
                });
            }
        });
    }

    /**
     * Get Braintree specific tokenization parameters for a Google Payment. Useful for when full control over the
     * {@link PaymentDataRequest} is required.
     * <p>
     * {@link PaymentMethodTokenizationParameters} should be supplied to the
     * {@link PaymentDataRequest} via
     * {@link PaymentDataRequest.Builder#setPaymentMethodTokenizationParameters(PaymentMethodTokenizationParameters)}
     * and {@link Collection <Integer>} allowedCardNetworks should be supplied to the
     * {@link CardRequirements} via
     * {@link CardRequirements.Builder#addAllowedCardNetworks(Collection)}}.
     *
     * @param activity {@link FragmentActivity}
     * @param callback Instance of {@link GooglePaymentGetTokenizationParametersCallback} to receive the
     *                 {@link PaymentMethodTokenizationParameters}.
     */
    public void getTokenizationParameters(final FragmentActivity activity, final GooglePaymentGetTokenizationParametersCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception e) {
                callback.onResult(getTokenizationParameters(activity, configuration), getAllowedCardNetworks(configuration));
            }
        });
    }

    /**
     * Launch a Google Payments request. This method will show the payment instrument chooser to the user.
     *
     * @param activity {@link FragmentActivity}
     * @param request  The {@link GooglePaymentRequest} containing options for the transaction.
     * @param callback Instance of {@link GooglePaymentRequestPaymentCallback} to receive the result.
     */
    public void requestPayment(final FragmentActivity activity, final GooglePaymentRequest request, final GooglePaymentRequestPaymentCallback callback) {
        braintreeClient.sendAnalyticsEvent("google-payment.selected");

        if (!validateManifest(activity)) {
            callback.onResult(false, new BraintreeException("GooglePaymentActivity was not found in the Android " +
                    "manifest, or did not have a theme of R.style.bt_transparent_activity"));
            braintreeClient.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        if (request == null) {
            callback.onResult(false, new BraintreeException("Cannot pass null GooglePaymentRequest to requestPayment"));
            braintreeClient.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        if (request.getTransactionInfo() == null) {
            callback.onResult(false, new BraintreeException("Cannot pass null TransactionInfo to requestPayment"));
            braintreeClient.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception e) {
                if (!configuration.getGooglePayment().isEnabled()) {
                    callback.onResult(false, new BraintreeException("Google Pay is not enabled for your Braintree account," +
                            " or Google Play Services are not configured correctly."));
                    return;
                }

                setGooglePaymentRequestDefaults(activity, configuration, request);

                braintreeClient.sendAnalyticsEvent("google-payment.started");

                PaymentDataRequest paymentDataRequest = PaymentDataRequest.fromJson(request.toJson());
                Intent intent = new Intent(activity, GooglePaymentActivity.class)
                        .putExtra(EXTRA_ENVIRONMENT, getEnvironment(configuration.getGooglePayment()))
                        .putExtra(EXTRA_PAYMENT_DATA_REQUEST, paymentDataRequest);

                activity.startActivityForResult(intent, BraintreeRequestCodes.GOOGLE_PAYMENT);
            }
        });

    }

    /**
     * Call this method when you've received a successful {@link PaymentData} response in your
     * activity or fragment's {@code onActivityResult} method to get a {@link GooglePaymentCardNonce}
     * or {@link PayPalAccountNonce}.
     *
     * @param activity    {@link FragmentActivity}
     * @param paymentData {@link PaymentData} from the Intent in {@code onActivityResult} method.
     * @param callback    Instance of {@link GooglePaymentOnActivityResultCallback} to receive the result.
     */
    public void tokenize(FragmentActivity activity, PaymentData paymentData, GooglePaymentOnActivityResultCallback callback) {
        try {
            callback.onResult(PaymentMethodNonceFactory.fromString(paymentData.toJson()), null);
            braintreeClient.sendAnalyticsEvent("google-payment.nonce-received");
        } catch (JSONException | NullPointerException e) {
            braintreeClient.sendAnalyticsEvent("google-payment.failed");

            try {
                String token = new JSONObject(paymentData.toJson())
                        .getJSONObject("paymentMethodData")
                        .getJSONObject("tokenizationData")
                        .getString("token");
                callback.onResult(null, ErrorWithResponse.fromJson(token));
            } catch (JSONException | NullPointerException e1) {
                callback.onResult(null, e1);
            }
        }
    }

    void onActivityResult(FragmentActivity activity, int resultCode, Intent data, final GooglePaymentOnActivityResultCallback callback) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            braintreeClient.sendAnalyticsEvent("google-payment.authorized");
            tokenize(activity, PaymentData.getFromIntent(data), callback);
        } else if (resultCode == AutoResolveHelper.RESULT_ERROR) {
            braintreeClient.sendAnalyticsEvent("google-payment.failed");

            callback.onResult(null, new GooglePaymentException("An error was encountered during the Google Payments " +
                    "flow. See the status object in this exception for more details.",
                    AutoResolveHelper.getStatusFromIntent(data)));
        } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            braintreeClient.sendAnalyticsEvent("google-payment.canceled");
        }
    }

    int getEnvironment(GooglePaymentConfiguration configuration) {
        if ("production".equals(configuration.getEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }

    PaymentMethodTokenizationParameters getTokenizationParameters(FragmentActivity activity, Configuration configuration) {
        String version;

        JSONObject metadata = new MetadataBuilder()
                .integration(braintreeClient.getIntegrationType())
                .sessionId(braintreeClient.getSessionId())
                .version()
                .build();

        try {
            version = metadata.getString("version");
        } catch (JSONException e) {
            version = com.braintreepayments.api.BuildConfig.VERSION_NAME;
        }

        PaymentMethodTokenizationParameters.Builder parameters = PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "braintree")
                .addParameter("braintree:merchantId", configuration.getMerchantId())
                .addParameter("braintree:authorizationFingerprint", configuration.getGooglePayment().getGoogleAuthorizationFingerprint())
                .addParameter("braintree:apiVersion", "v1")
                .addParameter("braintree:sdkVersion", version)
                .addParameter("braintree:metadata", metadata.toString());

        if (braintreeClient.getAuthorization() instanceof TokenizationKey) {
            parameters.addParameter("braintree:clientKey", braintreeClient.getAuthorization().getBearer());
        }

        return parameters.build();
    }

    ArrayList<Integer> getAllowedCardNetworks(Configuration configuration) {
        ArrayList<Integer> allowedNetworks = new ArrayList<>();
        for (String network : configuration.getGooglePayment().getSupportedNetworks()) {
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
                default:
                    break;
            }
        }

        return allowedNetworks;
    }

    private JSONArray buildCardNetworks(Configuration configuration) {
        JSONArray cardNetworkStrings = new JSONArray();

        for (int network : getAllowedCardNetworks(configuration)) {
            switch (network) {
                case WalletConstants.CARD_NETWORK_AMEX:
                    cardNetworkStrings.put("AMEX");
                    break;
                case WalletConstants.CARD_NETWORK_DISCOVER:
                    cardNetworkStrings.put("DISCOVER");
                    break;
                case WalletConstants.CARD_NETWORK_JCB:
                    cardNetworkStrings.put("JCB");
                    break;
                case WalletConstants.CARD_NETWORK_MASTERCARD:
                    cardNetworkStrings.put("MASTERCARD");
                    break;
                case WalletConstants.CARD_NETWORK_VISA:
                    cardNetworkStrings.put("VISA");
                    break;
            }
        }
        return cardNetworkStrings;
    }

    private JSONObject buildCardPaymentMethodParameters(GooglePaymentRequest request, Configuration configuration) {
        JSONObject defaultParameters = new JSONObject();

        try {
            if (request.getAllowedCardNetworksForType(CARD_PAYMENT_TYPE) == null) {
                JSONArray cardNetworkStrings = buildCardNetworks(configuration);

                if (request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE) == null) {
                    request.setAllowedAuthMethods(CARD_PAYMENT_TYPE,
                            new JSONArray()
                                    .put("PAN_ONLY")
                                    .put("CRYPTOGRAM_3DS"));
                } else {
                    request.setAllowedAuthMethods(CARD_PAYMENT_TYPE,
                            request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE));
                }

                request.setAllowedCardNetworks(CARD_PAYMENT_TYPE, cardNetworkStrings);
            }

            defaultParameters
                    .put("billingAddressRequired", request.isBillingAddressRequired())
                    .put("allowPrepaidCards", request.getAllowPrepaidCards())
                    .put("allowedAuthMethods",
                            request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE))
                    .put("allowedCardNetworks",
                            request.getAllowedCardNetworksForType(CARD_PAYMENT_TYPE));

            if (request.isBillingAddressRequired()) {
                defaultParameters
                        .put("billingAddressParameters", new JSONObject()
                                .put("format", request.billingAddressFormatToString())
                                .put("phoneNumberRequired", request.isPhoneNumberRequired()));
            }
        } catch (JSONException ignored) {
        }
        return defaultParameters;
    }

    private JSONObject buildPayPalPaymentMethodParameters(Configuration configuration) {
        JSONObject defaultParameters = new JSONObject();

        try {
            JSONObject purchaseContext = new JSONObject()
                    .put("purchase_units", new JSONArray()
                            .put(new JSONObject()
                                    .put("payee", new JSONObject()
                                            .put("client_id", configuration.getGooglePayment().getPaypalClientId())
                                    )
                                    .put("recurring_payment", "true")
                            )
                    );

            defaultParameters.put("purchase_context", purchaseContext);
        } catch (JSONException ignored) {
        }

        return defaultParameters;

    }

    private JSONObject buildCardTokenizationSpecification(FragmentActivity activity, Configuration configuration) {
        JSONObject cardJson = new JSONObject();
        JSONObject parameters = new JSONObject();
        String googlePaymentVersion = com.braintreepayments.api.googlepayment.BuildConfig.VERSION_NAME;

        try {
            parameters
                    .put("gateway", "braintree")
                    .put("braintree:apiVersion", "v1")
                    .put("braintree:sdkVersion", googlePaymentVersion)
                    .put("braintree:merchantId", configuration.getMerchantId())
                    .put("braintree:metadata", (new JSONObject()
                            .put("source", "client")
                            .put("integration", braintreeClient.getIntegrationType())
                            .put("sessionId", braintreeClient.getSessionId())
                            .put("version", googlePaymentVersion)
                            .put("platform", "android")).toString());

            if (braintreeClient.getAuthorization() instanceof TokenizationKey) {
                parameters
                        .put("braintree:clientKey", braintreeClient.getAuthorization().toString());
            } else {
                parameters
                        .put("braintree:authorizationFingerprint", configuration
                                .getGooglePayment()
                                .getGoogleAuthorizationFingerprint());
            }
        } catch (JSONException ignored) {
        }

        try {
            cardJson
                    .put("type", "PAYMENT_GATEWAY")
                    .put("parameters", parameters);
        } catch (JSONException ignored) {
        }

        return cardJson;
    }

    private JSONObject buildPayPalTokenizationSpecification(FragmentActivity activity, Configuration configuration) {
        JSONObject json = new JSONObject();
        String googlePaymentVersion = com.braintreepayments.api.googlepayment.BuildConfig.VERSION_NAME;

        try {
            json.put("type", "PAYMENT_GATEWAY")
                    .put("parameters", new JSONObject()
                            .put("gateway", "braintree")
                            .put("braintree:apiVersion", "v1")
                            .put("braintree:sdkVersion", googlePaymentVersion)
                            .put("braintree:merchantId", configuration.getMerchantId())
                            .put("braintree:paypalClientId", configuration.getGooglePayment().getPaypalClientId())
                            .put("braintree:metadata", (new JSONObject()
                                    .put("source", "client")
                                    .put("integration", braintreeClient.getIntegrationType())
                                    .put("sessionId", braintreeClient.getSessionId())
                                    .put("version", googlePaymentVersion)
                                    .put("platform", "android")).toString()));
        } catch (JSONException ignored) {
        }

        return json;
    }

    private void setGooglePaymentRequestDefaults(FragmentActivity activity, Configuration configuration,
                                                 GooglePaymentRequest request) {
        if (request.isEmailRequired() == null) {
            request.emailRequired(false);
        }

        if (request.isPhoneNumberRequired() == null) {
            request.phoneNumberRequired(false);
        }

        if (request.isBillingAddressRequired() == null) {
            request.billingAddressRequired(false);
        }

        if (request.isBillingAddressRequired() &&
                request.getBillingAddressFormat() == null) {
            request.billingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_MIN);
        }

        if (request.isShippingAddressRequired() == null) {
            request.shippingAddressRequired(false);
        }

        if (request.getAllowPrepaidCards() == null) {
            request.allowPrepaidCards(true);
        }

        if (request.getAllowedPaymentMethod(CARD_PAYMENT_TYPE) == null) {
            request.setAllowedPaymentMethod(CARD_PAYMENT_TYPE,
                    buildCardPaymentMethodParameters(request, configuration));
        }

        if (request.getTokenizationSpecificationForType(CARD_PAYMENT_TYPE) == null) {
            request.setTokenizationSpecificationForType("CARD",
                    buildCardTokenizationSpecification(activity, configuration));
        }

        boolean googlePaymentCanProcessPayPal = request.isPayPalEnabled() &&
                !TextUtils.isEmpty(configuration.getGooglePayment().getPaypalClientId());

        if (googlePaymentCanProcessPayPal) {
            if (request.getAllowedPaymentMethod("PAYPAL") == null) {
                request.setAllowedPaymentMethod(PAYPAL_PAYMENT_TYPE,
                        buildPayPalPaymentMethodParameters(configuration));
            }


            if (request.getTokenizationSpecificationForType(PAYPAL_PAYMENT_TYPE) == null) {
                request.setTokenizationSpecificationForType("PAYPAL",
                        buildPayPalTokenizationSpecification(activity, configuration));
            }
        }

        request.environment(configuration.getGooglePayment().getEnvironment());
    }

    private boolean validateManifest(Context context) {
        ActivityInfo activityInfo = braintreeClient.getManifestActivityInfo(GooglePaymentActivity.class);
        return activityInfo != null && activityInfo.getThemeResource() == R.style.bt_transparent_activity;
    }
}