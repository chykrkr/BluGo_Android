package com.example.user.blugo;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by user on 2016-06-02.
 */
public class GoRuleJapan extends GoRule {
    private ArrayList<NewBoardState> new_timeline = new ArrayList<>();
    private ArrayList<GoControl.GoAction> action_history = new ArrayList<>();
    private int seq_no = 0;

    GoRuleJapan()
    {
        new_timeline.add(new NewBoardState());
    }

    @Override
    public HashSet<GoControl.GoAction> get_stones() {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        return state.get_stones();
    }

    @Override
    public ArrayList<BoardPos> get_calc_info() {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        return state.get_calc_info();
    }

    @Override
    public ArrayList<GoControl.GoAction> get_action_history() {
        return action_history;
    }

    public void pass(GoControl.Player next_turn)
    {
        NewBoardState state = null;

        /* copy time line */
        try {
            NewBoardState tmp = new_timeline.get(new_timeline.size() - 1);
            state = (NewBoardState) (tmp.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        action_history.add(new GoControl.GoAction(
            (next_turn == GoControl.Player.BLACK)? GoControl.Player.WHITE : GoControl.Player.BLACK,
            null, GoControl.Action.PASS));

        state.ko_x = state.ko_y = -1;
        seq_no++;
        new_timeline.add(state);
    }

    @Override
    public boolean putStoneAt(int x, int y, GoControl.Player stone_color, GoControl.Player next_turn, int board_size) {
        NewBoardState state = null;
        GoControl.GoAction pos;

        try {
            NewBoardState tmp = new_timeline.get(new_timeline.size() - 1);
            state = (NewBoardState) (tmp.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        pos = new GoControl.GoAction(stone_color, x, y);

        if (!state.put_stone(pos)) {
            state = null;
            return false;
        }

        action_history.add(pos);
        new_timeline.add(state);
        return true;
    }

    @Override
    public void toggle_owner(int x, int y) {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        state.toggle_owner(x, y);
    }

    @Override
    public boolean undo()
    {
        if (new_timeline.size() <= 1)
            return false;

        action_history.remove(action_history.size() - 1);

        new_timeline.remove(new_timeline.size() - 1);
        return true;
    }

    @Override
    public void get_dead(AtomicInteger white, AtomicInteger black) {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        white.set(state.white_dead);
        black.set(state.black_dead);
    }

    @Override
    public void get_score(AtomicInteger white, AtomicInteger black) {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        state.get_score(white, black);
    }

    @Override
    public void cancel_calc()
    {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        state.cancel_calc();
    }
}
