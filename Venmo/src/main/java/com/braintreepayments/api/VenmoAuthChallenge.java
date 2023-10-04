package com.braintreepayments.api;

public class VenmoAuthChallenge {

    private final Configuration configuration;
    private final String profileId;
    private final String paymentContextId;
    private final String sessionId;
    private final String integrationType;

    VenmoAuthChallenge(Configuration configuration, String profileId, String paymentContextId, String sessionId, String integrationType) {
       this.configuration = configuration;
       this.profileId = profileId;
       this.paymentContextId = paymentContextId;
       this.sessionId = sessionId;
       this.integrationType = integrationType;
    }

    Configuration getConfiguration() {
        return configuration;
    }

    String getProfileId() {
        return profileId;
    }

    String getPaymentContextId() {
        return paymentContextId;
    }

    String getSessionId() {
        return sessionId;
    }

    String getIntegrationType() {
        return integrationType;
    }
}
