package com.braintreepayments.api.threedsecure

import android.content.Context
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.cardinalcommerce.ThreeDotOh.CardinalService
import com.cardinalcommerce.ThreeDotOh.models.enums.CardinalEnvironment
import com.cardinalcommerce.ThreeDotOh.models.enums.CardinalRenderType
import com.cardinalcommerce.ThreeDotOh.models.enums.CardinalUiType
import com.cardinalcommerce.ThreeDotOh.models.CardinalChallengeObserver
import com.cardinalcommerce.ThreeDotOh.models.CardinalChallengeParameters
import com.cardinalcommerce.ThreeDotOh.models.CardinalConfigurationParameters
import com.cardinalcommerce.ThreeDotOh.interfaces.CardinalInitializeCallback as ThreeDotOhCardinalInitializeCallback
import com.cardinalcommerce.shared.cs.interfaces.Error
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
            CardinalService.getInstance()
                .initialize(context,
                    configuration.cardinalAuthenticationJwt,
                    CardinalConfigurationParameters(),
                    object : ThreeDotOhCardinalInitializeCallback {
                        override fun onSuccess(sdkTransactionID: String?) {
                            TODO("Not yet implemented")
                        }

                        override fun onError(error: Error<Int>?) {
                            TODO("Not yet implemented")
                        }

                    }
                )
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
            CardinalService.getInstance().doChallenge(challengeObserver, CardinalChallengeParameters("threeDSServerTransactionId", "acsTransactionId", "acsReferenceNumber", "acsSignedContent", "threeDSRequestorAppURL", transactionId), 60000)
//            Cardinal.getInstance().cca_continue(transactionId, paReq, challengeObserver)
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
        val cardinalEnvironment = CardinalEnvironment.STAGING
//            if ("production".equals(configuration.environment, ignoreCase = true)) {
//                CardinalEnvironment.PRODUCTION // doesn't exist right now
//            } else {
//                CardinalEnvironment.STAGING
//            }

        val cardinalConfigurationParameters = CardinalConfigurationParameters().apply {
            environment = cardinalEnvironment
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

        request.requestorAppUrl?.let {
            cardinalConfigurationParameters.threeDSRequestorAppURL = it
        }
//        try {
//            CardinalService.getInstance().configure(context, cardinalConfigurationParameters)
//        } catch (e: RuntimeException) {
//            throw BraintreeException("Cardinal SDK configure Error.", e)
//        }
    }

    fun cleanup() {
        CardinalService.getInstance().cleanup()
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
