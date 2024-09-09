package com.braintreepayments.api.googlepay

import android.os.Parcelable
import com.braintreepayments.api.core.BraintreeException
import com.google.android.gms.common.api.Status
import kotlinx.parcelize.Parcelize

/**
 *
 * Error class thrown when a Google Pay exception is encountered.
 *
 * @property message Human readable top level summary of the error.
 * @property status The object that contains more details about the error and how to resolve it.
 */
@Parcelize
internal class GooglePayException(
    override val message: String? = null,
    val status: Status? = null,
) : BraintreeException(), Parcelable
