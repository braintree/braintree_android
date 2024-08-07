package com.braintreepayments.api.googlepay;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.paypal.PayPalAccountNonce;
import com.braintreepayments.api.core.Authorization;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.ErrorWithResponse;
import com.braintreepayments.api.core.MetadataBuilder;
import com.braintreepayments.api.core.PaymentMethodNonce;
import com.braintreepayments.api.core.TokenizationKey;
import com.braintreepayments.api.core.UserCanceledException;
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
 * Used to create and tokenize Google Pay payment methods. For more information see the <a
 * href="https://developer.paypal.com/braintree/docs/guides/google-pay/overview">documentation</a>
 */
public class GooglePayClient {

    protected static final String EXTRA_ENVIRONMENT = "com.braintreepayments.api.EXTRA_ENVIRONMENT";
    protected static final String EXTRA_PAYMENT_DATA_REQUEST =
            "com.braintreepayments.api.EXTRA_PAYMENT_DATA_REQUEST";

    private static final String VISA_NETWORK = "visa";
    private static final String MASTERCARD_NETWORK = "mastercard";
    private static final String AMEX_NETWORK = "amex";
    private static final String DISCOVER_NETWORK = "discover";
    private static final String ELO_NETWORK = "elo";

    private static final String CARD_PAYMENT_TYPE = "CARD";
    private static final String PAYPAL_PAYMENT_TYPE = "PAYPAL";

    private final BraintreeClient braintreeClient;
    private final GooglePayInternalClient internalGooglePayClient;

    /**
     * Initializes a new {@link GooglePayClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public GooglePayClient(@NonNull Context context, @NonNull String authorization) {
        this(new BraintreeClient(context, authorization));
    }

    @VisibleForTesting
    GooglePayClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new GooglePayInternalClient());
    }

    @VisibleForTesting
    GooglePayClient(BraintreeClient braintreeClient,
                    GooglePayInternalClient internalGooglePayClient) {
        this.braintreeClient = braintreeClient;
        this.internalGooglePayClient = internalGooglePayClient;
    }

    /**
     * Before starting the Google Pay flow, use this method to check whether the Google Pay API is
     * supported and set up on the device. When the callback is called with {@code true}, show the
     * Google Pay button. When it is called with {@code false}, display other checkout options.
     *
     * @param context  Android Context
     * @param callback {@link GooglePayIsReadyToPayCallback}
     */
    public void isReadyToPay(@NonNull final Context context,
                             @NonNull final GooglePayIsReadyToPayCallback callback) {
        isReadyToPay(context, null, callback);
    }

    /**
     * Before starting the Google Pay flow, use this method to check whether the Google Pay API is
     * supported and set up on the device. When the callback is called with {@code true}, show the
     * Google Pay button. When it is called with {@code false}, display other checkout options.
     *
     * @param context  Android Context
     * @param request  {@link ReadyForGooglePayRequest}
     * @param callback {@link GooglePayIsReadyToPayCallback}
     */
    public void isReadyToPay(@NonNull final Context context,
                             @Nullable final ReadyForGooglePayRequest request,
                             @NonNull final GooglePayIsReadyToPayCallback callback) {
        try {
            Class.forName(PaymentsClient.class.getName());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            callback.onGooglePayReadinessResult(new GooglePayReadinessResult.NotReadyToPay(null));
            return;
        }

        braintreeClient.getConfiguration((configuration, e) -> {
            if (configuration == null) {
                callback.onGooglePayReadinessResult(new GooglePayReadinessResult.NotReadyToPay(e));
                return;
            }

            if (!configuration.isGooglePayEnabled()) {
                callback.onGooglePayReadinessResult(new GooglePayReadinessResult.NotReadyToPay(null));
                return;
            }

            //noinspection ConstantConditions
            if (context == null) {
                callback.onGooglePayReadinessResult(new GooglePayReadinessResult.NotReadyToPay(new IllegalArgumentException("Activity cannot be null.")));
                return;
            }

            JSONObject json = new JSONObject();
            JSONArray allowedCardNetworks = buildCardNetworks(configuration);

            try {
                json.put("apiVersion", 2).put("apiVersionMinor", 0).put("allowedPaymentMethods",
                        new JSONArray().put(new JSONObject().put("type", "CARD")
                                .put("parameters", new JSONObject().put("allowedAuthMethods",
                                                new JSONArray().put("PAN_ONLY").put("CRYPTOGRAM_3DS"))
                                        .put("allowedCardNetworks", allowedCardNetworks))));

                if (request != null) {
                    json.put("existingPaymentMethodRequired",
                            request.isExistingPaymentMethodRequired());
                }

            } catch (JSONException ignored) {
            }
            IsReadyToPayRequest request1 = IsReadyToPayRequest.fromJson(json.toString());
            internalGooglePayClient.isReadyToPay(context, configuration, request1, callback);
        });
    }

