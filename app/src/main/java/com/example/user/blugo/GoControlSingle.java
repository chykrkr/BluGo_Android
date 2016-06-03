package com.example.user.blugo;

import java.util.ArrayList;

/**
 * Created by user on 2016-06-02.
 */
public class GoControlSingle extends GoControl {
    private int board_size = 19;
    private Player current_turn = Player.BLACK;
    private GoRule rule;
    private float komi = 6.5f;

    GoControlSingle() {
        this(19, Player.BLACK, null, new GoRuleJapan());
    }

    GoControlSingle(Callback callback_receiver) {
        this(19, Player.BLACK, callback_receiver, new GoRuleJapan());
    }

    GoControlSingle(int board_size, Player current_turn, Callback callback_receiver, GoRule rule) {
        this.board_size = board_size;
        this.current_turn = current_turn;
        this.callback_receiver = callback_receiver;
        this.rule = rule;
    }

    @Override
    public synchronized boolean isMyTurn() {
        return true;
    }

    @Override
    public synchronized ArrayList<GoAction> getStone_pos() {
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
        /* put stone according to specified RULE */
        if (rule.putStoneAt(x, y, current_turn, next_turn, board_size) == false)
            return false;

	current_turn = next_turn;

        if (callback_receiver != null)
            callback_receiver.callback_board_state_changed();

        return true;
    }

    @Override
    public String get_sgf() {
        ArrayList<GoAction> actions = rule.get_action_history();
        int i;

        String sgf_string = "(;GM[1]FF[4]CA[UTF-8]\n";
        sgf_string += String.format("SZ[%d]HA[0]KM[%.1f]\n\n", board_size, komi);

        for (i = 0 ; i < actions.size() ; i++) {
            sgf_string += actions.get(i).get_sgf_string() + "\n";
        }
        sgf_string += "\n)";

        return sgf_string;
    }

    @Override
    public synchronized void pass() {
        Player next_turn = (current_turn == Player.WHITE)? Player.BLACK : Player.WHITE;

        current_turn = next_turn;
        rule.pass(next_turn);

        if (callback_receiver != null)
            callback_receiver.callback_board_state_changed();
    }

    @Override
    public synchronized void undo()
    {
	ArrayList<GoRule.BoardState> timeline;
	GoRule.BoardState state;

	if (!this.rule.undo()) {
	    return;
	}

	timeline = rule.getTimeline();
	state = timeline.get(timeline.size() - 1);
	current_turn = state.next_turn;

	this.callback_receiver.callback_board_state_changed();
    }

    @Override
    public synchronized void new_game() {
        this.current_turn = Player.BLACK;
        this.rule = null;
        this.rule = new GoRuleJapan();
        this.callback_receiver.callback_board_state_changed();
    }

    public synchronized void load_game(String sgf_string) {
        this.callback_receiver.callback_board_state_changed();
    }
}
