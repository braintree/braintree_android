package com.braintreepayments.api.core

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity

// NEXT MAJOR VERSION: Convert to data class, we're unable to do so now because the
// counterpart Java class is technically extendable, and making this a data class would
// result in a breaking change
@Entity(tableName = "analytics_event")
internal class AnalyticsEvent(
    val name: String,

    @ColumnInfo(name = "paypal_context_id")
    val payPalContextId: String? = null,

    @ColumnInfo(name = "link_type")
    val linkType: String? = null,

    @ColumnInfo(name = "venmo_installed", defaultValue = "0")
    val venmoInstalled: Boolean = false,

    @ColumnInfo(name = "is_vault", defaultValue = "0") val isVaultRequest: Boolean = false,

    @ColumnInfo(name = "start_time", defaultValue = "-1")
    val startTime: Long? = -1,

    @ColumnInfo(name = "end_time", defaultValue = "-1")
    val endTime: Long? = -1,

    @ColumnInfo(name = "endpoint")
    val endpoint: String? = null,

    val timestamp: Long = System.currentTimeMillis()
) {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0L
}
