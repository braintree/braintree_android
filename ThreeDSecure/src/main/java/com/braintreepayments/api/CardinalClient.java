package com.braintreepayments.api;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver;

public class CardinalClient {

    private String consumerSessionId;

    public static CardinalClient newInstance() {
        return new CardinalClient();
    }

    private CardinalClient () {}

    public void initialize(Context context, Configuration configuration, final ThreeDSecureRequest request, final CardinalInitializeCallback callback) {
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

    public void continueLookup(FragmentActivity activity, ThreeDSecureLookup lookup, CardinalValidateReceiver validateReceiver) {
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
        cardinalConfigurationParameters.setUICustomization(request.getUiCustomization());

        Cardinal.getInstance().configure(context, cardinalConfigurationParameters);
    }

    public String getConsumerSessionId() {
        return consumerSessionId;
    }
}
