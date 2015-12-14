package com.paypal.android.sdk.onetouch.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.base.SdkRiskComponent;
import com.paypal.android.sdk.onetouch.core.config.ConfigManager;
import com.paypal.android.sdk.onetouch.core.config.OtcConfiguration;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;
import com.paypal.android.sdk.onetouch.core.exception.InvalidEncryptionDataException;
import com.paypal.android.sdk.onetouch.core.fpti.FptiManager;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;
import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;
import com.paypal.android.sdk.onetouch.core.network.PayPalHttpClient;
import com.paypal.android.sdk.onetouch.core.sdk.V1WalletHelper;
import com.paypal.android.sdk.onetouch.core.sdk.V2WalletHelper;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Central class for One Touch functionality.
 * <p>
 * Note that all public methods should call initService which starts the config file querying
 */
public class PayPalOneTouchCore {

    private static final String TAG = PayPalOneTouchCore.class.getSimpleName();
    private static final ExecutorService OTC_EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private static ContextInspector sContextInspector;
    private static ConfigManager sConfigManager;
    private static FptiManager sFptiManager;

    public static void useHardcodedConfig(Context context, boolean useHardcodedConfig) {
        initService(context);
        sConfigManager.useHardcodedConfig(useHardcodedConfig);
    }

    public static FptiManager getFptiManager(Context context) {
        initService(context);
        return sFptiManager;
    }

    private static void initService(Context context) {
        if (sConfigManager == null || sFptiManager == null) {
            PayPalHttpClient httpClient = new PayPalHttpClient()
                    .setBaseUrl(EnvironmentManager.LIVE_API_M_ENDPOINT);
            sConfigManager = new ConfigManager(getContextInspector(context), httpClient);
            sFptiManager = new FptiManager(getContextInspector(context), httpClient);
        }

        // always refresh configuration
        sConfigManager.refreshConfiguration();
    }

    /**
     * Return true if the modern wallet app is installed (one that has either v1 or v2 touch
     * intents). Returns false if the wallet app is older than the touch releases, or not present.
     */
    public static boolean isWalletAppInstalled(Context context, boolean enableSecurityCheck) {
        initService(context);

        boolean isV2WalletAppInstalled = new V2WalletHelper()
                .isValidV2TouchAuthenticatorInstalled(context, enableSecurityCheck);
        sFptiManager.trackFpti((isV2WalletAppInstalled) ? TrackingPoint.WalletIsPresent :
                        TrackingPoint.WalletIsAbsent, ""/*no environment set yet*/,
                Collections.<String, String>emptyMap(), Protocol.v2);

        boolean isV1WalletAppInstalled = false;
        if (!isV2WalletAppInstalled) {
            isV1WalletAppInstalled = new V1WalletHelper()
                    .isValidV1TouchAuthenticatorInstalled(context, enableSecurityCheck);
            sFptiManager.trackFpti((isV1WalletAppInstalled) ? TrackingPoint.WalletIsPresent :
                            TrackingPoint.WalletIsAbsent, ""/*no environment set yet*/,
                    Collections.<String, String>emptyMap(), Protocol.v1);
        }
        return isV2WalletAppInstalled || isV1WalletAppInstalled;
    }

    /**
     * Returns the expected target of the request, or null if none can handle. Note that there is a
     * very small possibility that between the time you do this preflight and the time that the
     * request is actually performed, OTC might receive updated configuration information from
     * PayPal's servers which will change this outcome.
     */
    public static RequestTarget preflightRequest(Context context, Request request,
            boolean enableSecurityCheck) {
        initService(context);

        // calling this method functionally does nothing, but ensures that we send off FPTI data about wallet installs
        isWalletAppInstalled(context, enableSecurityCheck);

        Recipe recipe = request.getRecipeToExecute(context, getConfig(context),
                enableSecurityCheck);

        if (null != recipe) {
            if (RequestTarget.browser == recipe.getTarget()) {
                request.trackFpti(context, TrackingPoint.PreflightBrowser, recipe.getProtocol());
            } else if (RequestTarget.wallet == recipe.getTarget()) {
                request.trackFpti(context, TrackingPoint.PreflightWallet, recipe.getProtocol());
            } else {
                // will never happen - only two enums possible
            }
            return recipe.getTarget();
        } else {
            request.trackFpti(context, TrackingPoint.PreflightNone, null);
            return null;
        }
    }

