package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;
import com.paypal.android.sdk.onetouch.core.Request;
import com.paypal.android.sdk.onetouch.core.Result;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;
import com.paypal.android.sdk.onetouch.core.enums.ResponseType;
import com.paypal.android.sdk.onetouch.core.enums.ResultType;
import com.paypal.android.sdk.onetouch.core.sdk.PendingRequest;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

class BraintreePowerMockHelper {
    static class MockStaticPayPalOneTouch {
        static void parseResponse(final ResultType wantedResultType) {
            mockStatic(PayPalOneTouchCore.class);
            doAnswer(new Answer<Result>() {
                @Override
                public Result answer(InvocationOnMock invocation) throws Throwable {
                    return new Result("test", ResponseType.web, null, "");
                }
            }).when(PayPalOneTouchCore.class);
            PayPalOneTouchCore.parseResponse(any(Context.class), any(Request.class), any(Intent.class));
        }

        static void getStartIntent(final String switchType) {
            mockStatic(PayPalOneTouchCore.class);
            doAnswer(new Answer<PendingRequest>() {
                @Override
                public PendingRequest answer(InvocationOnMock invocation) throws Throwable {
                    RequestTarget requestTarget = "browser-switch".equals(switchType) ? RequestTarget.browser : RequestTarget.wallet;
                    return new PendingRequest(true, requestTarget, "", null);
                }
            }).when(PayPalOneTouchCore.class);
            PayPalOneTouchCore.getStartIntent(any(Context.class), any(Request.class));
        }
    }

    static class MockStaticTokenizationClient {
        static void mockTokenizeSuccess(final PaymentMethodNonce paymentMethodNonce) {
            mockStatic(TokenizationClient.class);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(paymentMethodNonce);
                    return null;
                }
            }).when(TokenizationClient.class);
            TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                    any(PaymentMethodNonceCallback.class));
        }
    }
}
