package com.example.user.blugo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class PlayRequestActivity extends AppCompatActivity implements GoMessageListener,
    Handler.Callback {
    public final static int REQUEST_COARSE_LOCATION = 2;

    private ListView dev_listview;
    private ArrayList<BluetoothDeviceWrap> device_array;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter arrayAdapter;
    private SingleReceiver mReceiver = null;
    private ProgressBar pbar_discover;
    private BlutoothClientThread client;
    public Handler msg_handler = new Handler(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_request);

        dev_listview = (ListView) findViewById(R.id.device_list);
        device_array = new ArrayList<BluetoothDeviceWrap>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, device_array);
        dev_listview.setAdapter(arrayAdapter);
        dev_listview.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                    Object o = dev_listview.getItemAtPosition(position);
                    BluetoothDevice device = ((BluetoothDeviceWrap) o).getBluetoothDevice();

                    if (client != null) {
                        client.cancel();
                        try {
                            client.join();
                        } catch (InterruptedException e) {
                        }
                        client = null;
                    }

                    client = new BlutoothClientThread(mBluetoothAdapter, device,
                        PlayRequestActivity.this);
                    client.start();

                }
            }
        );

        pbar_discover = (ProgressBar) findViewById(R.id.discover_on_progress);
        pbar_discover.setVisibility(View.INVISIBLE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listBluetoothDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /* We must execute below codes */
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        if (mReceiver != null)
            unregisterReceiver(mReceiver);
    }

    public void listBluetoothDevice()
    {
        BluetoothDeviceWrap wrap;

        device_array.clear();

	/* List already found devices */
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d("MYTAG", "Device : " + String.format("%s, %s", device.getName(), device.getAddress()));
                wrap = new BluetoothDeviceWrap(device);

                if (!device_array.contains(wrap)) {
                    device_array.add(wrap);
                }
            }
        }

        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    proceedDiscovery();
                }
                break;
        }
    }

    protected void proceedDiscovery()
    {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        if (mReceiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            mReceiver = new SingleReceiver();
            registerReceiver(mReceiver, filter);
        }

        Log.d("TAG", "Result:" + mBluetoothAdapter.startDiscovery());
    }

    public void discover_bluetooth_devices(View view)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
        } else
            proceedDiscovery();
    }

    @Override
    public Handler get_msg_handler() {
        return msg_handler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        String m;
        switch (msg.what) {
            case GoMessageListener.BLUTOOTH_CLIENT_SOCKET_ERROR:
                Toast.makeText(this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                break;
            case GoMessageListener.BLUTOOTH_CLIENT_CONNECT_SUCCESS:
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }

                if (mReceiver != null)
                    unregisterReceiver(mReceiver);

                Toast.makeText(this, (String) msg.obj, Toast.LENGTH_SHORT).show();

                /* send game request */
                m = BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.REQUEST_PLAY,
                    null);
                client.get_connected().write(m);
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
            case REQUEST_PLAY_ACK:
                Log.d("CLIENT", "PLAY_ACK RECEIVED");
                Intent intent = new Intent(this, BluetoothGameActivity.class);
                intent.putExtra(GoMessageListener.STONE_COLOR_MESSAGE, 1); /* client is white */
                startActivity(intent);
                break;
        }
    }

    private class BluetoothDeviceWrap {
        public BluetoothDevice device;

        public BluetoothDeviceWrap(BluetoothDevice device)
        {
            this.device = device;
        }

        @Override
        public String toString()
        {
            return String.format("%s (%s)", device.getAddress(), device.getName());
        }

        @Override
        public boolean equals(Object o)
        {
            BluetoothDeviceWrap target = (BluetoothDeviceWrap) o;
            return device.equals(target.device);
        }

        public BluetoothDevice getBluetoothDevice()
        {
            return this.device;
        }
    }

    private class SingleReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            BluetoothDeviceWrap wrap;

            Log.d("MYTAG", "Received : " + action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("FOUND", String.format("%s, %s", device.getName(), device.getAddress()));

                wrap = new BluetoothDeviceWrap(device);

                if (!device_array.contains(wrap)) {
                    device_array.add(wrap);
                    arrayAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                pbar_discover.setVisibility(View.VISIBLE);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                pbar_discover.setVisibility(View.INVISIBLE);
            }
        }
    }
}
