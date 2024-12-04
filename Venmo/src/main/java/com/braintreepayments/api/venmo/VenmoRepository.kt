package com.braintreepayments.api.venmo

import android.net.Uri

/**
 * An internal, in memory repository that holds properties specific for the Venmo payment flow.
 */
internal class VenmoRepository {

    /**
     * The Venmo URL that is used to load the CCT or app switch into the Venmo payment flow.
     */
    var venmoUrl: Uri? = null

    companion object {

        /**
         * Singleton instance of the VenmoRepository.
         */
        val instance: VenmoRepository by lazy { VenmoRepository() }
    }
}
