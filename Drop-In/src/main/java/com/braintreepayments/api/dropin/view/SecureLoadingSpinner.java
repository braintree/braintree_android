package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.dropin.utils.LoadingSpinnerInterpolator;

/**
 * A custom {@link android.widget.ProgressBar} with extra graphics for waiting states.
 * Designed to give customers an extra sense of security while dealing with sensitive data.
 */
public class SecureLoadingSpinner extends RelativeLayout {

    public SecureLoadingSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SecureLoadingSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        RelativeLayout.LayoutParams fillParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        fillParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        ProgressBar loadingSpinner =
                (ProgressBar) inflate(context, R.layout.secure_loading_spinner, null);
        loadingSpinner.setIndeterminate(true);
        loadingSpinner.setIndeterminateDrawable(
                context.getResources().getDrawable(R.drawable.loading_animation));
        loadingSpinner.setInterpolator(new LoadingSpinnerInterpolator());
        loadingSpinner.setLayoutParams(fillParams);
        addView(loadingSpinner);

        TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.SecureLoadingSpinner, 0, 0);
        int size = styledAttributes.getLayoutDimension(R.styleable.SecureLoadingSpinner_imageSize, 0);
        styledAttributes.recycle();

        RelativeLayout.LayoutParams imageParams = new LayoutParams(size, size);
        imageParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        ImageView lockView = new ImageView(context);
        lockView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_lock));
        lockView.setLayoutParams(imageParams);
        lockView.setPadding(0, 0, 0, 4);
        addView(lockView);
    }
}
