package com.iir_eq.usb;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bes.usblib.contants.USBContants;
import com.bes.usblib.manager.BESImpl;
import com.bes.usblib.utils.ErrorStringUtils;
import com.iir_eq.R;
import com.iir_eq.ui.activity.BaseActivity;
import com.iir_eq.util.FileUtils;
import com.iir_eq.util.LogUtils;
import com.iir_eq.util.SPHelper;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

/**
 * 此部分用于演示用户定义接口到使用过程，每一步都是存手动操作
 */
public class USBOtaActivity extends BaseActivity implements View.OnClickListener , BESImpl.OnOtaCallBack{

    protected static final String KEY_USB_OTA_FILE = "usb_ota_file";
    protected static final String KEY_IS_HUMAN_MODE_OTA = "is_auto_ota";
    private static final int REQUEST_OTA_FILE = 0X00;
    BESImpl otaImpl ;

    TextView mOtaFileText ;
    Switch mHumanSwitch ;
    Button mOtaBtn ;
    Button mFilePickBtn ;
    Button mConnectBtn ;
    TextView mUsbDeviceText ;
    TextView mFwVersionText , mSerialText , mBootTypeText , mIsNeedUpdateText;
    ProgressBar mProgressBar ;
    TextView mOtaInfoText , mOtaResult;

    String mBurnFilePath ;

