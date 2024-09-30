package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.cardinalcommerce.shared.models.enums.ButtonType
import com.cardinalcommerce.shared.userinterfaces.UiCustomization
import kotlinx.parcelize.Parcelize

/**
 * UI customization options for 3D Secure 2 flows.
 *
 * @property buttonCustomization button customization options for 3D Secure 2 flows
 * @property buttonType type of 3D Secure button
 * @property labelCustomization label customization options for 3D Secure 2 flows
 * @property textBoxCustomization text box customization options for 3D Secure 2 flows
 * @property toolbarCustomization toolbar customization options for 3D Secure 2 flows
 */
@Parcelize
data class ThreeDSecureV2UiCustomization @JvmOverloads constructor(
    var buttonCustomization: ThreeDSecureV2ButtonCustomization? = null,
    var buttonType: ThreeDSecureV2ButtonType? = null,
    var labelCustomization: ThreeDSecureV2LabelCustomization? = null,
    var textBoxCustomization: ThreeDSecureV2TextBoxCustomization? = null,
    var toolbarCustomization: ThreeDSecureV2ToolbarCustomization? = null,
) : Parcelable {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val cardinalUiCustomization: UiCustomization
        get() {
            return UiCustomization().also {
                if (buttonCustomization != null && buttonType != null) {
                    it.setButtonCustomization(
                        buttonCustomization?.cardinalButtonCustomization,
                        getCardinalButtonType(buttonType)
                    )
                }
                labelCustomization?.let { labelCustomization ->
                    it.labelCustomization = labelCustomization.cardinalLabelCustomization
                }
                textBoxCustomization?.let { textBoxCustomization ->
                    it.textBoxCustomization = textBoxCustomization.cardinalTextBoxCustomization
                }
                toolbarCustomization?.let { toolbarCustomization ->
                    it.toolbarCustomization = toolbarCustomization.cardinalToolbarCustomization
                }
            }
        }

    private fun getCardinalButtonType(buttonType: ThreeDSecureV2ButtonType?): ButtonType? {
        return when (buttonType) {
            ThreeDSecureV2ButtonType.BUTTON_TYPE_VERIFY -> ButtonType.VERIFY
            ThreeDSecureV2ButtonType.BUTTON_TYPE_CONTINUE -> ButtonType.CONTINUE
            ThreeDSecureV2ButtonType.BUTTON_TYPE_NEXT -> ButtonType.NEXT
            ThreeDSecureV2ButtonType.BUTTON_TYPE_CANCEL -> ButtonType.CANCEL
            ThreeDSecureV2ButtonType.BUTTON_TYPE_RESEND -> ButtonType.RESEND
            else -> null
        }
    }
}
