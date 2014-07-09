package com.braintreepayments.api.dropin.utils;

import android.view.animation.Interpolator;

public class LoadingSpinnerInterpolator implements Interpolator {

    // Bezier curve control points
    private static final float START_X = 0.645f;
    private static final float START_Y = 0.045f;
    private static final float END_X = 0.355f;
    private static final float END_Y = 1f;

    private static final float C_X = 3 * START_X;
    private static final float C_Y = 3 * START_Y;
    private static final float B_X = 3 * (END_X - START_X) - C_X;
    private static final float B_Y = 3 * (END_Y - START_Y) - C_Y;
    private static final float A_X = 1 - C_X - B_X;
    private static final float A_Y = 1 - C_Y - B_Y;

    @Override
    public float getInterpolation(float time) {
        return getBezierCoordinateY(getXForTime(time));
    }

    protected float getBezierCoordinateY(float time) {
        return time * (C_Y + time * (B_Y + time * A_Y));
    }

    private float getXForTime(float time) {
        float x = time;
        float z;
        for (int i = 1; i < 14; i++) {
            z = getBezierCoordinateX(x) - time;
            if (Math.abs(z) < 1e-3) {
                break;
            }
            x -= z / getXDerivate(x);
        }
        return x;
    }

    private float getXDerivate(float time) {
        return C_X + time * (2 * B_X + 3 * A_X * time);
    }

    private float getBezierCoordinateX(float time) {
        return time * (C_X + time * (B_X + time * A_X));
    }
}

