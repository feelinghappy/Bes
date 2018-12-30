package com.bes.usblib.driver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;


import com.bes.usblib.callback.BaseUsbDriver;
import com.bes.usblib.callback.IUsbCallback;
import com.bes.usblib.contants.USBContants;
import com.bes.usblib.message.UsbMessage;
import com.bes.usblib.utils.ArrayUtil;
import com.bes.usblib.utils.FileUtils;
import com.bes.usblib.utils.LogUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * CDC模式的设备驱动相关类
 */
public class BesCdcDriver extends BaseUsbDriver {
    private static final String TAG = "BESDriver";

    private boolean mReadPakcetChecker = false;

    private UsbManager mManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    /**
     * cdc 模式下bulk方式的端点信息，输入属性端点
     */
    private UsbEndpoint mCDCEndpointIN;
    /**
     * cdc 模式下bulk方式的端点信息，输出属性端点
     */
    private UsbEndpoint mCDCEndpointOUT;
    /**
     * cdc 模式下的接口信息，cdc模块下配置信息下定义了了两个接口信息。
     * 其中一个使用通用接口
     * 另一个cdc 数据接口，此接口下挂载两个端点信息，分别作为输入输出使用，采用bulk方式
     */
    private UsbInterface mCDCInterface ;
    /**
     * cdc 模式下的接口信息，cdc模块下配置信息下定义了了两个接口信息。
     * 其中一个使用通用接口
     * 另一个cdc 数据接口，此接口下挂载两个端点信息，分别作为输入输出使用，采用bulk方式
     */
    private UsbInterface mCommInterface ;

    /**
     * 上下文
     */
    Context mContext ;
    /**
     * USB枚举广播通知
     */
    PermissionBroadcastReceiver mPermissionBroadcastReceiver ;
    private static final String ACTION_REQ_PERMISSION = "com.usb.device.action.request.permission";
    private XwInputThread mUSBInputThread;
    private XwOutputThread mUSBOutputThread;
    private int maxPacketSize = 64 ;

    IUsbCallback usbCallback ;


