package com.example.user.blugo;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by user on 2016-06-02.
 */
public abstract class GoControl {
    protected Callback callback_receiver = null;

    public enum Action {
        PUT, PASS,
    }

    public enum Player {
        WHITE, BLACK,
    }

    public static class GoAction {
        /* who */
        public Player player;
        /* what */
        public Action action = Action.PUT;
        /* where */
        public Point where = null;

        public GoAction(int x, int y) {
            this(Player.BLACK, new Point(x, y), Action.PUT);
        }

        public GoAction(Player player, int x, int y) {
            this(player, new Point(x, y), Action.PUT);
        }

        public GoAction(Player player, int x, int y, Action action) {
            this(player, new Point(x, y), action);
        }

        public GoAction(Player player, Point where, Action action) {
            this.player = player;
            this.where = where;
            this.action = action;
        }

        public String get_sgf_string()
        {
            String string = ";";
            switch (player) {
                case WHITE:
                    string += "W";
                    break;
                case BLACK:
                    string += "B";
                    break;
            }

            string += "[";

            if (action == Action.PUT && where != null) {
                /* a - z : 0 ~ 24 */
                /* A - Z : 25 ~ 49 */
                string += (char)(where.x + (int)('a'));
                string += (char)(where.y + (int)('a'));
            }

            string += "]";

            return string;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            Point p;
            if (where == null)
                p = where;
            else
                p = new Point(where.x, where.y);

            return new GoAction(player, p, action);
        }

        @Override
        public boolean equals(Object o) {
            GoAction go_action;
            Point p;

            go_action = (GoAction) o;

            if (go_action.where == null)
                return false;

            if (this.where == null)
                return false;

            return this.where.equals(go_action.where);
        }

        @Override
        public String toString() {
            return get_sgf_string();
        }
    }

    public interface Callback {
        public void callback_board_state_changed();
    }

    public abstract boolean isMyTurn();
    public abstract  ArrayList<GoAction> getStone_pos();
    public abstract int getBoardSize();
    public abstract Player getCurrent_turn();
    public abstract boolean putStoneAt(int x, int y, boolean pass);
    public abstract String get_sgf();

    public abstract void pass();
    public abstract void undo();

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
