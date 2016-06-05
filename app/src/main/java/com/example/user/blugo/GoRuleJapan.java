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
    private ArrayList<BoardState> timeline = new ArrayList<>();
    private ArrayList<NewBoardState> new_timeline = new ArrayList<>();
    private ArrayList<GoControl.GoAction> action_history = new ArrayList<>();
    private int seq_no = 0;

    GoRuleJapan()
    {
        timeline.add(new BoardState());
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

    //@Override
    public HashSet<GoControl.GoAction> _get_stones() {
        BoardState state = timeline.get(timeline.size() - 1);
        return state.stone_pos;
    }

    @Override
    public ArrayList<GoControl.GoAction> get_action_history() {
        return action_history;
    }

    /*
    public ArrayList<BoardState> getTimeline()
    {
	return timeline;
    }
    */

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

    public void _pass(GoControl.Player next_turn)
    {
        BoardState state = null;

        /* copy time line */
        try {
            BoardState tmp = timeline.get(timeline.size() - 1);
            state = (BoardState) (tmp.clone());
            state.next_turn = next_turn;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        action_history.add(new GoControl.GoAction(
            (next_turn == GoControl.Player.BLACK)? GoControl.Player.WHITE : GoControl.Player.BLACK,
            null, GoControl.Action.PASS));

        state.ko_pos = null;
        seq_no++;
        timeline.add(state);
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

    //@Override
    public boolean _undo()
    {
	if (timeline.size() <= 1)
	    return false;

        action_history.remove(action_history.size() - 1);

	timeline.remove(timeline.size() - 1);
	return true;
    }

    @Override
    public void cancel_calc()
    {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        state.cancel_calc();
    }

    private void add_stone_to_link(BoardState state, GoControl.GoAction pos)
    {
        int i;
        ArrayList links;
        ArrayList<Object> link_index = new ArrayList<Object>();
        HashSet<GoControl.GoAction> new_link, link;

        links = (pos.player == GoControl.Player.BLACK)? state.black_links : state.white_links;

        for (i = 0 ; i < links.size() ; i++) {
            link = (HashSet<GoControl.GoAction>) links.get(i);

            if (isLinkable(link, pos)) {
                link_index.add(link);
            }
        }

        /* Make a link */
        if (link_index.size() == 0) {
            new_link = new HashSet<GoControl.GoAction>();
            new_link.add(pos);
            links.add(new_link);
            return;
        }

        /* Add to a exist link */
        if (link_index.size() == 1) {
            link = (HashSet<GoControl.GoAction>) link_index.get(0);
            link.add(pos);
            return;
        }

        /* Make new merged link */
        new_link = new HashSet<GoControl.GoAction>();

        for (i = 0 ; i < link_index.size() ; i++) {
            link = (HashSet<GoControl.GoAction>) link_index.get(i);

            new_link.addAll(link);
        }
        new_link.add(pos);

        /* Removed links that were merged */
        for (i = 0 ; i < link_index.size() ; i++) {
            /*
                Crucial remove function is overloaded with Object type and int (primitive) type.
                 we should pass primitive int type to remove function to insure desired removing.
             */
            links.remove(link_index.get(i));
        }

        /* Add new merged link to links */
        links.add(new_link);
    }

    private int remove_dead_stones(BoardState state, GoControl.GoAction pos, int board_size, Point single_stone)
    {
        int i, j, dead_count = 0;
        ArrayList links;
        HashSet<GoControl.GoAction> link;

        //Log.d("PARS", "REMOVE DEAD GROUP START");

        links = (pos.player == GoControl.Player.WHITE)? state.black_links : state.white_links;

        for (i = 0 ; i < links.size() ; ) {
            link = (HashSet<GoControl.GoAction>)links.get(i);

            if (check_link_dead(state.stone_pos, link, board_size)) {
                dead_count += link.size();

                if (dead_count == 1) {
                    for (GoControl.GoAction action : link) {
                        single_stone.x = action.where.x;
                        single_stone.y = action.where.y;
                    }
                }

                state.stone_pos.removeAll(link);
                links.remove(i);
                continue;
            }

            i++;
        }

        //Log.d("PARS", "REMOVE DEAD GROUP END");

        return dead_count;
    }

    private boolean check_link_dead(HashSet<GoControl.GoAction> stone_pos, HashSet<GoControl.GoAction> link, int board_size)
    {
        int i;

        /* To disallow duplication (It makes count easy)*/
        HashSet<GoControl.GoAction> life_count = new HashSet<GoControl.GoAction>();

        for (GoControl.GoAction pos : link) {
            GoControl.GoAction tmp;

            /* left */
            tmp = new GoControl.GoAction(pos.where.x - 1, pos.where.y);
            if (tmp.where.x >= 0 && !stone_pos.contains(tmp)) {
                life_count.add(tmp);
            }

            /* right */
            tmp = new GoControl.GoAction(pos.where.x + 1, pos.where.y);
            if (tmp.where.x < board_size && !stone_pos.contains(tmp))
                life_count.add(tmp);

            /* up */
            tmp = new GoControl.GoAction(pos.where.x, pos.where.y - 1);
            if (tmp.where.y >= 0 && !stone_pos.contains(tmp))
                life_count.add(tmp);

            /* down */
            tmp = new GoControl.GoAction(pos.where.x, pos.where.y + 1);
            if (tmp.where.y < board_size && !stone_pos.contains(tmp))
                life_count.add(tmp);
        }

        return (life_count.size()) > 0 ? false : true;
    }

    private int calc_stone_life(ArrayList<GoControl.GoAction> stone_pos, GoControl.GoAction pos, int board_size)
    {
        GoControl.GoAction tmp;
        int life_count = 0;

        /* left */
        tmp = new GoControl.GoAction(pos.where.x - 1, pos.where.y);
        if (tmp.where.x >= 0 && !stone_pos.contains(tmp)) {
            life_count++;
        }

            /* right */
        tmp = new GoControl.GoAction(pos.where.x + 1, pos.where.y);
        if (tmp.where.x < board_size && !stone_pos.contains(tmp))
            life_count++;

            /* up */
        tmp = new GoControl.GoAction(pos.where.x, pos.where.y - 1);
        if (tmp.where.y >= 0 && !stone_pos.contains(tmp))
            life_count++;

            /* down */
        tmp = new GoControl.GoAction(pos.where.x, pos.where.y + 1);
        if (tmp.where.y < board_size && !stone_pos.contains(tmp))
            life_count++;

        return life_count;
    }

    private boolean isLinkable(HashSet<GoControl.GoAction> link, GoControl.GoAction pos) {
        boolean result;

        /* up */
        if (link.contains(new GoControl.GoAction(pos.where.x, pos.where.y - 1))) {
            return true;
        }

        /* down */
        if (link.contains(new GoControl.GoAction(pos.where.x, pos.where.y + 1))) {
            return true;
        }

        /* left */
        if (link.contains(new GoControl.GoAction(pos.where.x - 1, pos.where.y))) {
            return true;
        }

        /* right */
        if (link.contains(new GoControl.GoAction(pos.where.x + 1, pos.where.y))) {
            return true;
        }

        return false;
    }
}
