package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import com.cardinalcommerce.shared.userinterfaces.LabelCustomization
import kotlinx.parcelize.Parcelize

/**
 * Label customization options for 3D Secure 2 flows.
 *
 * @property textFontName Font type for the UI element.
 * @property textColor Color code in Hex format. For example, the color code can be “#999999”.
 * @property textFontSize Font size for the UI element.
 * @property headingTextColor Color code in Hex format. For example, the color code can be
 * “#999999”.
 * @property headingTextFontName Font type for the heading label text.
 * @property headingTextFontSize Font size for the heading label text.
 */
@Parcelize
data class ThreeDSecureV2LabelCustomization(
    var textFontName: String? = null,
    var textColor: String? = null,
    var textFontSize: Int = 0,
    var headingTextColor: String? = null,
    var headingTextFontName: String? = null,
    var headingTextFontSize: Int = 0
) : Parcelable {

    val cardinalLabelCustomization: LabelCustomization
        get() {
            return LabelCustomization().also {
                textFontName?.let { textFontName -> it.textFontName = textFontName }
                textColor?.let { textColor -> it.textColor = textColor }
                if (textFontSize != 0) it.textFontSize = textFontSize
                headingTextColor?.let { headingTextColor -> it.headingTextColor = headingTextColor }
                headingTextFontName?.let { headingTextFontName ->
                    it.headingTextFontName = headingTextFontName
                }
                if (headingTextFontSize != 0) it.headingTextFontSize = headingTextFontSize
            }
        }
}
