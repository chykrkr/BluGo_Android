package com.example.user.blugo;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.PathShape;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class GoBoardView extends View  {
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private int mBoardSize = 19;

    public final static int EMPTY = 0;
    public final static int BLACK_STONE = 1;
    public final static int WHITE_STONE = 2;
    private final static int OPAQUE_ALPHA = 255;
    private final static int GHOST_ALPHA = 50;

    private ArrayList<BoardPos> stone_pos = new ArrayList<BoardPos>();

    private int current_turn = BLACK_STONE;
    private Point ghost_pos = new Point(-1, -1);

    private int board_canvas_x = -1, board_canvas_y = -1;
    private int board_canvas_w = -1, board_canvas_h = -1;
    private float board_square_size = -1;

    private class BoardPos {
        public int x, y;
        public int state = EMPTY;

        public BoardPos(Point pos, int state) {
            x = pos.x;
            y = pos.y;
            this.state = state;
        }

        public BoardPos(int x, int y, int state) {
            this.x = x;
            this.y = y;
            this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            BoardPos p = (BoardPos) o;
            if (p.x == x && p.y == y)
                return true;

            return false;
        }
    }

    public GoBoardView(Context context) {
        super(context);
        init(null, 0);
    }

    public GoBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public GoBoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private Point getGhost_pos(float x, float y)
    {
        Point p = new Point(-1, -1);
        int i,j;
        int found_x = -1, found_y = -1;
        float pos_x, pos_y;
        float tol = board_square_size * 0.9f;


        if (x < board_canvas_x || x > (board_canvas_x + board_canvas_w))
            return p;

        if (y < board_canvas_y || y > (board_canvas_y + board_canvas_h))
            return p;

        for (i = 0 ; i < mBoardSize ; i++) {
            pos_x = board_canvas_x + board_square_size/2.0f + i * (board_canvas_w -  board_square_size)/(mBoardSize - 1);

            if ( x > pos_x - tol && x <= pos_x + tol) {
                found_x = i;
                break;
            }
        }

        if (found_x == -1)
            return p;

        for (j = 0 ; j < mBoardSize ; j++) {
            pos_y = board_canvas_y + board_square_size/2.0f + j * (board_canvas_h -  board_square_size)/(mBoardSize - 1);

            if (y > pos_y - tol && y <= pos_y + tol) {
                found_y = j;
                break;
            }
        }

        if (found_y == -1)
            return p;

        p.x = found_x;
        p.y = found_y;

        return p;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Point p;

        Log.d("TOUCHEVT", event.toString());

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                p = getGhost_pos(event.getX(), event.getY());

                if (!ghost_pos.equals(p)) {
                    ghost_pos.x = p.x;
                    ghost_pos.y = p.y;
                } else {
                    /* Cancel putting stone if current position isn't empty.*/
                    if (stone_pos.contains(new BoardPos(ghost_pos, EMPTY)))
                        break;
                    this.putStoneAt(p.x, p.y, current_turn);
                }

                this.invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                p = getGhost_pos(event.getX(), event.getY());

                if (!ghost_pos.equals(p)) {
                    ghost_pos.x = p.x;
                    ghost_pos.y = p.y;
                    this.invalidate();
                }
                break;
        }
        return true;
        //return super.onTouchEvent(event);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
            attrs, R.styleable.GoBoardView, defStyle, 0);

        mExampleString = a.getString(
            R.styleable.GoBoardView_exampleString);
        mExampleColor = a.getColor(
            R.styleable.GoBoardView_exampleColor,
            mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
            R.styleable.GoBoardView_exampleDimension,
            mExampleDimension);

        if (a.hasValue(R.styleable.GoBoardView_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                R.styleable.GoBoardView_exampleDrawable);
            mExampleDrawable.setCallback(this);
        }

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        ShapeDrawable boardline, flower_point;
        Path path;
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        int i, j;
        float tmpx, tmpy, tmpw, tmph;
        float tmp;

        Paint paint;

        Log.d("DIM", getWidth() + "x" + getHeight());

        // Draw the text.
        canvas.drawText(mExampleString,
            paddingLeft + (contentWidth - mTextWidth) / 2,
            paddingTop + (contentHeight + mTextHeight) / 2,
            mTextPaint);

        if (contentWidth > contentHeight) {
            board_canvas_h = board_canvas_w = contentHeight;
            board_canvas_x = paddingLeft + (contentWidth - contentHeight) / 2;
            board_canvas_y = paddingTop;
        } else {
            board_canvas_h = board_canvas_w = contentWidth;
            board_canvas_x = paddingLeft;
            board_canvas_y = paddingTop + (contentHeight -contentWidth) / 2;
        }

        // Draw the example drawable on top of the text.
        if (mExampleDrawable != null) {
            mExampleDrawable.setBounds(board_canvas_x, board_canvas_y, board_canvas_x + board_canvas_w, board_canvas_y + board_canvas_h);
            mExampleDrawable.draw(canvas);
        }

        board_square_size = board_canvas_w / mBoardSize;
        path = new Path();

        for (i = 0 ; i < mBoardSize; i++) {
            tmp = board_square_size/2.0f + i * (board_canvas_w -  board_square_size)/(mBoardSize - 1);
            path.moveTo(tmp, board_square_size/2.0f);
            path.lineTo(tmp, board_canvas_h - board_square_size/2);
        }

        for (i = 0 ; i < mBoardSize; i++) {
            tmp = board_square_size/2.0f + i * (board_canvas_w -  board_square_size)/(mBoardSize - 1);
            path.moveTo(board_square_size/2.0f, tmp);
            path.lineTo(board_canvas_w - board_square_size/2, tmp);
        }

        /* draw GoBoard lines */
        boardline = new ShapeDrawable(new PathShape(path, board_canvas_w, board_canvas_h));
        boardline.getPaint().setColor(0xff000000);
        boardline.getPaint().setStrokeWidth(3);
        boardline.getPaint().setStyle(Paint.Style.STROKE);
        boardline.setBounds(board_canvas_x, board_canvas_y, board_canvas_x + board_canvas_w, board_canvas_y + board_canvas_h);
        boardline.draw(canvas);

        /* draw flower point */
        if (mBoardSize >= 19) {

            flower_point = new ShapeDrawable(new OvalShape());
            flower_point.getPaint().setColor(0xff000000);


            int [] flower_pos = new int[] {
                3, 3, 9, 3, 15, 3,
                3, 9, 9, 9, 15, 9,
                3, 15, 9, 15, 15, 15
            };

            for (i = 0 ; i < flower_pos.length - 1;) {
                draw_flower(canvas, flower_point, board_canvas_x, board_canvas_y,
                    flower_pos[i], flower_pos[i + 1], board_canvas_w, board_canvas_h, (int) board_square_size);
                i += 2;
            }
        }

        for ( i = 0 ; i < stone_pos.size() ; i++) {
            BoardPos p = stone_pos.get(i);

            draw_stone(canvas, p.state, board_canvas_x, board_canvas_y, p.x, p.y, board_canvas_w, board_canvas_h, (int) board_square_size, OPAQUE_ALPHA);
        }

        boolean draw_ghost;

        draw_ghost = current_turn == BLACK_STONE || current_turn == WHITE_STONE;
        draw_ghost = draw_ghost && (ghost_pos.x >= 0 && ghost_pos.y < mBoardSize);
        draw_ghost = draw_ghost && (ghost_pos.y >= 0 && ghost_pos.y < mBoardSize);
        draw_ghost = draw_ghost && !stone_pos.contains(new BoardPos(ghost_pos.x, ghost_pos.y, EMPTY));

        /* draw ghost */
        if (draw_ghost) {
            draw_stone(canvas, current_turn, board_canvas_x, board_canvas_y, ghost_pos.x, ghost_pos.y, board_canvas_w, board_canvas_h, (int) board_square_size, GHOST_ALPHA);
        }
        /*
        draw_stone(canvas, 0, x, y, 2, 2, width, height, (int) square);
        draw_stone(canvas, 1, x, y, 3, 2, width, height, (int) square);
        draw_stone(canvas, 0, x, y, 4, 2, width, height, (int) square);
        draw_stone(canvas, 0, x, y, 3, 1, width, height, (int) square);
        draw_stone(canvas, 0, x, y, 3, 3, width, height, (int) square);
        */
    }

    private void draw_stone(Canvas canvas, int stone_color, int x, int y, int i, int j,
                             int width, int height, int square, int alpha)
    {
        int tmpx, tmpy, tmpw, tmph;
        Resources res = getContext().getResources();
        Drawable image;

        if (stone_color == BLACK_STONE)
            image = res.getDrawable(R.drawable.go_b_no_bg);
        else
            image = res.getDrawable(R.drawable.go_w_no_bg);

        tmph = tmpw = (square - square / 11) / 2;

        tmpx = x + square/2 + i * (width -  square)/(mBoardSize - 1);
        tmpy = y + square/2 + j * (height -  square)/(mBoardSize - 1);

        image.setBounds(tmpx - tmpw, tmpy - tmph, tmpx + tmpw, tmpy + tmph);
        image.setAlpha(alpha);
        image.draw(canvas);
    }

    private void draw_flower(Canvas canvas, ShapeDrawable s, int x, int y, int i, int j,
                             int width, int height, int square)
    {
        int tmpx, tmpy, tmpw, tmph;

        tmph = tmpw = square/7;

        tmpx = x + square/2 + i * (width -  square)/(mBoardSize - 1);
        tmpy = y + square/2 + j * (height -  square)/(mBoardSize - 1);
        s.setBounds(tmpx - tmpw, tmpy - tmph, tmpx + tmpw, tmpy + tmph);
        s.draw(canvas);
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    public int getBoardSize() {
        return mBoardSize;
    }

    public void setBoardSize(int boardSize) {
        mBoardSize = boardSize;
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }

    public void putStoneAt(int x, int y, int state)
    {
        BoardPos pos = new BoardPos(x, y, state);

        if (stone_pos.contains(pos)) {
            stone_pos.remove(pos);
        }

        if (state != GoBoardView.EMPTY) {
            stone_pos.add(pos);
        }
    }

    public void setCurrent_turn(int turn)
    {
        current_turn = turn;
    }

    public int getCurrent_turn()
    {
        return current_turn;
    }
}
