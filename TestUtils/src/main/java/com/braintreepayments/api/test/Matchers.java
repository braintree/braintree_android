package com.braintreepayments.api.test;

import android.content.res.Resources;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class Matchers {

    /**
     * Returns a matcher that matches {@link View}s based on resource ids. Note: Android resource ids
     * are not guaranteed to be unique. You may have to pair this matcher with another one to
     * guarantee a unique view selection.
     *
     * @param resourceId the resource id to match
     */
    public static Matcher<View> withId(final int resourceId) {
        return new TypeSafeMatcher<View>() {
            private String resourceName = null;

            @Override
            public void describeTo(Description description) {
                description.appendText("with resource id: ");
                description.appendValue(resourceId);
                if (null != resourceName) {
                    description.appendText(" resource name: ");
                    description.appendText(resourceName);
                }
            }

            @Override
            public boolean matchesSafely(View view) {
                if (null == resourceName) {
                    try {
                        resourceName = view.getResources().getResourceEntryName(resourceId);
                    } catch (Resources.NotFoundException ignored) {
                        /* view could be from a context unaware of the resource id. */
                    }
                }
                return resourceId == view.getId();
            }
        };
    }
}
