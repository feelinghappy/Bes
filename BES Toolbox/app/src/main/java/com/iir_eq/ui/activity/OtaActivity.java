package com.iir_eq.ui.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.iir_eq.R;
import com.iir_eq.bluetooth.BtHelper;
import com.iir_eq.bluetooth.callback.ConnectCallback;
import com.iir_eq.contants.Constants;
import com.iir_eq.ui.fragment.OtaConfigFragment;
import com.iir_eq.ui.fragment.OtaDaulPickFileFragment;
import com.iir_eq.util.ArrayUtil;
import com.iir_eq.util.LogUtils;
import com.iir_eq.util.Logger;
import com.iir_eq.util.ProfileUtils;
import com.iir_eq.util.SPHelper;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;


/**
 * Created by zhaowanxing on 2017/7/12.
 */

public abstract class OtaActivity extends BaseActivity implements ConnectCallback, View.OnClickListener {

    protected static final String KEY_OTA_FILE = "ota_file";

    protected static final byte[] OTA_PASS_RESPONSE = new byte[]{0x11, 0x22};
    protected static final byte[] OTA_RESEND_RESPONSE = new byte[]{0x33, 0x44};

    protected static final int DEFAULT_MTU = 512;

    private static final int REQUEST_OTA_FILE = 0X00;
    private static final int REQUEST_DEVICE = 0x01;

    private static final int REQUEST_OTA_FILE_DAUL = 0x02;

    private static final int MSG_UPDATE_INFO = 0x00;
    private static final int MSG_UPDATE_PROGRESS = 0x01;
    private static final int MSG_OTA_TIME_OUT = 0x02;
    private static final int MSG_SEND_INFO_TIME_OUT = 0x03;
    private static final int MSG_UPDATE_RESULT_INFO = 0x04;
    private static final int MSG_UPDATE_VERSION = 0x05;
    private static final int MSG_UPDATE_OTA_DAUL_FILE_INFO = 0x06;
    private static final int MSG_UPDATE_OTA_CONNECT_STATE = 0x07;
    private static final int MSG_UPDATE_BT_CONNECTED_ADDRESS= 0x08;
    private static final int MSG_UPDATE_BT_CONNECTED_NAME = 0x09;
    protected static final int CMD_CONNECT = 0x80;
    protected static final int CMD_DISCONNECT = 0x81;
    protected static final int CMD_LOAD_FILE = 0x82;
    protected static final int CMD_START_OTA = 0x83;
    protected static final int CMD_OTA_NEXT = 0x84;
    protected static final int CMD_SEND_FILE_INFO = 0x85;
    protected static final int CMD_LOAD_FILE_FOR_NEW_PROFILE = 0x86;
    protected static final int CMD_RESEND_MSG = 0X88;
    protected static final int CMD_LOAD_FILE_FOR_NEW_PROFILE_SPP = 0X89;
    //////////////////////////add by fxl 1227 begin//////////////////////////////////
    protected static final int CMD_RESUME_OTA_CHECK_MSG = 0X8C;   //resume
    protected static final int CMD_RESUME_OTA_CHECK_MSG_RESPONSE = 0X8D; //resume back
    protected static final int CMD_SEND_HW_INFO = 0X4C;
    protected static final int CMD_READ_CURRENT_VERSION_MSG = 0X8E;   //   read current version
    protected static final int CMD_READ_CURRENT_VERSION_RESPONSE = 0X8F; // read version response

    protected static final int CMD_APPLY_THE_IMAGE_MSG = 0X99;   //   apply the image


    protected static final int CMD_LOAD_OTA_CONFIG = 0x90;
    protected static final int CMD_START_OTA_CONFIG = 0x91;
    protected static final int CMD_OTA_CONFIG_NEXT = 0x92;

    protected static final int STATE_IDLE = 0x00;
    protected static final int STATE_CONNECTING = 0x01;
    protected static final int STATE_CONNECTED = 0x02;
    protected static final int STATE_DISCONNECTING = 0x03;
    protected static final int STATE_DISCONNECTED = 0x04;
    protected static final int STATE_OTA_ING = 0x05;
    protected static final int STATE_OTA_FAILED = 0x06;
    protected static final int STATE_OTA_CONFIG = 0x07;
    protected static final int STATE_BUSY = 0x0F;

    protected static final byte IMAGE_STEREO = (byte) 0x00;
    protected static final byte IMAGE_LEFT_EARBUD = (byte) 0x01;
    protected static final byte IMAGE_RIGHT_EARBUD = (byte) 0x10;
    protected static final byte IMAGE_BOTH_EARBUD_IN_ONE = (byte) 0x11;

    protected static final int APPLY_STEREO = -1;
    protected static final int APPLY_LEFT_EARBUD_ONLY = 0;
    protected static final int APPLY_RIGHT_EARBUD_ONLY = 1;
    protected static final int APPLY_BOTH_EARBUD_IN_ONE = 2;
    protected static final int APPLY_BOTH_EARBUD_IN_TWO = 3;

    protected static final int DAUL_CONNECT_LEFT = 1;
    protected static final int DAUL_CONNECT_RIGHT = 2;
    protected static final int DAUL_CONNECT_STEREO = 0;

    protected volatile int mDaulConnectState = DAUL_CONNECT_STEREO;//stereo



    protected volatile int mState = STATE_IDLE;
    protected BluetoothDevice mDevice;

    protected boolean mExit = false;

    protected HandlerThread mCmdThread;
    protected CmdHandler mCmdHandler;
    protected byte[] mOtaResumeDataReq;
    protected byte[] mOtaResumeData;

    protected byte[][][] mOtaData;
    protected int mOtaPacketCount = 0;
    protected int mOtaPacketItemCount = 0;
    protected boolean mSupportNewOtaProfile = false;

    protected byte[][] mOtaConfigData;
    protected int mOtaConfigPacketCount = 0;

    protected int totalPacketCount = 0;

    protected Object mOtaLock = new Object();

    protected int mMtu;

    protected volatile boolean mWritten = true;

    private final String OTA_CONFIG_TAG = "ota_config";
    private OtaConfigFragment mOtaConfigDialog;

    private final String OTA_DAUL_PICK_FILE = "ota_daul_pick_file";
    private OtaDaulPickFileFragment motaDaulPickFileFragment;

    protected long castTime;//temp data for log
    protected long sendMsgFailCount = 0;//temp data for log
    protected long otaImgSize = 0;

    protected final int RECONNECT_MAX_TIMES = 5; // 5 times
    protected final int RECONNECT_SPAN = 3000; // 3 seconds
    protected int reconnectTimes = 0;

    protected int totalCount = 0;
    protected int failedCount = 0;
    protected int resumeSegment = 0;

    protected boolean resumeFlg = false;

    protected int segment_verify_error_time = 0;
    TextView mAddress;
    TextView mName;
    TextView mOtaFile;
    TextView mOtaInfo;
    TextView mUpdateStatic;
    ProgressBar mOtaProgress;
    TextView mOtaStatus;
    Button pickDevice, pickOtaFile, startOta;
    private int daulApply = -1; //-1为stereo状态 0，1，2，3为daul状态 5是待定状态
    private byte imageApply = 0x00;

    TextView send_details_info;
    Button mOtaConnectDevice;
    private int daul_step = -1;
    private boolean stereo_flg = true;
    private int dual_in_one_response_ok_time = 0;
	private String mOtaIngFile = "";
    private BluetoothAdapter bluetoothAdapter = null;

    private TextView mcurrentVersionDetails;

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.pick_device:
                if (isIdle()) {
                    pickDevice(REQUEST_DEVICE);
                }
                break;
            case R.id.pick_ota_file:
                if (daulApply == -1) {
                    if (isIdle()) {
                        pickFile(REQUEST_OTA_FILE);
                    }
                } else {
                    showChooseApplyDialog();
                }
                break;
            case R.id.start_ota:
                Log.e("start_ota", daulApply + "");
                if (daulApply != -1) {
                    readyOtaDaul();
                } else {
                    readyOta();
                }
                break;

