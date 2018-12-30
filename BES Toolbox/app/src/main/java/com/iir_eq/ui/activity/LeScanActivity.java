package com.iir_eq.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.iir_eq.R;
import com.iir_eq.bluetooth.BtHelper;
import com.iir_eq.bluetooth.LeManager;
import com.iir_eq.bluetooth.callback.LeConnectCallback;
import com.iir_eq.bluetooth.scanner.BtScanner;
import com.iir_eq.bluetooth.scanner.LeScannerCompat;
import com.iir_eq.contants.Constants;
import com.iir_eq.util.ArrayUtil;

/**
 * Created by zhaowanxing on 2017/4/16.
 */

public class LeScanActivity extends ScanActivity implements LeConnectCallback {

    private Handler mHandler = new Handler();
    private LeManager mLeManager;

    private static final int REQUEST_LOCATION_PERMISSION = 0x01;
    private static final byte[] FILTER_EQ = null; //new byte[]{0x45, 0x51, 0x5F, 0x54, 0x45, 0x53, 0x54};//EQ_TEST; 0x42,0x45,0x53
    private static final byte[] FILTER_OTA = null;
    private static final byte[] FILTER_PLAYER = null;

    public static final String EXTRA_MODE = "mode";
    public static final int MODE_OTA = 1;
    public static final int MODE_EQ = 2;
    public static final int MODE_PLAYER = 3;

    private int mMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    private void initData() {
        mMode = getIntent().getIntExtra(EXTRA_MODE, MODE_EQ);
        if (mMode == MODE_EQ || mMode == MODE_PLAYER) {
            mLeManager = LeManager.getLeManager();
            mLeManager.addConnectCallback(this);
        }
    }

    @Override
    protected BtScanner getBtScanner() {
        return LeScannerCompat.getLeScanner(this);
    }

    private boolean initPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.ble_location_permission_tip)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(LeScanActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                        }
                    }).create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        return false;
    }

    private boolean initLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return true;
        }
        new AlertDialog.Builder(this).setMessage(R.string.ble_gps_enable_tip)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                }).create().show();
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (permissions != null && permissions.length > 0) {
                for (int i = 0; i < permissions.length; i++) {
                    if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i]) || Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean checkConditions() {
        if (!initPermission())
            return false;
        if (!initLocation())
            return false;
        return true;
    }

    @Override
    protected boolean filter(BluetoothDevice device, byte[] scanRecord) {
        byte[] manufacture = BtHelper.parseManufacturerSpecificData(scanRecord);
        switch (mMode) {
            case MODE_EQ:
                return ArrayUtil.contains(manufacture, FILTER_EQ);
            case MODE_OTA:
                return ArrayUtil.contains(manufacture, FILTER_OTA);
            case MODE_PLAYER:
                return ArrayUtil.contains(manufacture, FILTER_PLAYER);
            default:
                return true;
        }
    }

    @Override
    public void onScanStart() {
        super.onScanStart();
        mHandler.postDelayed(mStopScan, 5000);
    }

    @Override
    public void onScanFinish() {
        mHandler.removeCallbacks(mStopScan);
        super.onScanFinish();
    }

    private Runnable mStopScan = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    @Override
    protected void onPause() {
        if (isFinishing()) {
            mHandler.removeCallbacks(mStopScan);
            if (mLeManager != null) {
                mLeManager.removeConnectCallback(this);
            }
        }
        super.onPause();
    }


    @Override
    protected void onDeviceSelected(BluetoothDevice device) {
        if (mMode == MODE_EQ || mMode == MODE_PLAYER) {
            mLeManager.connect(this, device);
        } else if (mMode == MODE_OTA) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE, device);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onConnectionStateChanged(final boolean connected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connected) {
                    if (!mLeManager.discoverServices()) {
                        mLeManager.close();
                    }
                } else {
                    showToast(getString(R.string.connect_failed));
                }
            }
        });
    }

    @Override
    public void onReceive(byte[] data) {

    }

    @Override
    public void onServicesDiscovered(int status) {
        if (mLeManager.setWriteCharacteristic(Constants.BES_SERVICE_UUID, Constants.BES_CHARACTERISTIC_TX_UUID)) {
            if (mMode == MODE_PLAYER) {
                if (!mLeManager.enableCharacteristicNotify(Constants.BES_SERVICE_UUID, Constants.BES_CHARACTERISTIC_RX_UUID, Constants.BES_DESCRIPTOR_UUID)) {
                    mLeManager.close();
                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(LeScanActivity.this, EQActivity.class));
                        finish();
                    }
                });
            }
        } else {
            mLeManager.close();
        }
    }

    @Override
    public void onCharacteristicNotifyEnabled(int status) {
        if (mMode == MODE_PLAYER) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(LeScanActivity.this, PlayerActivity.class));
                    finish();
                }
            });
        }
    }

    @Override
    public void onWritten(int status) {

    }

    @Override
    public void onMtuChanged(int status, int mtu) {

    }
}
