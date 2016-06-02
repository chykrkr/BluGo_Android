package com.example.user.blugo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

/**
 * Created by user on 2016-06-02.
 */
public class GoRuleJapan extends GoRule {
    private ArrayList<BoardState> timeline = new ArrayList<>();

    GoRuleJapan()
    {
        BoardState state = new BoardState();

        timeline.add(state);
    }

    @Override
    public ArrayList<GoControl.BoardPos> get_stones() {
        BoardState state = timeline.get(timeline.size() - 1);
        return state.stone_pos;
    }

    @Override
    public boolean putStoneAt(int x, int y, int stone_color, int board_size) {
        GoControl.BoardPos pos, single_stone;
        int tmpx, tmpy, dead_count;
        BoardState state = null;

        if (stone_color == GoControl.BoardPos.EMPTY) {
            return false;
        }

        try {
            BoardState tmp = timeline.get(timeline.size() - 1);
            state = (BoardState) (tmp.clone());
            state.turn = stone_color;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        pos = new GoControl.BoardPos(x, y, stone_color);
        single_stone = new GoControl.BoardPos(-1, -1);

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
            (pos.state == GoControl.BoardPos.BLACK_STONE) ?
                GoControl.BoardPos.WHITE_STONE : GoControl.BoardPos.BLACK_STONE,
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
            state.ko_pos = pos;
        } else
            state.ko_pos = null;

        /* remove my dead group */
        dead_count = remove_dead_stones(
            state,
            pos.state,
            board_size,
            single_stone
        );

        /* It's nonsens if dead_count is greater than 0. Because it means suicide. */
        if (dead_count > 0) {
            /* It's suicide. Suicide is not allowed */
            state = null;
            return false;
        }

        timeline.add(state);

        return true;
    }

    private void add_stone_to_link(BoardState state, GoControl.BoardPos pos)
    {
        int i;
        ArrayList links;
        ArrayList<Object> link_index = new ArrayList<Object>();
        ArrayList<GoControl.BoardPos> new_link, link;

        links = (pos.state == GoControl.BoardPos.BLACK_STONE)? state.black_links : state.white_links;

        for (i = 0 ; i < links.size() ; i++) {
            link = (ArrayList<GoControl.BoardPos>) links.get(i);

            if (isLinkable(link, pos)) {
                link_index.add(link);
            }
        }

        /* Make a link */
        if (link_index.size() == 0) {
            new_link = new ArrayList<GoControl.BoardPos>();
            new_link.add(pos);
            links.add(new_link);
            return;
        }

        /* Add to a exist link */
        if (link_index.size() == 1) {
            link = (ArrayList<GoControl.BoardPos>) link_index.get(0);
            link.add(pos);
            return;
        }

        /* Make new merged link */
        new_link = new ArrayList<GoControl.BoardPos>();

        for (i = 0 ; i < link_index.size() ; i++) {
            link = (ArrayList<GoControl.BoardPos>) link_index.get(i);

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

    private int remove_dead_stones(BoardState state, int color, int board_size, GoControl.BoardPos single_stone)
    {
        int i, j, dead_count = 0;
        ArrayList links;
        ArrayList<GoControl.BoardPos> link;

        links = (color== GoControl.BoardPos.BLACK_STONE)? state.black_links : state.white_links;

        for (i = 0 ; i < links.size() ; ) {
            link = (ArrayList<GoControl.BoardPos>)links.get(i);

            if (check_link_dead(state.stone_pos, link, board_size)) {
                dead_count += link.size();

                if (dead_count == 1) {
                    single_stone.x = ((GoControl.BoardPos) link.get(0)).x;
                    single_stone.y = ((GoControl.BoardPos) link.get(0)).y;
                }

                state.stone_pos.removeAll(link);
                links.remove(i);
                continue;
            }

            i++;
        }

        return dead_count;
    }

    private boolean check_link_dead(ArrayList<GoControl.BoardPos> stone_pos, ArrayList<GoControl.BoardPos> link, int board_size)
    {
        int i;

        /* To disallow duplication (It makes count easy)*/
        HashSet<GoControl.BoardPos> life_count = new HashSet<GoControl.BoardPos>();

        for (i = 0 ; i < link.size() ; i++) {
            GoControl.BoardPos pos = link.get(i);
            GoControl.BoardPos tmp;

            /* left */
            tmp = new GoControl.BoardPos(pos.x - 1, pos.y);
            if (tmp.x >= 0 && !stone_pos.contains(tmp)) {
                life_count.add(tmp);
            }

            /* right */
            tmp = new GoControl.BoardPos(pos.x + 1, pos.y);
            if (tmp.x < board_size && !stone_pos.contains(tmp))
                life_count.add(tmp);

            /* up */
            tmp = new GoControl.BoardPos(pos.x, pos.y - 1);
            if (tmp.y >= 0 && !stone_pos.contains(tmp))
                life_count.add(tmp);

            /* down */
            tmp = new GoControl.BoardPos(pos.x, pos.y + 1);
            if (tmp.y < board_size && !stone_pos.contains(tmp))
                life_count.add(tmp);
        }

        return (life_count.size()) > 0 ? false : true;
    }

    private int calc_stone_life(ArrayList<GoControl.BoardPos> stone_pos, GoControl.BoardPos pos, int board_size)
    {
        GoControl.BoardPos tmp;
        int life_count = 0;

        /* left */
        tmp = new GoControl.BoardPos(pos.x - 1, pos.y);
        if (tmp.x >= 0 && !stone_pos.contains(tmp)) {
            life_count++;
        }

            /* right */
        tmp = new GoControl.BoardPos(pos.x + 1, pos.y);
        if (tmp.x < board_size && !stone_pos.contains(tmp))
            life_count++;

            /* up */
        tmp = new GoControl.BoardPos(pos.x, pos.y - 1);
        if (tmp.y >= 0 && !stone_pos.contains(tmp))
            life_count++;

            /* down */
        tmp = new GoControl.BoardPos(pos.x, pos.y + 1);
        if (tmp.y < board_size && !stone_pos.contains(tmp))
            life_count++;

        return life_count;
    }

    private boolean isLinkable(ArrayList<GoControl.BoardPos> link, GoControl.BoardPos pos) {
        boolean result;

        /* up */
        if (link.contains(new GoControl.BoardPos(pos.x, pos.y - 1))) {
            return true;
        }

        /* down */
        if (link.contains(new GoControl.BoardPos(pos.x, pos.y + 1))) {
            return true;
        }

        /* left */
        if (link.contains(new GoControl.BoardPos(pos.x - 1, pos.y))) {
            return true;
        }

        /* right */
        if (link.contains(new GoControl.BoardPos(pos.x + 1, pos.y))) {
            return true;
        }

        return false;
    }
}
