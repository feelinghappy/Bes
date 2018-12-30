package com.iir_eq.ui.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.iir_eq.R;
import com.iir_eq.bluetooth.LeConnector;
import com.iir_eq.bluetooth.callback.LeConnectCallback;
import com.iir_eq.bluetooth.LeManager;
import com.iir_eq.bluetooth.callback.ConnectCallback;
import com.iir_eq.db.DataHelper;
import com.iir_eq.model.EQConfig;
import com.iir_eq.ui.view.Counter;
import com.iir_eq.util.Logger;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTouch;

/**
 * Created by zhaowanxing on 2017/4/17.
 */

public class EQActivity extends BaseActivity implements ConnectCallback, Counter.OnCounterTouchListener {

    private static final float MIN_GAIN = -24.0f;
    private static final float MAX_GAIN = 24.0f;

    @BindView(R.id.iir_sub_1)
    Counter mSubCounter1;
    @BindView(R.id.iir_sub_2)
    Counter mSubCounter2;
    @BindView(R.id.iir_sub_3)
    Counter mSubCounter3;
    @BindView(R.id.iir_sub_4)
    Counter mSubCounter4;
    @BindView(R.id.iir_sub_5)
    Counter mSubCounter5;

    @BindView(R.id.iir_add_1)
    Counter mAddCounter1;
    @BindView(R.id.iir_add_2)
    Counter mAddCounter2;
    @BindView(R.id.iir_add_3)
    Counter mAddCounter3;
    @BindView(R.id.iir_add_4)
    Counter mAddCounter4;
    @BindView(R.id.iir_add_5)
    Counter mAddCounter5;

    @BindView(R.id.iir_chk_1)
    CheckBox mChk1;
    @BindView(R.id.iir_chk_2)
    CheckBox mChk2;
    @BindView(R.id.iir_chk_3)
    CheckBox mChk3;
    @BindView(R.id.iir_chk_4)
    CheckBox mChk4;
    @BindView(R.id.iir_chk_5)
    CheckBox mChk5;

    @BindView(R.id.iir_gain_1)
    EditText mIIRGain1;
    @BindView(R.id.iir_gain_2)
    EditText mIIRGain2;
    @BindView(R.id.iir_gain_3)
    EditText mIIRGain3;
    @BindView(R.id.iir_gain_4)
    EditText mIIRGain4;
    @BindView(R.id.iir_gain_5)
    EditText mIIRGain5;

    @BindView(R.id.iir_freq_1)
    EditText mIIRFreq1;
    @BindView(R.id.iir_freq_2)
    EditText mIIRFreq2;
    @BindView(R.id.iir_freq_3)
    EditText mIIRFreq3;
    @BindView(R.id.iir_freq_4)
    EditText mIIRFreq4;
    @BindView(R.id.iir_freq_5)
    EditText mIIRFreq5;

    @BindView(R.id.iir_q_1)
    EditText mIIRQ1;
    @BindView(R.id.iir_q_2)
    EditText mIIRQ2;
    @BindView(R.id.iir_q_3)
    EditText mIIRQ3;
    @BindView(R.id.iir_q_4)
    EditText mIIRQ4;
    @BindView(R.id.iir_q_5)
    EditText mIIRQ5;

    @BindView(R.id.iir_left_gain)
    EditText mLeftGain;
    @BindView(R.id.iir_right_gain)
    EditText mRightGain;

    @OnClick({
            R.id.enable,
            R.id.disable,
            R.id.save,
            R.id.load,
    })
    void onClick(View view) {
        switch (view.getId()) {
            case R.id.enable:
                enable();
                break;
            case R.id.disable:
                disable();
                break;
            case R.id.save:
                save();
                break;
            case R.id.load:
                load();
                break;
        }
    }

    private LeManager mLeManager;

    private EditText[] mGains = new EditText[EQConfig.PARAM_NUM];
    private EditText[] mFreqs = new EditText[EQConfig.PARAM_NUM];
    private EditText[] mQs = new EditText[EQConfig.PARAM_NUM];
    private CheckBox[] mChks = new CheckBox[EQConfig.PARAM_NUM];

    private EQConfig mEQConfig = new EQConfig();

