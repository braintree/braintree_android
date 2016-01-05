package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.Result;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.base.DeviceInspector;
import com.paypal.android.sdk.onetouch.core.config.ConfigManager;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.enums.ResponseType;
import com.paypal.android.sdk.onetouch.core.exception.ResponseParsingException;
import com.paypal.android.sdk.onetouch.core.exception.WalletSwitchException;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;
import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class AppSwitchHelper {

    public static Intent getAppSwitchIntent(ContextInspector contextInspector,
            ConfigManager configManager, Request request, Recipe recipe) {
        Intent intent = new Intent(recipe.getTargetIntentAction());
        intent.setComponent(ComponentName.unflattenFromString(WalletHelper.WALLET_APP_PACKAGE + "/"
                + recipe.getTargetComponent()));
        intent.setPackage(WalletHelper.WALLET_APP_PACKAGE);
        intent.putExtra("version", recipe.getProtocol().getVersion());

        // app_guid now present on all v1/v2 requests.  Deemed not sensitive.
        intent.putExtra("app_guid", contextInspector.getInstallationGUID());
        intent.putExtra("client_metadata_id", request.getClientMetadataId());
        intent.putExtra("client_id", request.getClientId());
        // Roman confirmed this is correct, but is not ever read from the app.
        intent.putExtra("app_name",
                DeviceInspector.getApplicationInfoName(contextInspector.getContext()));
        intent.putExtra("environment", request.getEnvironment());
        intent.putExtra("environment_url",
                EnvironmentManager.getEnvironmentUrl(request.getEnvironment()));

        if (request instanceof AuthorizationRequest) {
            AuthorizationRequest authorizationRequest = (AuthorizationRequest) request;
            intent.putExtra("scope", authorizationRequest.getScopeString());
            intent.putExtra("response_type", "code");
            intent.putExtra("privacy_url", authorizationRequest.getPrivacyUrl());
            intent.putExtra("agreement_url", authorizationRequest.getUserAgreementUrl());
        } else {
            CheckoutRequest checkoutRequest = (CheckoutRequest) request;
            intent.putExtra("response_type", "web");
            String webURL = checkoutRequest.getBrowserSwitchUrl(contextInspector.getContext(),
                    configManager.getConfig());
            intent.putExtra("webURL", webURL);

            // only checkoutRequest actually cares about correlating request/response, since there's no v3 consent support.
            checkoutRequest.persistRequiredFields(contextInspector);
        }

        return intent;
    }

    public static Result parseAppSwitchResponse(ContextInspector contextInspector, Request request,
            Intent data) {
        Bundle bundle = data.getExtras();
        if (request.validateV1V2Response(contextInspector, bundle)) {
            request.trackFpti(contextInspector.getContext(), TrackingPoint.Return, null);
            return processResponseIntent(bundle);
        } else {
            if (bundle.containsKey("error")) {
                request.trackFpti(contextInspector.getContext(), TrackingPoint.Error, null);
                return new Result(new WalletSwitchException(bundle.getString("error")));
            } else {
                request.trackFpti(contextInspector.getContext(), TrackingPoint.Error, null);
                return new Result(new ResponseParsingException("invalid wallet response"));
            }
        }
    }

    private static Result processResponseIntent(Bundle bundle) {
        String error = bundle.getString("error");
        if (!TextUtils.isEmpty(error)) {
            return new Result(new WalletSwitchException(error));
        } else {
            String environment = bundle.getString("environment");
            String bundleResponseType = bundle.getString("response_type").toLowerCase(Locale.US);
            ResponseType response_type;
            if ("code".equals(bundleResponseType)) {
                response_type = ResponseType.authorization_code;
            } else {
                response_type = ResponseType.web;
            }

            try {
                if (ResponseType.web == response_type) {
                    String webURL = bundle.getString("webURL");
                    return new Result(environment, response_type,
                            new JSONObject().put("webURL", webURL), null); // email not sent back in checkout requests since Hermes doesn't return that info
                } else {
                    String authorization_code = bundle.getString("authorization_code");
                    String email = bundle.getString("email");
                    return new Result(environment, response_type,
                            new JSONObject().put("code", authorization_code), email);
                }
            } catch (JSONException e) {
                return new Result(new ResponseParsingException(e));
            }
        }
    }
}
