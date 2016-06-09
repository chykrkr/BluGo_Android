package com.example.user.blugo;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class FrontDoorActivity extends AppCompatActivity implements FileChooser.FileSelectedListener,
    Handler.Callback, DialogInterface.OnDismissListener, GoMessageListener {
    public Handler msg_handler = new Handler(this);

    public final static int REQUEST_ENABLE_BT = 1;
    public final static int REQUEST_READ_EXTERNAL_STORAGE = 1;

    public final static String EXTRA_MESSAGE = "com.example.user.blugo.FrontDoorActivity.MESSAGE";

    private BluetoothAdapter mBluetoothAdapter = null;

    private BlutoothServerThread server = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_door);
    }

    public void load_SGF(View view)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
            return;
        }

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

    private Boolean enableBluetooth()
    {
        if (mBluetoothAdapter == null)
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Log.e("", "Bluetooth isn't supported.");
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else
            return true;

        return false;
    }

    public void wait_game_request(View view)
    {
        if (enableBluetooth() == false)
            return;

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.wait_dialog, null);

        final Dialog dialog;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog);
        } else {
            dialog = new Dialog(this);
        }

        TextView t = (TextView) layout.findViewById(R.id.text_b_addr);

        String macAddress;

        /*
        After marshmallow,
        getAddress() method returns only 00:02:00:00:00:00 (deliberated false address).
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            macAddress = android.provider.Settings.Secure.getString(this.getContentResolver(),
                "bluetooth_address");
        } else {
            macAddress = mBluetoothAdapter.getAddress();
        }

        t.setText(macAddress);
        t = (TextView) layout.findViewById(R.id.text_b_name);
        t.setText(mBluetoothAdapter.getName());

        server = new BlutoothServerThread(mBluetoothAdapter, this);
        server.start();

        dialog.setContentView(layout);
        dialog.setTitle("Waiting request ...");
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    public void enable_discover(View view)
    {
        /* goto discoverable mode */
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    public void request_play(View view)
    {
        if (enableBluetooth() == false)
            return;

        Intent intent = new Intent(this, PlayRequestActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case FRONTDOORACTIVITY_MSG_LOAD_END:
                Intent intent = new Intent(this, ReviewGameActivity.class);
                intent.putExtra(EXTRA_MESSAGE, (String) msg.obj);
                startActivity(intent);
		/* sgf_string = null;*/
                return true;

            case GoMessageListener.BLUTOOTH_SERVER_SOCKET_ERROR:
                if (server != null) {
                    server.cancel();
                    try {
                        server.join();
                    } catch (InterruptedException e) {}
                    server = null;
                }
                break;

            case GoMessageListener.BLUTOOTH_COMM_MSG:
                BlutoothMsgParser.MsgParsed parsed = (BlutoothMsgParser.MsgParsed) msg.obj;
                handle_comm_message(parsed);
                break;
        }
        return false;
    }

    private void handle_comm_message(BlutoothMsgParser.MsgParsed msg)
    {
        String m;
        switch (msg.type) {
            case REQUEST_PLAY:
                Log.d("SERVER", "REQUEST_PLAY RECEIVED");
                m = BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.REQUEST_PLAY_ACK,
                    null);
                server.get_connected().write(m);

                Intent intent = new Intent(this, BluetoothGameActivity.class);
                intent.putExtra(GoMessageListener.STONE_COLOR_MESSAGE, 0); /* Server is black */
                startActivity(intent);
                break;
        }
    }

    @Override
    public void fileSelected(File file) {
        Message msg;
        msg = Message.obtain(FrontDoorActivity.this.msg_handler, FRONTDOORACTIVITY_MSG_LOAD_END, file.getPath());
        FrontDoorActivity.this.msg_handler.sendMessage(msg);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (server == null)
            return;
        server.cancel();
        try {
            server.join();
        } catch (InterruptedException e) {}
        server = null;
    }

    @Override
    public Handler get_msg_handler() {
        return this.msg_handler;
    }
}
