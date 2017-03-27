package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VisaCheckoutBuilder;
import com.braintreepayments.api.models.VisaCheckoutConfiguration;
import com.braintreepayments.api.models.VisaCheckoutNonce;
import com.visa.checkout.Environment;
import com.visa.checkout.Profile.DataLevel;
import com.visa.checkout.Profile.ProfileBuilder;
import com.visa.checkout.PurchaseInfo.PurchaseInfoBuilder;
import com.visa.checkout.VisaCheckoutSdk;
import com.visa.checkout.VisaPaymentSummary;

import java.util.List;

/**
 * Used to create and tokenize Visa Checkout. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/visa-checkout/overview">documentation</a>
 */
public class VisaCheckout {

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
     *         {@link ProfileBuilder#setDateLevel(String)} - Required to be {@link DataLevel#FULL} for Braintree to
     *     access card details
     *     </li>
     *     <li>
     *         {@link ProfileBuilder#setExternalClientId(String)} -  Allows the encrypted payload to be processable
     *         by Braintree.
     *     </li>
     * </ul>
     *
     * @param fragment - {@link BraintreeFragment}
     * @param profileBuilderResponseListener {@link BraintreeResponseListener<ProfileBuilder>} - listens for the
     * Braintree flavored {@link ProfileBuilder}.
     */
    public static void createProfileBuilder(final BraintreeFragment fragment, final BraintreeResponseListener<ProfileBuilder>
            profileBuilderResponseListener) {

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                VisaCheckoutConfiguration visaCheckoutConfiguration = configuration.getVisaCheckout();

                if (!configuration.getVisaCheckout().isEnabled()) {
                    fragment.postCallback(new ConfigurationException("Visa Checkout is not enabled."));
                    return;
                }

                String environment = Environment.SANDBOX;
                String merchantApiKey = visaCheckoutConfiguration.getApiKey();
                List<String> acceptedCardBrands = visaCheckoutConfiguration.getAcceptedCardBrands();

                if ("production".equals(configuration.getEnvironment())) {
                    environment = Environment.PRODUCTION;
                }

                ProfileBuilder profileBuilder = new ProfileBuilder(merchantApiKey, environment);
                profileBuilder.setCardBrands(acceptedCardBrands.toArray(new String[acceptedCardBrands.size()]));
                profileBuilder.setDateLevel(DataLevel.FULL);
                profileBuilder.setExternalClientId(visaCheckoutConfiguration.getExternalClientId());

                profileBuilderResponseListener.onResponse(profileBuilder);
            }
        });
    }

    /**
     * Starts Visa Checkout to authorize a payment from the customer.
     * @param fragment {@link BraintreeFragment}
     * @param purchaseInfoBuilder {@link PurchaseInfoBuilder} Used to customize the authorization process.
     * </p>
     */
    public static void authorize(final BraintreeFragment fragment, final PurchaseInfoBuilder purchaseInfoBuilder) {
        Intent intent = VisaCheckoutSdk.getCheckoutIntent(fragment.getActivity(),
                purchaseInfoBuilder.build());

        fragment.sendAnalyticsEvent("visacheckout.initiate.started");
        fragment.startActivityForResult(intent, BraintreeRequestCodes.VISA_CHECKOUT);
    }

    static void onActivityResult(BraintreeFragment fragment, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            fragment.sendAnalyticsEvent("visacheckout.result.cancelled");
        } else if (resultCode == Activity.RESULT_OK && data != null) {
            VisaPaymentSummary visaPaymentSummary = data.getParcelableExtra(
                    VisaCheckoutSdk.INTENT_PAYMENT_SUMMARY);
            tokenize(fragment, visaPaymentSummary);
            fragment.sendAnalyticsEvent("visacheckout.result.succeeded");
        } else {
            fragment.postCallback(
                    new BraintreeException("Visa Checkout responded with an invalid resultCode: " + resultCode));
            fragment.sendAnalyticsEvent("visacheckout.result.failed");
        }
    }

    static void tokenize(final BraintreeFragment fragment, final VisaPaymentSummary visaPaymentSummary) {
        TokenizationClient.tokenize(fragment, new VisaCheckoutBuilder(visaPaymentSummary),
                new PaymentMethodNonceCallback() {
                    @Override
                    public void success(PaymentMethodNonce paymentMethodNonce) {
                        VisaCheckoutNonce visaCheckoutNonce = (VisaCheckoutNonce) paymentMethodNonce;
                        fragment.postCallback(paymentMethodNonce);
                        fragment.sendAnalyticsEvent("visacheckout.tokenize.succeeded");
                    }

                    @Override
                    public void failure(Exception exception) {
                        fragment.postCallback(exception);
                        fragment.sendAnalyticsEvent("visacheckout.tokenize.failed");
                    }
                });
    }
}
