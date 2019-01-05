package com.bes.usblib.driver;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
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
 * CDC模式 数据接口
 */
public class BesCdcDriver {
    private static final String TAG = "BesCdcDriver";

    public static final int INIT_DONE = 0 ;
    public static final int INIT_FAILED_USBCONNECTION_NULL = 1 ; //USB 未连接
    public static final int INIT_FAILED_USBDDEVICE_NULL = 2 ; // USB 设备未空
    public static final int INIT_FAILED_BULK_ENDPOINT_WRONG = 3 ; // 找不到usb数据端点
    public static final int INIT_OTHER_ERROR = 4 ;

    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbDevice mUsbDevice ;
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


    private XwInputThread mUSBInputThread;
    private XwOutputThread mUSBOutputThread;
    private int maxPacketSize = 64 ;

    IUsbCallback usbCallback ;


    public BesCdcDriver(UsbDeviceConnection usbDeviceConnection, UsbDevice usbDevice , IUsbCallback usbCallback) {
        this.mUsbDeviceConnection = usbDeviceConnection ;
        this.mUsbDevice = usbDevice ;
        this.usbCallback = usbCallback ;
    }

    /**
     * 释放接口资源， 但不关闭 UsbDeviceConnection 逻辑由外部关闭
     */
    public void release() {
        LOG("BesCdcDriver release()") ;
        stopThread();
        if(mUsbDeviceConnection != null) {
            if(mCDCInterface != null) {
                mUsbDeviceConnection.releaseInterface(mCDCInterface);
                mCDCInterface = null;
            }
        }
    }

    /**
     * 发送usb消息数据,数据长度问题由外部控制，此处不做处理
     * @param msg
     * @return
     */
    public boolean sendMeesage(UsbMessage msg) {
        if(mUSBOutputThread != null){
            mUSBOutputThread.sendMessage(msg);
            return  true ;
        }
        return false;
    }

    /**
     * 已授权情况下发起连接
     * @return
     */
    public int init() {
        if(mUsbDeviceConnection == null){
            return INIT_FAILED_USBCONNECTION_NULL ;
        }
        if(mUsbDevice == null){
            return INIT_FAILED_USBDDEVICE_NULL ;
        }
        for(int i = 0 ; i < mUsbDevice.getInterfaceCount() ; i++){
            final UsbInterface usbInterface = mUsbDevice.getInterface(i);
            if (mUsbDeviceConnection.claimInterface(usbInterface, true)) {
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
            }else{
                LOG("the number interface i = "+i+" class = "+ usbInterface.getInterfaceClass()) ;
            }
        }
        if(mCDCInterface != null && mCDCEndpointIN != null & mCDCEndpointOUT != null){
            int initRet = mUsbDeviceConnection.controlTransfer(33, 34, 0, 0, (byte[])null, 0, 0);
            LOG("initCdcAcm controlTransfer ret "+ initRet);
            boolean ret =  setCdcBaudrate(USBContants.BAUD_19200);
            if(ret){
                startThread();
                return INIT_DONE ;
            }else{
                return INIT_OTHER_ERROR ;
            }
        }else{
            return INIT_FAILED_BULK_ENDPOINT_WRONG ;
        }
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
            byte[] buffer = new byte[4*1024];
            while (true) {
                if (shouldDie || Thread.currentThread().isInterrupted()) {
                    break;
                }
                int ret = mUsbDeviceConnection.bulkTransfer(mCDCEndpointIN, buffer, buffer.length, 500);
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
                            int ret = mUsbDeviceConnection.bulkTransfer(mCDCEndpointOUT, temp, temp.length, 1000);
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

    private static final int SET_CONTROL_REQUEST = 34;
    private static final int SET_CONTROL_REQUEST_TYPE = 33;
    private boolean setCdcBaudrate(int baudrate) {
        byte[] baudByte = new byte[]{(byte)(baudrate & 255), (byte)((baudrate & '\uff00') >> 8), (byte)((baudrate & 16711680) >> 16), (byte)((baudrate & -16777216) >> 24)};
        int ret = mUsbDeviceConnection.controlTransfer(33, 32, 0, 0, new byte[]{baudByte[0], baudByte[1], baudByte[2], baudByte[3], 0, 0, 8}, 7, 0);
        LOG("setCdcBaudrate ret = "+ret);
        ret = mUsbDeviceConnection.controlTransfer(SET_CONTROL_REQUEST_TYPE, SET_CONTROL_REQUEST, 3, 0, null, 0, 100);
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
