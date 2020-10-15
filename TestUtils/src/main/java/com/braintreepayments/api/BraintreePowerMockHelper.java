package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;

import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.a.a.c;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

class BraintreePowerMockHelper {

    static class MockStaticCardinal {
        static void initCompletesSuccessfully(final String dfReferenceId) {
            Cardinal cruiseService = mock(Cardinal.class);
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) {
                    CardinalInitService cardinalInitService = (CardinalInitService) invocation.getArguments()[1];

                    cardinalInitService.onSetupCompleted(dfReferenceId);
                    return null;
                }
            }).when(cruiseService).init(anyString(), any(CardinalInitService.class));

            mockStatic(Cardinal.class);
            doReturn(cruiseService).when(Cardinal.class);
            Cardinal.getInstance();
        }

        static void initCallsOnValidated() {
            Cardinal cruiseService = mock(Cardinal.class);
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) {
                    CardinalInitService cardinalInitService = (CardinalInitService) invocation.getArguments()[1];

                    cardinalInitService.onValidated(null, null);
                    return null;
                }
            }).when(cruiseService).init(anyString(), any(CardinalInitService.class));

            mockStatic(Cardinal.class);
            doReturn(cruiseService).when(Cardinal.class);
            Cardinal.getInstance();
        }

        public static Cardinal cca_continue(final CardinalActionCode actionCode) {
            Cardinal cruiseService = mock(Cardinal.class);
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) {
                    CardinalValidateReceiver callback = invocation.getArgumentAt(3, CardinalValidateReceiver.class);

                    ValidateResponse validateResponse = new ValidateResponse(
                            false,
                            actionCode,
                            new c(0, "")
                    );

                    callback.onValidated(null, validateResponse, "jwt");
                    return null;
                }
            }).when(cruiseService).cca_continue(
                    anyString(),
                    anyString(),
                    any(Activity.class),
                    any(CardinalValidateReceiver.class)
            );

            mockStatic(Cardinal.class);
            doReturn(cruiseService).when(Cardinal.class);
            Cardinal.getInstance();

            return cruiseService;
        }
    }

    static class MockStaticTokenizationClient {
        static void mockTokenizeSuccess(final PaymentMethodNonce paymentMethodNonce) {
            mockStatic(TokenizationClient.class);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) {
                    ((PaymentMethodNonceCallback) invocation.getArguments()[2]).success(paymentMethodNonce);
                    return null;
                }
            }).when(TokenizationClient.class);
            TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                    any(PaymentMethodNonceCallback.class));
        }
        static void mockTokenizeFailure(final Exception ex) {
            mockStatic(TokenizationClient.class);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) {
                    ((PaymentMethodNonceCallback) invocation.getArguments()[2]).failure(ex);
                    return null;
                }
            }).when(TokenizationClient.class);
            TokenizationClient.tokenize(any(BraintreeFragment.class), any(PaymentMethodBuilder.class),
                    any(PaymentMethodNonceCallback.class));
        }
    }

    static class MockManifestValidator {
        static void mockUrlSchemeDeclaredInAndroidManifest(boolean returnValue) {
            spy(ManifestValidator.class);
            try {
                doReturn(returnValue).when(ManifestValidator.class,
                        "isUrlSchemeDeclaredInAndroidManifest", any(Context.class),
                        anyString(), any(Class.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
