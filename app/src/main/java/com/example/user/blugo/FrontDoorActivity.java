package com.example.user.blugo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;

public class FrontDoorActivity extends AppCompatActivity implements FileChooser.FileSelectedListener, Handler.Callback {
    public Handler msg_handler = new Handler(this);
    public final static int MSG_LOAD_END = 1;

    public final static String EXTRA_MESSAGE = "com.example.user.blugo.FrontDoorActivity.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_door);
    }

    public void load_SGF(View view)
    {
        FileChooser f = new FileChooser(this);

        f.setExtension("sgf");
        f.setFileListener(this);
        f.showDialog();
    }

    public void start_single_game(View view)
    {
        Intent intent = new Intent(this, GoBoardActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_LOAD_END:
                Intent intent = new Intent(this, ReviewGameActivity.class);
		intent.putExtra(EXTRA_MESSAGE, (String) msg.obj);
                startActivity(intent);
		/* sgf_string = null;*/
                return true;
        }
        return false;
    }

    @Override
    public void fileSelected(File file) {
        Message msg;
        msg = Message.obtain(FrontDoorActivity.this.msg_handler, MSG_LOAD_END, file.getPath());
        FrontDoorActivity.this.msg_handler.sendMessage(msg);
    }
}
