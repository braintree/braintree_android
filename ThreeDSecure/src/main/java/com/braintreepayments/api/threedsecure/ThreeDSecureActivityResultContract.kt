package com.braintreepayments.api.threedsecure

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.UserCanceledException
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse

internal class ThreeDSecureActivityResultContract :
    ActivityResultContract<ThreeDSecureParams?, ThreeDSecurePaymentAuthResult?>() {

    override fun createIntent(context: Context, input: ThreeDSecureParams?): Intent {
        val intent = Intent(context, ThreeDSecureActivity::class.java)

        val extras = Bundle()
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, input)
        intent.putExtras(extras)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ThreeDSecurePaymentAuthResult {
        return when {
            resultCode == Activity.RESULT_CANCELED -> {
                ThreeDSecurePaymentAuthResult(error = UserCanceledException("User canceled 3DS."))
            }

            intent == null -> {
                val unknownErrorMessage =
                    "An unknown Android error occurred with the activity result API."
                ThreeDSecurePaymentAuthResult(error = BraintreeException(unknownErrorMessage))
            }

            resultCode == ThreeDSecureActivity.RESULT_COULD_NOT_START_CARDINAL -> {
                val errorMessage = intent.getStringExtra(ThreeDSecureActivity.EXTRA_ERROR_MESSAGE)
                ThreeDSecurePaymentAuthResult(error = BraintreeException(errorMessage))
            }

            else -> {
                val threeDSecureParams = intent.getParcelableExtra<ThreeDSecureParams>(
                    ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT
                )
                val validateResponse = intent.getSerializableExtra(
                    ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE
                ) as ValidateResponse?
                ThreeDSecurePaymentAuthResult(
                    jwt = intent.getStringExtra(ThreeDSecureActivity.EXTRA_JWT),
                    validateResponse = validateResponse,
                    threeDSecureParams = threeDSecureParams
                )
            }
        }
    }
}
