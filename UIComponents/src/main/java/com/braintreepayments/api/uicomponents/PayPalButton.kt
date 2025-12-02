package com.braintreepayments.api.uicomponents

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest

/**
 * A customizable PayPal branded button to initiate the PayPal flow
 */
class PayPalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var currentStyle: PayPalButtonColor = PayPalButtonColor.BLUE
    private val gradientDrawable = GradientDrawable()
    private var logo: Drawable? = null

    private val logoOffset = resources.getDimension(R.dimen.pp_logo_offset).toInt()
    private val desiredWidth = resources.getDimension(R.dimen.pay_button_width).toInt()
    private val desiredHeight = resources.getDimension(R.dimen.pay_button_height).toInt()
    private val minDesiredWidth = resources.getDimension(R.dimen.pay_button_min_width).toInt()

    private var payPalRequest: PayPalRequest? = null

    lateinit var payPalClient: PayPalClient
    private val payPalLauncher: PayPalLauncher

    var payPalLaunchCallback: PayPalLaunchCallback? = null

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

        payPalLauncher = PayPalLauncher()
    }

    fun initialize(
        authorization: String,
        appLinkReturnUrl: Uri,
        deepLinkFallbackUrlScheme: String? = null,
    ) {
        payPalClient = PayPalClient(
            context = context,
            authorization = authorization,
            appLinkReturnUrl = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
        )
    }

    fun setPayPalRequest(payPalRequest: PayPalRequest) {
        setOnClickListener {
            payPalClient.createPaymentAuthRequest(
                context = context,
                payPalRequest = payPalRequest
            ) { paymentAuthRequest: PayPalPaymentAuthRequest ->
                when (paymentAuthRequest) {
                    is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                        getActivity()?.let { activity ->
                            val payPalPendingRequest = payPalLauncher.launch(
                                activity = activity,
                                paymentAuthRequest = paymentAuthRequest
                            )
                            when (payPalPendingRequest) {
                                is PayPalPendingRequest.Started -> {
                                    payPalLaunchCallback?.onPayPalPaymentAuthRequest(
                                        PayPalPendingRequest.Started(payPalPendingRequest.pendingRequestString)
                                    )
                                }

                                is PayPalPendingRequest.Failure -> {
                                    payPalLaunchCallback?.onPayPalPaymentAuthRequest(
                                        PayPalPendingRequest.Failure(payPalPendingRequest.error)
                                    )
                                }
                            }

                        } ?: run {
                            // invoke failure callback
                        }
                    }

                    is PayPalPaymentAuthRequest.Failure -> {
                        payPalLaunchCallback?.onPayPalPaymentAuthRequest(
                            PayPalPendingRequest.Failure(paymentAuthRequest.error)
                        )
                    }
                }
            }
        }
    }

    fun handleReturnToApp(
        pendingRequest: PayPalPendingRequest.Started,
        intent: Intent
    ) {
        val paymentAuthResult = payPalLauncher.handleReturnToApp(
            pendingRequest = pendingRequest,
            intent = intent,
        )

        when (paymentAuthResult) {
            is PayPalPaymentAuthResult.Success -> {
//                payPalClient.tokenize() {
//
//                }
            }
            is PayPalPaymentAuthResult.NoResult -> {
                // notify the merchant of no result
            }
            is PayPalPaymentAuthResult.Failure -> {
                // notify the merchant of failure
            }
        }
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
}
