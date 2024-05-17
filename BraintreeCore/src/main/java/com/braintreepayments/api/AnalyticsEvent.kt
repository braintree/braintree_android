package com.braintreepayments.api

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity

// NEXT MAJOR VERSION: Convert to data class, we're unable to do so now because the
// counterpart Java class is technically extendable, and making this a data class would
// result in a breaking change

// NEXT MAJOR VERSION: remove open modifiers

@Entity(tableName = "analytics_event")
open class AnalyticsEvent internal constructor(
    open val name: String,

    @ColumnInfo(name = "paypal_context_id")
    open val payPalContextId: String? = null,

    @ColumnInfo(name = "link_type")
    open val linkType: String? = null,

    open val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "venmo_installed", defaultValue = "0")
    open val venmoInstalled: Boolean = false,

    @ColumnInfo(name = "is_vault", defaultValue = "0")
    open val isVaultRequest: Boolean = false
) {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0L
}
