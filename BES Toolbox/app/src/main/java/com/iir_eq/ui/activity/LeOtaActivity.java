package com.iir_eq.ui.activity;

import android.content.Intent;

import com.iir_eq.R;
import com.iir_eq.bluetooth.LeConnector;
import com.iir_eq.bluetooth.callback.LeConnectCallback;
import com.iir_eq.contants.Constants;
import com.iir_eq.util.ArrayUtil;
import com.iir_eq.util.SPHelper;

/**
 * Created by zhaowanxing on 2017/7/12.
 */

public class LeOtaActivity extends OtaActivity implements LeConnectCallback {

    private static final String KEY_OTA_DEVICE_NAME = "ble_ota_device_name";
    private static final String KEY_OTA_DEVICE_ADDRESS = "ble_ota_device_addr";

    private LeConnector mConnector;


    @Override
    protected void initConfig() {
        super.initConfig();
        mConnector = new LeConnector();
        mConnector.addConnectCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LOG(TAG , "onPause");
        if (isFinishing()) {
            if (mConnector != null) {
                mConnector.removeConnectCallback(this);
                mConnector.close();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LOG(TAG , "onResume");
    }

    @Override
    protected void connect() {
        if (!mExit) {
            if (!mConnector.connect(this, mDevice)) {
                mConnector.close();
            } else {
                sendMsgFailCount = 0 ;
                onConnecting();
            }
        }
    }

    @Override
    protected void disconnect() {
        if (mConnector != null) {
            mConnector.close();
        }
    }

    @Override
    synchronized  protected boolean sendData( byte[] data) {
        if (!mExit) {
            if (mConnector.write(data )) {
                mWritten = false;
                LOG(TAG , "sendData mConnector.write(data  , isResponse) send true mWritten reset to false "+" data "+ ArrayUtil.toHex(data));
                return true;
            }else{
                sendMsgFailCount++;
                LOG(TAG, "sendData mConnector.write(data) return false failCount = "+sendMsgFailCount );
            }

            return false;
        }
        return true;
    }

    @Override
    protected boolean isBle() {
        return true;
    }

    @Override
    public void onServicesDiscovered(int status) {
        LOG(TAG, "onServicesDiscovered " + status);
        if (!mExit) {
            if (mConnector.setWriteCharacteristic(Constants.OTA_SERVICE_OTA_UUID, Constants.OTA_CHARACTERISTIC_OTA_UUID)) {
                if (!mConnector.requestMtu(DEFAULT_MTU)) {
                    LOG(TAG, "requestMtu result false");
                    enableCharacteristicNotification();
                } else {
                    LOG(TAG, "requestMtu DEFAULT_MTU = "+DEFAULT_MTU);
                    updateInfo(R.string.configing_mtu);
                }
            } else {
                LOG(TAG, "onServicesDiscovered error service");
                updateInfo(R.string.ota_error_service_uuid);
                sendCmdDelayed(CMD_DISCONNECT, 1000);
            }
        }
    }

    private void enableCharacteristicNotification() {
        if (!mExit) {
            if (!mConnector.enableCharacteristicNotify(Constants.OTA_SERVICE_OTA_UUID, Constants.OTA_CHARACTERISTIC_OTA_UUID, Constants.OTA_DESCRIPTOR_OTA_UUID)) {
                LOG(TAG, "enableCharacteristicNotification false ");
                mConnector.refresh();
                mConnector.close();
            }else{
                LOG(TAG, "enableCharacteristicNotification return true");
            }
        }
    }

    @Override
    public void onCharacteristicNotifyEnabled(int status) {
        if (!mExit) {
            if (status == LeConnector.LE_SUCCESS) {
                super.onConnectionStateChanged(true);
            }else{
                LOG(TAG, "onCharacteristicNotifyEnabled false status is "+ status);
            }
        }
    }

    @Override
    public void onWritten(int status) {
        if (status == LeConnector.LE_SUCCESS) {
            super.onWritten();
            LOG(TAG, "onWritten SUCCESS");
            otaNextDelayed(10);
        }else{
            LOG(TAG, "onWritten return false status is "+status);
        }
    }

    @Override
    public void onMtuChanged(int status, int mtu) {
        if (!mExit) {
            if (status == LeConnector.LE_SUCCESS) {
                LOG(TAG, "onMtuChanged mtu = "+ mtu);
                mMtu = mtu;
                updateInfo(R.string.config_mtu_successfully);
            } else {
                LOG(TAG, "onMtuChanged false status = "+status);
                updateInfo(R.string.config_mtu_failed);
            }
            enableCharacteristicNotification();
        }
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        if (!mExit) {
            if (connected) {
                LOG(TAG, "onConnectionStateChanged connected ready to discoverServices");
                discoverServices();
            }else{
                super.onConnectionStateChanged(connected);
            }

        }
    }

    private void discoverServices() {
        if (!mExit) {
            if (!mConnector.discoverServices()) {
                LOG(TAG, "discoverServices reture false so bad");
                mConnector.close();
                updateInfo(R.string.discover_services_error);
            }else{
                LOG(TAG, "discoverServices reture true but we need to wait the callback");
            }
        }
    }

    @Override
    protected int getMtu() {
        return mMtu;
    }

    @Override
    protected void pickDevice(int request) {
        Intent intent = new Intent(this, LeScanActivity.class);
        intent.putExtra(LeScanActivity.EXTRA_MODE, LeScanActivity.MODE_OTA);
        startActivityForResult(intent, request);
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
}
