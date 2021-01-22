package com.braintreepayments.api;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV1UiCustomizationUnitTest {

    @Test
    public void writeToParcel() {
        ThreeDSecureV1UiCustomization customization = new ThreeDSecureV1UiCustomization()
                .redirectButtonText("some-button-text")
                .redirectDescription("some-label-text");

        Parcel parcel = Parcel.obtain();
        customization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureV1UiCustomization actual = ThreeDSecureV1UiCustomization.CREATOR.createFromParcel(parcel);

        assertEquals("some-button-text", actual.getRedirectButtonText());
        assertEquals("some-label-text", actual.getRedirectDescription());
    }
}