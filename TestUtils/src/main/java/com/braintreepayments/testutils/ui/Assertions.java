package com.braintreepayments.testutils.ui;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.view.View;
import android.widget.EditText;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class Assertions {

    /**
     * Asserts that the right drawable of an {@link android.widget.EditText} matches a given
     * drawable resource.
     *
     * @param context
     * @param resourceId The id of the resource to check.
     * @return {@link android.support.test.espresso.ViewAssertion} for chaining
     */
    public static ViewAssertion theIconHintIs(Context context, int resourceId) {
        Drawable right = context.getResources().getDrawable(resourceId);
        return assertHintsAre(null, null, right, null);
    }

    /**
     * Asserts that the drawables of an {@link android.widget.EditText} match the given
     * drawables.
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     * @return {@link android.support.test.espresso.ViewAssertion} for chaining
     */
    public static ViewAssertion assertHintsAre(final Drawable left, final Drawable top,
            final Drawable right, final Drawable bottom) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException e) {
                assert (view) != null;
                Drawable[] drawables = ((EditText) view).getCompoundDrawables();
                assertBitmapsEqual(drawables[0], left);
                assertBitmapsEqual(drawables[1], top);
                assertBitmapsEqual(drawables[2], right);
                assertBitmapsEqual(drawables[3], bottom);
            }
        };
    }

    public static void assertBitmapsEqual(Drawable d1, Drawable d2) {
        if (d1 == null || d2 == null) {
            assertEquals(d1, d2);
        } else {
            assertTrue(((BitmapDrawable) d1).getBitmap().sameAs(((BitmapDrawable) d2).getBitmap()));
        }
    }
}
