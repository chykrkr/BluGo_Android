package com.example.user.blugo;

import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothGameActivity extends AppCompatActivity implements Handler.Callback,
    GoBoardViewListener, GoMessageListener {
    private GoBoardView gv;
    private TextView txt_info;
    private GoControlBluetooth game;

    private String get_info_text() {
        String str, result;
        GoControl.GoInfo info =  game.get_info();
        float score_diff;
        float white_final, black_final;

        if (game.calc_mode()) {
            white_final = info.white_score + info.black_dead + info.komi;
            black_final = info.black_score + info.white_dead;
            score_diff = white_final - black_final;

            if (score_diff == 0) {
                result = "DRAW";
            } else if (score_diff > 0) {
                result = String.format("W+%.1f", score_diff);
            } else {
                result = String.format("B+%.1f", Math.abs(score_diff));
            }

            str = String.format("ws: %.1f, bs: %d, %s", white_final, (int) black_final, result);
        } else {
            str = String.format("%s(%d), wd: %d, bd: %d",
                info.turn == GoControl.Player.WHITE? "W" : "B",
                info.turn_num,
                info.white_dead, info.black_dead);
        }

        return str;
    }

    private Handler msg_handler = new Handler(this);

    private BlutoothCommThread comm_thread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_game);

        Bundle bundle;
        Intent intent = getIntent();
        bundle = intent.getExtras();

        GoPlaySetting setting = bundle.getParcelable(GoMessageListener.GAME_SETTING_MESSAGE);

        int bw = setting.wb;
        Log.d("TEST", "bw: " + bw);

        game = new GoControlBluetooth(bw == 0? GoControl.Player.BLACK : GoControl.Player.WHITE);

        gv = (GoBoardView) findViewById(R.id.go_board_view);
        gv.setGo_control(game);
        gv.setFocusable(true);

        txt_info = (TextView) findViewById(R.id.text_info);
        txt_info.setText(get_info_text());

        comm_thread = BlutoothCommThread.getInstance();
        if (comm_thread != null) {
            comm_thread.changeListener(this);
        }
    }

    private void stop_server_client()
    {
        BlutoothServerThread server;
        BlutoothClientThread client;

        /* stop communicator */
        BlutoothCommThread comm;
        comm = BlutoothCommThread.getInstance();
        if (comm != null) {
            comm.cancel();
            try {
                comm.join();
            } catch (InterruptedException e) {}
        }

        /* stop server */
        server = BlutoothServerThread.getInstance();
        if (server != null) {
            server.cancel();
            try {
                server.join();
            } catch (InterruptedException e) {}
        }

        /* stop client */
        client = BlutoothClientThread.getInstance();
        if (client != null) {
            client.cancel();
            try {
                client.join();
            } catch (InterruptedException e) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop_server_client();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case GoBoardViewListener.MSG_VIEW_FULLY_DRAWN:
                txt_info.setText(get_info_text());
                return true;

            case GoMessageListener.BLUTOOTH_COMM_ERROR:
                stop_server_client();
                Toast.makeText(this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                return true;

            case GoMessageListener.BLUTOOTH_COMM_MSG:
                BlutoothMsgParser.MsgParsed parsed = (BlutoothMsgParser.MsgParsed) msg.obj;
                handle_comm_message(parsed);
                return true;
        }

        return false;
    }

    public void pass(View view)
    {
        game.pass();
    }

    private void handle_comm_message(BlutoothMsgParser.MsgParsed msg)
    {
        String m;
        Point p;
        switch (msg.type) {
            case PUTSTONE:
                p = (Point) msg.content;
                game.op_putStoneAt(p.x, p.y);
                break;

            case PASS:
                game.op_pass();
                break;
        }
    }

    @Override
    public Handler get_msg_handler() {
        return msg_handler;
    }
}
