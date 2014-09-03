package com.braintreepayments.api.ui;

import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.EditText;

import com.google.android.apps.common.testing.ui.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.is;

/* http://stackoverflow.com/a/20682630 */
public class Matchers {

    /**
     * Returns a matcher that matches {@link View}s based on resource ids. Note: Android resource ids
     * are not guaranteed to be unique. You may have to pair this matcher with another one to
     * guarantee a unique view selection.
     *
     * @param id the resource id to match
     */
    public static Matcher<View> withId(final int id) {
        final Matcher<Integer> integerMatcher = is(id);
        checkNotNull(integerMatcher);
        return new TypeSafeMatcher<View>() {
            private View mView;

            @Override
            public void describeTo(Description description) {
                description.appendText(mView.getResources().getResourceName(id));
            }

            @Override
            public boolean matchesSafely(View view) {
                mView = view;
                return integerMatcher.matches(view.getId());
            }
        };
    }

    public static Matcher<View> withHint(String hintText) {
        // use preconditions to fail fast when a test is creating an invalid matcher.
        checkNotNull(hintText);

        final Matcher<String> matcher = is(hintText);
        return new BoundedMatcher<View, EditText>(EditText.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with hint: " + matcher);
            }

            @Override
            protected boolean matchesSafely(EditText editTextField) {
                return matcher.matches(editTextField.getHint().toString());
            }
        };
    }

    @SuppressLint("NewApi")
    public static Matcher<View> hasBackgroundColor(final int color) {
        return new BoundedMatcher<View, View>(View.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with color: " + color);
            }

            @Override
            protected boolean matchesSafely(View view) {
                return is(view.getContext().getResources().getColor(color))
                        .matches(((ColorDrawable) view.getBackground()).getColor());
            }
        };
    }
}
