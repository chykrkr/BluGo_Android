package com.example.user.blugo;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by user on 2016-06-02.
 */
public class GoRuleJapan extends GoRule {
    private ArrayList<BoardState> timeline = new ArrayList<>();
    private ArrayList<GoControl.GoAction> action_history = new ArrayList<>();
    private int seq_no = 0;

    GoRuleJapan()
    {
        BoardState state = new BoardState();

        timeline.add(state);
    }

    @Override
    public ArrayList<GoControl.GoAction> get_stones() {
        BoardState state = timeline.get(timeline.size() - 1);
        return state.stone_pos;
    }

    @Override
    public ArrayList<GoControl.GoAction> get_action_history() {
        return action_history;
    }

    public ArrayList<BoardState> getTimeline()
    {
	return timeline;
    }

    public void pass(GoControl.Player next_turn)
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
        GoControl.GoAction pos;
        Point single_stone;
        int dead_count;
        BoardState state = null;

        try {
            BoardState tmp = timeline.get(timeline.size() - 1);
            state = (BoardState) (tmp.clone());
            state.next_turn = next_turn;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        pos = new GoControl.GoAction(stone_color, x, y);

        single_stone = new Point(-1, -1);

        /* Rule 1 : Check if there is a stone already. */
        if (state.stone_pos.contains(pos)) {
            state = null;
            return  false;
        }

        state.stone_pos.add(pos);
        add_stone_to_link(state, pos);

        /* remove opponent dead group */
        dead_count = remove_dead_stones(
            state,
            (pos.player == GoControl.Player.BLACK) ?
                GoControl.Player.WHITE : GoControl.Player.BLACK,
            board_size,
            single_stone
        );

        /* Is opponent's dead stone marked as KO */
        if (dead_count == 1 && state.ko_pos != null && state.ko_pos.equals(single_stone)) {
            /* We cannot kill just one stone that is marked as KO */
            state = null;
            return false;
        }
        /* If we killed just one stone, mark current stone position as a ko */
        if (dead_count == 1) {
            state.ko_pos = pos.where;
        } else
            state.ko_pos = null;

        /* remove my dead group */
        dead_count = remove_dead_stones(
            state,
            pos.player,
            board_size,
            single_stone
        );

        /* It's nonsens if dead_count is greater than 0. Because it means suicide. */
        if (dead_count > 0) {
            /* It's suicide. Suicide is not allowed */
            state = null;
            return false;
        }

        seq_no++;
        action_history.add(pos);
        timeline.add(state);

        return true;
    }

    @Override
    public boolean undo()
    {
	if (timeline.size() <= 1)
	    return false;

        action_history.remove(action_history.size() - 1);

	timeline.remove(timeline.size() - 1);
	return true;
    }

    private void add_stone_to_link(BoardState state, GoControl.GoAction pos)
    {
        int i;
        ArrayList links;
        ArrayList<Object> link_index = new ArrayList<Object>();
        ArrayList<GoControl.GoAction> new_link, link;

        links = (pos.player == GoControl.Player.BLACK)? state.black_links : state.white_links;

        for (i = 0 ; i < links.size() ; i++) {
            link = (ArrayList<GoControl.GoAction>) links.get(i);

            if (isLinkable(link, pos)) {
                link_index.add(link);
            }
        }

        /* Make a link */
        if (link_index.size() == 0) {
            new_link = new ArrayList<GoControl.GoAction>();
            new_link.add(pos);
            links.add(new_link);
            return;
        }

        /* Add to a exist link */
        if (link_index.size() == 1) {
            link = (ArrayList<GoControl.GoAction>) link_index.get(0);
            link.add(pos);
            return;
        }

        /* Make new merged link */
        new_link = new ArrayList<GoControl.GoAction>();

        for (i = 0 ; i < link_index.size() ; i++) {
            link = (ArrayList<GoControl.GoAction>) link_index.get(i);

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

    private int remove_dead_stones(BoardState state, GoControl.Player color, int board_size, Point single_stone)
    {
        int i, j, dead_count = 0;
        ArrayList links;
        ArrayList<GoControl.GoAction> link;

        links = (color== GoControl.Player.BLACK)? state.black_links : state.white_links;

        for (i = 0 ; i < links.size() ; ) {
            link = (ArrayList<GoControl.GoAction>)links.get(i);

            if (check_link_dead(state.stone_pos, link, board_size)) {
                dead_count += link.size();

                if (dead_count == 1) {
                    single_stone.x = ((GoControl.GoAction) link.get(0)).where.x;
                    single_stone.y = ((GoControl.GoAction) link.get(0)).where.y;
                }

                state.stone_pos.removeAll(link);
                links.remove(i);
                continue;
            }

            i++;
        }

        return dead_count;
    }

    private boolean check_link_dead(ArrayList<GoControl.GoAction> stone_pos, ArrayList<GoControl.GoAction> link, int board_size)
    {
        int i;

        /* To disallow duplication (It makes count easy)*/
        HashSet<GoControl.GoAction> life_count = new HashSet<GoControl.GoAction>();

        for (i = 0 ; i < link.size() ; i++) {
            GoControl.GoAction pos = link.get(i);
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

    private boolean isLinkable(ArrayList<GoControl.GoAction> link, GoControl.GoAction pos) {
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
