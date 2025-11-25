package com.braintreepayments.api.uicomponents

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback

/**
 * A customizable PayPal branded button that handles the complete PayPal payment flow.
 */
class PayPalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var currentStyle: PayPalButtonColor = PayPalButtonColor.BLUE
    private val gradientDrawable = GradientDrawable()
    private var logo: Drawable? = null

    private val logoOffset = resources.getDimension(R.dimen.pp_logo_offset).toInt()
    private val desiredWidth = resources.getDimension(R.dimen.pay_button_width).toInt()
    private val desiredHeight = resources.getDimension(R.dimen.pay_button_height).toInt()
    private val minDesiredWidth = resources.getDimension(R.dimen.pay_button_min_width).toInt()

    private var payPalRequest: PayPalRequest? = null
    private var launchCallback: PayPalButtonLaunchCallback? = null
    private var payPalClient: PayPalClient? = null
    private var payPalPaymentAuthCallback: PayPalPaymentAuthCallback? = null
    //private lateinit var payPalPaymentAuthCallback: PayPalPaymentAuthCallback

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.PayPalButton, 0, 0).apply {
            try {
                currentStyle = PayPalButtonColor.fromId(getInt(R.styleable.PayPalButton_paymentButtonColor, 0))
            } finally {
                recycle()
            }
        }
        setupBackground()
        applyStyle()
       // payPalClient = PayPalClient(context, authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)

    }

    private fun setupBackground() {
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = resources.getDimension(R.dimen.pay_button_corner_radius)
        background = gradientDrawable
        minWidth = minDesiredWidth
    }

    private fun applyStyle() {
        gradientDrawable.setColor(currentStyle.fill)
        val strokeWidth = resources.getDimension(R.dimen.pay_button_border).toInt()
        gradientDrawable.setStroke(strokeWidth, currentStyle.border)
        logo = ContextCompat.getDrawable(context, currentStyle.logoId)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = logo ?: return
        val w = d.intrinsicWidth
        val h = d.intrinsicHeight
        val left = (width - w) / 2
        val top = (height - h) / 2 + logoOffset
        d.setBounds(left, top, left + w, top + h)
        d.draw(canvas)
    }

    /**
     * Sets the color of the PayPal button
     *
     * @property color Value representing the button color. Valid values are BLUE, BLACK, and WHITE
     */
    fun setButtonColor(color: PayPalButtonColor) {
        val style = color
        if (style == currentStyle) return
        currentStyle = style
        applyStyle()
    }

    fun updatePayPalRequest(request: PayPalRequest) {
        this.payPalRequest = request
    }

    fun setLaunchCallback(callback: PayPalButtonLaunchCallback) {
        this.launchCallback = callback
    }

    fun setPayPalPaymentAuthCallback(callback: PayPalPaymentAuthCallback) {
        this.payPalPaymentAuthCallback = callback
    }

    fun setPayPalClient(payPalClient: PayPalClient) {
        this.payPalClient = payPalClient
    }

    override fun performClick(): Boolean {
        super.performClick()
        payPalPaymentAuthCallback?.let {
            payPalClient?.createPaymentAuthRequest(
                context, payPalRequest!!, it)
        }
        return true
    }

    fun tokenize(paymentAuthResult: PayPalPaymentAuthResult.Success, payPalResult: PayPalTokenizeCallback) {
        payPalClient?.tokenize(paymentAuthResult, payPalResult)
    }

}
