package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.internal.ClassHelper;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.Models.Parameters.CardinalConfigurationParameters;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest({ ThreeDSecure.class, Cardinal.class, ClassHelper.class })
public class ThreeDSecureWithCardinalUnitTest {
    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    private BraintreeFragment mFragment;

    @Before
    public void setup() throws Exception {

        String configuration = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .cardinalAuthenticationJWT("cardinal_authentication_jwt")
                .build();

        mFragment = new MockFragmentBuilder()
                .authorization(Authorization.fromString(TOKENIZATION_KEY))
                .configuration(configuration)
                .build();
    }

    @Test
    public void configureCardinal_whenSdkIsUnavailable_doesNothing() {
        mockStatic(ClassHelper.class);
        mockStatic(Cardinal.class);

        when(ClassHelper.isClassAvailable(eq("com.cardinalcommerce.cardinalmobilesdk.Cardinal")))
                .thenReturn(false);

        ThreeDSecure.configureCardinal(mFragment);

        verifyZeroInteractions(Cardinal.class);
    }

    @Test
    public void configureCardinal_configures() {
        Cardinal mockCardinal = mock(Cardinal.class);
        doNothing().when(mockCardinal).configure(any(Context.class),
                any(CardinalConfigurationParameters.class));

        mockStatic(Cardinal.class);
        when(Cardinal.getInstance()).thenReturn(mockCardinal);

        ThreeDSecure.configureCardinal(mFragment);

        verify(mockCardinal).configure(any(Context.class), any(CardinalConfigurationParameters.class));
    }
}
