package com.braintreepayments.api.utils;

import android.view.View;
import android.widget.EditText;

import com.google.android.apps.common.testing.ui.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.is;

/* http://stackoverflow.com/a/20682630 */
public class Matchers {

    public static Matcher<View> withHint(String hintText) {
        // use preconditions to fail fast when a test is creating an invalid matcher.
        checkArgument(!(hintText.equals(null)));
        return withHint(is(hintText));
    }

    public static Matcher<View> withHint(final Matcher<String> matcherText) {
        // use preconditions to fail fast when a test is creating an invalid matcher.
        checkNotNull(matcherText);
        return new BoundedMatcher<View, EditText>(EditText.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with hint: " + matcherText);
            }

            @Override
            protected boolean matchesSafely(EditText editTextField) {
                return matcherText.matches(editTextField.getHint().toString());
            }
        };
    }
}
