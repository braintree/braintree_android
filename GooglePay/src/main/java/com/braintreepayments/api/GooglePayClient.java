package com.braintreepayments.api;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.googlepay.R;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Used to create and tokenize Google Pay payment methods. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/pay-with-google/overview">documentation</a>
 */
public class GooglePayClient {

    protected static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.EXTRA_ENVIRONMENT";
    protected static final String EXTRA_PAYMENT_DATA_REQUEST = "com.braintreepayments.api.EXTRA_PAYMENT_DATA_REQUEST";

    private static final String VISA_NETWORK = "visa";
    private static final String MASTERCARD_NETWORK = "mastercard";
    private static final String AMEX_NETWORK = "amex";
    private static final String DISCOVER_NETWORK = "discover";
    private static final String ELO_NETWORK = "elo";

    private static final String CARD_PAYMENT_TYPE = "CARD";
    private static final String PAYPAL_PAYMENT_TYPE = "PAYPAL";

    private final BraintreeClient braintreeClient;
    private final GooglePayInternalClient internalGooglePayClient;

    public GooglePayClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new GooglePayInternalClient());
    }

    @VisibleForTesting
    GooglePayClient(BraintreeClient braintreeClient, GooglePayInternalClient internalGooglePayClient) {
        this.braintreeClient = braintreeClient;
        this.internalGooglePayClient = internalGooglePayClient;
    }

    /**
     * Before starting the Google Pay flow, use this method to check whether the
     * Google Pay API is supported and set up on the device. When the callback is called with
     * {@code true}, show the Google Pay button. When it is called with {@code false}, display other
     * checkout options.
     *
     * @param activity Android FragmentActivity
     * @param callback {@link GooglePayIsReadyToPayCallback}
     */
    public void isReadyToPay(final FragmentActivity activity, final GooglePayIsReadyToPayCallback callback) {
        isReadyToPay(activity, null, callback);
    }

    /**
     * Before starting the Google Pay flow, use this method to check whether the
     * Google Pay API is supported and set up on the device. When the callback is called with
     * {@code true}, show the Google Pay button. When it is called with {@code false}, display other
     * checkout options.
     *
     * @param activity Android FragmentActivity
     * @param request  {@link ReadyForGooglePayRequest}
     * @param callback {@link GooglePayIsReadyToPayCallback}
     */
    public void isReadyToPay(final FragmentActivity activity, final ReadyForGooglePayRequest request, final GooglePayIsReadyToPayCallback callback) {

        try {
            Class.forName(PaymentsClient.class.getName());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            callback.onResult(false, null);
            return;
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception e) {
                if (!configuration.isGooglePayEnabled()) {
                    callback.onResult(false, null);
                    return;
                }

                if (activity == null) {
                    callback.onResult(false, new IllegalArgumentException("Activity cannot be null."));
                    return;
                }

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
                internalGooglePayClient.isReadyToPay(activity, configuration, request, callback);
            }
        });
    }

    /**
     * Get Braintree specific tokenization parameters for a Google Pay. Useful for when full control over the
     * {@link PaymentDataRequest} is required.
     * <p>
     * {@link PaymentMethodTokenizationParameters} should be supplied to the {@link PaymentDataRequest} via
     * {@link PaymentDataRequest.Builder#setPaymentMethodTokenizationParameters(PaymentMethodTokenizationParameters)}
     * and {@link Collection <Integer>} allowedCardNetworks should be supplied to the {@link CardRequirements} via
     * {@link CardRequirements.Builder#addAllowedCardNetworks(Collection)}}.
     *
     * @param callback {@link GooglePayGetTokenizationParametersCallback}
     */
    public void getTokenizationParameters(final GooglePayGetTokenizationParametersCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception e) {
                callback.onResult(getTokenizationParameters(configuration), getAllowedCardNetworks(configuration));
            }
        });
    }

    /**
     * Launch a Google Pay request. This method will show the payment instrument chooser to the user.
     *
     * @param activity Android FragmentActivity
     * @param request The {@link GooglePayRequest} containing options for the transaction.
     * @param callback {@link GooglePayRequestPaymentCallback}
     */
    public void requestPayment(final FragmentActivity activity, final GooglePayRequest request, final GooglePayRequestPaymentCallback callback) {
        braintreeClient.sendAnalyticsEvent("google-payment.selected");

        if (!validateManifest()) {
            callback.onResult(new BraintreeException("GooglePayActivity was not found in the Android " +
                    "manifest, or did not have a theme of R.style.bt_transparent_activity"));
            braintreeClient.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        if (request == null) {
            callback.onResult(new BraintreeException("Cannot pass null GooglePayRequest to requestPayment"));
            braintreeClient.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        if (request.getTransactionInfo() == null) {
            callback.onResult(new BraintreeException("Cannot pass null TransactionInfo to requestPayment"));
            braintreeClient.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception e) {
                if (!configuration.isGooglePayEnabled()) {
                    callback.onResult(new BraintreeException("Google Pay is not enabled for your Braintree account," +
                            " or Google Play Services are not configured correctly."));
                    return;
                }

                setGooglePayRequestDefaults(configuration, request);

                braintreeClient.sendAnalyticsEvent("google-payment.started");

                PaymentDataRequest paymentDataRequest = PaymentDataRequest.fromJson(request.toJson());
                Intent intent = new Intent(activity, GooglePayActivity.class)
                        .putExtra(EXTRA_ENVIRONMENT, getGooglePayEnvironment(configuration))
                        .putExtra(EXTRA_PAYMENT_DATA_REQUEST, paymentDataRequest);

                activity.startActivityForResult(intent, BraintreeRequestCodes.GOOGLE_PAY);
            }
        });

    }

    /**
     * This method is called when you've received a successful {@link PaymentData} response in
     * {@link GooglePayClient#onActivityResult(int, Intent, GooglePayOnActivityResultCallback)}
     * to get a {@link GooglePayCardNonce} or {@link PayPalAccountNonce}.
     *
     * @param paymentData {@link PaymentData} from the Intent in
     * {@link GooglePayClient#onActivityResult(int, Intent, GooglePayOnActivityResultCallback)} method.
     * @param callback {@link GooglePayOnActivityResultCallback}
     */
    void tokenize(PaymentData paymentData, GooglePayOnActivityResultCallback callback) {
        try {
            JSONObject result = new JSONObject(paymentData.toJson());
            callback.onResult(GooglePayCardNonce.fromJSON(result), null);
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

    /**
     * @param resultCode a code associated with the Activity result
     * @param data Android Intent
     * @param callback {@link GooglePayOnActivityResultCallback}
     */
    public void onActivityResult(int resultCode, Intent data, final GooglePayOnActivityResultCallback callback) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            braintreeClient.sendAnalyticsEvent("google-payment.authorized");
            tokenize(PaymentData.getFromIntent(data), callback);
        } else if (resultCode == AutoResolveHelper.RESULT_ERROR) {
            braintreeClient.sendAnalyticsEvent("google-payment.failed");

            callback.onResult(null, new GooglePayException("An error was encountered during the Google Pay " +
                    "flow. See the status object in this exception for more details.",
                    AutoResolveHelper.getStatusFromIntent(data)));
        } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            braintreeClient.sendAnalyticsEvent("google-payment.canceled");
            callback.onResult(null, new UserCanceledException("User canceled Google Pay."));
        }
    }

    int getGooglePayEnvironment(Configuration configuration) {
        if ("production".equals(configuration.getGooglePayEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }

    PaymentMethodTokenizationParameters getTokenizationParameters(Configuration configuration) {
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
                .addParameter("braintree:authorizationFingerprint", configuration.getGooglePayAuthorizationFingerprint())
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
        for (String network : configuration.getGooglePaySupportedNetworks()) {
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
                case ELO_NETWORK:
                    allowedNetworks.add(BraintreeGooglePayWalletConstants.CARD_NETWORK_ELO);
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
                case BraintreeGooglePayWalletConstants.CARD_NETWORK_ELO:
                    cardNetworkStrings.put("ELO");
                    cardNetworkStrings.put("ELO_DEBIT");
                    break;
            }
        }
        return cardNetworkStrings;
    }

    private JSONObject buildCardPaymentMethodParameters(Configuration configuration, GooglePayRequest request) {
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
                                            .put("client_id", configuration.getGooglePayPayPalClientId())
                                    )
                                    .put("recurring_payment", "true")
                            )
                    );

            defaultParameters.put("purchase_context", purchaseContext);
        } catch (JSONException ignored) {
        }

        return defaultParameters;

    }

    private JSONObject buildCardTokenizationSpecification(Configuration configuration) {
        JSONObject cardJson = new JSONObject();
        JSONObject parameters = new JSONObject();
        String googlePayVersion = com.braintreepayments.api.googlepay.BuildConfig.VERSION_NAME;

        try {
            parameters
                    .put("gateway", "braintree")
                    .put("braintree:apiVersion", "v1")
                    .put("braintree:sdkVersion", googlePayVersion)
                    .put("braintree:merchantId", configuration.getMerchantId())
                    .put("braintree:metadata", (new JSONObject()
                            .put("source", "client")
                            .put("integration", braintreeClient.getIntegrationType())
                            .put("sessionId", braintreeClient.getSessionId())
                            .put("version", googlePayVersion)
                            .put("platform", "android")).toString());

            if (braintreeClient.getAuthorization() instanceof TokenizationKey) {
                parameters
                        .put("braintree:clientKey", braintreeClient.getAuthorization().toString());
            } else {
                String googlePayAuthFingerprint = configuration.getGooglePayAuthorizationFingerprint();
                parameters
                        .put("braintree:authorizationFingerprint", googlePayAuthFingerprint);
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

    private JSONObject buildPayPalTokenizationSpecification(Configuration configuration) {
        JSONObject json = new JSONObject();
        String googlePayVersion = com.braintreepayments.api.googlepay.BuildConfig.VERSION_NAME;

        try {
            json.put("type", "PAYMENT_GATEWAY")
                    .put("parameters", new JSONObject()
                            .put("gateway", "braintree")
                            .put("braintree:apiVersion", "v1")
                            .put("braintree:sdkVersion", googlePayVersion)
                            .put("braintree:merchantId", configuration.getMerchantId())
                            .put("braintree:paypalClientId", configuration.getGooglePayPayPalClientId())
                            .put("braintree:metadata", (new JSONObject()
                                    .put("source", "client")
                                    .put("integration", braintreeClient.getIntegrationType())
                                    .put("sessionId", braintreeClient.getSessionId())
                                    .put("version", googlePayVersion)
                                    .put("platform", "android")).toString()));
        } catch (JSONException ignored) {
        }

        return json;
    }

    private void setGooglePayRequestDefaults(Configuration configuration, GooglePayRequest request) {

        if (request.getAllowedPaymentMethod(CARD_PAYMENT_TYPE) == null) {
            request.setAllowedPaymentMethod(CARD_PAYMENT_TYPE,
                    buildCardPaymentMethodParameters(configuration, request));
        }

        if (request.getTokenizationSpecificationForType(CARD_PAYMENT_TYPE) == null) {
            request.setTokenizationSpecificationForType("CARD",
                    buildCardTokenizationSpecification(configuration));
        }

        boolean googlePayCanProcessPayPal = request.isPayPalEnabled() &&
                !TextUtils.isEmpty(configuration.getGooglePayPayPalClientId());

        if (googlePayCanProcessPayPal) {
            if (request.getAllowedPaymentMethod("PAYPAL") == null) {
                request.setAllowedPaymentMethod(PAYPAL_PAYMENT_TYPE,
                        buildPayPalPaymentMethodParameters(configuration));
            }


            if (request.getTokenizationSpecificationForType(PAYPAL_PAYMENT_TYPE) == null) {
                request.setTokenizationSpecificationForType("PAYPAL",
                        buildPayPalTokenizationSpecification(configuration));
            }
        }

        request.setEnvironment(configuration.getGooglePayEnvironment());
    }

    private boolean validateManifest() {
        ActivityInfo activityInfo = braintreeClient.getManifestActivityInfo(GooglePayActivity.class);
        return activityInfo != null && activityInfo.getThemeResource() == R.style.bt_transparent_activity;
    }
}