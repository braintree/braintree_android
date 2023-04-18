package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Cardinal.class})
public class CardinalClientUnitTest {

    private Cardinal cardinalInstance;

    private FragmentActivity activity;
    private Configuration configuration;

    private Context context;
    private CardinalInitializeCallback cardinalInitializeCallback;
    private CardinalValidateReceiver cardinalValidateReceiver;

    @Before
    public void beforeEach() {
        mockStatic(Cardinal.class);
        context = mock(Context.class);
        configuration = mock(Configuration.class);
        cardinalInitializeCallback = mock(CardinalInitializeCallback.class);
        cardinalInstance = mock(Cardinal.class);

        activity = mock(FragmentActivity.class);
        cardinalValidateReceiver = mock(CardinalValidateReceiver.class);
    }

    @Test
    public void initialize_configuresDefaultCardinalConfigurationParameters() throws BraintreeException {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        CardinalClient sut = new CardinalClient();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        sut.initialize(context, configuration, request, cardinalInitializeCallback);

        ArgumentCaptor<CardinalConfigurationParameters> captor = ArgumentCaptor.forClass(CardinalConfigurationParameters.class);
        verify(cardinalInstance).configure(same(context), captor.capture());

        CardinalConfigurationParameters parameters = captor.getValue();
        assertEquals(CardinalEnvironment.STAGING, parameters.getEnvironment());
        assertEquals(8000, parameters.getRequestTimeout());
        assertTrue(parameters.isEnableDFSync());
    }

    @Test
    public void initialize_whenV2UiCustomizationNotNull_setsCardinalConfigurationParameters() throws BraintreeException {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        CardinalClient sut = new CardinalClient();

        ThreeDSecureV2UiCustomization v2UiCustomization = new ThreeDSecureV2UiCustomization();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setV2UiCustomization(v2UiCustomization);
        sut.initialize(context, configuration, request, cardinalInitializeCallback);

        ArgumentCaptor<CardinalConfigurationParameters> captor = ArgumentCaptor.forClass(CardinalConfigurationParameters.class);
        verify(cardinalInstance).configure(same(context), captor.capture());

        CardinalConfigurationParameters parameters = captor.getValue();
        assertEquals(request.getV2UiCustomization().getCardinalUiCustomization(), parameters.getUICustomization());
    }

    @Test
    public void initialize_whenEnvironmentProduction_configuresCardinalEnvironmentProduction() throws BraintreeException {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        when(configuration.getEnvironment()).thenReturn("production");

        CardinalClient sut = new CardinalClient();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        sut.initialize(context, configuration, request, cardinalInitializeCallback);

        ArgumentCaptor<CardinalConfigurationParameters> captor = ArgumentCaptor.forClass(CardinalConfigurationParameters.class);
        verify(cardinalInstance).configure(same(context), captor.capture());

        CardinalConfigurationParameters parameters = captor.getValue();
        assertEquals(CardinalEnvironment.PRODUCTION, parameters.getEnvironment());
    }

    @Test
    public void initialize_returnsConsumerSessionIdToListener() throws BraintreeException {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        when(configuration.getCardinalAuthenticationJwt()).thenReturn("token");
        CardinalClient sut = new CardinalClient();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        sut.initialize(context, configuration, request, cardinalInitializeCallback);

        ArgumentCaptor<CardinalInitService> captor = ArgumentCaptor.forClass(CardinalInitService.class);
        verify(cardinalInstance).init(eq("token"), captor.capture());

        CardinalInitService cardinalInitService = captor.getValue();
        cardinalInitService.onSetupCompleted("session-id");

        verify(cardinalInitializeCallback).onResult(sut.getConsumerSessionId(), null);
        assertEquals("session-id", sut.getConsumerSessionId());
    }

