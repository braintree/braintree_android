package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.cardinalcommerce.shared.userinterfaces.ButtonCustomization
import kotlinx.parcelize.Parcelize

/**
 * Button customization options for 3D Secure 2 flows.
 *
 * @property textFontName Font type for the UI element.
 * @property textColor Color code in Hex format. For example, the color code can be “#999999”.
 * @property textFontSize Font size for the UI element.
 * @property backgroundColor @param backgroundColor Color code in Hex format. For example, the color
 * code can be “#999999”.
 * @property cornerRadius Radius (integer value) for the button corners.
 */
@Parcelize
data class ThreeDSecureV2ButtonCustomization @JvmOverloads constructor(
    var textFontName: String? = null,
    var textColor: String? = null,
    var textFontSize: Int = 0,
    var backgroundColor: String? = null,
    var cornerRadius: Int = 0
) : Parcelable {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val cardinalButtonCustomization: ButtonCustomization
        get() {
            return ButtonCustomization().also {
                textFontName?.let { textFontName -> it.textFontName = textFontName }
                textColor?.let { textColor -> it.textColor = textColor }
                if (textFontSize != 0) it.textFontSize = textFontSize
                backgroundColor?.let { backgroundColor -> it.backgroundColor = backgroundColor }
                if (cornerRadius != 0) it.cornerRadius = cornerRadius
            }
        }
}
