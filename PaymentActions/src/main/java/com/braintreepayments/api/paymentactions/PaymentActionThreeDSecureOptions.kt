package com.braintreepayments.api.paymentactions

/**
 * Options for how the SDK presents the 3D Secure challenge during [PaymentActionsClient.handleNextAction].
 *
 * Pass them when creating the [PaymentActionsClient]. They apply to every call the client makes, so if the
 * app is killed, create the client again with the same options.
 *
 * @property uiType the challenge interfaces the device supports. Defaults to
 *   [PaymentActionThreeDSecureUiType.BOTH].
 * @property renderTypes the challenge render types the device supports. With
 *   [PaymentActionThreeDSecureUiType.BOTH] or [PaymentActionThreeDSecureUiType.HTML], set every render type;
 *   with [PaymentActionThreeDSecureUiType.NATIVE], set every render type except
 *   [PaymentActionThreeDSecureRenderType.RENDER_HTML]. `null` keeps the default render types.
 * @property uiCustomization the look of the challenge screens. `null` keeps the default look.
 * @property requestorAppUrl the URL an out-of-band authentication app, such as a bank app, opens to return the
 *   customer to this app. `null` sends no URL. When set, an activity in this app must handle the URL: declare
 *   an intent filter with the `VIEW` action, the `DEFAULT` category and a `<data>` element that matches it, on
 *   an activity with `android:exported="true"` and no `android:permission`, so the other app can open it.
 *   Otherwise [PaymentActionsClient.handleNextAction] returns a [PaymentActionResult.Failure] with an
 *   [com.braintreepayments.api.core.InvalidArgumentException] before 3D Secure starts.
 */
class PaymentActionThreeDSecureOptions @JvmOverloads constructor(
    val uiType: PaymentActionThreeDSecureUiType = PaymentActionThreeDSecureUiType.BOTH,
    val renderTypes: List<PaymentActionThreeDSecureRenderType>? = null,
    val uiCustomization: PaymentActionThreeDSecureUiCustomization? = null,
    val requestorAppUrl: String? = null,
)

/**
 * The interfaces the device supports for displaying the 3D Secure challenge.
 */
enum class PaymentActionThreeDSecureUiType {
    NATIVE,
    HTML,
    BOTH,
}

/**
 * The render types the device supports for displaying the 3D Secure challenge.
 */
enum class PaymentActionThreeDSecureRenderType {
    OTP,
    SINGLE_SELECT,
    MULTI_SELECT,
    OOB,
    RENDER_HTML,
}

/**
 * UI customization for the 3D Secure challenge screens.
 *
 * @property buttonCustomizations customization for each type of button. Button types left out keep the
 *   default look.
 * @property labelCustomization customization for the labels.
 * @property textBoxCustomization customization for the text boxes.
 * @property toolbarCustomization customization for the toolbar.
 */
class PaymentActionThreeDSecureUiCustomization @JvmOverloads constructor(
    val buttonCustomizations: Map<PaymentActionThreeDSecureButtonType, PaymentActionThreeDSecureButtonCustomization> =
        emptyMap(),
    val labelCustomization: PaymentActionThreeDSecureLabelCustomization? = null,
    val textBoxCustomization: PaymentActionThreeDSecureTextBoxCustomization? = null,
    val toolbarCustomization: PaymentActionThreeDSecureToolbarCustomization? = null,
)

/**
 * The buttons on the 3D Secure challenge screens that can be customized.
 */
enum class PaymentActionThreeDSecureButtonType {
    BUTTON_TYPE_VERIFY,
    BUTTON_TYPE_CONTINUE,
    BUTTON_TYPE_NEXT,
    BUTTON_TYPE_CANCEL,
    BUTTON_TYPE_RESEND,
}

/**
 * Button customization for the 3D Secure challenge screens. Colors are hex codes, such as "#999999". A
 * `null` text or a `0` number keeps the default.
 *
 * @property textFontName the font of the button text.
 * @property textColor the color of the button text.
 * @property textFontSize the font size of the button text.
 * @property backgroundColor the background color of the button.
 * @property cornerRadius the radius of the button corners.
 */
class PaymentActionThreeDSecureButtonCustomization @JvmOverloads constructor(
    val textFontName: String? = null,
    val textColor: String? = null,
    val textFontSize: Int = 0,
    val backgroundColor: String? = null,
    val cornerRadius: Int = 0,
)

/**
 * Label customization for the 3D Secure challenge screens. Colors are hex codes, such as "#999999". A
 * `null` text or a `0` number keeps the default.
 *
 * @property textFontName the font of the label text.
 * @property textColor the color of the label text.
 * @property textFontSize the font size of the label text.
 * @property headingTextColor the color of the heading text.
 * @property headingTextFontName the font of the heading text.
 * @property headingTextFontSize the font size of the heading text.
 */
class PaymentActionThreeDSecureLabelCustomization @JvmOverloads constructor(
    val textFontName: String? = null,
    val textColor: String? = null,
    val textFontSize: Int = 0,
    val headingTextColor: String? = null,
    val headingTextFontName: String? = null,
    val headingTextFontSize: Int = 0,
)

/**
 * Text box customization for the 3D Secure challenge screens. Colors are hex codes, such as "#999999". A
 * `null` text or a `0` number keeps the default.
 *
 * @property textFontName the font of the text box text.
 * @property textColor the color of the text box text.
 * @property textFontSize the font size of the text box text.
 * @property borderWidth the width of the text box border.
 * @property borderColor the color of the text box border.
 * @property cornerRadius the radius of the text box corners.
 */
class PaymentActionThreeDSecureTextBoxCustomization @JvmOverloads constructor(
    val textFontName: String? = null,
    val textColor: String? = null,
    val textFontSize: Int = 0,
    val borderWidth: Int = 0,
    val borderColor: String? = null,
    val cornerRadius: Int = 0,
)

/**
 * Toolbar customization for the 3D Secure challenge screens. Colors are hex codes, such as "#999999". A
 * `null` text or a `0` number keeps the default.
 *
 * @property textFontName the font of the toolbar text.
 * @property textColor the color of the toolbar text.
 * @property textFontSize the font size of the toolbar text.
 * @property backgroundColor the background color of the toolbar.
 * @property headerText the header text.
 * @property buttonText the text of the toolbar button, such as "Cancel".
 */
class PaymentActionThreeDSecureToolbarCustomization @JvmOverloads constructor(
    val textFontName: String? = null,
    val textColor: String? = null,
    val textFontSize: Int = 0,
    val backgroundColor: String? = null,
    val headerText: String? = null,
    val buttonText: String? = null,
)
