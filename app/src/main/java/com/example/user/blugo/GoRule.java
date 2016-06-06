package com.example.user.blugo;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by user on 2016-06-02.
 */
public abstract class GoRule {
    public enum BoardPosState {
        BLACK,
        WHITE,
        EMPTY,
        BLACK_DEAD,
        WHITE_DEAD,
        EMPTY_NEUTRAL,
        EMPTY_BLACK,
        EMPTY_WHITE,
    }

    protected class NewBoardState {
        /* May one dimensional array be better ? */
        public BoardPos [][] pos;
        public int size = 19;
        public int ko_x = -1, ko_y = -1;
        public int white_dead = 0, black_dead = 0;

        NewBoardState() {
            this(19);
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            NewBoardState state = new NewBoardState(this.size);
            int i, j;

            for (i = 0 ; i < pos.length ; i++) {
                for (j = 0 ; j < pos[i].length ; j++) {
                    state.pos[i][j] = (BoardPos) pos[i][j].clone();
                }
            }

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
            pos = new BoardPos[size][size];
            int i, j;

            for (i = 0 ; i < pos.length ; i++) {
                for (j = 0 ; j < pos[i].length ; j++) {
                    pos[i][j] = new BoardPos();
                }
            }

            this.ko_x = ko_x;
            this.ko_y = ko_y;
        }

        boolean isEmpty(int x, int y)
        {
            if (x < 0 || x >= size)
                return false;

            if (y < 0 || y >= size)
                return false;

            return pos[x][y].state == BoardPosState.EMPTY;
        }

