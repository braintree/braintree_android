package com.braintreepayments.demo.test.utilities;

import androidx.test.uiautomator.Configurator;
import androidx.test.uiautomator.UiObjectNotFoundException;

import static com.braintreepayments.AutomatorAction.click;
import static com.braintreepayments.DeviceAutomator.onDevice;
import static com.braintreepayments.UiObjectMatcher.withContentDescription;
import static com.braintreepayments.UiObjectMatcher.withText;

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
