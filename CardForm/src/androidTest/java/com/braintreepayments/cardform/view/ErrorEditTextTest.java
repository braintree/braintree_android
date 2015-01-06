package com.braintreepayments.cardform.view;

import android.graphics.drawable.Drawable;
import android.test.AndroidTestCase;
import android.widget.EditText;

public class ErrorEditTextTest extends AndroidTestCase {

    private ErrorEditText view;

    @Override
    protected void setUp() {
        view = new ErrorEditText(getContext());
    }

    public void testIsErrorIsTrueWhenErrorIsSet() {
        view.setError();
        assertTrue(view.isError());
    }

    public void testDefautlsToNoError() {
        assertFalse(view.isError());
    }

    public void testClearsErrorStateOnClearError() {
        view.setError();
        view.clearError();
        assertFalse(view.isError());
    }

    public void testUsesErrorSelectorWhenErrorIsSet() {
        Drawable startingDrawable = view.getBackground();
        view.setError();
        assertNotSame(startingDrawable, view.getBackground());
    }

    public void testUsesDefaultSelectorWhenErrorIsCleared() {
        view.setError();
        Drawable errorDrawable = view.getBackground();
        view.clearError();
        assertNotSame(errorDrawable, view.getBackground());
    }

    public void testClearsErrorOnTextChange() {
        view.setError();
        Drawable errorDrawable = view.getBackground();
        view.onTextChanged("4", 0, 0, 1);
        assertNotSame(errorDrawable, view.getBackground());
        assertFalse(view.isError());
    }
}