    boolean isFirstCreate = true ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_usb_ota);
        otaImpl = new BESImpl(this);
        otaImpl.init();
        otaImpl.setOnDataCallBack(this);
        initView();
    }

    private void initView(){
        mFwVersionText = (TextView) findViewById(R.id.usb_device_fw_version);
        mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),getString(R.string.usb_no_connect)));
        mSerialText = (TextView) findViewById(R.id.usb_device_serial_number);
        mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number), getString(R.string.usb_no_connect)));
        mBootTypeText = (TextView) findViewById(R.id.usb_device_boot_type);
        mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type), getString(R.string.usb_no_connect)));
        mIsNeedUpdateText = (TextView) findViewById(R.id.usb_device_is_need_update);
        mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update), getString(R.string.usb_no_connect)));
        mOtaFileText = (TextView) findViewById(R.id.usb_ota_file);
        mBurnFilePath = (String)SPHelper.getPreference(this , KEY_USB_OTA_FILE , "");
        mOtaFileText.setText(mBurnFilePath != null ? mBurnFilePath : getString(R.string.usb_no_ota_file));
        mHumanSwitch = (Switch) findViewById(R.id.usb_auto_ota_switch);
        mHumanSwitch.setOnClickListener(this);
        mOtaBtn = (Button) findViewById(R.id.usb_ota_by_human_btn);
        mOtaBtn.setOnClickListener(this);
        mFilePickBtn = (Button) findViewById(R.id.usb_pick_ota_file);
        mFilePickBtn.setOnClickListener(this);
        mConnectBtn = (Button) findViewById(R.id.usb_connect_btn);
        mConnectBtn.setOnClickListener(this);
        mUsbDeviceText = (TextView) findViewById(R.id.usb_device_info);
        boolean isHumanMode = (boolean)SPHelper.getPreference(this, KEY_IS_HUMAN_MODE_OTA, false) ;
        if(isHumanMode){
            mHumanSwitch.setChecked(true);
            otaImpl.setHumanOta(true , mBurnFilePath);
            Toast.makeText(this , getString(R.string.usb_ota_human_mode_message),Toast.LENGTH_LONG ).show();
        }else{
            mHumanSwitch.setChecked(false);
            otaImpl.setHumanOta(false , mBurnFilePath);
        }
        mProgressBar  = (ProgressBar) findViewById(R.id.usb_ota_progress);
        mOtaInfoText = (TextView) findViewById(R.id.usb_ota_info);
        mOtaResult = (TextView) findViewById(R.id.usb_ota_result);
    }

    private void initViewData(){
        if(otaImpl != null && otaImpl.isHadUsbDevice()){
            String fwVersion = otaImpl.getTypeCVersionString() ;
            String serialNumber = otaImpl.getSerialNumberString();
            String bootType = otaImpl.getFirmwareABInfo();
            boolean isNeedUpdate = otaImpl.detectNewFirmware(mBurnFilePath);
            mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),fwVersion));
            mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number),serialNumber));
            mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type),bootType));
            mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update),isNeedUpdate?"true":"false"));
            mConnectBtn.setEnabled(true);
            int vid = otaImpl.getVendorID();
            int pid = otaImpl.getProductID() ;
            String manufacturer  = otaImpl.getManufacturerString();
            String productName = otaImpl.getProductString() ;
            mUsbDeviceText.setText(String.format(getString(R.string.usb_info), productName , vid+"",pid+"",manufacturer));
        }
        if(otaImpl != null && otaImpl.getUsbType() == USBContants.USB_TYPE_AUDIO && isFirstCreate){
            otaImpl.connect(0);
        }
        isFirstCreate = false ;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.usb_connect_btn:
                if(otaImpl.isConnected()){
                    otaImpl.disconnect();
                }else{
                    LOG("onClick connect");
                    otaImpl.connect(0);
                }
                break;
            case R.id.usb_pick_ota_file:
                if (otaImpl.isIdle()) {
                    pickFile(REQUEST_OTA_FILE);
                }
                break;
            case R.id.usb_ota_by_human_btn:
                startOta();
                break;
            case R.id.usb_auto_ota_switch:
                SPHelper.putPreference(this , KEY_IS_HUMAN_MODE_OTA , mHumanSwitch.isChecked());
                if(mHumanSwitch.isChecked()){
                    otaImpl.setHumanOta(true , mBurnFilePath);
                }else{
                    otaImpl.setHumanOta(false , mBurnFilePath);
                }
                break;
        }
    }

    private void startOta(){
        if(otaImpl.isConnected() && otaImpl.isIdle()){
            int ret = otaImpl.download(mBurnFilePath , 0 );
            showOtaResultMsg(ret , null);
        }else{
            Toast.makeText(this , getString(R.string.usb_ota_human_disable_note),Toast.LENGTH_SHORT ).show();
        }
    }

    protected void pickFile(int request) {
        startActivityForResult(new Intent(this, FilePickerActivity.class), request);
    }

    private void onPickFile(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String file = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            SPHelper.putPreference(this, KEY_USB_OTA_FILE, file);
            mBurnFilePath = file ;
            mOtaFileText.setText(mBurnFilePath);
            boolean isNeedUpdate = otaImpl.detectNewFirmware(mBurnFilePath);
            mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update),isNeedUpdate?"true":"false"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOG("onActivityResult requestCode = "+requestCode+" resultCode = "+resultCode);
        if (requestCode == REQUEST_OTA_FILE) {
            onPickFile(resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initViewData();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(otaImpl != null){
            otaImpl.finalize();
        }
    }

    @Override
    public void onOtaProgress(int progress) {
        mOtaResult.setVisibility(View.GONE);
        mOtaInfoText.setTextColor(Color.GRAY);
        if(progress >= 0 && progress <= 100){
            mProgressBar.setProgress(progress);
            mOtaInfoText.setText(progress+"%");
        }
    }

    /**
     * USB 连接状态结果回调，由于usb连接过程是存在异步操作。没办法使用同步的方式进行结果返回。（涉及到权限弹框问题）
     * 故 connect 返回值，只能作为发起连接到状态返回 而不是结果返回
     * @param status
     */
    @Override
    public void onConnectCallback(int status) {
        showOtaInfoMsg(status);
        switch (status){
            case BESImpl.CONNECTION_DISCONNECTED:
                mOtaBtn.setEnabled(false);
                mConnectBtn.setText(getString(R.string.usb_connect_btn));
                mConnectBtn.setTextColor(Color.BLACK);
                mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),getString(R.string.usb_no_connect)));
                mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number),getString(R.string.usb_no_connect)));
                mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type), getString(R.string.usb_no_connect)));
                mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update), getString(R.string.usb_no_connect)));
                break;
            case BESImpl.CONNECTION_ATTACHED:
                mConnectBtn.setEnabled(true);
                mConnectBtn.setText(getString(R.string.usb_connect_btn));
                mConnectBtn.setTextColor(Color.BLACK);
                int vid = otaImpl.getVendorID();
                int pid = otaImpl.getProductID() ;
                String manufacturer  = otaImpl.getManufacturerString();
                String productName = otaImpl.getProductString() ;
                mUsbDeviceText.setText(String.format(getString(R.string.usb_info), productName , vid+"",pid+"",manufacturer));
                if(otaImpl.getUsbType() == USBContants.USB_TYPE_AUDIO){
                    otaImpl.connect(0);
                }
                break;
            case BESImpl.CONNECTION_CONNECTED:
                if(otaImpl.getUsbType() == USBContants.USB_TYPE_AUDIO){
                    startOta();
                }
                break;
            case BESImpl.CONNECTION_DETACHED:
                mOtaBtn.setEnabled(false);
                mConnectBtn.setEnabled(false);
                mConnectBtn.setText(getString(R.string.usb_connect_btn));
                mConnectBtn.setTextColor(Color.BLACK);
                mUsbDeviceText.setText(getString(R.string.usb_current_no_device));
                break;
            default:
        }
    }

    /**
     * public static final int CONNECTION_CONNECTED = 0 ;
     public static final int CONNECTION_FAILED_WITH_NO_DEVICE = 1 ;
     public static final int CONNECTION_FAILED_WITH_NO_BES_DEVICE=  2 ;
     public static final int CONNECTION_FAILED_IN_CDC_MODE = 3 ;
     public static final int CONNECTION_FAILED_NO_PREMISSION = 4 ;
     public static final int CONNECTION_CDC_DEVICE_GOING = 5 ;
     public static final int CONNECTION_AUDIO_DEVICE_GOING = 6 ;
     public static final int CONNECTION_DISCONNECTED = 5 ;
     public static final int CONNECTION_ATTACHED = 6 ;
     public static final int CONNECTION_DETACHED = 7 ;
     public static final int CONNECTION_IDLE = 8 ;
     * @param msgCode
     */
    private void showOtaInfoMsg(int msgCode){
        if(msgCode == otaImpl.CONNECTION_CONNECTED){
            mOtaInfoText.setTextColor(Color.GREEN);
            mOtaBtn.setEnabled(true);
            mConnectBtn.setText(getString(R.string.usb_diconnect_btn));
            mConnectBtn.setTextColor(Color.RED);
            String fwVersion = otaImpl.getTypeCVersionString() ;
            String serialNumber = otaImpl.getSerialNumberString();
            String bootType = otaImpl.getFirmwareABInfo();
            boolean isNeedUpdate = otaImpl.detectNewFirmware(mBurnFilePath);
            mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),fwVersion));
            mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number),serialNumber));
            mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type),bootType));
            mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update),isNeedUpdate?"true":"false"));
        }else if(msgCode == otaImpl.CONNECTION_CDC_DEVICE_GOING || msgCode == otaImpl.CONNECTION_IDLE ||
                msgCode == otaImpl.CONNECTION_AUDIO_DEVICE_GOING || msgCode == otaImpl.CONNECTION_ATTACHED){
            mOtaInfoText.setTextColor(Color.GRAY);
        }else{
            mOtaInfoText.setTextColor(Color.RED);
            mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),getString(R.string.usb_no_connect)));
            mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number),getString(R.string.usb_no_connect)));
            mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type), getString(R.string.usb_no_connect)));
            mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update), getString(R.string.usb_no_connect)));
        }
        mOtaInfoText.setText(ErrorStringUtils.getConnectStatusString(msgCode));
    }

    /**
     * public static final int DOWNLOAD_FILE_NOT_FOUND = -1 ;
     public static final int DOWNLOAD_VERSION_IS_SAME = - 2;
     public static final int DOWNLOAD_NO_DEVICE_CONNECTED = -3 ;
     public static final int DOWN_IN_CDC_DEVICE = - 5;
     public static final int DOWN_ONLY_SUPPORT_AUTO_OTA = -6 ;
     public static final int DOWNLOAD_READ_FILE_VERSION_FIALED  = -7 ;
     public static final int DOWNLAOD_READY = 0 ;
     * @param msgCode
     */
    void showOtaResultMsg(int msgCode , String msgString){
        if(msgCode == otaImpl.DOWNLOAD_VERSION_IS_SAME){
            mOtaInfoText.setTextColor(Color.GREEN);
            mOtaResult.setVisibility(View.GONE);
            mOtaResult.setTextColor(Color.GREEN);
            mOtaResult.setText("");
        }else if(msgCode == otaImpl.DOWNLAOD_READY ){
            mOtaInfoText.setTextColor(Color.GRAY);
            mOtaResult.setVisibility(View.GONE);
            mOtaResult.setTextColor(Color.GREEN);
        }else if(msgCode == otaImpl.OTA_DONE){
            mOtaResult.setTextColor(Color.GREEN);
            mOtaResult.setVisibility(View.VISIBLE);
            mOtaResult.setText("升级成功");
        }else{
            mOtaInfoText.setTextColor(Color.RED);
            mOtaResult.setVisibility(View.VISIBLE);
            mOtaResult.setTextColor(Color.RED);
            mOtaResult.setText("升级失败");
        }
        mOtaInfoText.setText(ErrorStringUtils.getDownloadStatusString(msgCode , msgString));
    }

    @Override
    public void onOtaStart() {
        if(mProgressBar != null){
            mProgressBar.setProgress(0);
        }
    }

    @Override
    public void onOtaDone() {
        if(mProgressBar != null){
            mProgressBar.setProgress(100);
        }
       showOtaResultMsg(otaImpl.OTA_DONE , null);
    }

    @Override
    public void onOtaError(int msgCode , String msgString) {
        showOtaResultMsg(msgCode , msgString);
    }

    void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }



}
