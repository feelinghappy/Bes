package com.bes.usblib.driver;

import android.hardware.usb.UsbDeviceConnection;
import com.bes.usblib.utils.ArrayUtil;
import com.bes.usblib.utils.FileUtils;
import com.bes.usblib.utils.LogUtils;

/**
 * 获取 bes  audio 产品信息
 */
public class BesAudioDriver {
    private static final String TAG = "BesAudioDriver";

    public static final int ENTER_CDC_AND_REBOOT = 0 ;
    public static final int ENTER_CDC_FAILED_USBCONNECTION_NULL = 1;
    public static final int ENTER_CDC_FAILED_ONLY_SUPPORT_HUMANE_MODE = 2;

    private UsbDeviceConnection mUsbDeviceConnection ;


    public BesAudioDriver(UsbDeviceConnection usbDeviceConnection) {
        this.mUsbDeviceConnection = usbDeviceConnection;
    }

    public int sendCdcCmd() {
        if(mUsbDeviceConnection == null){
            LOG("ENTER_CDC_FAILED_USBCONNECTION_NULL");
            return ENTER_CDC_FAILED_USBCONNECTION_NULL ;
        }
        byte[] CHECK_UPDATE_ENABLE = new byte[]{(byte)'F',(byte)'W',(byte)'_',(byte)'U',(byte)'P',(byte)'D',(byte)'A',(byte)'T',(byte)'E'};
        int ret = mUsbDeviceConnection.controlTransfer(64, 6, 0, 0, CHECK_UPDATE_ENABLE, CHECK_UPDATE_ENABLE.length, 4000);
        LOG("sendCdcCmd CHECK_UPDATE_ENABLE ret "+ ret+" data = "+ ArrayUtil.toHex(CHECK_UPDATE_ENABLE) +" ascii = "+ArrayUtil.toASCII(CHECK_UPDATE_ENABLE));
        byte[] buffer = new byte[1];
        ret = mUsbDeviceConnection.controlTransfer(192, 12, 0, 0, buffer, 1, 0);
        LOG("sendCdcCmd CHECK_UPDATE_ENABLE BACK "+ ret+" data = "+ArrayUtil.toHex(buffer) +" ascii = "+ArrayUtil.toASCII(buffer));
        if(buffer[0] == 0x31){  // ASCII eq 1
            byte[] SYS_REBOOT = new byte[]{(byte)'S',(byte)'Y',(byte)'S',(byte)'_',(byte)'R',(byte)'E',(byte)'B',(byte)'O',(byte)'O',(byte)'T'};
            ret = mUsbDeviceConnection.controlTransfer(64, 6, 0, 0, SYS_REBOOT, SYS_REBOOT.length, 4000);
            LOG("sendCdcCmd SYS_REBOOT ret "+ ret+" data = "+ArrayUtil.toHex(SYS_REBOOT) +" ascii = "+ArrayUtil.toASCII(SYS_REBOOT));
            ret = mUsbDeviceConnection.controlTransfer(192, 12, 0, 0, buffer, 1, 0);     //强制reboot
            return ENTER_CDC_AND_REBOOT;
        }else{
            LOG("ENTER_CDC_FAILED_ONLY_SUPPORT_HUMANE_MODE");
            return ENTER_CDC_FAILED_ONLY_SUPPORT_HUMANE_MODE;
        }
    }


    /**
     * 获取厂商协议版本（非软件版本）
     * @return
     */
    public String getProfileVersion(){
        final int VERSION_LEN = 3 ;
        byte[] checkVersion = new byte[]{(byte)'C',(byte)'H',(byte)'E',(byte)'C', (byte)'K'};
        int ret = mUsbDeviceConnection.controlTransfer(64, 6, 0, 0, checkVersion, checkVersion.length, 4000);
        LOG("getProfileVersion  SEND "+ ret+" data = "+ArrayUtil.toHex(checkVersion) +" ascii = "+ArrayUtil.toASCII(checkVersion));
        byte[] buffer = new byte[VERSION_LEN];
        ret = mUsbDeviceConnection.controlTransfer(192, 12, 0, 0, buffer, VERSION_LEN, 0);
        String version = ArrayUtil.toASCII(buffer) ;
        LOG("getProfileVersion  BACK "+ ret+" data = "+ArrayUtil.toHex(buffer) +" ascii = "+version);
        if(version == null || "".equals(version.trim())){
            LOG("getProfileVersion  version reset null");
            version = null ;
        }
        return  version ;
    }

