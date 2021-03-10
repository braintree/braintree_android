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

    void initialize(Context context, Configuration configuration, final ThreeDSecureRequest request, final CardinalInitializeCallback callback) {
        configurationCardinal(context, configuration, request);
        Cardinal.getInstance().init(configuration.getCardinalAuthenticationJwt(), new CardinalInitService() {
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
        });
    }

    void continueLookup(FragmentActivity activity, ThreeDSecureLookup lookup, CardinalValidateReceiver validateReceiver) {
        String tranactionId = lookup.getTransactionId();
        String paReq = lookup.getPareq();
        Cardinal.getInstance().cca_continue(tranactionId, paReq, activity, validateReceiver);
    }

    private void configurationCardinal(Context context, Configuration configuration, ThreeDSecureRequest request) {
        CardinalEnvironment cardinalEnvironment = CardinalEnvironment.STAGING;
        if ("production".equalsIgnoreCase(configuration.getEnvironment())) {
            cardinalEnvironment = CardinalEnvironment.PRODUCTION;
        }

        CardinalConfigurationParameters cardinalConfigurationParameters = new CardinalConfigurationParameters();
        cardinalConfigurationParameters.setEnvironment(cardinalEnvironment);
        cardinalConfigurationParameters.setRequestTimeout(8000);
        cardinalConfigurationParameters.setEnableQuickAuth(false);
        cardinalConfigurationParameters.setEnableDFSync(true);
        if (request.getV2UiCustomization() != null) {
            cardinalConfigurationParameters.setUICustomization(request.getV2UiCustomization().getUiCustomization());
        }

        Cardinal.getInstance().configure(context, cardinalConfigurationParameters);
    }

    String getConsumerSessionId() {
        return consumerSessionId;
    }
}
