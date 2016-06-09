package com.example.user.blugo;

import android.graphics.Point;

/**
 * Created by user on 2016-06-08.
 */
public class GoControlBluetooth extends GoControlSingle{
    private  Player my_turn;

    GoControlBluetooth(Player my_turn) {
        super();
        this.my_turn = my_turn;
    }

    private boolean _isMyTurn()
    {
        if (calc_mode())
            return true;

        return this.current_turn == my_turn;
    }

    @Override
    public synchronized boolean isMyTurn() {
        return _isMyTurn();
    }

    @Override
    public synchronized boolean putStoneAt(int x, int y, boolean pass) {
        boolean success;
        BlutoothCommThread comm = BlutoothCommThread.getInstance();

        success = super.putStoneAt(x, y, pass);

        if (success) {
            comm.write(BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.PUTSTONE,
                new Point(x, y)
                ));
        }

        return success;
    }

    public synchronized boolean op_putStoneAt(int x, int y) {

        return super.putStoneAt(x, y, false);
    }

    @Override
    public synchronized boolean pass() {
        BlutoothCommThread comm;

        if (!_isMyTurn())
            return false;

        if (!super.pass())
            return false;

        comm = BlutoothCommThread.getInstance();
        comm.write(BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.PASS,
            null
        ));

        return true;
    }

    public synchronized void op_pass() {
        super.pass();
    }

    @Override
    public synchronized void undo() {
        /* super.undo(); */
    }
}
