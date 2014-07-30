package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.braintreepayments.api.dropin.R;

public class LoadingHeader extends RelativeLayout {

    public enum HeaderState {
        LOADING, SUCCESS, ERROR
    }

    private SecureLoadingSpinner mLoadingSpinner;
    private ImageView mStatusIcon;
    private TextView mHeaderMessage;

    private HeaderState mCurrentState;

    public LoadingHeader(Context context) {
        super(context);
        init(context);
    }

    public LoadingHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadingHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.loading_header, this);

        mLoadingSpinner = findView(R.id.header_loading_spinner);
        mStatusIcon = findView(R.id.header_status_icon);
        mHeaderMessage = findView(R.id.header_message);
    }

    public HeaderState getCurrentState() {
        return mCurrentState;
    }

    public void setLoading() {
        mCurrentState = HeaderState.LOADING;

        mStatusIcon.setVisibility(GONE);
        mHeaderMessage.setVisibility(GONE);

        setBackgroundColor(getResources().getColor(R.color.bt_white));
        mLoadingSpinner.setVisibility(VISIBLE);

        setVisibility(VISIBLE);
    }

    public void setSuccessful() {
        mCurrentState = HeaderState.SUCCESS;

        mLoadingSpinner.setVisibility(GONE);
        mHeaderMessage.setVisibility(GONE);

        setBackgroundColor(getResources().getColor(R.color.bt_blue));
        mStatusIcon.setImageResource(R.drawable.ic_success);
        mStatusIcon.setVisibility(VISIBLE);

        setVisibility(VISIBLE);
    }

    public void setError(String errorMessage) {
        mCurrentState = HeaderState.ERROR;

        mLoadingSpinner.setVisibility(GONE);

        mHeaderMessage.setVisibility(GONE);
        mHeaderMessage.setText(errorMessage);

        setBackgroundColor(getResources().getColor(R.color.bt_red));
        mStatusIcon.setImageResource(R.drawable.ic_error);
        mStatusIcon.setVisibility(VISIBLE);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setStartOffset(500);
        fadeIn.setDuration(200);

        AnimationSet fadeInAnimation = new AnimationSet(false);
        fadeInAnimation.addAnimation(fadeIn);
        fadeInAnimation.setRepeatMode(0);
        fadeInAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mHeaderMessage.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(500);

        AnimationSet fadeOutAnimation = new AnimationSet(false);
        fadeOutAnimation.addAnimation(fadeOut);
        fadeOutAnimation.setRepeatMode(0);
        fadeOutAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mStatusIcon.setVisibility(GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        mStatusIcon.setAnimation(fadeOutAnimation);
        mHeaderMessage.setAnimation(fadeInAnimation);

        setVisibility(VISIBLE);
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

}
