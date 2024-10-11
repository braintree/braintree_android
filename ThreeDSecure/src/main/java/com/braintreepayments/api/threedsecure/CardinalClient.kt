package com.braintreepayments.api.threedsecure

import android.content.Context
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalRenderType
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import org.json.JSONArray

internal class CardinalClient {
    var consumerSessionId: String? = null
        private set

    @Throws(BraintreeException::class)
    @Suppress("TooGenericExceptionCaught")
    fun initialize(
        context: Context,
        configuration: Configuration,
        request: ThreeDSecureRequest,
        callback: CardinalInitializeCallback
    ) {
        configureCardinal(context, configuration, request)

        try {
            val cardinalInitService = object : CardinalInitService {
                override fun onSetupCompleted(sessionId: String) {
                    consumerSessionId = sessionId
                    callback.onResult(consumerSessionId, null)
                }

                override fun onValidated(validateResponse: ValidateResponse?, serverJWT: String?) {
                    if (consumerSessionId == null) {
                        callback.onResult(
                            consumerSessionId = null,
                            error = BraintreeException("consumer session id not available")
                        )
                    } else {
                        callback.onResult(consumerSessionId, null)
                    }
                }
            }

            Cardinal.getInstance()
                .init(configuration.cardinalAuthenticationJwt, cardinalInitService)
        } catch (e: RuntimeException) {
            throw BraintreeException("Cardinal SDK init Error.", e)
        }
    }

    @Throws(BraintreeException::class)
    @Suppress("TooGenericExceptionCaught")
    fun continueLookup(
        threeDSecureParams: ThreeDSecureParams,
        challengeObserver: CardinalChallengeObserver?
    ) {
        if (challengeObserver == null) throw BraintreeException("challengeObserver is null")

        val lookup = threeDSecureParams.lookup
        val transactionId = lookup?.transactionId
        val paReq = lookup?.pareq
        try {
            Cardinal.getInstance().cca_continue(transactionId, paReq, challengeObserver)
        } catch (e: RuntimeException) {
            throw BraintreeException("Cardinal SDK cca_continue Error.", e)
        }
    }

    @Throws(BraintreeException::class)
    @Suppress("TooGenericExceptionCaught")
    private fun configureCardinal(
        context: Context,
        configuration: Configuration,
        request: ThreeDSecureRequest
    ) {
        val cardinalEnvironment =
            if ("production".equals(configuration.environment, ignoreCase = true)) {
                CardinalEnvironment.PRODUCTION
            } else {
                CardinalEnvironment.STAGING
            }

        val cardinalConfigurationParameters = CardinalConfigurationParameters().apply {
            environment = cardinalEnvironment
            requestTimeout = REQUEST_TIMEOUT
            isEnableDFSync = true
        }

        cardinalConfigurationParameters.uiType = when (request.uiType) {
            ThreeDSecureUiType.NATIVE -> CardinalUiType.NATIVE
            ThreeDSecureUiType.HTML -> CardinalUiType.HTML
            ThreeDSecureUiType.BOTH -> CardinalUiType.BOTH
        }

        request.renderTypes?.let { nonNullRenderType ->
            val renderTypes = JSONArray()
            nonNullRenderType.forEach { renderType ->
                renderTypes.put(getCardinalRenderType(renderType))
            }
            cardinalConfigurationParameters.renderType = renderTypes
        }

        request.v2UiCustomization?.let {
            cardinalConfigurationParameters.uiCustomization = it.cardinalUiCustomization
        }

        try {
            Cardinal.getInstance().configure(context, cardinalConfigurationParameters)
        } catch (e: RuntimeException) {
            throw BraintreeException("Cardinal SDK configure Error.", e)
        }
    }

    fun cleanup() {
        Cardinal.getInstance().cleanup()
    }

    private fun getCardinalRenderType(
        threeDSecureRenderType: ThreeDSecureRenderType
    ): CardinalRenderType {
        return when (threeDSecureRenderType) {
            ThreeDSecureRenderType.OTP -> CardinalRenderType.OTP
            ThreeDSecureRenderType.SINGLE_SELECT -> CardinalRenderType.SINGLE_SELECT
            ThreeDSecureRenderType.MULTI_SELECT -> CardinalRenderType.MULTI_SELECT
            ThreeDSecureRenderType.OOB -> CardinalRenderType.OOB
            ThreeDSecureRenderType.RENDER_HTML -> CardinalRenderType.HTML
        }
    }

    companion object {
        private const val REQUEST_TIMEOUT = 8000
    }
}
