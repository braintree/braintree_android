package com.braintreepayments.api;

public class VenmoContractInput {

    private final String profileId;
    private final String venmoAccessToken;
    private final String venmoEnvironment;

    private final String paymentContextId;
    private final String braintreeSessionId;
    private final String braintreeIntegrationType;

    public VenmoContractInput(
            String profileId,
            String venmoAccessToken,
            String venmoEnvironment,
            String paymentContextId,
            String braintreeSessionId,
            String braintreeIntegrationType
    ) {
        this.profileId = profileId;
        this.venmoAccessToken = venmoAccessToken;
        this.venmoEnvironment = venmoEnvironment;
        this.paymentContextId = paymentContextId;
        this.braintreeSessionId = braintreeSessionId;
        this.braintreeIntegrationType = braintreeIntegrationType;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getVenmoAccessToken() {
        return venmoAccessToken;
    }

    public String getVenmoEnvironment() {
        return venmoEnvironment;
    }

    public String getPaymentContextId() {
        return paymentContextId;
    }

    public String getBraintreeSessionId() {
        return braintreeSessionId;
    }

    public String getBraintreeIntegrationType() {
        return braintreeIntegrationType;
    }
}
