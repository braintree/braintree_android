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


    private BrowserSwitchOptions browserSwitchOptions;

    VenmoPaymentAuthRequestParams(Configuration configuration, String profileId, String paymentContextId,
                                  String sessionId, String integrationType,
                                  BrowserSwitchOptions browserSwitchOptions) {
        this.configuration = configuration;
        this.profileId = profileId;
        this.paymentContextId = paymentContextId;
        this.sessionId = sessionId;
        this.integrationType = integrationType;
        this.browserSwitchOptions = browserSwitchOptions;
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

    BrowserSwitchOptions getBrowserSwitchOptions() {
        return browserSwitchOptions;
    }
}
