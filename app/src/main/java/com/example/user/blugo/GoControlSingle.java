package com.example.user.blugo;

import java.util.ArrayList;

/**
 * Created by user on 2016-06-02.
 */
public class GoControlSingle extends GoControl {
    private int board_size = 19;
    private int current_turn = BoardPos.BLACK_STONE;
    private GoRule rule;

    GoControlSingle() {
        this(19, BoardPos.BLACK_STONE, null, new GoRuleJapan());
    }

    GoControlSingle(Callback callback_receiver) {
        this(19, BoardPos.BLACK_STONE, callback_receiver, new GoRuleJapan());
    }

    GoControlSingle(int board_size, int current_turn, Callback callback_receiver, GoRule rule) {
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
    public synchronized ArrayList<BoardPos> getStone_pos() {
        return rule.get_stones();
    }

    @Override
    public synchronized int getBoardSize() {
        return board_size;
    }

    @Override
    public synchronized int getCurrent_turn() {
        return current_turn;
    }

    @Override
    public synchronized boolean putStoneAt(int x, int y) {
        /* put stone according to specified RULE */
        if (rule.putStoneAt(x, y, current_turn, board_size) == false)
            return false;

        toggle_turn();

        if (callback_receiver != null)
            callback_receiver.callback_board_state_changed();

        return true;
    }

    @Override
    public synchronized void new_game() {
        this.current_turn = BoardPos.BLACK_STONE;
        this.rule = null;
        this.rule = new GoRuleJapan();
        this.callback_receiver.callback_board_state_changed();
    }

    private void toggle_turn()
    {
        current_turn = (current_turn == BoardPos.BLACK_STONE)? BoardPos.WHITE_STONE : BoardPos.BLACK_STONE;
    }
}
