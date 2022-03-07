package com.braintreepayments.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.net.Uri;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

@RunWith(RobolectricTestRunner.class)
public class SEPADebitClientUnitTest {
    private FragmentActivity activity;
    private Lifecycle lifecycle;
    private BraintreeClient braintreeClient;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        lifecycle = mock(Lifecycle.class);
        braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("example-scheme://")
                .configuration(mock(Configuration.class))
                .build();
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_launchesBrowserSwitch() throws BrowserSwitchException {
        CreateMandateResult createMandateResult = new CreateMandateResult(
                "http://www.example.com",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );

        SEPADebitRequest sepaDebitRequest = new SEPADebitRequest();

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
}