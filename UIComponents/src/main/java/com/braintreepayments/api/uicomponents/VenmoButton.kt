package com.braintreepayments.api.uicomponents

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.braintreepayments.api.venmo.VenmoClient
import com.braintreepayments.api.venmo.VenmoLauncher
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback

/**
 * A customizable Venmo branded button to initiate the Venmo flow.
 *
 * This button provides a pre-styled Venmo button with configurable colors and handles
 * the complete Venmo payment flow.
 */
class VenmoButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var currentStyle: VenmoButtonColor = VenmoButtonColor.BLUE
    private val gradientDrawable = GradientDrawable()
    private var logo: Drawable? = null

    private val desiredWidth = resources.getDimension(R.dimen.pay_button_width).toInt()
    private val desiredHeight = resources.getDimension(R.dimen.pay_button_height).toInt()
    private val minDesiredWidth = resources.getDimension(R.dimen.pay_button_min_width).toInt()

    lateinit var venmoClient: VenmoClient
    private var venmoLauncher: VenmoLauncher

    private var venmoRequest: VenmoRequest? = null
    var venmoLaunchCallback: VenmoLaunchCallback? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.VenmoButton, 0, 0).apply {
            try {
                currentStyle =
                    VenmoButtonColor.fromId(getInt(R.styleable.VenmoButton_paymentButtonColor, 0))
            } finally {
                recycle()
            }
        }
        setupBackground()
        applyStyle()

        venmoLauncher = VenmoLauncher()
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
        logo?.let { d ->
            val w = d.intrinsicWidth
            val h = d.intrinsicHeight
            val left = (width - w) / 2
            val top = (height - h) / 2
            d.setBounds(left, top, left + w, top + h)
            d.draw(canvas)
        }
    }

    /**
     * Sets the color of the Venmo button
     *
     * @property color Value representing the button color. Valid values are BLUE, BLACK, and WHITE
     */
    fun setButtonColor(color: VenmoButtonColor) {
        val style = color
        if (style == currentStyle) return
        currentStyle = style
        applyStyle()
    }

    /**
     * Initializes the Venmo button with the required parameters needed to start the payment flow.
     *
     * Call this before setting a Venmo request or handling any payment flows.
     *
     * @param authorization             a Tokenization Key or Client Token used to authenticate
     * @param appLinkReturnUrl          a [Uri] containing the Android App Link website associated with
     * merchant's application to be used to return to merchant's app from the Venmo payment flows.
     * @param deepLinkFallbackUrlScheme a return url scheme that will be used as a deep link fallback when returning to
     * merchant's app via App Link is not available (buyer unchecks the "Open supported links" setting).
     */

    fun initialize(
        authorization: String,
        appLinkReturnUrl: Uri,
        deepLinkFallbackUrlScheme: String? = null
    ) {
        venmoClient = VenmoClient(
            context = context,
            authorization = authorization,
            appLinkReturnUrl = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
        )
    }

    /**
     * Sets the Venmo request configuration and wires up the click handler.
     *
     * When the button is clicked, it:
     * 1. Creates a payment auth request using the provided Venmo request
     * 2. Launches the Venmo authentication flow
     * 3. Notifies the [venmoLaunchCallback] with the result
     *
     * @param venmoRequest the Venmo request containing payment details and configuration
     */
    fun setVenmoRequest(venmoRequest: VenmoRequest) {
        setOnClickListener {
            venmoClient.createPaymentAuthRequest(
                context = context,
                request = venmoRequest
            ) { venmoPaymentAuthRequest: VenmoPaymentAuthRequest ->
                when (venmoPaymentAuthRequest) {
                    is VenmoPaymentAuthRequest.ReadyToLaunch -> {
                        getActivity()?.let { activity ->
                            val venmoPendingRequest = venmoLauncher.launch(
                                activity = activity,
                                paymentAuthRequest = venmoPaymentAuthRequest
                            )
                            when (venmoPendingRequest) {
                                is VenmoPendingRequest.Started -> {
                                    venmoLaunchCallback?.onVenmoPaymentAuthRequest(
                                        VenmoPendingRequest.Started(venmoPendingRequest.pendingRequestString)
                                    )
                                }
                                is VenmoPendingRequest.Failure -> {
                                    venmoLaunchCallback?.onVenmoPaymentAuthRequest(
                                        VenmoPendingRequest.Failure(venmoPendingRequest.error)
                                    )
                                }
                            }
                        } ?: run {
                            venmoLaunchCallback?.onVenmoPaymentAuthRequest(
                                VenmoPendingRequest.Failure(NullPointerException("Activity is null"))
                            )
                        }
                    }
                    is VenmoPaymentAuthRequest.Failure -> {
                        venmoLaunchCallback?.onVenmoPaymentAuthRequest(
                            VenmoPendingRequest.Failure(venmoPaymentAuthRequest.error)
                        )
                    }
                }
            }
        }
    }

    /**
     * Handles the return from the Venmo authentication flow and tokenizes the result.
     *
     * Call this method on [onResume] after the user completes the Venmo authentication and is returned to app.
     *
     * If the Activity used to launch the Venmo flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of the Activity.
     *
     * This method:
     * 1. Extracts the authentication result from the intent
     * 2. Tokenizes the successful authentication
     * 3. Returns the final Venmo result via the callback
     *
     * @param pendingRequest the [VenmoPendingRequest.Started] stored after successfully
     * invoking [VenmoLauncher.launch]
     * @param intent         the intent to return to merchant's application containing a deep link result
     * from the Venmo browser flow
     * @param callback       callback to receive the final Venmo result (success, cancel, or failure)
     */

    fun handleReturnToApp(
        pendingRequest: VenmoPendingRequest.Started,
        intent: Intent,
        callback: VenmoTokenizeCallback
    ) {
        val paymentAuthResult = venmoLauncher.handleReturnToApp(
            pendingRequest = pendingRequest,
            intent = intent
        )
        when (paymentAuthResult) {
            is VenmoPaymentAuthResult.Success -> {
                venmoClient.tokenize(paymentAuthResult) { venmoResult ->
                    callback.onVenmoResult(venmoResult)
                }
            }
            is VenmoPaymentAuthResult.Failure -> {
                callback.onVenmoResult(VenmoResult.Failure(paymentAuthResult.error))
            }
            is VenmoPaymentAuthResult.NoResult -> {
                callback.onVenmoResult(VenmoResult.Cancel)
            }
        }
    }
}
