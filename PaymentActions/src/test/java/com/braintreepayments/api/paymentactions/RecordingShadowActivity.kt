package com.braintreepayments.api.paymentactions

import android.app.Activity
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowActivity

/**
 * Records, in order, how an Activity closes the Activities it started and itself. Robolectric's own
 * [ShadowActivity] doesn't record [Activity.finishActivity].
 */
@Implements(Activity::class)
class RecordingShadowActivity : ShadowActivity() {

    val closeCalls = mutableListOf<String>()

    @Implementation
    protected fun finishActivity(requestCode: Int) {
        closeCalls += "finishActivity($requestCode)"
    }

    @Implementation
    override fun finish() {
        closeCalls += "finish"
        super.finish()
    }
}
