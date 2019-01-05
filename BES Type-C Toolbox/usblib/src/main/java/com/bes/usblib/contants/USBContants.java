package com.bes.usblib.contants;

/**
 * Created by alloxuweibin on 2017/9/30.
 */

public class USBContants {

    //USB 硬件参数 begin
    /**
     * CDC 设备 vid
     */
    public static final int BES_CDC_VENDER_ID = 48727;
    /**
     * CDC 设备 pid
     */
    public static final int BES_CDC_PRODUCT_ID = 257;
    /**
     * HUAWEI USB-C HEADSET  vid
     */
    public static final int BES_AUDIO_VENDER_ID  = 0x12d1 ;
    /**
     * HUAWEI USB-C HEADSET pid
     */
    public static final int BES_AUDIO_PRODUCT_ID  = 0x3a07 ;
    /**
     * CDC设备下的波特率
     */
    public static final int BAUD_19200 = 921600;//19200;

    public static final int USB_TYPE_CDC = 2 ;
    public static final int USB_TYPE_AUDIO = 0 ;

    //USB 状态回调 end
    public static final int MSG_OVERHEAD = 4 ; //<PREFIX> <TYPE> <SEQ> <LEN>   <--  <DATA> <CHKSUM> <EXTDATA>
    public static final int PUBLIC_HEAD = 0xBE ;

    //CDC 通讯协议类型
    public enum CmdType{
        HAND_SHAKE_CMD((byte)0x50),
        PROGRAMMER_INFO_CMD((byte)0x53),
        PROGRAMMER_BIN_CMD((byte)0x54),
        PROGRAMMER_RUN((byte)0x55),
        SECTOR_RESPONSE((byte)0x60),
        BURN_INFO_CMD((byte)0x061),
        BURN_BIN_CMD((byte)0x62),
        WRITE_OR_ERASE_CMD((byte)0X65),
        READ_CMD((byte)0x01),
        SYS_CMD((byte)0x00),
        UNKNOWN((byte)0xff);

        private byte value;

        CmdType(byte value)
        {
            this.value = value;
        }

        public byte getByte()
        {
            return this.value;
        }

        public static CmdType parseByte(byte data)
        {
            for(CmdType type : values())
            {
                if (type.value == data) {
                    return type;
                }
            }

            return UNKNOWN;
        }
     }


}
