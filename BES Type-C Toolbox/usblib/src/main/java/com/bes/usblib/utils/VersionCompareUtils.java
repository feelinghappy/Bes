package com.bes.usblib.utils;

/**
 *[version]_[vendor-id][project-id][manufacture-id]_[vendor-specific-info]
 	[version]：长度不固定，通常不会超过5字节；数字字符串，如”0.12”、”1.1”等；
 	[vendor-id]：2字节；BES对不同耳机厂商的编号；
 	[project-id]：2字节；BES对同一耳机厂家不同项目的编号；
 	[manufacturer-id]：2字节；BES对同一耳机厂家不同代工厂的编号；
 	[vendor-specific-info]：长度不固定，具体格式根据具体产品定义；
 *  “0.18_010102_aa”
 * Created by alloxuweibin on 2017/10/9.
 */

public class VersionCompareUtils {

    String TAG = "VersionCompareUtils";

    public static int DETECT_PASS = 0;
    public static int DETECT_DIFF_VID = 1 ;
    public static int DETECT_DIFF_PID = 2 ;
    public static int DETECT_DIFF_MID = 3 ;
    public static int DETECT_DIFF_VSI = 4 ;
    public static int DETECT_SAME_BOOT = 5 ;
    public static int DETECT_LOW_VERSION = 6 ;
    public static int DETECT_SAME_VERSION = 7;
    public static int DETECT_VERSION_FAILED = 8 ;
    public static int UNKNOW_FAILED = 9 ;

    String mLeftVid  ;
    String mLeftPid  ;
    String mLeftMid  ;
    String mLeftVsi  ;
    String mLeftBootType ;
    String mLeftVersion  ;

    String mRightVid  ;
    String mRightPid  ;
    String mRightMid  ;
    String mRightVsi  ;
    String mRightBootType ;
    String mRightVersion  ;

    public int checkVersion(String locationVersion , String updateVersion){
        initLeftVersion(locationVersion);
        initRightVersion(updateVersion);
        if(!isLeftVersionValid() || !isRightVersionValid()){
            return DETECT_VERSION_FAILED ;
        }
        if(mLeftBootType.equals(mRightBootType)){
            return DETECT_SAME_BOOT;
        }
        if(!mLeftVid.equals(mRightVid)){
            return DETECT_DIFF_VID;
        }
        if(!mLeftPid.equals(mRightPid)){
            return DETECT_DIFF_PID;
        }
        boolean isCompatibilityAll = "xx".equals(mLeftMid.toLowerCase())||"xx".equals(mRightMid.toLowerCase());
        if(!mLeftMid.equals(mRightMid) && !isCompatibilityAll){
            return DETECT_DIFF_MID;
        }
        if(!mLeftVsi.equals(mRightVsi)){
            return DETECT_DIFF_VSI;
        }if(mLeftVersion.equals(mRightVersion)){
            return DETECT_SAME_VERSION;
        }
        try{
            int compareVersionNumber = compareVersion(locationVersion , updateVersion);
            if(compareVersionNumber < 0){
                return DETECT_PASS;
            }else{
                return DETECT_LOW_VERSION ;
            }
        }catch (Exception e){
            return UNKNOW_FAILED;
        }
    }

    boolean isLeftVersionValid(){
        if(mLeftBootType == null || mLeftMid == null || mLeftPid == null || mLeftVersion == null || mLeftVid == null || mLeftVsi == null){
            return  false ;
        }
        return  true ;
    }

    boolean isRightVersionValid(){
        if(mRightVsi == null || mRightBootType == null || mRightMid == null || mRightPid == null || mRightVersion == null || mRightVid == null){
            return  false ;
        }
        return  true ;
    }

    private void initLeftVersion(String versionString){
        if(versionString != null){
            String[] versionSplit = versionString.split("\\_");
            if(versionSplit != null && versionSplit.length == 3 ){
                if(versionSplit[1] != null && versionSplit[1].length() == 6){
                    if(versionSplit[2] != null && versionSplit[2].length() == 2){
                        mLeftVersion = versionSplit[0];
                        mLeftVid = versionSplit[1].substring(0 , 2);
                        mLeftPid = versionSplit[1].substring(2 ,4);
                        mLeftMid = versionSplit[1].substring(4 , 6);
                        mLeftVsi = versionSplit[2].substring(0 , 1);
                        mLeftBootType = versionSplit[2].substring(1 , 2);
                        LOG("mLeftVersion = "+ mLeftVersion+" mLeftVid = "+ mLeftVid+" mLeftPid  = "+ mLeftPid+" mLeftMid = "+ mLeftMid
                        +" mLeftVsi = "+ mLeftVsi + " mLeftBootType = "+ mLeftBootType );
                    }
                }
            }
        }
    }


    private void initRightVersion(String versionString){
        if(versionString != null){
            String[] versionSplit = versionString.split("\\_");
            if(versionSplit != null && versionSplit.length == 3 ){
                if(versionSplit[1] != null && versionSplit[1].length() == 6){
                    if(versionSplit[2] != null && versionSplit[2].length() == 2){
                        mRightVersion = versionSplit[0];
                        mRightVid = versionSplit[1].substring(0 , 2);
                        mRightPid = versionSplit[1].substring(2 ,4);
                        mRightMid = versionSplit[1].substring(4 , 6);
                        mRightVsi = versionSplit[2].substring(0 , 1);
                        mRightBootType = versionSplit[2].substring(1 , 2);
                        LOG("mRightVersion = "+ mRightVersion+" mRightVid = "+ mRightVid+" mRightPid  = "+ mRightPid+" mRightMid = "+ mRightMid
                                +" mRightVsi = "+ mRightVsi + " mRightBootType = "+ mRightBootType );
                    }
                }
            }
        }
    }

    /**
     * 比较版本号的大小,前者大则返回一个正数,后者大返回一个负数,相等则返回0
     * @param version1
     * @param version2
     * @return
     */
    private  int compareVersion(String version1, String version2) throws Exception {
        if (version1 == null || version2 == null) {
            throw new Exception("compareVersion error:illegal params.");
        }
        String[] versionArray1 = version1.split("\\.");//注意此处为正则匹配，不能用"."；
        String[] versionArray2 = version2.split("\\.");
        int idx = 0;
        int minLength = Math.min(versionArray1.length, versionArray2.length);//取最小长度值
        int diff = 0;
        while (idx < minLength
                && (diff = versionArray1[idx].length() - versionArray2[idx].length()) == 0//先比较长度
                && (diff = versionArray1[idx].compareTo(versionArray2[idx])) == 0) {//再比较字符
            ++idx;
        }
        //如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大；
        diff = (diff != 0) ? diff : versionArray1.length - versionArray2.length;
        return diff;
    }

    private void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }


}
