package com.iir_eq.ui.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.iir_eq.bluetooth.scanner.BtScanner;
import com.iir_eq.bluetooth.scanner.ClassicScanner;

/**
 * Created by zhaowanxing on 2017/5/8.
 */

public class ClassicScanActivity extends ScanActivity {

    @Override
    protected BtScanner getBtScanner() {
        return new ClassicScanner(this);
    }

    @Override
    protected boolean checkConditions() {
        return true;
    }

    @Override
    protected void onDeviceSelected(BluetoothDevice device) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE, device);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected boolean filter(BluetoothDevice device, byte[] scanRecord) {
        return true;
    }
}
