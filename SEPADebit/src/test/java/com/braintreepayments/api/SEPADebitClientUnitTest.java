package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.net.Uri;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SEPADebitClientUnitTest {

    private FragmentActivity activity;
    private Lifecycle lifecycle;
    private BraintreeClient braintreeClient;
    private SEPADebitListener listener;
    private CreateMandateResult createMandateResult;
    private SEPADebitRequest sepaDebitRequest;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        lifecycle = mock(Lifecycle.class);
        braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("example-scheme://")
                .configuration(mock(Configuration.class))
                .build();
        listener = mock(SEPADebitListener.class);

        createMandateResult = new CreateMandateResult(
                "http://www.example.com",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );
        sepaDebitRequest = new SEPADebitRequest();
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_launchesBrowserSwitch() throws BrowserSwitchException {
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);

        sut.tokenize(activity, sepaDebitRequest);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());
        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(Uri.parse("http://www.example.com"), browserSwitchOptions.getUrl());
        assertEquals("example-scheme://", browserSwitchOptions.getReturnUrlScheme());
        assertEquals(BraintreeRequestCodes.SEPA, browserSwitchOptions.getRequestCode());
        // TODO: assert on metadata once it is added
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenApprovalURLInvalid_returnsErrorToListener() {
        createMandateResult = new CreateMandateResult(
                "null",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );

        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDebitRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADebitFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("An unexpected error occurred.", exception.getMessage());
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenStartBrowserSwitchFails_returnsErrorToListener() throws BrowserSwitchException {
        doThrow(BrowserSwitchException.class).when(braintreeClient).startBrowserSwitch(any(FragmentActivity.class), any(BrowserSwitchOptions.class));

        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDebitRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADebitFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BrowserSwitchException);
    }

    @Test
    public void tokenize_onConfigurationError_returnsErrorToListener() {
        Exception error = new Exception("config error");
        braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(error)
                .build();

        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder().build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDebitRequest);

        verify(listener).onSEPADebitFailure(same(error));
    }

    @Test
    public void tokenize_onCreateMandateError_returnsErrorToListener() {
        Exception error = new Exception("error");
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder()
                .createMandateError(error)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDebitRequest);

        verify(listener).onSEPADebitFailure(same(error));
    }
}