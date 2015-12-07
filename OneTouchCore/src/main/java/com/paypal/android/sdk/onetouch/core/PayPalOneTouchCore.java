package com.paypal.android.sdk.onetouch.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.paypal.android.networking.EnvironmentManager;
import com.paypal.android.networking.PayPalEnvironment;
import com.paypal.android.networking.ServerInterface;
import com.paypal.android.networking.bus.RequestRouter;
import com.paypal.android.networking.processing.NetworkRequestProcessor;
import com.paypal.android.networking.processing.RequestExecutorThread;
import com.paypal.android.networking.processing.RequestProcessor;
import com.paypal.android.networking.request.ServerRequest;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.base.CoreEnvironment;
import com.paypal.android.sdk.onetouch.core.base.SdkRiskComponent;
import com.paypal.android.sdk.onetouch.core.config.ConfigManager;
import com.paypal.android.sdk.onetouch.core.config.OtcConfiguration;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;
import com.paypal.android.sdk.onetouch.core.exception.InvalidEncryptionDataException;
import com.paypal.android.sdk.onetouch.core.fpti.FptiManager;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;
import com.paypal.android.sdk.onetouch.core.network.ConfigFileRequest;
import com.paypal.android.sdk.onetouch.core.network.OtcApiName;
import com.paypal.android.sdk.onetouch.core.network.OtcEnvironment;
import com.paypal.android.sdk.onetouch.core.network.OtcMockRequestProcessor;
import com.paypal.android.sdk.onetouch.core.sdk.V1WalletHelper;
import com.paypal.android.sdk.onetouch.core.sdk.V2WalletHelper;
import com.squareup.okhttp.Interceptor;

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
    private static final int NETWORK_TIMEOUT = 90000;
    private static final int MOCK_NETWORK_DELAY_IN_MS = 1000;

    private static ServerInterface sServerInterface;
    private static ConfigManager sConfigManager;
    private static ContextInspector sContextInspector;
    private static FptiManager sFptiManager;

    /**
     * Must be called from Main thread!
     */
    public static void useHardcodedConfig(Context context, boolean useHardcodedConfig) {
        initService(context);
        sConfigManager.useHardcodedConfig(useHardcodedConfig);
    }

    /**
     * Must be called from Main thread!
     */
    public static FptiManager getFptiManager(Context context) {
        initService(context);
        return sFptiManager;
    }

    private static class OtcRequestRouter implements RequestRouter {
        private final ConfigManager mConfigManager;

        public OtcRequestRouter(ConfigManager configManager) {
            this.mConfigManager = configManager;
        }

        @Override
        public void route(ServerRequest serverRequest) {
            if (serverRequest instanceof ConfigFileRequest) {
                if (serverRequest.isSuccess()) {
                    ConfigFileRequest configFileRequest = (ConfigFileRequest) serverRequest;

                    this.mConfigManager.updateConfig(configFileRequest.minifiedJson, false);
                    // everything checks out.  Store original json file in prefs (since you can't store complex objects)
                }
            }
        }
    }

    /**
     * Must be called from Main thread!
     */
    private static void initService(Context context) {
        if (null == sServerInterface) {
            PayPalEnvironment env =
                    getEnvironment(getEnvironmentName(), "https://api-m.paypal.com/v1/");

            sServerInterface =
                    new ServerInterface(getContextInspector(context), env, getCoreEnvironment());

            sConfigManager = new ConfigManager(getContextInspector(context), sServerInterface,
                    getCoreEnvironment());

            // Create Request Processor based on Mock mode or not
            RequestProcessor requestProcessor = EnvironmentManager.isMock(getEnvironmentName()) ?
                    new OtcMockRequestProcessor(MOCK_NETWORK_DELAY_IN_MS, sServerInterface) :
                    new NetworkRequestProcessor(getContextInspector(context), getEnvironmentName(),
                            getCoreEnvironment(),
                            sServerInterface, NETWORK_TIMEOUT, true,
                            Collections.<Interceptor>emptyList());
            ;

            RequestExecutorThread mExecutor = new RequestExecutorThread(sServerInterface,
                    requestProcessor);

            sServerInterface.setExecutor(mExecutor);

            // register listener immediately before touching config for the first time
            sServerInterface.register(new OtcRequestRouter(sConfigManager));

            sFptiManager = new FptiManager(sServerInterface, getCoreEnvironment(),
                    getContextInspector(context));
        }

        // always touch config
        sConfigManager.touchConfig();
    }

    /**
     * Return true if the modern wallet app is installed (one that has either v1 or v2 touch
     * intents). Returns false if the wallet app is older than the touch releases, or not present.
     * <p>
     * Must be called from Main thread!
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
     * <p>
     * Must be called from Main thread!
     */
    public static RequestTarget preflightRequest(Context context, Request request,
            boolean enableSecurityCheck) {
        initService(context);

        // calling this method functionally does nothing, but ensures that we send off FPTI data about wallet installs.
        isWalletAppInstalled(context, enableSecurityCheck);

        Recipe recipe =
                request.getRecipeToExecute(context, getConfig(context), enableSecurityCheck);

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
     * <p>
     * Must be called from Main thread!
     *
     * @return true if the request was started successfully or false if PayPal authentication was
     * not possible
     */
    public static PerformRequestStatus performRequest(Activity activity, Request request,
            int requestCode, boolean enableSecurityCheck,
            BrowserSwitchAdapter browserSwitchAdapter) {
        initService(activity);

        // calling this method functionally does nothing, but ensures that we send off FPTI data about wallet installs.
        isWalletAppInstalled(activity, enableSecurityCheck);

        Recipe recipe =
                request.getRecipeToExecute(activity, getConfig(activity), enableSecurityCheck);

        PerformRequestStatus status;
        if (null == recipe) {
            status = new PerformRequestStatus(false, null, null);
        } else {

            // Set CMID for Single Payment and Billing Agreements
            if (request.getClass() == BillingAgreementRequest.class) {
                request.clientMetadataId(PayPalOneTouchCore.getClientMetadataId(activity,
                        ((BillingAgreementRequest) request).getPairingId()));
            } else if (request.getClass() == CheckoutRequest.class) {
                request.clientMetadataId(PayPalOneTouchCore
                        .getClientMetadataId(activity, ((CheckoutRequest) request).getPairingId()));
            }

            if (RequestTarget.wallet == recipe.getTarget()) {
                request.trackFpti(activity, TrackingPoint.SwitchToWallet, recipe.getProtocol());
                PayPalOneTouchActivity.Start(activity, requestCode, request, recipe.getProtocol());
                status = new PerformRequestStatus(true, RequestTarget.wallet,
                        request.getClientMetadataId());
            } else {
                Intent browserIntent = getBrowserIntent(activity, request);

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

    public static Intent getStartIntent(Activity activity,
            Request request,
            boolean enableSecurityCheck) {
        initService(activity);

        // calling this method functionally does nothing, but ensures that we send off FPTI data about wallet installs.
        isWalletAppInstalled(activity, enableSecurityCheck);

        Recipe recipe =
                request.getRecipeToExecute(activity, getConfig(activity), enableSecurityCheck);

        if (null != recipe) {
            // Set CMID for Single Payment and Billing Agreements
            if (request.getClass() == BillingAgreementRequest.class) {
                request.clientMetadataId(PayPalOneTouchCore.getClientMetadataId(activity,
                        ((BillingAgreementRequest) request).getPairingId()));
            } else if (request.getClass() == CheckoutRequest.class) {
                request.clientMetadataId(PayPalOneTouchCore
                        .getClientMetadataId(activity, ((CheckoutRequest) request).getPairingId()));
            }

            if (RequestTarget.wallet == recipe.getTarget()) {
                request.trackFpti(activity, TrackingPoint.SwitchToWallet, recipe.getProtocol());
                return PayPalOneTouchActivity
                        .getStartIntent(activity, request, recipe.getProtocol());
            } else {
                return getBrowserIntent(activity, request);
            }
        }
        return null;
    }

    /**
     * Return the browser launch intent
     * <p>
     * Must be called from Main thread!
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

    /**
     * Must be called from Main thread!
     *
     * @return
     */
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
    public static final String getClientMetadataId(Context context) {
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
    public static final String getClientMetadataId(Context context, String pairingId) {
        Log.d(TAG, "getClientMetadataId(pairingId)");
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
     * <p>
     * Must be called from Main thread!
     */
    static OtcConfiguration getConfig(Context context) {
        initService(context);
        return sConfigManager.getConfig();
    }

    /**
     * Lazily creates and returns a sContextInspector object
     */
    static synchronized ContextInspector getContextInspector(Context context) {
        if (null == sContextInspector) {
            sContextInspector = new ContextInspector(context, getCoreEnvironment().getPrefsFile());
        }
        return sContextInspector;
    }

    private static CoreEnvironment getCoreEnvironment() {
        return new OtcEnvironment();
    }

    private static String getEnvironmentName() {
        return EnvironmentManager.LIVE;
    }

    private static PayPalEnvironment getEnvironment(String environmentName, String baseUrl) {
        PayPalEnvironment env = new PayPalEnvironment(environmentName, baseUrl);

        // mock environments will not have endpoints
        // There should always be a check isMockEnabled before using any mocked endpoint.
        if (baseUrl != null) {
            if (!baseUrl.startsWith("https://")) {
                throw new RuntimeException(baseUrl + " does not start with 'https://', ignoring "
                        + environmentName);
            }

            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            for (OtcApiName apiInfo : OtcApiName.getAllValues()) {
                String url;
                if (apiInfo.isOverrideBaseUrl()) {
                    url = apiInfo.getUrl();
                } else {
                    url = baseUrl + apiInfo.getUrl();
                }
                env.getEndpoints().put(apiInfo.getName(), url);
            }
        }

        return env;
    }

}
