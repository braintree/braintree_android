package com.braintreepayments.api.ui;

import android.annotation.TargetApi;
import android.app.UiAutomation;
import android.os.Build.VERSION_CODES;
import android.test.ActivityInstrumentationTestCase2;

public class RotationHelper {

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    public static void rotateToLandscape(ActivityInstrumentationTestCase2<?> testCase) {
        rotateTo(UiAutomation.ROTATION_FREEZE_90, testCase);
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    public static void rotateToPortrait(ActivityInstrumentationTestCase2<?> testCase) {
        rotateTo(UiAutomation.ROTATION_FREEZE_0, testCase);
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    public static void rotateTo(int direction, ActivityInstrumentationTestCase2<?> testCase) {
        UiAutomation automation = testCase.getInstrumentation().getUiAutomation();
        automation.setRotation(UiAutomation.ROTATION_UNFREEZE);
        automation.setRotation(direction);
    }

}
