package com.braintreepayments.testutils.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;

import com.google.android.apps.common.testing.ui.espresso.NoMatchingViewException;
import com.google.android.apps.common.testing.ui.espresso.ViewAssertion;
import com.google.common.base.Optional;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class Assertions {

    public static ViewAssertion theIconHintIs(Context context, int resId) {
        Drawable right = context.getResources().getDrawable(resId);
        return assertHintsAre(null, null, right, null);
    }

    public static ViewAssertion assertHintsAre(final Drawable left, final Drawable top,
            final Drawable right, final Drawable bottom) {
        return new ViewAssertion() {
            @Override
            public void check(Optional<View> viewOptional,
                    Optional<NoMatchingViewException> noMatchingViewExceptionOptional) {
                EditText editText = ((EditText) viewOptional.get());
                Drawable[] drawables = editText.getCompoundDrawables();
                assertBitmapsEqual(drawables[0], left);
                assertBitmapsEqual(drawables[1], top);
                assertBitmapsEqual(drawables[2], right);
                assertBitmapsEqual(drawables[3], bottom);
            }
        };
    }

    @TargetApi(HONEYCOMB_MR1)
    public static void assertBitmapsEqual(Drawable d1, Drawable d2) {
        if (d1 == null || d2 == null) {
            assertEquals(d1, d2);
        } else {
            Bitmap b1 = ((BitmapDrawable) d1).getBitmap();
            Bitmap b2 = ((BitmapDrawable) d2).getBitmap();
            if (SDK_INT >= HONEYCOMB_MR1) {
                assertTrue(b1.sameAs(b2));
            } else {
                assertEquals(b1.getHeight(), b2.getHeight());
                assertEquals(b1.getWidth(), b2.getWidth());
                for (int x = 0; x < b1.getWidth(); x++) {
                    for (int y = 0; y < b1.getHeight(); y++) {
                        assertEquals(b1.getPixel(x, y), b2.getPixel(x, y));
                    }
                }
            }
        }
    }

}
