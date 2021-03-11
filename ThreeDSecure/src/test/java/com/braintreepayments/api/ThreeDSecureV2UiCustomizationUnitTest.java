package com.braintreepayments.api;

import com.cardinalcommerce.shared.models.enums.ButtonType;
import com.cardinalcommerce.shared.userinterfaces.UiCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2UiCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2UiCustomization sut = new ThreeDSecureV2UiCustomization()
                .labelCustomization(new ThreeDSecureV2LabelCustomization())
                .buttonCustomization(new ThreeDSecureV2ButtonCustomization(), ThreeDSecureV2UiCustomization.BUTTON_TYPE_NEXT)
                .textBoxCustomization(new ThreeDSecureV2TextBoxCustomization())
                .toolbarCustomization(new ThreeDSecureV2ToolbarCustomization());

        UiCustomization cardinalUiCustomization = sut.getCardinalUiCustomization();
        assertNotNull(cardinalUiCustomization.getLabelCustomization());
        assertNotNull(cardinalUiCustomization.getButtonCustomization(ButtonType.NEXT));
        assertNotNull(cardinalUiCustomization.getTextBoxCustomization());
        assertNotNull(cardinalUiCustomization.getToolbarCustomization());
    }
}
