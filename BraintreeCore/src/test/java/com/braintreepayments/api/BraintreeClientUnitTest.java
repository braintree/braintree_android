package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// TODO: Complete unit tests
@RunWith(RobolectricTestRunner.class)
public class BraintreeClientUnitTest {

    private Authorization authorization;
    private Context context;

    private BraintreeHttpClient braintreeHttpClient;
    private ConfigurationManager configurationManager;

    @Before
    public void beforeEach() {
        authorization = mock(Authorization.class);
        context = mock(Context.class);

        braintreeHttpClient = mock(BraintreeHttpClient.class);
        configurationManager = mock(ConfigurationManager.class);
    }

    @Test
    public void getConfiguration_onSuccess_forwardsInvocationToConfigurationLoader() {
        BraintreeClient sut = new BraintreeClient(authorization, null, braintreeHttpClient, configurationManager);

        ConfigurationCallback configurationCallback = mock(ConfigurationCallback.class);
        sut.getConfiguration(context, configurationCallback);

        verify(configurationManager).loadConfiguration(same(context), same(authorization), same(configurationCallback));
    }
}
