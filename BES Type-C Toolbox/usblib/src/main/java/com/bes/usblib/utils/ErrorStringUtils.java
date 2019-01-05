package com.bes.usblib.utils;


import com.bes.usblib.manager.BESImpl;

public class ErrorStringUtils {

   /**
    * public static final int DOWNLAOD_DONE = 0 ;  //下载成功
    public static final int DOWNLOAD_FILE_NOT_FOUND = 1 ; //文件未找到
    public static final int DOWNLOAD_NO_DEVICE_CONNECTED = 2 ; //没有设备连接
    public static final int DOWNLOAD_HANDSHAKE_FAILED = 4 ; //烧录握手失败
    public static final int DOWNLOAD_PROGRAMMER_INFO_FAILED = 5 ; //烧录 programmer 信息失败
    public static final int DOWNLOAD_RUN_PROGRAMMER_FAILED = 6 ; //执行 programmer 失败
    public static final int DOWNLOAD_BURN_INFO_FAILED = 7 ; //烧录 firmware 信息失败
    public static final int DOWNLOAD_OTHER_ERROR = 8 ; //其他原因导致的失败
    public static final int DOWNLOAD_NO_IN_CDC_MODE = 9 ;
    public static final int DOWNLOAD_READ_FILE_VERSION_FIALED = 10 ; //读取升级固件地址，
    private static final int DOWNLOAD_READY = 11 ;
    private static final int DOWNLAOD_NO_BULK_ENDPOINT = 12 ;
    public static final int DOWNLAOD_FAILED = 13 ;  //下载失败
    public static final int DOWNLOAD_BUSSING = 14 ;
     * @param msgCode
     * @return
     */
    public static String getDownloadStatusString(int msgCode){
        if(BESImpl.DOWNLOAD_NO_DEVICE_CONNECTED == msgCode){
            return "设备未连接";
        }else if(BESImpl.DOWNLOAD_FILE_NOT_FOUND == msgCode){
            return "未找到对应文件，请重新选择待升级文件";
        }else if(BESImpl.DOWNLOAD_READ_FILE_VERSION_FIALED == msgCode){
            return "升级文件解析出错，请联系开发人员";
        }else if(msgCode == BESImpl.DOWNLOAD_HANDSHAKE_FAILED){
            return "设备握手失败";
        }else if(msgCode == BESImpl.DOWNLOAD_PROGRAMMER_INFO_FAILED){
            return "Programmer 文件信息校验失败";
        }else if(msgCode == BESImpl.DOWNLOAD_RUN_PROGRAMMER_FAILED){
            return "RunRaw 失败";
        }else if(msgCode == BESImpl.DOWNLOAD_BURN_INFO_FAILED){
            return "Burn 文件信息校验失败";
        }else if(msgCode == BESImpl.DOWNLOAD_OTHER_ERROR){
            return "其他原因导致失败";
        }else if(msgCode == BESImpl.DOWNLOAD_NO_IN_CDC_MODE){
            return "设备不是cdc";
        }else if(msgCode == BESImpl.DOWNLAOD_NO_BULK_ENDPOINT){
            return "USB 没有 bulk endpoint";
        }else if(msgCode == BESImpl.DOWNLOAD_BUSSING){
            return "USB 正处于ota状态";
        }else if(msgCode == BESImpl.DOWNLOAD_BUSSING){
            return "其他原因导致失败";
        }else if(msgCode == BESImpl.DOWNLAOD_DONE){
            return "升级成功";
        }
        return "未知错误";
    }



    /**
     *  public static int DETECT_DIFF_VID = 1 ;
     public static int DETECT_DIFF_PID = 2 ;
     public static int DETECT_DIFF_MID = 3 ;
     public static int DETECT_DIFF_VSI = 4 ;
     public static int DETECT_SAME_BOOT = 5 ;
     public static int DETECT_LOW_VERSION = 6 ;
     public static int DETECT_SAME_VERSION = 7;
     public static int DETECT_PASS = 0;
     public static int DETECT_VERSION_FAILED = 8 ;
     public static int UNKNOW_FAILED = 9 ;
     */
    public static String getDetectFailedString(int errorCode){
        if(VersionCompareUtils.DETECT_DIFF_VID == errorCode){
            return "设备 Vid 不一致";
        }else if(VersionCompareUtils.DETECT_DIFF_PID == errorCode){
            return "设备 Pid 不一致";
        }else if(VersionCompareUtils.DETECT_DIFF_MID == errorCode){
            return "设备 Mid 不一致";
        }else if(VersionCompareUtils.DETECT_DIFF_VSI == errorCode){
            return "设备 VSI 不一致";
        }else if(VersionCompareUtils.DETECT_SAME_BOOT == errorCode){
            return "设备 boot 类型一致";
        }else if(VersionCompareUtils.DETECT_LOW_VERSION == errorCode){
            return "升级固件必须比设备固件版本高";
        }else if(VersionCompareUtils.DETECT_SAME_VERSION == errorCode){
            return "升级固件必须比设备固件版本高";
        }else if(VersionCompareUtils.DETECT_VERSION_FAILED == errorCode){
            return "读取版本错误";
        }else if(VersionCompareUtils.UNKNOW_FAILED == errorCode){
            return "其他未知错误";
        }
        return "其他未知错误";
    }

}