    public BesCdcDriver(Context context , UsbManager manager , IUsbCallback usbCallback) {
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
        if(mDevice.getDeviceClass() != USBContants.USB_TYPE_CDC){
            LOG("mDevice 非 cdc设备") ;
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
        stopThread();
        if(mConnection != null) {
            if(mCDCInterface != null) {
                mConnection.releaseInterface(mCDCInterface);
                mCDCInterface = null;
            }
            if(mCommInterface != null) {
                mConnection.releaseInterface(mCommInterface);
                mCommInterface = null;
            }
            mConnection.close();
        }
        mDevice = null;
        mConnection = null;
    }

    @Override
    @Deprecated
    public boolean sendMessage(byte[] msg) {
        return false;
    }

    /**
     * 发送usb消息数据,数据长度问题由外部控制，此处不做处理
     * @param msg
     * @return
     */
    @Override
    public boolean sendMeesage(UsbMessage msg) {
        if(mUSBOutputThread != null){
            mUSBOutputThread.sendMessage(msg);
            return  true ;
        }
        return false;
    }

    /**
     * 此接口只支持手动升级  cdc设备无此功能
     * @return
     */
    @Override
    @Deprecated
    public boolean startOta() {
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
        return USBContants.USB_TYPE_CDC;
    }

    private class PermissionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(context != null && intent != null && intent.getAction() != null){
                String action = intent.getAction();
                if(ACTION_REQ_PERMISSION.equals(action)) {
                    Bundle bundle = intent.getExtras();
                    LOG("CDC PermissionBroadcastReceiver ");
                    boolean granted = bundle.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if(granted) {
                        if(connectWithPermission()) {
                            LOG("USB 通讯参数配置成功");
                        } else {
                            LOG("USB 通讯参数配置失败");
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
        for(int i = 0 ; i < mDevice.getInterfaceCount() ; i++){
            final UsbInterface usbInterface = mDevice.getInterface(i);
            if (mConnection.claimInterface(usbInterface, true) && usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA) {
                LOG("the number interface i = "+i+" class = "+ usbInterface.getInterfaceClass()) ;
                mCDCInterface = usbInterface;
                for (int j = 0;j < usbInterface.getEndpointCount(); ++j) {
                    final UsbEndpoint endpoint = usbInterface.getEndpoint(j);
                    switch (endpoint.getType()) {
                        case UsbConstants.USB_ENDPOINT_XFER_INT: //中断方式
                            LOG("found  USB endpoint the Type is  USB_ENDPOINT_XFER_INT" ) ;
                            if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                LOG("found USB_DIR_OUT in USB_ENDPOINT_XFER_INT type" ) ;
                            } else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN){
                                LOG("found USB_DIR_IN in USB_ENDPOINT_XFER_INT type" ) ;
                            }
                            break;
                        case UsbConstants.USB_ENDPOINT_XFER_BULK: //大数据方式
                            LOG("found USB endpoint the Type is  USB_ENDPOINT_XFER_BULK" +"\t and the getMaxPacketSize = "+endpoint.getMaxPacketSize()) ;
                            if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                LOG("found USB_DIR_OUT in USB_ENDPOINT_XFER_INT type" ) ;
                                mCDCEndpointOUT = endpoint ;
                            } else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN){
                                LOG("found USB_DIR_IN in USB_ENDPOINT_XFER_INT type" ) ;
                                mCDCEndpointIN = endpoint;
                                maxPacketSize = endpoint.getMaxPacketSize() <= 0 ? 64 : endpoint.getMaxPacketSize();
                            }
                            break;
                        case UsbConstants.USB_ENDPOINT_XFER_CONTROL: //控制方式
                            LOG("found  USB endpoint the Type is  USB_ENDPOINT_XFER_CONTROL" ) ;
                            if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                LOG("found USB_DIR_OUT in USB_ENDPOINT_XFER_CONTROL type" ) ;
                            } else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN){
                                LOG("found USB_DIR_IN in USB_ENDPOINT_XFER_CONTROL type" ) ;
                            }
                            break;
                        case UsbConstants.USB_ENDPOINT_XFER_ISOC: //异步方式
                            LOG("found  USB endpoint the Type is  USB_ENDPOINT_XFER_ISOC" ) ;
                            if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                LOG("found USB_DIR_OUT in USB_ENDPOINT_XFER_ISOC type" ) ;
                            } else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN){
                                LOG("found USB_DIR_IN in USB_ENDPOINT_XFER_ISOC type" ) ;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }else if (mConnection.claimInterface(usbInterface, true) && usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_COMM){
                LOG("the number interface i = "+i+" class = "+ usbInterface.getInterfaceClass()) ;
                mCommInterface = usbInterface;;
            }else{
                continue;
            }
        }
        if(mCDCInterface != null && mCDCEndpointIN != null & mCDCEndpointOUT != null){
            int initRet = mConnection.controlTransfer(33, 34, 0, 0, (byte[])null, 0, 0);
            LOG("initCdcAcm controlTransfer ret "+ initRet);
            boolean ret =  setCdcBaudrate(USBContants.BAUD_19200);
            if(ret){
                startThread();
                return  true ;
            }
        }
        disconnect();
        return false;
    }

    private void startThread() {
        LOG("startThread() ") ;
        stopThread();
        startInputOutThread();
    }

    private void stopThread() {
        if (mUSBInputThread != null) {
            mUSBInputThread.signalShutdown();
            mUSBInputThread = null;
        }
        if (mUSBOutputThread != null) {
            mUSBOutputThread.signalShutdown();
            mUSBOutputThread = null;
        }
    }

    private void startInputOutThread(){
        mUSBOutputThread = new XwOutputThread();
        mUSBOutputThread.start();
        mUSBInputThread = new XwInputThread();
        mUSBInputThread.start();
    }

    private class XwStreamThread extends Thread {
        protected boolean shouldDie;

        public XwStreamThread(final String name) {
            super(name);
            shouldDie = false;
        }

        public void signalShutdown() {
            shouldDie = true;
            interrupt();
        }
    }

    private class XwInputThread extends XwStreamThread {
        private static final int BUFFER_CAPACITY = 64;
        private final ByteBuffer mReassemblyBuffer;

        public XwInputThread() {
            super("XwInputThread");
            mReassemblyBuffer = ByteBuffer.allocate(BUFFER_CAPACITY);
        }

        private void handle(byte[] byteBuffer) {
            LOG("input "+ ArrayUtil.toHex(byteBuffer));
            if(usbCallback != null){
                usbCallback.onDataReceive(byteBuffer);
            }
        }

        public void run() {
            byte[] buffer = new byte[64];
            while (true) {
                if (shouldDie || Thread.currentThread().isInterrupted()) {
                    break;
                }
                int ret = mConnection.bulkTransfer(mCDCEndpointIN, buffer, buffer.length, 500);
                if(ret > 0){
                    handle(Arrays.copyOfRange(buffer, 0, ret)) ;
                }
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    LOG(e.getMessage()+"");
                }
            }
        }
    }

    public class XwOutputThread extends XwStreamThread {
        private final BlockingQueue<UsbMessage> outputMessageQueue;

        public XwOutputThread() {
            super("XwOutputThread");
            outputMessageQueue = new LinkedBlockingQueue<UsbMessage>();
        }

        public void run() {
            while (true) {
                if (shouldDie) {
                    boolean interrupted = Thread.currentThread().isInterrupted();
                    if (interrupted) {
                        break;
                    }
                }
                try {
                    UsbMessage message = outputMessageQueue.take();
                    byte[] data = message.getBytes() ;
                    if(data != null && data.length > 0){
                        int len = data.length ;
                        boolean isFirst = true ;
                        int position = 0 ;
                        byte[] temp = null;
                        long time = System.currentTimeMillis() ;
                        int msgCount = (len+4096-1)/4096;
                        LOG(" len \t"+len+" msgCount = "+ msgCount+" seq = "+ ArrayUtil.toHex(new byte[]{(byte)(message.getSeq()&0xff)})) ;
                        for (int i = 0 ; i < msgCount ; i++){
                            int payLoad = 4096 ;
                            if(i == msgCount - 1){
                                if(len < 4096){
                                    payLoad = len  ;
                                }else{
                                    payLoad = len%4096 ;
                                }
                            }
                            temp = new byte[payLoad];
                            System.arraycopy(data, position ,temp, 0 , payLoad);
                            int ret = mConnection.bulkTransfer(mCDCEndpointOUT, temp, temp.length, 1000);
                            position+=payLoad;
                            LOG(" send Message \t"+" data.length = "+ data.length+" ret = "+ ret+" position = "+position+" i = "+i) ;
                        }
                        LOG("send done"+(System.currentTimeMillis() - time)+" position = "+position);
                    }
                    continue;
                } catch (InterruptedException e) {
                    LOG(e.getMessage()+"");
                } catch (NullPointerException e) {
                    LOG(e.getMessage()+"");
                } catch (Exception e){
                    LOG(e.getMessage()+"");
                }
                break;
            }
        }

        public boolean sendMessage(UsbMessage BluetoothRequest) {
            return outputMessageQueue.offer(BluetoothRequest);
        }
    }

    public boolean isConnected() {
        return mDevice != null && mCDCEndpointIN != null && mCDCEndpointOUT != null;
    }

    public byte getPinState() {
        byte index = 0;
        byte[] buffer = new byte[1];
        mConnection.controlTransfer(192, 12, 0, index, buffer, 1, 0);
        return buffer[0];
    }

    public String getSerialNumber() {
        return mConnection == null?"":mConnection.getSerial();
    }

    private static final int SET_CONTROL_REQUEST = 34;
    private static final int SET_CONTROL_REQUEST_TYPE = 33;
    private boolean setCdcBaudrate(int baudrate) {
        byte[] baudByte = new byte[]{(byte)(baudrate & 255), (byte)((baudrate & '\uff00') >> 8), (byte)((baudrate & 16711680) >> 16), (byte)((baudrate & -16777216) >> 24)};
        int ret = mConnection.controlTransfer(33, 32, 0, 0, new byte[]{baudByte[0], baudByte[1], baudByte[2], baudByte[3], 0, 0, 8}, 7, 0);
        LOG("setCdcBaudrate ret = "+ret);
        ret =mConnection.controlTransfer(SET_CONTROL_REQUEST_TYPE, SET_CONTROL_REQUEST, 3, 0, null, 0, 100);
        LOG( "controlTransfer ret = "+ret);
        if(ret < 0){
            return  false ;
        }
        return  true;
    }

    private void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }

}
