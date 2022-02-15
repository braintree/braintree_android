package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.visa.checkout.Environment;
import com.visa.checkout.Profile;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Used to create and tokenize Visa Checkout. For more information see the
 * <a href="https://developer.paypal.com/braintree/docs/guides/secure-remote-commerce/overview">documentation</a>
 */
public class VisaCheckoutClient {

    private final BraintreeClient braintreeClient;
    private final ApiClient apiClient;

    public VisaCheckoutClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new ApiClient(braintreeClient));
    }

    @VisibleForTesting
    VisaCheckoutClient(BraintreeClient braintreeClient, ApiClient apiClient) {
        this.braintreeClient = braintreeClient;
        this.apiClient = apiClient;
    }

    /**
     * Creates a {@link Profile.ProfileBuilder} with the merchant API key, environment, and other properties to be used with
     * Visa Checkout.
     *
     * In addition to setting the `merchantApiKey` and `environment` the other properties that Braintree will fill in
     * on the ProfileBuilder are:
     * <ul>
     *     <li>
     *         {@link Profile.ProfileBuilder#setCardBrands(String[])} A list of Card brands that your merchant account can
     *         transact.
     *     </li>
     *     <li>
     *         {@link Profile.ProfileBuilder#setDataLevel(String)} - Required to be {@link Profile.DataLevel#FULL} for Braintree to
     *     access card details
     *     </li>
     *     <li>
     *         {@link Profile.ProfileBuilder#setExternalClientId(String)} -  Allows the encrypted payload to be processable
     *         by Braintree.
     *     </li>
     * </ul>
     *
     * @param callback {@link VisaCheckoutCreateProfileBuilderCallback}
     */
    public void createProfileBuilder(@NonNull final VisaCheckoutCreateProfileBuilderCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception e) {
                boolean enabledAndSdkAvailable =
                    isVisaCheckoutSDKAvailable() && configuration.isVisaCheckoutEnabled();

                if (!enabledAndSdkAvailable) {
                    callback.onResult(null, new ConfigurationException("Visa Checkout is not enabled."));
                    return;
                }

                String merchantApiKey = configuration.getVisaCheckoutApiKey();
                List<String> acceptedCardBrands = configuration.getVisaCheckoutSupportedNetworks();
                String environment = Environment.SANDBOX;

                if ("production".equals(configuration.getEnvironment())) {
                    environment = Environment.PRODUCTION;
                }

                Profile.ProfileBuilder profileBuilder = new Profile.ProfileBuilder(merchantApiKey, environment);
                profileBuilder.setCardBrands(acceptedCardBrands.toArray(new String[acceptedCardBrands.size()]));
                profileBuilder.setDataLevel(Profile.DataLevel.FULL);
                profileBuilder.setExternalClientId(configuration.getVisaCheckoutExternalClientId());

                callback.onResult(profileBuilder, null);
            }
        });
    }

    static boolean isVisaCheckoutSDKAvailable() {
        try {
            Class.forName("com.visa.checkout.VisaCheckoutSdk");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Tokenizes the payment summary of the Visa Checkout flow.
     *
     * @param visaPaymentSummary {@link VisaPaymentSummary} The Visa payment to tokenize.
     * @param callback {@link VisaCheckoutTokenizeCallback}
     */
    public void tokenize(@NonNull VisaPaymentSummary visaPaymentSummary, @NonNull final VisaCheckoutTokenizeCallback callback) {
        apiClient.tokenizeREST(new VisaCheckoutAccount(visaPaymentSummary), new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (tokenizationResponse != null) {
                    try {
                        VisaCheckoutNonce visaCheckoutNonce = VisaCheckoutNonce.fromJSON(tokenizationResponse);
                        callback.onResult(visaCheckoutNonce, null);
                        braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.succeeded");
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    callback.onResult(null, exception);
                    braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.failed");
                }
            }
        });
    }
}