            case R.id.connect_device_ota:
                connectDevice();
                break;


        }
    }

    void readyOtaDaul() {
        if (TextUtils.isEmpty(mAddress.getText())) {
            showToast(getString(R.string.pick_device_tips));
            return;
        }
        if (TextUtils.isEmpty(mOtaFile.getText())) {
            showToast(getString(R.string.pick_File_tips));
            return;
        }
        if (mState == STATE_CONNECTED && (daulApply != -1)) {
            mOtaConfigDialog.show(getSupportFragmentManager(), OTA_CONFIG_TAG);
        }
    }

    void connectDevice() {
        if (!((mAddress.getText() == "--") || mAddress.getText() == null)) {
            sendCmdDelayed(CMD_CONNECT, 0);
        } 
		else if(getConnectBt()==-1)
		{
		    showToast("请在设置中选择需要升级的耳机并配对");
		}
		else 
		{
            showToast("请在设置中选择需要升级的耳机并配对");
        }

    }


    private void initView() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mAddress = (TextView) findViewById(R.id.address);
        mName = (TextView) findViewById(R.id.name);
        mOtaFile = (TextView) findViewById(R.id.ota_file);
        mOtaInfo = (TextView) findViewById(R.id.ota_info);
        mUpdateStatic = (TextView) findViewById(R.id.update_static);
        mOtaProgress = (ProgressBar) findViewById(R.id.ota_progress);
        mOtaStatus = (TextView) findViewById(R.id.ota_status);
        pickDevice = (Button) findViewById(R.id.pick_device);
        pickOtaFile = (Button) findViewById(R.id.pick_ota_file);
        startOta = (Button) findViewById(R.id.start_ota);
        mcurrentVersionDetails = (TextView) findViewById(R.id.current_version_details);
        mOtaConnectDevice = (Button) findViewById(R.id.connect_device_ota);
        pickDevice.setOnClickListener(this);
        pickOtaFile.setOnClickListener(this);
        startOta.setOnClickListener(this);
        mOtaConnectDevice.setOnClickListener(this);


        mOtaFile.setText(SPHelper.getPreference(this, KEY_OTA_FILE, "").toString());
        mName.setText(loadLastDeviceName());
        mAddress.setText(loadLastDeviceAddress());
        mDevice = BtHelper.getRemoteDevice(this, mAddress.getText().toString());

        mOtaConfigDialog = new OtaConfigFragment();
        mOtaConfigDialog.setOtaConfigCallback(mOtaConfigCallback);


        motaDaulPickFileFragment = new OtaDaulPickFileFragment();
        motaDaulPickFileFragment.setPickFileCallback(motaPickFileCallback);

        int i = getConnectBt();
        if(!(i==-1))
        {
            getConnectBtDetails(i);
        }

    }

    private Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_INFO:
                    LOG(TAG, "MSG_UPDATE_INFO");
                    mOtaInfo.setText(msg.obj.toString());
                    break;
                case MSG_UPDATE_RESULT_INFO:
                    LOG(TAG, "MSG_UPDATE_RESULT_INFO");
                    if (mUpdateStatic != null) {
                        mUpdateStatic.setText(msg.obj.toString());
                    }
                    break;
                case MSG_UPDATE_PROGRESS:
                    LOG(TAG, "MSG_UPDATE_PROGRESS");
                    if (mOtaProgress != null) {
                        mOtaProgress.setProgress((Integer) msg.obj);
                        mOtaStatus.setText((Integer) msg.obj + "%");
                    } else {
                        LOG(TAG, "mOtaProgress is null");
                    }
                    break;
                case MSG_OTA_TIME_OUT:
                case MSG_SEND_INFO_TIME_OUT:
                    Log.e("OtaActivity", "MSG_SEND_INFO_TIME_OUT|MSG_SEND_INFO_TIME_OUT time out");//add by fxl 1226
                    LOG(TAG, "MSG_SEND_INFO_TIME_OUT|MSG_SEND_INFO_TIME_OUT time out");
                    if (mOtaInfo != null) {
                        mOtaInfo.setText(msg.arg1);
                    } else {
                        LOG(TAG, "mOtaInfo is null");
                    }
                    sendCmdDelayed(msg.arg2, 0);
                    break;
                case MSG_UPDATE_VERSION:
                    LOG(TAG, "MSG_UPDATE_VERSION");
                    mcurrentVersionDetails.setText(msg.obj.toString());
                    break;

                case MSG_UPDATE_OTA_DAUL_FILE_INFO:
                    LOG(TAG, "MSG_UPDATE_OTA_DAUL_FILE_INFO");
                    HandleOtaFileShow();
                    break;
                case MSG_UPDATE_OTA_CONNECT_STATE:
                    LOG(TAG, "MSG_UPDATE_OTA_DAUL_FILE_INFO");
                    if (msg.obj.toString().equals("true")) {
                        mOtaConnectDevice.setVisibility(View.GONE);
                    } else {
                        mOtaConnectDevice.setVisibility(View.VISIBLE);
						mcurrentVersionDetails.setText("--");
                    }
                    break;
                case MSG_UPDATE_BT_CONNECTED_ADDRESS:
                    LOG(TAG, "MSG_UPDATE_BT_CONNECTED_ADDRESS");
                    mAddress.setText(msg.obj.toString());
                   
                    break;
					case MSG_UPDATE_BT_CONNECTED_NAME:
                    LOG(TAG, "MSG_UPDATE_BT_CONNECTED_NAME");
                    mName.setText(msg.obj.toString());
                    break;



                default:// donot left the default , even nothing to do
            }
        }
    };

    private void HandleOtaFileShow() {
        String details = "";
        String tmp;
        if (daulApply == APPLY_LEFT_EARBUD_ONLY) {
            details = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
            details = getString(R.string.left_earbud_only) + ":\n" + details;
            mOtaFile.setText(details);
            imageApply = IMAGE_LEFT_EARBUD;
            daul_step = -1;
        } else if (daulApply == APPLY_RIGHT_EARBUD_ONLY) {
            details = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
            details = getString(R.string.right_earbud_only) + ":\n" + details;
            mOtaFile.setText(details);
            imageApply = IMAGE_RIGHT_EARBUD;
            daul_step = -1;
        } else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE) {
            details = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
            details = getString(R.string.both_earbuds_in_one_bin) + ":\n" + details;
            mOtaFile.setText(details);
            imageApply = IMAGE_BOTH_EARBUD_IN_ONE;
            daul_step = -1;
            dual_in_one_response_ok_time = 0;
        } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO) {
            details = getString(R.string.both_earbuds_in_two_bins) + "\n";
            tmp = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
            details = details + getString(R.string.left_earbud_image) + ":" + tmp + "\n";
            tmp = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
            details = details + getString(R.string.right_earbud_image) + ":" + tmp;
            mOtaFile.setText(details);
            daul_step = 0;
        }
    }

    private boolean isIdle() {
        return mState == STATE_IDLE || mState == STATE_OTA_FAILED || mState == STATE_DISCONNECTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG(TAG, "onCreate");
        setContentView(R.layout.act_ota);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initView();
        initConfig();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    protected void initConfig() {
        mCmdThread = new HandlerThread(TAG);
        mCmdThread.start();
        mCmdHandler = new CmdHandler(mCmdThread.getLooper());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LOG(TAG, "onDestroy");
        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSG_SEND_INFO_TIME_OUT);
            mMsgHandler.removeMessages(MSG_OTA_TIME_OUT);
        }
        if (mCmdHandler != null) {
            mCmdHandler.removeMessages(CMD_OTA_NEXT);
            mCmdHandler.removeMessages(CMD_OTA_CONFIG_NEXT);
        }
        if (mCmdThread != null && mCmdThread.isAlive()) {
            mCmdThread.quit();
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                exit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void exit() {
        LOG(TAG, "exit");
        if (mState == STATE_IDLE) {
            finish();
        } else {
            showConfirmDialog(R.string.ota_exit_tips, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    exitOta();
                    finish();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOG(TAG, "onActivityResult");
        if (requestCode == REQUEST_OTA_FILE) {
            onPickFile(resultCode, data);
        } else if (requestCode == REQUEST_DEVICE) {
            onPickDevice(resultCode, data);
        }
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        LOG(TAG, "onConnectionStateChanged " + connected + "; " + mState);
        if (connected) {
            onConnected();

        } else {
            removeTimeout();
            if (mState == STATE_CONNECTING) {
                updateConnectState("true");
                LOG(TAG, "mState == STATE_CONNECTING");
                reconnectTimes++;
                if (reconnectTimes > RECONNECT_MAX_TIMES) {
                    updateInfo(R.string.connect_failed);
                    mState = STATE_DISCONNECTED;
                    onOtaFailed();
                } else {
                    updateInfo(String.format(getString(R.string.connect_reconnect_try), reconnectTimes));
                    reconnect();
                }
            } else if (mState == STATE_OTA_ING) {
                updateConnectState("true");
                LOG(TAG, "mState == STATE_OTA_ING");
                onOtaFailed();
            } else if (mState != STATE_IDLE) {
                LOG(TAG, "mState != STATE_IDLE");
                updateInfo(R.string.disconnected);
                mState = STATE_DISCONNECTED;
                updateConnectState("false");
                onOtaFailed();
            }
			else if(mState == STATE_IDLE)
			{
			    LOG(TAG, "mState == STATE_IDLE");
                updateInfo(R.string.disconnected);
                mState = STATE_DISCONNECTED;
                updateConnectState("false");
			}
        }
    }

    protected void updateInfo(int info) {
        updateInfo(getString(info));
    }

    protected void updateResultInfo(String info) {
        Message message = mMsgHandler.obtainMessage(MSG_UPDATE_RESULT_INFO);
        message.obj = info;
        mMsgHandler.sendMessage(message);
    }

    protected void updateInfo(String info) {
        Message message = mMsgHandler.obtainMessage(MSG_UPDATE_INFO);
        message.obj = info;
        mMsgHandler.sendMessage(message);
    }

    protected void updateVersion(String info) {
        Message message = mMsgHandler.obtainMessage(MSG_UPDATE_VERSION);
        message.obj = info;
        mMsgHandler.sendMessage(message);
    }

    protected void updateDaulFile(String info) {
        Message message = mMsgHandler.obtainMessage(MSG_UPDATE_OTA_DAUL_FILE_INFO);
        message.obj = info;
        mMsgHandler.sendMessage(message);
    }

    protected void updateConnectState(String info) {
        Message message = mMsgHandler.obtainMessage(MSG_UPDATE_OTA_CONNECT_STATE);
        message.obj = info;
        mMsgHandler.sendMessage(message);
    }


    protected void updateProgress(int progress) {
        Message message = mMsgHandler.obtainMessage(MSG_UPDATE_PROGRESS);
        message.obj = progress;
        mMsgHandler.sendMessage(message);
    }

    protected void updateConnectedBtAddress(String address) {
        Message message = mMsgHandler.obtainMessage(MSG_UPDATE_BT_CONNECTED_ADDRESS);
        message.obj = address;
        mMsgHandler.sendMessage(message);
    }

    protected void updateConnectedBtName(String name) {
        Message message = mMsgHandler.obtainMessage(MSG_UPDATE_BT_CONNECTED_NAME);
        message.obj = name;
        mMsgHandler.sendMessage(message);
    }




    protected void sendCmdDelayed(int cmd, long millis) {
        mCmdHandler.removeMessages(cmd);
        if (millis == 0) {
            mCmdHandler.sendEmptyMessage(cmd);
        } else {
            mCmdHandler.sendEmptyMessageDelayed(cmd, millis);
        }
    }

    protected void sendTimeout(int info, int cmd, long millis) {
        LOG(TAG, "sendTimeout info " + info + " ; cmd " + cmd + " ; millis " + millis);
        Message message = mMsgHandler.obtainMessage(MSG_OTA_TIME_OUT);
        message.arg1 = info;
        message.arg2 = cmd;
        mMsgHandler.sendMessageDelayed(message, millis);
    }

    protected void removeTimeout() {
        LOG(TAG, "removeTimeout");
        mMsgHandler.removeMessages(MSG_OTA_TIME_OUT);
    }

    private void onPickDevice(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            mDevice = data.getParcelableExtra(LeScanActivity.EXTRA_DEVICE);
            if (mDevice != null) {
                saveLastDeviceName(mDevice.getName());
                saveLastDeviceAddress(mDevice.getAddress());
                mAddress.setText(mDevice.getAddress());
                mName.setText(mDevice.getName());
            }
        }
    }

    private void onPickFile(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            mOtaData = null;
            String file = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            SPHelper.putPreference(this, KEY_OTA_FILE, file);
            mOtaFile.setText(file);

            //sendCmdDelayed(CMD_CONNECT, 0);
        }
    }

    protected void onConnecting() {
        LOG(TAG, "onConnecting");
        LogUtils.writeForOTAStatic(TAG, "onConnecting ");
        castTime = System.currentTimeMillis();//startTime
        updateInfo(R.string.connecting_device);
        mState = STATE_CONNECTING;
    }

    protected void onConnected() {
        LOG(TAG, "onConnected");
        /*
        sendCmdDelayed(CMD_SEND_FILE_INFO, 0);
        LogUtils.writeForOTAStatic(TAG , "onConnected ");
        updateInfo(R.string.connected);
        mState = STATE_CONNECTED;
        reconnectTimes = 0 ;*///////fxl 1227 connect连接上以后需要先check flash信息

        ///////////////////////////////////////////////////////////////////////////////////
        /*sendCmdDelayed(CMD_RESUME_OTA_CHECK_MSG, 0);
        LogUtils.writeForOTAStatic(TAG , "onConnected ");
        updateInfo(R.string.connected);
        mState = STATE_CONNECTED;
        reconnectTimes = 0 ;*/
        updateConnectState("true");
        sendCmdDelayed(CMD_SEND_HW_INFO, 0);
        LogUtils.writeForOTAStatic(TAG, "onConnected ");
        updateInfo(R.string.connected);
        mState = STATE_CONNECTED;
        reconnectTimes = 0;


    }

    protected void onConnectFailed() {
        LOG(TAG, "onConnectFailed");
        LogUtils.writeForOTAStatic(TAG, "onConnectFailed " + ((System.currentTimeMillis() - castTime) / 1000));
        updateInfo(R.string.connect_failed);
        mState = STATE_DISCONNECTED;
    }

    protected void onLoadFileFailed() {
        LOG(TAG, "onLoadFileFailed");
        updateInfo(R.string.load_file_failed);
    }

    protected void onLoadFileSuccessfully() {
        LOG(TAG, "onLoadFileSuccessfully");
        updateInfo(R.string.load_file_successfully);
        sendCmdDelayed(CMD_START_OTA, 0);
    }

    protected void onLoadOtaConfigFailed() {
        LOG(TAG, "onLoadOtaConfigFailed");
        updateInfo(R.string.load_ota_config_failed);
    }

    protected void onLoadOtaConfigSuccessfully() {
        LOG(TAG, "onLoadOtaConfigSuccessfully");
        updateInfo(R.string.load_ota_config_successfully);
        sendCmdDelayed(CMD_START_OTA_CONFIG, 0);
    }

    protected void onOtaOver() {
        LOG(TAG, "onOtaOver");
        Log.e("OtaActivity", "onOtaOver");
        totalCount++;
        String result = "Result：Total count = " + totalCount + "  Failure count = " + failedCount;
        updateResultInfo(result);
        Log.e("OtaActivity", result);
        int updateTime = (int) ((System.currentTimeMillis() - castTime) / 1000);
        String msg = "Successful time-cost " + updateTime + " s" + " Retransmission count " + sendMsgFailCount + " Speed :" + otaImgSize / (updateTime == 0 ? otaImgSize : updateTime) + " B/s";
        LogUtils.writeForOTAStatic(TAG, msg);
        msg = "Successful time-cost " + updateTime + " s" + " Speed :" + otaImgSize / (updateTime == 0 ? otaImgSize : updateTime) + " B/s";
        updateInfo(msg);
        Log.e("OtaActivity", msg);
        updateProgress(100);
        mOtaPacketCount = 0;
        resumeFlg = false;
        mOtaPacketItemCount = 0;
        mOtaConfigPacketCount = 0;
        mState = STATE_IDLE;
    }

	protected void onOtaOverDaulOneStep() {
        LOG(TAG, "onOtaOverDaulOneStep");
        Log.e("OtaActivity", "onOtaOverDaulOneStep");
        totalCount++;
        String result = "Result：Total count = " + totalCount + "  Failure count = " + failedCount;
        updateResultInfo(result);
        updateProgress(100);
        mOtaPacketCount = 0;
        mOtaPacketItemCount = 0;
        mOtaConfigPacketCount = 0;
		resumeFlg = false;
    }

    protected void onOtaFailed() {
        Log.e("OtaActivity", "onOtaFailed");
        totalCount++;
        failedCount++;
        String result = "Result：Total count = " + totalCount + "  Failure count = " + failedCount;
        updateResultInfo(result);
        int updateTime = (int) ((System.currentTimeMillis() - castTime) / 1000);
        String msg = "Failed time-cost " + updateTime + " s" + " Retransmission count " + sendMsgFailCount + " Speed :" + otaImgSize / (updateTime == 0 ? otaImgSize : updateTime) + " B/s";
        LogUtils.writeForOTAStatic(TAG, msg);
        Log.e("OtaActivity", msg);
        msg = "Failed time-cost " + updateTime + " s" + " Speed :" + otaImgSize / (updateTime == 0 ? otaImgSize : updateTime) + " B/s";
        updateInfo(msg);
        reconnectTimes = 0;
        mOtaPacketCount = 0;
        resumeFlg = false;
        mOtaPacketItemCount = 0;
        mOtaConfigPacketCount = 0;
        mState = STATE_OTA_FAILED;
    }

    protected void onOtaConfigFailed() {
        updateInfo(R.string.ota_config_failed);
        mOtaConfigData = null;
        mOtaConfigPacketCount = 0;
        mOtaConfigPacketCount = 0;
        mState = STATE_IDLE;
    }

    protected void onWritten() {
        mWritten = true;
        LOG(TAG, "onWritten mWritten = true");
    }

    protected void sendBreakPointCheckReq() {
        LOG("OtaActivity", "sendBreakPointCheckReq");
        try {
            mOtaResumeDataReq = new byte[37 + 4 + 4];//依据协议BES_OTA_SPE_3.0.docx
            String randomCodestr = null;
            byte[] randomCode = new byte[32];
            randomCodestr = (String) SPHelper.getPreference(this, Constants.KEY_OTA_RESUME_VERTIFY_RANDOM_CODE, "");
            if (randomCodestr == null || randomCodestr == "") {
                for (int i = 0; i < randomCode.length; i++) {
                    randomCode[i] = (byte) 0x01;
                    mOtaResumeDataReq[i + 5] = (byte) 0x01;
                }
                SPHelper.putPreference(this.getApplicationContext(), Constants.KEY_OTA_RESUME_VERTIFY_RANDOM_CODE, ArrayUtil.toHex(randomCode));
                Log.e("null fanxiaoli", ArrayUtil.toHex(randomCode));
            } else {
                Log.e("randomCodestr", randomCodestr);
                randomCode = ArrayUtil.toBytes(randomCodestr);
                Log.e("sendBreakPoCodestr", "randomCodestr:" + randomCodestr);
                Log.e("sendBreak PorandomCode", "fanxiaoli:" + ArrayUtil.toHex(randomCode));
                for (int i = 0; i < randomCode.length; i++) {
                    mOtaResumeDataReq[i + 5] = randomCode[i];
                }
                Log.e("mOtaResumeDatnormally", "" + ArrayUtil.toHex(mOtaResumeDataReq));
            }
            mOtaResumeDataReq[0] = (byte) 0x8C;
            mOtaResumeDataReq[1] = (byte) 0x42;
            mOtaResumeDataReq[2] = (byte) 0x45;
            mOtaResumeDataReq[3] = (byte) 0x53;
            mOtaResumeDataReq[4] = (byte) 0x54;
            mOtaResumeDataReq[37] = (byte) 0x01;
            mOtaResumeDataReq[38] = (byte) 0x02;
            mOtaResumeDataReq[39] = (byte) 0x03;
            mOtaResumeDataReq[40] = (byte) 0x04;
            byte[] crc32_data = new byte[36];
            for (int i = 0; i < crc32_data.length; i++) {
                crc32_data[i] = mOtaResumeDataReq[i + 5];
            }

            long crc32 = ArrayUtil.crc32(crc32_data, 0, 36);
            mOtaResumeDataReq[41] = (byte) crc32;
            mOtaResumeDataReq[42] = (byte) (crc32 >> 8);
            mOtaResumeDataReq[43] = (byte) (crc32 >> 16);
            mOtaResumeDataReq[44] = (byte) (crc32 >> 24);
            Log.e("mOtaResumeDataReq", "" + ArrayUtil.toHex(mOtaResumeDataReq));
            updateInfo(R.string.resume_request_verify);
            sendData(mOtaResumeDataReq);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception", e.getMessage().toString());
        } finally {

        }
    }

    protected void sendCmdSendHWInfo() {
        try {
            byte[] send = new byte[5];
            send[0] = (byte) 0x8C;
            send[1] = (byte) 0x42;
            send[2] = (byte) 0x45;
            send[3] = (byte) 0x53;
            send[4] = (byte) 0x54;
            Log.e("sendCmdSendHWInfo send", ArrayUtil.toHex(send));
            send_details_info.setText(ArrayUtil.toHex(send));
            sendData(send);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void sendFileInfo() {
        LOG(TAG, "sendFileInfo MSG_SEND_INFO_TIME_OUT");
        Log.e("daulApply", daulApply + "" + "sendFileInfo");
        String file_path = "";
        FileInputStream inputStream = null;
        try {
            if (stereo_flg == true) {
                inputStream = new FileInputStream(mOtaFile.getText().toString());
            } else if (daulApply == APPLY_LEFT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                Log.e("daulApply==0", file_path);
                inputStream = inputStream = new FileInputStream(file_path);
            } else if (daulApply == APPLY_RIGHT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==1", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==2", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 0) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else if(mDaulConnectState == DAUL_CONNECT_RIGHT)
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
                Log.e("daulApply==3 s 0", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 1) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
                	Log.e("daulApply==3 s 1", file_path);
				}
            } else {
                showToast("请选择升级方式");
                return;
            }
			mOtaIngFile = file_path;
            int totalSize = inputStream.available();
            otaImgSize = totalSize; //TODO :TEMP ADD
            int dataSize = totalSize - 4;
            byte[] data = new byte[dataSize];
            inputStream.read(data, 0, dataSize);
            long crc32 = ArrayUtil.crc32(data, 0, dataSize);
            Message message = mMsgHandler.obtainMessage(MSG_SEND_INFO_TIME_OUT);
            message.arg1 = R.string.old_ota_profile;
            message.arg2 = CMD_LOAD_FILE;
            mMsgHandler.sendMessageDelayed(message, 5000);
            sendData(new byte[]{(byte) 0x80, 0x42, 0x45, 0x53, 0x54, (byte) dataSize, (byte) (dataSize >> 8), (byte) (dataSize >> 16), (byte) (dataSize >> 24), (byte) crc32, (byte) (crc32 >> 8), (byte) (crc32 >> 16), (byte) (crc32 >> 24)});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadOtaConfig() {
        Logger.e(TAG, "loadOtaConfig");
        String file_path = "";
        FileInputStream inputStream = null;
        try {
            if (stereo_flg == true) {
                inputStream = new FileInputStream(mOtaFile.getText().toString());
            } else if (daulApply == APPLY_LEFT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                Log.e("daulApply==0", file_path);
                inputStream = inputStream = new FileInputStream(file_path);
            } else if (daulApply == APPLY_RIGHT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==1", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==2", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 0) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else if(mDaulConnectState == DAUL_CONNECT_RIGHT)
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
                Log.e("daulApply==3 s 0", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 1) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
                	Log.e("daulApply==3 s 1", file_path);
				}
            }else {
                showToast("请选择升级方式");
                return;
            }
			mOtaIngFile = file_path;
            int totalSize = inputStream.available();
            int dataSize = totalSize;
            byte[] data = new byte[dataSize];
            inputStream.read(data, 0, dataSize);

            int configLength = 92;
            byte[] config = new byte[configLength];
            int lengthOfFollowingData = configLength - 4;
            config[0] = (byte) lengthOfFollowingData;
            config[1] = (byte) (lengthOfFollowingData >> 8);
            config[2] = (byte) (lengthOfFollowingData >> 16);
            config[3] = (byte) (lengthOfFollowingData >> 24);
            config[4] = data[dataSize - 4];
            config[5] = data[dataSize - 3];
            config[6] = data[dataSize - 2];

            boolean clearUserData = (boolean) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_CLEAR_USER_DATA, Constants.DEFAULT_CLEAR_USER_DATA);
            boolean updateBtAddress = (boolean) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_UPDATE_BT_ADDRESS, Constants.DEFAULT_UPDATE_BT_ADDRESS);
            boolean updateBtName = (boolean) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_UPDATE_BT_NAME, Constants.DEFAULT_UPDATE_BT_NAME);
            boolean updateBleAddress = (boolean) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_UPDATE_BLE_ADDRESS, Constants.DEFAULT_UPDATE_BLE_ADDRESS);
            boolean updateBleName = (boolean) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_UPDATE_BLE_NAME, Constants.DEFAULT_UPDATE_BLE_NAME);
            byte enable = 0x00;
            enable |= (clearUserData ? 0x01 : 0x00);
            enable |= (updateBtName ? (0x01 << 1) : 0x00);
            enable |= (updateBleName ? (0x01 << 2) : 0x00);
            enable |= (updateBtAddress ? (0x01 << 3) : 0x00);
            enable |= (updateBleAddress ? (0x01 << 4) : 0x00);
            config[8] = enable;
            if (updateBtName) {
                String btName = (String) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_UPDATE_BT_NAME_VALUE, "");
                byte[] btNameBytes = btName.getBytes();
                int btNameLength = btNameBytes.length;
                if (btNameLength > 32) {
                    btNameLength = 32;
                }
                for (int i = 0; i < btNameLength; i++) {
                    config[12 + i] = btNameBytes[i];
                }
            }
            if (updateBleName) {
                String bleName = (String) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_UPDATE_BLE_NAME_VALUE, "");
                byte[] bleNameBytes = bleName.getBytes();
                int bleNameLength = bleNameBytes.length;
                if (bleNameLength > 32) {
                    bleNameLength = 32;
                }
                for (int i = 0; i < bleNameLength; i++) {
                    config[44 + i] = bleNameBytes[i];
                }
            }
            if (updateBtAddress) {
                String btAddress = (String) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_UPDATE_BT_ADDRESS_VALUE, "");
                for (int i = 0; i < 6; i++) {
                    config[76 + 5 - i] = Integer.valueOf(btAddress.substring(i, i * 2 + 2), 16).byteValue();
                }
            }
            if (updateBleAddress) {
                String bleAddress = (String) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_UPDATE_BLE_ADDRESS_VALUE, "");
                for (int i = 0; i < 6; i++) {
                    config[82 + 5 - i] = Integer.valueOf(bleAddress.substring(i, i * 2 + 2), 16).byteValue();
                }
            }
            long crc32 = ArrayUtil.crc32(config, 0, configLength - 4);
            config[88] = (byte) crc32;
            config[89] = (byte) (crc32 >> 8);
            config[90] = (byte) (crc32 >> 16);
            config[91] = (byte) (crc32 >> 24);

            int mtu = getMtu();
            int packetPayload = mtu - 1;
            int packetCount = (configLength + packetPayload - 1) / packetPayload;
            mOtaConfigData = new byte[packetCount][];
            int position = 0;
            for (int i = 0; i < packetCount; i++) {
                if (position + packetPayload > configLength) {
                    packetPayload = configLength - position;
                }
                mOtaConfigData[i] = new byte[packetPayload + 1];
                mOtaConfigData[i][0] = (byte) 0x86;
                System.arraycopy(config, position, mOtaConfigData[i], 1, packetPayload);
                position += packetPayload;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mOtaConfigData = null;
        } catch (IOException e) {
            e.printStackTrace();
            mOtaConfigData = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mOtaConfigData != null) {
            onLoadOtaConfigSuccessfully();
        } else {
            onLoadOtaConfigFailed();
        }
    }

    protected void loadFileForNewProfile() {
        LOG(TAG, "loadFileForNewProfile");
        String file_path = "";
        mSupportNewOtaProfile = true;
        FileInputStream inputStream = null;
        try {
            if (stereo_flg == true) {
                inputStream = new FileInputStream(mOtaFile.getText().toString());
            } else if (daulApply == APPLY_LEFT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                Log.e("daulApply==0", file_path);
                inputStream = inputStream = new FileInputStream(file_path);
            } else if (daulApply == APPLY_RIGHT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==1", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==2", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 0) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else if(mDaulConnectState == DAUL_CONNECT_RIGHT)
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
                Log.e("daulApply==3 s 0", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 1) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
                	Log.e("daulApply==3 s 1", file_path);
				}
            }else {
                showToast("请选择升级方式");
                return;
            }
			mOtaIngFile = file_path;
            int totalSize = inputStream.available();
            int dataSize = totalSize - 4;
            int mtu = getMtu();
            int packetPayload = ProfileUtils.calculateBLESinglePacketLen(dataSize, mtu, isBle());
            int totalPacketCount = (dataSize + packetPayload - 1) / packetPayload;
            int onePercentBytes = ProfileUtils.calculateBLEOnePercentBytes(dataSize);
            int crcCount = (dataSize + onePercentBytes - 1) / onePercentBytes;
            this.totalPacketCount = totalPacketCount;
            mOtaData = new byte[crcCount + 1][][];
            Logger.e(TAG, "new profile imgeSize: " + dataSize + "; totalPacketCount " + totalPacketCount + "; onePercentBytes " + onePercentBytes + "; crcCount " + crcCount);
            byte[] data = new byte[dataSize];
            inputStream.read(data, 0, dataSize);
            int position = 0;
            for (int i = 0; i < crcCount; i++) {
                int crcBytes = onePercentBytes; //要校验百分之一的数据量
                if (packetPayload == 0) {
                    Log.e(TAG, ">>");
                }
                int length = (crcBytes + packetPayload - 1) / packetPayload; //根据MTU ，算出百分之一需要多少个包满足要求
                if (crcCount - 1 == i) { // 最后一包取余数
                    crcBytes = dataSize - position;
                    length = (crcBytes + packetPayload - 1) / packetPayload;
                }
                //LOG(TAG, "CRC BYTES = " + crcBytes);//by fxl 20190117
                mOtaData[i] = new byte[length + 1][]; //加 1 为增加最后结束整包校验命令
                int realySinglePackLen = 0;
                int crcPosition = position;
                int tempCount = 0;
                for (int j = 0; j < length; j++) {
                    realySinglePackLen = packetPayload;
                    if (j == length - 1) { //每百分之一的最后一包取余数
                        realySinglePackLen = (crcBytes % packetPayload == 0) ? packetPayload : crcBytes % packetPayload;
                    }
                    mOtaData[i][j] = new byte[realySinglePackLen + 1];
                    System.arraycopy(data, position, mOtaData[i][j], 1, realySinglePackLen);
                    mOtaData[i][j][0] = (byte) 0x85;
                    position += realySinglePackLen;
                    tempCount += realySinglePackLen;
                }
                tempCount = 0;
                long crc32 = ArrayUtil.crc32(data, crcPosition, crcBytes);
                mOtaData[i][length] = new byte[]{(byte) 0x82, 0x42, 0x45, 0x53, 0x54, (byte) crc32, (byte) (crc32 >> 8), (byte) (crc32 >> 16), (byte) (crc32 >> 24)};
            }
            mOtaData[crcCount] = new byte[1][];
            mOtaData[crcCount][0] = new byte[]{(byte) 0x88};
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mOtaData = null;
        } catch (IOException e) {
            e.printStackTrace();
            mOtaData = null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mOtaData == null) {
            onLoadFileFailed();
        } else {
            onLoadFileSuccessfully();
        }
    }

    @Deprecated
    protected void loadFileForNewProfileSPP() {
        LOG(TAG, "loadFileForNewProfile");
        String file_path = "";
        mSupportNewOtaProfile = true;
        FileInputStream inputStream = null;
        try {
            if (stereo_flg == true) {
                inputStream = new FileInputStream(mOtaFile.getText().toString());
            } else if (daulApply == APPLY_LEFT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                Log.e("daulApply==0", file_path);
                inputStream = inputStream = new FileInputStream(file_path);
            } else if (daulApply == APPLY_RIGHT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==1", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==2", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 0) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else if(mDaulConnectState == DAUL_CONNECT_RIGHT)
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
                Log.e("daulApply==3 s 0", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 1) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
                	Log.e("daulApply==3 s 1", file_path);
				}
            }else {
                showToast("请选择升级方式");
                return;
            }
			mOtaIngFile = file_path;
            int totalSize = inputStream.available();
            int dataSize = totalSize - 4;
            int mtu = getMtu();
            int packetPayload = ProfileUtils.calculateSppSinglePacketLen(dataSize);
            int packetTotalCount = ProfileUtils.calculateSppTotalPacketCount(dataSize);
            int crcCount = ProfileUtils.calculateSppTotalCrcCount(dataSize);
            mOtaData = new byte[crcCount + 1][][];
            Logger.e(TAG, "new profile totalLength: " + totalSize + "; packetTotalCount " + packetTotalCount + "; packet payload " + packetPayload + "; crcCount " + crcCount);
            byte[] data = new byte[dataSize];
            inputStream.read(data, 0, dataSize);
            int position = 0;
            for (int i = 0; i < crcCount; i++) {
                int startIndex = (int) Math.ceil(i * 1.0 / crcCount * packetTotalCount);
                int endIndex = (int) Math.ceil((i + 1) * 1.0 / crcCount * packetTotalCount);
                int length = endIndex - startIndex;
                int crcPosition = position;
                mOtaData[i] = new byte[length + 1][];
                for (int j = 0; j < length; j++) {
                    if (position + packetPayload > dataSize) {
                        packetPayload = dataSize - position;
                    }
                    mOtaData[i][j] = new byte[packetPayload + 1];
                    System.arraycopy(data, position, mOtaData[i][j], 1, packetPayload);
                    mOtaData[i][j][0] = (byte) 0x85;
                    position += packetPayload;
                }
                long crc32 = ArrayUtil.crc32(data, crcPosition, position - crcPosition);
                mOtaData[i][length] = new byte[]{(byte) 0x82, 0x42, 0x45, 0x53, 0x54, (byte) crc32, (byte) (crc32 >> 8), (byte) (crc32 >> 16), (byte) (crc32 >> 24)};
            }
            mOtaData[crcCount] = new byte[1][];
            mOtaData[crcCount][0] = new byte[]{(byte) 0x88};
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mOtaData = null;
        } catch (IOException e) {
            e.printStackTrace();
            mOtaData = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mOtaData == null) {
            onLoadFileFailed();
        } else {
            onLoadFileSuccessfully();
        }
    }

    protected void loadFile() {
        LOG(TAG, "loadFile");
        mSupportNewOtaProfile = false;
        String file_path = "";
        FileInputStream inputStream = null;
        try {
            if (stereo_flg == true) {
                inputStream = new FileInputStream(mOtaFile.getText().toString());
            } else if (daulApply == APPLY_LEFT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                Log.e("daulApply==0", file_path);
                inputStream = inputStream = new FileInputStream(file_path);
            } else if (daulApply == APPLY_RIGHT_EARBUD_ONLY) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==1", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE) {
                file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                inputStream = inputStream = new FileInputStream(file_path);
                Log.e("daulApply==2", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 0) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else if(mDaulConnectState == DAUL_CONNECT_RIGHT)
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
                Log.e("daulApply==3 s 0", file_path);
            } else if (daulApply == APPLY_BOTH_EARBUD_IN_TWO && daul_step == 1) {
				if(mDaulConnectState == DAUL_CONNECT_LEFT)
				{
					file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_LEFT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
				}
				else
				{
                	file_path = (String) SPHelper.getPreference(this, Constants.KEY_OTA_DAUL_RIGHT_FILE, "");
                	inputStream = inputStream = new FileInputStream(file_path);
                	Log.e("daulApply==3 s 1", file_path);
				}
            } else {
                showToast("请选择升级方式");
                return;
            }
			mOtaIngFile = file_path;
            int totalSize = inputStream.available();
            int dataSize = totalSize - 4;
            int mtu = getMtu();
            int packetPayload = 256; //mtu - 16;
            int packetCount = (dataSize + packetPayload - 1) / packetPayload;

            mOtaData = new byte[packetCount][][];
            int position = 0;
            Logger.e(TAG, "totalLength: " + totalSize + " packetCount " + packetCount + " packet payload " + packetPayload);
            for (int i = 0; i < packetCount; i++) {
                if (position + 256 > dataSize) {
                    packetPayload = dataSize - position;
                }
                mOtaData[i] = new byte[1][];
                mOtaData[i][0] = new byte[packetPayload + 16];
                inputStream.read(mOtaData[i][0], 16, packetPayload);
                long crc32 = ArrayUtil.crc32(mOtaData[i][0], 16, packetPayload);
                mOtaData[i][0][0] = (byte) 0xBE;
                mOtaData[i][0][1] = 0x64;
                mOtaData[i][0][2] = (byte) packetCount;
                mOtaData[i][0][3] = (byte) (packetCount >> 8);
                mOtaData[i][0][4] = (byte) packetPayload;
                mOtaData[i][0][5] = (byte) (packetPayload >> 8);
                mOtaData[i][0][6] = (byte) (packetPayload >> 16);
                mOtaData[i][0][7] = (byte) (packetPayload >> 24);
                mOtaData[i][0][8] = (byte) crc32;
                mOtaData[i][0][9] = (byte) (crc32 >> 8);
                mOtaData[i][0][10] = (byte) (crc32 >> 16);
                mOtaData[i][0][11] = (byte) (crc32 >> 24);
                mOtaData[i][0][12] = (byte) i;
                mOtaData[i][0][13] = (byte) (i >> 8);
                mOtaData[i][0][14] = 0x00;
                mOtaData[i][0][15] = (byte) ~ArrayUtil.checkSum(mOtaData[i][0], 15);
                position += packetPayload;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mOtaData = null;
        } catch (IOException e) {
            e.printStackTrace();
            mOtaData = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mOtaData == null) {
            onLoadFileFailed();
        } else {
            onLoadFileSuccessfully();
        }
    }

    protected void readyOta() {
        LOG(TAG, "readyOta " + mState);
        if (TextUtils.isEmpty(mAddress.getText())) {
            showToast(getString(R.string.pick_device_tips));
            return;
        } else if (TextUtils.isEmpty(mOtaFile.getText()) || daulApply == 5) {
            showToast(getString(R.string.pick_File_tips));
            return;
        } else if (mState == STATE_CONNECTED && (daulApply != -1)) {
            mOtaConfigDialog.show(getSupportFragmentManager(), OTA_CONFIG_TAG);
        } else if (isIdle()) {
            mOtaConfigDialog.show(getSupportFragmentManager(), OTA_CONFIG_TAG);
            //updateProgress(0);
            //sendCmdDelayed(CMD_CONNECT, 0);
        }
    }

    protected void reconnect() {
        LOG(TAG, "reconnect " + mState + " SPAN TIME IS " + RECONNECT_SPAN);
        LogUtils.writeForOTAStatic(TAG, "reconnect " + mState + " SPAN TIME IS " + RECONNECT_SPAN);
        mState = STATE_IDLE;
        if (isIdle()) {
            updateProgress(0);
            sendCmdDelayed(CMD_CONNECT, RECONNECT_SPAN);

        }
    }


    protected void startOta() {
        LOG(TAG, "startOta " + mSupportNewOtaProfile);
		if(daulApply == APPLY_BOTH_EARBUD_IN_TWO)
		{
		    String str ="正在传输升级文件"+ mOtaIngFile;
			updateInfo(str);
		}
		else
		{
        	updateInfo(R.string.ota_ing);
		}
        mState = STATE_OTA_ING;
        sendCmdDelayed(CMD_OTA_NEXT, 0);

    }

    protected void startOtaConfig() {
        Logger.e(TAG, "startOta " + mState);
        mState = STATE_OTA_CONFIG;
        sendCmdDelayed(CMD_OTA_CONFIG_NEXT, 0);
    }

    protected void pickFile(int request) {
        startActivityForResult(new Intent(this, FilePickerActivity.class), request);
    }

    protected void otaNext() {
        synchronized (mOtaLock) {
            Log.e("otaNext  -> mState ", mState + "");
            if (mOtaData == null) {
                Log.e("otaNext", "mOtaData == null");
            }
            if (mState != STATE_OTA_ING || mOtaData == null) {
                LOG(TAG, "otaNext  -> mState != STATE_OTA_ING || mOtaData == null ");
                return;
            }
            if (mOtaPacketCount == mOtaData.length) {
                LOG(TAG, "otaNext -> mState != STATE_OTA_ING || mOtaData == null ");
                return;
            }
            LOG(TAG, "otaNext totalPacketCount = " + totalPacketCount + " ; subCount " + mOtaPacketCount + "; " + mOtaPacketItemCount + "; " + mOtaData[mOtaPacketCount].length);

            if (mSupportNewOtaProfile || mWritten) {

                if ((mOtaPacketItemCount < mOtaData[mOtaPacketCount].length)) {
                    boolean sendRet = sendData(mOtaData[mOtaPacketCount][mOtaPacketItemCount]);
                    if (!sendRet) {
                        LOG(TAG, "otaNext write failed , try to resend");
                        sendCmdDelayed(CMD_OTA_NEXT, 40);
                    } else {
                        if (!mSupportNewOtaProfile && mOtaPacketCount == mOtaData.length - 1) {
                            onOtaOver();
                            return;
                        }
                        if (mOtaPacketItemCount == 0) {
                            LOG(TAG, "---------------------------------START--------------------------------------");
                            LOG(TAG, ">>START " + ArrayUtil.toHex(mOtaData[mOtaPacketCount][mOtaPacketItemCount]));
                        } else if (mOtaPacketItemCount == mOtaData[mOtaPacketCount].length - 1) {
                            LOG(TAG, "---------------------------------END--------------------------------------");
                            LOG(TAG, ">>END " + ArrayUtil.toHex(mOtaData[mOtaPacketCount][mOtaPacketItemCount]));
                        }
                        mOtaPacketItemCount++;
                        if (mOtaPacketItemCount == mOtaData[mOtaPacketCount].length) {
                            removeTimeout();
                            sendTimeout(R.string.ota_time_out, CMD_DISCONNECT, 30000);   //RESEND
                        } else {
                            removeTimeout();
                            sendTimeout(R.string.ota_time_out, CMD_RESEND_MSG, 10000);   //RESEND
                        }
                    }
                }
            } else {
                LOG(TAG, "otaNext  -> (mSupportNewOtaProfile || mWritten) is false  " + mSupportNewOtaProfile + " ;" + mWritten);
            }
        }
    }

    protected void otaConfigNext() {
        synchronized (mOtaLock) {
            Log.e("otaConfigNext mState", mState + "");
            if (mOtaConfigData == null) {
                Log.e("ConfigNext  ConfigData", "==null");
            }
            if (mState != STATE_OTA_CONFIG || mOtaConfigData == null) {
                LOG(TAG, "otaConfigNext mState != STATE_OTA_CONFIG || mOtaConfigData == null");
                return;
            }
            if (mOtaConfigPacketCount == mOtaConfigData.length) {
                LOG(TAG, "otaConfigNext mOtaConfigPacketCount == mOtaConfigData.length");
                return;
            }
            LOG(TAG, "otaConfigNext " + mOtaConfigPacketCount + "; " + mOtaConfigData.length + " mWritten = " + mWritten);
            if (true) {
                if (!sendData(mOtaConfigData[mOtaConfigPacketCount])) {
                    Logger.e(TAG, "otaConfigNext write failed");
                    sendCmdDelayed(CMD_OTA_CONFIG_NEXT, 10);
                } else {
                    mOtaConfigPacketCount++;
                    if (mOtaConfigPacketCount == mOtaConfigData.length) {
                        sendTimeout(R.string.ota_config_time_out, CMD_DISCONNECT, 5000);
                    }
                }
            }
        }
    }

    protected void exitOta() {
        removeTimeout();
        sendCmdDelayed(CMD_DISCONNECT, 0);
        dual_in_one_response_ok_time = 0;
        daulApply = -1;
        mMsgHandler.removeMessages(MSG_SEND_INFO_TIME_OUT);
        mExit = true;
    }

    protected void otaNextDelayed(long millis) {
        synchronized (mOtaLock) {
            if (mState == STATE_OTA_ING) {
                sendCmdDelayed(CMD_OTA_NEXT, millis);
            } else if (mState == STATE_OTA_CONFIG) {
                sendCmdDelayed(CMD_OTA_CONFIG_NEXT, millis);
            }
        }
    }

    //0x8E
    private void handleGetCurrentVersion() {
        LOG(TAG, "handleGetCurrentVersion");
        try {
            sendData(new byte[]{(byte) 0x8E, 0x42, 0x45, 0x53, 0x54});
            byte[] send = new byte[5];
            send[0] = (byte) 0x8E;
            send[1] = (byte) 0x42;
            send[2] = (byte) 0x45;
            send[3] = (byte) 0x53;
            send[3] = (byte) 0x54;
            String sendstr = ArrayUtil.toHex(send);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    private void handleApplyTheImage() {
        LOG(TAG, "handleApplyTheImage " + "CMD_APPLY_THE_IMAGE_MSG");
        FileInputStream inputStream = null;
		//先升级对耳，再升级本耳
        if (daulApply == 3 && daul_step == 0) {
			if(mDaulConnectState == DAUL_CONNECT_LEFT)
			{
				imageApply = IMAGE_RIGHT_EARBUD;
			}
			else if(mDaulConnectState == DAUL_CONNECT_RIGHT)
			{
				imageApply = IMAGE_LEFT_EARBUD;
			}
        }
        if (daulApply == 3 && daul_step == 1) {
            if(mDaulConnectState == DAUL_CONNECT_LEFT)
			{
				imageApply = IMAGE_LEFT_EARBUD;
			}
			else if(mDaulConnectState == DAUL_CONNECT_RIGHT)
			{
				imageApply = IMAGE_RIGHT_EARBUD;
			}
        }
        try {

            byte[] data = new byte[2];
            data[0] = (byte) 0x90;
            data[1] = imageApply;
            sendData(data);
            String str = ArrayUtil.toHex(data);
            Log.e("handleApplyTheImage", str);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onReceive(byte[] data)
    {
        LOG(TAG, "onReceive data = " + ArrayUtil.toHex(data));
        synchronized (mOtaLock) {
            if (ArrayUtil.isEqual(OTA_PASS_RESPONSE, data)) {
                removeTimeout();
                mOtaPacketItemCount = 0;
                mOtaPacketCount++;
                updateProgress(mOtaPacketCount * 100 / mOtaData.length);
                sendCmdDelayed(CMD_OTA_NEXT, 0);
            }
            else if (ArrayUtil.isEqual(OTA_RESEND_RESPONSE, data)) {
                removeTimeout();
                mOtaPacketItemCount = 0;
                sendCmdDelayed(CMD_OTA_NEXT, 0);
            }
            else if (ArrayUtil.startsWith(data, new byte[]{(byte) 0x81, 0x42, 0x45, 0x53, 0x54})) {
                if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 0) {
                    Log.e("0x81", "dual_in_one_response_ok_time == 0");
                    dual_in_one_response_ok_time = 1;
				    return;
                }
                else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 1) {
                    Log.e("0x81", "dual_in_one_response_ok_time==1");
                    dual_in_one_response_ok_time = 0;
                    mMsgHandler.removeMessages(MSG_SEND_INFO_TIME_OUT);
                    int softwareVersion = ((data[5] & 0xFF) | ((data[6] & 0xFF) << 8));
                    int hardwareVersion = ((data[7] & 0xFF) | ((data[8] & 0xFF) << 8));
                    Logger.e(TAG, "softwareVersion " + Integer.toHexString(softwareVersion) + "; hardwareVersion " + Integer.toHexString(hardwareVersion));
                    mMtu = (data[9] & 0xFF) | ((data[10] & 0xFF) << 8);
                    sendCmdDelayed(CMD_LOAD_OTA_CONFIG, 0);
                }
                else {
                    mMsgHandler.removeMessages(MSG_SEND_INFO_TIME_OUT);
                    int softwareVersion = ((data[5] & 0xFF) | ((data[6] & 0xFF) << 8));
                    int hardwareVersion = ((data[7] & 0xFF) | ((data[8] & 0xFF) << 8));
                    Logger.e(TAG, "softwareVersion " + Integer.toHexString(softwareVersion) + "; hardwareVersion " + Integer.toHexString(hardwareVersion));
                    mMtu = (data[9] & 0xFF) | ((data[10] & 0xFF) << 8);
                    sendCmdDelayed(CMD_LOAD_OTA_CONFIG, 0);

                }

            }
            else if ((data[0] & 0xFF) == 0x83) {
                if (data.length == 4 && (data[2] & 0xff) == 0x84) {
                    if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 0) {
                        dual_in_one_response_ok_time = 1;
						return;
                    }
                    else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 1) {
                        dual_in_one_response_ok_time = 0;
                        removeTimeout();
                        if ((data[3] & 0xFF) == 0x01) {
                            onOtaOver();
                            sendCmdDelayed(CMD_DISCONNECT, 0);
                        }
                        else if ((data[3] & 0xFF) == 0X00) {
                            onOtaFailed();
                            sendCmdDelayed(CMD_DISCONNECT, 0);
                        }
                        mOtaPacketItemCount = 0;
                    }
                    else {
                        removeTimeout();
                        if ((data[3] & 0xFF) == 0x01) {
                            onOtaOver();
                            sendCmdDelayed(CMD_DISCONNECT, 0);
                        }
                        else if ((data[3] & 0xFF) == 0X00) {
                            onOtaFailed();
                            sendCmdDelayed(CMD_DISCONNECT, 0);
                        }
                        mOtaPacketItemCount = 0;
                    }
                }
                else {

                    if ((data[1] & 0xFF) == 0x01) {
                        if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 1) {
							removeTimeout();
                            mOtaPacketCount++;
                            updateProgress(mOtaPacketCount * 100 / mOtaData.length);
                            dual_in_one_response_ok_time = 0;
                        }
                        else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 0) {
							removeTimeout();
                            dual_in_one_response_ok_time = 1;
							return;
                        }
                        else {
							removeTimeout();
                            mOtaPacketCount++;
                            updateProgress(mOtaPacketCount * 100 / mOtaData.length);
                        }
                    }
                    else if ((data[1] & 0xFF) == 0X00) {
                        if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 1) {
                            mOtaPacketCount = mOtaPacketCount; //虽然多余，保留协议可读性
                            updateProgress(mOtaPacketCount * 100 / mOtaData.length);
                            dual_in_one_response_ok_time = 0;
                        }
                        else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 0) {
                            dual_in_one_response_ok_time = 1;
							return;
                        }
                        else {
                            mOtaPacketCount = mOtaPacketCount; //虽然多余，保留协议可读性
                            updateProgress(mOtaPacketCount * 100 / mOtaData.length);
                        }
                    }
                    mOtaPacketItemCount = 0;
                    Log.e("test", "befor time " + System.currentTimeMillis());
                    sendCmdDelayed(CMD_OTA_NEXT, 0);
                }
            }
            else if ((data[0] & 0xFF) == 0x84) {
				Log.e("0x84",ArrayUtil.toHex(data));
                removeTimeout();
                if ((data[1] & 0xFF) == 0x01) {
                    if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 0) {
                        dual_in_one_response_ok_time = 1;
						return;
                    }
                    else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 1) {
                        onOtaOver();
                        sendCmdDelayed(CMD_DISCONNECT, 0);
                        dual_in_one_response_ok_time = 0;
                    }
                    else if (daul_step == 0 && daulApply == APPLY_BOTH_EARBUD_IN_TWO) {
                        daul_step = 1;
                         onOtaOverDaulOneStep();
                         sendCmdDelayed(CMD_APPLY_THE_IMAGE_MSG, 0);
					     return;
                    }
                    else if (daul_step == 1 && daulApply == APPLY_BOTH_EARBUD_IN_TWO) {
                        Log.e("daul_step == 1", "&&daulApply == 3");
						daul_step = 0;
					    onOtaOver();
                        sendCmdDelayed(CMD_DISCONNECT, 0);
                    }
                    else {
                        onOtaOver();
                        sendCmdDelayed(CMD_DISCONNECT, 0);
                    }
                }
                else if ((data[1] & 0xFF) == 0X00) {
                    if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 0) {
                        dual_in_one_response_ok_time = 1;
						return;
                    }
                    else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 1) {
                        onOtaFailed();
                        sendCmdDelayed(CMD_DISCONNECT, 0);
                        dual_in_one_response_ok_time = 0;
                    }
                    else {
                        onOtaFailed();
                        sendCmdDelayed(CMD_DISCONNECT, 0);
                    }
                }
                else if ((data[1] & 0xFF) == 0x02) {
                    updateInfo(getString(R.string.received_size_error));
                    sendCmdDelayed(CMD_DISCONNECT, 0);
                }
                else if ((data[1] & 0xFF) == 0x03) {
                    updateInfo(getString(R.string.write_flash_offset_error));
                    sendCmdDelayed(CMD_DISCONNECT, 0);
                }
                else if ((data[1] & 0xFF) == 0x04) {
                    if (daulApply == 2 && dual_in_one_response_ok_time == 0) {
                        dual_in_one_response_ok_time = 1;
						return;
                    }
                    else if (daulApply == 2 && dual_in_one_response_ok_time == 1) {
                        dual_in_one_response_ok_time = 0;
                        segment_verify_error_time++;
                        if (segment_verify_error_time < 3) {
                            updateInfo(segment_verify_error_time + "次重传");
                            mOtaPacketCount = mOtaPacketCount - 1;//回退一位
                            sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
                        }
                        else {
                            updateInfo("重传失败");
                            segment_verify_error_time = 0;
                            byte[] clearbyte = new byte[32];
                            for (int i = 0; i < clearbyte.length; i++) {
                                clearbyte[i] = (byte) 0x00;
                            }
                            SPHelper.putPreference(this.getApplicationContext(), Constants.KEY_OTA_RESUME_VERTIFY_RANDOM_CODE, ArrayUtil.toHex(clearbyte));
                            updateInfo(getString(R.string.segment_verify_error));
                            sendCmdDelayed(CMD_DISCONNECT, 0);
                        }
                    }
                    else {
                        segment_verify_error_time++;
                        if (segment_verify_error_time < 3) {
                            updateInfo(segment_verify_error_time + "次重传");
                            mOtaPacketCount = mOtaPacketCount - 1;//回退一位
                            sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
                        }
                        else {
                            updateInfo("重传失败");
                            segment_verify_error_time = 0;
                            byte[] clearbyte = new byte[32];
                            for (int i = 0; i < clearbyte.length; i++) {
                                clearbyte[i] = (byte) 0x00;
                            }
                            SPHelper.putPreference(this.getApplicationContext(), Constants.KEY_OTA_RESUME_VERTIFY_RANDOM_CODE, ArrayUtil.toHex(clearbyte));
                            updateInfo(getString(R.string.segment_verify_error));
                            sendCmdDelayed(CMD_DISCONNECT, 0);
                        }
                    }

                }
                else if ((data[1] & 0xFF) == 0x05) {
                    updateInfo(getString(R.string.breakpoint_error));
                    sendCmdDelayed(CMD_DISCONNECT, 0);

                }
                mOtaPacketItemCount = 0;
            }

            else if ((data[0] & 0xFF) == 0x8D) {
                Log.e("fanxiaoli fanxiaoli 8d", ArrayUtil.toHex(data) + "");
                Log.e("onReceive", "CMD_RESUME_OTA_CHECK_MSG_RESPONSE");
                removeTimeout();
                byte[] breakpoint = new byte[4];
                breakpoint = ArrayUtil.extractBytes(data, 1, 4);
                Log.e("extractBytes", ArrayUtil.toHex(breakpoint) + "");
                if (ArrayUtil.isEqual(breakpoint, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF})) {
                    resumeSegment = 0;
                    resumeFlg = false;
                    Log.e("resume", "error");
                }
                else if (ArrayUtil.isEqual(breakpoint, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00})) {
                    Log.e("daulApply ffffff", daulApply + "");
                    Log.e("dual_in_ok_time", dual_in_one_response_ok_time + "");

                    if ((daulApply == APPLY_BOTH_EARBUD_IN_ONE) && (dual_in_one_response_ok_time == 1)) {
                        Log.e("resume", "APPLY_BOTH_EARBUD_IN_ONE 1");
                        resumeFlg = false;
                        resumeSegment = 0;
                        sendCmdDelayed(CMD_SEND_FILE_INFO, 0);
                        LogUtils.writeForOTAStatic(TAG, "onConnected ");
                        updateInfo(R.string.connected);
                        mState = STATE_CONNECTED;
                        reconnectTimes = 0;
                    }
                    else if ((daulApply == APPLY_BOTH_EARBUD_IN_ONE) && (dual_in_one_response_ok_time == 0)) {
                        Log.e("resume", "APPLY_BOTH_EARBUD_IN_ONE 0");
                        dual_in_one_response_ok_time = 1;
					    return;
                    }
                    else {
                        Log.e("resume", "from 0 fanxiaoli");
                        resumeFlg = false;
                        resumeSegment = 0;
                        sendCmdDelayed(CMD_SEND_FILE_INFO, 0);
                        LogUtils.writeForOTAStatic(TAG, "onConnected ");
                        updateInfo(R.string.connected);
                        mState = STATE_CONNECTED;
                        reconnectTimes = 0;
                        byte[] randomCode = new byte[32];
                        randomCode = ArrayUtil.extractBytes(data, 5, 32);
                        String randomCodeStr = ArrayUtil.toHex(randomCode);
                        LOG(TAG, "random_code_str  fanxiaoli= " + randomCodeStr);
                        SPHelper.putPreference(this.getApplicationContext(), Constants.KEY_OTA_RESUME_VERTIFY_RANDOM_CODE, randomCodeStr);
                    }
                }
                else {

                    int segment = ArrayUtil.bytesToIntLittle(breakpoint);
                    Log.e("segment", segment + "");
                    if (segment != 0) {
                        resumeFlg = true;
                        mOtaPacketCount = segment / (1024 * 4);
                        updateInfo(getString(R.string.resume_start));
                        sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
                        Log.e("mOtaData is null", "resume mOtaPacketCount" + mOtaPacketCount);
                        resumeFlg = false;
                        Log.e("resume", "resume mOtaPacketCount" + mOtaPacketCount);
                    }

                }
            }
            else if ((data[0] & 0xFF) == 0x87) {
                removeTimeout();
                if ((data[1] & 0xFF) == 0x01) {
                    if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 0) {
						Log.e("0x87 01","dual_in_one_response_ok_time == 0");
                        dual_in_one_response_ok_time = 1;
					    return;
                    }
                    else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 1) {
						Log.e("0x87 01","dual_in_one_response_ok_time == 1");
                        dual_in_one_response_ok_time = 0;
                        if (isBle()) {
                            sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
                        }
                        else {
                            sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
                            //                   		 sendCmdDelayed(CMD_
                            // LOAD_FILE_FOR_NEW_PROFILE_SPP, 0);
                        }
                    }
                    else {
                        if (isBle()) {
                            sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
                        }
                        else {
                            sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
                            //                   		 sendCmdDelayed(CMD_
                            // LOAD_FILE_FOR_NEW_PROFILE_SPP, 0);
                        }
                    }
                }
                else {
                    onOtaConfigFailed();
                    sendCmdDelayed(CMD_DISCONNECT, 0);
                }
            }
            else if (ArrayUtil.startsWith(data, new byte[]{(byte) 0x8F, 0x42, 0x45, 0x53, 0x54})) { //Get current version response packet:
                mMsgHandler.removeMessages(MSG_SEND_INFO_TIME_OUT);
                String current_version = "";
                if ((data[5] & 0xFF) == 0x00) {
                    Log.e("received 0x8f", "stereo device");
                    byte[] version = new byte[4];
                    version = ArrayUtil.extractBytes(data, 6, 4);
                    String version_str = ArrayUtil.bytesToVersion(version);
					mDaulConnectState = DAUL_CONNECT_STEREO;
					if(version_str==null)
					{
					    updateVersion("版本号错误");
						return;
					}
					else
					{
	                    Log.e("version_str", version_str);
	                    current_version = "stereo device version ：" + version_str;
	                    updateVersion(current_version);
	                    daulApply = -1;
	                    imageApply = IMAGE_STEREO;
	                    stereo_flg = true;
					}


                }
                else if ((data[5] & 0xFF) == 0x01) {
                    current_version = "current connected device is left earbud\n";
                    Log.e("received 0x8f", " FWS device, current connected device is left earbud");
                    byte[] version = new byte[4];
                    version = ArrayUtil.extractBytes(data, 6, 4);
                    String version_str = ArrayUtil.bytesToVersion(version);
                    current_version = current_version + "left earbud version :" + version_str + "\n";
                    version_str = ArrayUtil.bytesToVersion(ArrayUtil.extractBytes(data, 10, 4));
                    current_version = current_version + "right earbud version:" + version_str;
					mDaulConnectState = DAUL_CONNECT_LEFT;
                    updateVersion(current_version);
                    daulApply = 5;
                    stereo_flg = false;

                }
                else if ((data[5] & 0xFF) == 0x02) {
                    current_version = "current connected device is right earbud\n";
                    Log.e("received 0x8f", "FWS device, current connected device is right earbud");
                    byte[] version = new byte[4];
                    version = ArrayUtil.extractBytes(data, 6, 4);
                    String version_str = ArrayUtil.bytesToVersion(version);
                    current_version = current_version + "left earbud version :" + version_str + "\n";
                    version_str = ArrayUtil.bytesToVersion(ArrayUtil.extractBytes(data, 10, 4));
                    current_version = current_version + "right earbud version:" + version_str;
					mDaulConnectState = DAUL_CONNECT_RIGHT;
                    updateVersion(current_version);
                    daulApply = 5;
                    stereo_flg = false;

                }

            }
            else if ((data[0] & 0xFF) == 0x91) {//New image apply result response:
                removeTimeout();
                if ((data[1] & 0xFF) == 0x01) {
                    Log.e("received 0x91", "isAppliedSuccessfully, 1 is pass,");
                    if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 0) {
                        dual_in_one_response_ok_time = 1;
						return;
                    }
                    else if (daulApply == APPLY_BOTH_EARBUD_IN_ONE && dual_in_one_response_ok_time == 1) {
                        dual_in_one_response_ok_time = 0;
                        sendCmdDelayed(CMD_RESUME_OTA_CHECK_MSG, 0);
                    }
                    else {
                        sendCmdDelayed(CMD_RESUME_OTA_CHECK_MSG, 0);
                    }
                }
                else if ((data[1] & 0xFF) == 0x00) {
                    Log.e("received 0x91", " 0 is fail");
                }

            }
        }


    }

        protected abstract void connect();

        protected abstract void disconnect ();

        protected abstract int getMtu ();

        protected abstract void pickDevice ( int request);

        protected abstract String loadLastDeviceName ();

        protected abstract void saveLastDeviceName (String name);

        protected abstract String loadLastDeviceAddress ();

        protected abstract void saveLastDeviceAddress (String address);

        protected abstract boolean sendData ( byte[] data);

        protected abstract boolean isBle ();

        public class CmdHandler extends Handler {
            public CmdHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CMD_CONNECT:
                        connect();
                        break;
                    case CMD_DISCONNECT:
                        disconnect();
                        break;
                    case CMD_LOAD_FILE:
                        loadFile();
                        break;
                    case CMD_OTA_NEXT:
                        Log.e(TAG, "after time " + System.currentTimeMillis());
                        otaNext();
                        break;
                    case CMD_START_OTA:
                        Log.e("CMD_START_OTA", "CMD_START_OTA");
                        startOta();
                        break;
                    case CMD_SEND_FILE_INFO:
                        sendFileInfo();
                        break;
                    case CMD_LOAD_OTA_CONFIG:
                        loadOtaConfig();
                        break;
                    case CMD_START_OTA_CONFIG:
                        startOtaConfig();
                        break;
                    case CMD_OTA_CONFIG_NEXT:
                        otaConfigNext();
                        break;
                    case CMD_LOAD_FILE_FOR_NEW_PROFILE:
                        loadFileForNewProfile();
                        break;
                    case CMD_LOAD_FILE_FOR_NEW_PROFILE_SPP:
                        loadFileForNewProfileSPP();
                        break;
                    case CMD_RESEND_MSG:
                        LOG(TAG, "resend the msg");
                        sendCmdDelayed(CMD_OTA_NEXT, 0);
                        break;
                    case CMD_RESUME_OTA_CHECK_MSG:
                        Log.e(TAG, "CMD_RESUME_OTA_CHECK_MSG");
                        //sendCmdDelayed(CMD_OTA_NEXT, 0);
                        sendBreakPointCheckReq();
                        break;
                    case CMD_SEND_HW_INFO:
                        //sendCmdSendHWInfo();
                        Log.e(TAG, "CMD_SEND_HW_INFO");
                        handleGetCurrentVersion();
                        break;
                    case CMD_APPLY_THE_IMAGE_MSG:
                        Log.e("CMD_APPLY_THE_IMAGE_MSG", "CMD_APPLY_THE_IMAGE_MSG");
                        handleApplyTheImage();
                }
            }
        }

        private final OtaConfigFragment.OtaConfigCallback mOtaConfigCallback = new OtaConfigFragment.OtaConfigCallback() {
            @Override
            public void onOtaConfigOk() {
                updateProgress(0);
                if (daulApply == -1) {
                    sendCmdDelayed(CMD_CONNECT, 0);
                } else {
                    sendCmdDelayed(CMD_APPLY_THE_IMAGE_MSG, 0);
                    Log.e("onOtaConfigOk", "CMD_APPLY_THE_IMAGE_MSG");
                }

            }

            @Override
            public void onOtaConfigCancel() {

            }
        };


        private final OtaDaulPickFileFragment.OtaPickFileCallback motaPickFileCallback = new OtaDaulPickFileFragment.OtaPickFileCallback() {


            @Override
            public void onOtaPickFileOk() {
                updateDaulFile("1111111111111111");
            }

            @Override
            public void onOtaPickFileCancel() {

            }
        };

        @Override
        protected void onResume () {
            LOG(TAG, "onResume");
            super.onResume();
			
		    int i = getConnectBt();
			if(!(i==-1))
			{
				getConnectBtDetails(i);
			}
        }

        @Override
        protected void onStop () {
            LOG(TAG, "onStop");

            super.onStop();

        }


        protected void LOG (String TAG, String msg){
            if (msg != null && TAG != null) {
                if (isBle()) {
                    LogUtils.writeForBle(TAG, msg);
                } else {
                    LogUtils.writeForClassicBt(TAG, msg);
                }
            }
        }


        private void showChooseApplyDialog () {
            final String[] items = {getString(R.string.left_earbud_only), getString(R.string.right_earbud_only), getString(R.string.both_earbuds_in_one_bin), getString(R.string.both_earbuds_in_two_bins)};
            final int past = daulApply;
            AlertDialog.Builder singleChoiceDialog =
                    new AlertDialog.Builder(OtaActivity.this);
            singleChoiceDialog.setTitle(getString(R.string.daul_earbuds_ota_apply));
            // 第二个参数是默认选项，此处设置为0
            singleChoiceDialog.setSingleChoiceItems(items, -1,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            daulApply = which;
                        }
                    });
            singleChoiceDialog.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (daulApply != -1) {
                                Log.e("11111111111111", daulApply + "");
                                SPHelper.putPreference(OtaActivity.this, Constants.KEY_OTA_DAUL_APPLY_WAY, daulApply);
                                Bundle bundle = new Bundle();
                                bundle.putInt("apply_type", daulApply);
                                motaDaulPickFileFragment.setArguments(bundle);
                                motaDaulPickFileFragment.show(getSupportFragmentManager(), OTA_DAUL_PICK_FILE);
                            }
                        }
                    });
            singleChoiceDialog.show();
        }


    //获取已连接的蓝牙设备名称
    private void getConnectBtDetails(int flag)
    {
        bluetoothAdapter.getProfileProxy(OtaActivity.this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceDisconnected(int profile) {
                Toast.makeText(OtaActivity.this,profile+"",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                List<BluetoothDevice> mDevices = proxy.getConnectedDevices();
                if (mDevices != null && mDevices.size() > 0) {
                    for (BluetoothDevice device : mDevices) {
						saveLastDeviceName(device.getName());
                        saveLastDeviceAddress(device.getAddress());
						 mDevice = BtHelper.getRemoteDevice(OtaActivity.this, device.getAddress().toString());
                        updateConnectedBtName(device.getName());
                        updateConnectedBtAddress(device.getAddress());                   

                    }
                } else {
                    updateConnectedBtAddress("请在手机端和耳机配对，以使用相应功能");
					updateConnectedBtName("--");
                }
            }
        }, flag);

    }

    //获取已连接的蓝牙设备状态
    private int getConnectBt()
    {

        int a2dp = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
        int headset = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        int health = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEALTH);
        int flag = -1;
        if (a2dp == BluetoothProfile.STATE_CONNECTED) {
            flag = a2dp;
        }
        else if (headset == BluetoothProfile.STATE_CONNECTED) {
            flag = headset;
        }
        else if (health == BluetoothProfile.STATE_CONNECTED) {
            flag = health;
        }
        if(flag==-1)
        {
            updateConnectedBtAddress("请在设置里连接蓝牙耳机，以使用相应功能");
			updateConnectedBtName("--");
        }
        return flag;
    }

}
