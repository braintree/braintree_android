package com.braintreepayments.api;

import android.text.TextUtils;
import android.util.Base64;

import java.util.regex.Pattern;

/* package */ class ClientToken {

    private String clientApiUrl;
    private String[] challenges;
    private String authorizationFingerprint;
    private PayPal paypal;
    private boolean paypalEnabled;
    private String venmo;
    private boolean threeDSecureEnabled;
    private Analytics analytics;
    private String merchantId;
    private String merchantAccountId;

    protected static ClientToken getClientToken(String clientToken) {
        Pattern pattern = Pattern.compile("([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)");
        if (pattern.matcher(clientToken).matches()) {
            clientToken = new String(Base64.decode(clientToken, Base64.DEFAULT));
        }

        return Utils.getGson().fromJson(clientToken, ClientToken.class);
    }

    protected String getClientApiUrl() {
        return clientApiUrl;
    }

    protected boolean isCvvChallengePresent() {
        return isChallengePresent("cvv");
    }

    protected boolean isPostalCodeChallengePresent() {
        return isChallengePresent("postal_code");
    }

    protected String getAuthorizationFingerprint() {
        return authorizationFingerprint;
    }

    protected PayPal getPayPal() {
        return paypal;
    }

    protected boolean isPayPalEnabled() {
        return (paypalEnabled && paypal != null);
    }

    protected String getVenmoState() {
        if (venmo == null) {
            return "off";
        } else {
            return venmo;
        }
    }

    protected boolean isThreeDSecureEnabled() {
        return threeDSecureEnabled;
    }

    protected Analytics getAnalytics() {
        return analytics;
    }

    protected boolean isAnalyticsEnabled() {
        return (analytics != null && !TextUtils.isEmpty(analytics.getUrl()));
    }

    private boolean isChallengePresent(String requestedChallenge) {
        if (challenges != null && challenges.length > 0) {
            for (String challenge : challenges) {
                if (challenge.equals(requestedChallenge)) {
                    return true;
                }
            }
        }

        return false;
    }

    protected String getMerchantId() {
        return merchantId;
    }

    protected String getMerchantAccountId() {
        return merchantAccountId;
    }

    class PayPal {

        private String displayName;
        private String clientId;
        private String privacyUrl;
        private String userAgreementUrl;
        private String directBaseUrl;
        private boolean allowHttp;
        private String environment;
        private boolean touchDisabled;

        protected String getDisplayName() {
            return displayName;
        }

        protected String getClientId() {
            return clientId;
        }

        protected String getPrivacyUrl() {
            return privacyUrl;
        }

        protected String getUserAgreementUrl() {
            return userAgreementUrl;
        }

        protected boolean getAllowHttp() {
            return allowHttp;
        }

        protected String getDirectBaseUrl() {
            return directBaseUrl + "/v1/";
        }

        protected String getEnvironment() {
            return environment;
        }

        protected boolean getTouchDisabled() {
            return touchDisabled;
        }
    }

    class Analytics {
        private String url;

        protected String getUrl() {
            return url;
        }
    }
}