    @Test
    public void initialize_whenConsumerSessionIdIsNull_returnsBraintreeExceptionToListener() throws BraintreeException {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        when(configuration.getCardinalAuthenticationJwt()).thenReturn("token");
        CardinalClient sut = new CardinalClient();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        sut.initialize(context, configuration, request, cardinalInitializeCallback);

        ArgumentCaptor<CardinalInitService> captor = ArgumentCaptor.forClass(CardinalInitService.class);
        verify(cardinalInstance).init(eq("token"), captor.capture());

        CardinalInitService cardinalInitService = captor.getValue();
        cardinalInitService.onValidated(null, null);

        ArgumentCaptor<BraintreeException> exceptionCaptor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(cardinalInitializeCallback).onResult(isNull(String.class), exceptionCaptor.capture());
        assertEquals(exceptionCaptor.getValue().getMessage(), "consumer session id not available");
    }

    @Test
    public void initialize_onCardinalConfigureRuntimeException_throwsError() {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        when(configuration.getCardinalAuthenticationJwt()).thenReturn("token");
        CardinalClient sut = new CardinalClient();

        RuntimeException runtimeException = new RuntimeException("fake message");
        doThrow(runtimeException)
                .when(cardinalInstance)
                .configure(any(Context.class), any(CardinalConfigurationParameters.class));

        ThreeDSecureRequest request = new ThreeDSecureRequest();

        try {
            sut.initialize(context, configuration, request, cardinalInitializeCallback);
            fail("should not get here");
        } catch (BraintreeException e) {
            assertEquals("Cardinal SDK configure Error.", e.getMessage());
            assertSame(runtimeException, e.getCause());
        }
    }

    @Test
    public void initialize_onCardinalInitRuntimeException_throwsError() {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        when(configuration.getCardinalAuthenticationJwt()).thenReturn("token");
        CardinalClient sut = new CardinalClient();

        RuntimeException runtimeException = new RuntimeException("fake message");
        doThrow(runtimeException)
                .when(cardinalInstance)
                .init(anyString(), any(CardinalInitService.class));

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        try {
            sut.initialize(context, configuration, request, cardinalInitializeCallback);
            fail("should not get here");
        } catch (BraintreeException e) {
            assertEquals("Cardinal SDK init Error.", e.getMessage());
            assertSame(runtimeException, e.getCause());
        }
    }

    @Test
    public void continueLookup_continuesCardinalLookup() throws BraintreeException {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        CardinalClient sut = new CardinalClient();

        ThreeDSecureLookup threeDSecureLookup = mock(ThreeDSecureLookup.class);
        when(threeDSecureLookup.getTransactionId()).thenReturn("sample-transaction-id");
        when(threeDSecureLookup.getPareq()).thenReturn("sample-payer-authentication-request");

        ThreeDSecureResult threeDSecureResult = mock(ThreeDSecureResult.class);
        when(threeDSecureResult.getLookup()).thenReturn(threeDSecureLookup);

        sut.continueLookup(activity, threeDSecureResult, cardinalValidateReceiver);

        verify(cardinalInstance).cca_continue(
                "sample-transaction-id",
                "sample-payer-authentication-request",
                activity,
                cardinalValidateReceiver
        );
    }

    @Test
    public void continueLookup_onCardinalRuntimeException_throwsError() {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        RuntimeException runtimeException = new RuntimeException("fake message");
        doThrow(runtimeException)
                .when(cardinalInstance).cca_continue(anyString(), anyString(), any(Activity.class), any(CardinalValidateReceiver.class));
        CardinalClient sut = new CardinalClient();

        ThreeDSecureLookup threeDSecureLookup = mock(ThreeDSecureLookup.class);
        when(threeDSecureLookup.getTransactionId()).thenReturn("sample-transaction-id");
        when(threeDSecureLookup.getPareq()).thenReturn("sample-payer-authentication-request");

        ThreeDSecureResult threeDSecureResult = mock(ThreeDSecureResult.class);
        when(threeDSecureResult.getLookup()).thenReturn(threeDSecureLookup);

        try {
            sut.continueLookup(activity, threeDSecureResult, cardinalValidateReceiver);
            fail("should not get here");
        } catch (BraintreeException e) {
            assertEquals("Cardinal SDK cca_continue Error.", e.getMessage());
            assertSame(runtimeException, e.getCause());
        }
    }
}
