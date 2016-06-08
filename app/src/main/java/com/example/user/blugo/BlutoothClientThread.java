package com.example.user.blugo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.os.Handler;

import java.io.IOException;

/**
 * Created by user on 2016-06-08.
 */
public class BlutoothClientThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final BluetoothAdapter mBluetoothAdapter;
    private final GoMessageListener listener;
    private BlutoothCommThread connected_thread;

    public BlutoothClientThread(BluetoothAdapter adapter, BluetoothDevice device, GoMessageListener listener)
    {
        BluetoothSocket tmp = null;
        mmDevice = device;

        try {
            tmp = device.createRfcommSocketToServiceRecord(BlutoothServerThread.uuid);
        } catch (IOException e) {}
        mmSocket = tmp;
        this. mBluetoothAdapter = adapter;
        this.listener = listener;
    }

    public BlutoothCommThread get_connected()
    {
        return this.connected_thread;
    }

    public void run()
    {
        Handler h;
        Message msg;

        mBluetoothAdapter.cancelDiscovery();
        h = listener.get_msg_handler();

        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            msg = Message.obtain(h, GoMessageListener.BLUTOOTH_CLIENT_SOCKET_ERROR,
                connectException.toString());
            h.sendMessage(msg);
            cancel();
            return;
        }

        //manage connection
        connected_thread = BlutoothCommThread.getInstance(mmSocket, listener);
        connected_thread.start();
        try {
            msg = Message.obtain(h, GoMessageListener.BLUTOOTH_CLIENT_CONNECT_SUCCESS,
                "connection success");
            h.sendMessage(msg);
            connected_thread.join();
        } catch(InterruptedException e) {
            msg = Message.obtain(h, GoMessageListener.BLUTOOTH_CLIENT_SOCKET_ERROR,
                e.toString());
            h.sendMessage(msg);
        }
        connected_thread = null;
        cancel();
    }

    public synchronized void cancel()
    {
        try {
            mmSocket.close();
        } catch (IOException e) {}
    }
}