    /**
     * Get Braintree specific tokenization parameters for a Google Pay. Useful for when full control
     * over the {@link PaymentDataRequest} is required.
     * <p>
     * {@link PaymentMethodTokenizationParameters} should be supplied to the
     * {@link PaymentDataRequest} via
     * {@link
     * PaymentDataRequest.Builder#setPaymentMethodTokenizationParameters(PaymentMethodTokenizationParameters)}
     * and {@link Collection <Integer>} allowedCardNetworks should be supplied to the
     * {@link CardRequirements} via
     * {@link CardRequirements.Builder#addAllowedCardNetworks(Collection)}}.
     *
     * @param callback {@link GooglePayGetTokenizationParametersCallback}
     */
    public void getTokenizationParameters(
            @NonNull final GooglePayGetTokenizationParametersCallback callback) {
        braintreeClient.getConfiguration((configuration, e) -> {
            if (configuration == null && e != null) {
                callback.onTokenizationParametersResult(new GooglePayTokenizationParameters.Failure(e));
                return;
            }
            callback.onTokenizationParametersResult(new GooglePayTokenizationParameters.Success(
                    getTokenizationParameters(configuration, braintreeClient.getAuthorization()), getAllowedCardNetworks(configuration)));
        });

    }

    /**
     * Start the Google Pay payment flow. This will return {@link GooglePayPaymentAuthRequestParams} that are
     * used to present Google Pay payment sheet in
     * {@link GooglePayLauncher#launch(GooglePayPaymentAuthRequestParams)}
     *
     * @param request  The {@link GooglePayRequest} containing options for the transaction.
     * @param callback {@link GooglePayPaymentAuthRequestCallback}
     */
    public void createPaymentAuthRequest(@NonNull final GooglePayRequest request,
                                         @NonNull final GooglePayPaymentAuthRequestCallback callback) {
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_STARTED);

        if (!validateManifest()) {
            callbackPaymentRequestFailure(new GooglePayPaymentAuthRequest.Failure(new BraintreeException(
                    "GooglePayActivity was not found in the Android " +
                            "manifest, or did not have a theme of R.style.bt_transparent_activity")), callback);
            return;
        }

        //noinspection ConstantConditions
        if (request == null) {
            callbackPaymentRequestFailure(new GooglePayPaymentAuthRequest.Failure(new BraintreeException(
                    "Cannot pass null GooglePayRequest to requestPayment")), callback);
            return;
        }

        if (request.getTransactionInfo() == null) {
            callbackPaymentRequestFailure(new GooglePayPaymentAuthRequest.Failure(new BraintreeException(
                    "Cannot pass null TransactionInfo to requestPayment")), callback);
            return;
        }

