package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeGraphQLHttpClient;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.json.JSONException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockFragmentBuilder {

    private Context mContext;
    private Configuration mConfiguration;

    public MockFragmentBuilder() {
        mContext = getTargetContext();
        mConfiguration = TestConfigurationBuilder.basicConfig();
    }

    public MockFragmentBuilder configuration(String configuration) {
        try {
            mConfiguration = Configuration.fromJson(configuration);
        } catch (JSONException ignored) {}
        return this;
    }

    public BraintreeFragment build() {
        BraintreeFragment fragment = mock(BraintreeFragment.class);
        when(fragment.getApplicationContext()).thenReturn(mContext);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ConfigurationListener) invocation.getArguments()[0]).onConfigurationFetched(mConfiguration);
                return null;
            }
        }).when(fragment).waitForConfiguration(any(ConfigurationListener.class));
        when(fragment.getConfiguration()).thenReturn(mConfiguration);

        when(fragment.getHttpClient()).thenReturn(mock(BraintreeHttpClient.class));

        when(fragment.getGraphQLHttpClient()).thenReturn(mock(BraintreeGraphQLHttpClient.class));

        return fragment;
    }
}
