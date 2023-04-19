package com.braintreepayments.api;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver;

class CardinalClient {

    private String consumerSessionId;

    CardinalClient () {}

    void initialize(Context context, Configuration configuration, final ThreeDSecureRequest request, final CardinalInitializeCallback callback) throws BraintreeException {
        configureCardinal(context, configuration, request);

        CardinalInitService cardinalInitService = new CardinalInitService() {
            @Override
            public void onSetupCompleted(String sessionId) {
                consumerSessionId = sessionId;
                callback.onResult(consumerSessionId, null);
            }

            @Override
            public void onValidated(ValidateResponse validateResponse, String serverJWT) {
                if (consumerSessionId == null) {
                    callback.onResult(null, new BraintreeException("consumer session id not available"));
                } else {
                    callback.onResult(consumerSessionId, null);
                }
            }
        };

        try {
            Cardinal.getInstance().init(configuration.getCardinalAuthenticationJwt(), cardinalInitService);
        } catch (RuntimeException e) {
            throw new BraintreeException("Cardinal SDK init Error.", e);
        }
    }

    void continueLookup(FragmentActivity activity, ThreeDSecureResult threeDSecureResult, CardinalValidateReceiver validateReceiver) throws BraintreeException {
        ThreeDSecureLookup lookup = threeDSecureResult.getLookup();
        String transactionId = lookup.getTransactionId();
        String paReq = lookup.getPareq();
        try {
            Cardinal.getInstance().cca_continue(transactionId, paReq, activity, validateReceiver);
        } catch (RuntimeException e) {
            throw new BraintreeException("Cardinal SDK cca_continue Error.", e);
        }
    }

    private void configureCardinal(Context context, Configuration configuration, ThreeDSecureRequest request) throws BraintreeException {
        CardinalEnvironment cardinalEnvironment = CardinalEnvironment.STAGING;
        if ("production".equalsIgnoreCase(configuration.getEnvironment())) {
            cardinalEnvironment = CardinalEnvironment.PRODUCTION;
        }

        CardinalConfigurationParameters cardinalConfigurationParameters = new CardinalConfigurationParameters();
        cardinalConfigurationParameters.setEnvironment(cardinalEnvironment);
        cardinalConfigurationParameters.setRequestTimeout(8000);
        cardinalConfigurationParameters.setEnableDFSync(true);
        if (request.getV2UiCustomization() != null) {
            cardinalConfigurationParameters.setUICustomization(request.getV2UiCustomization().getCardinalUiCustomization());
        }

        try {
            Cardinal.getInstance().configure(context, cardinalConfigurationParameters);
        } catch (RuntimeException e) {
            throw new BraintreeException("Cardinal SDK configure Error.", e);
        }
    }

    String getConsumerSessionId() {
        return consumerSessionId;
    }
}
