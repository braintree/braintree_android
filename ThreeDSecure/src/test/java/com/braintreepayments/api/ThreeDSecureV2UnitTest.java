package com.braintreepayments.api;

import static android.app.Activity.RESULT_OK;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.os.TransactionTooLargeException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2UnitTest {

    private FragmentActivity activity;
    private Lifecycle lifecycle;

    private Configuration threeDSecureEnabledConfig;
    private ThreeDSecureRequest basicRequest;

    @Before
    public void setup() {
        activity = mock(FragmentActivity.class);

        threeDSecureEnabledConfig = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .cardinalAuthenticationJWT("cardinal_authentication_jwt")
                .buildConfiguration();

        ThreeDSecureV2TextBoxCustomization textBoxCustomization =
                new ThreeDSecureV2TextBoxCustomization();
        textBoxCustomization.setBorderWidth(12);

        ThreeDSecureV2UiCustomization v2UiCustomization = new ThreeDSecureV2UiCustomization();
        v2UiCustomization.setTextBoxCustomization(textBoxCustomization);

        basicRequest = new ThreeDSecureRequest();
        basicRequest.setNonce("a-nonce");
        basicRequest.setAmount("1.00");
        basicRequest.setV2UiCustomization(v2UiCustomization);

        ActivityResultRegistry resultRegistry = mock(ActivityResultRegistry.class);
        when(activity.getActivityResultRegistry()).thenReturn(resultRegistry);

        lifecycle = mock(Lifecycle.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);
    }




}
