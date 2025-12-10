package com.braintreepayments.api.uicomponents

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.braintreepayments.api.core.AnalyticsClient
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
@Suppress("TooManyFunctions")
class PayPalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var currentStyle: PayPalButtonColor = PayPalButtonColor.BLUE
    private val gradientDrawable = GradientDrawable()
    private var logo: Drawable? = null
    private var spinner: ProgressBar? = null

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
        val analyticsClient = AnalyticsClient.lazyInstance.value
        analyticsClient.sendEvent(UIComponentsAnalytics.PAYPAL_BUTTON_PRESENTED)
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

            setButtonClicked()
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
            val analyticsClient = AnalyticsClient.lazyInstance.value
            analyticsClient.sendEvent(UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED)
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
        setButtonReEnabled()
    }

    private fun setButtonClicked() {
        this.isEnabled = false
        logo = ContextCompat.getDrawable(context, currentStyle.spinnerId)
        (logo as? android.graphics.drawable.Animatable)?.start()
        invalidate()
    }

    private fun setButtonReEnabled() {
        this.isEnabled = true
        (logo as? android.graphics.drawable.Animatable)?.stop()
        logo = ContextCompat.getDrawable(context, currentStyle.logoId)
        invalidate()
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
