package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeGraphQLHttpClient;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO: Complete unit tests
@RunWith(RobolectricTestRunner.class)
public class BraintreeClientUnitTest {

    private Authorization authorization;
    private Context context;
    private Context applicationContext;

    private BraintreeHttpClient braintreeHttpClient;
    private BraintreeGraphQLHttpClient braintreeGraphQLHttpClient;
    private ConfigurationManager configurationManager;

    @Before
    public void beforeEach() {
        authorization = mock(Authorization.class);
        context = mock(Context.class);
        applicationContext = mock(Context.class);

        braintreeHttpClient = mock(BraintreeHttpClient.class);
        braintreeGraphQLHttpClient = mock(BraintreeGraphQLHttpClient.class);
        configurationManager = mock(ConfigurationManager.class);

        when(context.getApplicationContext()).thenReturn(applicationContext);
    }

    @Test
    public void getConfiguration_onSuccess_forwardsInvocationToConfigurationLoader() {
        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager);

        ConfigurationCallback configurationCallback = mock(ConfigurationCallback.class);
        sut.getConfiguration(configurationCallback);

        verify(configurationManager).loadConfiguration(same(applicationContext), same(authorization), same(configurationCallback));
    }

    @Test
    public void sendGET_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configuration(configuration)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGET("sample-url", httpResponseCallback);

        verify(braintreeHttpClient).get(eq("sample-url"), same(configuration), same(httpResponseCallback));
    }

    @Test
    public void sendGET_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configurationError(exception)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGET("sample-url", httpResponseCallback);

        verify(httpResponseCallback).failure(same(exception));
    }

    @Test
    public void sendPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configuration(configuration)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendPOST("sample-url", "{}", httpResponseCallback);

        verify(braintreeHttpClient).post(eq("sample-url"), eq("{}"), same(configuration), same(httpResponseCallback));
    }

    @Test
    public void sendPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configurationError(exception)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendPOST("sample-url", "{}", httpResponseCallback);

        verify(httpResponseCallback).failure(same(exception));
    }

    @Test
    public void sendGraphQLPOST_onGetConfigurationSuccess_forwardsRequestToHttpClient() {
        Configuration configuration = mock(Configuration.class);
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configuration(configuration)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGraphQLPOST("{}", httpResponseCallback);

        verify(braintreeGraphQLHttpClient).post(eq("{}"), same(configuration), same(httpResponseCallback));
    }

    @Test
    public void sendGraphQLPOST_onGetConfigurationFailure_forwardsErrorToCallback() {
        Exception exception = new Exception("configuration error");
        ConfigurationManager configurationManager = new MockConfigurationManagerBuilder()
                .configurationError(exception)
                .build();

        BraintreeClient sut = new BraintreeClient(authorization, context, null, braintreeHttpClient, braintreeGraphQLHttpClient, configurationManager);

        HttpResponseCallback httpResponseCallback = mock(HttpResponseCallback.class);
        sut.sendGraphQLPOST("{}", httpResponseCallback);

        verify(httpResponseCallback).failure(same(exception));
    }
}
