package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.GoogleApiClientException;
import com.braintreepayments.api.exceptions.GoogleApiClientException.ErrorType;
import com.braintreepayments.api.exceptions.GooglePaymentException;
import com.braintreepayments.api.googlepayment.R;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.TokenizationParametersListener;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.GooglePaymentConfiguration;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.MetadataBuilder;
import com.braintreepayments.api.models.PaymentMethodNonceFactory;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    private static final String CARD_PAYMENT_TYPE = "CARD";
    private static final String PAYPAL_PAYMENT_TYPE = "PAYPAL";

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
    public static void isReadyToPay(final BraintreeFragment fragment,
                                    final BraintreeResponseListener<Boolean> listener) {
        try {
            Class.forName(PaymentsClient.class.getName());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            listener.onResponse(false);
            return;
        }

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.getGooglePayment().isEnabled(fragment.getApplicationContext())) {
                    listener.onResponse(false);
                    return;
                }

                if (fragment.getActivity() == null) {
                    fragment.postCallback(new GoogleApiClientException(ErrorType.NotAttachedToActivity, 1));
                }

                PaymentsClient paymentsClient = Wallet.getPaymentsClient(fragment.getActivity(),
                        new Wallet.WalletOptions.Builder()
                                .setEnvironment(getEnvironment(configuration.getGooglePayment()))
                                .build());


                JSONObject json = new JSONObject();
                JSONArray allowedCardNetworks = new JSONArray();

                for (String cardnetwork : configuration.getGooglePayment().getSupportedNetworks()) {
                    allowedCardNetworks.put(cardnetwork);
                }

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
                } catch (JSONException ignored) {
                }

                IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(json.toString());

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
     * @param request  The {@link GooglePaymentRequest} containing options for the transaction.
     */
    public static void requestPayment(final BraintreeFragment fragment, final @NonNull GooglePaymentRequest request) {
        fragment.sendAnalyticsEvent("google-payment.selected");

        if (!validateManifest(fragment.getApplicationContext())) {
            fragment.postCallback(new BraintreeException("GooglePaymentActivity was not found in the Android " +
                    "manifest, or did not have a theme of R.style.bt_transparent_activity"));
            fragment.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        if (request == null) {
            fragment.postCallback(new BraintreeException("Cannot pass null GooglePaymentRequest to requestPayment"));
            fragment.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        if (request.getTransactionInfo() == null) {
            fragment.postCallback(new BraintreeException("Cannot pass null TransactionInfo to requestPayment"));
            fragment.sendAnalyticsEvent("google-payment.failed");
            return;
        }

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {

                setGooglePaymentRequestDefaults(fragment, configuration, request);

                fragment.sendAnalyticsEvent("google-payment.started");

                PaymentDataRequest paymentDataRequest = PaymentDataRequest.fromJson(request.toJson());

                Intent intent = new Intent(fragment.getApplicationContext(), GooglePaymentActivity.class)
                        .putExtra(EXTRA_ENVIRONMENT, getEnvironment(configuration.getGooglePayment()))
                        .putExtra(EXTRA_PAYMENT_DATA_REQUEST, paymentDataRequest);
                fragment.startActivityForResult(intent, BraintreeRequestCodes.GOOGLE_PAYMENT);
            }
        });
    }

    /**
     * Call this method when you've received a successful {@link PaymentData} response in your activity's
     * {@link AppCompatActivity#onActivityResult(int, int, Intent)} to get a {@link GooglePaymentCardNonce}.
     *
     * @param fragment    An instance of {@link BraintreeFragment}.
     * @param paymentData {@link PaymentData} from the Intent in {@link AppCompatActivity#onActivityResult(int, int, Intent)}.
     */
    public static void tokenize(BraintreeFragment fragment, PaymentData paymentData) {
        try {
            fragment.postCallback(PaymentMethodNonceFactory.fromString(paymentData.toJson()));
            fragment.sendAnalyticsEvent("google-payment.nonce-received");
        } catch (JSONException | NullPointerException e) {
            fragment.sendAnalyticsEvent("google-payment.failed");

            try {
                String token = new JSONObject(paymentData.toJson())
                        .getJSONObject("paymentMethodData")
                        .getJSONObject("tokenizationData")
                        .getString("token");
                fragment.postCallback(ErrorWithResponse.fromJson(token));
            } catch (JSONException | NullPointerException e1) {
                fragment.postCallback(e1);
            }
        }
    }

    static void onActivityResult(BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            fragment.sendAnalyticsEvent("google-payment.authorized");
            tokenize(fragment, PaymentData.getFromIntent(data));
        } else if (resultCode == AutoResolveHelper.RESULT_ERROR) {
            fragment.sendAnalyticsEvent("google-payment.failed");

            fragment.postCallback(new GooglePaymentException("An error was encountered during the Google Payments " +
                    "flow. See the status object in this exception for more details.",
                    AutoResolveHelper.getStatusFromIntent(data)));
        } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            fragment.sendAnalyticsEvent("google-payment.canceled");
        }
    }

    static int getEnvironment(GooglePaymentConfiguration configuration) {
        if ("production".equals(configuration.getEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }

    static PaymentMethodTokenizationParameters getTokenizationParameters(BraintreeFragment fragment) {
        String version;

        JSONObject metadata = new MetadataBuilder()
                .integration(fragment.getIntegrationType())
                .sessionId(fragment.getSessionId())
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
                .addParameter("braintree:merchantId", fragment.getConfiguration().getMerchantId())
                .addParameter("braintree:authorizationFingerprint",
                        fragment.getConfiguration().getGooglePayment().getGoogleAuthorizationFingerprint())
                .addParameter("braintree:apiVersion", "v1")
                .addParameter("braintree:sdkVersion", version)
                .addParameter("braintree:metadata", metadata.toString());

        if (fragment.getAuthorization() instanceof TokenizationKey) {
            parameters.addParameter("braintree:clientKey", fragment.getAuthorization().getBearer());
        }

        return parameters.build();
    }

    static ArrayList<Integer> getAllowedCardNetworks(BraintreeFragment fragment) {
        ArrayList<Integer> allowedNetworks = new ArrayList<>();
        for (String network : fragment.getConfiguration().getGooglePayment().getSupportedNetworks()) {
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

    private static JSONObject buildCardPaymentMethodParameters(GooglePaymentRequest request,
                                                               BraintreeFragment fragment) {
        JSONObject defaultParameters = new JSONObject();

        try {
            if (request.getAllowedCardNetworksForType(CARD_PAYMENT_TYPE) == null) {
                JSONArray cardNetworkStrings = new JSONArray();

                for (int network : getAllowedCardNetworks(fragment)) {
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

    private static JSONObject buildPayPalPaymentMethodParameters(BraintreeFragment fragment) {
        JSONObject defaultParameters = new JSONObject();

        try {
            JSONObject purchaseContext = new JSONObject()
                    .put("purchase_units", new JSONArray()
                            .put(new JSONObject()
                                    .put("payee", new JSONObject()
                                            .put("client_id", fragment.getConfiguration().getGooglePayment().getPaypalClientId())
                                    )
                                    .put("recurring_payment", "true")
                            )
                    );

            defaultParameters.put("purchase_context", purchaseContext);
        } catch (JSONException ignored) {
        }

        return defaultParameters;

    }

    private static JSONObject buildCardTokenizationSpecification(BraintreeFragment fragment) {
        JSONObject cardJson = new JSONObject();
        JSONObject parameters = new JSONObject();

        try {
            parameters
                    .put("gateway", "braintree")
                    .put("braintree:apiVersion", "v1")
                    .put("braintree:sdkVersion", BuildConfig.VERSION_NAME)
                    .put("braintree:merchantId", fragment.getConfiguration().getMerchantId())
                    .put("braintree:metadata", (new JSONObject()
                            .put("source", "client")
                            .put("integration", fragment.getIntegrationType())
                            .put("sessionId", fragment.getSessionId())
                            .put("version", BuildConfig.VERSION_NAME)
                            .put("platform", "android")).toString());

            if (Authorization.isTokenizationKey(fragment.getAuthorization().toString())) {
                parameters
                        .put("braintree:clientKey",
                                fragment
                                        .getAuthorization()
                                        .toString());
            } else {
                parameters
                        .put("braintree:authorizationFingerprint",
                                fragment
                                        .getConfiguration()
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

    private static JSONObject buildPayPalTokenizationSpecification(BraintreeFragment fragment) {
        JSONObject json = new JSONObject();

        try {
            json.put("type", "PAYMENT_GATEWAY")
                    .put("parameters", new JSONObject()
                            .put("gateway", "braintree")
                            .put("braintree:apiVersion", "v1")
                            .put("braintree:sdkVersion", BuildConfig.VERSION_NAME)
                            .put("braintree:merchantId",
                                    fragment.getConfiguration().getMerchantId())
                            .put("braintree:paypalClientId",
                                    fragment.getConfiguration().getGooglePayment().getPaypalClientId())
                            .put("braintree:metadata", (new JSONObject()
                                    .put("source", "client")
                                    .put("integration", fragment.getIntegrationType())
                                    .put("sessionId", fragment.getSessionId())
                                    .put("version", BuildConfig.VERSION_NAME)
                                    .put("platform", "android")).toString()));
        } catch (JSONException ignored) {
        }

        return json;
    }

    private static void setGooglePaymentRequestDefaults(BraintreeFragment fragment,
                                                        Configuration configuration,
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
                    buildCardPaymentMethodParameters(request, fragment));
        }

        if (request.getTokenizationSpecificationForType(CARD_PAYMENT_TYPE) == null) {
            request.setTokenizationSpecificationForType("CARD",
                    buildCardTokenizationSpecification(fragment));
        }

        boolean googlePaymentCanProcessPayPal = request.isPayPalEnabled() &&
                !TextUtils.isEmpty(configuration.getGooglePayment().getPaypalClientId());

        if (googlePaymentCanProcessPayPal) {
            if (request.getAllowedPaymentMethod("PAYPAL") == null) {
                request.setAllowedPaymentMethod(PAYPAL_PAYMENT_TYPE,
                        buildPayPalPaymentMethodParameters(fragment));
            }


            if (request.getTokenizationSpecificationForType(PAYPAL_PAYMENT_TYPE) == null) {
                request.setTokenizationSpecificationForType("PAYPAL",
                        buildPayPalTokenizationSpecification(fragment));
            }
        }

        request.environment(configuration.getGooglePayment().getEnvironment());
    }

    private static boolean validateManifest(Context context) {
        ActivityInfo activityInfo = ManifestValidator.getActivityInfo(context, GooglePaymentActivity.class);
        return activityInfo != null && activityInfo.getThemeResource() == R.style.bt_transparent_activity;
    }
}
