package com.braintreepayments.api;

class VenmoIntentData {

    private final Configuration configuration;
    private final String profileId;
    private final String paymentContextId;
    private final String sessionId;
    private final String integrationType;

    VenmoIntentData(Configuration configuration, String profileId, String paymentContextId, String sessionId, String integrationType) {
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

    public String getSessionId() {
        return sessionId;
    }

    public String getIntegrationType() {
        return integrationType;
    }
}
