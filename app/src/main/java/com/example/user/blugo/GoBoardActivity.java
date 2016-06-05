package com.example.user.blugo;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class GoBoardActivity extends AppCompatActivity implements FileChooser.FileSelectedListener, Handler.Callback {
    private GoBoardView gv;
    private TextView txt_info;
    private GoControl single_game = new GoControlSingle();
    private ProgressDialog progressBar;
    private String sgf_string = null;
    private File file;
    public static final int MSG_LOAD_END = 1;
    public static final int MSG_INFO_CHANGED = 2;

    public Handler msg_handler = new Handler(this);

    private String get_info_text() {
        String str, result;
        GoControl.GoInfo info =  single_game.get_info();
        float score_diff;
        float white_final, black_final;

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

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_board);

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

        path += File.separator + app_name;

        sgf_text = single_game.get_sgf();

	FileOutputStream os;
	try {
	    os = new FileOutputStream(path + "test.sgf");
	    os.write(sgf_text.getBytes("UTF-8"));
	    os.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
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

            case MSG_INFO_CHANGED:
                txt_info.setText(get_info_text());
                return true;
        }

        return false;
    }
}
