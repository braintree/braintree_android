package com.braintreepayments.api;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

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
    public void initialize_configuresDefaultCardinalConfigurationParameters() {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        CardinalClient sut = new CardinalClient();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        sut.initialize(context, configuration, request, cardinalInitializeCallback);

        ArgumentCaptor<CardinalConfigurationParameters> captor = ArgumentCaptor.forClass(CardinalConfigurationParameters.class);
        verify(cardinalInstance).configure(same(context), captor.capture());

        CardinalConfigurationParameters parameters = captor.getValue();
        assertEquals(CardinalEnvironment.STAGING, parameters.getEnvironment());
        assertEquals(8000, parameters.getRequestTimeout());
        assertFalse(parameters.isEnableQuickAuth());
        assertTrue(parameters.isEnableDFSync());
        assertEquals(request.getV2UiCustomization(), parameters.getUICustomization());
    }

    @Test
    public void initialize_whenEnvironmentProduction_configuresCardinalEnvironmentProduction() {
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
    public void initialize_returnsConsumerSessionIdToListener() {
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
    public void initialize_whenConsumerSessionIdIsNull_returnsBraintreeExceptionToListener() {
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
    public void continueLookup_continuesCardinalLookup() {
        when(Cardinal.getInstance()).thenReturn(cardinalInstance);
        CardinalClient sut = new CardinalClient();

        ThreeDSecureLookup threeDSecureLookup = mock(ThreeDSecureLookup.class);
        when(threeDSecureLookup.getTransactionId()).thenReturn("sample-transaction-id");
        when(threeDSecureLookup.getPareq()).thenReturn("sample-payer-authentication-request");

        sut.continueLookup(activity, threeDSecureLookup, cardinalValidateReceiver);

        verify(cardinalInstance).cca_continue(
                "sample-transaction-id",
                "sample-payer-authentication-request",
                activity,
                cardinalValidateReceiver
        );
    }
}