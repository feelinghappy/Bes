package com.iir_eq.stress;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.iir_eq.R;
import com.iir_eq.bluetooth.SppConnector;
import com.iir_eq.bluetooth.callback.ConnectCallback;
import com.iir_eq.ui.activity.ClassicScanActivity;
import com.iir_eq.util.Logger;
import com.iir_eq.util.SPHelper;

/**
 * Created by zhaowanxing on 2017/4/23.
 */

public class StressSppOtaActivity extends StressOtaActivity implements ConnectCallback {

    private static final String KEY_OTA_DEVICE_NAME = "spp_ota_device_name";
    private static final String KEY_OTA_DEVICE_ADDRESS = "spp_ota_device_addr";

    private SppConnector mSppConnector;

    @Override
    protected void initConfig() {
        super.initConfig();
        mSppConnector = SppConnector.getConnector();
        mSppConnector.addConnectCallback(this);
        registerBtReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            unregisterReceiver(mBtReceiver);
            if (mSppConnector != null) {
                mSppConnector.removeConnectCallback(this);
                mSppConnector.disconnect();
            }
        }
    }

    @Override
    protected void pickDevice(int request) {
        startActivityForResult(new Intent(this, ClassicScanActivity.class), request);
    }

    @Override
    protected String loadLastDeviceName() {
        return SPHelper.getPreference(this, KEY_OTA_DEVICE_NAME, "--").toString();
    }

    @Override
    protected void saveLastDeviceName(String name) {
        SPHelper.putPreference(this, KEY_OTA_DEVICE_NAME, name);
    }

    @Override
    protected String loadLastDeviceAddress() {
        return SPHelper.getPreference(this, KEY_OTA_DEVICE_ADDRESS, "--").toString();
    }

    @Override
    protected void saveLastDeviceAddress(String address) {
        SPHelper.putPreference(this, KEY_OTA_DEVICE_ADDRESS, address);
    }

    @Override
    protected void connect() {
        if (!mExit) {
            connectSpp();
        }
    }

    @Override
    protected void disconnect() {
        if (mSppConnector != null) {
            mSppConnector.disconnect();
        }
    }

    private void connectSpp() {
        if (!mExit) {
            if (mSppConnector.connect(mDevice)) {
                onConnecting();
            }
        }
    }

    private void onBonded() {
    }

    private void onBondNone() {
        //可以添加重新配对的操作
        updateInfo(R.string.bond_none);
    }

    @Override
    protected boolean sendData(byte[] data) {
        if (!mExit) {
            if (mSppConnector.write(data)) {
                onWritten();
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    protected boolean isBle() {
        return false;
    }

    @Override
    protected void onWritten() {
        Logger.e(TAG, "onWritten");
        super.onWritten();
        otaNextDelayed(90);
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        if (!mExit) {
//            if (connected) {
//                sendCmdDelayed(CMD_SEND_FILE_INFO, 0);
//            }
            super.onConnectionStateChanged(connected);
        }
    }

    private void registerBtReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBtReceiver, filter);
    }

    private BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.e(TAG, "onReceive " + intent);
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    onReceiveBondState((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE), intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE));
                    break;
            }
        }
    };

    private void onReceiveBondState(BluetoothDevice device, int state) {
        Logger.e(TAG, "onReceiveBondState " + state + "; device to connect " + mDevice + "; bond changed device " + device);
        if (!device.equals(mDevice)) {
            return;
        }
        if (state == BluetoothDevice.BOND_BONDED) {
            onBonded();
        } else if (state == BluetoothDevice.BOND_NONE) {
            onBondNone();
        }
    }

    @Override
    protected int getMtu() {
        return mMtu > 0 ? mMtu : DEFAULT_MTU;
    }
}
