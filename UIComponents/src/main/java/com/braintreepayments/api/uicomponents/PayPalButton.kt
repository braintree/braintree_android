package com.braintreepayments.api.uicomponents

import android.content.Context
import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback

/**
 * A customizable PayPal branded button to initiate the PayPal flow.
 *
 * This button provides a pre-styled PayPal button with configurable colors and handles
 * the complete PayPal payment flow.
 */
@Suppress("MagicNumber")
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

    /**
     * The PayPal client used to create payment auth requests and tokenize results.
     * Must be initialized via [initialize] before use.
     */
    private lateinit var payPalClient: PayPalClient
    private val payPalLauncher: PayPalLauncher

    /**
     * Callback invoked when the PayPal payment authentication request is launched.
     *
     * This callback is used to receive notifications about the launch status, such as
     * [PayPalPendingRequest.Started] or [PayPalPendingRequest.Failure]. The pending request
     * returned on success should be stored and passed to [handleReturnToApp] to complete the payment flow.
     */
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

    /**
     * Initializes the PayPal button with the required parameters needed to start the payment flow.
     *
     * Call this before setting a PayPal request or handling any payment flows.
     *
     * @param authorization             a Tokenization Key or Client Token used to authenticate
     * @param appLinkReturnUrl          a [Uri] containing the Android App Link website associated with
     * merchant's application to be used to return to merchant's app from the PayPal payment flows.
     * @param deepLinkFallbackUrlScheme a return url scheme that will be used as a deep link fallback when returning to
     * merchant's app via App Link is not available (buyer unchecks the "Open supported links" setting).
     */
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

    /**
     * Sets the PayPal request configuration and wires up the click handler.
     *
     * When the button is clicked, it creates a payment auth request and either
     * launches the PayPal flow or notifies the callback of any failures.
     *
     * @param payPalRequest the PayPal request containing payment details and configuration
     */
    fun setPayPalRequest(payPalRequest: PayPalRequest) {
        setOnClickListener {
            if (payPalLaunchCallback == null) {
                throw(NullPointerException("PayPalLaunchCallback must be initialized first"))
            }
            payPalClient.createPaymentAuthRequest(
                context = context,
                payPalRequest = payPalRequest
            ) { paymentAuthRequest: PayPalPaymentAuthRequest ->
                when (paymentAuthRequest) {
                    is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                        completePayPalFlow(paymentAuthRequest)
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

    /**
     * Completes the PayPal flow by launching the authentication and handling the result.
     *
     * Launches the PayPal authentication flow and notifies the [payPalLaunchCallback]
     * with the launch result (started or failure).
     *
     * @param paymentAuthRequest the ready-to-launch payment authentication request
     */
    private fun completePayPalFlow(paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch) {
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
            payPalLaunchCallback?.onPayPalPaymentAuthRequest(
                PayPalPendingRequest.Failure(NullPointerException("Activity is null")))
        }
    }

    /**
     * Handles the return from the PayPal authentication flow and tokenizes the result.
     *
     * Call this method on [onResume] after the user completes the PayPal authentication and is returned to app.
     *
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of the Activity.
     *
     * This method:
     * 1. Extracts the authentication result from the intent
     * 2. Tokenizes the successful authentication
     * 3. Returns the final PayPal result via the callback
     *
     * @param pendingRequest the [PayPalPendingRequest.Started] stored after successfully
     * invoking [PayPalLauncher.launch]
     * @param intent         the intent to return to merchant's application containing a deep link result
     * from the PayPal browser flow
     * @param callback       callback to receive the final PayPal result (success, cancel, or failure)
     */
    fun handleReturnToApp(
        pendingRequest: PayPalPendingRequest.Started,
        intent: Intent,
        callback: PayPalTokenizeCallback
    ) {
        val paymentAuthResult = payPalLauncher.handleReturnToApp(
            pendingRequest = pendingRequest,
            intent = intent,
        )

        when (paymentAuthResult) {
            is PayPalPaymentAuthResult.Success -> {
                payPalClient.tokenize(paymentAuthResult) { payPalResult ->
                    callback.onPayPalResult(payPalResult)
                }
            }
            is PayPalPaymentAuthResult.NoResult -> {
                callback.onPayPalResult(PayPalResult.Cancel)
            }
            is PayPalPaymentAuthResult.Failure -> {
                callback.onPayPalResult(PayPalResult.Failure(paymentAuthResult.error))
            }
        }
    }

    private fun setupBackground() {
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = resources.getDimension(R.dimen.pay_button_corner_radius)
        background = gradientDrawable
        minWidth = minDesiredWidth
    }

    private fun rippleEffect() {
        val rippleEffectWhite = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.black)),
            null,
            ShapeDrawable(RectShape()).apply { paint.color = Color.WHITE })
        val rippleEffectBlack = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white)),
            null,
            ShapeDrawable(RectShape()).apply { paint.color = Color.BLACK })
        val rippleEffectBlue = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white)),
            null,
            ShapeDrawable(RectShape()).apply { paint.color = PayPalButtonColor.BLUE.fill })
        val rippleDrawable = when (currentStyle) {
            PayPalButtonColor.BLUE -> rippleEffectBlue
            PayPalButtonColor.WHITE -> rippleEffectWhite
            PayPalButtonColor.BLACK -> rippleEffectBlack
        }
        isClickable = true
        isFocusable = true

        foreground = rippleDrawable
    }

    private fun applyStyle() {
        updateButtonAppearance()
        rippleEffect()
        gradientDrawable.setColor(currentStyle.fill)
        val strokeWidth = resources.getDimension(R.dimen.pay_button_border).toInt()
        gradientDrawable.setStroke(strokeWidth, currentStyle.border)
        logo = ContextCompat.getDrawable(context, currentStyle.logoId)
        invalidate()
    }

    private fun updateButtonAppearance() {
        val strokeWidth = resources.getDimension(R.dimen.pay_button_border).toInt()

        when {
            !isEnabled -> {
                // Disabled state: reduced opacity
                gradientDrawable.setColor(applyAlpha(currentStyle.fill, 0.4f))
                gradientDrawable.setStroke(strokeWidth, applyAlpha(currentStyle.border, 0.4f))
                alpha = 0.5f
            }
            isPressed -> {
                // Pressed state: darker fill
                gradientDrawable.setColor(darkenColor(currentStyle.fill, 0.8f))
                gradientDrawable.setStroke(strokeWidth, currentStyle.border)
                alpha = 1.0f
            }
            isHovered || isFocused -> {
                // Hover/Focus state: slightly lighter fill
                gradientDrawable.setColor(lightenColor(currentStyle.fill, 1.1f))
                gradientDrawable.setStroke(strokeWidth, currentStyle.border)
                alpha = 1.0f
            }
            else -> {
                // Default state
                gradientDrawable.setColor(currentStyle.fill)
                gradientDrawable.setStroke(strokeWidth, currentStyle.border)
                alpha = 1.0f
            }
        }
    }

    private fun applyAlpha(color: Int, alphaFactor: Float): Int {
        val alpha = ((color shr 24 and 0xFF) * alphaFactor).toInt()
        return (alpha shl 24) or (color and 0x00FFFFFF)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xFF) * factor).toInt()
        val g = ((color shr 8 and 0xFF) * factor).toInt()
        val b = ((color and 0xFF) * factor).toInt()
        val a = (color shr 24 and 0xFF)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = minOf(255, ((color shr 16 and 0xFF) * factor).toInt())
        val g = minOf(255, ((color shr 8 and 0xFF) * factor).toInt())
        val b = minOf(255, ((color and 0xFF) * factor).toInt())
        val a = (color shr 24 and 0xFF)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
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

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        updateButtonAppearance()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        updateButtonAppearance()
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
