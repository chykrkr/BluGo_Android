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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class GoBoardActivity extends AppCompatActivity implements FileChooser.FileSelectedListener, Handler.Callback {
    private GoBoardView gv;
    private GoControl single_game = new GoControlSingle();
    private ProgressDialog progressBar;
    private String sgf_string = null;
    private File file;

    public Handler msg_handler = new Handler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_board);

        gv = (GoBoardView) findViewById(R.id.go_board_view);
        gv.setGo_control(single_game);
        gv.setFocusable(true);
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
                msg = Message.obtain(GoBoardActivity.this.msg_handler, 1, "msg");
                GoBoardActivity.this.msg_handler.sendMessage(msg);
            }
        }).start();


    }

    @Override
    public boolean handleMessage(Message msg) {
        gv.invalidate();
        progressBar.dismiss();
        return false;
    }
}