    /**
     * 其中包含三部分信息：版本号、厂家标识以及厂家自定义信息。具体格式定义如下，各部分信息之间用下划线“_”连接。
     [version]_[vendor-id][project-id][manufacture-id]_[vendor-specific-info]
     	[version]：长度不固定，通常不会超过5字节；数字字符串，如”0.12”、”1.1”等；
     	[vendor-id]：2字节；BES对不同耳机厂商的编号；
     	[project-id]：2字节；BES对同一耳机厂家不同项目的编号；
     	[manufacturer-id]：2字节；BES对同一耳机厂家不同代工厂的编号；
     	[vendor-specific-info]：长度不固定，具体格式根据具体产品定义；
     目前基于BES方案的Type-C耳机信息如表3-1。
     表3-1 BES Type-C耳机信息
     VID*	PID*	MID*	UVID*	 UPID*		 	   UPN*					VSI*
     -----------------------------------------------------------------------------------
     01		01		01		0x12d1	 0x3a07·	BBIITT USB-C HEADSET*	[e/w/a][a/b]*
     02
     03
     -----------------------------------------------------------------------------------
     02		01		01		0x2717	 0x3801				em006				 /
     -----------------------------------------------------------------------------------
     *说明：
     	VID表示[vendor-id]，PID表示[product-id]，MID表示[manufacturer-id]，UVID表示USB Vendor-ID，UPID表示USB Product ID，UPN表示USB Product Name，VSI表示[vendor-specific-info]；
     	BBIITT USB-C HEADSET，注意包含空格。
     	[e/w/a][a/b]，表示2个字节的vendor-specific-info
     第一个字节为’e’、’w’或’a’，分别表示欧洲版、世界版和无区分版；
     第二个字节为’a’或’b’，对应于AB boot架构的work boot a和work boot b；
     */
    public String getFirmwareVersion(){
        final int VERSION_LEN = 14 ;
        byte[] getFWVersion = new byte[]{(byte)'Q',(byte)'U',(byte)'E',(byte)'R', (byte)'Y',(byte)'_',(byte)'S',(byte)'W',(byte)'_',(byte)'V',(byte)'E',(byte)'R'};
        int ret = mUsbDeviceConnection.controlTransfer(64, 6, 0, 0, getFWVersion, getFWVersion.length, 4000);
        LOG("getFirmwareVersion getFirmwareVersion SEND "+ ret+" data = "+ArrayUtil.toHex(getFWVersion) +" ascii = "+ArrayUtil.toASCII(getFWVersion));
        byte[] buffer = new byte[VERSION_LEN];
        ret = mUsbDeviceConnection.controlTransfer(192, 12, 0, 0, buffer, VERSION_LEN, 0);
        String version = ArrayUtil.toASCII(buffer) ;
        LOG("getFirmwareVersion getFirmwareVersion BACK "+ ret+" data = "+ArrayUtil.toHex(buffer) +" ascii = "+version);
        return version ;
    }


    public String getSerialNumber(){
        final int VERSION_LEN = 64 ;
        byte[] getFWVersion = new byte[]{(byte)'Q',(byte)'U',(byte)'E',(byte)'R',(byte)'Y',(byte)'_',(byte)'S',(byte)'N'};
        int ret = mUsbDeviceConnection.controlTransfer(64, 6, 0, 0, getFWVersion, getFWVersion.length, 4000);
        LOG("getSerialNumber getSerialNumber SEND "+ ret+" data = "+ArrayUtil.toHex(getFWVersion) +" ascii = "+ArrayUtil.toASCII(getFWVersion));
        byte[] buffer = new byte[VERSION_LEN];
        ret = mUsbDeviceConnection.controlTransfer(192, 12, 0, 0, buffer, VERSION_LEN, 0);
        String sn = ArrayUtil.toASCII(buffer) ;
        LOG("getSerialNumber getSerialNumber BACK "+ ret+" data = "+ArrayUtil.toHex(buffer) +" ascii = "+ sn);
        return  sn ;
    }

    private void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }
}
