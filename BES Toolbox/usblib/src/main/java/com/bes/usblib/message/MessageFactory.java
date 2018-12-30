package com.bes.usblib.message;


import com.bes.usblib.contants.USBContants;
import com.bes.usblib.utils.ArrayUtil;
import com.bes.usblib.utils.FileUtils;
import com.bes.usblib.utils.LogUtils;

import java.nio.ByteBuffer;

/**
 * Created by alloxuweibin on 2017/9/30.
 */

public class MessageFactory {

    static String TAG = "MessageFactory";

    /**
     * 握手协议
     * @param req
     * @return
     */
    public static UsbMessage getHandShakeCmdResponse(byte req){
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.HAND_SHAKE_CMD.getByte() , req , new byte[]{0x01});
        return  usbMessage;
    }

    /**
     * 整套升级过程中有两处需要进行擦除动作。
     * 第一处：boot标志位备份时候，需要将 备份boot标志位地址到值擦除
     * 第二处：在升级文件首地址固定内容写入固定值之后，需要擦除 boot 标志位
     * 我们定义
     * 0x6801E000为 boot 标志位首地址
     * 0x6801F000为 备份 boot 标志位首地址
     * @return
     */
    public static UsbMessage getEraseBootFlagMsg( byte req){
        byte COMM_BURN_ERASE_CMD = 0x21 ;
        int  BOOT_ADDR = 0x6801E000 ;
        byte BOOT_FLAG_LEN = 8 ;
        ByteBuffer byteBuffer = ByteBuffer.allocate(9);
        byteBuffer.put(COMM_BURN_ERASE_CMD);
        byteBuffer.put((byte)BOOT_ADDR);
        byteBuffer.put((byte)(BOOT_ADDR>>8));
        byteBuffer.put((byte)(BOOT_ADDR>>16));
        byteBuffer.put((byte)(BOOT_ADDR>>24));
        byteBuffer.put(BOOT_FLAG_LEN);
        byteBuffer.put((byte)(BOOT_FLAG_LEN>>8));
        byteBuffer.put((byte)(BOOT_FLAG_LEN>>16));
        byteBuffer.put((byte)(BOOT_FLAG_LEN>>24));
        LOG("getEraseMsg: "+ ArrayUtil.toHex(byteBuffer.array()));
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.WRITE_OR_ERASE_CMD.getByte() , req , byteBuffer.array()) ;
        return usbMessage ;
    }

    /**
     * 整套升级过程中有两处需要进行擦除动作。
     * 第一处：boot标志位备份时候，需要将 备份boot标志位地址到值擦除
     * 第二处：在升级文件首地址固定内容写入固定值之后，需要擦除 boot 标志位
     * 我们定义
     * 0x6801E000为 boot 标志位首地址
     * 0x6801F000为 备份 boot 标志位首地址
     * @return
     */
    public static UsbMessage getEraseBatBootFlagMsg(byte req){
        byte COMM_BURN_ERASE_CMD = 0x21 ;
        int  BOOT_ADDR = 0x6801F000 ;
        byte BOOT_FLAG_LEN = 8 ;
        ByteBuffer byteBuffer = ByteBuffer.allocate(9);
        byteBuffer.put(COMM_BURN_ERASE_CMD);
        byteBuffer.put((byte)BOOT_ADDR);
        byteBuffer.put((byte)(BOOT_ADDR>>8));
        byteBuffer.put((byte)(BOOT_ADDR>>16));
        byteBuffer.put((byte)(BOOT_ADDR>>24));
        byteBuffer.put(BOOT_FLAG_LEN);
        byteBuffer.put((byte)(BOOT_FLAG_LEN>>8));
        byteBuffer.put((byte)(BOOT_FLAG_LEN>>16));
        byteBuffer.put((byte)(BOOT_FLAG_LEN>>24));
        LOG("getEraseMsg: "+ ArrayUtil.toHex(byteBuffer.array()));
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.WRITE_OR_ERASE_CMD.getByte() , req , byteBuffer.array()) ;
        return usbMessage ;
    }

    /**
     * 写入标志位，此部分操作无论a或者b wort boot 均写入统一个地址。
     * 我们定义
     * 0x6801E000为 boot 标志位首地址
     * 0x6801F000为 备份 boot 标志位首地址
     * @param isAboot
     * @return
     */
    public static UsbMessage getWriteBootFlagMsg(boolean  isAboot , byte req){
        byte WRITE_CMD = 0x22 ;
        int  BOOT_ADDR = 0x6801E000 ;
        byte[] A_BOOT_DATA_FLAG = new byte[]{0x41 ,0x41,0x41,0x41,0x41,0x41,0x41,0x41 };
        byte[] B_BOOT_DATA_FLAG = new byte[]{0x42 ,0x42,0x42,0x42,0x42,0x42,0x42,0x42 };
        byte[] dataFlag = B_BOOT_DATA_FLAG ;
        if(isAboot){
            dataFlag = A_BOOT_DATA_FLAG ;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(13);
        byteBuffer.put(WRITE_CMD);
        byteBuffer.put((byte)BOOT_ADDR);
        byteBuffer.put((byte)(BOOT_ADDR>>8));
        byteBuffer.put((byte)(BOOT_ADDR>>16));
        byteBuffer.put((byte)(BOOT_ADDR>>24));
        byteBuffer.put(dataFlag);
        LOG("writeSectorFlagMsg: "+ ArrayUtil.toHex(byteBuffer.array()));
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.WRITE_OR_ERASE_CMD.getByte() , req , byteBuffer.array()) ;

        return  usbMessage;
    }

    /**
     * 拷贝boot flag值到备份 boot 首地址
     * @param bootFlagDate
     * @param req
     * @return
     */
    public static UsbMessage getWriteBatBootFlagMsg(byte[] bootFlagDate , byte req){
        if(bootFlagDate == null || bootFlagDate.length != 8){
            return  null ;
        }
        byte WRITE_CMD = 0x22 ;
        int  BOOT_ADDR = 0x6801F000 ;
        ByteBuffer byteBuffer = ByteBuffer.allocate(13);
        byteBuffer.put(WRITE_CMD);
        byteBuffer.put((byte)BOOT_ADDR);
        byteBuffer.put((byte)(BOOT_ADDR>>8));
        byteBuffer.put((byte)(BOOT_ADDR>>16));
        byteBuffer.put((byte)(BOOT_ADDR>>24));
        byteBuffer.put(bootFlagDate);
        LOG("writeSectorFlagMsg: "+ ArrayUtil.toHex(byteBuffer.array()));
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.WRITE_OR_ERASE_CMD.getByte() , req , byteBuffer.array()) ;

        return  usbMessage;
    }


    /**
     * 读取标志位地址已写入到boot标志位值，用于升级前拷贝boot标志位到备份boot标志位地址
     * 我们定义
     * 0x6801E000为 boot 标志位首地址
     * 0x6801F000为 备份 boot 标志位首地址
     * @return
     */
    public static UsbMessage getReadBootFlagMsg(){
        int  BOOT_ADDR = 0x6801E000 ;
        byte A_OR_B_FLAG_LEN = 0X08 ;
        ByteBuffer byteBuffer = ByteBuffer.allocate(5);
        byteBuffer.put((byte)BOOT_ADDR);
        byteBuffer.put((byte)(BOOT_ADDR>>8));
        byteBuffer.put((byte)(BOOT_ADDR>>16));
        byteBuffer.put((byte)(BOOT_ADDR>>24));
        byteBuffer.put(A_OR_B_FLAG_LEN);
        LOG("readSectorFlagMsg: "+ ArrayUtil.toHex(byteBuffer.array()));
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.READ_CMD.getByte() , (byte)0x06 , byteBuffer.array()) ;
        return usbMessage ;
    }

    public static UsbMessage getReadMsdByAddr(byte[] addr ,byte len){
        ByteBuffer byteBuffer = ByteBuffer.allocate(5);
        byteBuffer.put(addr);
        byteBuffer.put(len);
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.READ_CMD.getByte() , (byte)0x07 , byteBuffer.array()) ;
        return usbMessage ;
    }

    /**
     * 升级文件发送完成之后，需要在升级文件首地址位置写入固定的值 0xBE57EC1C
     * @return
     */
    public static UsbMessage getWritBrunDataToBurnAddress(byte[] burnAddr , byte req){
        byte COMM_BURN_CMD = 0x22 ;
        ByteBuffer byteBuffer = ByteBuffer.allocate(9);
        byteBuffer.put(COMM_BURN_CMD);
        byteBuffer.put(burnAddr);
        int imagicNumber = 0xBE57EC1c ;
        byteBuffer.put((byte)imagicNumber);
        byteBuffer.put((byte)(imagicNumber>>8));
        byteBuffer.put((byte)(imagicNumber>>16));
        byteBuffer.put((byte)(imagicNumber>>24));
        LOG("getWritBrunDataToBurnAddress: "+ ArrayUtil.toHex(byteBuffer.array()));
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.WRITE_OR_ERASE_CMD.getByte() , req , byteBuffer.array()) ;
        return usbMessage ;
    }

    /**
     * 设置 boot mode
     * @param req
     * @return
     */
    public static UsbMessage getSetModeMsg(byte req){
        byte SET_BOOT_MODE = (byte)0xE1 ;
        int  CPARAM = 0x800000 ;
        ByteBuffer byteBuffer = ByteBuffer.allocate(5);
        byteBuffer.put(SET_BOOT_MODE);
        byteBuffer.put((byte)CPARAM);
        byteBuffer.put((byte)(CPARAM>>8));
        byteBuffer.put((byte)(CPARAM>>16));
        byteBuffer.put((byte)(CPARAM>>24));
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.SYS_CMD.getByte() , req , byteBuffer.array());
        return  usbMessage;
    }

    /**
     * 重启
     * @param req
     * @return
     */
    public static UsbMessage getSystemReBootMsg(byte req){
        byte RE_BOOT = (byte)0xF1 ;
        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        byteBuffer.put(RE_BOOT);
        UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.SYS_CMD.getByte() , req , byteBuffer.array());
        return  usbMessage;
    }

    private static void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }
}
