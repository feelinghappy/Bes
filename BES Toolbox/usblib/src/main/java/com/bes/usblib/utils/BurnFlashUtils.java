package com.bes.usblib.utils;

import com.bes.usblib.contants.USBContants;
import com.bes.usblib.message.UsbMessage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by alloxuweibin on 2017/10/2.
 */

public class BurnFlashUtils {

    String TAG = "ProgrammerUtils";
    int BURN_DATA_MSG_SEQ_START = 0xC1;
    final int SECTOR_4K  = 4*1024 ;
    final int SECTOR_32K = 32*1024 ;
    int BOOT_ADDR_LEN = 4 ;
    byte[] mOtaDatas ;
    byte[][] mOtaSeq ;
    byte[] mBootAddr  = new byte[4];
    byte[] otaLen = new byte[4];
    byte[] sectorLen = new byte[4];
    int seqNumber = 0 ;
    int maxSeq  = 0 ;
    int mSectorSize = SECTOR_4K ;
    public byte[] getmBootAddr(){
        return mBootAddr ;
    }
    /**
     * 读取并初始化 programmer 升级文件
     */
    public boolean initProgrammer(String path , int SectorSize){
        if(SectorSize > SECTOR_32K){
            mSectorSize = SECTOR_32K;
        }else{
            mSectorSize = SECTOR_4K;
        }
        FileInputStream inputStream = null;
        boolean initDone = false ;
        try {
            inputStream = new FileInputStream(path);//;context.getAssets().open("evb_ulc_boot_aa_sw0.16_170926.bin");
            int totalSize = inputStream.available();
            int dataSize = totalSize -  BOOT_ADDR_LEN;
            mOtaDatas = new byte[dataSize];
            inputStream.read(mOtaDatas, 0, dataSize);
            inputStream.read(mBootAddr,0 , BOOT_ADDR_LEN);
            otaLen[0] = (byte)(dataSize );
            otaLen[1] = (byte)(dataSize >> 8);
            otaLen[2] = (byte)(dataSize >> 16);
            otaLen[3] = (byte)(dataSize >> 24);
            sectorLen[0] = (byte)(mSectorSize );
            sectorLen[1] = (byte)(mSectorSize >> 8);
            sectorLen[2] = (byte)(mSectorSize >> 16);
            sectorLen[3] = (byte)(mSectorSize >> 24);
            maxSeq = (dataSize + mSectorSize - 1)/mSectorSize ;
            int position = 0 ;
            LOG("totalSize = "+totalSize+" dataSize = "+dataSize+" maxSeq = "+ maxSeq);
            mOtaSeq = new byte[maxSeq][];
            for(int i = 0 ; i < maxSeq ; i++){
                int seqLen = mSectorSize ;
                if(i == maxSeq - 1){//last one
                    if(i == 0){
                        seqLen = dataSize ;
                    }else{
                        seqLen = dataSize%mSectorSize ;
                    }
                }
                mOtaSeq[i] = new byte[seqLen];
                System.arraycopy(mOtaDatas , position , mOtaSeq[i] , 0 , seqLen);
                position +=seqLen ;
            }
            initDone = true ;
            LOG("programmer size = "+ dataSize+" bootAddr = "+ArrayUtil.toHex(mBootAddr)+" mSectorSize = "+ mSectorSize+
                     " mSectorSize hex = "+ArrayUtil.toHex(sectorLen));
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
    public UsbMessage prepareBurn(){
        if(otaLen != null && mBootAddr != null && sectorLen != null){
            ByteBuffer byteBuffer = ByteBuffer.allocate(otaLen.length+mBootAddr.length+sectorLen.length);
            byteBuffer.put(mBootAddr);
            byteBuffer.put(otaLen);
            byteBuffer.put(sectorLen);
            UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.BURN_INFO_CMD.getByte() , (byte)0x03  , byteBuffer.array());
            return usbMessage;
        }
        return  null ;
    }

    /**
     * <SDATA_LEN> <CRC32> <SECTOR_SEQ> 0x00 <SDATA>
     * 发送 programmer 升级包
     */
    public UsbMessage sendProgrammerBinNext(){
        if(mOtaDatas != null){
            ByteBuffer byteBuffer = ByteBuffer.allocate(11);
            int dataLen = mOtaSeq[seqNumber].length ;
            byteBuffer.put((byte)dataLen);
            byteBuffer.put((byte)(dataLen >> 8));
            byteBuffer.put((byte)(dataLen >> 16));
            byteBuffer.put((byte)(dataLen >> 24));
            long crc32 = new Crc32().crc32(mOtaSeq[seqNumber]);
            byteBuffer.put((byte)crc32);
            byteBuffer.put((byte)(crc32 >> 8));
            byteBuffer.put((byte)(crc32 >> 16));
            byteBuffer.put((byte)(crc32 >> 24));
            byteBuffer.put((byte)seqNumber);
            byteBuffer.put((byte)(seqNumber >> 8));
            byteBuffer.put((byte)0x00);
            UsbMessage usbMessage = new UsbMessage(USBContants.CmdType.BURN_BIN_CMD.getByte() , (byte)((seqNumber+BURN_DATA_MSG_SEQ_START))  , byteBuffer.array());
            usbMessage.setExtData(mOtaSeq[seqNumber]);
            seqNumber++;
            return usbMessage;
        }
        return  null ;
    }

    public boolean isSeqEnd(){
        if(seqNumber == maxSeq){
            return  true ;
        }
        return false ;
    }

    public int getMaxSeq(){
        return maxSeq ;
    }

    private void LOG(String msg){
        if(msg != null){
            LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
        }
    }



}
