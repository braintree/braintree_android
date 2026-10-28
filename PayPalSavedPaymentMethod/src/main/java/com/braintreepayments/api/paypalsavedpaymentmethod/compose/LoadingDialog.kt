package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.drawable.toDrawable
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/** Blocks the screen while the edit flow runs, so the buyer can't tap Edit twice. */
@Composable
internal fun LoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            // Draws behind the status and navigation bars, so the scrim covers the whole screen with edge-to-edge.
            decorFitsSystemWindows = false
        )
    ) {
        // Clears the dialog's default dim and background, so only the translucent background below shows.
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        SideEffect {
            dialogWindowProvider?.window?.let { window ->
                window.setDimAmount(0f)
                window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.paypal_saved_payment_method_loading_dialog_background)),
            contentAlignment = Alignment.Center
        ) {
            val loadingDescription =
                stringResource(R.string.paypal_saved_payment_method_loading_dialog_content_description)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.paypal_saved_payment_method_loading_dialog_spinner_size))
                    .semantics { contentDescription = loadingDescription },
                color = colorResource(R.color.paypal_saved_payment_method_loading_dialog_content),
                strokeWidth = dimensionResource(R.dimen.paypal_saved_payment_method_loading_dialog_spinner_stroke_width)
            )
        }
    }
}
