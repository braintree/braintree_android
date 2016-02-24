package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.Intent;
import android.net.Uri;

import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.Result;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.config.ConfigManager;
import com.paypal.android.sdk.onetouch.core.config.OtcConfiguration;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.exception.InvalidEncryptionDataException;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class BrowserSwitchHelper {

    public static Intent getBrowserSwitchIntent(ContextInspector contextInspector,
            ConfigManager configManager, Request request) {
        OtcConfiguration configuration = configManager.getConfig();
        try {
            String url = request.getBrowserSwitchUrl(contextInspector.getContext(), configuration);

            request.persistRequiredFields(contextInspector);

            Recipe<?> recipe = request.getBrowserSwitchRecipe(configuration);

            for (String allowedBrowserPackage : recipe.getTargetPackagesInReversePriorityOrder()) {
                boolean canIntentBeResolved = Recipe.isValidBrowserTarget(contextInspector.getContext(), url,
                        allowedBrowserPackage);
                if (canIntentBeResolved) {
                    request.trackFpti(contextInspector.getContext(), TrackingPoint.SwitchToBrowser,
                            recipe.getProtocol());
                    return Recipe.getBrowserIntent(url, allowedBrowserPackage);
                }
            }
        } catch (CertificateException | UnsupportedEncodingException | NoSuchPaddingException
                | NoSuchAlgorithmException | IllegalBlockSizeException | JSONException
                | BadPaddingException | InvalidEncryptionDataException
                | InvalidKeyException ignored) {}

        return null;
    }

    public static Result parseBrowserSwitchResponse(ContextInspector contextInspector,
            Request request, Uri uri) {
        Result result = request.parseBrowserResponse(contextInspector, uri);
        switch (result.getResultType()) {
            case Error:
                request.trackFpti(contextInspector.getContext(), TrackingPoint.Error, null);
                break;
            case Cancel:
                request.trackFpti(contextInspector.getContext(), TrackingPoint.Cancel, null);
                break;
            case Success:
                request.trackFpti(contextInspector.getContext(), TrackingPoint.Return, null);
                break;
        }
        return result;
    }
}