        braintreeClient.getConfiguration((configuration, configError) -> {
            if (configuration == null && configError != null) {
                callbackPaymentRequestFailure(new GooglePayPaymentAuthRequest.Failure(configError), callback);
                return;
            }

            if (!configuration.isGooglePayEnabled()) {
                callbackPaymentRequestFailure(new GooglePayPaymentAuthRequest.Failure(new BraintreeException(
                        "Google Pay is not enabled for your Braintree account, or Google Play Services are not configured correctly.")), callback);
                return;
            }

            setGooglePayRequestDefaults(configuration, braintreeClient.getAuthorization(), request);

            PaymentDataRequest paymentDataRequest =
                    PaymentDataRequest.fromJson(request.toJson());

            GooglePayPaymentAuthRequestParams params =
                    new GooglePayPaymentAuthRequestParams(getGooglePayEnvironment(configuration),
                            paymentDataRequest);
            callbackPaymentRequestSuccess(new GooglePayPaymentAuthRequest.ReadyToLaunch(params), callback);

        });

    }

    /**
     * Call this method when you've received a successful {@link PaymentData} response from a
     * direct Google Play Services integration to get a {@link GooglePayCardNonce} or
     * {@link PayPalAccountNonce}.
     *
     * @param paymentData {@link PaymentData} retrieved from directly integrating with Google Play
     *                                       Services through {@link PaymentsClient#loadPaymentData(PaymentDataRequest)}
     * @param callback    {@link GooglePayTokenizeCallback}
     */
    void tokenize(PaymentData paymentData, GooglePayTokenizeCallback callback) {
        try {
            JSONObject result = new JSONObject(paymentData.toJson());
            callbackTokenizeSuccess(new GooglePayResult.Success(GooglePayCardNonce.fromJSON(result)), callback);
        } catch (JSONException | NullPointerException e) {
            try {
                String token =
                        new JSONObject(paymentData.toJson()).getJSONObject("paymentMethodData")
                                .getJSONObject("tokenizationData").getString("token");
                callbackTokenizeFailure(new GooglePayResult.Failure(ErrorWithResponse.fromJson(token)), callback);
            } catch (JSONException | NullPointerException e1) {
                callbackTokenizeFailure(new GooglePayResult.Failure(e1), callback);
            }
        }
    }


    /**
     * After a user successfully authorizes Google Pay payment via
     * {@link GooglePayClient#createPaymentAuthRequest(GooglePayRequest, GooglePayPaymentAuthRequestCallback)}, this
     * method should be invoked to tokenize the payment method to retrieve a
     * {@link PaymentMethodNonce}
     *
     * @param paymentAuthResult the result of {@link GooglePayLauncher#launch(GooglePayPaymentAuthRequestParams)}
     * @param callback        {@link GooglePayTokenizeCallback}
     */
    public void tokenize(GooglePayPaymentAuthResult paymentAuthResult,
                         GooglePayTokenizeCallback callback) {
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_STARTED);
        if (paymentAuthResult.getPaymentData() != null) {
            tokenize(paymentAuthResult.getPaymentData(), callback);
        } else if (paymentAuthResult.getError() != null) {
            if (paymentAuthResult.getError() instanceof UserCanceledException) {
                callbackTokenizeCancel(callback);
                return;
            }
            callbackTokenizeFailure(new GooglePayResult.Failure(paymentAuthResult.getError()), callback);
        }
    }

    int getGooglePayEnvironment(Configuration configuration) {
        if ("production".equals(configuration.getGooglePayEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }

    PaymentMethodTokenizationParameters getTokenizationParameters(Configuration configuration,
                                                                  Authorization authorization) {
        String version;

        JSONObject metadata =
                new MetadataBuilder().integration(braintreeClient.getIntegrationType())
                        .sessionId(braintreeClient.getSessionId()).version().build();

        try {
            version = metadata.getString("version");
        } catch (JSONException e) {
            version = com.braintreepayments.api.core.BuildConfig.VERSION_NAME;
        }

        PaymentMethodTokenizationParameters.Builder parameters =
                PaymentMethodTokenizationParameters.newBuilder().setPaymentMethodTokenizationType(
                                WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                        .addParameter("gateway", "braintree")
                        .addParameter("braintree:merchantId", configuration.getMerchantId())
                        .addParameter("braintree:authorizationFingerprint",
                                configuration.getGooglePayAuthorizationFingerprint())
                        .addParameter("braintree:apiVersion", "v1")
                        .addParameter("braintree:sdkVersion", version)
                        .addParameter("braintree:metadata", metadata.toString());

        if (authorization instanceof TokenizationKey) {
            parameters.addParameter("braintree:clientKey", authorization.getBearer());
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

    private JSONObject buildCardPaymentMethodParameters(Configuration configuration,
                                                        GooglePayRequest request) {
        JSONObject defaultParameters = new JSONObject();

        try {
            if (request.getAllowedCardNetworksForType(CARD_PAYMENT_TYPE) == null) {
                JSONArray cardNetworkStrings = buildCardNetworks(configuration);

                if (request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE) == null) {
                    request.setAllowedAuthMethods(CARD_PAYMENT_TYPE,
                            new JSONArray().put("PAN_ONLY").put("CRYPTOGRAM_3DS"));
                } else {
                    request.setAllowedAuthMethods(CARD_PAYMENT_TYPE,
                            request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE));
                }

                request.setAllowedCardNetworks(CARD_PAYMENT_TYPE, cardNetworkStrings);
            }

            defaultParameters.put("billingAddressRequired", request.isBillingAddressRequired())
                    .put("allowPrepaidCards", request.getAllowPrepaidCards())
                    .put("allowedAuthMethods",
                            request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE))
                    .put("allowedCardNetworks",
                            request.getAllowedCardNetworksForType(CARD_PAYMENT_TYPE));

            if (request.isBillingAddressRequired()) {
                defaultParameters.put("billingAddressParameters",
                        new JSONObject().put("format", request.billingAddressFormatToString())
                                .put("phoneNumberRequired", request.isPhoneNumberRequired()));
            }
        } catch (JSONException ignored) {
        }
        return defaultParameters;
    }

    private JSONObject buildPayPalPaymentMethodParameters(Configuration configuration) {
        JSONObject defaultParameters = new JSONObject();

        try {
            JSONObject purchaseContext = new JSONObject().put("purchase_units", new JSONArray().put(
                    new JSONObject().put("payee", new JSONObject().put("client_id",
                                    configuration.getGooglePayPayPalClientId()))
                            .put("recurring_payment", "true")));

            defaultParameters.put("purchase_context", purchaseContext);
        } catch (JSONException ignored) {
        }

        return defaultParameters;

    }

    private JSONObject buildCardTokenizationSpecification(Configuration configuration,
                                                          Authorization authorization) {
        JSONObject cardJson = new JSONObject();
        JSONObject parameters = new JSONObject();
        String googlePayVersion = com.braintreepayments.api.googlepay.BuildConfig.VERSION_NAME;

        try {
            parameters.put("gateway", "braintree").put("braintree:apiVersion", "v1")
                    .put("braintree:sdkVersion", googlePayVersion)
                    .put("braintree:merchantId", configuration.getMerchantId())
                    .put("braintree:metadata", (new JSONObject().put("source", "client")
                            .put("integration", braintreeClient.getIntegrationType())
                            .put("sessionId", braintreeClient.getSessionId())
                            .put("version", googlePayVersion)
                            .put("platform", "android")).toString());

            if (authorization instanceof TokenizationKey) {
                parameters.put("braintree:clientKey", authorization.toString());
            } else {
                String googlePayAuthFingerprint =
                        configuration.getGooglePayAuthorizationFingerprint();
                parameters.put("braintree:authorizationFingerprint", googlePayAuthFingerprint);
            }
        } catch (JSONException ignored) {
        }

        try {
            cardJson.put("type", "PAYMENT_GATEWAY").put("parameters", parameters);
        } catch (JSONException ignored) {
        }

        return cardJson;
    }

    private JSONObject buildPayPalTokenizationSpecification(Configuration configuration) {
        JSONObject json = new JSONObject();
        String googlePayVersion = com.braintreepayments.api.googlepay.BuildConfig.VERSION_NAME;

        try {
            json.put("type", "PAYMENT_GATEWAY").put("parameters",
                    new JSONObject().put("gateway", "braintree").put("braintree:apiVersion", "v1")
                            .put("braintree:sdkVersion", googlePayVersion)
                            .put("braintree:merchantId", configuration.getMerchantId())
                            .put("braintree:paypalClientId",
                                    configuration.getGooglePayPayPalClientId())
                            .put("braintree:metadata", (new JSONObject().put("source", "client")
                                    .put("integration", braintreeClient.getIntegrationType())
                                    .put("sessionId", braintreeClient.getSessionId())
                                    .put("version", googlePayVersion)
                                    .put("platform", "android")).toString()));
        } catch (JSONException ignored) {
        }

        return json;
    }

    private void setGooglePayRequestDefaults(Configuration configuration,
                                             Authorization authorization,
                                             GooglePayRequest request) {

        if (request.getAllowedPaymentMethod(CARD_PAYMENT_TYPE) == null) {
            request.setAllowedPaymentMethod(CARD_PAYMENT_TYPE,
                    buildCardPaymentMethodParameters(configuration, request));
        }

        if (request.getTokenizationSpecificationForType(CARD_PAYMENT_TYPE) == null) {
            request.setTokenizationSpecificationForType("CARD",
                    buildCardTokenizationSpecification(configuration, authorization));
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
        ActivityInfo activityInfo =
                braintreeClient.getManifestActivityInfo(GooglePayActivity.class);
        return activityInfo != null &&
                activityInfo.getThemeResource() == R.style.bt_transparent_activity;
    }

    private void callbackPaymentRequestSuccess(@NonNull GooglePayPaymentAuthRequest.ReadyToLaunch request,
                                               GooglePayPaymentAuthRequestCallback callback) {
        callback.onGooglePayPaymentAuthRequest(request);
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_SUCCEEDED);
    }

    private void callbackPaymentRequestFailure(@NonNull GooglePayPaymentAuthRequest.Failure request,
                                               GooglePayPaymentAuthRequestCallback callback) {
        callback.onGooglePayPaymentAuthRequest(request);
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_FAILED);
    }

    private void callbackTokenizeSuccess(@NonNull GooglePayResult.Success result,
                                         GooglePayTokenizeCallback callback) {
        callback.onGooglePayResult(result);
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_SUCCEEDED);
    }

    private void callbackTokenizeCancel(GooglePayTokenizeCallback callback) {
        callback.onGooglePayResult(GooglePayResult.Cancel.INSTANCE);
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_SHEET_CANCELED);
    }

    private void callbackTokenizeFailure(@NonNull GooglePayResult.Failure result,
                                         GooglePayTokenizeCallback callback) {
        callback.onGooglePayResult(result);
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_FAILED);
    }
}