        boolean put_stone(GoControl.GoAction action)
        {
            BoardPos spot;
            int x = action.where.x, y = action.where.y;
            /* Because default ko_pos is -1, this value should be -2 */
            int dead_stone = 0, dead;
            Point ko_p = null;


            /* 1. check if there are already a stone */
            if (!isEmpty(x, y))
                return false;

            /* Put a stone. Link surrounding stones as one group */
            spot = new BoardPos();
            spot.state = (action.player == GoControl.Player.BLACK) ? BoardPosState.BLACK : BoardPosState.WHITE;
            spot.group_id = get_grpid_new_stone(x, y, spot);

            pos[x][y] = spot;

            /* remove opponent dead group */

            dead_stone += dead = try_kill_position(x - 1, y, spot.state);
            if (dead == 1)
                ko_p = new Point(x - 1, y);
            dead_stone += dead = try_kill_position(x + 1, y, spot.state);
            if (dead == 1)
                ko_p = new Point(x + 1, y);
            dead_stone += dead = try_kill_position(x, y - 1, spot.state);
            if (dead == 1)
                ko_p = new Point(x, y - 1);
            dead_stone += dead = try_kill_position(x, y + 1, spot.state);
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

        private int get_grpid_new_stone(int x, int y, BoardPos spot)
        {
            int group_id  = -1;
            int gid_l, gid_r, gid_u, gid_d;
            boolean lc, rc, uc, dc;

            /*  Get minimum grp_id from surrounding stones */
            group_id = gid_l = left_grp_id(x, y, spot.state);
            lc = (gid_l >= 0)? true : false;

            gid_r = right_grp_id(x, y, spot.state);
            if ((group_id < 0) || (gid_r >=0 && gid_r < group_id))
                group_id = gid_r;
            rc = (gid_r >= 0)? true : false;

            gid_u = up_grp_id(x, y, spot.state);
            if ((group_id < 0) || (gid_u >=0 && gid_u < group_id))
                group_id = gid_u;
            uc = (gid_u >= 0)? true : false;

            gid_d = down_grp_id(x, y, spot.state);
            if ((group_id < 0) || (gid_d >=0 && gid_d < group_id))
                group_id = gid_d;
            dc = (gid_d >= 0)? true : false;

            if (group_id < 0) {
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
            int grp_id = -1;
            int i, j;

            for (i = 0 ; i < pos.length ; i++) {
                for (j = 0 ; j < pos[i].length ; j++) {
                    if (pos[i][j].group_id > grp_id)
                        grp_id = pos[i][j].group_id;
                }
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
                for (j = 0 ; j < pos[i].length ; j++) {
                    if (pos[i][j].group_id == from)
                        pos[i][j].group_id = to;
                }
            }

            return to;
        }

        private boolean check_dead(int x, int y)
        {
            int i, j;
            int group_id;
            group_id = pos[x][y].group_id;

            /* need optimization */
            for (i = 0 ; i < pos.length ; i++) {
                for (j = 0 ; j < pos[i].length ; j++) {
                    if (pos[i][j].group_id == group_id && calc_life_count(i, j) > 0) {
                        return false;
                    }
                }
            }

            return true;
        }

        private int try_kill_position(int x, int y, BoardPosState color)
        {
            int group_id;
            int i, j;
            ArrayList<BoardPos> deadpos = new ArrayList<>();


            if (x < 0 || x >= size)
                return 0;

            if (y < 0 || y >= size)
                return 0;

            if (pos[x][y].state == BoardPosState.EMPTY)
                return 0;

            if (pos[x][y].state == color)
                return 0;

            group_id = pos[x][y].group_id;

            /* need optimization */
            for (i = 0 ; i < pos.length ; i++) {
                for (j = 0 ; j < pos[i].length ; j++) {
                    if (pos[i][j].group_id != group_id)
                        continue;

                    if (calc_life_count(i, j) > 0)
                        return 0;

                    deadpos.add(pos[i][j]);
                }
            }

            for (BoardPos p : deadpos) {
                p.group_id = -1;
                p.state = BoardPosState.EMPTY;
            }

            return deadpos.size();
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
                return -1;

            if (pos[x][y].state == color)
                return pos[x][y].group_id;

            return -1;
        }

        private int right_grp_id(int x, int y, BoardPosState color)
        {
            x++;
            if (x >= size)
                return -1;

            if (pos[x][y].state == color)
                return pos[x][y].group_id;

            return -1;
        }

        private int up_grp_id(int x, int y, BoardPosState color)
        {
            y--;
            if (y < 0)
                return -1;

            if (pos[x][y].state == color)
                return pos[x][y].group_id;

            return -1;
        }

        private int down_grp_id(int x, int y, BoardPosState color)
        {
            y++;
            if (y >= size)
                return -1;

            if (pos[x][y].state == color)
                return pos[x][y].group_id;

            return -1;
        }

        public HashSet<GoControl.GoAction> get_stones()
        {
            int i, j;

            HashSet<GoControl.GoAction> stones = new HashSet<>();
            GoControl.GoAction action;

            /* need optimization */
            for (i = 0 ; i < pos.length ; i++) {
                for (j = 0 ; j < pos[i].length ; j++) {
                    if (pos[i][j].state == BoardPosState.EMPTY) {
                        continue;
                    }

                    action = new  GoControl.GoAction(
                        pos[i][j].state  == BoardPosState.BLACK?
                            GoControl.Player.BLACK : GoControl.Player.WHITE,
                        i, j);
                    stones.add(action);
                }
            }

            return stones;
        }

        public ArrayList<BoardPos> get_calc_info() {
            int i;
            ArrayList<BoardPos> info = new ArrayList<>();

            for (i = 0 ; i < pos.length ; i++) {
                info.addAll(new ArrayList<BoardPos>(Arrays.asList(pos[i])));
            }

            return info;
        }

        public void cancel_calc()
        {
            int i, j;

            for (i = 0 ; i < pos.length ; i++) {
                for (j = 0 ; j < pos[i].length ; j++) {
                    switch (pos[i][j].state) {
                        case EMPTY_NEUTRAL:
                        case EMPTY_BLACK:
                        case EMPTY_WHITE:
                            pos[i][j].state = BoardPosState.EMPTY;
                            break;

                        case BLACK_DEAD:
                            pos[i][j].state = BoardPosState.BLACK;
                            break;

                        case WHITE_DEAD:
                            pos[i][j].state = BoardPosState.WHITE;
                            break;
                    }
                }
            }
        }

        public void toggle_owner(int x, int y)
        {
            BoardPosState from, to;

            try {
                to = from = pos[x][y].state;
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

                case EMPTY:
                case EMPTY_NEUTRAL:
                    to = BoardPosState.EMPTY_BLACK;
                    break;

                case EMPTY_BLACK:
                    to = BoardPosState.EMPTY_WHITE;
                    break;

                case EMPTY_WHITE:
                    to = BoardPosState.EMPTY_NEUTRAL;
                    break;

                case BLACK_DEAD:
                    to = BoardPosState.BLACK;
                    break;

                case WHITE_DEAD:
                    to = BoardPosState.WHITE;
                    break;
            }

            flood_fill(x, y, from, to);
        }

        private void flood_fill(int x, int y, BoardPosState from, BoardPosState to)
        {
            if (x < 0 || x >= size)
                return;

            if (y < 0 || y >= size)
                return;

            if (pos[x][y].state == to)
                return;

            if (pos[x][y].state != from)
                return;

            pos[x][y].state = to;

            /* south */
            flood_fill(x, y + 1, from, to);

            /* north */
            flood_fill(x, y - 1, from, to);

            /* west */
            flood_fill(x - 1, y, from, to);

            /* east */
            flood_fill(x + 1, y, from, to);
        }

        public void get_score(AtomicInteger white, AtomicInteger black)
        {
            int i, j;
            int white_count = 0, black_count = 0;

            for (i = 0 ; i < pos.length ; i++) {
                for (j = 0 ; j < pos[i].length ; j++) {
                    switch (pos[i][j].state) {
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
            }

            white.set(white_count); black.set(black_count);
        }
    }

    protected class BoardPos {
        BoardPosState state = BoardPosState.EMPTY;
        int group_id = -1;

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

    public abstract void get_dead(AtomicInteger white, AtomicInteger black);
    public abstract void get_score(AtomicInteger white, AtomicInteger black);
}
