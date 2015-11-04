/*
 * Copyright (c) 2015 PayPal, Inc.
 *
 * All rights reserved.
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */

package com.paypal.android.sdk.onetouch.core;

import com.paypal.android.sdk.onetouch.core.base.Constants;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.sdk.ThemeManifestValidator;
import com.paypal.android.sdk.onetouch.core.sdk.V1WalletHelper;
import com.paypal.android.sdk.onetouch.core.sdk.V2WalletHelper;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;
import com.paypal.android.sdk.onetouch.core.network.OtcEnvironment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class PayPalOneTouchActivity extends Activity {
    private static final String TAG = PayPalOneTouchActivity.class.getSimpleName();

    private static final String EXTRA_REQUEST = "com.paypal.android.sdk.onetouch.core.EXTRA_REQUEST";
    private static final String EXTRA_PROTOCOL = "com.paypal.android.sdk.onetouch.core.EXTRA_PROTOCOL";

    /**
     * Parcelable Extra containing a {@link Result} in the activity result
     */
    public static final String EXTRA_ONE_TOUCH_RESULT =
            "com.paypal.android.sdk.onetouch.core.EXTRA_ONE_TOUCH_RESULT";

    private static final int V1_TOUCH_AUTHENTICATOR_REQUEST_CODE = 1;
    private static final int V2_TOUCH_AUTHENTICATOR_REQUEST_CODE = 2;

    private boolean mIsFirstInstantiation;
    private ContextInspector mContextInspector;
    private Request mPaypalRequest;

    /**
     * start the OneTouch activity for results
     *
     * @param activity the Activity who will receive the results
     */
    public static void Start(Activity activity, int requestCode, Request paypalRequest,
                             Protocol protocol) {
        Log.i(TAG, "Start()");

        Intent intent = new Intent(activity, PayPalOneTouchActivity.class);
        intent.putExtras(activity.getIntent());
        intent.putExtra(EXTRA_REQUEST, paypalRequest);
        intent.putExtra(EXTRA_PROTOCOL, protocol);

        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        mContextInspector = new ContextInspector(getApplicationContext(), new OtcEnvironment().getPrefsFile());

        if (savedInstanceState == null) {
            // only validate on first instantiation
            new ThemeManifestValidator(this).validateTheme(PayPalOneTouchActivity.class);
            mIsFirstInstantiation = true;
        } else {
            mIsFirstInstantiation = false;
        }

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setTheme(android.R.style.Theme_Translucent_NoTitleBar);

        if (null == getIntent().getExtras()) {
            // extras should never be null in this activity.
            // This is some weird android state that we handle by just going back.
            onBackPressed();
            return;
        }

        mPaypalRequest = getIntent().getParcelableExtra(EXTRA_REQUEST);
        Protocol protocol =
                ( Protocol ) getIntent().getSerializableExtra(EXTRA_PROTOCOL);

        if (mIsFirstInstantiation) {
            doAuthenticatorRequest(mPaypalRequest, protocol);
        }
    }

    private void doAuthenticatorRequest(Request request, Protocol protocol) {
        Intent intent;
        int requestCode;

        if(Protocol.v1 == protocol) {
            intent = new V1WalletHelper().getPayPalTouchIntent();
            intent.putExtra("version", "1.0");
            requestCode = V1_TOUCH_AUTHENTICATOR_REQUEST_CODE;
        } else {
            intent = new V2WalletHelper().getPayPalTouchIntent();
            intent.putExtra("version", "2.0");
            requestCode = V2_TOUCH_AUTHENTICATOR_REQUEST_CODE;
        }

        // app_guid now present on all v1/v2 requests.  Deemed not sensitive.
        intent.putExtra("app_guid", mContextInspector.getInstallationGUID());
        intent.putExtra("client_metadata_id", request.getClientMetadataId());

        intent.putExtra("client_id", request.getClientId());
        // Roman confirmed this is correct, but is not ever read from the app.
        intent.putExtra("app_name", mContextInspector.getApplicationInfoName());
        intent.putExtra("environment", request.getEnvironment());
        intent.putExtra("environment_url", request.getEnvironmentUrl());

        if(request instanceof AuthorizationRequest) {
            AuthorizationRequest authorizationRequest = (AuthorizationRequest) request;
            intent.putExtra("scope", authorizationRequest.getScopeString());
            intent.putExtra("response_type", "code");
            intent.putExtra("privacy_url", authorizationRequest.getPrivacyUrl());
            intent.putExtra("agreement_url",
                    authorizationRequest.getUserAgreementUrl());

            // public logging for debugging
            Log.w(Constants.PUBLIC_TAG, "requesting " + intent.getStringExtra("response_type") +
                    " with scope:\"" + intent.getStringExtra("scope") + "\" from Authenticator.");
        } else {
            CheckoutRequest checkoutRequest = (CheckoutRequest) request;
            intent.putExtra("response_type", "web");
            String webURL = checkoutRequest.getBrowserSwitchUrl(getApplicationContext(), PayPalOneTouchCore.getConfig(getApplicationContext()));
            intent.putExtra("webURL", webURL);

            // only checkoutRequest actually cares about correlating request/response, since there's no v3 consent support.
            checkoutRequest.persistRequiredFields(mContextInspector);

            // public logging for debugging
            Log.w(Constants.PUBLIC_TAG, "requesting " + intent.getStringExtra("response_type") +
                    " with webURL\"" + intent.getStringExtra("webURL") + "\" from Authenticator.");
        }

        Log.i(TAG, "startActivityForResult(" + intent + ", " + requestCode + ") extras: " + intent.getExtras());

        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data +")");

        TrackingPoint pointToTrack;
        Result result;

        if(null != data) {
            logAllKeys(data.getExtras());
        }
        switch (requestCode) {
            case V1_TOUCH_AUTHENTICATOR_REQUEST_CODE:
                // fallthrough
            case V2_TOUCH_AUTHENTICATOR_REQUEST_CODE:
                if (null != data
                        && null != data.getExtras()
                        && !data.getExtras().isEmpty()){

                    Bundle bundle = data.getExtras();
                    boolean isValidResponse = mPaypalRequest.validateV1V2Response(mContextInspector, bundle);

                    if(resultCode == RESULT_OK && isValidResponse){
                        result = processResponseIntent(bundle);
                        pointToTrack = TrackingPoint.Return;
                    } else {
                        if(bundle.containsKey("error")){
                            result = new Result(new WalletSwitchException(bundle.getString("error")));
                            pointToTrack = TrackingPoint.Error;
                        } else if(!isValidResponse) {
                            result = new Result(new ResponseParsingException("invalid wallet response"));
                            pointToTrack = TrackingPoint.Error;
                        } else {
                            result = new Result();
                            pointToTrack = TrackingPoint.Cancel;
                        }


                        //Intent returnIntent = new Intent();
                        // result may include the "error" field, which we want to propagate
                        // back up to Braintree

                        // TODO do we still want to propagate all fields to Braintree?
                        //returnIntent.putExtras(data.getExtras());

                        logAllKeysPublicly(data.getExtras());
                    }

                } else {
                    // exit or something
                    //override the result code we got from wallet.  If we can't trust them,
                    // THEN WHO CAN WE TRUST?!?!?
                    result = new Result();
                    pointToTrack = TrackingPoint.Cancel;
                }
                break;

            default:
                Log.wtf(TAG, "unexpected request code " + requestCode + " call it a cancel");
                result = new Result();
                pointToTrack = TrackingPoint.Cancel;
                break;
        }

        Intent returnIntent = new Intent()
                .putExtra(PayPalOneTouchActivity.EXTRA_ONE_TOUCH_RESULT, result);
        // Always return OK, and use the PayPalOneTouchActivity to relay the response status
        setResult(Activity.RESULT_OK, returnIntent);

        this.mPaypalRequest.trackFpti(this, pointToTrack, null);
        finish();
    }

    private Result processResponseIntent(Bundle bundle) {
        Result result;
        String error = bundle.getString("error");
        if(!TextUtils.isEmpty(error)){
            result = new Result(new WalletSwitchException(error));
        } else {
            String environment = bundle.getString("environment");
            String bundleResponseType = bundle.getString("response_type").toLowerCase(Locale.US);
            ResponseType response_type;
            if("code".equals(bundleResponseType)){
                response_type = ResponseType.authorization_code;
            } else {
                response_type = ResponseType.web;
            }

            try {
                if(ResponseType.web == response_type){
                    String webURL = bundle.getString("webURL");
                    result = new Result(
                            environment,
                            response_type,
                            new JSONObject().put("webURL", webURL),
                            null /* email not sent back in checkout requests since Hermes doesn't return that info*/);
                } else {
                    String authorization_code = bundle.getString("authorization_code");
                    String email = bundle.getString("email");
                    result = new Result(
                            environment,
                            response_type,
                            new JSONObject().put("code", authorization_code),
                            email
                    );
                }
            } catch (JSONException e) {
                result = new Result(new ResponseParsingException(e));
            }
        }
        return result;
    }

    private void logAllKeys(Bundle bundle) {
        if(null != bundle) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                String message;
                if (value == null) {
                    message = String.format("%s:null", key);
                } else {
                    message =
                            String.format("%s:%s (%s)", key, value.toString(), value.getClass()
                                    .getName());
                }

                Log.d(TAG, message);
            }
        }
    }

    private void logAllKeysPublicly(Bundle bundle) {
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            String message;
            if (value == null) {
                message = String.format("%s:null", key);
            } else {
                message =
                        String.format("%s:%s (%s)", key, value.toString(), value.getClass()
                                .getName());
            }

            Log.w(Constants.PUBLIC_TAG, message);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void finish() {
        super.finish();
        Log.d(TAG, "finish");
    }
}
