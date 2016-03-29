package com.braintreepayments.testutils.ui;

import android.support.test.espresso.PerformException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.CloseKeyboardAction;
import android.view.View;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.internal.util.Checks.checkNotNull;

/* http://stackoverflow.com/a/22563297 */
public class ViewHelper {

    public static final int HALF_SECOND = 500;
    public static final int ONE_SECOND = 1000;
    public static final int TWO_SECONDS = 2000;
    public static final int THREE_SECONDS = 3000;
    public static final int FOUR_SECONDS = 4000;
    public static final int FIVE_SECONDS = 5000;
    public static final int TEN_SECONDS = 10000;
    public static final int FIFTEEN_SECONDS = 15000;

    public static boolean sWaitingForView;

    public static ViewInteraction waitForView(final Matcher<View> viewFinder, Matcher<View> viewCondition, final long millis) {
        checkNotNull(viewFinder);

        final long endTime = System.currentTimeMillis() + millis;
        ViewHelper.sWaitingForView = true;

        do {
            try {
                ViewInteraction interaction = onView(viewFinder);
                interaction.check(matches(viewCondition));

                ViewHelper.sWaitingForView = false;
                return interaction;
            } catch (Exception ignored) {
            } catch (Error ignored) {
            }
        } while (System.currentTimeMillis() < endTime);

        ViewHelper.sWaitingForView = false;

        ViewInteraction interaction = onView(viewFinder);
        interaction.check(matches(viewCondition));

        return interaction;
    }

    public static ViewInteraction waitForView(final Matcher<View> viewMatcher, final long millis) {
        return waitForView(viewMatcher, isDisplayed(), millis);
    }

    public static ViewInteraction waitForView(final Matcher<View> viewMatcher, Matcher<View> viewCondition) {
        return waitForView(viewMatcher, viewCondition, TEN_SECONDS);
    }

    public static ViewInteraction waitForView(final Matcher<View> viewMatcher) {
        return waitForView(viewMatcher, TEN_SECONDS);
    }

    /**
     * Closes the soft keyboard and sleeps to ensure the keyboard is fully closed.
     *
     * @return {@link android.support.test.espresso.ViewAction} instance for chaining
     */
    public static ViewAction closeSoftKeyboard() {
        return new ViewAction() {
            /**
             * The delay time to allow the soft keyboard to dismiss.
             */
            private static final long KEYBOARD_DISMISSAL_DELAY_MILLIS = 500L;

            /**
             * The real {@link CloseKeyboardAction} instance.
             */
            private final ViewAction mCloseSoftKeyboard = new CloseKeyboardAction();

            @Override
            public Matcher<View> getConstraints() {
                return mCloseSoftKeyboard.getConstraints();
            }

            @Override
            public String getDescription() {
                return mCloseSoftKeyboard.getDescription();
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                try {
                    mCloseSoftKeyboard.perform(uiController, view);
                } catch (PerformException ignored) {}
                uiController.loopMainThreadForAtLeast(KEYBOARD_DISMISSAL_DELAY_MILLIS);
            }
        };
    }
}
