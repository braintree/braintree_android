package com.braintreepayments.api.threedsecure

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.threedsecure.ThreeDSecureActivity.Companion.RESULT_COULD_NOT_START_CARDINAL
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureActivityUnitTest {

    private val originalFactory = ThreeDSecureActivity.challengeObserverFactory

    @Before
    fun setUp() {
        // The real CardinalChallengeObserver constructor requires a configured Cardinal SDK.
        ThreeDSecureActivity.challengeObserverFactory = { null }
    }

    @After
    fun tearDown() {
        ThreeDSecureActivity.challengeObserverFactory = originalFactory
    }

    @Test
    fun `onCreate with extras invokes cardinal with lookup data`() {
        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
        val extras = Bundle()
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureParams)
        val intent = Intent()
        intent.putExtras(extras)

        val sut = ThreeDSecureActivity()
        sut.intent = intent

        val cardinalClient = mockk<CardinalClient>(relaxed = true)
        sut.launchCardinalAuthChallenge(cardinalClient)

        val paramsSlot = slot<ThreeDSecureParams>()
        verify { cardinalClient.continueLookup(capture(paramsSlot), any()) }

        val actualResult = paramsSlot.captured
        val actualLookup = actualResult.lookup
        assertEquals("sample-transaction-id", actualLookup?.transactionId)
        assertEquals("sample-pareq", actualLookup?.pareq)
    }

    @Test
    fun `onCreate with extras and cardinal error finishes with error`() {
        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
        val extras = Bundle()
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureParams)
        val intent = Intent()
        intent.putExtras(extras)

        val sut = spyk<ThreeDSecureActivity>()
        sut.intent = intent

        val cardinalError = BraintreeException("fake cardinal error")
        val cardinalClient = mockk<CardinalClient>(relaxed = true)
        every { cardinalClient.continueLookup(any<ThreeDSecureParams>(), any()) } throws cardinalError

        sut.launchCardinalAuthChallenge(cardinalClient)
        verify { sut.finish() }

        val slot = slot<Intent>()
        verify { sut.setResult(RESULT_COULD_NOT_START_CARDINAL, capture(slot)) }
        val intentForResult = slot.captured
        assertEquals("fake cardinal error", intentForResult.getStringExtra(ThreeDSecureActivity.EXTRA_ERROR_MESSAGE))
    }

    @Test
    fun `onCreate without extras finishes with error`() {
        val intent = Intent()
        val sut = spyk<ThreeDSecureActivity>()
        sut.intent = intent

        val cardinalClient = mockk<CardinalClient>(relaxed = true)
        sut.launchCardinalAuthChallenge(cardinalClient)

        verify(exactly = 0) {
            cardinalClient.continueLookup(any<ThreeDSecureParams>(), any<CardinalChallengeObserver>())
        }
        verify { sut.finish() }

        val slot = slot<Intent>()
        verify { sut.setResult(RESULT_COULD_NOT_START_CARDINAL, capture(slot)) }
        val intentForResult = slot.captured
        assertEquals(
            "Unable to launch 3DS authentication.",
            intentForResult.getStringExtra(ThreeDSecureActivity.EXTRA_ERROR_MESSAGE)
        )
    }

    @Test
    fun `handleValidated returns validation results`() {
        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
        val extras = Bundle()
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, threeDSecureParams)
        val intent = Intent()
        intent.putExtras(extras)
        val sut = spyk<ThreeDSecureActivity>()
        sut.intent = intent

        val cardinalValidateResponse = mockk<ValidateResponse>(relaxed = true)
        every { cardinalValidateResponse.actionCode } returns CardinalActionCode.SUCCESS
        val cardinalClient = mockk<CardinalClient>(relaxed = true)
        sut.handleValidated(cardinalClient, cardinalValidateResponse, "jwt")
        verify { sut.finish() }

        val slot = slot<Intent>()
        verify { sut.setResult(RESULT_OK, capture(slot)) }
        verify { cardinalClient.cleanup() }

        val intentForResult = slot.captured
        val activityResult =
            intentForResult.getSerializableExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE) as ValidateResponse
        assertEquals("jwt", intentForResult.getStringExtra(ThreeDSecureActivity.EXTRA_JWT))
        assertEquals(
            threeDSecureParams,
            intentForResult.getParcelableExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT)
        )
        assertNotNull(activityResult)
        assertEquals("SUCCESS", activityResult.actionCode.string)
    }

    // With no lookup extras, launchCardinalAuthChallenge finishes the Activity with
    // RESULT_COULD_NOT_START_CARDINAL, which signals that onCreate scheduled the launch.
    @Test
    fun `onCreate with null savedInstanceState schedules the auth challenge launch`() {
        val controller = Robolectric.buildActivity(ThreeDSecureActivity::class.java, Intent())

        controller.create(null)
        shadowOf(Looper.getMainLooper()).idle()

        val activity = controller.get()
        assertTrue(activity.isFinishing)
        assertEquals(RESULT_COULD_NOT_START_CARDINAL, shadowOf(activity).resultCode)
    }

    @Test
    fun `onCreate with non-null savedInstanceState does not relaunch the auth challenge`() {
        val controller = Robolectric.buildActivity(ThreeDSecureActivity::class.java, Intent())

        controller.create(Bundle())
        shadowOf(Looper.getMainLooper()).idle()

        val activity = controller.get()
        assertFalse(activity.isFinishing)
        assertNull(shadowOf(activity).resultIntent)
    }
}
