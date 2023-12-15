package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.visa.checkout.Environment;
import com.visa.checkout.Profile;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;

import java.util.List;

/**
 * Used to create and tokenize Visa Checkout. For more information see the <a
 * href="https://developer.paypal.com/braintree/docs/guides/secure-remote-commerce/overview">documentation</a>
 */
public class VisaCheckoutClient {

    private final BraintreeClient braintreeClient;
    private final ApiClient apiClient;

    /**
     * Initializes a new {@link VisaCheckoutClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public VisaCheckoutClient(@NonNull Context context, @NonNull String authorization) {
        this(new BraintreeClient(context, authorization));
    }

    @VisibleForTesting
    VisaCheckoutClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new ApiClient(braintreeClient));
    }

    @VisibleForTesting
    VisaCheckoutClient(BraintreeClient braintreeClient, ApiClient apiClient) {
        this.braintreeClient = braintreeClient;
        this.apiClient = apiClient;
    }

    /**
     * Creates a {@link Profile.ProfileBuilder} with the merchant API key, environment, and other
     * properties to be used with Visa Checkout.
     * <p>
     * In addition to setting the `merchantApiKey` and `environment` the other properties that
     * Braintree will fill in on the ProfileBuilder are:
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
    public void createProfileBuilder(
            @NonNull final VisaCheckoutCreateProfileBuilderCallback callback) {
        braintreeClient.getConfiguration((configuration, e) -> {
            boolean enabledAndSdkAvailable =
                    isVisaCheckoutSDKAvailable() && configuration.isVisaCheckoutEnabled();

            if (!enabledAndSdkAvailable) {
                callback.onVisaCheckoutProfileBuilderResult(new VisaCheckoutProfileBuilderResult.Failure(new ConfigurationException("Visa Checkout is not enabled.")));
                return;
            }

            String merchantApiKey = configuration.getVisaCheckoutApiKey();
            List<String> acceptedCardBrands = configuration.getVisaCheckoutSupportedNetworks();
            String environment = Environment.SANDBOX;

            if ("production".equals(configuration.getEnvironment())) {
                environment = Environment.PRODUCTION;
            }

            Profile.ProfileBuilder profileBuilder =
                    new Profile.ProfileBuilder(merchantApiKey, environment);
            profileBuilder.setCardBrands(
                    acceptedCardBrands.toArray(new String[acceptedCardBrands.size()]));
            profileBuilder.setDataLevel(Profile.DataLevel.FULL);
            profileBuilder.setExternalClientId(configuration.getVisaCheckoutExternalClientId());

            callback.onVisaCheckoutProfileBuilderResult(new VisaCheckoutProfileBuilderResult.Success(profileBuilder));
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
     * @param callback           {@link VisaCheckoutTokenizeCallback}
     */
    public void tokenize(@NonNull VisaPaymentSummary visaPaymentSummary,
                         @NonNull final VisaCheckoutTokenizeCallback callback) {
        apiClient.tokenizeREST(new VisaCheckoutAccount(visaPaymentSummary),
                (tokenizationResponse, exception) -> {
                    if (tokenizationResponse != null) {
                        try {
                            VisaCheckoutNonce visaCheckoutNonce =
                                    VisaCheckoutNonce.fromJSON(tokenizationResponse);
                            callback.onVisaCheckoutResult(new VisaCheckoutResult.Success(visaCheckoutNonce));
                            braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.succeeded");
                        } catch (JSONException e) {
                            callback.onVisaCheckoutResult(new VisaCheckoutResult.Failure(e));
                        }
                    } else if (exception != null) {
                        callback.onVisaCheckoutResult(new VisaCheckoutResult.Failure(exception));
                        braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.failed");
                    }
                });
    }
}
