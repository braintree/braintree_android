package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/** Blocks the screen while the edit flow runs, so the buyer can't tap Edit twice. */
internal class EditLoadingDialog(context: Context) : Dialog(context) {

    init {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        val spinnerSize = context.resources.getDimensionPixelSize(
            R.dimen.paypal_saved_payment_method_loading_dialog_spinner_size
        )
        // Spinner only: the same loader shows while the order is created and while tokenizing, so no single message
        // fits both.
        val spinner = ProgressBar(context).apply {
            isIndeterminate = true
            // The platform spinner's stroke and padding can't be set, so a drawable gives it the Compose spinner's size
            // and stroke.
            indeterminateDrawable =
                ContextCompat.getDrawable(context, R.drawable.paypal_saved_payment_method_loading_spinner)
            contentDescription =
                context.getString(R.string.paypal_saved_payment_method_loading_dialog_content_description)
        }
        val root = FrameLayout(context).apply {
            setBackgroundColor(
                ContextCompat.getColor(context, R.color.paypal_saved_payment_method_loading_dialog_background)
            )
            addView(spinner, FrameLayout.LayoutParams(spinnerSize, spinnerSize, Gravity.CENTER))
        }
        setContentView(root)
        // Clears the window's default dim and background, so only the translucent background above shows.
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setDimAmount(0f)
        }
    }
}
