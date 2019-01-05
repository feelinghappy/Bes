package com.bes.usblib.utils;

import android.content.Context;

import com.bes.usblib.contants.USBContants;
import com.bes.usblib.message.UsbMessage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by alloxuweibin on 2017/10/2.
 */

public class ProgrammerUtils {

    String TAG = "ProgrammerUtils";

    int AVAILABLE_START_POINT = 1052 ;
    int BOOT_ADDR_LEN = 4 ;
    byte[] mOtaDatas ;
    byte[] mBootAddr  = new byte[4];
    byte[] crc = new byte[4];
    byte[] otaLen = new byte[4];

    /**
     * 读取并初始化 programmer 升级文件
     */
    public boolean initProgrammer(Context context){
        InputStream inputStream = null;
        boolean initDone = false ;
        try {
            inputStream = context.getAssets().open("bes/programmer.bin");
            int totalSize = inputStream.available();
            int dataSize = totalSize - AVAILABLE_START_POINT - BOOT_ADDR_LEN;
            LOG("totalSize = "+totalSize+" dataSize = "+dataSize);
            mOtaDatas = new byte[dataSize];
            byte[] temp = new byte[AVAILABLE_START_POINT];
            inputStream.read(temp , 0 , AVAILABLE_START_POINT);
            inputStream.read(mOtaDatas, 0, dataSize);
            inputStream.read(mBootAddr,0 , BOOT_ADDR_LEN);
            long crc32 = new Crc32().crc32(mOtaDatas);
            crc[0] = (byte)(crc32 >> 0);
            crc[1] = (byte)(crc32 >> 8);
            crc[2] = (byte)(crc32 >> 16);
            crc[3] = (byte)(crc32 >> 24);
            otaLen[0] = (byte)(dataSize );
            otaLen[1] = (byte)(dataSize >> 8);
            otaLen[2] = (byte)(dataSize >> 16);
            otaLen[3] = (byte)(dataSize >> 24);
            initDone = true ;
            LOG("programmer size = "+ dataSize+" bootAddr = "+ArrayUtil.toHex(mBootAddr)+ " crc32 = "+crc32+ " crc hex "+ArrayUtil.toHex(crc));
        } catch (FileNotFoundException e) {
            LOG(e.getMessage()+"");
        } catch (IOException e) {
            LOG(e.getMessage()+"");
        } catch (Exception e){
            LOG(e.getMessage()+"");
        }finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                LOG(e.getMessage()+"");
            }
        }
        return  initDone ;
    }
    /**
     * int prepare_code_msg(void)
     * 发送预备指令
     */
    public UsbMessage prepareProgrammer(){
        if(otaLen != null && mBootAddr != null && crc != null){
            ByteBuffer byteBuffer = ByteBuffer.allocate(otaLen.length+mBootAddr.length+crc.length);
            byteBuffer.put(mBootAddr);
            byteBuffer.put(otaLen);
            byteBuffer.put(crc);
            UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.PROGRAMMER_INFO_CMD.getByte() , (byte)0x01  , byteBuffer.array());
            return usbMessage;
        }
        return  null ;
    }

    /**
     * 发送 programmer 升级包
     */
    public UsbMessage sendProgrammerBin(){
        if(mOtaDatas != null){
            byte[] contentData = new byte[]{0 , 0 , 0};
            UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.PROGRAMMER_BIN_CMD.getByte() , (byte)0xa2  , contentData);
            usbMessage.setExtData(mOtaDatas);
            return usbMessage;
        }
        return  null ;
    }

    public UsbMessage sendRunProgrammer(){
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.PROGRAMMER_RUN.getByte() , (byte)0x03 , null);
        return usbMessage;
    }


    private void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }



}
