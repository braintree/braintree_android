package com.braintreepayments.api

import android.content.Context
import org.json.JSONArray

internal class CardinalClient {
    var consumerSessionId: String? = null
        private set

    @Throws(BraintreeException::class)
    fun initialize(
        context: Context,
        configuration: Configuration,
        request: ThreeDSecureRequest,
        callback: CardinalInitializeCallback
    ) {
        configureCardinal(context, configuration, request)
        val cardinalInitService: CardinalInitService = object : CardinalInitService() {
            fun onSetupCompleted(sessionId: String?) {
                consumerSessionId = sessionId
                callback.onResult(consumerSessionId, null)
            }

            fun onValidated(validateResponse: ValidateResponse?, serverJWT: String?) {
                if (consumerSessionId == null) {
                    callback.onResult(null, BraintreeException("consumer session id not available"))
                } else {
                    callback.onResult(consumerSessionId, null)
                }
            }
        }
        try {
            Cardinal.getInstance()
                .init(configuration.cardinalAuthenticationJwt, cardinalInitService)
        } catch (e: RuntimeException) {
            throw BraintreeException("Cardinal SDK init Error.", e)
        }
    }

    @Throws(BraintreeException::class)
    fun continueLookup(
        threeDSecureResult: ThreeDSecureResult,
        challengeObserver: CardinalChallengeObserver?
    ) {
        val lookup = threeDSecureResult.lookup
        val transactionId = lookup.transactionId
        val paReq = lookup.pareq
        try {
            Cardinal.getInstance().cca_continue(transactionId, paReq, challengeObserver)
        } catch (e: RuntimeException) {
            throw BraintreeException("Cardinal SDK cca_continue Error.", e)
        }
        Cardinal.getInstance().cleanup()
    }

    @Throws(BraintreeException::class)
    private fun configureCardinal(
        context: Context,
        configuration: Configuration,
        request: ThreeDSecureRequest
    ) {
        var cardinalEnvironment: CardinalEnvironment = CardinalEnvironment.STAGING
        if ("production".equals(configuration.environment, ignoreCase = true)) {
            cardinalEnvironment = CardinalEnvironment.PRODUCTION
        }
        val cardinalConfigurationParameters = CardinalConfigurationParameters()
        cardinalConfigurationParameters.setEnvironment(cardinalEnvironment)
        cardinalConfigurationParameters.setRequestTimeout(8000)
        cardinalConfigurationParameters.setEnableDFSync(true)
        when (request.uiType) {
            ThreeDSecureRequest.NATIVE -> {
                cardinalConfigurationParameters.setUiType(CardinalUiType.NATIVE)
                cardinalConfigurationParameters.setUiType(CardinalUiType.HTML)
                cardinalConfigurationParameters.setUiType(CardinalUiType.BOTH)
            }

            ThreeDSecureRequest.HTML -> {
                cardinalConfigurationParameters.setUiType(CardinalUiType.HTML)
                cardinalConfigurationParameters.setUiType(CardinalUiType.BOTH)
            }

            ThreeDSecureRequest.BOTH -> cardinalConfigurationParameters.setUiType(CardinalUiType.BOTH)
        }
        if (request.renderTypes != null) {
            val renderTypes = JSONArray()
            for (renderType in request.renderTypes) {
                if (renderType == ThreeDSecureRequest.OTP) {
                    renderTypes.put(CardinalRenderType.OTP)
                } else if (renderType == ThreeDSecureRequest.SINGLE_SELECT) {
                    renderTypes.put(CardinalRenderType.SINGLE_SELECT)
                } else if (renderType == ThreeDSecureRequest.MULTI_SELECT) {
                    renderTypes.put(CardinalRenderType.MULTI_SELECT)
                } else if (renderType == ThreeDSecureRequest.OOB) {
                    renderTypes.put(CardinalRenderType.OOB)
                } else if (renderType == ThreeDSecureRequest.RENDER_HTML) {
                    renderTypes.put(CardinalRenderType.HTML)
                }
            }
            cardinalConfigurationParameters.setRenderType(renderTypes)
        }
        if (request.v2UiCustomization != null) {
            cardinalConfigurationParameters.setUICustomization(request.v2UiCustomization!!.cardinalUiCustomization)
        }
        try {
            Cardinal.getInstance().configure(context, cardinalConfigurationParameters)
        } catch (e: RuntimeException) {
            throw BraintreeException("Cardinal SDK configure Error.", e)
        }
    }
}