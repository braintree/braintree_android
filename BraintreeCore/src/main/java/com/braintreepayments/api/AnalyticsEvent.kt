package com.braintreepayments.api

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import kotlinx.serialization.Serializable

// NEXT MAJOR VERSION: Convert to data class, we're unable to do so now because the
// counterpart Java class is technically extendable, and making this a data class would
// result in a breaking change

// NEXT MAJOR VERSION: remove open modifiers

@Entity(tableName = "analytics_event")
data class AnalyticsEvent internal constructor(
    val name: String,

//    @ColumnInfo(name = "paypal_context_id")
//    val payPalContextId: String? = null,
//
//    @ColumnInfo(name = "link_type")
//    val linkType: String? = null,

    val timestamp: Long = System.currentTimeMillis(),

//    @ColumnInfo(name = "venmo_installed", defaultValue = "0")
//    val venmoInstalled: Boolean = false,

    @Embedded
    val payload: AnalyticsEventParams

//    @ColumnInfo(name = "is_vault", defaultValue = "0")
//    val isVaultRequest: Boolean = false
) {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0L
}
