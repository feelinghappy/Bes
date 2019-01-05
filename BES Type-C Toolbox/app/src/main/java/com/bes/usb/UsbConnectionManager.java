package com.bes.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.MutableContextWrapper;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import com.bes.usblib.callback.IUsbCallback;
import com.bes.usblib.contants.USBContants;
import com.bes.usblib.utils.FileUtils;
import com.bes.usblib.utils.LogUtils;

import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * USB 连接管理类。此类非工程库所需，演示库调用辅助类
 * Created by alloxuweibin on 2017/10/13.
 */

public class UsbConnectionManager {

    private static final String TAG = "UsbConnectionManager";
    private static final String ACTION_REQ_PERMISSION = "com.usb.device.action.request.permission";

    public static final int CONNECTION_CONNECTED = 0 ;
    public static final int CONNECTION_FAILED_CONNECTION_NULL = 1 ;
    public static final int CONNECTION_FAILED_NO_PERMISSION = 2 ;

    Context mContext ;
    UsbManager mUsbManager ;
    UsbDevice  mUsbDevice ;
    UsbDeviceConnection mUsbDeviceConnection ;
    PermissionBroadcastReceiver mPermissionBroadcastReceiver ;

    public UsbConnectionManager(Context context) {
        mContext = context;
        mPermissionBroadcastReceiver = new PermissionBroadcastReceiver();
        init();
        registerReceiver();
    }

    public UsbDevice getmUsbDevice(){
        return mUsbDevice ;
    }

    public UsbDeviceConnection getmUsbDeviceConnection(){
        return mUsbDeviceConnection ;
    }

    /**
     * 初始化
     * @return
     */
    private boolean init(){
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        if(null == mUsbManager) {
            LOG("Get UsbManager fail! null == usbManager") ;
        } else {
            HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
            LOG("deviceMap.size(): "+ deviceMap.size()) ;
            if(!deviceMap.isEmpty()) {
                Iterator<UsbDevice> iterator = deviceMap.values().iterator();
                while(iterator.hasNext()) {
                    mUsbDevice = iterator.next();
                    LOG(" find usb device name =  "+mUsbDevice.getProductName()) ;
                }
                return true;
            } else {
                return false ;
            }
        }
        return  false ;
    }

    public boolean connect(UsbDevice usbDevice) {
        mUsbDevice = usbDevice;
        if(null == mUsbDevice || null == mUsbManager) {
            LOG("mDevice 为空") ;
            return false ;
        }
        if(mUsbManager.hasPermission(mUsbDevice)) {
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
            mUsbManager.requestPermission(mUsbDevice, pendingIntent);
            return false;
        }
    }

    /**
     * 已授权情况下发起连接
     * @return
     */
    private boolean connectWithPermission() {
        if(mUsbDeviceConnection != null){
            disconnect(true);
        }
        mUsbDeviceConnection = mUsbManager.openDevice(mUsbDevice);
        if(mUsbDeviceConnection == null){
            LOG("mManager.openDevice(mDevice) is null ret false");
            return false ;
        }
        if(onConnectionChangerCallBack != null){
            onConnectionChangerCallBack.onConnected(mUsbDevice , mUsbDeviceConnection);
        }
        return true;
    }

    public void disconnect(boolean isBesDevice) {
        LOG("USB 通讯断开") ;
        if(mUsbDeviceConnection != null) {
            mUsbDeviceConnection.close();
            mUsbDeviceConnection = null;
        }
        if(onConnectionChangerCallBack != null){
            onConnectionChangerCallBack.onDisConnected(isBesDevice);
        }
    }

    /**
     * 取消连接前判断id有效性，使用vender指令代替，通过连接成功后调用 getProfileVersion 接口为空进行判断有效性
     * @param device
     * @return
     */
    @Deprecated
    public boolean isBESDevie(UsbDevice device){
//        if(USBContants.BES_CDC_PRODUCT_ID == device.getProductId() && USBContants.BES_CDC_VENDER_ID == device.getVendorId()){
//            return true ;
//        }
//        if(USBContants.BES_AUDIO_PRODUCT_ID == device.getProductId() && USBContants.BES_AUDIO_VENDER_ID == device.getVendorId()){
//            return true ;
//        }
//        return false ;
        return  true;
    }

    public void release(){
        unregisterReceiver();
        disconnect(true);
    }

    private void registerReceiver() {
        if(mContext != null && mPermissionBroadcastReceiver != null){
            try{
                unregisterReceiver();
            }catch (Exception e){

            }finally {
                IntentFilter filter = new IntentFilter();
                filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
                filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                filter.addAction(ACTION_REQ_PERMISSION);
                mContext.registerReceiver(mPermissionBroadcastReceiver, filter);
            }
        }
    }

    private void unregisterReceiver() {
        mContext.unregisterReceiver(mPermissionBroadcastReceiver);
    }

    class PermissionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.getAction() != null){
                String action = intent.getAction();
                if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    LOG("UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)");
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if(onConnectionChangerCallBack != null){
                        onConnectionChangerCallBack.onAttachedCallBack(device);
                    }
                    mUsbDevice = device;
                    connect(device);
                }else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
                    LOG("UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)");
                    if(onConnectionChangerCallBack != null){
                        onConnectionChangerCallBack.onDetachedCallBack();
                    }
                    disconnect(true);
                    mUsbDevice = null ;
                }else if(ACTION_REQ_PERMISSION.equals(action)) {
                    Bundle bundle = intent.getExtras();
                    boolean granted = bundle.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if(granted) {
                        if(connectWithPermission()) {
                            LOG("USB 连接成功");
                        } else {
                            LOG("USB 连接失败");
                            if(onConnectionChangerCallBack != null){
                                onConnectionChangerCallBack.onConnectionFailed(CONNECTION_FAILED_CONNECTION_NULL);
                            }
                        }
                    } else {
                        LOG("USB 授权失败");
                        if(onConnectionChangerCallBack != null){
                            onConnectionChangerCallBack.onConnectionFailed(CONNECTION_FAILED_NO_PERMISSION);
                        }
                    }
                }
            }
        }
    }

    OnConnectionChangerCallBack onConnectionChangerCallBack ;
    public void setOnConnectionChangerCallBack(OnConnectionChangerCallBack onConnectionChangerCallBack){
        this.onConnectionChangerCallBack = onConnectionChangerCallBack ;
    }

    public interface OnConnectionChangerCallBack{
        void onConnected(UsbDevice usbDevice , UsbDeviceConnection usbDeviceConnection);
        void onDisConnected(boolean isBesDevice);
        void onConnectionFailed(int status);
        void onAttachedCallBack(UsbDevice usbDevice);
        void onDetachedCallBack();
    }

    private void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }

}