    private static final int MSG_WRITE_CMD_FAILED = 0x00;
    private static final int MSG_WRITE_CMD_SUCCESS = 0x01;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WRITE_CMD_FAILED:
                    showToast("Set failed");
                    break;
                case MSG_WRITE_CMD_SUCCESS:
                    showToast("Set Successfully");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_eq);
        initView();
        initTouchListener();
    }

    private void initTouchListener(){
        mSubCounter1.setCounterTouchListener(this);
        mSubCounter2.setCounterTouchListener(this);
        mSubCounter3.setCounterTouchListener(this);
        mSubCounter4.setCounterTouchListener(this);
        mSubCounter5.setCounterTouchListener(this);

        mAddCounter1.setCounterTouchListener(this);
        mAddCounter2.setCounterTouchListener(this);
        mAddCounter3.setCounterTouchListener(this);
        mAddCounter4.setCounterTouchListener(this);
        mAddCounter5.setCounterTouchListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            mLeManager.close();
            mLeManager.removeConnectCallback(this);
        }
    }

    private void initView() {
        mLeManager = LeManager.getLeManager();
        mLeManager.addConnectCallback(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mGains[0] = mIIRGain1;
        mGains[1] = mIIRGain2;
        mGains[2] = mIIRGain3;
        mGains[3] = mIIRGain4;
        mGains[4] = mIIRGain5;

        mFreqs[0] = mIIRFreq1;
        mFreqs[1] = mIIRFreq2;
        mFreqs[2] = mIIRFreq3;
        mFreqs[3] = mIIRFreq4;
        mFreqs[4] = mIIRFreq5;

        mQs[0] = mIIRQ1;
        mQs[1] = mIIRQ2;
        mQs[2] = mIIRQ3;
        mQs[3] = mIIRQ4;
        mQs[4] = mIIRQ5;

        mChks[0] = mChk1;
        mChks[1] = mChk2;
        mChks[2] = mChk3;
        mChks[3] = mChk4;
        mChks[4] = mChk5;
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        if (!connected) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(getString(R.string.disconnected));
                    finish();
                }
            });
        }
    }

    @Override
    public void onReceive(byte[] data) {

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_LOAD_HISTORY_CONFIG:
                onRequestHistoryConfig(resultCode, data);
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onRequestHistoryConfig(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String name = data.getStringExtra(HistoryConfigActivity.EXTRA_CONFIG_NAME);
            mEQConfig = DataHelper.getDataHelper(this).getConfigByName(name);
            updateConfig();
        }
    }

    private void loadConfig() {
        mEQConfig.setLeftGain(Float.parseFloat(mLeftGain.getText().toString()));
        mEQConfig.setRightGain(Float.parseFloat(mRightGain.getText().toString()));
        for (int i = 0; i < mEQConfig.getParams().length; i++) {
            mEQConfig.getParams()[i].setGain(Float.parseFloat(mGains[i].getText().toString()));
            mEQConfig.getParams()[i].setFrequency(Float.parseFloat(mFreqs[i].getText().toString()));
            mEQConfig.getParams()[i].setQ(Float.parseFloat(mQs[i].getText().toString()));
            mEQConfig.getChks()[i] = mChks[i].isChecked();
        }
    }

    private void updateConfig() {
        mLeftGain.setText(String.format("%.2f", mEQConfig.getLeftGain()));
        mRightGain.setText(String.format("%.2f", mEQConfig.getRightGain()));

        mIIRGain1.setText(String.format("%.2f", mEQConfig.getParams()[0].getGain()));
        mIIRGain1.setText(String.format("%.2f", mEQConfig.getParams()[0].getGain()));
        mIIRGain1.setText(String.format("%.2f", mEQConfig.getParams()[0].getGain()));
        mIIRGain1.setText(String.format("%.2f", mEQConfig.getParams()[0].getGain()));
        mIIRGain1.setText(String.format("%.2f", mEQConfig.getParams()[0].getGain()));

        mIIRFreq1.setText(String.valueOf((int) mEQConfig.getParams()[0].getFrequency()));
        mIIRFreq1.setText(String.valueOf((int) mEQConfig.getParams()[0].getFrequency()));
        mIIRFreq1.setText(String.valueOf((int) mEQConfig.getParams()[0].getFrequency()));
        mIIRFreq1.setText(String.valueOf((int) mEQConfig.getParams()[0].getFrequency()));
        mIIRFreq1.setText(String.valueOf((int) mEQConfig.getParams()[0].getFrequency()));

        mIIRQ1.setText(String.format("%.1f", mEQConfig.getParams()[0].getQ()));
        mIIRQ1.setText(String.format("%.1f", mEQConfig.getParams()[0].getQ()));
        mIIRQ1.setText(String.format("%.1f", mEQConfig.getParams()[0].getQ()));
        mIIRQ1.setText(String.format("%.1f", mEQConfig.getParams()[0].getQ()));
        mIIRQ1.setText(String.format("%.1f", mEQConfig.getParams()[0].getQ()));

        for (int i = 0; i < EQConfig.PARAM_NUM; i++) {
            mChks[i].setChecked(mEQConfig.getChks()[i]);
        }
    }

    private void enable() {
        loadConfig();
        setIIR(mEQConfig.parse());
    }

    private void disable() {
        setIIR(EQConfig.DEFAULT_CONFIG);
    }

    private void save() {
        if (!checkStoragePermission())
            return;
        loadConfig();
        final EditText input = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.config_to_save)
                .setView(input)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        DataHelper helper = DataHelper.getDataHelper(EQActivity.this);
                        if (helper.isExist(name)) {
                            showToast(getString(R.string.config_exist_already));
                        } else {
                            helper.saveConfig(name, mEQConfig);
                            helper.saveFile(mEQConfig);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        input.setText(new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()));
        dialog.show();
    }

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 0x01;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 0x02;
    private static final int REQUEST_LOAD_HISTORY_CONFIG = 0x03;

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
            return false;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    private void load() {
        if (!checkStoragePermission()) {
            return;
        }
        startActivityForResult(new Intent(this, HistoryConfigActivity.class), REQUEST_LOAD_HISTORY_CONFIG);
    }

    private void setIIR(byte[] data) {
        new IIRSetterThread(data).start();
    }

    @Override
    public void onTouch(View view) {
        float gain;
        switch (view.getId()) {
            case R.id.iir_sub_1:
                gain = Float.parseFloat(mIIRGain1.getText().toString()) + mSubCounter1.getCountUnit();
                if (gain < MIN_GAIN) {
                    gain = MIN_GAIN;
                }
                mIIRGain1.setText(String.format("%.2f", gain));
                break;
            case R.id.iir_sub_2:
                gain = Float.parseFloat(mIIRGain2.getText().toString()) + mSubCounter2.getCountUnit();
                if (gain < MIN_GAIN) {
                    gain = MIN_GAIN;
                }
                mIIRGain2.setText(String.format("%.2f", gain));
                break;
            case R.id.iir_sub_3:
                gain = Float.parseFloat(mIIRGain3.getText().toString()) + mSubCounter3.getCountUnit();
                if (gain < MIN_GAIN) {
                    gain = MIN_GAIN;
                }
                mIIRGain3.setText(String.format("%.2f", gain));
                break;
            case R.id.iir_sub_4:
                gain = Float.parseFloat(mIIRGain4.getText().toString()) + mSubCounter4.getCountUnit();
                if (gain < MIN_GAIN) {
                    gain = MIN_GAIN;
                }
                mIIRGain4.setText(String.format("%.2f", gain));
                break;
            case R.id.iir_sub_5:
                gain = Float.parseFloat(mIIRGain5.getText().toString()) + mSubCounter5.getCountUnit();
                if (gain < MIN_GAIN) {
                    gain = MIN_GAIN;
                }
                mIIRGain5.setText(String.format("%.2f", gain));
                break;
            case R.id.iir_add_1:
                gain = Float.parseFloat(mIIRGain1.getText().toString()) + mAddCounter1.getCountUnit();
                if (gain > MAX_GAIN) {
                    gain = MAX_GAIN;
                }
                mIIRGain1.setText(String.format("%.2f", gain));
                break;
            case R.id.iir_add_2:
                gain = Float.parseFloat(mIIRGain2.getText().toString()) + mAddCounter2.getCountUnit();
                if (gain > MAX_GAIN) {
                    gain = MAX_GAIN;
                }
                mIIRGain2.setText(String.format("%.2f", gain));
                break;
            case R.id.iir_add_3:
                gain = Float.parseFloat(mIIRGain3.getText().toString()) + mAddCounter3.getCountUnit();
                if (gain > MAX_GAIN) {
                    gain = MAX_GAIN;
                }
                mIIRGain3.setText(String.format("%.2f", gain));
                break;
            case R.id.iir_add_4:
                gain = Float.parseFloat(mIIRGain4.getText().toString()) + mAddCounter4.getCountUnit();
                if (gain > MAX_GAIN) {
                    gain = MAX_GAIN;
                }
                mIIRGain4.setText(String.format("%.2f", gain));
                break;
            case R.id.iir_add_5:
                gain = Float.parseFloat(mIIRGain5.getText().toString()) + mAddCounter5.getCountUnit();
                if (gain > MAX_GAIN) {
                    gain = MAX_GAIN;
                }
                mIIRGain5.setText(String.format("%.2f", gain));
                break;
        }
    }

    private class IIRSetterThread extends Thread {
        byte[] mData;

        public IIRSetterThread(byte[] data) {
            mData = data;
        }

        @Override
        public void run() {
            Logger.e(TAG, "setIIR " + Arrays.toString(mData));
            byte[] cmd_start = new byte[]{0x01, 0x00, 0x00, 0x00};
            if (!mLeManager.write_no_rsp(cmd_start)) {
                mHandler.sendEmptyMessage(MSG_WRITE_CMD_FAILED);
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int baseIndex = 0;
            byte[] cmd_raw;
            while (baseIndex < mData.length) {
                int remained = mData.length - baseIndex + 2;
                cmd_raw = new byte[(remained > 20) ? 20 : remained];
                cmd_raw[0] = 0x00;
                cmd_raw[1] = 0x00;
                for (int j = 2; j < cmd_raw.length; j++) {
                    cmd_raw[j] = mData[baseIndex++];
                }
                if (!mLeManager.write_no_rsp(cmd_raw)) {
                    mHandler.sendEmptyMessage(MSG_WRITE_CMD_FAILED);
                    return;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            byte[] cmd_stop = new byte[]{0x02, 0x00, 0x00, 0x00};
            if (!mLeManager.write_no_rsp(cmd_stop)) {
                mHandler.sendEmptyMessage(MSG_WRITE_CMD_FAILED);
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            byte[] cmd_set_up = new byte[]{0x06, 0x00, 0x00, 0x00};
            if (!mLeManager.write_no_rsp(cmd_set_up)) {
                mHandler.sendEmptyMessage(MSG_WRITE_CMD_FAILED);
            } else {
                mHandler.sendEmptyMessage(MSG_WRITE_CMD_SUCCESS);
            }
        }
    }
}
