package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.os.Looper
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.robolectric.Shadows.shadowOf

/** The accessibility node TalkBack reads for this view. */
internal fun View.nodeInfo(): AccessibilityNodeInfoCompat =
    AccessibilityNodeInfoCompat.obtain().also { ViewCompat.onInitializeAccessibilityNodeInfo(this, it) }

/** The label TalkBack reads for this view's click action, or `null` if it has no labelled click action. */
internal fun View.clickActionLabel(): CharSequence? =
    nodeInfo().actionList.firstOrNull { it.id == AccessibilityNodeInfoCompat.ACTION_CLICK && it.label != null }?.label

/** Runs everything posted to the main looper. */
internal fun idle() = shadowOf(Looper.getMainLooper()).idle()
