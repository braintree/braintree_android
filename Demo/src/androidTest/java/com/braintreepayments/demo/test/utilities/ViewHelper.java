package com.braintreepayments.demo.test.utilities;

import android.support.test.espresso.ViewInteraction;
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

    public static ViewInteraction waitForView(final Matcher<View> viewFinder, Matcher<View> viewCondition, final long millis) {
        checkNotNull(viewFinder);

        final long endTime = System.currentTimeMillis() + millis;
        do {
            try {
                ViewInteraction interaction = onView(viewFinder);
                interaction.check(matches(viewCondition));

                return interaction;
            } catch (Exception | Error ignored) {}
        } while (System.currentTimeMillis() < endTime);

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
}
