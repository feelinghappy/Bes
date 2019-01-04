package com.iir_eq.ui.activity;

import android.bluetooth.BluetoothDevice;
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

import com.iir_eq.R;
import com.iir_eq.bluetooth.BtHelper;
import com.iir_eq.bluetooth.callback.ConnectCallback;
import com.iir_eq.contants.Constants;
import com.iir_eq.ui.fragment.OtaConfigFragment;
import com.iir_eq.util.ArrayUtil;
import com.iir_eq.util.CommUtils;
import com.iir_eq.util.LogUtils;
import com.iir_eq.util.Logger;
import com.iir_eq.util.ProfileUtils;
import com.iir_eq.util.SPHelper;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.iir_eq.util.ArrayUtil.extractBytes;


/**
 * Created by zhaowanxing on 2017/7/12.
 */

public abstract class OtaActivity extends BaseActivity implements ConnectCallback,View.OnClickListener {

    protected static final String KEY_OTA_FILE = "ota_file";

    protected static final byte[] OTA_PASS_RESPONSE = new byte[]{0x11, 0x22};
    protected static final byte[] OTA_RESEND_RESPONSE = new byte[]{0x33, 0x44};

    protected static final int DEFAULT_MTU = 512;

    private static final int REQUEST_OTA_FILE = 0X00;
    private static final int REQUEST_DEVICE = 0x01;

    private static final int MSG_UPDATE_INFO = 0x00;
    private static final int MSG_UPDATE_PROGRESS = 0x01;
    private static final int MSG_OTA_TIME_OUT = 0x02;
    private static final int MSG_SEND_INFO_TIME_OUT = 0x03;
    private static final int MSG_UPDATE_RESULT_INFO = 0x04;


    protected static final int CMD_CONNECT = 0x80;
    protected static final int CMD_DISCONNECT = 0x81;
    protected static final int CMD_LOAD_FILE = 0x82;
    protected static final int CMD_START_OTA = 0x83;
    protected static final int CMD_OTA_NEXT = 0x84;
    protected static final int CMD_SEND_FILE_INFO = 0x85;
    protected static final int CMD_LOAD_FILE_FOR_NEW_PROFILE = 0x86;
    protected static final int CMD_RESEND_MSG = 0X88 ;
    protected static final int CMD_LOAD_FILE_FOR_NEW_PROFILE_SPP = 0X89 ;
    //////////////////////////add by fxl 1227 begin//////////////////////////////////
    protected static final int CMD_RESUME_OTA_CHECK_MSG = 0X8C ;   //resume
    protected static final int CMD_RESUME_OTA_CHECK_MSG_RESPONSE = 0X8D ; //resume back
    //////////////////////////add by fxl 1227 end//////////////////////////////////


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
	
    protected int totalPacketCount = 0 ;

    protected Object mOtaLock = new Object();

    protected int mMtu;

    protected volatile boolean mWritten = true;

    private final String OTA_CONFIG_TAG = "ota_config";
    private OtaConfigFragment mOtaConfigDialog;

    protected long castTime ;//temp data for log
    protected long sendMsgFailCount = 0 ;//temp data for log
    protected long otaImgSize = 0 ;

    protected final int RECONNECT_MAX_TIMES = 5 ; // 5 times
    protected final int RECONNECT_SPAN = 3000 ; // 3 seconds
    protected int reconnectTimes = 0 ;

    protected int totalCount = 0 ;
    protected int failedCount = 0 ;
    protected int resumeSegment = 0;

    protected boolean resumeFlg = false ;

    TextView mAddress;
    TextView mName;
    TextView mOtaFile;
    TextView mOtaInfo;
    TextView mUpdateStatic ;
    ProgressBar mOtaProgress;
    TextView mOtaStatus;
    Button pickDevice , pickOtaFile , startOta ;

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.pick_device:
                if (isIdle()) {
                    pickDevice(REQUEST_DEVICE);
                }
                break;
            case R.id.pick_ota_file:
                if (isIdle()) {
                    pickFile(REQUEST_OTA_FILE);
                }
                break;
            case R.id.start_ota:
                readyOta();
                break;
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
        mOtaStatus = (TextView)findViewById(R.id.ota_status);
        pickDevice = (Button) findViewById(R.id.pick_device);
        pickOtaFile = (Button) findViewById(R.id.pick_ota_file);
        startOta = (Button) findViewById(R.id.start_ota);
        pickDevice.setOnClickListener(this);
        pickOtaFile.setOnClickListener(this);
        startOta.setOnClickListener(this);

