package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import androidx.lifecycle.ViewModel

/**
 * Which XML view shows each view model of a fragment or activity, so two views with the same id can't share one. Kept
 * in the same owner as those view models, so it lives as long as they do.
 */
internal class AttachedViews : ViewModel() {

    // View model key to the instance id of the view showing it.
    private val viewByKey = mutableMapOf<String, String>()

    /** Whether another view shows the view model under [key]. */
    fun isAttachedToAnotherView(key: String, viewInstanceId: String): Boolean {
        val attached = viewByKey[key]
        return attached != null && attached != viewInstanceId
    }

    /** Records that the view shows the view model under [key]. Returns `false` if another view already does. */
    fun attach(key: String, viewInstanceId: String): Boolean {
        if (isAttachedToAnotherView(key, viewInstanceId)) return false
        viewByKey[key] = viewInstanceId
        return true
    }

    /** Records that the view no longer shows the view model under [key]. */
    fun detach(key: String, viewInstanceId: String) {
        if (viewByKey[key] == viewInstanceId) viewByKey.remove(key)
    }
}

/** One view's place in [AttachedViews] for the view model under [key]. */
internal class ViewModelAttachment(
    private val attachedViews: AttachedViews,
    private val key: String,
    private val viewInstanceId: String
) {
    /** Returns `false` if another view with the same id already shows the view model. */
    fun attach(): Boolean = attachedViews.attach(key, viewInstanceId)

    fun detach() = attachedViews.detach(key, viewInstanceId)
}
