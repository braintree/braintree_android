package com.braintreepayments.api.uicomponents.cardfields

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.braintreepayments.api.uicomponents.R

internal class CvvHintOverlay(context: Context) {

    private val popupWindow: PopupWindow
    private val popupWidth: Int

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.cvv_hint_overlay, null)
        popupWidth = context.resources.getDimensionPixelSize(R.dimen.cvv_overlay_max_width)

        val cornerRadius = context.resources.getDimension(R.dimen.card_field_corner_radius)
        val backgroundColor = ContextCompat.getColor(context, R.color.card_field_background)

        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setStroke(
                context.resources.getDimensionPixelSize(R.dimen.card_field_border_width),
                Color.WHITE
            )
            setColor(backgroundColor)
        }
        view.clipToOutline = true

        val popupBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(backgroundColor)
        }

        popupWindow = PopupWindow(view, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            setBackgroundDrawable(popupBackground)
            isOutsideTouchable = true
            elevation = context.resources.getDimension(R.dimen.cvv_overlay_elevation)
        }

        view.findViewById<View>(R.id.close_button).setOnClickListener { popupWindow.dismiss() }
    }

    fun show(anchor: View) {
        val xOffset = anchor.width - popupWidth
        val contentView = popupWindow.contentView
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val yOffset = -(contentView.measuredHeight + anchor.height)
        popupWindow.showAsDropDown(anchor, xOffset, yOffset)
    }
}
