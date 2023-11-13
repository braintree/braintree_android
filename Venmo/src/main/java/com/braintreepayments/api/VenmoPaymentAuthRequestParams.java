package com.braintreepayments.api;

/**
 * Used to request Venmo authentication via {@link VenmoLauncher#launch(VenmoPaymentAuthRequest.ReadyToLaunch)}
 */
public class VenmoPaymentAuthRequestParams {

    private Configuration configuration;
    private String profileId;
    private String paymentContextId;
    private String sessionId;
    private String integrationType;

    VenmoPaymentAuthRequestParams(Configuration configuration, String profileId, String paymentContextId,
                                  String sessionId, String integrationType) {
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
