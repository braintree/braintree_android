package com.braintreepayments.demo.test.utilities;

import android.support.test.uiautomator.Configurator;
import android.support.test.uiautomator.UiObjectNotFoundException;

import static com.lukekorth.deviceautomator.AutomatorAction.click;
import static com.lukekorth.deviceautomator.DeviceAutomator.onDevice;
import static com.lukekorth.deviceautomator.UiObjectMatcher.withText;

public class UiTestActions {

    public static void clickWebViewText(String text) {
        clickWebViewText(text,1000);
    }

    public static void clickWebViewText(String text, Integer waitTimeout) {
        Configurator configurator = Configurator.getInstance();
        long originalTimeout = configurator.getWaitForSelectorTimeout();

        configurator.setWaitForSelectorTimeout(waitTimeout);

        try {
            onDevice(withText(text)).perform(click());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof UiObjectNotFoundException) {
                onDevice(withContentDescription(text)).perform(click());
            } else {
                throw e;
            }
        } finally {
            configurator.setWaitForSelectorTimeout(originalTimeout);
        }
    }
}
