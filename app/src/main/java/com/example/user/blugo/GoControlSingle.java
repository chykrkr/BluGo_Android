package com.example.user.blugo;

import android.graphics.Point;
import android.util.Log;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by user on 2016-06-02.
 */
public class GoControlSingle extends GoControl {
    private int board_size = 19;
    protected Player current_turn = Player.BLACK;
    protected GoRule rule;
    protected float komi = 6.5f;
    private int pass_count = 0;
    private static final int MAX_PASS_COUNT = 2;
    private int start_turn = 0;
    /*
    null : Result not determined.
    Positive : W won by this value
    Negative : B won by this value
    0 : draw
     */
    protected Float final_score_diff = null;
    /*
    -1 : No one resigned
    0 : white resigned
    1 : black resigned
     */
    protected Player resigned = null;

    GoControlSingle() {
        this(19, Player.BLACK, null, new GoRuleJapan(19),0);
    }

    GoControlSingle(Callback callback_receiver) {
        this(19, Player.BLACK, callback_receiver, new GoRuleJapan(19), 0);
    }

    GoControlSingle(int board_size, Player current_turn, GoRule rule) {
        this(board_size, current_turn, null, rule, 0);
    }

    GoControlSingle(int board_size, Player current_turn, GoRule rule, int start_turn) {
        this(board_size, current_turn, null, rule, start_turn);
    }

    GoControlSingle(int board_size, Player current_turn, Callback callback_receiver, GoRule rule, int start_turn) {
        this.board_size = board_size;
        this.current_turn = current_turn;
        this.callback_receiver = callback_receiver;
        this.rule = rule;
        this.start_turn = start_turn;
    }

    private boolean _isMyTurn() {
        if (resigned != null)
            return false;

        return true;
    }

    @Override
    public synchronized boolean isMyTurn() {
        return _isMyTurn();
    }

    @Override
    public synchronized HashSet<GoAction> getStone_pos() {
        return rule.get_stones();
    }

    @Override
    public synchronized int getBoardSize() {
        return board_size;
    }

    @Override
    public synchronized Player getCurrent_turn() {
        return current_turn;
    }

    @Override
    public synchronized boolean putStoneAt(int x, int y, boolean pass) {
        Player next_turn = (current_turn == Player.WHITE)? Player.BLACK : Player.WHITE;

        if (this.calc_mode()) {
            rule.toggle_owner(x, y);

            if (callback_receiver != null)
                callback_receiver.callback_board_state_changed();

            return true;
        }
        /* put stone according to specified RULE */
        if (rule.putStoneAt(x, y, current_turn, next_turn, board_size) == false)
            return false;

        pass_count = 0;

	current_turn = next_turn;

        if (callback_receiver != null)
            callback_receiver.callback_board_state_changed();

        return true;
    }

    private String get_sgf_from_calc_info()
    {
        int i, x, y;
        ArrayList<GoRule.BoardPos> info = rule.get_calc_info();

        String tw = "";
        String tb = "";

        for (i = 0 ; i < info.size() ; i++) {
            GoRule.BoardPos cinfo = info.get(i);

            switch (cinfo.state) {
                case WHITE:
                case BLACK:
                case EMPTY:
                case EMPTY_NEUTRAL:
                    continue;

                case WHITE_DEAD:
                case EMPTY_BLACK:
                    /* black territory */
                    x = i % board_size;
                    y = i / board_size;

                    if (tb.length() < 1)
                        tb = "TB";

                    tb += "[";
                    tb += (char)(x + (int)('a'));
                    tb += (char)(y + (int)('a'));
                    tb += "]";
                    break;

                case BLACK_DEAD:
                case EMPTY_WHITE:
                    /* white territory */
                    x = i % board_size;
                    y = i / board_size;

                    if (tw.length() < 1)
                        tw = "TW";

                    tw += "[";
                    tw += (char)(x + (int)('a'));
                    tw += (char)(y + (int)('a'));
                    tw += "]";
                    break;
            }
        }

        return tw + tb;
    }

    @Override
    public String get_sgf() {
        ArrayList<GoAction> actions = rule.get_action_history();
        int i;

        String sgf_string = "(;GM[1]FF[4]CA[UTF-8]\n";
        sgf_string += String.format("SZ[%d]HA[0]KM[%.1f]", board_size, komi);
        sgf_string += this.rule.get_rule_id().get_sgf_string();

        if (resigned != null) {
            sgf_string += "RE[";
            sgf_string += (resigned == Player.BLACK)? "W+R" : "B+R";
            sgf_string += "]";
        } else if (calc_mode()) {
            GoInfo info = get_info();

            sgf_string += "RE[";

            if (info.score_diff == 0) {
                sgf_string += "Draw";
            } else {
                sgf_string += String.format("%c+%.1f",
                    (info.score_diff > 0) ? 'W' : 'B',
                    Math.abs(info.score_diff));
            }
            sgf_string += "]";
        }

        sgf_string += "\n\n";

        for (i = 0 ; i < actions.size() ; i++) {
            sgf_string += actions.get(i).get_sgf_string() + "\n";
        }

        if (calc_mode()) {
            String calc_info = get_sgf_from_calc_info();
            sgf_string += "\n" + calc_info;
        }

        sgf_string += "\n)";

        return sgf_string;
    }

