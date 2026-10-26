package com.braintreepayments.api.uicomponents.compose

import android.content.Intent
import androidx.activity.ComponentActivity

/**
 * SingleTop host for the compose payment buttons under test. New intents update the activity
 * intent, which is the source the buttons read when a browser switch flow returns on resume.
 */
class ButtonFlowTestActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
