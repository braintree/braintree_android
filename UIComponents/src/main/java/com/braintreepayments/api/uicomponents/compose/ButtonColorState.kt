package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.VenmoButtonColor

internal val paypalButtonColorSaver: Saver<PayPalButtonColor, Int> =
    Saver(save = { it.key }, restore = { PayPalButtonColor.fromId(it) })

internal val venmoButtonColorSaver: Saver<VenmoButtonColor, Int> =
    Saver(save = { it.key }, restore = { VenmoButtonColor.fromId(it) })

/**
 * Creates and remembers a [PayPalButtonColor] selection that survives recomposition,
 * configuration changes, and process death.
 * @param initial: The [PayPalButtonColor] to use the first time this state is created.
 */
@Composable
fun rememberPayPalButtonColorState(
    initial: PayPalButtonColor = PayPalButtonColor.Blue
): MutableState<PayPalButtonColor> =
    rememberSaveable(stateSaver = paypalButtonColorSaver) { mutableStateOf(initial) }

/**
 * Creates and remembers a [VenmoButtonColor] selection that survives recomposition,
 * configuration changes, and process death.
 * @param initial: The [VenmoButtonColor] to use the first time this state is created.
 */
@Composable
fun rememberVenmoButtonColorState(
    initial: VenmoButtonColor = VenmoButtonColor.Blue
): MutableState<VenmoButtonColor> =
    rememberSaveable(stateSaver = venmoButtonColorSaver) { mutableStateOf(initial) }
