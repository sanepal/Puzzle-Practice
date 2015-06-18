package com.snepal.padpractice;

import android.graphics.Bitmap;

/**
 * Created by snepal on 6/3/2015.
 */
public class Piece {
    private static final int MAX_ANIMATION_STAGE = 6;

    float x, y;
    private Bitmap bitmap;
    private Bitmap scaledBitmap;
    boolean selected = false;
    boolean swapped = false;

    private int animationStage = -1;
    private float animateToX = 0, animateToY = 0;
    private float startX = 0, startY = 0;
    private AnimationDirection animationDirection;

    public Piece(float x, float y, Bitmap b) {
        this.x = x;
        this.y = y;
        bitmap = b;
        scaledBitmap = Bitmap.createScaledBitmap(b, (int) (b.getWidth() * 1.1), (int) (b.getHeight() * 1.1), false);
    }

    public void move(float newX, float newY) {
        finishAnimating();
        this.x = newX;
        this.y = newY;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public Bitmap getScaledBitmap() {
        return scaledBitmap;
    }

    public void animateTo(float newX, float newY, AnimationDirection dir) {
        if (isAnimating()) {
            finishAnimating();
        }
        animationDirection = dir;
        animateToX = newX;
        animateToY = newY;
        startX = x;
        startY = y;
        animationStage = MAX_ANIMATION_STAGE;
    }

    public void animate() {
        float signX = Math.signum(animateToX - startX);
        float signY = Math.signum(animateToY - startY);
        float diffX = Math.abs(startX - animateToX);
        float diffY = Math.abs(startY - animateToY);

        float stageCoeff = (MAX_ANIMATION_STAGE - (animationStage - 1f)) / MAX_ANIMATION_STAGE;

        if (animationDirection == AnimationDirection.HORIZONTAL) {
            x = startX + (signX * diffX * stageCoeff);
            if (stageCoeff > 0.5) {
                stageCoeff = 1 - stageCoeff;
            }
            if (selected) {
                y = startY - (diffX * stageCoeff);
            } else {
                y = startY + (diffX * stageCoeff);
            }
        } else if (animationDirection == AnimationDirection.VERTICAL) {
            y = startY + (signY * diffY * stageCoeff);
            if (stageCoeff > 0.5) {
                stageCoeff = 1 - stageCoeff;
            }
            if (selected) {
                x = startX - (diffY * stageCoeff);
            } else {
                x = startX + (diffY * stageCoeff);
            }
        } else {
            x = animateToX;
            y = animateToY;
            animationStage = 0;
        }

        animationStage-- ;
    }

    public boolean isAnimating() {
        return animationStage > 0;
    }

    private void finishAnimating() {
        x = animateToX;
        y = animateToY;
        animationStage = 0;
    }
}
