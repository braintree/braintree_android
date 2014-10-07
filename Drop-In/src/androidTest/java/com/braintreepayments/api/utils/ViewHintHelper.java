package com.braintreepayments.api.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;

import com.google.android.apps.common.testing.ui.espresso.NoMatchingViewException;
import com.google.android.apps.common.testing.ui.espresso.ViewAssertion;
import com.google.common.base.Optional;

import static com.braintreepayments.api.BraintreeTestUtils.assertBitmapsEqual;

public class ViewHintHelper {

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
}
