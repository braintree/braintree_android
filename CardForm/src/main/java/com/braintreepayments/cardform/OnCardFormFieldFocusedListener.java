package com.braintreepayments.cardform;

import android.view.View;

/**
 * Listener to receive a callback when a field is focused in the card form
 */
public interface OnCardFormFieldFocusedListener {

    /**
     * @param field that was focused
     */
    public void onCardFormFieldFocused(View field);
}