    /**
     * Attempt to start a PayPal authentication request using the best possible authentication
     * mechanism, wallet or browser
     *
     * @return true if the request was started successfully or false if PayPal authentication was
     * not possible
     */
    public static PerformRequestStatus performRequest(Activity activity, Request request,
            int requestCode, boolean enableSecurityCheck,
            BrowserSwitchAdapter browserSwitchAdapter) {
        Context context = activity.getApplicationContext();
        initService(context);

        // calling this method functionally does nothing, but ensures that we send off FPTI data about wallet installs.
        isWalletAppInstalled(context, enableSecurityCheck);

        Recipe recipe = request.getRecipeToExecute(context, getConfig(context),
                enableSecurityCheck);

        PerformRequestStatus status;
        if (null == recipe) {
            status = new PerformRequestStatus(false, null, null);
        } else {

            // Set CMID for Single Payment and Billing Agreements
            if (request.getClass() == BillingAgreementRequest.class) {
                request.clientMetadataId(PayPalOneTouchCore.getClientMetadataId(context,
                        ((BillingAgreementRequest) request).getPairingId()));
            } else if (request.getClass() == CheckoutRequest.class) {
                request.clientMetadataId(PayPalOneTouchCore.getClientMetadataId(context,
                        ((CheckoutRequest) request).getPairingId()));
            }

            if (RequestTarget.wallet == recipe.getTarget()) {
                request.trackFpti(context, TrackingPoint.SwitchToWallet, recipe.getProtocol());
                PayPalOneTouchActivity.Start(activity, requestCode, request, recipe.getProtocol());
                status = new PerformRequestStatus(true, RequestTarget.wallet,
                        request.getClientMetadataId());
            } else {
                Intent browserIntent = getBrowserIntent(context, request);

                if (browserIntent != null) {
                    browserSwitchAdapter.handleBrowserSwitchIntent(browserIntent);

                    status = new PerformRequestStatus(true, RequestTarget.browser,
                            request.getClientMetadataId());
                } else {
                    status = new PerformRequestStatus(false, RequestTarget.browser,
                            request.getClientMetadataId());
                }
            }
        }

        return status;
    }

    public static Intent getStartIntent(Activity activity, Request request,
            boolean enableSecurityCheck) {
        Context context = activity.getApplicationContext();
        initService(context);

        // calling this method functionally does nothing, but ensures that we send off FPTI data about wallet installs.
        isWalletAppInstalled(context, enableSecurityCheck);

        Recipe recipe = request.getRecipeToExecute(context, getConfig(context),
                enableSecurityCheck);

        if (null != recipe) {
            // Set CMID for Single Payment and Billing Agreements
            if (request.getClass() == BillingAgreementRequest.class) {
                request.clientMetadataId(PayPalOneTouchCore.getClientMetadataId(context,
                        ((BillingAgreementRequest) request).getPairingId()));
            } else if (request.getClass() == CheckoutRequest.class) {
                request.clientMetadataId(PayPalOneTouchCore.getClientMetadataId(context,
                        ((CheckoutRequest) request).getPairingId()));
            }

            if (RequestTarget.wallet == recipe.getTarget()) {
                request.trackFpti(context, TrackingPoint.SwitchToWallet, recipe.getProtocol());
                return PayPalOneTouchActivity.getStartIntent(activity, request,
                        recipe.getProtocol());
            } else {
                return getBrowserIntent(context, request);
            }
        }
        return null;
    }

