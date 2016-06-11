package com.example.user.blugo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;

public class ReviewGameActivity extends AppCompatActivity implements Handler.Callback, GoBoardViewListener, SeekBar.OnSeekBarChangeListener{
    public Handler msg_handler = new Handler(this);
    public final static int MSG_LOAD_END = GoBoardViewListener.MSG_MAX + 1;
    public final static int MSG_LOAD_FAIL = MSG_LOAD_END + 1;
    public final static String MSG_BOARD_STATE = "com.example.user.blugo.ReviewGameActivity.MSG_BOARD_STATE";
    public final static String MSG_CURRENT_TURN = "com.example.user.blugo.ReviewGameActivity.MSG_CURRENT_TURN";
    public final static String MSG_START_TURNNO = "com.example.user.blugo.ReviewGameActivity.MSG_START_TURNNO";
    private GoBoardView gv;
    private SeekBar sbar;
    private TextView text_pos;
    private GoControlReview game = new GoControlReview();
    private ProgressDialog load_progress;
    private String sgf_path;
    private boolean need_to_load = false;
    private boolean loading_finished = false;

    private Button button, btn_detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_game);

	Intent intent = getIntent();
        sgf_path = intent.getStringExtra(FrontDoorActivity.EXTRA_MESSAGE);

        gv = (GoBoardView) findViewById(R.id.go_board_view);
        gv.setGo_control(game);
        gv.setView_only_mode(true);
        gv.setFocusable(true);

        sbar = (SeekBar) findViewById(R.id.seek_pos);
        sbar.setOnSeekBarChangeListener(this);

        text_pos = (TextView) findViewById(R.id.text_pos);

        need_to_load = true;
        loading_finished = false;

        button = (Button) findViewById(R.id.btn_variation);
        btn_detail = (Button) findViewById(R.id.btn_detail);
    }

    private void load_sgf()
    {
        load_progress = new ProgressDialog(this);
        load_progress.setCancelable(true);
        load_progress.setMessage("Loading SGF ...");
        load_progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        load_progress.setProgress(0);
        load_progress.setMax(100);
        load_progress.show();

        new Thread(new Runnable() {
            public void run() {
                Message msg;
                FileInputStream is;
                byte [] buffer = new byte[512];
                int read;
                String tmp;

                String sgf_string = new String();

                try {
                    is = new FileInputStream(sgf_path);

                    while (true) {
                        read = is.read(buffer, 0, buffer.length);

                        if (read > 0) {
                            tmp = new String(buffer, 0, read, "UTF-8");
                            sgf_string += tmp;
                        } else
                            break;
                    }
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg = Message.obtain(ReviewGameActivity.this.msg_handler, MSG_LOAD_FAIL, "msg");
                    ReviewGameActivity.this.msg_handler.sendMessage(msg);
                    return;
                }

                game.load_sgf(sgf_string);
                msg = Message.obtain(ReviewGameActivity.this.msg_handler, MSG_LOAD_END, "msg");
                ReviewGameActivity.this.msg_handler.sendMessage(msg);
            }
        }).start();
    }

    private void set_button_enables()
    {
        if (game.calc_mode()) {
            button.setEnabled(false);
        } else {
            button.setEnabled(true);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        String tmp;
        switch (msg.what) {
            case MSG_LOAD_END:
		gv.invalidate();
                sbar.setMax(game.get_last_pos());
                sbar.setProgress(game.getCur_pos());
                tmp = String.format("%d/%d", game.getCur_pos(), this.sbar.getMax());
                text_pos.setText(tmp);
                loading_finished = true;

                set_button_enables();

                load_progress.dismiss();

                return true;

            case GoBoardViewListener.MSG_VIEW_FULLY_DRAWN:
                if (need_to_load) {
                    Log.d("DEBUG", "TEST");
                    load_sgf();
                    need_to_load = !need_to_load;
                }
                return true;
        }
        return false;
    }

    @Override
    public Handler get_msg_handler() {
        return msg_handler;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String text;

        if (!loading_finished)
            return;

        text = String.format("%d/%d", progress, this.sbar.getMax());
        text_pos.setText(text);
        game.goto_pos(progress);

        set_button_enables();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void try_varation(View view)
    {
        Intent intent = new Intent(this, GoBoardActivity.class);
        Bundle bundle = new Bundle();

        bundle.putParcelable(MSG_BOARD_STATE, game.get_current_board_state());
        bundle.putInt(MSG_CURRENT_TURN, game.getCurrent_turn() == GoControl.Player.BLACK ? 0 : 1);
        bundle.putInt(MSG_START_TURNNO, game.getCur_pos());
        intent.putExtras(bundle);

        startActivity(intent);
    }

    public void goto_next_move(View view)
    {
        int progress = sbar.getProgress();
        int max = sbar.getMax();

        if (++progress > max)
            return;

        sbar.setProgress(progress);
    }

    public void goto_prev_move(View view)
    {
        int progress = sbar.getProgress();

        if (--progress < 0)
            return;

        sbar.setProgress(progress);
    }

    public void check_detail(View view)
    {
        AlertDialog.Builder builder;

        String message = "";
        GoControl.GoInfo info =  game.get_info();

        if (game.calc_mode()) {
            message += String.format("white dead : %d, black dead : %d\n",
                info.white_dead, info.black_dead);
            message += String.format("white house : %d, black house : %d\n",
                info.white_score, info.black_score);
            message += String.format("Live W on board : %d\n",
                info.white_count);
            message += String.format("Live B on board : %d\n",
                info.black_count);
            message += String.format("komi : %.1f\n", info.komi);
            message += String.format("white total : %.1f\n", info.white_final);
            message += String.format("black total : %.1f\n", info.black_final);
            message += "Result : ";

            if (info.score_diff == 0)
                message += "DRAW";
            else if (info.score_diff > 0)
                message += String.format("White won by %.1f", info.score_diff);
            else
                message += String.format("Black won by %.1f", Math.abs(info.score_diff));
        } else {
            message += String.format("white dead : %d\n", info.white_dead);
            message += String.format("black dead : %d\n", info.black_dead);
            message += String.format("komi : %.1f\n", info.komi);
        }

        builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
            .setTitle("Information")
            .setCancelable(false)
            .setPositiveButton("OK", null);

        AlertDialog alert = builder.create();
        alert.show();
    }
}
