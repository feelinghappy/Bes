package com.iir_eq.ui.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.iir_eq.R;
import com.iir_eq.bluetooth.BtHelper;
import com.iir_eq.bluetooth.callback.ScanCallback;
import com.iir_eq.bluetooth.scanner.BtScanner;
import com.iir_eq.ui.adapter.DeviceAdapter;

import butterknife.BindView;

/**
 * Created by zhaowanxing on 2017/5/8.
 */

public abstract class ScanActivity extends BaseActivity implements ScanCallback, SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener {

    public static final String EXTRA_DEVICE = "extra_device";

    @BindView(R.id.devices)
    ListView mDevices;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefresh;

    private DeviceAdapter mAdapter;
    private BtScanner mScanner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_scan);
        initView();
        initScanner();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            stopScan();
            mScanner.close();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initScanner() {
        mScanner = getBtScanner();
        if (mScanner == null) {
            finish();
            return;
        }
    }

    private void initView() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAdapter = new DeviceAdapter(this);
        mDevices.setAdapter(mAdapter);
        mSwipeRefresh.setOnRefreshListener(this);
        mDevices.setOnItemClickListener(this);
    }

    protected boolean startScan() {
        mAdapter.clear();
        if (!initBluetooth())
            return false;
        if (!checkConditions())
            return false;
        mScanner.startScan(this);
        return true;
    }

    protected void stopScan() {
        mScanner.stopScan();
    }

    protected abstract BtScanner getBtScanner();

    protected abstract boolean checkConditions();

    protected abstract void onDeviceSelected(BluetoothDevice device);

    protected abstract boolean filter(BluetoothDevice device, byte[] scanRecord);

    @Override
    public void onRefresh() {
        startScan();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onDeviceSelected(mAdapter.getItem(position));
    }

    @Override
    public void onFound(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (filter(device, scanRecord)) {
                    mAdapter.add(device, rssi);
                }
            }
        });
    }

    @Override
    public void onScanStart() {

    }

    @Override
    public void onScanFinish() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSwipeRefresh != null && mSwipeRefresh.isRefreshing())
                    mSwipeRefresh.setRefreshing(false);
            }
        });
    }

    private boolean initBluetooth() {
        if (BtHelper.getBluetoothAdapter(this).isEnabled()) {
            return true;
        }
        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        return false;
    }
}