    /**
     * Return the browser launch intent
     *
     * @return
     */
    static Intent getBrowserIntent(Context context, Request request) {
        OtcConfiguration configuration = getConfig(context);

        try {
            String url = request.getBrowserSwitchUrl(context, configuration);

            request.persistRequiredFields(getContextInspector(context));

            Recipe<?> recipe = request.getBrowserSwitchRecipe(configuration);

            for (String allowedBrowserPackage : recipe.getTargetPackagesInReversePriorityOrder()) {
                boolean canIntentBeResolved =
                        recipe.isValidBrowserTarget(context, url, allowedBrowserPackage);

                if (canIntentBeResolved) {
                    request.trackFpti(context, TrackingPoint.SwitchToBrowser, recipe.getProtocol());
                    return Recipe.getBrowserIntent(url, allowedBrowserPackage);
                }
            }

        } catch (CertificateException | UnsupportedEncodingException | NoSuchPaddingException | NoSuchAlgorithmException
                | IllegalBlockSizeException | JSONException | BadPaddingException | InvalidEncryptionDataException | InvalidKeyException
                | InvalidKeySpecException e) {
            Log.e(TAG, "cannot create browser switch URL", e);
        }

        return null;
    }

    public static Result handleBrowserResponse(Context context, Uri uri, Request request) {
        initService(context);
        Result result = request.parseBrowserResponse(getContextInspector(context), uri);
        switch (result.getResultType()) {
            case Error:
                request.trackFpti(context, TrackingPoint.Error, null);
                break;
            case Cancel:
                request.trackFpti(context, TrackingPoint.Cancel, null);
                break;
            case Success:
                request.trackFpti(context, TrackingPoint.Return, null);
                break;
        }
        return result;
    }

    /**
     * Gets a Client Metadata ID at the time of future payment activity. Once a user has consented
     * to future payments, when the user subsequently initiates a PayPal payment from their device
     * to be completed by your server, PayPal uses a Correlation ID to verify that the payment is
     * originating from a valid, user-consented device+application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context The application context
     * @return The applicationCorrelationID - Your server will send this to PayPal in a
     * 'PayPal-Client-Metadata-Id' header.
     */
    public static String getClientMetadataId(Context context) {
        return SdkRiskComponent.getClientMetadataId(OTC_EXECUTOR_SERVICE, context,
                getContextInspector(context).getInstallationGUID(),
                BuildConfig.PRODUCT_VERSION,
                null);
    }

    /**
     * Gets a Client Metadata ID at the time of future payment activity. Once a user has consented
     * to future payments, when the user subsequently initiates a PayPal payment from their device
     * to be completed by your server, PayPal uses a Correlation ID to verify that the payment is
     * originating from a valid, user-consented device+application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context The application context
     * @param pairingId The desired pairing id
     * @return The applicationCorrelationID - Your server will send this to PayPal in a
     * 'PayPal-Client-Metadata-Id' header.
     */
    public static String getClientMetadataId(Context context, String pairingId) {
        return SdkRiskComponent.getClientMetadataId(OTC_EXECUTOR_SERVICE, context,
                getContextInspector(context).getInstallationGUID(),
                BuildConfig.PRODUCT_VERSION,
                pairingId);
    }

    /**
     * @return The version of the SDK library in use. Version numbering follows http://semver.org/.
     * <p>
     * Please be sure to include this library version in tech support requests.
     */
    public static String getLibraryVersion() {
        return BuildConfig.PRODUCT_VERSION;
    }

    /**
     * Returns the currently active config.
     */
    static OtcConfiguration getConfig(Context context) {
        initService(context);
        return sConfigManager.getConfig();
    }

    /**
     * Lazily creates and returns a sContextInspector object
     */
    static ContextInspector getContextInspector(Context context) {
        if (null == sContextInspector) {
            sContextInspector = new ContextInspector(context);
        }
        return sContextInspector;
    }
}
