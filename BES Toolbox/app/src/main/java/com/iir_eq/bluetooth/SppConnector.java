package com.iir_eq.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;

import com.iir_eq.bluetooth.callback.ConnectCallback;
import com.iir_eq.util.ArrayUtil;
import com.iir_eq.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by zhaowanxing on 2017/4/23.
 */

public class SppConnector {
    private final String TAG = getClass().getSimpleName();

    public static final UUID sUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static volatile SppConnector sConnector;
    private BluetoothSocket mBluetoothSocket;

    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_DISCONNECTED = 0;

    private int mConnState = STATE_DISCONNECTED;
    private Object mCallbackLock = new Object();

    public static SppConnector getConnector() {
        if (sConnector == null) {
            synchronized (SppConnector.class) {
                if (sConnector == null) {
                    sConnector = new SppConnector();
                }
            }
        }
        return sConnector;
    }

    public boolean connect(@NonNull BluetoothDevice device) {
        if (mConnState == STATE_CONNECTING || mConnState == STATE_CONNECTED) {
            return false;
        }
        new Thread(new ConnectRunnable(device)).start();
        return true;
    }

    public void disconnect() {
        try {
            if (mBluetoothSocket != null)
                mBluetoothSocket.close();
            onConnectionStateChanged(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean write(byte[] data) {
        if (mConnectedRunnable != null) {
            return mConnectedRunnable.write(data);
        }
        return false;
    }

    private List<ConnectCallback> mConnectCallbacks = new ArrayList<>();

    public void addConnectCallback(ConnectCallback callback) {
        synchronized (mCallbackLock) {
            if (!mConnectCallbacks.contains(callback))
                mConnectCallbacks.add(callback);
        }
    }

    public void removeConnectCallback(ConnectCallback callback) {
        synchronized (mCallbackLock) {
            mConnectCallbacks.remove(callback);
        }
    }

    private void onConnectionStateChanged(boolean connected) {
        synchronized (mCallbackLock) {
            if (connected && mConnState != STATE_CONNECTED) {
                for (ConnectCallback callback : mConnectCallbacks)
                    callback.onConnectionStateChanged(true);
                mConnState = STATE_CONNECTED;
            } else if (!connected && mConnState != STATE_DISCONNECTED) {
                mBluetoothSocket = null;
                mConnectedRunnable = null;
                mConnState = STATE_DISCONNECTED;
                for (ConnectCallback callback : mConnectCallbacks)
                    callback.onConnectionStateChanged(false);
            }
        }
    }

    private void onReceive(byte[] data) {
        synchronized (mCallbackLock) {
            for (ConnectCallback callback : mConnectCallbacks)
                callback.onReceive(data);
        }
    }

    private ConnectedRunnable mConnectedRunnable;

    private class ConnectRunnable implements Runnable {

        private BluetoothDevice mDevice;

        public ConnectRunnable(BluetoothDevice device) {
            mDevice = device;
        }

        @Override
        public void run() {
            try {
                mConnState = STATE_CONNECTING;
                mBluetoothSocket = mDevice.createInsecureRfcommSocketToServiceRecord(sUUID);
                mBluetoothSocket.connect();
                mConnectedRunnable = new ConnectedRunnable(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());
                onConnectionStateChanged(true);
                new Thread(mConnectedRunnable).start();
            } catch (IOException e) {
                e.printStackTrace();
                onConnectionStateChanged(false);
            }
        }
    }

    private class ConnectedRunnable implements Runnable {

        private OutputStream mWrite;
        private InputStream mRead;

        public ConnectedRunnable(InputStream read, OutputStream write) {
            mRead = read;
            mWrite = write;
        }

        @Override
        public void run() {
            try {
                byte[] data = new byte[1024 * 1024];
                Logger.e("SPP", "connected thread run");
                while (true) {
                    int length = mRead.read(data);
                    onReceive(ArrayUtil.extractBytes(data, 0, length));
                }
            } catch (IOException e) {
                e.printStackTrace();
                onConnectionStateChanged(false);
            } finally {
                try {
                    if (mRead != null)
                        mRead.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean write(byte[] data) {
            try {
                mWrite.write(data);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                onConnectionStateChanged(false);
            }
            return false;
        }
    }
}
