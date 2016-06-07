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
import android.graphics.drawable.shapes.RectShape;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;

import com.example.user.blugo.GoControl;

/**
 * TODO: document your custom view class.
 */
public class GoBoardView extends View implements GoControl.Callback {
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private final static int OPAQUE_ALPHA = 255;
    private final static int GHOST_ALPHA = 50;


    private Point ghost_pos = new Point(-1, -1);

    private int board_canvas_x = -1, board_canvas_y = -1;
    private int board_canvas_w = -1, board_canvas_h = -1;
    private float board_square_size = -1;

    private GoControl go_control = null;

    public boolean isView_only_mode() {
        return view_only_mode;
    }

    public void setView_only_mode(boolean view_only_mode) {
        this.view_only_mode = view_only_mode;
    }

    private boolean view_only_mode = false;

    public GoControl getGo_control() {
        return go_control;
    }

    public void setGo_control(GoControl go_control) {
        this.go_control = go_control;
        go_control.setCallback_receiver(this);
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
        int board_size;

        if (go_control == null)
            return p;

        if (x < board_canvas_x || x > (board_canvas_x + board_canvas_w))
            return p;

        if (y < board_canvas_y || y > (board_canvas_y + board_canvas_h))
            return p;

        board_size = go_control.getBoardSize();

        for (i = 0 ; i < board_size ; i++) {
            pos_x = board_canvas_x + board_square_size/2.0f + i * (board_canvas_w -  board_square_size)/(board_size - 1);

            if ( x > pos_x - tol && x <= pos_x + tol) {
                found_x = i;
                break;
            }
        }

        if (found_x == -1)
            return p;

        for (j = 0 ; j < board_size ; j++) {
            pos_y = board_canvas_y + board_square_size/2.0f + j * (board_canvas_h -  board_square_size)/(board_size - 1);

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

        if (view_only_mode)
            return super.onTouchEvent(event);

        if (go_control == null)
            return true;

        if (!go_control.isMyTurn()) {
            ghost_pos.x = ghost_pos.y = -1;
            return true;
        }

        //Log.d("TOUCHEVT", event.toString());

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                p = getGhost_pos(event.getX(), event.getY());

                if (go_control.calc_mode()) {
                    go_control.putStoneAt(p.x, p.y, false);
                } else if (!ghost_pos.equals(p)) {
                    ghost_pos.x = p.x;
                    ghost_pos.y = p.y;
                    this.invalidate();
                } else {
                    /* If putStoneAt is successful then this view is updated automatically */
                    go_control.putStoneAt(p.x, p.y, false);
                }
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
        ShapeDrawable boardline, flower_point, rect;
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

        if (go_control == null)
            return;

        int board_size = go_control.getBoardSize();

        board_square_size = board_canvas_w / board_size;
        path = new Path();

        for (i = 0 ; i < board_size; i++) {
            tmp = board_square_size/2.0f + i * (board_canvas_w -  board_square_size)/(board_size - 1);
            path.moveTo(tmp, board_square_size/2.0f);
            path.lineTo(tmp, board_canvas_h - board_square_size/2);
        }

        for (i = 0 ; i < board_size; i++) {
            tmp = board_square_size/2.0f + i * (board_canvas_w -  board_square_size)/(board_size - 1);
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
        if (board_size >= 19) {

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

        boolean draw_ghost = false;

        if (go_control.calc_mode()) {
            ArrayList<GoRule.BoardPos> info = go_control.get_calc_info();

            rect = new ShapeDrawable(new RectShape());

            for (i = 0 ; i < info.size() ; i++) {
                GoRule.BoardPos cinfo = info.get(i);

                switch (cinfo.state) {
                    case BLACK:
                        draw_stone(canvas, GoControl.Player.BLACK,
                            board_canvas_x, board_canvas_y,
                            i % board_size, i / board_size,
                            board_canvas_w, board_canvas_h,
                            (int) board_square_size, OPAQUE_ALPHA);
                        continue;
                    case WHITE:
                        draw_stone(canvas, GoControl.Player.WHITE,
                            board_canvas_x, board_canvas_y,
                            i % board_size, i / board_size,
                            board_canvas_w, board_canvas_h,
                            (int) board_square_size, OPAQUE_ALPHA);
                        continue;
                }

                switch (cinfo.state) {
                    case EMPTY:
                    case EMPTY_NEUTRAL:
                        rect.getPaint().setColor(0xffff0000);
                        break;

                    case WHITE_DEAD:
                        draw_stone(canvas, GoControl.Player.WHITE,
                            board_canvas_x, board_canvas_y,
                            i % board_size, i / board_size,
                            board_canvas_w, board_canvas_h,
                            (int) board_square_size, OPAQUE_ALPHA);
                    case EMPTY_BLACK:
                        rect.getPaint().setColor(0xff000000);
                        break;

                    case BLACK_DEAD:
                        draw_stone(canvas, GoControl.Player.BLACK,
                            board_canvas_x, board_canvas_y,
                            i % board_size, i / board_size,
                            board_canvas_w, board_canvas_h,
                            (int) board_square_size, OPAQUE_ALPHA);
                    case EMPTY_WHITE:
                        rect.getPaint().setColor(0xffffffff);
                        break;
                }

                draw_rect(canvas, rect,  board_canvas_x, board_canvas_y,
                    i % board_size, i / board_size,
                    board_canvas_w, board_canvas_h, (int) board_square_size);
            }
        } else {
            HashSet<GoControl.GoAction> stone_pos = go_control.getStone_pos();

            for (GoControl.GoAction p : stone_pos) {
                if (p.action != GoControl.Action.PUT || p.where == null)
                    continue;

                draw_stone(canvas, p.player, board_canvas_x, board_canvas_y, p.where.x, p.where.y, board_canvas_w, board_canvas_h, (int) board_square_size, OPAQUE_ALPHA);
            }

            if (view_only_mode) {
                draw_ghost = false;
            } else {
                draw_ghost = go_control.getCurrent_turn() == GoControl.Player.BLACK || go_control.getCurrent_turn() == GoControl.Player.WHITE;
                draw_ghost = draw_ghost && (ghost_pos.x >= 0 && ghost_pos.y < board_size);
                draw_ghost = draw_ghost && (ghost_pos.y >= 0 && ghost_pos.y < board_size);
                draw_ghost = draw_ghost && !stone_pos.contains(new GoControl.GoAction(ghost_pos.x, ghost_pos.y));
                draw_ghost = draw_ghost && !go_control.calc_mode();
            }
        }

        /* draw ghost */
        if (draw_ghost) {
            draw_stone(canvas, go_control.getCurrent_turn(), board_canvas_x, board_canvas_y, ghost_pos.x, ghost_pos.y, board_canvas_w, board_canvas_h, (int) board_square_size, GHOST_ALPHA);
        }

        Point cur_coord = go_control.get_cur_coord();

        if (view_only_mode && cur_coord != null) {
            ShapeDrawable red_dot = new ShapeDrawable(new OvalShape());
            red_dot.getPaint().setColor(0xffff0000);

            draw_reddot(canvas, red_dot,
                board_canvas_x, board_canvas_y, cur_coord.x, cur_coord.y,
                board_canvas_w, board_canvas_h, (int) board_square_size,
                (int) (board_square_size / 5));
        }
        /*
        draw_stone(canvas, 0, x, y, 2, 2, width, height, (int) square);
        draw_stone(canvas, 1, x, y, 3, 2, width, height, (int) square);
        draw_stone(canvas, 0, x, y, 4, 2, width, height, (int) square);
        draw_stone(canvas, 0, x, y, 3, 1, width, height, (int) square);
        draw_stone(canvas, 0, x, y, 3, 3, width, height, (int) square);
        */

        Message msg;
        GoBoardViewListener parent = (GoBoardViewListener) this.getContext();
        Handler h;

        h = parent.get_msg_handler();
        msg = Message.obtain(h, GoBoardViewListener.MSG_VIEW_FULLY_DRAWN, "msg");
        h.sendMessage(msg);
    }

    private void draw_stone(Canvas canvas, GoControl.Player stone_color, int x, int y, int i, int j,
                             int width, int height, int square, int alpha)
    {
        int tmpx, tmpy, tmpw, tmph;
        int board_size;
        Drawable image;

        if (go_control == null)
            return;

        Resources res = getContext().getResources();
        board_size = go_control.getBoardSize();

        if (stone_color == GoControl.Player.BLACK)
            image = res.getDrawable(R.drawable.go_b_no_bg);
        else
            image = res.getDrawable(R.drawable.go_w_no_bg);

        tmph = tmpw = (square - square / 11) / 2;

        tmpx = x + square/2 + i * (width -  square)/(board_size - 1);
        tmpy = y + square/2 + j * (height -  square)/(board_size - 1);

        image.setBounds(tmpx - tmpw, tmpy - tmph, tmpx + tmpw, tmpy + tmph);
        image.setAlpha(alpha);
        image.draw(canvas);
    }

    private void draw_rect(Canvas canvas, ShapeDrawable s,
                           int x, int y, int i, int j,
                           int width, int height, int square)
    {
        int tmpx, tmpy, tmpw, tmph;
        int board_size;

        if (go_control == null)
            return;

        board_size = go_control.getBoardSize();

        tmph = tmpw = square/5;

        tmpx = x + square/2 + i * (width -  square)/(board_size - 1);
        tmpy = y + square/2 + j * (height -  square)/(board_size - 1);
        s.setBounds(tmpx - tmpw, tmpy - tmph, tmpx + tmpw, tmpy + tmph);
        s.draw(canvas);
    }

    private void draw_reddot(Canvas canvas, ShapeDrawable s,
                           int x, int y, int i, int j,
                           int width, int height, int square, int shape_size)
    {
        int tmpx, tmpy, tmpw, tmph;
        int board_size;

        if (go_control == null)
            return;

        board_size = go_control.getBoardSize();

        tmph = tmpw = shape_size;

        tmpx = x + square/2 + i * (width -  square)/(board_size - 1);
        tmpy = y + square/2 + j * (height -  square)/(board_size - 1);
        s.setBounds(tmpx - tmpw, tmpy - tmph, tmpx + tmpw, tmpy + tmph);
        s.draw(canvas);
    }

    private void draw_flower(Canvas canvas, ShapeDrawable s, int x, int y, int i, int j,
                             int width, int height, int square)
    {
        int tmpx, tmpy, tmpw, tmph;
        int board_size;

        if (go_control == null)
            return;

        board_size = go_control.getBoardSize();

        tmph = tmpw = square/7;

        tmpx = x + square/2 + i * (width -  square)/(board_size - 1);
        tmpy = y + square/2 + j * (height -  square)/(board_size - 1);
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

    @Override
    public void callback_board_state_changed() {
        this.invalidate();
    }
}
