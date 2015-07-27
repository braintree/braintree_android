package com.braintreepayments.testutils.ui;

import android.annotation.TargetApi;
import android.app.UiAutomation;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;

public class RotationHelper {

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    public static void rotateToLandscape() {
        rotateTo(UiAutomation.ROTATION_FREEZE_90);
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    public static void rotateToPortrait() {
        rotateTo(UiAutomation.ROTATION_FREEZE_0);
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    public static void rotateTo(int direction) {
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        automation.setRotation(UiAutomation.ROTATION_UNFREEZE);
        automation.setRotation(direction);

        SystemClock.sleep(1000);
    }
}
