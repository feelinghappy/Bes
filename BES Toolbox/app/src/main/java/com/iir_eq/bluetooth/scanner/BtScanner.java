package com.iir_eq.bluetooth.scanner;

import com.iir_eq.bluetooth.callback.ScanCallback;

/**
 * Created by zhaowanxing on 2017/4/17.
 */

public interface BtScanner {

    void startScan(ScanCallback callback);

    void stopScan();

    void close();
}
