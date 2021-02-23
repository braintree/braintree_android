package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.visa.checkout.Environment;
import com.visa.checkout.Profile;
import com.visa.checkout.VisaPaymentSummary;

import java.util.List;

/**
 * Used to create and tokenize Visa Checkout. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/visa-checkout/overview">documentation</a>
 */
public class VisaCheckoutClient {

    private BraintreeClient braintreeClient;
    private TokenizationClient tokenizationClient;

    public VisaCheckoutClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new TokenizationClient(braintreeClient));
    }

    @VisibleForTesting
    VisaCheckoutClient(BraintreeClient braintreeClient, TokenizationClient tokenizationClient) {
        this.braintreeClient = braintreeClient;
        this.tokenizationClient = tokenizationClient;
    }

    /**
     * Creates a {@link ProfileBuilder} with the merchant API key, environment, and other properties to be used with
     * Visa Checkout.
     *
     * In addition to setting the `merchantApiKey` and `environment` the other properties that Braintree will fill in
     * on the ProfileBuilder are:
     * <ul>
     *     <li>
     *         {@link ProfileBuilder#setCardBrands(String[])} A list of Card brands that your merchant account can
     *         transact.
     *     </li>
     *     <li>
     *         {@link ProfileBuilder#setDataLevel(String)} - Required to be {@link DataLevel#FULL} for Braintree to
     *     access card details
     *     </li>
     *     <li>
     *         {@link ProfileBuilder#setExternalClientId(String)} -  Allows the encrypted payload to be processable
     *         by Braintree.
     *     </li>
     * </ul>
     *
     * @param callback {@link VisaCheckoutCreateProfileBuilderCallback} - listens for the
     * Braintree flavored {@link ProfileBuilder}.
     */
    public void createProfileBuilder(final VisaCheckoutCreateProfileBuilderCallback callback) {
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
     * @param visaPaymentSummary {@link VisaPaymentSummary} The Visa payment to tokenize.
     * @param callback {@link VisaCheckoutTokenizeCallback}
     */
    public void tokenize(VisaPaymentSummary visaPaymentSummary, final VisaCheckoutTokenizeCallback callback) {
        tokenizationClient.tokenize(new VisaCheckoutBuilder(visaPaymentSummary), new PaymentMethodNonceCallback() {
            @Override
            public void success(PaymentMethodNonce paymentMethodNonce) {
                callback.onResult(paymentMethodNonce, null);
                braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.succeeded");
            }

            @Override
            public void failure(Exception e) {
                callback.onResult(null, e);
                braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.failed");
            }
        });
    }
}
