package com.paypal.android.networking;

import java.util.Arrays;
import java.util.List;

/**
 * Utility methods
 */
public class EnvironmentManager {
    public static final String SANDBOX = "sandbox";
    public static final String LIVE = "live";
    public static final String MOCK = "mock";

    // Don't touch these!
    public static final List<String> UNTOUCHABLE_ENDPOINTS = Arrays.asList(LIVE, SANDBOX, MOCK);

    public static final String LIVE_API_M_ENDPOINT = "https://api-m.paypal.com/v1/";
    public static final String SANDBOX_API_M_ENDPOINT = "https://api-m.sandbox.paypal.com/v1/";

    /**
     * @return <code>true</code> if full API mocking is enabled.
     */
    public static boolean isMock(String environmentName) {
        return environmentName.equals(MOCK);
    }

    /**
     * @return <code>true</code> if the SANDBOX server is active.
     */
    public static boolean isSandbox(String environmentName) {
        // startsWith to support mitm debugging.
        return environmentName.startsWith(SANDBOX);
    }

    /**
     * @return <code>true</code> if the live server is active.
     */
    public static boolean isLive(String environmentName) {
        return environmentName.equals(LIVE);
    }

    /**
     * @return <code>true</code> if the environment is a stage (not mock, sandbox, or live).
     */
    public static boolean isStage(String environmentName) {
        return !(isLive(environmentName) || isSandbox(environmentName) || isMock(environmentName));
    }

    /**
     * @return <code>true</code> if the environment is a mock or sandbox, duh
     */
    public static boolean isMockOrSandbox(String environmentName) {
        return isSandbox(environmentName) || isMock(environmentName);
    }
}
