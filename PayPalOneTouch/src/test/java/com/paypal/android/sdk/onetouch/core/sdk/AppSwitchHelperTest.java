package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import com.braintreepayments.api.internal.SignatureVerificationOverrides;
import com.paypal.android.sdk.onetouch.core.AuthorizationRequest;
import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.Result;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.config.ConfigManager;
import com.paypal.android.sdk.onetouch.core.config.OtcConfiguration;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.enums.ResponseType;
import com.paypal.android.sdk.onetouch.core.enums.ResultType;
import com.paypal.android.sdk.onetouch.core.exception.ResponseParsingException;
import com.paypal.android.sdk.onetouch.core.exception.WalletSwitchException;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;
import com.paypal.android.sdk.onetouch.core.network.PayPalHttpClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.paypal.android.sdk.onetouch.core.test.TestSetupHelper.getMockContextInspector;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class AppSwitchHelperTest {

    private ContextInspector mContextInspector;
    private ConfigManager mConfigManager;

    @Before
    public void setup() throws NameNotFoundException {
        SignatureVerificationOverrides.disableSignatureVerification(false);
        mContextInspector = getMockContextInspector();
        mConfigManager = new ConfigManager(mContextInspector, mock(PayPalHttpClient.class));
        mConfigManager.useHardcodedConfig(true);
    }

    @Test
    public void isSignatureValid_returnsTrueIfSecurityNotEnabled() {
        SignatureVerificationOverrides.disableSignatureVerification(true);
        assertTrue(AppSwitchHelper.isSignatureValid(null, ""));
    }

    @Test
    public void createBaseIntent_createsIntentCorrectly() {
        Intent intent = AppSwitchHelper.createBaseIntent("action", "component", "package");

        assertEquals("action", intent.getAction());
        assertEquals("package/component", intent.getComponent().flattenToString());
        assertEquals("package", intent.getPackage());
    }

    @Test
    public void buildAppSwitchIntent_buildsIntentForProtocolV1() {
        Request request = mock(CheckoutRequest.class);
        when(request.getEnvironment()).thenReturn("test");

        Intent intent = AppSwitchHelper.getAppSwitchIntent(mContextInspector, mConfigManager,
                request, getMockRecipe(1));

        assertEquals("com.paypal.android.lib.authenticator.activity.v1.TouchActivity", intent.getAction());
        assertEquals("com.paypal.android.p2pmobile/com.paypal.android.lib.authenticator.activity.v1.TouchActivity",
                intent.getComponent().flattenToString());
        assertEquals("com.paypal.android.p2pmobile", intent.getPackage());
        assertEquals("1.0", intent.getStringExtra("version"));
    }

    @Test
    public void buildAppSwitchIntent_buildsIntentForProtocolV2() {
        Request request = mock(CheckoutRequest.class);
        when(request.getEnvironment()).thenReturn("test");

        Intent intent = AppSwitchHelper.getAppSwitchIntent(mContextInspector, mConfigManager,
                request, getMockRecipe(2));

        assertEquals("com.paypal.android.lib.authenticator.activity.v2.TouchActivity", intent.getAction());
        assertEquals("com.paypal.android.p2pmobile/com.paypal.android.lib.authenticator.activity.v2.TouchActivity",
                intent.getComponent().flattenToString());
        assertEquals("com.paypal.android.p2pmobile", intent.getPackage());
        assertEquals("2.0", intent.getStringExtra("version"));
    }

    @Test
    public void buildAppSwitchIntent_buildsIntentForProtocolV3() {
        Request request = mock(CheckoutRequest.class);
        when(request.getEnvironment()).thenReturn("test");

        Intent intent = AppSwitchHelper.getAppSwitchIntent(mContextInspector, mConfigManager,
                request, getMockRecipe(3));

        assertEquals("com.paypal.android.lib.authenticator.activity.v3.TouchActivity", intent.getAction());
        assertEquals("com.paypal.android.p2pmobile/com.paypal.android.lib.authenticator.activity.v3.TouchActivity",
                intent.getComponent().flattenToString());
        assertEquals("com.paypal.android.p2pmobile", intent.getPackage());
        assertEquals("3.0", intent.getStringExtra("version"));
    }

    @Test
    public void buildAppSwitchIntent_buildsIntentForAuthorizationRequest() {
        AuthorizationRequest request = mock(AuthorizationRequest.class);
        when(request.getEnvironment()).thenReturn("test");
        when(request.getScopeString()).thenReturn("scope-string");
        when(request.getPrivacyUrl()).thenReturn("privacy-url");
        when(request.getUserAgreementUrl()).thenReturn("agreement-url");

        Intent intent = AppSwitchHelper.getAppSwitchIntent(mContextInspector, mConfigManager,
                request, getMockRecipe(2));

        assertEquals("scope-string", intent.getStringExtra("scope"));
        assertEquals("code", intent.getStringExtra("response_type"));
        assertEquals("privacy-url", intent.getStringExtra("privacy_url"));
        assertEquals("agreement-url", intent.getStringExtra("agreement_url"));
    }

    @Test
    public void buildAppSwitchIntent_buildsIntentForCheckoutRequest() {
        CheckoutRequest request = mock(CheckoutRequest.class);
        when(request.getEnvironment()).thenReturn("test");
        when(request.getBrowserSwitchUrl(any(Context.class), any(OtcConfiguration.class)))
                .thenReturn("web-url");

        Intent intent = AppSwitchHelper.getAppSwitchIntent(mContextInspector, mConfigManager,
                request, getMockRecipe(2));

        assertEquals("web", intent.getStringExtra("response_type"));
        assertEquals("web-url", intent.getStringExtra("webURL"));
    }

    @Test
    public void buildAppSwitchIntent_buildsIntent() {
        Request request = mock(CheckoutRequest.class);
        when(request.getEnvironment()).thenReturn("test");
        when(request.getClientMetadataId()).thenReturn("client-metadata-id");
        when(request.getClientId()).thenReturn("client-id");

        Intent intent = AppSwitchHelper.getAppSwitchIntent(mContextInspector, mConfigManager,
                request, getMockRecipe(2));

        assertEquals("installation-guid", intent.getStringExtra("app_guid"));
        assertEquals("client-metadata-id", intent.getStringExtra("client_metadata_id"));
        assertEquals("client-id", intent.getStringExtra("client_id"));
        assertEquals("application-name", intent.getStringExtra("app_name"));
        assertEquals("test", intent.getStringExtra("environment"));
        assertEquals("test", intent.getStringExtra("environment_url"));
    }

    @Test
    public void parseAppSwitchResponse_parsesAuthorizationCodeResponseAndReturnsResult()
            throws JSONException {
        Request request = mock(AuthorizationRequest.class);
        when(request.validateV1V2Response(any(ContextInspector.class), any(Bundle.class)))
                .thenReturn(true);
        Intent intent = new Intent()
                .putExtra("environment", "test")
                .putExtra("response_type", "code")
                .putExtra("authorization_code", "auth-code")
                .putExtra("email", "test@paypal.com");

        Result result = AppSwitchHelper.parseAppSwitchResponse(mContextInspector, request, intent);

        verify(request).trackFpti(any(Context.class), eq(TrackingPoint.Return), isNull(Protocol.class));
        assertEquals(ResultType.Success, result.getResultType());

        assertEquals(ResponseType.authorization_code.name(),
                result.getResponse().getString("response_type"));

        JSONObject client = result.getResponse().getJSONObject("client");
        assertEquals("test", client.getString("environment"));
        assertEquals(BuildConfig.PRODUCT_VERSION, client.getString("paypal_sdk_version"));
        assertEquals("Android", client.getString("platform"));
        assertEquals(BuildConfig.PRODUCT_NAME, client.getString("product_name"));

        assertEquals("auth-code", result.getResponse().getJSONObject("response").getString("code"));

        assertEquals("test@paypal.com", result.getResponse().getJSONObject("user").getString("display_string"));
    }

    @Test
    public void parseAppSwitchResponse_parsesWebResponseAndReturnsResult() throws JSONException {
        Request request = mock(AuthorizationRequest.class);
        when(request.validateV1V2Response(any(ContextInspector.class), any(Bundle.class)))
                .thenReturn(true);
        Intent intent = new Intent()
                .putExtra("environment", "test")
                .putExtra("response_type", "web")
                .putExtra("webURL", "web-url");

        Result result = AppSwitchHelper.parseAppSwitchResponse(mContextInspector, request, intent);

        verify(request).trackFpti(any(Context.class), eq(TrackingPoint.Return), isNull(Protocol.class));
        assertEquals(ResultType.Success, result.getResultType());

        assertEquals(ResponseType.web.name(), result.getResponse().getString("response_type"));

        JSONObject client = result.getResponse().getJSONObject("client");
        assertEquals("test", client.getString("environment"));
        assertEquals(BuildConfig.PRODUCT_VERSION, client.getString("paypal_sdk_version"));
        assertEquals("Android", client.getString("platform"));
        assertEquals(BuildConfig.PRODUCT_NAME, client.getString("product_name"));

        assertEquals("web-url", result.getResponse().getJSONObject("response").getString("webURL"));
    }

    @Test
    public void parseAppSwitchResponse_parsesErrorForErrorResponses() {
        Request request = mock(AuthorizationRequest.class);
        when(request.validateV1V2Response(any(ContextInspector.class), any(Bundle.class)))
                .thenReturn(false);
        Intent intent = new Intent()
                .putExtra("error", "there was an error");

        Result result = AppSwitchHelper.parseAppSwitchResponse(mContextInspector, request, intent);

        verify(request).trackFpti(any(Context.class), eq(TrackingPoint.Error), isNull(Protocol.class));
        assertTrue(result.getError() instanceof WalletSwitchException);
        assertEquals("there was an error", result.getError().getMessage());
    }

    @Test
    public void parseAppSwitchResponse_returnsErrorWhenResponseCouldNotBeParsed() {
        Request request = mock(AuthorizationRequest.class);
        when(request.validateV1V2Response(any(ContextInspector.class), any(Bundle.class)))
                .thenReturn(false);
        Intent intent = new Intent()
                .putExtra("braintree", "nonce");

        Result result = AppSwitchHelper.parseAppSwitchResponse(mContextInspector, request, intent);

        verify(request).trackFpti(any(Context.class), eq(TrackingPoint.Error), isNull(Protocol.class));
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("invalid wallet response", result.getError().getMessage());
    }

    @Test
    public void parseAppSwitchResponse_parsesErrorAndReturnsResult() {
        Request request = mock(AuthorizationRequest.class);
        when(request.validateV1V2Response(any(ContextInspector.class), any(Bundle.class)))
                .thenReturn(true);
        Intent intent = new Intent()
                .putExtra("error", "there was an error");

        Result result = AppSwitchHelper.parseAppSwitchResponse(mContextInspector, request, intent);

        verify(request).trackFpti(any(Context.class), eq(TrackingPoint.Return), isNull(Protocol.class));
        assertTrue(result.getError() instanceof WalletSwitchException);
        assertEquals("there was an error", result.getError().getMessage());
    }

    private Recipe getMockRecipe(int version) {
        Recipe recipe = mock(Recipe.class);
        when(recipe.getTargetIntentAction()).thenReturn("com.paypal.android.lib.authenticator.activity.v" + version + ".TouchActivity");
        when(recipe.getTargetComponent()).thenReturn("com.paypal.android.lib.authenticator.activity.v" + version + ".TouchActivity");

        switch (version) {
            case 0:
                when(recipe.getProtocol()).thenReturn(Protocol.v0);
                break;
            case 1:
                when(recipe.getProtocol()).thenReturn(Protocol.v1);
                break;
            case 2:
                when(recipe.getProtocol()).thenReturn(Protocol.v2);
                break;
            case 3:
                when(recipe.getProtocol()).thenReturn(Protocol.v3);
                break;
        }

        return recipe;
    }
}
