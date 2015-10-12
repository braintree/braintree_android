package com.braintreepayments.api.models;

import android.content.Context;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.BillingAgreementRequest;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.Request;

public class PayPalRequestBuilder {

    public CheckoutRequest createCheckoutRequest(Context context, Configuration configuration) throws ConfigurationException {
        CheckoutRequest request = new CheckoutRequest();
        buildRequest(request, context, configuration);
        return request;
    }

    public BillingAgreementRequest createBillingAgreementRequest(Context context, Configuration configuration) throws ConfigurationException {
        BillingAgreementRequest request = new BillingAgreementRequest();
        buildRequest(request, context, configuration);
        return request;
    }

    public AuthorizationRequest createAuthorizationRequest(Context context, Configuration configuration) throws ConfigurationException {
        AuthorizationRequest request = new AuthorizationRequest(context);
        buildRequest(request, context, configuration);
        return request;
    }

    private void buildRequest(Request request, Context context, Configuration configuration) throws ConfigurationException {
        validatePayPalConfiguration(configuration);
        populateCommonData(request, context, configuration);

    }

    private void populateCommonData(Request request, Context context, Configuration configuration) {
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();
        String environment;
        if (payPalConfiguration.getEnvironment().equals("live")) {
            environment = AuthorizationRequest.ENVIRONMENT_LIVE;
        } else if (payPalConfiguration.getEnvironment().equals("offline")) {
            environment = AuthorizationRequest.ENVIRONMENT_MOCK;
        } else {
            environment = payPalConfiguration.getEnvironment();
        }

        String clientId = payPalConfiguration.getClientId();
        if (clientId == null && environment == AuthorizationRequest.ENVIRONMENT_MOCK) {
            clientId = "FAKE-PAYPAL-CLIENT-ID";
        }

        request.environment(environment);
        request.clientId(clientId);
        request.cancelUrl(context.getPackageName() + ".braintree", "cancel");
        request.successUrl(context.getPackageName() + ".braintree", "success");
    }

    /**
     * Throws a {@link ConfigurationException} when the config is invalid
     *
     * @param configuration
     * @throws ConfigurationException
     */
    private void validatePayPalConfiguration(Configuration configuration)
            throws ConfigurationException {
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        if (!configuration.isPayPalEnabled() ||
                payPalConfiguration.getEnvironment() == null ||
                payPalConfiguration.getPrivacyUrl() == null ||
                payPalConfiguration.getUserAgreementUrl() == null) {
            throw new ConfigurationException("PayPal is disabled or configuration is invalid");
        }
    }

}
