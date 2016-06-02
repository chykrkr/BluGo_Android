package com.example.user.blugo;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by user on 2016-06-02.
 */
public abstract class GoControl {
    protected Callback callback_receiver = null;

    public static class BoardPos {
        public final static int EMPTY = 0;
        public final static int BLACK_STONE = 1;
        public final static int WHITE_STONE = 2;
        public int x, y;
        public int state = EMPTY;

        public BoardPos(Point pos, int state) {
            this(pos.x, pos.y, state);
        }

        public BoardPos(Point pos) {
            this(pos.x, pos.y, EMPTY);
        }

        public BoardPos(int x, int y)
        {
            this(x, y, EMPTY);
        }

        public BoardPos(int x, int y, int state) {
            this.x = x;
            this.y = y;
            this.state = state;
        }

        public BoardPos(BoardPos pos)
        {
            this(pos.x, pos.y, pos.state);
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            BoardPos pos = new BoardPos(this);
            return pos;
        }

        @Override
        public boolean equals(Object o) {
            BoardPos p = (BoardPos) o;
            if (p.x == x && p.y == y)
                return true;

            return false;
        }
    }

    public interface Callback {
        public void callback_board_state_changed();
    }

    public abstract boolean isMyTurn();
    public abstract  ArrayList<BoardPos> getStone_pos();
    public abstract int getBoardSize();
    public abstract int getCurrent_turn();
    public abstract  boolean putStoneAt(int x, int y);
    public void new_game() {

    }

    public Callback getCallback_receiver() {
        return callback_receiver;
    }

    public void setCallback_receiver(Callback callback_receiver) {
        this.callback_receiver = callback_receiver;

        callback_receiver.callback_board_state_changed();
    }
}
