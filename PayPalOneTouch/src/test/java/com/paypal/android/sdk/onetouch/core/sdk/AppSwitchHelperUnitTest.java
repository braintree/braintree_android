package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;

import com.braintreepayments.api.internal.SignatureVerificationOverrides;
import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.config.ConfigManager;
import com.paypal.android.sdk.onetouch.core.config.Recipe;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.network.PayPalHttpClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.paypal.android.sdk.onetouch.core.test.TestSetupHelper.getMockContextInspector;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AppSwitchHelperUnitTest {

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
        Intent intent = AppSwitchHelper.createBaseIntent("action", "package");

        assertEquals("action", intent.getAction());
        assertEquals("package", intent.getPackage());
    }

    @Test
    public void buildAppSwitchIntent_buildsIntentForProtocolV1() {
        Request request = mock(CheckoutRequest.class);
        when(request.getEnvironment()).thenReturn("test");

        Intent intent = AppSwitchHelper.getAppSwitchIntent(mContextInspector, mConfigManager,
                request, getMockRecipe(1));

        assertEquals("com.paypal.android.lib.authenticator.activity.v1.TouchActivity", intent.getAction());
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
        assertEquals("com.paypal.android.p2pmobile", intent.getPackage());
        assertEquals("3.0", intent.getStringExtra("version"));
    }

    @Test
    public void buildAppSwitchIntent_buildsIntentForCheckoutRequest() {
        CheckoutRequest request = mock(CheckoutRequest.class);
        when(request.getEnvironment()).thenReturn("test");
        when(request.getBrowserSwitchUrl())
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

    private Recipe getMockRecipe(int version) {
        Recipe recipe = mock(Recipe.class);
        when(recipe.getTargetIntentAction()).thenReturn("com.paypal.android.lib.authenticator.activity.v" + version + ".TouchActivity");

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
