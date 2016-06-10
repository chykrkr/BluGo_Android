package com.example.user.blugo;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by user on 2016-06-02.
 */
public abstract class GoRule {
    public enum BoardPosState {
        EMPTY(0x00),
        BLACK(0x01),
        WHITE(0x02),
        BLACK_DEAD(0x03),
        WHITE_DEAD(0x04),
        EMPTY_NEUTRAL(0x5),
        EMPTY_BLACK(0x6),
        EMPTY_WHITE(0x7);

        private final int value;

        private BoardPosState(int value) {
            this.value = value;
        }

        public static BoardPosState valueOf(int type)
        {
            /*
            Enumeration values must be sequential or else
            ArrayIndexoutofbound exeception may be thrown.
            */
            return (BoardPosState) BoardPosState.values()[type];
        }

        public int getValue()
        {
            return value;
        }
    }

    public static class BoardPos {
        BoardPosState state = BoardPosState.EMPTY;
        int group_id = 0;

        @Override
        protected Object clone() throws CloneNotSupportedException {
            BoardPos pos = new BoardPos();
            pos.state = this.state;
            pos.group_id = this.group_id;

            return pos;
        }
    }


    public abstract HashSet<GoControl.GoAction> get_stones();
    public abstract ArrayList<BoardPos> get_calc_info();
    public abstract ArrayList<NewBoardState> get_time_line();

    public abstract ArrayList<GoControl.GoAction> get_action_history();
    /*public abstract ArrayList<BoardState> getTimeline();*/
    public abstract boolean putStoneAt(int x, int y, GoControl.Player stone_color, GoControl.Player next_turn, int board_size);
    public abstract void toggle_owner(int x, int y);
    public abstract void pass(GoControl.Player next_turn);
    public abstract boolean undo();
    public abstract void cancel_calc();
    public abstract void prepare_calc();

    public abstract void get_dead(AtomicInteger white, AtomicInteger black);
    public abstract void get_score(AtomicInteger white, AtomicInteger black,
                          AtomicInteger add_wd, AtomicInteger add_bd,
                          AtomicInteger wcount, AtomicInteger bcount);
}
