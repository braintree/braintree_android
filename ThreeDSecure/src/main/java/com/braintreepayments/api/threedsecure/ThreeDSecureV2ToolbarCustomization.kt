package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization
import kotlinx.parcelize.Parcelize

/**
 * Toolbar customization options for 3D Secure 2 flows.
 *
 * @property textFontName Font type for the UI element.
 * @property textColor Color code in Hex format. For example, the color code can be “#999999”.
 * @property textFontSize Font size for the UI element.
 * @property backgroundColor Color code in Hex format. For example, the color code can be “#999999”.
 * @property headerText Text for the header.
 * @property buttonText Text for the button. For example, “Cancel”.
 */
@Parcelize
data class ThreeDSecureV2ToolbarCustomization(
    var textFontName: String? = null,
    var textColor: String? = null,
    var textFontSize: Int = 0,
    var backgroundColor: String? = null,
    var headerText: String? = null,
    var buttonText: String? = null
) : Parcelable {

    val cardinalToolbarCustomization: ToolbarCustomization
        get() {
            return ToolbarCustomization().also {
                textFontName?.let { textFontName -> it.textFontName = textFontName }
                textColor?.let { textColor -> it.textColor = textColor }
                if (textFontSize != 0) it.textFontSize = textFontSize
                backgroundColor?.let { backgroundColor -> it.backgroundColor = backgroundColor }
                headerText?.let { headerText -> it.headerText = headerText }
                buttonText?.let { buttonText -> it.buttonText = buttonText }
            }
        }
}
