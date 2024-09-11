package com.braintreepayments.api.threedsecure

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.threedsecure.ThreeDSecureParams.Companion.fromJson
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertSame
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureActivityResultContractUnitTest {

    private var context: Context? = null
    private var threeDSecureParams: ThreeDSecureParams? = null

    private var sut: ThreeDSecureActivityResultContract? = null

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        context = ApplicationProvider.getApplicationContext()
        threeDSecureParams = fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
    }

    @Test
    fun `createIntent returns Intent with extras`() {
        sut = ThreeDSecureActivityResultContract()
        val result = sut!!.createIntent(context!!, threeDSecureParams)

        val extraThreeDSecureParams =
            result.getParcelableExtra<ThreeDSecureParams>(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT)
        assertSame(threeDSecureParams, extraThreeDSecureParams)
    }

    @Test
    fun `parseResult when result is ok returns Cardinal result with success data`() {
        sut = ThreeDSecureActivityResultContract()

        val successIntent = Intent()
        successIntent.putExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureParams)

        val validateResponse = Mockito.mock(
            ValidateResponse::class.java
        )
        successIntent.putExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE, validateResponse)

        val jwt = "sample-jwt"
        successIntent.putExtra(ThreeDSecureActivity.EXTRA_JWT, jwt)

        val paymentAuthResult = sut!!.parseResult(Activity.RESULT_OK, successIntent)
        assertNotNull(paymentAuthResult)

        assertSame(threeDSecureParams, paymentAuthResult.threeDSecureParams)
        assertSame(validateResponse, paymentAuthResult.validateResponse)
        assertSame(jwt, paymentAuthResult.jwt)
    }

    @Test
    fun `parseResult when result is OK and intent is null returns Cardinal result with error`() {
        sut = ThreeDSecureActivityResultContract()

        val paymentAuthResult = sut!!.parseResult(Activity.RESULT_OK, null)
        assertNotNull(paymentAuthResult)

        val error = paymentAuthResult.error as BraintreeException
        val expectedMessage = "An unknown Android error occurred with the activity result API."
        assertNotNull(expectedMessage, error.message)
    }

    @Test
    fun `parseResult when result is CANCELED returns Cardinal result with error`() {
        sut = ThreeDSecureActivityResultContract()

        val paymentAuthResult = sut!!.parseResult(Activity.RESULT_CANCELED, Intent())
        assertNotNull(paymentAuthResult)

        val error = paymentAuthResult.error as UserCanceledException
        assertEquals("User canceled 3DS.", error.message)
    }

    @Test
    fun `parseResult when result is error and has error message returns Cardinal result with error`() {
        sut = ThreeDSecureActivityResultContract()

        val intent = Intent()
        intent.putExtra(ThreeDSecureActivity.EXTRA_ERROR_MESSAGE, "sample error message")

        val paymentAuthResult =
            sut!!.parseResult(ThreeDSecureActivity.RESULT_COULD_NOT_START_CARDINAL, intent)
        assertNotNull(paymentAuthResult)

        val error = paymentAuthResult.error as BraintreeException
        assertEquals("sample error message", error.message)
    }
}
