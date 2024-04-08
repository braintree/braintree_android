package com.braintreepayments.api

import android.content.Context
import com.cardinalcommerce.ThreeDotOh.CardinalService
import com.cardinalcommerce.ThreeDotOh.models.CardinalChallengeObserver
import com.cardinalcommerce.ThreeDotOh.models.CardinalChallengeParameters
import com.cardinalcommerce.ThreeDotOh.models.CardinalConfigurationParameters
import com.cardinalcommerce.ThreeDotOh.models.enums.CardinalEnvironment
import com.cardinalcommerce.ThreeDotOh.models.enums.CardinalRenderType
import com.cardinalcommerce.ThreeDotOh.models.enums.CardinalUiType
import com.cardinalcommerce.shared.cs.interfaces.Error
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
        // TODO: uncomment once PRODUCTION enum is visible to SDK
//        val environment = if ("production".equals(configuration.environment, ignoreCase = true)) {
//            // NOTE: PRODUCTION enum not visible to SDK
//            CardinalEnvironment.PRODUCTION
//        } else {
//            CardinalEnvironment.STAGING
//        }
        val cardinalEnvironment = CardinalEnvironment.STAGING
        val cardinalUIType = when (request.uiType) {
            ThreeDSecureRequest.NATIVE -> CardinalUiType.NATIVE
            ThreeDSecureRequest.HTML -> CardinalUiType.HTML
            else -> CardinalUiType.BOTH
        }

        val cardinalRenderTypes = JSONArray()
        request.renderTypes?.let { renderTypes ->
            for (renderType in renderTypes) {
                val cardinalRenderType = when (renderType) {
                    ThreeDSecureRequest.OTP -> CardinalRenderType.OTP
                    ThreeDSecureRequest.SINGLE_SELECT -> CardinalRenderType.SINGLE_SELECT
                    ThreeDSecureRequest.MULTI_SELECT -> CardinalRenderType.MULTI_SELECT
                    ThreeDSecureRequest.OOB -> CardinalRenderType.OOB
                    ThreeDSecureRequest.RENDER_HTML -> CardinalRenderType.HTML
                    else -> null
                }
                cardinalRenderType?.let { cardinalRenderTypes.put(it) }
            }
        }

        val cardinalParams = CardinalConfigurationParameters().apply {
            environment = cardinalEnvironment
            // QUESTION: is this supposed to be requestTimeout?
            sdkMaxTimeout = 8000
            // QUESTION: is dfsync property no longer needed?
            uiType = cardinalUIType
            renderType = cardinalRenderTypes
        }

        request.v2UiCustomization?.cardinalUiCustomization?.let {
            cardinalParams.uiCustomization = it
        }

        val cardinalService = CardinalService.getInstance()
        val cardinalCallback =
            object : com.cardinalcommerce.ThreeDotOh.interfaces.CardinalInitializeCallback {
                override fun onSuccess(sdkTransactionId: String?) {
                    // QUESTION: is SDK transaction ID the same as Consumer Session ID?
                    consumerSessionId = sdkTransactionId
                    callback.onResult(consumerSessionId, null)
                }

                override fun onError(error: Error<Int>?) {
                    val braintreeException = error?.let {
                        BraintreeException("An 3DS initialize error occurred. Cardinal Error code: $it")
                    }
                    braintreeException?.let { btError ->
                        callback.onResult(null, btError)
                    }
                }
            }
        val serverJWT = configuration.cardinalAuthenticationJwt
        cardinalService.initialize(context, serverJWT, cardinalParams, cardinalCallback)
    }

    @Throws(BraintreeException::class)
    fun continueLookup(
        threeDSecureResult: ThreeDSecureResult,
        challengeObserver: CardinalChallengeObserver?
    ) {
        val lookup = threeDSecureResult.lookup
        val transactionId = lookup.transactionId
        val paReq = lookup.pareq

        val cardinalService = CardinalService.getInstance()
        try {
            // QUESTION: Original integration required two parameters, what are these new required values?
            val challengeParams = CardinalChallengeParameters(
                "",
                "",
                "",
                "",
                "",
                ""
            )
            // QUESTION: What does the timeout parameter do? Is there a good default value we should use?
            cardinalService.doChallenge(challengeObserver, challengeParams, 80000)
        } catch (e: RuntimeException) {
            throw BraintreeException("Cardinal SDK cca_continue Error.", e)
        }
        cardinalService.cleanup()
    }
}