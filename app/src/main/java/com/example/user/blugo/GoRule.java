package com.example.user.blugo;

import android.graphics.Point;
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

    protected class NewBoardState {
        /* May one dimensional array be better ? */
        public int [] pos;
        public int size = 19;
        public int ko_x = -1, ko_y = -1;
        public int white_dead = 0, black_dead = 0;

        NewBoardState(boolean clone) {
            if (clone)
                return;
        }

        NewBoardState() {
            this(19);
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            int i;
            /*Prevent duplicated BoardPos object generation*/
            NewBoardState state = new NewBoardState(true);

            /* swallow copy */
            //System.arraycopy(this.pos, 0, state.pos, 0, this.pos.length);
            state.pos = new int[size * size];
            System.arraycopy(this.pos, 0, state.pos, 0, this.pos.length);

            /*
            for (i = 0 ; i  < pos.length ;i ++) {
                state.pos[i] = (BoardPos) pos[i].clone();
            }
            */

            state.ko_x = ko_x;
            state.ko_y = ko_y;

            state.white_dead = white_dead;
            state.black_dead = black_dead;

            return state;
        }

        NewBoardState(int size) {
            this(size, -1, -1);
        }

        NewBoardState(int size, int ko_x, int ko_y) {
            pos = new int[size * size];
            int i;

            this.ko_x = ko_x;
            this.ko_y = ko_y;
        }

        boolean isEmpty(int x, int y)
        {
            if (x < 0 || x >= size)
                return false;

            if (y < 0 || y >= size)
                return false;

            return  (pos[x + y * size] & 0xFF) == BoardPosState.EMPTY.getValue();
        }

        private final int combine_to_int(int group_id, BoardPosState state)
        {
            return group_id << 8 | state.getValue();
        }

        private final int get_group_id(int value)
        {
            return (value >> 8) & 0xFFFF;
        }

        private final BoardPosState get_state(int value)
        {
            /* Enumeration's actual value cannot be zero*/
            return BoardPosState.valueOf(value & 0xFF);
        }

        boolean put_stone(GoControl.GoAction action)
        {
            BoardPos spot;
            int x = action.where.x, y = action.where.y;
            /* Because default ko_pos is -1, this value should be -2 */
            int dead_stone = 0, dead;
            Point ko_p = null;
            BoardPosState state;
            int group_id;


            /* 1. check if there are already a stone */
            if (!isEmpty(x, y))
                return false;

            /* Put a stone. Link surrounding stones as one group */
            state = (action.player == GoControl.Player.BLACK) ? BoardPosState.BLACK : BoardPosState.WHITE;
            group_id = get_grpid_new_stone(x, y, state);

            pos[x + y * size] = combine_to_int(group_id, state);

            /* remove opponent dead group */

            dead_stone += dead = try_kill_position(x - 1, y, state);
            if (dead == 1)
                ko_p = new Point(x - 1, y);
            dead_stone += dead = try_kill_position(x + 1, y, state);
            if (dead == 1)
                ko_p = new Point(x + 1, y);
            dead_stone += dead = try_kill_position(x, y - 1, state);
            if (dead == 1)
                ko_p = new Point(x, y - 1);
            dead_stone += dead = try_kill_position(x, y + 1, state);
            if (dead == 1)
                ko_p = new Point(x, y + 1);

            /* Is opponent's dead stone marked as KO */
            if (dead_stone == 1 && ko_p != null && ko_p.x == ko_x && ko_p.y == ko_y) {
                return false;
            }

            /* If we killed just one stone, mark current stone position as a ko */
            if (dead_stone == 1) {
                ko_x = x;
                ko_y = y;
            } else
                /* Else we should clear ko position */
                ko_x = ko_y = -1;

            /* check suicide */
            if (dead_stone == 0 && check_dead(x, y) == true)
                return false;

            if (action.player == GoControl.Player.BLACK)
                white_dead += dead_stone;
            else
                black_dead += dead_stone;

            return true;
        }

        private int get_grpid_new_stone(int x, int y, BoardPosState state)
        {
            int group_id  = 0;
            int gid_l, gid_r, gid_u, gid_d;
            boolean lc, rc, uc, dc;

            /*  Get minimum grp_id from surrounding stones */
            group_id = gid_l = left_grp_id(x, y, state);
            lc = (gid_l >= 1)? true : false;

            gid_r = right_grp_id(x, y, state);
            if ((group_id < 1) || (gid_r >=1 && gid_r < group_id))
                group_id = gid_r;
            rc = (gid_r >= 1)? true : false;

            gid_u = up_grp_id(x, y, state);
            if ((group_id < 1) || (gid_u >=1 && gid_u < group_id))
                group_id = gid_u;
            uc = (gid_u >= 1)? true : false;

            gid_d = down_grp_id(x, y, state);
            if ((group_id < 1) || (gid_d >= 1 && gid_d < group_id))
                group_id = gid_d;
            dc = (gid_d >= 1)? true : false;

            if (group_id < 1) {
                /* New unlinked single stone */
                group_id = get_next_grpid();
            } else {
                /*
                    minimum grp_id surrounding given stone has been found
                    Adjust surrounding stone's grp_id to minimum because it's now in same group
                */
                if (lc)
                    change_grp_id(gid_l, group_id);

                if (rc)
                    change_grp_id(gid_r, group_id);

                if (uc)
                    change_grp_id(gid_u, group_id);

                if (dc)
                    change_grp_id(gid_d, group_id);
            }

            return group_id;
        }

        private int get_next_grpid() {
            int grp_id = 0;
            int i, tmp;

            for (i = 0 ; i < pos.length ; i++) {
                tmp = get_group_id(pos[i]);
                if (tmp > grp_id)
                    grp_id = tmp;
            }

            /* now grp_id is max */

            return grp_id + 1;
        }

        private int change_grp_id(int from, int to) {
            int i, j;
            /* No target to change */
            if (from < 0)
                return to;

            /* Cannot change target to */
            if (to < 0)
                return from;

            /* No need to change */
            if (from == to)
                return to;

            /* need optimization */
            for (i = 0 ; i < pos.length ; i++) {
                if (get_group_id(pos[i]) == from)

                    pos[i] = combine_to_int(to, get_state(pos[i]));
            }

            return to;
        }

        private boolean check_dead(int x, int y)
        {
            int i;
            int group_id;
            group_id = get_group_id(pos[x + y * size]);

            /* need optimization */
            for (i = 0 ; i < pos.length ; i++) {
                    if (get_group_id(pos[i]) == group_id && calc_life_count(i % size, i / size) > 0) {
                        return false;
                    }
            }

            return true;
        }

        private int try_kill_position(int x, int y, BoardPosState color)
        {
            int group_id;
            int i, j;
            int dead_count = 0;

            if (x < 0 || x >= size)
                return 0;

            if (y < 0 || y >= size)
                return 0;

            if (get_state(pos[x + y * size]) == BoardPosState.EMPTY)
                return 0;

            if (get_state(pos[x + y * size]) == color)
                return 0;

            group_id = get_group_id(pos[x + y * size]);

            /* need optimization */
            for (i = 0 ; i < pos.length ; i++) {
                if (get_group_id(pos[i]) != group_id)
                    continue;

                if (calc_life_count(i % size, i / size) > 0)
                    return 0;

                dead_count++;
            }

            if (dead_count < 1)
                return dead_count;

            for (i = 0 ; i < pos.length ; i++) {
                if (get_group_id(pos[i]) != group_id)
                    continue;
                pos[i] = combine_to_int(0, BoardPosState.EMPTY);
            }

            return dead_count;
        }

        private int calc_life_count(int x, int y)
        {
            int count = 0;

            count += (isEmpty(x - 1, y))? 1 : 0;
            count += (isEmpty(x + 1, y))? 1 : 0;
            count += (isEmpty(x, y - 1))? 1 : 0;
            count += (isEmpty(x, y + 1))? 1 : 0;

            return count;
        }

        private int left_grp_id(int x, int y, BoardPosState color)
        {
            x--;
            if (x < 0)
                return 0;

            if (get_state(pos[x + y * size]) == color)
                return get_group_id(pos[x + y * size]);

            return 0;
        }

        private int right_grp_id(int x, int y, BoardPosState color)
        {
            x++;
            if (x >= size)
                return 0;

            if (get_state(pos[x + y * size]) == color)
                return get_group_id(pos[x + y * size]);

            return 0;
        }

        private int up_grp_id(int x, int y, BoardPosState color)
        {
            y--;
            if (y < 0)
                return 0;

            if (get_state(pos[x + y * size]) == color)
                return get_group_id(pos[x + y * size]);

            return 0;
        }

        private int down_grp_id(int x, int y, BoardPosState color)
        {
            y++;
            if (y >= size)
                return 0;

            if (get_state(pos[x + y * size]) == color)
                return get_group_id(pos[x + y * size]);

            return 0;
        }

        public HashSet<GoControl.GoAction> get_stones()
        {
            int i, j;
            BoardPosState cur_state;

            HashSet<GoControl.GoAction> stones = new HashSet<>();
            GoControl.GoAction action;

            /* need optimization */
            for (i = 0 ; i < pos.length ; i++) {
                cur_state = get_state(pos[i]);

                if (cur_state == BoardPosState.EMPTY) {
                    continue;
                }

                action = new  GoControl.GoAction(
                    cur_state  == BoardPosState.BLACK?
                        GoControl.Player.BLACK : GoControl.Player.WHITE,
                    i % size, i / size);
                stones.add(action);
            }

            return stones;
        }

        public ArrayList<BoardPos> get_calc_info() {
            ArrayList<BoardPos> result = new ArrayList<>();
            BoardPos bpos;
            int i;

            for (i = 0 ; i < pos.length ; i++) {
                bpos = new BoardPos();
                bpos.state = get_state(pos[i]);
                bpos.group_id = get_group_id(pos[i]);
                result.add(bpos);
            }

            return result;
        }

        public void cancel_calc()
        {
            int i, j;

            for (i = 0 ; i < pos.length ; i++) {
                switch (get_state(pos[i])) {
                    case EMPTY_NEUTRAL:
                    case EMPTY_BLACK:
                    case EMPTY_WHITE:
                        pos[i] = combine_to_int(get_group_id(pos[i]), BoardPosState.EMPTY);
                        break;

                    case BLACK_DEAD:
                        pos[i] = combine_to_int(get_group_id(pos[i]), BoardPosState.BLACK);
                        break;

                    case WHITE_DEAD:
                        pos[i] = combine_to_int(get_group_id(pos[i]), BoardPosState.WHITE);
                        break;
                }
            }
        }

        public void toggle_owner(int x, int y)
        {
            BoardPosState from, to;

            try {
                to = from = get_state(pos[x + y * size]);
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }

            switch (from) {
                case BLACK:
                    to = BoardPosState.BLACK_DEAD;
                    break;

                case WHITE:
                    to = BoardPosState.WHITE_DEAD;
                    break;

                case BLACK_DEAD:
                    to = BoardPosState.BLACK;
                    break;

                case WHITE_DEAD:
                    to = BoardPosState.WHITE;
                    break;

                default:
                    return;
            }

            HashSet<Point> empty_pos = new HashSet<>();
            flood_fill(x, y, from, to, empty_pos);

            AtomicInteger boarder_color = new AtomicInteger(0x00);
            HashSet<Point> vhistory = new HashSet<>();
            BoardPosState empty_from, empty_to;

            for (Point p : empty_pos) {
                empty_from = get_state(pos[p.x + p.y * size]);
                boarder_color.set(0x00);
                /* check border again */
                /*
                Because we don't have additional information to stop recursive function call,
                we provide information for already visited point as a function parameter.
                 */
                vhistory.clear();
                find_border(p.x, p.y, boarder_color,vhistory);

                if (boarder_color.get() == 0x01) {
                    flood_fill(p.x, p.y, empty_from, BoardPosState.EMPTY_WHITE);
                } else if (boarder_color.get() == 0x02) {
                    flood_fill(p.x, p.y, empty_from, BoardPosState.EMPTY_BLACK);
                } else
                    flood_fill(p.x, p.y, empty_from, BoardPosState.EMPTY_NEUTRAL);
            }

            /* Release memory */
            vhistory = null;
        }

        /* Do flood_fill and find surrounding empty points */
        private void flood_fill(int x, int y, BoardPosState from, BoardPosState to, HashSet<Point> empty_pos)
        {
            BoardPosState cur_state;

            /*We need to change it to non-recursive version*/
            if (x < 0 || x >= size)
                return;

            if (y < 0 || y >= size)
                return;

            cur_state = get_state(pos[x + y * size]);

            if (empty_pos != null) {
                switch (cur_state) {
                    case EMPTY:
                    case EMPTY_NEUTRAL:
                    case EMPTY_WHITE:
                    case EMPTY_BLACK:
                        empty_pos.add(new Point(x, y));
                        break;
                }
            }

            if (cur_state == to)
                return;

            if (cur_state != from)
                return;

            pos[x + y * size] = combine_to_int(get_group_id(pos[x + y * size]), to);

            /* south */
            flood_fill(x, y + 1, from, to, empty_pos);

            /* north */
            flood_fill(x, y - 1, from, to, empty_pos);

            /* west */
            flood_fill(x - 1, y, from, to, empty_pos);

            /* east */
            flood_fill(x + 1, y, from, to, empty_pos);
        }

        private void flood_fill(int x, int y, BoardPosState from, BoardPosState to)
        {
            /*We need to change it to non-recursive version*/
            if (x < 0 || x >= size)
                return;

            if (y < 0 || y >= size)
                return;

            if (get_state(pos[x + y * size]) == to)
                return;

            if (get_state(pos[x + y * size]) != from)
                return;

            pos[x + y * size] = combine_to_int(get_group_id(pos[x + y * size]), to);

            /* south */
            flood_fill(x, y + 1, from, to);

            /* north */
            flood_fill(x, y - 1, from, to);

            /* west */
            flood_fill(x - 1, y, from, to);

            /* east */
            flood_fill(x + 1, y, from, to);
        }

        private void find_border(int x, int y, AtomicInteger color, HashSet<Point> vhistory)
        {
            BoardPosState state;
            Point p;

            p = new Point(x, y);
            /* It's aready visited point.
            Don't do anything to prevent infinite recursion
             */
            if (vhistory.contains(p))
                return;

            vhistory.add(p);

            if (x < 0 || x >= size || y < 0 || y >= size)
                return;

            Log.d("SEARCH", "Searching: " + x + "," + y);

            state = get_state(pos[x + y * size]);

            switch (state) {
                case WHITE:
                case BLACK_DEAD:
                    color.set(color.get() | 0x01);
                    break;
                case BLACK:
                case WHITE_DEAD:
                    color.set(color.get() | 0x02);
                    break;
            }

            switch (state) {
                case EMPTY:
                case EMPTY_NEUTRAL:
                case EMPTY_WHITE:
                case EMPTY_BLACK:
                    break;

                default:
                    /* If it's not empty */
                    return;
            }

            /* Apply recursively for each direction of up, down, left, right */
            find_border(x - 1, y, color, vhistory);
            find_border(x + 1, y, color, vhistory);
            find_border(x, y - 1, color, vhistory);
            find_border(x, y + 1, color, vhistory);
        }

        private void find_border(int x, int y, int group_id, AtomicInteger color)
        {
            int cur_group_id;
            BoardPosState state;

            if (x < 0 || x >= size || y < 0 || y >= size)
                return;

            cur_group_id = get_group_id(pos[x + y * size]);

            if (cur_group_id == group_id)
                return;

            state = get_state(pos[x + y * size]);

            switch (state) {
                case WHITE:
                case BLACK_DEAD:
                    color.set(color.get() | 0x01);
                    break;
                case BLACK:
                case WHITE_DEAD:
                    color.set(color.get() | 0x02);
                    break;
            }

            switch (state) {
                case EMPTY:
                case EMPTY_NEUTRAL:
                case EMPTY_WHITE:
                case EMPTY_BLACK:
                    break;

                default:
                    /* If it's not empty */
                    return;
            }

            /* change group id */
            pos[x + y * size] = combine_to_int(group_id, get_state(pos[x + y * size]));

            /* Apply recursively for each direction of up, down, left, right */
            find_border(x - 1, y, group_id, color);
            find_border(x + 1, y, group_id, color);
            find_border(x, y - 1, group_id, color);
            find_border(x, y + 1, group_id, color);
        }

        /* Roughly determine owner of empty space */
        public void prepare_calc()
        {
            /* find empty space who's group is 0 */
            int i;
            /*
                0x00 : MET NOTHING -> UNDETERMINED.
                0x01 : MET ONLY WHITE -> Belongs to white territory
                0x02 : MET BLACK -> Belongs to black territory
                0x03 : MET BOTH -> UNDETERMINED.
                */
            AtomicInteger boarder_color = new AtomicInteger(0x00);

            for (i = 0 ; i < pos.length ; i++) {
                if (get_group_id(pos[i]) != 0)
                    continue;

                if (get_state(pos[i]) != BoardPosState.EMPTY)
                    continue;

                boarder_color.set(0x00);

                /* empty space will have respective group id */
                find_border(i % size, i / size, get_next_grpid(), boarder_color);

                if (boarder_color.get() == 0x01) {
                    flood_fill(i % size, i / size, get_state(pos[i]), BoardPosState.EMPTY_WHITE);
                } else if (boarder_color.get() == 0x02) {
                    flood_fill(i % size, i / size, get_state(pos[i]), BoardPosState.EMPTY_BLACK);
                }
            }
        }

        public void get_score(AtomicInteger white, AtomicInteger black)
        {
            int i, j;
            int white_count = 0, black_count = 0;

            for (i = 0 ; i < pos.length ; i++) {
                switch (get_state(pos[i])) {
                    case BLACK:
                    case WHITE:
                    case EMPTY:
                    case EMPTY_NEUTRAL:
                        break;

                    case EMPTY_BLACK:
                        black_count++;
                        break;

                    case EMPTY_WHITE:
                        white_count++;
                        break;

                    case BLACK_DEAD:
                        white_count+=2;
                        break;

                    case WHITE_DEAD:
                        black_count+=2;
                        break;
                }
            }

            white.set(white_count); black.set(black_count);
        }
    }

    protected class BoardPos {
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

    public abstract ArrayList<GoControl.GoAction> get_action_history();
    /*public abstract ArrayList<BoardState> getTimeline();*/
    public abstract boolean putStoneAt(int x, int y, GoControl.Player stone_color, GoControl.Player next_turn, int board_size);
    public abstract void toggle_owner(int x, int y);
    public abstract void pass(GoControl.Player next_turn);
    public abstract boolean undo();
    public abstract void cancel_calc();
    public abstract void prepare_calc();

    public abstract void get_dead(AtomicInteger white, AtomicInteger black);
    public abstract void get_score(AtomicInteger white, AtomicInteger black);
}
