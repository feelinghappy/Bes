package com.bes.usblib.driver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import com.bes.usblib.callback.BaseUsbDriver;
import com.bes.usblib.callback.IUsbCallback;
import com.bes.usblib.contants.USBContants;
import com.bes.usblib.message.UsbMessage;
import com.bes.usblib.utils.ArrayUtil;
import com.bes.usblib.utils.FileUtils;
import com.bes.usblib.utils.LogUtils;


/**
 * CDC模式的设备驱动相关类
 */
public class BesAudioDriver extends BaseUsbDriver {
    private static final String TAG = "BESHeadSetDriver";

    private boolean mReadPakcetChecker = false;

    private UsbManager mManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;

    Context mContext ;
    IUsbCallback usbCallback ;

    PermissionBroadcastReceiver mPermissionBroadcastReceiver ;
    private static final String ACTION_REQ_PERMISSION = "com.usb.device.action.request.permission";


    public BesAudioDriver(Context context , UsbManager manager , IUsbCallback usbCallback) {
        mManager = manager;
        mContext = context;
        this.usbCallback = usbCallback ;
        mPermissionBroadcastReceiver = new PermissionBroadcastReceiver();
    }

    public boolean connect(UsbDevice usbDevice) {
        mDevice = usbDevice;
        if(null == mDevice) {
            LOG("mDevice 为空") ;
            return false ;
        }
        if(mDevice.getDeviceClass() == USBContants.USB_TYPE_CDC){
            LOG("mDevice  cdc设备") ;
            return false ;
        }
        if(mManager.hasPermission(mDevice)) {
            LOG("USB 已获取系统授权") ;
            if(connectWithPermission()) {
                return true;
            } else {
                return false;
            }
        } else {
            LOG("USB 未获取系统授权，准备请求系统授权") ;
            Intent intent = new Intent(ACTION_REQ_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mManager.requestPermission(mDevice, pendingIntent);
            return false;
        }
    }

    @Override
    public void disconnect() {
        LOG("USB 通讯断开") ;
        if(mConnection != null) {
            mConnection.close();
        }
        mDevice = null;
        mConnection = null;
        if(usbCallback != null){
            usbCallback.onConnectionStateChanged(IUsbCallback.USB_DISCONNECTED);
        }
    }

    @Override
    @Deprecated
    public boolean sendMessage(byte[] msg) {
        return false;
    }

    /**
     * 在此模式下无数据模式，不可使用
     */
    @Override
    @Deprecated
    public boolean sendMeesage(UsbMessage msg) {
        return false;
    }

    @Override
    public boolean startOta() {
        byte[] CHECK_UPDATE_ENABLE = new byte[]{(byte)'F',(byte)'W',(byte)'_',(byte)'U',(byte)'P',(byte)'D',(byte)'A',(byte)'T',(byte)'E'};
        int ret = mConnection.controlTransfer(64, 6, 0, 0, CHECK_UPDATE_ENABLE, CHECK_UPDATE_ENABLE.length, 4000);
        LOG("testContrlTransfer CHECK_UPDATE_ENABLE ret "+ ret+" data = "+ ArrayUtil.toHex(CHECK_UPDATE_ENABLE) +" ascii = "+ArrayUtil.toASCII(CHECK_UPDATE_ENABLE));
        byte[] buffer = new byte[1];
        ret = mConnection.controlTransfer(192, 12, 0, 0, buffer, 1, 0);
        LOG("testContrlTransfer CHECK_UPDATE_ENABLE BACK "+ ret+" data = "+ArrayUtil.toHex(buffer) +" ascii = "+ArrayUtil.toASCII(buffer));
        if(buffer[0] == 0x31){  // ASCII eq 1
            LOG("支持手动升级");
            byte[] SYS_REBOOT = new byte[]{(byte)'S',(byte)'Y',(byte)'S',(byte)'_',(byte)'R',(byte)'E',(byte)'B',(byte)'O',(byte)'O',(byte)'T'};
            ret = mConnection.controlTransfer(64, 6, 0, 0, SYS_REBOOT, SYS_REBOOT.length, 4000);
            LOG("testContrlTransfer SYS_REBOOT ret "+ ret+" data = "+ArrayUtil.toHex(SYS_REBOOT) +" ascii = "+ArrayUtil.toASCII(SYS_REBOOT));
            ret = mConnection.controlTransfer(192, 12, 0, 0, buffer, 1, 0);     //强制reboot
            return true;
        }else{
            LOG("不支持手动升级，仅仅支持自动升级");

        }
        return false;
    }

    @Override
    public void registerReceiver() {
        if(mContext != null && mPermissionBroadcastReceiver != null){
            try{
                unregisterReceiver();
            }catch (Exception e){

            }finally {
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_REQ_PERMISSION);
                mContext.getApplicationContext().registerReceiver(mPermissionBroadcastReceiver, filter);
            }
        }
    }

