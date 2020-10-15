package com.braintreepayments.api.network;

public class PayPalApiEnvironmentManager {

    public static final String LIVE = "live";
    public static final String SANDBOX = "sandbox";
    public static final String MOCK = "mock";

    public static final String LIVE_API_M_ENDPOINT = "https://api-m.paypal.com/v1/";
    public static final String SANDBOX_API_M_ENDPOINT = "https://api-m.sandbox.paypal.com/v1/";

    /**
     * @param environmentName environment name to check
     * @return <code>true</code> if full API mocking is enabled, otherwise <code>false</code>.
     */
    public static boolean isMock(String environmentName) {
        return environmentName.equals(MOCK);
    }

    /**
     * @param environmentName environment name to check
     * @return <code>true</code> if the SANDBOX server is active, otherwise <code>false</code>.
     */
    public static boolean isSandbox(String environmentName) {
        return environmentName.equals(SANDBOX);
    }

    /**
     * @param environmentName environment name to check
     * @return <code>true</code> if the LIVE server is active, otherwise <code>false</code>.
     */
    public static boolean isLive(String environmentName) {
        return environmentName.equals(LIVE);
    }

    /**
     * @param environmentName environment name to check
     * @return <code>true</code> if the environment is a stage (not MOCK, SANDBOX, or LIVE),
     * otherwise <code>false</code>
     */
    public static boolean isStage(String environmentName) {
        return !(isLive(environmentName) || isSandbox(environmentName) || isMock(environmentName));
    }

    /**
     * @param environmentName environment name to check
     * @return the url for the environment or <code>null</code> for MOCK
     */
    public static String getEnvironmentUrl(String environmentName) {
        if (PayPalApiEnvironmentManager.isLive(environmentName)) {
            return PayPalApiEnvironmentManager.LIVE_API_M_ENDPOINT;
        } else if (PayPalApiEnvironmentManager.isSandbox(environmentName)) {
            return PayPalApiEnvironmentManager.SANDBOX_API_M_ENDPOINT;
        } else if (PayPalApiEnvironmentManager.isMock(environmentName)) {
            return null;
        } else {
            return environmentName;
        }
    }
}
