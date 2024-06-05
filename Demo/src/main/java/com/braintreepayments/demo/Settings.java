package com.braintreepayments.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    private static final String ENVIRONMENT = "environment";

    static final String SANDBOX_ENV_NAME = "Sandbox";
    static final String MOCKED_PAY_PAL_ENV_NAME = "Mock PayPal";
    static final String PRODUCTION_ENV_NAME = "Production";

    private static final String MERCHANT_SERVER_URL = "https://sdk-sample-merchant-server.herokuapp.com";

    private static final String SANDBOX_BASE_SERVER_URL = MERCHANT_SERVER_URL + "/braintree/sandbox";
    private static final String PRODUCTION_BASE_SERVER_URL = MERCHANT_SERVER_URL + "/braintree/production";
    private static final String MOCKED_PAY_PAL_SANDBOX_SERVER_URL = MERCHANT_SERVER_URL + "/braintree/mock_pay_pal";

    private static final String SANDBOX_TOKENIZATION_KEY = "sandbox_tmxhyf7d_dcpspy2brwdjr3qn";
    private static final String PRODUCTION_TOKENIZATION_KEY = "production_t2wns2y2_dfy45jdj3dxkmz5m";
    private static final String MOCKED_PAY_PAL_SANDBOX_TOKENIZATION_KEY = "sandbox_q7v35n9n_555d2htrfsnnmfb3";

    static final String LOCAL_PAYMENTS_TOKENIZATION_KEY = "sandbox_f252zhq7_hh4cpc39zq4rgjcg";

    private static final String XO_SANDBOX_TOKENIZATION_KEY = "sandbox_rz48bqvw_jcyycfw6f9j4nj9c";

    private static SharedPreferences sharedPreferences;

    public static SharedPreferences getPreferences(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }

        return sharedPreferences;
    }

    public static String getEnvironment(Context context) {
        return getPreferences(context).getString(ENVIRONMENT, SANDBOX_ENV_NAME);
    }

    public static void setEnvironment(Context context, String environment) {
        getPreferences(context)
                .edit()
                .putString(ENVIRONMENT, environment)
                .apply();

        DemoApplication.resetApiClient();
    }

    public static String getSandboxUrl() {
        return SANDBOX_BASE_SERVER_URL;
    }

    public static String getEnvironmentUrl(Context context) {
        String environment = getEnvironment(context);
        if (SANDBOX_ENV_NAME.equals(environment)) {
            return SANDBOX_BASE_SERVER_URL;
        } else if (MOCKED_PAY_PAL_ENV_NAME.equals(environment)) {
            return MOCKED_PAY_PAL_SANDBOX_SERVER_URL;
        } else if (PRODUCTION_ENV_NAME.equals(environment)) {
            return PRODUCTION_BASE_SERVER_URL;
        } else {
            return "";
        }
    }

    public static String getAuthorizationType(Context context) {
        return getPreferences(context).getString("authorization_type", context.getString(R.string.client_token));
    }

    public static String getCustomerId(Context context) {
        return getPreferences(context).getString("customer", null);
    }

    public static String getMerchantAccountId(Context context) {
        return getPreferences(context).getString("merchant_account", null);
    }

    public static boolean shouldCollectDeviceData(Context context) {
        return getPreferences(context).getBoolean("collect_device_data", false);
    }

    public static String getThreeDSecureMerchantAccountId(Context context) {
        if (isThreeDSecureEnabled(context) && "production".equals(getEnvironment(context))) {
            return "test_AIB";
        } else {
            return null;
        }
    }

    public static String getUnionPayMerchantAccountId(Context context) {
        if ("sandbox".equals(getEnvironment(context))) {
            return "fake_switch_usd";
        } else {
            return null;
        }
    }

    public static boolean useTokenizationKey(Context context) {
        return getAuthorizationType(context).equals(context.getString(R.string.tokenization_key));
    }

    public static String getTokenizationKey(Context context) {
        String environment = getEnvironment(context);

        if (SANDBOX_ENV_NAME.equals(environment)) {
            return SANDBOX_TOKENIZATION_KEY;
        } else if (MOCKED_PAY_PAL_ENV_NAME.equals(environment)) {
            return MOCKED_PAY_PAL_SANDBOX_TOKENIZATION_KEY;
        } else if (PRODUCTION_ENV_NAME.equals(environment)) {
            return PRODUCTION_TOKENIZATION_KEY;
        } else {
            return null;
        }
    }

    public static String getPayPalCheckoutTokenizationKey(Context context) {
        String environment = getEnvironment(context);

        if (SANDBOX_ENV_NAME.equals(environment)) {
            return XO_SANDBOX_TOKENIZATION_KEY;
        } else if (PRODUCTION_ENV_NAME.equals(environment)) {
            return PRODUCTION_TOKENIZATION_KEY;
        } else {
            return null;
        }
    }

    public static String getLocalPaymentsTokenizationKey(Context context) {
        String environment = getEnvironment(context);

        if (SANDBOX_ENV_NAME.equals(environment)) {
            return LOCAL_PAYMENTS_TOKENIZATION_KEY;
        } else {
            return null;
        }
    }

    public static boolean areGooglePayPrepaidCardsAllowed(Context context) {
        return getPreferences(context).getBoolean("google_pay_allow_prepaid_cards", true);
    }

    public static boolean isGooglePayShippingAddressRequired(Context context) {
        return getPreferences(context).getBoolean("google_pay_require_shipping_address", false);
    }

    public static boolean isGooglePayBillingAddressRequired(Context context) {
        return getPreferences(context).getBoolean("google_pay_require_billing_address", false);
    }

    public static boolean isGooglePayPhoneNumberRequired(Context context) {
        return getPreferences(context).getBoolean("google_pay_require_phone_number", false);
    }

    public static boolean isGooglePayEmailRequired(Context context) {
        return getPreferences(context).getBoolean("google_pay_require_email", false);
    }

    public static String getGooglePayCurrency(Context context) {
        return getPreferences(context).getString("google_pay_currency", "EUR");
    }

    public static String getGooglePayMerchantId(Context context) {
        return getPreferences(context).getString("google_pay_merchant_id", "18278000977346790994");
    }

    public static List<String> getGooglePayAllowedCountriesForShipping(Context context) {
        String[] preference = getPreferences(context).getString("google_pay_allowed_countries_for_shipping", "US")
                .split(",");
        List<String> countries = new ArrayList<>();
        for(String country : preference) {
            countries.add(country.trim());
        }

        return countries;
    }

    public static String getPayPalIntentType(Context context) {
        return getPreferences(context).getString("paypal_intent_type", context.getString(R.string.paypal_intent_authorize));
    }

    public static String getPayPalDisplayName(Context context) {
        return getPreferences(context).getString("paypal_display_name", null);
    }

    public static String getPayPalLandingPageType(Context context) {
        return getPreferences(context).getString("paypal_landing_page_type", context.getString(R.string.none));
    }

    public static boolean isPayPalUseractionCommitEnabled(Context context) {
        return getPreferences(context).getBoolean("paypal_useraction_commit", false);
    }

    public static boolean isPayPalCreditOffered(Context context) {
        return getPreferences(context).getBoolean("paypal_credit_offered", false);
    }

    public static boolean isPayPalSignatureVerificationDisabled(Context context) {
        return getPreferences(context).getBoolean("paypal_disable_signature_verification", true);
    }

    public static boolean usePayPalAddressOverride(Context context) {
        return getPreferences(context).getBoolean("paypal_address_override", true);
    }

    public static boolean useHardcodedPayPalConfiguration(Context context) {
        return getPreferences(context).getBoolean("paypal_use_hardcoded_configuration", false);
    }

    public static String getPayPalLinkType(Context context) {
        return getPreferences(context).getString("paypal_link_type", context.getString(R.string.paypal_deep_link));
    }

    public static boolean isThreeDSecureEnabled(Context context) {
        return getPreferences(context).getBoolean("enable_three_d_secure", false);
    }

    public static boolean isThreeDSecureRequired(Context context) {
        return getPreferences(context).getBoolean("require_three_d_secure", true);
    }

    public static boolean vaultVenmo(Context context) {
        return getPreferences(context).getBoolean("vault_venmo", true);
    }

    public static boolean venmoFallbackToWeb(Context context) {
        return getPreferences(context).getBoolean("venmo_fallback_to_web", false);
    }

    public static boolean isAmexRewardsBalanceEnabled(Context context) {
        return getPreferences(context).getBoolean("amex_rewards_balance", false);
    }

    public static boolean isManualBrowserSwitchingEnabled(Context context) {
        return getPreferences(context).getBoolean("enable_manual_browser_switching", false);
    }

    public static boolean showCheckoutExperience(Context context) {
        return getPreferences(context).getBoolean("show_checkout_experience", false);
    }
}
