package com.bes.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bes.usb.UsbConnectionManager;
import com.bes.usblib.manager.BESImpl;
import com.bes.usblib.utils.ErrorStringUtils;
import com.bes.util.CommUtils;
import com.bes.util.LogUtils;
import com.bes.util.SPHelper;
import com.iir_eq.R;
import com.bes.util.FileUtils;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;


import butterknife.BindView;

/**
 * 此部分用于演示用户定义接口到使用过程，每一步都是存手动操作
 */
public class USBOtaActivity extends BaseActivity implements View.OnClickListener {
    private final static String TAG = "USBOtaActivity";
    /**
     * OTA升级文件路径
     */
    protected static final String KEY_USB_OTA_FILE = "my_usb_ota_file_";
    /**
     * 升级文件选择跳转
     */
    private static final int REQUEST_OTA_FILE = 0X00;
    /**
     * bes USB 实现类
     */
    BESImpl otaImpl;
    /**
     * 选择升级文件路径
     */
    String mBurnFilePath;
    /**
     * 升级文件textview
     */
    TextView mOtaFileText;
    /**
     * 开始升级按钮 / 选择文件按钮 ／ 连接按钮
     */
    Button mOtaStartBtn, mFilePickBtn, mConnectBtn;
    /**
     * USB设备名称 / USB固件版本 / USB SN 号（非枚举出来的数据，需要通过vender指令获取） / 设备当前运行镜像类型 ／ 匹配文件结果是否需要升级
     */
    TextView mUsbDeviceText, mFwVersionText, mSerialText, mBootTypeText, mIsNeedUpdateText;
    /**
     * 升级进度条
     */
    ProgressBar mProgressBar;
    /**
     * 升级过程文字描述及结果描述文本
     */
    TextView mOtaInfoText, mOtaResult;

    UsbConnectionManager mUsbDeviceConnectionManager;