    @Override
    public void unregisterReceiver() {
        mContext.getApplicationContext().unregisterReceiver(mPermissionBroadcastReceiver);
    }

    @Override
    public int getUsbType() {
        return USBContants.USB_TYPE_AUDIO;
    }

    private class PermissionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(context != null && intent != null && intent.getAction() != null){
                String action = intent.getAction();
                if(ACTION_REQ_PERMISSION.equals(action)) {
                    Bundle bundle = intent.getExtras();
                    LOG("Audio PermissionBroadcastReceiver "+Thread.currentThread().getName());
                    boolean granted = bundle.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if(granted) {
                        if(connectWithPermission()) {
                            LOG("USB 通讯参数配置成功");
                            //TODO
                        } else {
                            LOG("USB 通讯参数配置失败");
                            //TODO
                        }
                    } else {
                        LOG("USB 授权失败");
                        if(usbCallback != null){
                            usbCallback.onConnectionStateChanged(IUsbCallback.USB_NO_PERMISSION);
                        }
                    }
                }
            }
        }
    }

    /**
     * 已授权情况下发起连接
     * @return
     */
    private boolean connectWithPermission() {
        if(mConnection != null){
            disconnect();
        }
        mConnection = mManager.openDevice(mDevice);
        if(mConnection == null){
            LOG("mManager.openDevice(mDevice) is null ret false");
            return false ;
        }
        checkProfileVersion();
        getFirmwareVersion();
        getSerialNumber();
        if(usbCallback != null){
            usbCallback.onConnectionStateChanged(IUsbCallback.USB_CONNECTED);
        }
        return true;
    }

    private void checkProfileVersion(){
        final int VERSION_LEN = 3 ;
        byte[] checkVersion = new byte[]{(byte)'C',(byte)'H',(byte)'E',(byte)'C', (byte)'K'};
        int ret = mConnection.controlTransfer(64, 6, 0, 0, checkVersion, checkVersion.length, 4000);
        LOG("testContrlTransfer checkVersion SEND "+ ret+" data = "+ArrayUtil.toHex(checkVersion) +" ascii = "+ArrayUtil.toASCII(checkVersion));
        byte[] buffer = new byte[VERSION_LEN];
        ret = mConnection.controlTransfer(192, 12, 0, 0, buffer, VERSION_LEN, 0);
        LOG("testContrlTransfer checkVersion BACK "+ ret+" data = "+ArrayUtil.toHex(buffer) +" ascii = "+ArrayUtil.toASCII(buffer));
    }

    private void getFirmwareVersion(){
        final int VERSION_LEN = 14 ;
        byte[] getFWVersion = new byte[]{(byte)'Q',(byte)'U',(byte)'E',(byte)'R', (byte)'Y',(byte)'_',(byte)'S',(byte)'W',(byte)'_',(byte)'V',(byte)'E',(byte)'R'};
        int ret = mConnection.controlTransfer(64, 6, 0, 0, getFWVersion, getFWVersion.length, 4000);
        LOG("testContrlTransfer getFirmwareVersion SEND "+ ret+" data = "+ArrayUtil.toHex(getFWVersion) +" ascii = "+ArrayUtil.toASCII(getFWVersion));
        byte[] buffer = new byte[VERSION_LEN];
        ret = mConnection.controlTransfer(192, 12, 0, 0, buffer, VERSION_LEN, 0);
        LOG("testContrlTransfer getFirmwareVersion BACK "+ ret+" data = "+ArrayUtil.toHex(buffer) +" ascii = "+ArrayUtil.toASCII(buffer));
        if(usbCallback != null){
            usbCallback.onVersionReceive(ArrayUtil.toASCII(buffer)+"");
        }
    }

    private void getSerialNumber(){
        final int VERSION_LEN = 64 ;
        byte[] getFWVersion = new byte[]{(byte)'Q',(byte)'U',(byte)'E',(byte)'R',(byte)'Y',(byte)'_',(byte)'S',(byte)'N'};
        int ret = mConnection.controlTransfer(64, 6, 0, 0, getFWVersion, getFWVersion.length, 4000);
        LOG("testContrlTransfer getSerialNumber SEND "+ ret+" data = "+ArrayUtil.toHex(getFWVersion) +" ascii = "+ArrayUtil.toASCII(getFWVersion));
        byte[] buffer = new byte[VERSION_LEN];
        ret = mConnection.controlTransfer(192, 12, 0, 0, buffer, VERSION_LEN, 0);
        LOG("testContrlTransfer getSerialNumber BACK "+ ret+" data = "+ArrayUtil.toHex(buffer) +" ascii = "+ArrayUtil.toASCII(buffer));
        if(usbCallback != null){
            usbCallback.onSerialNumberReceive(ArrayUtil.toASCII(buffer)+"");
        }
    }

    public boolean isConnected() {
        return mDevice != null && mConnection != null ;
    }


    private void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }

}
