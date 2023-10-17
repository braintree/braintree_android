package com.braintreepayments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.os.TransactionTooLargeException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureLauncherUnitTest {

    @Test
    public void launch_whenTransactionTooLarge_callsBackError() {
//        CardinalClient cardinalClient = new MockCardinalClientBuilder()
//                .successReferenceId("reference-id")
//                .build();
//
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
//                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
//                .configuration(threeDSecureEnabledConfig)
//                .build();
//
//        ThreeDSecureClient sut =
//                new ThreeDSecureClient(braintreeClient, cardinalClient,
//                        new ThreeDSecureAPI(braintreeClient));
//        ThreeDSecureResult threeDSecureResult =
//                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
//
//        TransactionTooLargeException transactionTooLargeException =
//                new TransactionTooLargeException();
//        RuntimeException runtimeException = new RuntimeException(
//                "runtime exception caused by transaction too large", transactionTooLargeException);
//
//        doThrow(runtimeException)
//                .when(activity).startActivityForResult(any(Intent.class), anyInt());
//
//        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
//        sut.continuePerformVerification(threeDSecureResult, callback);
//
//        ArgumentCaptor<BraintreeException> captor =
//                ArgumentCaptor.forClass(BraintreeException.class);
//        verify(callback).onResult(isNull(), captor.capture());
//
//        BraintreeException braintreeException = captor.getValue();
//        String expectedMessage = "The 3D Secure response returned is too large to continue. "
//                + "Please contact Braintree Support for assistance.";
//        Assert.assertEquals(expectedMessage, braintreeException.getMessage());
    }
}
