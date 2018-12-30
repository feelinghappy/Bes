package com.iir_eq.util;

import android.util.Log;

import java.util.concurrent.RecursiveTask;

/**
 * Created by alloxuweibin on 2017/9/12.
 */

public class ProfileUtils {

    private static final int SPP_SINGLE_CRC_BYTE_COUNTS = 1024*4 ;
    private static final int SPP_SINGLE_PACKE_BYTES = 256 ;
    /**
     * SPP 固化每包的大小为 256
     * @return
     */
    public static int calculateSppSinglePacketLen(int imageSize){
        int len = SPP_SINGLE_PACKE_BYTES ;
        if(imageSize < len){
            len =  imageSize ;
        }
        Log.e("BES","calculateSppSinglePacketLen = "+ len);
        return  len ;
    }

    /**
     * 镜像文件总包数
     * @param imageSize
     * @return
     */
    public static int calculateSppTotalPacketCount(int imageSize){
        return (imageSize + SPP_SINGLE_PACKE_BYTES - 1)/SPP_SINGLE_PACKE_BYTES ;
    }

    /**
     * SPP 总校验次数
     * @param imageSize 升级包总大小
     * @return
     */
    public static int calculateSppTotalCrcCount(int imageSize){
        return (imageSize + SPP_SINGLE_CRC_BYTE_COUNTS - 1)/SPP_SINGLE_CRC_BYTE_COUNTS;
    }


    /**
     * BLE 限制最大单包字节数不超过 256
     */
    private static final int BLE_SINGLE_PACKE_MAC_BYTES = 256 ;


    /**
     * @return
     */
    public static int calculateBLESinglePacketLen(int imageSize , int mtu , boolean isBle){
        if(imageSize != 0 && imageSize < mtu - 1)
        {
            return imageSize;
        }

        if (!isBle)
        {
            return ((mtu - 1) > 512)?512:(mtu - 1);
        }
        else {
			// mtu shouldn't bigger than (512-3)
            return (mtu > 509)?508:(mtu - 1);
        }
    }

    /**
     * ble 镜像文件总包数
     * @param imageSize
     * @return
     */
    public static int calculateBLETotalPacketCount(int imageSize, int mtu, boolean isble){
        int totalCount = (imageSize + calculateBLESinglePacketLen(imageSize , mtu, isble) - 1)/calculateBLESinglePacketLen(imageSize , mtu,isble) ;
        Log.e("BES","imageSize = "+ imageSize + " mtu = "+mtu + " totalCount = "+ totalCount);
        return  totalCount;
    }

    /**
     * ble 计算百分之一的数据量，并按256倍数就近补齐
     * @param imageSize 升级包总大小
     * @return
     */
    public static int calculateBLEOnePercentBytes(int imageSize){
        int onePercentBytes = imageSize/100 ;
        if(imageSize < 256){
            onePercentBytes = imageSize ;
        }else{
            int rightBytes = 0 ;
            if(onePercentBytes < 256){
                rightBytes = 256 - onePercentBytes;
            }else{
                rightBytes = 256 - onePercentBytes%256 ;
            }
            if(rightBytes != 0){
                onePercentBytes = onePercentBytes + rightBytes ;
            }
        }

        if (onePercentBytes > 4*1024) {
            onePercentBytes = 4*1024;
        }
        int tempCount = (imageSize+onePercentBytes-1)/onePercentBytes;
        Log.e("BES","imageSize = "+imageSize+" onepercentBytes = "+onePercentBytes+" crc total Count "+ tempCount);
        return onePercentBytes;
    }


}
