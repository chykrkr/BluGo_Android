package com.example.user.blugo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class GoBoardActivity extends AppCompatActivity implements FileChooser.FileSelectedListener, Handler.Callback, GoBoardViewListener {
    private GoBoardView gv;
    private TextView txt_info;
    private GoControl single_game;
    private ProgressDialog progressBar;
    private String sgf_string = null;
    private File file;
    public static final int MSG_LOAD_END = GoBoardViewListener.MSG_MAX + 1;

    public Handler msg_handler = new Handler(this);

    private String get_info_text() {
        String str, result;
        GoControl.GoInfo info =  single_game.get_info();
        float score_diff;
        float white_final, black_final;

        GoControl.Player resigned = null;

        resigned = single_game.is_resigned();

        if (resigned != null) {
            if (resigned == GoControl.Player.BLACK) {
                return "W+R";
            } else {
                return "B+R";
            }
        }

        if (single_game.calc_mode()) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GoRule rule;
        NewBoardState state;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_board);

        Intent intent = getIntent();
        Bundle bundle;

        bundle = intent.getExtras();

        if (bundle == null) {
            single_game = new GoControlSingle();
        } else {
            state = bundle.getParcelable(ReviewGameActivity.MSG_BOARD_STATE);
            int bw = bundle.getInt(ReviewGameActivity.MSG_CURRENT_TURN);
            int start_turn = bundle.getInt(ReviewGameActivity.MSG_START_TURNNO);
            single_game = new GoControlSingle(state.size,
                bw == 0? GoControl.Player.BLACK : GoControl.Player.WHITE,
                new GoRuleJapan(state), start_turn);
        }

        gv = (GoBoardView) findViewById(R.id.go_board_view);
        gv.setGo_control(single_game);
        gv.setFocusable(true);

        txt_info = (TextView) findViewById(R.id.text_info);
        txt_info.setText(get_info_text());
    }

    public void undo(View view)
    {
        /* board clear */
        // single_game.new_game();

	/* undo last move */
	single_game.undo();
    }

    public void pass(View view)
    {
        single_game.pass();
    }

    public void load_SGF(View view)
    {
        FileChooser f = new FileChooser(this);

        f.setExtension("sgf");
        f.setFileListener(this);
        f.showDialog();
    }

    public void save_SGF(View view)
    {
        String file_name;
        Calendar cal = Calendar.getInstance();

        AlertDialog.Builder builder;
        final EditText file_name_input = new EditText(this);

        file_name = String.format(
            "%04d%02d%02d_%02d%02d%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DATE),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND));

        file_name_input.setText(file_name);

        builder = new AlertDialog.Builder(this);
        builder.setView(file_name_input)
            .setTitle("Input save file name")
            .setCancelable(false)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Message msg;
                    msg = Message.obtain(GoBoardActivity.this.msg_handler,
                        GoMessageListener.SAVE_FILE_NAME_INPUT_FINISHED,
                        file_name_input.getText().toString());

                    GoBoardActivity.this.msg_handler.sendMessage(msg);
                }
            })
            .setNegativeButton("CANCEL", null);

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void fileSelected(File file) {
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(true);
        progressBar.setMessage("Loading SGF ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        progressBar.show();

        this.file = file;

        Log.d("TEST", "selected file : " + file.toString());

        FileInputStream is;
        byte [] buffer = new byte[512];
        int read;
        String tmp;

        sgf_string = new String();

        try {
            is = new FileInputStream(file.getPath());

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
        }

        new Thread(new Runnable() {
            public void run() {
                single_game.load_sgf(sgf_string);
                Message msg;
                msg = Message.obtain(GoBoardActivity.this.msg_handler, MSG_LOAD_END, "msg");
                GoBoardActivity.this.msg_handler.sendMessage(msg);
            }
        }).start();


    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_LOAD_END:
                gv.invalidate();
                txt_info.setText(get_info_text());
                progressBar.dismiss();
                return true;

            case GoBoardViewListener.MSG_VIEW_FULLY_DRAWN:
                txt_info.setText(get_info_text());
                return true;

            case GoMessageListener.SAVE_FILE_NAME_INPUT_FINISHED:
                save_sgf_file_as((String)msg.obj, true);
                break;
        }

        return false;
    }

    private void save_sgf_file_as(String file_name, boolean add_extension)
    {
        String app_name;
        String sgf_text;

        app_name = getApplicationContext().getString(getApplicationContext().getApplicationInfo().labelRes);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.d("TEST", "External storage not mounted");
            return;
        }

        String path = Environment.getExternalStorageDirectory() + File.separator + app_name;
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs())
                Log.d("TEST", "Directory creation failed");
        }

        path += File.separator;

        sgf_text = single_game.get_sgf();

	FileOutputStream os;
	try {
	    os = new FileOutputStream(path + file_name + (add_extension? ".sgf": ""));
	    os.write(sgf_text.getBytes("UTF-8"));
	    os.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    @Override
    public Handler get_msg_handler() {
        return msg_handler;
    }

    public void resign(View view)
    {
        single_game.resign();
        txt_info.setText(get_info_text());
    }
}