    @Override
    public synchronized boolean load_sgf(String text) {
        ArrayList<SgfParser.ParsedItem> result;
        ArrayList<Point> territory_black = new ArrayList<>();
        ArrayList<Point> territory_white = new ArrayList<>();
        SgfParser parser = new SgfParser();
        Point p;
        GoRule.RuleID rule_id = GoRule.RuleID.JAPANESE;

        pass_count = 0;

        Log.d("PARS", "SGF parsing started");
        result = parser.parse(text);
        Log.d("PARS", "SGF parsing ended");

        this.current_turn = Player.BLACK;
        this.rule = null;

        /* Two phase. we should get board size first */

        /* default board size if we cannot found board size information */
        this.board_size = 19;
        for (SgfParser.ParsedItem item : result) {
            switch (item.type) {
                case BOARD_SIZE:
                    Integer size = (Integer) item.content;
                    this.board_size = size;
                    break;

                case RULE:
                    rule_id = (GoRule.RuleID) item.content;
                    break;
            }
        }

        switch (rule_id) {
            case JAPANESE:
                this.rule = new GoRuleJapan(this.board_size);
                break;

            case CHINESE:
                this.rule = new GoRuleChinese(this.board_size);
                break;

            default:
                this.rule = new GoRuleJapan(this.board_size);
                break;
        }


        for (SgfParser.ParsedItem item : result) {
            switch (item.type) {
                /*
                //We already processed board size.
                case BOARD_SIZE:
                    Integer size = (Integer) item.content;
                    this.board_size = size;
                    break;
                    */

                case KOMI:
                    this.komi = (Float) item.content;
                    break;

                case WHITE_PUT:
                    p = (Point) item.content;
                    rule.putStoneAt(p.x, p.y, Player.WHITE, Player.BLACK, board_size);

                    current_turn = Player.BLACK;
                    break;

                case WHITE_PASS:
                    current_turn = Player.BLACK;
                    rule.pass(Player.BLACK);
                    break;

                case BLACK_PUT:
                    p = (Point) item.content;
                    rule.putStoneAt(p.x, p.y, Player.BLACK, Player.WHITE, board_size);

                    current_turn = Player.WHITE;
                    break;

                case BLACK_PASS:
                    current_turn = Player.WHITE;
                    rule.pass(Player.WHITE);
                    break;

                case TERRITORY_WHITE:
                    territory_white.add((Point) item.content);
                    break;

                case TERRITORY_BLACK:
                    territory_black.add((Point) item.content);
                    break;
            }
        }

        /* Final territory information is available */
        if (territory_black.size() > 0 || territory_white.size() > 0) {
            ArrayList<NewBoardState> time_line = rule.get_time_line();
            NewBoardState last = null;
            try {
                last = (NewBoardState) time_line.get(time_line.size() - 1).clone();
            } catch (CloneNotSupportedException e) {}

            for(Point each_p : territory_black) {
                last.mark_territory(each_p.x, each_p.y, 0);
            }

            for(Point each_p : territory_white) {
                last.mark_territory(each_p.x, each_p.y, 1);
            }

            time_line.add(last);
        }

        Log.d("PARS", "Game data generation completed");
        //this.callback_receiver.callback_board_state_changed();
        Log.d("PARS", "Draw done");

        return true;
    }

    @Override
    public synchronized boolean pass() {
        int pass;

        if (calc_mode()) {
            return false;
        }

        if (!_isMyTurn()) {
            return false;
        }

        Player next_turn = (current_turn == Player.WHITE)? Player.BLACK : Player.WHITE;

        current_turn = next_turn;
        rule.pass(next_turn);

        pass_count++;

        /* prepare calc territory */
        if (calc_mode()) {
            rule.prepare_calc();
        }

        if (callback_receiver != null)
            callback_receiver.callback_board_state_changed();

        return true;
    }

    @Override
    public synchronized boolean undo()
    {
        if (resigned != null) {
            resigned = null;
            this.callback_receiver.callback_board_state_changed();
            return true;
        }

        if (calc_mode()) {
            rule.cancel_calc();
        }

        ArrayList<GoAction> history = rule.get_action_history();
        if (history.size() < 1)
            return false;

        GoAction last_action;
        last_action = history.get(history.size() - 1);

	if (!this.rule.undo()) {
	    return false;
	}

        if (last_action.action == Action.PASS && pass_count > 0) {
            pass_count--;
        }

        current_turn = last_action.player;

        this.callback_receiver.callback_board_state_changed();
        return  true;
    }

    @Override
    public GoInfo get_info() {
        GoInfo info = new GoInfo();

        info.turn = this.current_turn;
        info.komi = this.komi;

        if (calc_mode()) {
            rule.get_score(info);
        } else {
            rule.get_dead(info);
        }

        ArrayList<GoAction> history = rule.get_action_history();
        info.turn_num = history.size() + 1 + start_turn;

        info.resigned = this.resigned;

        return info;
    }

    @Override
    public GoRule.RuleID get_rule() {
        if (this.rule == null)
            return null;

        return this.rule.get_rule_id();
    }

    @Override
    public GoPlaySetting get_game_setting() {
        GoPlaySetting setting = new GoPlaySetting();

        setting.rule = this.get_rule().getValue();
        setting.komi = this.komi;
        setting.size = this.board_size;
        // setting.wb;
        // setting.handicap;

        return setting;
    }

    @Override
    public synchronized void new_game() {
        this.current_turn = Player.BLACK;
        this.rule = null;
        this.rule = new GoRuleJapan(board_size);
        this.callback_receiver.callback_board_state_changed();
    }

    public synchronized void load_game(String sgf_string) {
        this.callback_receiver.callback_board_state_changed();
    }

    public boolean calc_mode()
    {
        if (pass_count >= MAX_PASS_COUNT)
            return true;

        return false;
    }

    @Override
    public Point get_cur_coord()
    {
        return null;
    }

    @Override
    public ArrayList<GoRule.BoardPos> get_calc_info() {
        return rule.get_calc_info();
    }

    @Override
    public synchronized void resign() {
        this.resigned = current_turn;
    }

    @Override
    public Player is_resigned() {
        return resigned;
    }
}
