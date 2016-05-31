package com.example.user.blugo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements Handler.Callback {

    public final static int REQUEST_ENABLE_BT = 1;
    public final static int REQUEST_COARSE_LOCATION = 2;
    public final static int MESSAGE_RECEIVED = 3;
    public final static int ERR_MESSAGE_RECEIVED = 4;
    public final static UUID MY_UUID = UUID.fromString("4a10c529-1d36-4fa2-b4b1-a95a88734c63");
    private BluetoothAdapter mBluetoothAdapter = null;

    private SingleReceiver mReceiver = null;

    private ListView dev_listview;
    private ArrayAdapter arrayAdapter;
    private List<BluetoothDeviceWrap> device_array;

    private AcceptThread accept_thread = null;
    private ConnectThread conn_thread = null;
    private ConnectedThread connected_thread = null;

    public Handler msg_handler = new Handler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

	dev_listview = (ListView) findViewById(R.id.device_list);
	device_array = new ArrayList<BluetoothDeviceWrap>();
	arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, device_array);
	dev_listview.setAdapter(arrayAdapter);
	dev_listview.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                    Object o = dev_listview.getItemAtPosition(position);
		    BluetoothDevice device = ((BluetoothDeviceWrap)o).getBluetoothDevice();
		    /*Toast.makeText(MainActivity.this, device.getName(), Toast.LENGTH_SHORT).show();*/
		    if (conn_thread != null) {
			conn_thread.cancel();
			try {
			    conn_thread.join();
			} catch(InterruptedException e) {}
			conn_thread = null;
		    }

		    conn_thread = new ConnectThread(device);
		    conn_thread.start();
                }
            }
        );
    }

    @Override
    public boolean handleMessage(Message msg)
    {
	switch (msg.what) {
	case MESSAGE_RECEIVED:
	case ERR_MESSAGE_RECEIVED:
	    Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_LONG).show();
	    break;
	}
        return false;
    }

    private Boolean enableBluetooth()
    {
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

    public void runServer(View view)
    {
	if (!enableBluetooth())
	    return;

	Log.d("MYLOG", "Bluetooth enabled");

	/* goto discoverable mode */
	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
	startActivity(discoverableIntent);

	if (accept_thread != null) {
	    accept_thread.cancel();
	    try {
		accept_thread.join();
	    } catch(InterruptedException e) {}
	    accept_thread = null;
	}

	accept_thread = new AcceptThread();
	accept_thread.start();

    }

    protected void checkPermission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
        } else
	    proceedDiscovery();
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

    public void listBluetoothDevice(View view)
    {
	BluetoothDeviceWrap wrap;

	if (!enableBluetooth())
	    return;

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

	checkPermission();
    }

    public void send_message(View view)
    {
	EditText editText;
	byte[] b;

	if (connected_thread == null)
	    return;

	editText = (EditText) findViewById(R.id.text_send);

	b = editText.getText().toString().getBytes(Charset.forName("UTF-8"));

	connected_thread.write(b);
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
	    }
	}
    }

    private class AcceptThread extends Thread {
	private final BluetoothServerSocket mmServerSocket;

	public AcceptThread() {
	    BluetoothServerSocket tmp = null;
	    try {
		tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BluGoServer", MY_UUID);
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	    mmServerSocket = tmp;
	}

	public void run() {
	    BluetoothSocket socket = null;

	    while (true) {
		try {
		    socket = mmServerSocket.accept();
		} catch (IOException e) {
		    Message msg;

		    Log.d("MYTAG", e.toString());

		    msg = Message.obtain(MainActivity.this.msg_handler, MainActivity.MESSAGE_RECEIVED,
					 e.toString());
		    MainActivity.this.msg_handler.sendMessage(msg);
		    cancel();
		    break;
		}

		if (socket != null) {
		    /*Manage server socket*/
		    connected_thread = new ConnectedThread(socket);
		    connected_thread.start();
		    try {
			connected_thread.join();
		    } catch(InterruptedException e) {
			Log.d("MYTAG", e.toString());
		    }
		    connected_thread = null;
		}
	    }
	}

	public void cancel() {
	    try {
		mmServerSocket.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    private class ConnectThread extends Thread {
	private final BluetoothSocket mmSocket;
	private final BluetoothDevice mmDevice;

	public ConnectThread(BluetoothDevice device)
	{
	    BluetoothSocket tmp = null;
	    mmDevice = device;

	    try {
		tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	    } catch (IOException e) {}
	    mmSocket = tmp;
	}

	public void run()
	{
	    mBluetoothAdapter.cancelDiscovery();

	    try {
		mmSocket.connect();
	    } catch (IOException connectException) {
		Message msg;
		msg = Message.obtain(MainActivity.this.msg_handler, MainActivity.MESSAGE_RECEIVED,
			       connectException.toString());
		MainActivity.this.msg_handler.sendMessage(msg);
		cancel();
		return;
	    }

	    //manage connection
	    connected_thread = new ConnectedThread(mmSocket);
	    connected_thread.start();
	    try {
		connected_thread.join();
	    } catch(InterruptedException e) {}
	    connected_thread = null;
	    cancel();
	}

	public void cancel()
	{
	    try {
		mmSocket.close();
	    } catch (IOException e) {}
	}
    }

    private class ConnectedThread extends Thread
    {
	private final BluetoothSocket mmSocket;
	private final InputStream mmInStream;
	private final OutputStream mmOutStream;

	public ConnectedThread(BluetoothSocket socket)
	{
	    InputStream tmpIn = null;
	    OutputStream tmpOut = null;

	    mmSocket = socket;

	    try {
		tmpIn = socket.getInputStream();
		tmpOut = socket.getOutputStream();
	    } catch (IOException e) {}

	    mmInStream = tmpIn;
	    mmOutStream = tmpOut;
	}

	public void run()
	{
	    byte[] buffer = new byte[1024];
	    int bytes;
	    String message;
	    Message msg;

	    while (true) {
		try {
		    bytes = mmInStream.read(buffer);
		    message = new String(buffer, Charset.forName("UTF-8"));

		    msg = Message.obtain();
		    msg.what = MainActivity.MESSAGE_RECEIVED;
		    msg.obj = message;
		    MainActivity.this.msg_handler.sendMessage(msg);
		    // Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
		    
		} catch(IOException e) {
		    Log.d("MYTAG", e.toString());
		    msg = Message.obtain(MainActivity.this.msg_handler, MainActivity.MESSAGE_RECEIVED,
					 e.toString());
		    MainActivity.this.msg_handler.sendMessage(msg);
		    cancel();
		    break;
		}
	    }
	}

	public void write(byte[] bytes) {
	    try {
		mmOutStream.write(bytes);
	    } catch (IOException e) {}
	}

	public void cancel()
	{
	    try {
		mmSocket.close();
	    } catch (IOException e) {}
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
}


