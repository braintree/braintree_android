package com.braintreepayments.api.core

import android.os.Parcelable

/**
 * Base class representing a method of payment for a customer. [PaymentMethodNonce] represents the
 * common interface of all payment method nonces, and can be handled by a server interchangeably.
 *
 * @property string The nonce generated for this payment method by the Braintree gateway. The nonce will
 * represent this PaymentMethod for the purposes of creating transactions and other monetary
 * actions.
 * @property isDefault `true` if this payment method is the default for the current customer, `false` otherwise
 */
abstract class PaymentMethodNonce(
    open val string: String,
    open val isDefault: Boolean
) : Parcelable
