package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization
import kotlinx.parcelize.Parcelize

/**
 * Text box customization options for 3D Secure 2 flows.
 *
 * @property textFontName Font type for the UI element.
 * @property textColor Color code in Hex format. For example, the color code can be “#999999”.
 * @property textFontSize Font size for the UI element.
 * @property borderWidth Width (integer value) of the text box border.
 * @property borderColor Color code in Hex format. For example, the color code can be “#999999”.
 * @property cornerRadius Radius (integer value) for the text box corners.
 */
@Parcelize
data class ThreeDSecureV2TextBoxCustomization(
    var textFontName: String? = null,
    var textColor: String? = null,
    var textFontSize: Int = 0,
    var borderWidth: Int = 0,
    var borderColor: String? = null,
    var cornerRadius: Int = 0
) : Parcelable {

    val cardinalTextBoxCustomization: TextBoxCustomization
        get() {
            return TextBoxCustomization().also {
                textFontName?.let { textFontName -> it.textFontName = textFontName }
                textColor?.let { textColor -> it.textColor = textColor }
                if (textFontSize != 0) it.textFontSize = textFontSize
                if (borderWidth != 0) it.borderWidth = borderWidth
                borderColor?.let { borderColor -> it.borderColor = borderColor }
                if (cornerRadius != 0) it.cornerRadius = cornerRadius
            }
        }
}