        mOtaFile.setText(SPHelper.getPreference(this, KEY_OTA_FILE, "").toString());
        mName.setText(loadLastDeviceName());
        mAddress.setText(loadLastDeviceAddress());
        mDevice = BtHelper.getRemoteDevice(this, mAddress.getText().toString());

        mOtaConfigDialog = new OtaConfigFragment();
        mOtaConfigDialog.setOtaConfigCallback(mOtaConfigCallback);
    }

    private Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_INFO:
                    LOG(TAG, "MSG_UPDATE_INFO");
                    Log.e("OtaActivity","MSG_UPDATE_INFO");//add by fxl 1226
                    mOtaInfo.setText(msg.obj.toString());
                    break;
                case MSG_UPDATE_RESULT_INFO :
                    LOG(TAG, "MSG_UPDATE_RESULT_INFO");
                    Log.e("OtaActivity","MSG_UPDATE_RESULT_INFO");//add by fxl 1226
                    if(mUpdateStatic != null){
                        mUpdateStatic.setText(msg.obj.toString());
                    }
                    break;
                case MSG_UPDATE_PROGRESS:
                    LOG(TAG, "MSG_UPDATE_PROGRESS");
                    Log.e("OtaActivity","MSG_UPDATE_PROGRESS");//add by fxl 1226
                    if(mOtaProgress != null){
                        mOtaProgress.setProgress((Integer) msg.obj);
                        mOtaStatus.setText((Integer) msg.obj+"%");
                    }else{
                        LOG(TAG, "mOtaProgress is null");
                    }
                    break;
                case MSG_OTA_TIME_OUT:
                case MSG_SEND_INFO_TIME_OUT:
                    Log.e("OtaActivity","MSG_SEND_INFO_TIME_OUT|MSG_SEND_INFO_TIME_OUT time out");//add by fxl 1226
                     LOG(TAG, "MSG_SEND_INFO_TIME_OUT|MSG_SEND_INFO_TIME_OUT time out");
                    if(mOtaInfo != null){
                        mOtaInfo.setText(msg.arg1);
                    }else{
                        LOG(TAG, "mOtaInfo is null");
                    }
                    sendCmdDelayed(msg.arg2, 0);
                    break;
                default:// donot left the default , even nothing to do
            }
        }
    };

    private boolean isIdle() {
        return mState == STATE_IDLE || mState == STATE_OTA_FAILED || mState == STATE_DISCONNECTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG(TAG , "onCreate");
        setContentView(R.layout.act_ota);
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
        LOG(TAG , "onDestroy");
        if(mMsgHandler != null){
            mMsgHandler.removeMessages(MSG_SEND_INFO_TIME_OUT);
            mMsgHandler.removeMessages(MSG_OTA_TIME_OUT);
        }
        if(mCmdHandler != null){
            mCmdHandler.removeMessages(CMD_OTA_NEXT);
            mCmdHandler.removeMessages(CMD_OTA_CONFIG_NEXT);
        }
        if (mCmdThread != null && mCmdThread.isAlive()) {
            mCmdThread.quit();
        }
        resumeFlg = false;
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
        LOG(TAG , "exit");
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
        LOG(TAG , "onActivityResult");
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
                LOG(TAG, "mState == STATE_CONNECTING");
                reconnectTimes++;
                if(reconnectTimes > RECONNECT_MAX_TIMES){
                    updateInfo(R.string.connect_failed);
                    mState = STATE_DISCONNECTED;
                    onOtaFailed();
                }else{
                    updateInfo(String.format(getString(R.string.connect_reconnect_try), reconnectTimes));
                    reconnect();
                }
            } else if (mState == STATE_OTA_ING) {
                LOG(TAG, "mState == STATE_OTA_ING");
                onOtaFailed();
            } else if (mState != STATE_IDLE) {
                LOG(TAG, "mState != STATE_IDLE");
                updateInfo(R.string.disconnected);
                mState = STATE_DISCONNECTED;
                onOtaFailed();
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

    protected void updateProgress(int progress) {
        Message message = mMsgHandler.obtainMessage(MSG_UPDATE_PROGRESS);
        message.obj = progress;
        mMsgHandler.sendMessage(message);
    }

    protected void sendCmdDelayed(int cmd, long millis) {
        mCmdHandler.removeMessages(cmd);
        if(millis == 0){
            mCmdHandler.sendEmptyMessage(cmd);
        }else{
            mCmdHandler.sendEmptyMessageDelayed(cmd, millis);
        }
    }

    protected void sendTimeout(int info, int cmd, long millis) {
        LOG(TAG, "sendTimeout info "+info+" ; cmd "+cmd +" ; millis "+millis);
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
        }
    }

    protected void onConnecting() {
        LOG(TAG, "onConnecting");
        LogUtils.writeForOTAStatic(TAG , "onConnecting ");
        castTime = System.currentTimeMillis() ;//startTime
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
        sendCmdDelayed(CMD_RESUME_OTA_CHECK_MSG, 0);
        LogUtils.writeForOTAStatic(TAG , "onConnected ");
        updateInfo(R.string.connected);
        mState = STATE_CONNECTED;
        reconnectTimes = 0 ;

    }

    protected void onConnectFailed() {
        LOG(TAG, "onConnectFailed");
        LogUtils.writeForOTAStatic(TAG , "onConnectFailed "+((System.currentTimeMillis() - castTime)/1000));
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
        Log.e("OtaActivity","onOtaOver");
        totalCount++;
        String result = "Result：Total count = "+totalCount+"  Failure count = "+failedCount ;
        updateResultInfo(result);
        Log.e("OtaActivity",result);
        int updateTime = (int)((System.currentTimeMillis() - castTime)/1000) ;
        String msg = "Successful time-cost "+updateTime+" s"+" Retransmission count "+sendMsgFailCount+" Speed :"+otaImgSize/(updateTime == 0?otaImgSize:updateTime)+" B/s";
        LogUtils.writeForOTAStatic(TAG , msg);
        msg = "Successful time-cost "+updateTime+" s"+" Speed :"+otaImgSize/(updateTime == 0?otaImgSize:updateTime)+" B/s";
        updateInfo(msg);
        Log.e("OtaActivity",msg);
        updateProgress(100);
        mOtaPacketCount = 0;
        resumeFlg = false;
        mOtaPacketItemCount = 0;
        mOtaConfigPacketCount = 0 ;
        mState = STATE_IDLE;
    }

    protected void onOtaFailed() {
        Log.e("OtaActivity", "onOtaFailed");
        totalCount++;
        failedCount++;
        String result = "Result：Total count = "+totalCount+"  Failure count = "+failedCount ;
        updateResultInfo(result);
        int updateTime = (int)((System.currentTimeMillis() - castTime)/1000) ;
        String msg = "Failed time-cost "+updateTime+" s"+" Retransmission count "+sendMsgFailCount+" Speed :"+otaImgSize/(updateTime == 0?otaImgSize:updateTime)+" B/s";
        LogUtils.writeForOTAStatic(TAG , msg);
        Log.e("OtaActivity",msg);
        msg = "Failed time-cost "+updateTime+" s"+" Speed :"+otaImgSize/(updateTime == 0?otaImgSize:updateTime)+" B/s";
        updateInfo(msg);
        reconnectTimes = 0 ;
        mOtaPacketCount = 0;
        resumeFlg = false;
        mOtaPacketItemCount = 0;
        mOtaConfigPacketCount = 0 ;
        mState = STATE_OTA_FAILED;
    }
	
	protected void onOtaConfigFailed() {
        updateInfo(R.string.ota_config_failed);
        mOtaConfigData = null;
        mOtaConfigPacketCount = 0;
        mOtaConfigPacketCount = 0 ;
        mState = STATE_IDLE;
    }

    protected void onWritten() {
        mWritten = true;
        LOG(TAG , "onWritten mWritten = true");
    }

    protected void sendBreakPointCheckReq() {
        LOG("OtaActivity", "sendBreakPointCheckReq");
        try {
            mOtaResumeDataReq = new byte[37+4+4] ;//依据协议BES_OTA_SPE_3.0.docx
            String randomCodestr = null;
            byte[] randomCode = new byte[32];
            randomCodestr = (String) SPHelper.getPreference(this, Constants.KEY_OTA_RESUME_VERTIFY_RANDOM_CODE, "");
            if(randomCodestr==null||randomCodestr=="")
            {
                for (int i=0;i<randomCode.length;i++)
                {
                    randomCode[i] = (byte)0x01;
                    mOtaResumeDataReq[i+5] = (byte)0x01;
                }
                SPHelper.putPreference(this.getApplicationContext(), Constants.KEY_OTA_RESUME_VERTIFY_RANDOM_CODE, ArrayUtil.toHex(randomCode));
                Log.e("null fanxiaoli",ArrayUtil.toHex(randomCode));
            }
            else
            {
                Log.e("randomCodestr",randomCodestr);
                randomCode = ArrayUtil.toBytes(randomCodestr);
                Log.e("sendBreakPoCodestr","randomCodestr:"+randomCodestr);
                Log.e("sendBreak PorandomCode","fanxiaoli:"+ArrayUtil.toHex(randomCode));
                for (int i=0;i<randomCode.length;i++)
                {
                    mOtaResumeDataReq[i+5] = randomCode[i];
                }
                Log.e("mOtaResumeDatnormally",""+ArrayUtil.toHex(mOtaResumeDataReq));
            }
            mOtaResumeDataReq[0] = (byte)0x8C;
            mOtaResumeDataReq[1] = (byte)0x42;
            mOtaResumeDataReq[2] = (byte)0x45;
            mOtaResumeDataReq[3] = (byte)0x53;
            mOtaResumeDataReq[4] = (byte)0x54;
            mOtaResumeDataReq[37] = (byte)0x01;
            mOtaResumeDataReq[38] = (byte) 0x02;
            mOtaResumeDataReq[39] = (byte) 0x03;
            mOtaResumeDataReq[40] = (byte) 0x04;
            byte[] crc32_data = new byte[36];
            for (int i=0;i<crc32_data.length;i++)
            {
                crc32_data[i] = mOtaResumeDataReq[i+5];
            }

            long crc32 = ArrayUtil.crc32(crc32_data, 0, 36);
            mOtaResumeDataReq[41] = (byte) crc32;
            mOtaResumeDataReq[42] = (byte)(crc32>> 8) ;
            mOtaResumeDataReq[43] = (byte) (crc32>>16);
            mOtaResumeDataReq[44] = (byte) (crc32>>24);
            Log.e("mOtaResumeDataReq",""+ArrayUtil.toHex(mOtaResumeDataReq));
            updateInfo(R.string.resume_request_verify);
            sendData(mOtaResumeDataReq);
        }catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception",e.getMessage().toString());
        } finally {

        }
    }

    /////////////////////////////////add by fxl 1227 end//////////////////////////////////////////////////////

    protected void sendFileInfo() {
        LOG(TAG, "sendFileInfo MSG_SEND_INFO_TIME_OUT");
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(mOtaFile.getText().toString());
            int totalSize = inputStream.available();
            otaImgSize = totalSize ; //TODO :TEMP ADD
            int dataSize = totalSize - 4;
            byte[] data = new byte[dataSize];
            inputStream.read(data, 0, dataSize);
            long crc32 = ArrayUtil.crc32(data, 0, dataSize);
            Message message = mMsgHandler.obtainMessage(MSG_SEND_INFO_TIME_OUT);
            message.arg1 = R.string.old_ota_profile;
            message.arg2 = CMD_LOAD_FILE;
            mMsgHandler.sendMessageDelayed(message, 5000);
            sendData(new byte[]{(byte) 0x80, 0x42, 0x45, 0x53, 0x54, (byte) dataSize, (byte) (dataSize >> 8), (byte) (dataSize >> 16), (byte) (dataSize >> 24), (byte) crc32, (byte) (crc32 >> 8), (byte) (crc32 >> 16), (byte) (crc32 >> 24)});
        }catch (Exception e) {
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
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(mOtaFile.getText().toString());
            int totalSize = inputStream.available();
            int dataSize = totalSize ;
            byte[] data = new byte[dataSize];
            inputStream.read(data, 0, dataSize);

            int configLength = 92;
            byte[] config = new byte[configLength];
            int lengthOfFollowingData = configLength - 4 ;
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
                    config[76 + 5-i] = Integer.valueOf(btAddress.substring(i, i * 2 + 2), 16).byteValue();
                }
            }
            if (updateBleAddress) {
                String bleAddress = (String) SPHelper.getPreference(this, Constants.KEY_OTA_CONFIG_UPDATE_BLE_ADDRESS_VALUE, "");
                for (int i = 0; i < 6; i++) {
                    config[82 + 5-i] = Integer.valueOf(bleAddress.substring(i, i * 2 + 2), 16).byteValue();
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
        } catch (Exception e){
            e.printStackTrace();
        }finally {
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
        mSupportNewOtaProfile = true;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(mOtaFile.getText().toString());
            int totalSize = inputStream.available();
            int dataSize = totalSize - 4;
            int mtu = getMtu();
            int packetPayload = ProfileUtils.calculateBLESinglePacketLen(dataSize , mtu , isBle());
            int totalPacketCount = (dataSize + packetPayload - 1) / packetPayload;
            int onePercentBytes = ProfileUtils.calculateBLEOnePercentBytes(dataSize);
            int crcCount = (dataSize + onePercentBytes -1 )/onePercentBytes ;
            this.totalPacketCount = totalPacketCount ;
            mOtaData = new byte[crcCount+1][][];
            Logger.e(TAG, "new profile imgeSize: " + dataSize + "; totalPacketCount " + totalPacketCount + "; onePercentBytes " + onePercentBytes + "; crcCount " + crcCount);
            byte[] data = new byte[dataSize];
            inputStream.read(data, 0, dataSize);
            int position = 0 ;
            for (int i = 0; i < crcCount; i++) {
                int crcBytes = onePercentBytes ; //要校验百分之一的数据量
                if(packetPayload == 0){
                    Log.e(TAG , ">>");
                }
                int length = (crcBytes + packetPayload - 1)/packetPayload; //根据MTU ，算出百分之一需要多少个包满足要求
                if(crcCount - 1 == i){ // 最后一包取余数
                    crcBytes = dataSize - position ;
                    length = (crcBytes + packetPayload - 1)/packetPayload;
                }
                LOG(TAG , "CRC BYTES = "+crcBytes);
                mOtaData[i] = new byte[length + 1][]; //加 1 为增加最后结束整包校验命令
                int realySinglePackLen = 0 ;
                int crcPosition = position ;
                int tempCount = 0 ;
                for (int j = 0; j < length; j++) {
                    realySinglePackLen = packetPayload ;
                    if (j == length - 1) { //每百分之一的最后一包取余数
                        realySinglePackLen = (crcBytes%packetPayload == 0)?packetPayload:crcBytes%packetPayload;
                    }
                    mOtaData[i][j] = new byte[realySinglePackLen + 1];
                    System.arraycopy(data, position, mOtaData[i][j], 1, realySinglePackLen);
                    mOtaData[i][j][0] = (byte) 0x85;
                    position += realySinglePackLen;
                    tempCount += realySinglePackLen ;
                }
                tempCount = 0 ;
                long crc32 = ArrayUtil.crc32(data, crcPosition, crcBytes);
                mOtaData[i][length] = new byte[]{(byte) 0x82, 0x42, 0x45, 0x53, 0x54, (byte) crc32, (byte) (crc32 >> 8), (byte) (crc32 >> 16), (byte) (crc32 >> 24)};
            }
            mOtaData[crcCount] = new byte[1][];
            mOtaData[crcCount][0] = new byte[]{(byte)0x88};
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
        mSupportNewOtaProfile = true;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(mOtaFile.getText().toString());
            int totalSize = inputStream.available();
            int dataSize = totalSize - 4;
            int mtu = getMtu();
            int packetPayload = ProfileUtils.calculateSppSinglePacketLen(dataSize);
            int packetTotalCount = ProfileUtils.calculateSppTotalPacketCount(dataSize);
            int crcCount = ProfileUtils.calculateSppTotalCrcCount(dataSize);
            mOtaData = new byte[crcCount+1][][];
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
            mOtaData[crcCount][0] = new byte[]{(byte)0x88};
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mOtaData = null;
        } catch (IOException e) {
            e.printStackTrace();
            mOtaData = null;
        } catch (Exception e){
            e.printStackTrace();
        }finally {
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
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(mOtaFile.getText().toString());
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
        } catch (Exception e){
            e.printStackTrace();
        }finally {
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
        }
        if (TextUtils.isEmpty(mOtaFile.getText())) {
            showToast(getString(R.string.pick_File_tips));
            return;
        }
        if (isIdle()) {
		    mOtaConfigDialog.show(getSupportFragmentManager(), OTA_CONFIG_TAG);
            //updateProgress(0);
            //sendCmdDelayed(CMD_CONNECT, 0);
        }
    }

    protected void reconnect(){
        LOG(TAG, "reconnect " + mState+" SPAN TIME IS "+RECONNECT_SPAN);
        LogUtils.writeForOTAStatic(TAG, "reconnect " + mState+" SPAN TIME IS "+RECONNECT_SPAN);
        mState = STATE_IDLE;
        if (isIdle()) {
            updateProgress(0);
            sendCmdDelayed(CMD_CONNECT, RECONNECT_SPAN);
        }
    }


    protected void startOta() {
        LOG(TAG, "startOta " + mSupportNewOtaProfile);
        updateInfo(R.string.ota_ing);
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
            if (mState != STATE_OTA_ING || mOtaData == null) {
                LOG(TAG, "otaNext  -> mState != STATE_OTA_ING || mOtaData == null ");
                return;
            }
            if (mOtaPacketCount == mOtaData.length) {
                LOG(TAG, "otaNext -> mState != STATE_OTA_ING || mOtaData == null ");
                return;
            }
            LOG(TAG, "otaNext totalPacketCount = "+ totalPacketCount +" ; subCount " + mOtaPacketCount + "; " + mOtaPacketItemCount + "; " + mOtaData[mOtaPacketCount].length);

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
                        if(mOtaPacketItemCount == 0){
                            LOG(TAG , "---------------------------------START--------------------------------------");
                            LOG(TAG , ">>START "+ ArrayUtil.toHex(mOtaData[mOtaPacketCount][mOtaPacketItemCount]));
                        }else if(mOtaPacketItemCount == mOtaData[mOtaPacketCount].length - 1)
                        {
                            LOG(TAG , "---------------------------------END--------------------------------------");
                            LOG(TAG , ">>END "+ ArrayUtil.toHex(mOtaData[mOtaPacketCount][mOtaPacketItemCount]));
                        }
                        mOtaPacketItemCount++;
                        if (mOtaPacketItemCount == mOtaData[mOtaPacketCount].length) {
                            removeTimeout();
                            sendTimeout(R.string.ota_time_out, CMD_DISCONNECT, 30000);   //RESEND
                        }else{
                            removeTimeout();
                            sendTimeout(R.string.ota_time_out, CMD_RESEND_MSG, 10000);   //RESEND
                        }
                    }
                }
            }else{
                LOG(TAG, "otaNext  -> (mSupportNewOtaProfile || mWritten) is false  "+ mSupportNewOtaProfile+" ;"+mWritten);
            }
        }
    }
	
	    protected void otaConfigNext() {
            synchronized (mOtaLock) {
                if (mState != STATE_OTA_CONFIG || mOtaConfigData == null) {
                    LOG(TAG, "otaConfigNext mState != STATE_OTA_CONFIG || mOtaConfigData == null");
                    return;
                }
                if (mOtaConfigPacketCount == mOtaConfigData.length) {
                    LOG(TAG, "otaConfigNext mOtaConfigPacketCount == mOtaConfigData.length");
                    return;
                }
                LOG(TAG, "otaConfigNext " + mOtaConfigPacketCount + "; " + mOtaConfigData.length+" mWritten = "+mWritten);
                if (true) {
                    if (!sendData(mOtaConfigData[mOtaConfigPacketCount])){
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

    @Override
    public void onReceive(byte[] data) {
        LOG(TAG , "onReceive data = "+ArrayUtil.toHex(data));
        synchronized (mOtaLock) {
            Logger.e(TAG, "onReceive " + ArrayUtil.toHex(data));
            if (ArrayUtil.isEqual(OTA_PASS_RESPONSE, data)) {
                removeTimeout();
                mOtaPacketItemCount = 0;
                mOtaPacketCount++;
                updateProgress(mOtaPacketCount * 100 / mOtaData.length);

                sendCmdDelayed(CMD_OTA_NEXT, 0);
            } else if (ArrayUtil.isEqual(OTA_RESEND_RESPONSE, data)) {
                removeTimeout();
                mOtaPacketItemCount = 0;
                sendCmdDelayed(CMD_OTA_NEXT, 0);
            } else if (ArrayUtil.startsWith(data, new byte[]{(byte) 0x81, 0x42, 0x45, 0x53, 0x54})) {
                mMsgHandler.removeMessages(MSG_SEND_INFO_TIME_OUT);
                int softwareVersion = ((data[5] & 0xFF) | ((data[6] & 0xFF) << 8));
                int hardwareVersion = ((data[7] & 0xFF) | ((data[8] & 0xFF) << 8));
                Logger.e(TAG, "softwareVersion " + Integer.toHexString(softwareVersion) + "; hardwareVersion " + Integer.toHexString(hardwareVersion));
                mMtu = (data[9] & 0xFF) | ((data[10] & 0xFF) << 8);
                sendCmdDelayed(CMD_LOAD_OTA_CONFIG, 0);
            } else if ((data[0] & 0xFF) == 0x83) {
                if(data.length == 4 && (data[2] & 0xff) == 0x84){
                    removeTimeout();
                    if ((data[3] & 0xFF) == 0x01) {
                        onOtaOver();
                        sendCmdDelayed(CMD_DISCONNECT, 0);
                    }else if((data[3]&0xFF) == 0X00){
                        onOtaFailed();
                        sendCmdDelayed(CMD_DISCONNECT, 0);
                    }
                    mOtaPacketItemCount = 0;
                }else{
                    removeTimeout();
                    if ((data[1] & 0xFF) == 0x01) {
                        mOtaPacketCount++;
                        updateProgress(mOtaPacketCount * 100 / mOtaData.length);
                    }else if((data[1]&0xFF) == 0X00){
                        mOtaPacketCount = mOtaPacketCount ; //虽然多余，保留协议可读性
                        updateProgress(mOtaPacketCount * 100 / mOtaData.length);
                    }
                    mOtaPacketItemCount = 0;
                    Log.e("test" , "befor time "+System.currentTimeMillis());
                    sendCmdDelayed(CMD_OTA_NEXT, 0);
                }
            } else if ((data[0] & 0xFF) == 0x84) {
                removeTimeout();
                if ((data[1] & 0xFF) == 0x01) {
                    onOtaOver();
                    sendCmdDelayed(CMD_DISCONNECT, 0);
                }else if((data[1]&0xFF) == 0X00){
                    onOtaFailed();
                    sendCmdDelayed(CMD_DISCONNECT, 0);
                }

                else if((data[1]&0xFF)==0x02)
                {
                    updateInfo(getString(R.string.received_size_error));
                    sendCmdDelayed(CMD_DISCONNECT, 0);
                }
                else if((data[1]&0xFF)==0x03)
                {
                    updateInfo(getString(R.string.write_flash_offset_error));
                    sendCmdDelayed(CMD_DISCONNECT, 0);
                }
                else if((data[1]&0xFF)==0x04)
                {
                    updateInfo(getString(R.string.segment_verify_error));
                    sendCmdDelayed(CMD_DISCONNECT, 0);
                }
                else if((data[1]&0xFF)==0x05)
                {
                    updateInfo(getString(R.string.breakpoint_error));
                    sendCmdDelayed(CMD_DISCONNECT, 0);

                }
                mOtaPacketItemCount = 0;

            }
            else if ((data[0] & 0xFF) == 0x8D) {
                Log.e("fanxiaoli fanxiaoli 8d",ArrayUtil.toHex(data)+"");
                Log.e("onReceive","CMD_RESUME_OTA_CHECK_MSG_RESPONSE");
                removeTimeout();
                byte[] breakpoint = new byte[4];
                breakpoint = extractBytes(data,1,4);
                Log.e("extractBytes",ArrayUtil.toHex(breakpoint)+"");
                if(ArrayUtil.isEqual(breakpoint,new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}))
                {
                    resumeSegment = 0;
                    resumeFlg = false;
                    Log.e("resume","error");
                }
                else if(ArrayUtil.isEqual(breakpoint,new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00}))
                {
                    Log.e("resume","from 0 fanxiaoli");
                    resumeFlg = false;
                    resumeSegment = 0;
                    sendCmdDelayed(CMD_SEND_FILE_INFO, 0);
                    LogUtils.writeForOTAStatic(TAG , "onConnected ");
                    updateInfo(R.string.connected);
                    mState = STATE_CONNECTED;
                    reconnectTimes = 0 ;
                    byte[] randomCode = new byte[32];
                    randomCode = extractBytes(data,5,32);
                    String randomCodeStr= ArrayUtil.toHex(randomCode);
                    LOG(TAG , "random_code_str  fanxiaoli= "+randomCodeStr);
                    SPHelper.putPreference(this.getApplicationContext(), Constants.KEY_OTA_RESUME_VERTIFY_RANDOM_CODE, randomCodeStr);
                    randomCodeStr = (String) SPHelper.getPreference(this.getApplicationContext(), Constants.KEY_OTA_RESUME_VERTIFY_RANDOM_CODE, "");
                    if(randomCodeStr==null) {
                        Log.e("KEY_OT_VE_RANDOM_CODE", "null    null");
                    }
                    else
                    {
                        Log.e("KEY_OT_VE_RANDOM_CODE", randomCodeStr);
                    }
                }
                else
                {

                    int segment = ArrayUtil.bytesToIntLittle(breakpoint);
                    Log.e("segment",segment+"");
                    if(segment!=0) {
                        resumeFlg = true;
                        mOtaPacketCount = segment/(1024*4) ;
                        updateInfo(getString(R.string.resume_start));
                        sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
                        Log.e("mOtaData is null", "resume mOtaPacketCount"+mOtaPacketCount);
                        resumeFlg = false;
                        Log.e("resume", "resume mOtaPacketCount"+mOtaPacketCount);
                    }


                }

            }
            else if ((data[0] & 0xFF) == 0x87) {
                removeTimeout();
                if ((data[1] & 0xFF) == 0x01) {
				 	if(isBle()){
                 	   sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
               		 }else{
                        sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE, 0);
//                   		 sendCmdDelayed(CMD_LOAD_FILE_FOR_NEW_PROFILE_SPP, 0);
               		 }
				 } else {
                    onOtaConfigFailed();
                    sendCmdDelayed(CMD_DISCONNECT, 0);
                }
            }
        }
    }

    protected abstract void connect();

    protected abstract void disconnect();

    protected abstract int getMtu();

    protected abstract void pickDevice(int request);

    protected abstract String loadLastDeviceName();

    protected abstract void saveLastDeviceName(String name);

    protected abstract String loadLastDeviceAddress();

    protected abstract void saveLastDeviceAddress(String address);

    protected abstract boolean sendData(byte[] data);

    protected abstract boolean isBle();

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
                    Log.e(TAG  , "after time "+System.currentTimeMillis());
                    otaNext();
                    break;
                case CMD_START_OTA:
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
                    LOG(TAG , "resend the msg");
                    sendCmdDelayed(CMD_OTA_NEXT, 0);
                    break;
                case CMD_RESUME_OTA_CHECK_MSG:
                    Log.e(TAG , "CMD_RESUME_OTA_CHECK_MSG");
                    //sendCmdDelayed(CMD_OTA_NEXT, 0);
                    sendBreakPointCheckReq();
                    break;
                case CMD_RESUME_OTA_CHECK_MSG_RESPONSE:
                    Log.e(TAG , "CMD_RESUME_OTA_CHECK_MSG_RESPONSE");
                    //sendCmdDelayed(CMD_OTA_NEXT, 0);
                    break;
            }
        }
    }
	
	private final OtaConfigFragment.OtaConfigCallback mOtaConfigCallback = new OtaConfigFragment.OtaConfigCallback() {
        @Override
        public void onOtaConfigOk() {
            updateProgress(0);
            sendCmdDelayed(CMD_CONNECT, 0);
        }

        @Override
        public void onOtaConfigCancel() {

        }
    };

    @Override
    protected void onResume() {
        LOG(TAG , "onResume");
        super.onResume();
    }

    @Override
    protected void onStop() {
        LOG(TAG , "onStop");
        super.onStop();
    }



    protected void LOG(String TAG , String msg){
        if(msg != null && TAG != null){
            if(isBle()){
                LogUtils.writeForBle(TAG , msg);
            }else{
                LogUtils.writeForClassicBt(TAG , msg);
            }
        }
    }
}
