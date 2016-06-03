package com.example.user.blugo;

import android.content.Context;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class GoBoardActivity extends AppCompatActivity implements FileChooser.FileSelectedListener {
    private GoBoardView gv;
    private GoControl single_game = new GoControlSingle();

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

        /*
        FileInputStream is;
        byte [] buffer = new byte[501];
        int read;
        String result;

        Log.d("READ", getApplicationContext().getFilesDir().toString());

        try {
            is = openFileInput("test.sgf");
            read = is.read(buffer, 0, 500);

            if (read > 0) {
                buffer[read] = 0x00;
                result = new String(buffer, 0, read);
                Log.d("READ", result);
            }

        } catch (Exception e) {

        }
        */
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
        Log.d("TEST", "selected file : " + file.toString());
    }
}
