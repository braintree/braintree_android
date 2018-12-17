package com.braintreepayments.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    protected static final String ENVIRONMENT = "environment";

    private static final String VERSION = "version";

    private static final String SANDBOX_BASE_SERVER_URL = "https://braintree-sample-merchant.herokuapp.com";
    private static final String PRODUCTION_BASE_SERVER_URL = "https://executive-sample-merchant.herokuapp.com";

    private static final String SANDBOX_TOKENIZATION_KEY = "sandbox_tmxhyf7d_dcpspy2brwdjr3qn";
    private static final String PRODUCTION_TOKENIZATION_KEY = "production_t2wns2y2_dfy45jdj3dxkmz5m";

    private static SharedPreferences sSharedPreferences;

    public static SharedPreferences getPreferences(Context context) {
        if (sSharedPreferences == null) {
            sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        }

        return sSharedPreferences;
    }

    public static int getEnvironment(Context context) {
        return getPreferences(context).getInt(ENVIRONMENT, 0);
    }

    public static void setEnvironment(Context context, int environment) {
        getPreferences(context)
                .edit()
                .putInt(ENVIRONMENT, environment)
                .apply();

        DemoApplication.resetApiClient();
    }

    public static String getSandboxUrl() {
        return SANDBOX_BASE_SERVER_URL;
    }

    public static String getEnvironmentUrl(Context context) {
        int environment = getEnvironment(context);
        if (environment == 0) {
            return SANDBOX_BASE_SERVER_URL;
        } else if (environment == 1) {
            return PRODUCTION_BASE_SERVER_URL;
        } else {
            return "";
        }
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
        if (isThreeDSecureEnabled(context) && getEnvironment(context) == 1) {
            return "test_AIB";
        } else {
            return null;
        }
    }

    public static String getUnionPayMerchantAccountId(Context context) {
        if (getEnvironment(context) == 0) {
            return "fake_switch_usd";
        } else {
            return null;
        }
    }

    public static boolean useTokenizationKey(Context context) {
        return getPreferences(context).getBoolean("tokenization_key", false);
    }

    public static String getEnvironmentTokenizationKey(Context context) {
        int environment = getEnvironment(context);
        if (environment == 0) {
            return SANDBOX_TOKENIZATION_KEY;
        } else if (environment == 1) {
            return PRODUCTION_TOKENIZATION_KEY;
        } else {
            return "";
        }
    }

    public static String getEnvironmentTokenizationKeyForLocalPayment(Context context) {
        int environment = getEnvironment(context);
        if (environment == 0) {
            return "sandbox_f252zhq7_hh4cpc39zq4rgjcg";
        } else if (environment == 1) {
            return PRODUCTION_TOKENIZATION_KEY;
        } else {
            return "";
        }
    }

    public static boolean isAndroidPayShippingAddressRequired(Context context) {
        return getPreferences(context).getBoolean("android_pay_require_shipping_address", false);
    }

    public static boolean isAndroidPayPhoneNumberRequired(Context context) {
        return getPreferences(context).getBoolean("android_pay_require_phone_number", false);
    }

    public static String getAndroidPayCurrency(Context context) {
        return getPreferences(context).getString("android_pay_currency", "USD");
    }

    public static String[] getAndroidPayAllowedCountriesForShipping(Context context) {
        String[] countries = getPreferences(context).getString("android_pay_allowed_countries_for_shipping", "US").split(",");
        for(int i = 0; i < countries.length; i++) {
            countries[i] = countries[i].trim();
        }

        return countries;
    }

    public static boolean areGooglePaymentPrepaidCardsAllowed(Context context) {
        return getPreferences(context).getBoolean("google_payment_allow_prepaid_cards", true);
    }

    public static boolean isGooglePaymentShippingAddressRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_shipping_address", false);
    }

    public static boolean isGooglePaymentBillingAddressRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_billing_address", false);
    }

    public static boolean isGooglePaymentPhoneNumberRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_phone_number", false);
    }

    public static boolean isGooglePaymentEmailRequired(Context context) {
        return getPreferences(context).getBoolean("google_payment_require_email", false);
    }

    public static String getGooglePaymentCurrency(Context context) {
        return getPreferences(context).getString("google_payment_currency", "USD");
    }

    public static List<String> getGooglePaymentAllowedCountriesForShipping(Context context) {
        String[] preference = getPreferences(context).getString("google_payment_allowed_countries_for_shipping", "US")
                .split(",");
        List<String> countries = new ArrayList<>();
        for(String country : preference) {
            countries.add(country.trim());
        }

        return countries;
    }

    public static boolean isPayPalAddressScopeRequested(Context context) {
        return getPreferences(context).getBoolean("paypal_request_address_scope", false);
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

    public static boolean isThreeDSecureEnabled(Context context) {
        return getPreferences(context).getBoolean("enable_three_d_secure", false);
    }

    public static boolean isThreeDSecureRequired(Context context) {
        return getPreferences(context).getBoolean("require_three_d_secure", true);
    }

    public static boolean vaultVenmo(Context context) {
        return getPreferences(context).getBoolean("vault_venmo", true);
    }

    public static boolean isAmexRewardsBalanceEnabled(Context context) {
        return getPreferences(context).getBoolean("amex_rewards_balance", false);
    }
}