    final int AUDIO_CONNECTED = 0X01 ;
    Handler uiHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == AUDIO_CONNECTED){
                String profileVersion = otaImpl.getProfileVersion();     //如果您是在连接前就已判断PID VID有效性，可不做此判断
                LOG(">>>> AUDIO_CONNECTED and profileVersion = "+profileVersion);
                if(profileVersion == null || "".equals(profileVersion)){ //获取不到协议版本信息，则判断为非bes设备，执行自动断开
                    mUsbDeviceConnectionManager.disconnect(false);
                }else{
                    showConnectedViewWithAudio();
                }
            }
        }
    };

    UsbConnectionManager.OnConnectionChangerCallBack onConnectionChangerCallBack = new UsbConnectionManager.OnConnectionChangerCallBack() {
        @Override
        public void onConnected(final UsbDevice usbDevice,final UsbDeviceConnection usbDeviceConnection) {
            if (mUsbDeviceConnectionManager.isBESDevie(usbDevice)) {
                showAttachedView(usbDevice);
                HandlerThread handlerThread = new HandlerThread("usb");
                handlerThread.start();
                Handler handler = new Handler(handlerThread.getLooper()){
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        otaImpl = new BESImpl();
                        otaImpl.init(USBOtaActivity.this, usbDeviceConnection, usbDevice);
                        if (otaImpl.isCdcDevice() && isOtaMode) {
                            progressHandler.sendEmptyMessage(UPDATE_UI);
                            if(otaImpl != null){
                                int ret = otaImpl.download(mBurnFilePath);
                                if (ret == otaImpl.DOWNLAOD_DONE) {
                                    progressHandler.obtainMessage(OTA_DONE).sendToTarget();
                                } else {
                                    progressHandler.obtainMessage(OTA_FAILED , ret).sendToTarget();
                                }
                                isOtaMode = false ;
                            }else {
                                LOG("otaImpl is null");
                            }
                        } else if (!otaImpl.isCdcDevice()) {
                            LOG("!otaImpl.isCdcDevice() && !isAudioFlag");
                            isOtaMode = false;
                            uiHandler.sendEmptyMessage(AUDIO_CONNECTED);
                        }
                    }
                };
                Message message = Message.obtain();
                message.obj = "usb connect";
                handler.sendMessage(message);
            } else {
                //TODO
                Toast.makeText(USBOtaActivity.this, "非 bes 设备", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onDisConnected(boolean isBesDevice) {
            if(isBesDevice){
                showDisconeectView();
            }else{
                showNoBesDevice();
            }

        }

        @Override
        public void onConnectionFailed(int status) {
            if (status == UsbConnectionManager.CONNECTION_FAILED_CONNECTION_NULL) {
                showConnectFailed("连接失败 usb connection 为空");
            } else if (status == UsbConnectionManager.CONNECTION_FAILED_NO_PERMISSION) {
                showConnectFailed("未获得usb 授权");
            }
        }

        @Override
        public void onAttachedCallBack(UsbDevice usbDevice) {
            showAttachedView(usbDevice);
        }

        @Override
        public void onDetachedCallBack() {
            LOG("onDetachedCallBack()");
            if(otaImpl != null){
                otaImpl.finalize();
                otaImpl = null ;
            }
            progressHandler.removeMessages(UPDATE_UI);
            showDetachedView();
        }
    };


    final int UPDATE_UI = 1 ;
    final int OTA_DONE = 2 ;
    final int OTA_FAILED = 3 ;
    boolean isOtaMode = false ;
    Handler progressHandler =  new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LOG("msg.what == UPDATE_UI ");
            if(msg.what == UPDATE_UI){
                if(isOtaMode){
                    LOG("handleMessage otaImpl != null && isOtaMode true");
                    if(otaImpl != null){
                        showProgress(otaImpl.getProcessPerCent());
                    }
                    progressHandler.postDelayed(progressRunnable , 50);
                }else{
                    LOG("handleMessage otaImpl != null && isOtaMode false");
                }
            }else if(msg.what == OTA_DONE){
                showProgress(100);
                showOTADone();
            }else if(msg.what == OTA_FAILED){
                int retCode = (int) msg.obj;
                showOTAFailed(retCode);
            }
        }
    };

    Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            progressHandler.sendEmptyMessage(UPDATE_UI);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.act_usb_ota);
        initView();
        initData();
        mUsbDeviceConnectionManager = new UsbConnectionManager(this);
        mUsbDeviceConnectionManager.setOnConnectionChangerCallBack(onConnectionChangerCallBack);
        if(mUsbDeviceConnectionManager.getmUsbDevice() != null){
            showAttachedView(mUsbDeviceConnectionManager.getmUsbDevice());
        }
    }

    private void initView(){
        mUsbDeviceText = (TextView) findViewById(R.id.usb_device_info);
        mFwVersionText = (TextView) findViewById(R.id.usb_device_fw_version);
        mSerialText = (TextView) findViewById(R.id.usb_device_serial_number);
        mBootTypeText = (TextView) findViewById(R.id.usb_device_boot_type);
        mIsNeedUpdateText = (TextView) findViewById(R.id.usb_device_is_need_update);
        mOtaFileText = (TextView) findViewById(R.id.usb_ota_file);
        mOtaStartBtn = (Button) findViewById(R.id.usb_ota_start_btn);
        mFilePickBtn = (Button) findViewById(R.id.usb_pick_ota_file);
        mConnectBtn = (Button) findViewById(R.id.usb_connect_btn);
        mProgressBar  = (ProgressBar) findViewById(R.id.usb_ota_progress);
        mOtaInfoText = (TextView) findViewById(R.id.usb_ota_info);
        mOtaResult = (TextView) findViewById(R.id.usb_ota_result);
        mOtaStartBtn.setOnClickListener(this);
        mFilePickBtn.setOnClickListener(this);
        mConnectBtn.setOnClickListener(this);
    }

    private void initData(){
        mBurnFilePath = (String) SPHelper.getPreference(this , KEY_USB_OTA_FILE , "");
        mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),getString(R.string.usb_no_connect)));
        mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number), getString(R.string.usb_no_connect)));
        mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type), getString(R.string.usb_no_connect)));
        mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update), getString(R.string.usb_no_connect)));
        if(mBurnFilePath != null && !"".equals(mBurnFilePath)){
            mOtaFileText.setText(mBurnFilePath);
        }else{
            mOtaFileText.setText(getString(R.string.usb_no_ota_file));
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.usb_connect_btn:
                if(mUsbDeviceConnectionManager != null &&
                        mUsbDeviceConnectionManager.getmUsbDevice() != null){
                    if(mUsbDeviceConnectionManager.getmUsbDeviceConnection() != null){
                        mUsbDeviceConnectionManager.disconnect(true);
                    }else{
                        mUsbDeviceConnectionManager.connect(mUsbDeviceConnectionManager.getmUsbDevice());
                    }
                }else{
                    Toast.makeText(this , "没有USB 设备",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.usb_pick_ota_file:
                if (otaImpl != null && !otaImpl.isIdle()) {
                    Toast.makeText(this , "升级状态下不能选择文件",Toast.LENGTH_SHORT).show();
                }else{
                    pickFile(REQUEST_OTA_FILE , false);
                }
                break;
            case R.id.usb_ota_start_btn:
                if(otaImpl != null && !otaImpl.isCdcDevice()){
                    if(otaImpl.detectNewFirmware(mBurnFilePath)){
                       boolean  ret =  otaImpl.sendCdcCmd();
                       if(ret){
                           isOtaMode = true ;
                            Toast.makeText(this , "等待重启升级" , Toast.LENGTH_SHORT).show();
                       }else{
                           Toast.makeText(this , "发送进入cdc指令失败" , Toast.LENGTH_SHORT).show();
                       }
                    }else{
                        mOtaResult.setVisibility(View.GONE);
                        mOtaInfoText.setTextColor(Color.RED);
                        mOtaInfoText.setText("当前升级版本不匹配 "+ErrorStringUtils.getDetectFailedString(otaImpl.getDetectVersionCode()));
                    }
                }else{
                    Toast.makeText(this , "当前已在cdc状态 或设备已拔出",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    protected void pickFile(int request , boolean isFolder) {
        Intent intent = new Intent(this, FilePickerActivity.class) ;
        intent.putExtra(FilePickerActivity.ARG_FORLDER , isFolder);
        startActivityForResult(intent, request);
    }

    private void onPickFile(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String file = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            SPHelper.putPreference(this, KEY_USB_OTA_FILE, file);
            mBurnFilePath = file;
            mOtaFileText.setText(mBurnFilePath);
        }
        boolean isNeedUpdate = false ;
        if(otaImpl != null){
            isNeedUpdate = otaImpl.detectNewFirmware(mBurnFilePath);
        }
        mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update),isNeedUpdate?"true":"false"));
//        if(otaImpl.isConnected()){
//            mOtaInfoText.setTextColor(isNeedUpdate?Color.GREEN:Color.RED);
//            mOtaInfoText.setText(isNeedUpdate?"文件可升级":"升级文件不匹配，请重新选择");
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OTA_FILE) {
            onPickFile(resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            otaImpl = null ;
        }
        if(mUsbDeviceConnectionManager != null){
            mUsbDeviceConnectionManager.release();
        }
    }


    /**
     * 显示升级过程信息
     */
    private void showProgress(int progress){
        mOtaInfoText.setTextColor(Color.GRAY);
        mOtaInfoText.setText(progress+" %");
        mProgressBar.setProgress(progress);
    }

    /**
     * 此处库还未初始化，故使用device做usb 信息显示
     * @param usbDevice
     */
    void showAttachedView(UsbDevice usbDevice){
        int vid = usbDevice.getVendorId();
        int pid = usbDevice.getProductId() ;
        String deviceName = usbDevice.getProductName();
        String manufacturer  = usbDevice.getManufacturerName();
        mUsbDeviceText.setText(String.format(getString(R.string.usb_info), deviceName , "0X"+Integer.toHexString(vid),"OX"+Integer.toHexString(pid),manufacturer));
        mConnectBtn.setEnabled(true);
        mConnectBtn.setText(getString(R.string.usb_connect_btn));
        mConnectBtn.setTextColor(Color.BLACK);
    }

    void showDetachedView(){
        mOtaStartBtn.setEnabled(false);
        mConnectBtn.setEnabled(false);
        mConnectBtn.setText(getString(R.string.usb_connect_btn));
        mConnectBtn.setTextColor(Color.BLACK);
        mUsbDeviceText.setText(getString(R.string.usb_current_no_device));
        mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),getString(R.string.usb_no_connect)));
        mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number),getString(R.string.usb_no_connect)));
        mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type), getString(R.string.usb_no_connect)));
        mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update), getString(R.string.usb_no_connect)));
    }

    void showDisconeectView(){
        mOtaStartBtn.setEnabled(false);
        mConnectBtn.setEnabled(true);
        mConnectBtn.setText(getString(R.string.usb_connect_btn));
        mConnectBtn.setTextColor(Color.BLACK);
        mOtaInfoText.setTextColor(Color.RED);
        mOtaInfoText.setText("已断开");
        mUsbDeviceText.setText(getString(R.string.usb_current_no_device));
        mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),getString(R.string.usb_no_connect)));
        mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number),getString(R.string.usb_no_connect)));
        mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type), getString(R.string.usb_no_connect)));
        mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update), getString(R.string.usb_no_connect)));
    }

    void showNoBesDevice(){
        mOtaStartBtn.setEnabled(false);
        mConnectBtn.setEnabled(true);
        mConnectBtn.setText(getString(R.string.usb_connect_btn));
        mConnectBtn.setTextColor(Color.BLACK);
        mOtaInfoText.setTextColor(Color.RED);
        mOtaInfoText.setText("自动断开,判断为非 BES 设备");
        mUsbDeviceText.setText(getString(R.string.usb_current_no_device));
        mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),getString(R.string.usb_no_connect)));
        mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number),getString(R.string.usb_no_connect)));
        mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type), getString(R.string.usb_no_connect)));
        mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update), getString(R.string.usb_no_connect)));
    }

    void showConnectFailed(String msg){
        mOtaStartBtn.setEnabled(false);
        mConnectBtn.setEnabled(true);
        mConnectBtn.setText(getString(R.string.usb_connect_btn));
        mConnectBtn.setTextColor(Color.BLACK);
        mUsbDeviceText.setText(getString(R.string.usb_connect_failed)+ msg);
        mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),getString(R.string.usb_no_connect)));
        mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number),getString(R.string.usb_no_connect)));
        mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type), getString(R.string.usb_no_connect)));
        mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update), getString(R.string.usb_no_connect)));
    }

    void showConnectedViewWithAudio(){
        mOtaResult.setVisibility(View.GONE);
        mOtaInfoText.setTextColor(Color.GREEN);
        mOtaStartBtn.setEnabled(true);
        mConnectBtn.setText(getString(R.string.usb_diconnect_btn));
        mConnectBtn.setTextColor(Color.RED);
        if(otaImpl != null){
            String fwVersion = otaImpl.getTypeCVersionString() ;
            String serialNumber = otaImpl.getSerialNumberString();
            String bootType = otaImpl.getFirmwareABInfo();
            boolean isNeedUpdate = otaImpl.detectNewFirmware(mBurnFilePath);
            mFwVersionText.setText(String.format(getString(R.string.usb_ota_fw_version),fwVersion));
            mSerialText.setText(String.format(getString(R.string.usb_ota_serial_number),serialNumber));
            mBootTypeText.setText(String.format(getString(R.string.usb_ota_boot_type),bootType));
            mIsNeedUpdateText.setText(String.format(getString(R.string.usb_ota_is_need_update),isNeedUpdate?"true":"false"));
        }
        mOtaInfoText.setTextColor(Color.GREEN);
        mOtaInfoText.setText("已连接");
    }

    void showConnectedViewWithCDC(){
        mOtaInfoText.setTextColor(Color.GREEN);
        mOtaStartBtn.setEnabled(true);
        mConnectBtn.setText(getString(R.string.usb_diconnect_btn));
        mConnectBtn.setTextColor(Color.RED);
        mOtaInfoText.setTextColor(Color.GREEN);
        mOtaInfoText.setText("已连接");
    }

    void showOTADone(){
        mOtaResult.setTextColor(Color.GREEN);
        mOtaResult.setVisibility(View.VISIBLE);
        mOtaResult.setText("升级成功");
    }

    void showOTAFailed(int retCode){
        mOtaResult.setVisibility(View.VISIBLE);
        mOtaResult.setTextColor(Color.RED);
        String retCodeString = ErrorStringUtils.getDownloadStatusString(retCode);
        mOtaResult.setText("升级失败" + retCodeString);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:
                moreInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private PopupWindow mInfo;
    @BindView(R.id.container)
    View mContainer;
    private void moreInfo() {
        if (mInfo != null && mInfo.isShowing()) {
            mInfo.dismiss();
            return;
        }
        if (mInfo == null) {
            View view = getLayoutInflater().inflate(R.layout.wnd_info, null);
            mInfo = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            mInfo.setOutsideTouchable(true);
            mInfo.setBackgroundDrawable(new ColorDrawable(0x00000000));
            final TextView version = (TextView) view.findViewById(R.id.version);
            String versionName = CommUtils.getVersion(USBOtaActivity.this);
            version.setText(versionName);

        }
        mInfo.showAsDropDown(mContainer, mContainer.getWidth(), -mContainer.getHeight());
    }

    void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }


}
