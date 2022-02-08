package com.braintreepayments.api;

class VenmoIntentData {

    private final Configuration configuration;
    private final String profileId;
    private final String paymentContextId;
    private final String sessionId;
    private final String integrationType;
    private final boolean shouldVault;

    VenmoIntentData(Configuration configuration, String profileId, String paymentContextId, String sessionId, String integrationType, boolean shouldVault) {
       this.configuration = configuration;
       this.profileId = profileId;
       this.paymentContextId = paymentContextId;
       this.sessionId = sessionId;
       this.integrationType = integrationType;
       this.shouldVault = shouldVault;
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

    public boolean shouldVault() {
        return shouldVault;
    }
}
