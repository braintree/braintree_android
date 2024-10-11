package com.braintreepayments.api.threedsecure

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.sharedutils.IntentExtensions.parcelable
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse

/**
 * The Activity that receives Cardinal SDK result from 3DS v2 flow
 */
internal class ThreeDSecureActivity : AppCompatActivity() {
    private val cardinalClient = CardinalClient()
    private var challengeObserver: CardinalChallengeObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        challengeObserver =
            CardinalChallengeObserver(this) { _, validateResponse: ValidateResponse?, s: String? ->
                handleValidated(cardinalClient, validateResponse, s)
            }

        /**
         * Here, we schedule the 3DS auth challenge launch to run immediately after onCreate() is
         * complete. This gives the CardinalValidateReceiver callback a chance to run before 3DS
         * is initiated.
         */
        Handler(Looper.getMainLooper()).post {
            launchCardinalAuthChallenge(cardinalClient)
        }
    }

    @VisibleForTesting
    fun launchCardinalAuthChallenge(cardinalClient: CardinalClient) {
        if (isFinishing) {
            /**
             * After a process kill, we may have already parsed a Cardinal result via the
             * CardinalChallengeObserver and are in the process of propagating the result back to
             * the merchant app.
             *
             * This guard prevents the Cardinal Auth Challenge from being shown while the Activity
             * is finishing.
             */
            return
        }

        val extras = intent.extras ?: Bundle()
        val threeDSecureParams = extras.parcelable<ThreeDSecureParams>(EXTRA_THREE_D_SECURE_RESULT)
        if (threeDSecureParams != null) {
            try {
                cardinalClient.continueLookup(threeDSecureParams, challengeObserver)
            } catch (e: BraintreeException) {
                finishWithError(e.message)
            }
        } else {
            finishWithError("Unable to launch 3DS authentication.")
        }
    }

    private fun finishWithError(errorMessage: String?) {
        val result = Intent()
        result.putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        setResult(RESULT_COULD_NOT_START_CARDINAL, result)
        finish()
    }

    @VisibleForTesting
    fun handleValidated(
        cardinalClient: CardinalClient,
        validateResponse: ValidateResponse?,
        jwt: String?
    ) {
        cardinalClient.cleanup()

        val result = Intent()
        result.putExtra(EXTRA_JWT, jwt)
        result.putExtra(
            EXTRA_THREE_D_SECURE_RESULT,
            intent.extras?.parcelable<ThreeDSecureParams>(EXTRA_THREE_D_SECURE_RESULT)
        )
        result.putExtra(EXTRA_VALIDATION_RESPONSE, validateResponse)

        setResult(RESULT_OK, result)
        finish()
    }

    companion object {
        const val EXTRA_ERROR_MESSAGE: String =
            "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_ERROR_MESSAGE"
        const val EXTRA_THREE_D_SECURE_RESULT: String =
            "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT"
        const val EXTRA_VALIDATION_RESPONSE: String =
            "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE"
        const val EXTRA_JWT: String = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_JWT"
        const val RESULT_COULD_NOT_START_CARDINAL: Int = RESULT_FIRST_USER
    }
}
