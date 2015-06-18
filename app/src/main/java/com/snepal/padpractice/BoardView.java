package com.snepal.padpractice;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


/**
 * TODO: document your custom view class.
 */
public class BoardView extends View {
    private static final int ROWS = 5;
    private static final int COLS = 6;

    private Piece selectedPiece;
    private int selectedPieceRow;
    private int selectedPieceCol;
    private float currTouchX;
    private float currTouchY;
    private float box;

    private Paint lighBackground;
    private Paint darkBackground;
    private Paint selected;

    private Piece[][] pieces = new Piece[5][6];

    private boolean allowMovement = true;
    private boolean hasCollided = false;
    private OnTouchStartedListener onTouchStartedListener;
    private OnTouchEndedListener onTouchEndedListener;

    // Animation stuff
    private boolean isSwapAnimation = false;
    private int swapStage = 0;
    private Piece swappedPiece;

    public BoardView(Context context) {
        super(context);
        init(null, 0);
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public BoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        lighBackground = new Paint();
        lighBackground.setColor(Color.argb(255, 85, 51, 34));
        darkBackground = new Paint();
        darkBackground.setColor(Color.argb(255, 51, 34, 34));
        selected = new Paint();
        selected.setAlpha(100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawARGB(255, 0, 0, 0);

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                float left = j*box;
                float top = i*box;
                float right = (j+1)*box;
                float bottom = (i+1)*box;
                canvas.drawRect(left, top, right, bottom, ((i + j) % 2 == 1) ? lighBackground : darkBackground);
            }
        }
        boolean drawSelected = false;
        boolean delayedInvalidate = false;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                Piece p = pieces[i][j];
                if (p == null) {
                    continue;
                }
                if (p.selected) {
                    drawSelected = true;
                } else {
                    if (p.isAnimating()) {
                        p.animate();
                        delayedInvalidate = true;
                    }
                    canvas.drawBitmap(p.getBitmap(), p.x - (p.getBitmap().getWidth()/2), p.y - (p.getBitmap().getHeight()/2), null);
                }
            }
        }

        // Draw selected piece at the end so it is drawn with highest z-index
        if (drawSelected) {
            if (selectedPiece.isAnimating()) {
                selectedPiece.animate();
                delayedInvalidate = true;
            }
            canvas.drawBitmap(selectedPiece.getBitmap(), selectedPiece.x - (box/2), selectedPiece.y - (box/2), selected);
            canvas.drawBitmap(selectedPiece.getScaledBitmap(), currTouchX - (box/2), (currTouchY - 50) - (box/2), selected);
        }

        if (delayedInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                selectedPieceRow = (int) (event.getY()/box);
                selectedPieceCol = (int) (event.getX()/box);
                selectedPiece = pieces[selectedPieceRow][selectedPieceCol];
                selectedPiece.selected = true;
                currTouchX = event.getX();
                currTouchY = event.getY();
                ViewCompat.postInvalidateOnAnimation(this);
                break;
            case MotionEvent.ACTION_MOVE:
                if (allowMovement) {
                    currTouchX = event.getX();
                    currTouchY = event.getY();
                    handleCollision();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (allowMovement) {
                    currTouchX = event.getX();
                    currTouchY = event.getY();
                    finishMoving();
                    ViewCompat.postInvalidateOnAnimation(this);
                } else {
                    allowMovement = true;
                }
                break;
        }
        return true;
    }

    private void handleCollision() {
        int row = (int) (currTouchY / box);
        int col = (int) (currTouchX / box);
        if (row < 0 || row > ROWS || col < 0 || col > COLS ) {
            return;
        }
        if (! pieces[row][col].equals(selectedPiece)) {
            Piece tmp = pieces[row][col];
            pieces[row][col] = selectedPiece;
            pieces[selectedPieceRow][selectedPieceCol] = tmp;
            float newY = (selectedPieceRow * box + (selectedPieceRow + 1) * box)/2;
            float newX = (selectedPieceCol * box + (selectedPieceCol + 1) * box)/2;

            AnimationDirection dir;
            if (row != selectedPieceRow && col != selectedPieceCol) {
                dir = AnimationDirection.DIAGONAL;
            } else if (row != selectedPieceRow) {
                dir = AnimationDirection.VERTICAL;
            } else {
                dir = AnimationDirection.HORIZONTAL;
            }
            selectedPiece.animateTo(tmp.x, tmp.y, dir);
            tmp.animateTo(newX, newY, dir);
            selectedPieceRow = row;
            selectedPieceCol = col;

            if (! hasCollided) {
                onTouchStartedListener.onStart();
                hasCollided = true;
            }
        }
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void finishMoving() {
        int row = (int) (currTouchY / box);
        int col = (int) (currTouchX / box);
        float newY, newX;

        if (row < 0 || row > ROWS || col < 0 || col > COLS) {
            newY = selectedPiece.y;
            newX = selectedPiece.x;
        } else {
            newY = (row * box + (row + 1) * box)/2;
            newX = (col * box + (col + 1) * box)/2;
        }

        selectedPiece.move(newX, newY);
        selectedPiece.selected = false;
        hasCollided = false;
        onTouchEndedListener.onEnd();
    }

    public void setPieces(Piece[][] pieces) {
        this.pieces = pieces;
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setBoxWidth(float b) {
        box = b;
    }

    public void setOnTouchStartedListener(OnTouchStartedListener onTouchStartedListener) {
        this.onTouchStartedListener = onTouchStartedListener;
    }

    public void setOnTouchEndedListener(OnTouchEndedListener onTouchEndedListener) {
        this.onTouchEndedListener = onTouchEndedListener;
    }

    public void stopMovement() {
        if (selectedPiece.selected) {
            allowMovement = false;
        }
        finishMoving();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public interface OnTouchStartedListener {
        void onStart();
    }

    public interface OnTouchEndedListener {
        void onEnd();
    }
}
