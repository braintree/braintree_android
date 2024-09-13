package com.braintreepayments.api.googlepay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Used to set shipping requirements.
 * @param allowedCountryCodes Optional. The ISO 3166-1 alpha-2 country code values of the countries where
 * shipping is allowed. If not set, all shipping address countries are allowed.
 * @param isPhoneNumberRequired Optional. Set `true` if a phone number is required for the shipping address.
 */
@Parcelize
data class GooglePayShippingAddressParameters @JvmOverloads constructor(
    var allowedCountryCodes: @RawValue List<String>? = null,
    var isPhoneNumberRequired: Boolean = false
) : Parcelable
