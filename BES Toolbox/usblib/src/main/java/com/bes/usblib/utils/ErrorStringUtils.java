package com.bes.usblib.utils;


import android.widget.RelativeLayout;

import com.bes.usblib.manager.BESImpl;

/**
 * Created by alloxuweibin on 2017/10/7.
 */

public class ErrorStringUtils {


    /**
     public static final int DOWNLAOD_READY = 0 ;
     public static final int DOWNLOAD_FILE_NOT_FOUND = -1 ; V
     public static final int DOWNLOAD_VERSION_IS_SAME = - 2; V
     public static final int DOWNLOAD_NO_DEVICE_CONNECTED = -3 ; V
     public static final int DOWNLOAD_IN_CDC_DEVICE = - 5; V
     public static final int DOWNLOAD_ONLY_SUPPORT_AUTO_OTA = -6 ; V
     public static final int DOWNLOAD_READ_FILE_VERSION_FIALED  = -7 ; V
     public static final int DOWNLOAD_HANDSHAKE_FAILED = -8 ; V
     public static final int DOWNLOAD_PROGRAMMER_INFO_FAILED = -9 ;
     public static final int DOWNLOAD_RUN_PROGRAMMER_FAILED = -10 ;
     public static final int DOWNLOAD_BURN_INFO_FAILED = -11 ;
     public static final int DOWNLOAD_OTHER_ERROR = -12 ;
     public static final int DOWNLOAD_NO_PERMISSION = -13 ;
     * @param msgCode
     * @return
     */
    public static String getDownloadStatusString(int msgCode , String msgString){
        if(BESImpl.DOWNLOAD_NO_DEVICE_CONNECTED == msgCode){
            return "设备未连接";
        }else if(BESImpl.DOWNLOAD_IN_CDC_DEVICE == msgCode){
            return "自动升级方式需获取版本信息，不支持CDC连接";
        }else if(BESImpl.DOWNLOAD_ONLY_SUPPORT_AUTO_OTA == msgCode){
            return "仅支持手动升级，请确保设备插入USB时可以进入CDC模式";
        }else if(BESImpl.DOWNLOAD_FILE_NOT_FOUND == msgCode){
            return "未找到对应文件，请重新选择待升级文件";
        }else if(BESImpl.DOWNLOAD_VERSION_IS_SAME == msgCode){
            return "当前版本与升级文件版本一致，不需要升级";
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
            return "其他原因导致失败"+ msgString;
        }else if(msgCode == BESImpl.DOWNLOAD_NO_PERMISSION){
            return "未获取USB系统权限";
        }else if(msgCode == BESImpl.OTA_DONE){
            return "升级成功";
        }else if(msgCode == BESImpl.DOWNLOAD_WRONG_BOOT_TYPE_FILE){
            return "当前升级文件boot类型于设备一致，需更换成对应的备份boot文件类型";
        }
        return "未知错误";
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
     * @param errorCode
     * @return
     */
    public static String getConnectStatusString(int errorCode){
        if(BESImpl.CONNECTION_CONNECTED == errorCode){
            return "设备已连接";
        }else if(BESImpl.CONNECTION_FAILED_WITH_NO_DEVICE == errorCode){
            return "没有检测到有任何USB设备插入";
        }else if(BESImpl.CONNECTION_FAILED_WITH_NO_BES_DEVICE == errorCode){
            return "不支持非BES设备连接";
        }else if(BESImpl.CONNECTION_FAILED_IN_CDC_MODE == errorCode){
            return "自动升级模式，不支持CDC设备手动连接";
        }else if(BESImpl.CONNECTION_FAILED_NO_PREMISSION == errorCode){
            return "未获得USB系统授权，请确保点击了系统权限弹框";
        }else if(BESImpl.CONNECTION_CDC_DEVICE_GOING == errorCode){
            return "正发起CDC设备连接";
        }else if(BESImpl.CONNECTION_AUDIO_DEVICE_GOING == errorCode){
            return "正发起AUDIO设备连接";
        }else if(BESImpl.CONNECTION_DISCONNECTED == errorCode){
            return "设备已断开";
        }else if(BESImpl.CONNECTION_ATTACHED == errorCode){
            return "设备已插入";
        }else if(BESImpl.CONNECTION_DETACHED == errorCode){
            return "设备已拔出";
        }else if(BESImpl.CONNECTION_IDLE == errorCode){
            return "当前设备空闲";
        }
        return "未知错误";
    }


}
