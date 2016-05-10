package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockFragmentBuilder {

    private Context mContext;
    private Authorization mAuthorization;
    private Configuration mConfiguration;
    private String mSuccessResponse;
    private Exception mErrorResponse;

    public MockFragmentBuilder() {
        mContext = RuntimeEnvironment.application;
    }

    public MockFragmentBuilder context(Context context) {
        mContext = context;
        return this;
    }

    public MockFragmentBuilder authorization(Authorization authorization) {
        mAuthorization = authorization;
        return this;
    }

    public MockFragmentBuilder configuration(String configuration) {
        try {
            mConfiguration = Configuration.fromJson(configuration);
        } catch (JSONException ignored) {}
        return this;
    }

    public MockFragmentBuilder configuration(Configuration configuration) {
        mConfiguration = configuration;
        return this;
    }

    public MockFragmentBuilder successResponse(String response) {
        mSuccessResponse = response;
        return this;
    }

    public MockFragmentBuilder errorResponse(Exception exception) {
        mErrorResponse = exception;
        return this;
    }

    public BraintreeFragment build() {
        BraintreeFragment fragment = mock(BraintreeFragment.class);
        when(fragment.getApplicationContext()).thenReturn(mContext);
        when(fragment.getAuthorization()).thenReturn(mAuthorization);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ConfigurationListener) invocation.getArguments()[0]).onConfigurationFetched(mConfiguration);
                return null;
            }
        }).when(fragment).waitForConfiguration(any(ConfigurationListener.class));
        when(fragment.getConfiguration()).thenReturn(mConfiguration);

        BraintreeHttpClient httpClient = mock(BraintreeHttpClient.class);
        if (mSuccessResponse != null) {
            setupSuccessResponses(httpClient);
        } else if (mErrorResponse != null) {
            setupErrorResponses(httpClient);
        }

        when(fragment.getHttpClient()).thenReturn(httpClient);

        return fragment;
    }

    private void setupSuccessResponses(BraintreeHttpClient httpClient) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((HttpResponseCallback) invocation.getArguments()[1]).success(mSuccessResponse);
                return null;
            }
        }).when(httpClient).get(any(String.class), any(HttpResponseCallback.class));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((HttpResponseCallback) invocation.getArguments()[2]).success(mSuccessResponse);
                return null;
            }
        }).when(httpClient).post(anyString(), anyString(), any(HttpResponseCallback.class));
    }

    private void setupErrorResponses(BraintreeHttpClient httpClient) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((HttpResponseCallback) invocation.getArguments()[1]).failure(mErrorResponse);
                return null;
            }
        }).when(httpClient).get(any(String.class), any(HttpResponseCallback.class));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((HttpResponseCallback) invocation.getArguments()[2]).failure(mErrorResponse);
                return null;
            }
        }).when(httpClient).post(anyString(), anyString(), any(HttpResponseCallback.class));
    }
}
