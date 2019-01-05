package com.bes.usblib.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.bes.usblib.callback.IUsbCallback;
import com.bes.usblib.contants.USBContants;
import com.bes.usblib.driver.BesAudioDriver;
import com.bes.usblib.driver.BesCdcDriver;
import com.bes.usblib.message.MessageFactory;
import com.bes.usblib.message.UsbMessage;
import com.bes.usblib.utils.ArrayUtil;
import com.bes.usblib.utils.BurnFlashUtils;
import com.bes.usblib.utils.FileUtils;
import com.bes.usblib.utils.LogUtils;
import com.bes.usblib.utils.ProgrammerUtils;
import com.bes.usblib.utils.VersionCompareUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class BESImpl implements IUsbCallback {

	String TAG = "BESCdcOtaImpl";

	public static final int INIT_DONE = 0 ;
	public static final int INIT_NO_BES_DEVICE = 1 ;
	public static final int INIT_USB_DEVICE_NULL = 2;
	public static final int INIT_USB_NO_CONNECTED = 3 ;
	public static final int INIT_CONTEXT_NULL = 4 ;

	//执行下载返回列表  begin
	public static final int DOWNLAOD_DONE = 0 ;  //下载成功
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
	public static final int DOWNLAOD_NO_BULK_ENDPOINT = 12 ;
	public static final int DOWNLAOD_FAILED = 13 ;  //下载失败
	public static final int DOWNLOAD_BUSSING = 14 ;
	public static final int IDLE_STATUS = 15 ;
    private int downMsgCode = IDLE_STATUS ;
	//执行下载返回码列表 end

	//由于以下擦除跟写入都用到了统一指令类型-x65,故使用固定req值进行区分 begin（升级协议内容）
	private static final byte REQ_ERASE_BAT_BOOT_FLAG = 0X10 ;
	private static final byte REQ_WRITE_BOOT_FLAG_TO_BAT_ADDR = 0X11 ;
	private static final byte REQ_WRITE_MAGIC_NUMBER = 0X12 ;
	private static final byte REQ_ERASE_BOOT_FLAG = 0X13 ;
	private static final byte REQ_WRITE_BOOT_FALG = 0X14 ;
	//由于以下擦除跟写入都用到了统一指令类型-x65,故使用固定req值进行区分 end

	//由于设置boot mode 与 reboot 用到了同一条指令 ，故使用固定req值进行区分 begin（升级协议内容）
	private static final byte REQ_SET_BOOT_MODE = 0X15 ;
	private static final byte REQ_REBOOT_SYSTEM = 0X16 ;
	private static final byte REQ_OTA_FAILED_REBOOT_SYSTEM = 0X16 ;
	//由于设置boot mode 与 reboot 用到了同一条指令 ，故使用固定req值进行区分 end

	// ota整套逻辑进度,从上往下按顺序 begin
	static final int OTA_START = 0;
	static final int OTA_FIRST_HANDSHAKE_START = 1;
	static final int OTA_FIRST_HANDSHAKE_FAILED = 2;
	static final int OTA_FIRST_HANDSHAKE_SUCCESSFUL = 3;
	static final int OTA_START_DOWNLOAD_PROGRAMMER = 4;
	static final int OTA_START_DOWNLOAD_PROGRAMMER_BIN = 5;
	static final int OTA_DOWNLOAD_PROGRAMMER_FAILED = 6;
	static final int OTA_DOWNLOAD_PROGRAMMER_SUCCESSFUL = 7;
	static final int OTA_RUN_PROGRAMMER = 8 ;
	static final int OTA_RUN_PROGRAMMER_SUCCESSFUL = 9 ;
	static final int OTA_RUN_PROGRAMMER_FAILED = 10 ;
	static final int OTA_WAIT_RAM_RUN = 11;
	static final int OTA_READ_BOOT_FLAG = 12 ;
	static final int OTA_ERASE_BAT_BOOT_FLAG = 13 ;
	static final int OTA_COPY_TO_BAT_BOOT_FLAG = 14 ;
	static final int OTA_START_DOWNLOAD_BURN = 15;
	static final int OTA_START_DOWNLOAD_BURN_NEXT = 16;
	static final int OTA_START_DOWNLOAD_BURN_FAILED = 17;
	static final int OTA_START_DOWNLOAD_BURN_SUCCESFUL = 18;
	static final int OTA_BURN_WIRTE_MAGIC_NUMBER = 19 ;
	static final int OTA_BURN_ERASE_BOOT_FLAG = 20 ;
	static final int OTA_BURN_WRITE_BOOT_FLAG = 21 ;
	static final int OTA_SET_BOOT_MODE = 22 ;
	static final int OTA_REBOOT_FLASH = 23;
	static final int OTA_DONE = 24 ;
	static final int OTA_OTHER_ERROR = 25 ; //代表OTA_BURN_WIRTE_MAGIC_NUMBER往后的所有指令交互错误
	static final int OTA_FAILED = 26 ;
	static final int OTA_IDLE = 27;
	int mOtaStatus = OTA_IDLE;
	// ota整套逻辑进度,从上往下按顺序 end

	// 用来获取assert资源
	Context mContext;
	UsbDeviceConnection mUsbDeviceConnection ;
	UsbDevice mUsbDevice ;
	//AUDIO 驱动工具，在这里主要做获取设备信息
	BesAudioDriver mBesAudioDriver;
	//cdc 设备驱动类，在这里主要做数据交互
	BesCdcDriver mBesCdcDriver ;
	//programmer 烧录工具
	ProgrammerUtils programmerUtils;
	// burn 烧录工具
	BurnFlashUtils burnFlashUtils ;
	//通讯协议版本
	String mProfileVersion ;
	//sn 号 仅在 audio设备下有效
	String mSerialNumber ;
	//固件版本号
	String mTypeCVersion ;
	//烧录目标 work boot 文件路径
	String mFirmwareFilePath ;
	//烧录进度
	int  mProcess ;
	// boot 标志位，需要现读取，再擦除备份标志位，在写入到备份标志为到过程，故需要中间值保存信息
	byte[] copyBootFlag ;
	// 烧录块大小，4k 或 32k 由设备端返回
	int mSectorSize = 0 ;
	// 待升级到升级文件boot 类型 默认为aboot，根据读取work boot  build info 值之后做正确判断
	boolean isAboot = true ;//
	//匹配版本编码返回码，VersionCompareUtils 的匹配结果常量
	int detectNewFirmwareCode = 0 ;

	private final Object mLock = new Object();

	public BESImpl() {
		LOG("BES lib V1.3");
	}

	public int init(Context context , UsbDeviceConnection usbDeviceConnection , UsbDevice usbDevice){
		if(context == null){
			return INIT_CONTEXT_NULL ;
		}
		if(usbDeviceConnection == null){
			return INIT_USB_NO_CONNECTED ;
		}
		if(usbDeviceConnection == null){
			return INIT_USB_DEVICE_NULL;
		}
		resetParm();
		mContext = context ;
		mUsbDeviceConnection = usbDeviceConnection ;
		mUsbDevice = usbDevice ;
		if(USBContants.BES_CDC_VENDER_ID == usbDevice.getVendorId() && USBContants.BES_CDC_PRODUCT_ID == usbDevice.getProductId()){
			mBesCdcDriver = new BesCdcDriver(mUsbDeviceConnection , mUsbDevice , this);
			return INIT_DONE ;
		}else{
			mBesAudioDriver = new BesAudioDriver(mUsbDeviceConnection);
			try{
				mTypeCVersion = mBesAudioDriver.getFirmwareVersion()  ;
				mSerialNumber = mBesAudioDriver.getSerialNumber() ;
				mProfileVersion = mBesAudioDriver.getProfileVersion();
			}catch (Exception e){
				LOG("GET Audio driver Msg exception "+e.getMessage());
			}
			return INIT_DONE ;
		}
	}

	public void finalize(){
		resetParm();
		if(mBesCdcDriver != null){
			mBesCdcDriver.release();
			mBesCdcDriver = null ;
		}
		if(mBesAudioDriver != null){
			mBesAudioDriver = null ;
		}
		try{
			synchronized (mLock){
				mLock.notifyAll();
			}
		}catch (Exception e){
			LOG(e.getMessage());
		}

	}

	/**
	 * 执行烧录动作,阻塞操作
	 * @param firmwareFileUrl
	 * @return
	 */
	public int download(String firmwareFileUrl){
		if( mUsbDevice == null || mUsbDeviceConnection == null){
			LOG("download DOWNLOAD_NO_DEVICE_CONNECTED");
			return DOWNLOAD_NO_DEVICE_CONNECTED ;
		}
		if(mUsbDevice.getDeviceClass() == USBContants.USB_TYPE_AUDIO){
			LOG("download DOWN_IN_CDC_DEVICE");
			return DOWNLOAD_NO_IN_CDC_MODE ;
		}
		if(!isIdle()){
			LOG("download !isIdle()");
			return DOWNLOAD_BUSSING ;
		}
		int initFileRet = initFile(firmwareFileUrl) ;
		mFirmwareFilePath = firmwareFileUrl;
		LOG("download initFileRet "+ initFileRet);
		if(initFileRet != DOWNLOAD_READY){
			return initFileRet ;
		}
		if(mBesCdcDriver != null){
			int ret = mBesCdcDriver.init();
			if(ret != BesCdcDriver.INIT_DONE){
				return  DOWNLAOD_NO_BULK_ENDPOINT ;
			}
			mOtaStatus = OTA_START;
			progress(0);
			try {
				LOG("ready synchronized (mLock)");
				synchronized (mLock) {
					LOG("ready mLock.wait();");
					while (!(mOtaStatus == OTA_DONE || mOtaStatus == OTA_FAILED || mOtaStatus == OTA_IDLE)){
						LOG("mLock.wait();");
						mLock.wait();
					}
				}
			} catch (Exception e) {
				LOG("failed mLock.wait();");
				LOG(e.getMessage());
			}
			if(mOtaStatus == OTA_DONE){
				resetParm();
				return DOWNLAOD_DONE ;
			}else{
				resetParm();
				return downMsgCode;
			}
		}
		return DOWNLOAD_OTHER_ERROR ;
	}

	/**
	 * 获取设备VENDOR ID
	 * @return
	 */
	public int getVendorID(){
		if(mUsbDevice != null){
			return  mUsbDevice.getVendorId() ;
		}
		return  0 ;
	}

	/**
	 * 获取设备PRODUCT ID
	 * @return
	 */
	public int getProductID(){
		if(mUsbDevice != null){
			return  mUsbDevice.getProductId() ;
		}
		return 0 ;
	}

	/**
	 * 获取厂商名称
	 * @return
	 */
	public String getManufacturerString(){
		if(mUsbDevice != null){
			return  mUsbDevice.getManufacturerName() ;
		}
		return null ;
	}

	/**
	 * 获取产品名称
	 * @return
	 */
	public String getProductString(){
		if(mUsbDevice != null){
			return  mUsbDevice.getProductName() ;
		}
		return  null ;
	}

	/**
	 * 获取通讯协议版本，可用于判断设备有效性或协议版本
	 * @return
	 */
	public String getProfileVersion(){
		return mProfileVersion;
	}

	/**
	 * 获取SN码
	 * @return
	 */
	public String getSerialNumberString(){
		return mSerialNumber ;
	}

	/**
	 * 获取TYPE_C 设备版本
	 * @return
	 */
	public String getTypeCVersionString(){
		return mTypeCVersion ;
	}

	/**
	 * 返回是否需要升级，只有连接状态下有效
	 * @param fileName
	 * @return
	 */
	public boolean detectNewFirmware(String fileName){
		String otafileVersion = readOtafileVersion(fileName);
		LOG("detectNewFirmware otafileVersion = "+otafileVersion+" mTypeCVersion = "+mTypeCVersion);
		if(otafileVersion != null && mTypeCVersion != null){
			VersionCompareUtils versionCompareUtils = new VersionCompareUtils();
			int ret = versionCompareUtils.checkVersion(mTypeCVersion , otafileVersion);
			detectNewFirmwareCode = ret ;
			if(ret == VersionCompareUtils.DETECT_PASS){
				return  true ;
			}
		}
		return  false ;
	}

	public boolean sendCdcCmd(){
		if(mBesAudioDriver != null){
			int ret = mBesAudioDriver.sendCdcCmd();
			if(ret == BesAudioDriver.ENTER_CDC_AND_REBOOT){
				return  true ;
			}
		}
		return  false ;
	}

	/**
	 * 获取文件匹配结果编码，一般调用与detectNewFirmware 之后
	 * @return
	 */
    public int getDetectVersionCode(){
		return detectNewFirmwareCode ;
	}

	/**
	 * 返回当前系统需要升级的镜像类型
	 * @return
	 */
	public String getFirmwareABInfo(){
		if(mTypeCVersion != null){
			if(mTypeCVersion.endsWith("a")){
				return "b";
			}else if(mTypeCVersion.endsWith("b")){
				return "a";
			}
		}
		return "" ;
	}

	/**
	 * 获取当前升级进度
	 * @return
	 */
	public int getProcessPerCent(){
		return  mProcess ;
	}

	private int getUsbType(){
		if(mUsbDevice != null){
			return  mUsbDevice.getDeviceClass() ;
		}
		return -1 ;
	}

	public boolean isCdcDevice(){
		if(mUsbDevice != null){
			if(getUsbType() == USBContants.USB_TYPE_CDC){
				return  true  ;
			}else{
				return  false ;
			}
		}
		return  false ;
	}


	public void setSupportLog(boolean isNeedOpen){
		if(isNeedOpen){
			LogUtils.InitLogUtils(true);
		}else{
			LogUtils.InitLogUtils(false);
		}
	}

	private void resetParm(){
		mProfileVersion = null ;
		mSerialNumber = null ;
		mTypeCVersion = null ;
		copyBootFlag = null ;
		mProcess = 0 ;
		mOtaStatus = OTA_IDLE;
		checkTimeOut.removeMessages(TIME_OUT_CHECK);
	}

	final int TIME_OUT_CHECK = 0X01 ;
	int timeOutSpan = 15000 ; 	// 十秒钟进度超时 TODO
	Handler checkTimeOut = new Handler(Looper.getMainLooper()){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(msg.what == TIME_OUT_CHECK){
				otaHandler.obtainMessage(OTA_OTHER_ERROR , "time out").sendToTarget();
			}
		}
	};

	void progress(int progress){
		checkTimeOut.removeMessages(TIME_OUT_CHECK);
		if (progress != 100) {
			checkTimeOut.sendEmptyMessageDelayed(TIME_OUT_CHECK , timeOutSpan);
		}
		mProcess = progress ;
	}

	Handler otaHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			mOtaStatus = msg.what ;
			switch (msg.what) {
				case OTA_START:
					LOG("OTA_START");
					progress(0);
					break;
				case OTA_FIRST_HANDSHAKE_START://cdc 下 读取 bluk 由设备发起握手
					progress(2);
					LOG("OTA_FIRST_HANDSHAKE_START");
					byte req = (byte) msg.obj;
					UsbMessage handShark = MessageFactory.getHandShakeCmdResponse(req);
					if(mBesCdcDriver != null){
						mBesCdcDriver.sendMeesage(handShark);
					}else {
						downMsgCode = DOWNLOAD_HANDSHAKE_FAILED ;
						otaHandler.sendEmptyMessage(OTA_FAILED);
					}
					break;
				case OTA_FIRST_HANDSHAKE_FAILED:
					LOG("OTA_FIRST_HANDSHAKE_FAILED");
					downMsgCode = DOWNLOAD_HANDSHAKE_FAILED ;
					otaHandler.sendEmptyMessage(OTA_FAILED);
					break;
				case OTA_FIRST_HANDSHAKE_SUCCESSFUL:
					progress(5);
					LOG("OTA_FIRST_HANDSHAKE_SUCCESSFUL");
					programmerUtils = new ProgrammerUtils();
					boolean ret = programmerUtils.initProgrammer(mContext);
					if (ret) {
						otaHandler.sendEmptyMessage(OTA_START_DOWNLOAD_PROGRAMMER);
					} else {
						LOG("programmerUtils.initProgrammer(mContext) failed");
						otaHandler.obtainMessage(OTA_DOWNLOAD_PROGRAMMER_FAILED , "info message is null").sendToTarget();
					}
					break;
				case OTA_START_DOWNLOAD_PROGRAMMER:
					progress(8);
					LOG("OTA_START_DOWNLOAD_PROGRAMMER");
					UsbMessage programmerInfo = programmerUtils.prepareProgrammer();
					if (programmerInfo != null) {
						mBesCdcDriver.sendMeesage(programmerInfo);
					} else {
						otaHandler.obtainMessage(OTA_DOWNLOAD_PROGRAMMER_FAILED , "info message is null").sendToTarget();
					}
					break;
				case OTA_START_DOWNLOAD_PROGRAMMER_BIN:
					progress(10);
					LOG("OTA_START_DOWNLOAD_PROGRAMMER_BIN");
					UsbMessage programmerBin = programmerUtils.sendProgrammerBin();
					if (programmerBin != null) {
						mBesCdcDriver.sendMeesage(programmerBin);
					} else {
						otaHandler.obtainMessage(OTA_DOWNLOAD_PROGRAMMER_FAILED , "bin message is null").sendToTarget();
					}
					break;
				case OTA_DOWNLOAD_PROGRAMMER_FAILED:
					String errorMsg = (String) msg.obj;
					LOG("OTA_DOWNLOAD_PROGRAMMER_FAILED errorMsg = "+errorMsg);
					downMsgCode = DOWNLOAD_PROGRAMMER_INFO_FAILED ;
					otaHandler.sendEmptyMessage(OTA_FAILED);
					break;
				case OTA_DOWNLOAD_PROGRAMMER_SUCCESSFUL:
					progress(15);
					LOG("OTA_DOWNLOAD_PROGRAMMER_SUCCESSFUL");
					otaHandler.sendEmptyMessage(OTA_RUN_PROGRAMMER);
					break;
				case OTA_RUN_PROGRAMMER:
					LOG("OTA_RUN_PROGRAMMER");
					UsbMessage runUseMessage = programmerUtils.sendRunProgrammer();
					mBesCdcDriver.sendMeesage(runUseMessage);
					break;
				case OTA_RUN_PROGRAMMER_SUCCESSFUL:
					progress(20);
					LOG("OTA_RUN_PROGRAMMER_SUCCESSFUL");
					otaHandler.sendEmptyMessage(OTA_WAIT_RAM_RUN);
					break;
				case OTA_RUN_PROGRAMMER_FAILED:
					String runProgrammerErrorMsg = (String) msg.obj;
					LOG("OTA_RUN_PROGRAMMER_FAILED errorMsg = "+runProgrammerErrorMsg);
					downMsgCode = DOWNLOAD_RUN_PROGRAMMER_FAILED ;
					otaHandler.sendEmptyMessage(OTA_FAILED);
					break;
				case OTA_WAIT_RAM_RUN:
					LOG("OTA_WAIT_RAM_RUN");
					break;
				case OTA_READ_BOOT_FLAG:
					progress(25);
					UsbMessage readBootMsg = MessageFactory.getReadBootFlagMsg();
					mBesCdcDriver.sendMeesage(readBootMsg);
					break;
				case OTA_ERASE_BAT_BOOT_FLAG:
					progress(30);
					UsbMessage eraseBatBootFlag = MessageFactory.getEraseBatBootFlagMsg( REQ_ERASE_BAT_BOOT_FLAG);
					mBesCdcDriver.sendMeesage(eraseBatBootFlag);
					break;
				case OTA_COPY_TO_BAT_BOOT_FLAG:
					progress(35);
					UsbMessage writeBatBootFlag = MessageFactory.getWriteBatBootFlagMsg(copyBootFlag , REQ_WRITE_BOOT_FLAG_TO_BAT_ADDR);
					if(writeBatBootFlag != null){
						mBesCdcDriver.sendMeesage(writeBatBootFlag);
					}else{
						otaHandler.obtainMessage(OTA_OTHER_ERROR, "writeBatBootFlag is null").sendToTarget();
					}
					break;
				case OTA_START_DOWNLOAD_BURN:
					progress(40);
					LOG("OTA_START_DOWNLOAD_BURN");
					burnFlashUtils = new BurnFlashUtils();
					burnFlashUtils.initProgrammer(mFirmwareFilePath , mSectorSize);
					UsbMessage burnInfo = burnFlashUtils.prepareBurn();
					if (burnInfo != null) {
						mBesCdcDriver.sendMeesage(burnInfo);
					} else {
						otaHandler.obtainMessage(OTA_START_DOWNLOAD_BURN_FAILED , "burnInfo message is null").sendToTarget();
					}
					break;
				case OTA_START_DOWNLOAD_BURN_NEXT:
					progress(50);
					LOG("OTA_START_DOWNLOAD_BURN_NEXT ");
					int sectorNum = (int) msg.obj ;
					if(!burnFlashUtils.isSeqEnd()){
						int max = burnFlashUtils.getMaxSeq();
						for (int i = 0 ; i < max ; i++){
							UsbMessage burnBinInfo = burnFlashUtils.sendProgrammerBinNext();
							mBesCdcDriver.sendMeesage(burnBinInfo);
							LOG("OTA_START_DOWNLOAD_BURN_DONE AND WAIT THE RESPONSE ");
						}
					}else{
						LOG("OTA_START_DOWNLOAD_BURN_DONE isSeqEnd  = sectorNum "+sectorNum+" getMaxSeq = "+burnFlashUtils.getMaxSeq());
						if(sectorNum == burnFlashUtils.getMaxSeq() - 1){
							otaHandler.sendEmptyMessage(OTA_START_DOWNLOAD_BURN_SUCCESFUL);
						}
					}
					break;
				case OTA_START_DOWNLOAD_BURN_FAILED:
					String burnErrorMsg = (String) msg.obj;
					LOG("OTA_START_DOWNLOAD_IMG_FAILED errorMsg = "+burnErrorMsg);
					downMsgCode = DOWNLOAD_BURN_INFO_FAILED ;
					otaHandler.sendEmptyMessage(OTA_FAILED);
					break;
				case OTA_START_DOWNLOAD_BURN_SUCCESFUL:
					progress(80);
					LOG("OTA_START_DOWNLOAD_BURN_SUCCESFUL ");
					otaHandler.sendEmptyMessage(OTA_BURN_WIRTE_MAGIC_NUMBER);
					break;
				case OTA_BURN_WIRTE_MAGIC_NUMBER :
					progress(85);
					LOG("OTA_BURN_WIRTE_MAGIC_NUMBER ");
					UsbMessage writeMagic = MessageFactory.getWritBrunDataToBurnAddress(burnFlashUtils.getmBootAddr() , REQ_WRITE_MAGIC_NUMBER);
					mBesCdcDriver.sendMeesage(writeMagic);
					break;
				case OTA_BURN_ERASE_BOOT_FLAG:
					progress(90);
					LOG("OTA_BURN_ERASE_FLAG_SECTOR ");
					UsbMessage eraseBootFlag = MessageFactory.getEraseBootFlagMsg(REQ_ERASE_BOOT_FLAG);
					mBesCdcDriver.sendMeesage(eraseBootFlag);
					break;
				case OTA_BURN_WRITE_BOOT_FLAG:
					progress(95);
					LOG("OTA_BURN_WRITE_FLAG_SECTOR ");
					UsbMessage writeSectorFlag = MessageFactory.getWriteBootFlagMsg(isAboot , REQ_WRITE_BOOT_FALG);
					mBesCdcDriver.sendMeesage(writeSectorFlag);
					break;
				case OTA_SET_BOOT_MODE:
					LOG("OTA_SET_BOOT_MODE ");
					UsbMessage setBootModeMsg = MessageFactory.getSetModeMsg(REQ_SET_BOOT_MODE);
					mBesCdcDriver.sendMeesage(setBootModeMsg);
					break;
				case OTA_REBOOT_FLASH:
					progress(99);
					LOG("OTA_REBOOT_FLASH ");
					UsbMessage rebootMsg = MessageFactory.getSystemReBootMsg(REQ_REBOOT_SYSTEM);
					mBesCdcDriver.sendMeesage(rebootMsg);
					break;
				case OTA_DONE:
					progress(100);
					LOG("OTA_DONE ");
					synchronized (mLock){
						mLock.notifyAll();
					}
					break;
				case OTA_OTHER_ERROR:
					LOG("OTA_OTHER_ERROR ");
					String otherError = (String) msg.obj;
					LOG("OTA_OTHER_ERROR errorMsg = "+otherError);
					downMsgCode = DOWNLOAD_OTHER_ERROR ;
					otaHandler.sendEmptyMessage(OTA_FAILED);
					break;
				case OTA_FAILED:
					UsbMessage rebootFailed = MessageFactory.getSystemReBootMsg(REQ_OTA_FAILED_REBOOT_SYSTEM);
					mBesCdcDriver.sendMeesage(rebootFailed);
					synchronized (mLock){
						mLock.notifyAll();
					}
				case OTA_IDLE:
					LOG("OTA_IDLE ");
					mProcess = 0 ;
					break;
				default:
			}
		}
	};

	@Override
	public void onDataReceive(byte[] datas) {
		if (datas.length == 8 && (datas[0] & 0xff) == USBContants.PUBLIC_HEAD && (datas[1] & 0xff) == USBContants.CmdType.HAND_SHAKE_CMD.getByte()) {
			if ((datas[4] & 0xff) == 0x00) {
				otaHandler.obtainMessage(OTA_FIRST_HANDSHAKE_START, datas[2]).sendToTarget();
			} else if ((datas[4] & 0xff) == 0x02) {
				otaHandler.sendEmptyMessage(OTA_FIRST_HANDSHAKE_SUCCESSFUL);
			}
		} else if (datas.length == 6 && (datas[0] & 0xff) == USBContants.PUBLIC_HEAD && (datas[1] & 0xff) == USBContants.CmdType.PROGRAMMER_INFO_CMD.getByte()) {
			if ((datas[4] & 0xff) == 0x00) {//be,53,00,01,00,ed,
				otaHandler.sendEmptyMessage(OTA_START_DOWNLOAD_PROGRAMMER_BIN);
			} else{
				otaHandler.obtainMessage(OTA_DOWNLOAD_PROGRAMMER_FAILED, getProgramInfoErrorMsg(datas[4])).sendToTarget();
			}
		} else if (datas.length == 6 && (datas[0] & 0xff) == USBContants.PUBLIC_HEAD && (datas[1] & 0xff) == USBContants.CmdType.PROGRAMMER_BIN_CMD.getByte()) {
			if ((datas[4] & 0xff) == 0x20) {
				otaHandler.sendEmptyMessage(OTA_DOWNLOAD_PROGRAMMER_SUCCESSFUL);
			} else
				otaHandler.obtainMessage(OTA_DOWNLOAD_PROGRAMMER_FAILED, getProgramBinErrorMsg(datas[4])).sendToTarget();
		} else if (datas.length > 2 && (datas[0] & 0xff) == USBContants.PUBLIC_HEAD && (datas[1] & 0xff) == USBContants.CmdType.PROGRAMMER_RUN.getByte()) {
			if(datas.length == 10){
				if(datas[4] == 0x00){
					otaHandler.sendEmptyMessage(OTA_WAIT_RAM_RUN);
					byte[] CODERET = new byte[4];
					System.arraycopy(datas , 5 ,CODERET , 0 , 4);
					LOG("CODERET is "+ ArrayUtil.toHex(CODERET));
				}else{
					otaHandler.obtainMessage(OTA_RUN_PROGRAMMER_FAILED, getRunProgrammerErrorMsg(datas[4])).sendToTarget();
				}
			}else if(datas.length == 6){
				if(datas[4] == 0x00){
					otaHandler.obtainMessage(OTA_RUN_PROGRAMMER_FAILED, "ram not run").sendToTarget();
				}else{
					otaHandler.obtainMessage(OTA_RUN_PROGRAMMER_FAILED, getRunProgrammerErrorMsg(datas[4])).sendToTarget();
				}
			}
		}else if (datas.length == 11 && (datas[0] & 0xff) == USBContants.PUBLIC_HEAD && (datas[1] & 0xff) == USBContants.CmdType.SECTOR_RESPONSE.getByte()) {
			byte[] version = new byte[2];
			byte[] sectorSize = new byte[4];
			System.arraycopy(datas , USBContants.MSG_OVERHEAD ,version , 0 , 2);
			System.arraycopy(datas , USBContants.MSG_OVERHEAD + 2 , sectorSize , 0 , 4);
			mSectorSize =  ((sectorSize[0]&0xff)+((sectorSize[1]<<8)&0xffff) +((sectorSize[2]<<16)&0xffffff)+((sectorSize[3]<<24)&0xffffffff)) ;
			LOG("BURN VERSION = "+ version[1]+"."+version[0]);
			LOG("BURN SECTOR SIZE  = "+mSectorSize);
			otaHandler.obtainMessage(OTA_READ_BOOT_FLAG ,  mSectorSize).sendToTarget();
		}else if ((datas[0] & 0xff) == USBContants.PUBLIC_HEAD && (datas[1] & 0xff) == USBContants.CmdType.READ_CMD.getByte()) {
			if(datas.length == 14 && datas[4] == 0){
				copyBootFlag = new byte[8];
				System.arraycopy(datas , 5 , copyBootFlag , 0 , 8);
				LOG("READ BOOT_FLAG IS "+ ArrayUtil.toASCII(copyBootFlag));
				otaHandler.sendEmptyMessage(OTA_ERASE_BAT_BOOT_FLAG);
			}else{
				otaHandler.obtainMessage(OTA_OTHER_ERROR, getReadErrorMsg(datas[4])).sendToTarget();
			}
		}else if ((datas[0] & 0xff) == USBContants.PUBLIC_HEAD && (datas[1] & 0xff) == USBContants.CmdType.WRITE_OR_ERASE_CMD.getByte()) {
			if ((datas[4] & 0xff) == 0x00) {// be,65,04,01,00,d7,
				byte reqNumber = datas[2];
				if(reqNumber == REQ_ERASE_BAT_BOOT_FLAG){ //读取boot标志位值后进行擦出备份boot标识位值
					otaHandler.sendEmptyMessage(OTA_COPY_TO_BAT_BOOT_FLAG );
				}else if(reqNumber == REQ_WRITE_BOOT_FLAG_TO_BAT_ADDR ){//把刚读取到到boot标志位复制到备份boot地址
					otaHandler.sendEmptyMessage(OTA_START_DOWNLOAD_BURN);
				}else if(reqNumber == REQ_ERASE_BOOT_FLAG){//擦除boot标志位值,之后执行写入
					otaHandler.sendEmptyMessage(OTA_BURN_WRITE_BOOT_FLAG);
				}else if(reqNumber == REQ_WRITE_MAGIC_NUMBER){//写入特定值之后，擦除boot地址数据
					otaHandler.sendEmptyMessage(OTA_BURN_ERASE_BOOT_FLAG);
				}else if(reqNumber == REQ_WRITE_BOOT_FALG){//写入标志位之后设置boot MODE
					otaHandler.sendEmptyMessage(OTA_SET_BOOT_MODE);
				}
			} else{
				otaHandler.obtainMessage(OTA_OTHER_ERROR, getWriteOrEraseErrorMsg(datas[4])).sendToTarget();
			}
		}else if (datas.length == 6 && (datas[0] & 0xff) == USBContants.PUBLIC_HEAD && (datas[1] & 0xff) == USBContants.CmdType.BURN_INFO_CMD.getByte()) {
			if ((datas[4] & 0xff) == 0x00) {
				otaHandler.obtainMessage(OTA_START_DOWNLOAD_BURN_NEXT , 0).sendToTarget();
			} else{
				otaHandler.obtainMessage(OTA_START_DOWNLOAD_BURN_FAILED, getBurnInfoErrorMsg(datas[4])).sendToTarget();
			}
		}else if (datas.length == 8 && (datas[0] & 0xff) == USBContants.PUBLIC_HEAD && (datas[1] & 0xff) == USBContants.CmdType.BURN_BIN_CMD.getByte()) {
			if ((datas[4] & 0xff) == 0x60) {//be,62,c3,03,60,02,00,b7
				int sectorSeq = datas[5]+ ((datas[6]<<8)&0XFF00);
				LOG("RECEIVE sectorSeq = "+ sectorSeq);
				otaHandler.obtainMessage(OTA_START_DOWNLOAD_BURN_NEXT , sectorSeq).sendToTarget();
			} else{
				otaHandler.obtainMessage(OTA_START_DOWNLOAD_BURN_FAILED, getBurnBinErrorMsg(datas[4])).sendToTarget();
			}
		}else if ((datas[0] & 0xff) == 0xBE && (datas[1] & USBContants.PUBLIC_HEAD) == USBContants.CmdType.SYS_CMD.getByte()){
			if(datas.length == 6 ){//be,00,08,01,00,38,
				if ((datas[4] & 0xff) == 0x00) {
					if(datas[2] == REQ_REBOOT_SYSTEM){
						otaHandler.obtainMessage(OTA_DONE ).sendToTarget();
						return;
					}else if(datas[2] == REQ_SET_BOOT_MODE){
						otaHandler.obtainMessage(OTA_REBOOT_FLASH ).sendToTarget();
						return;
					}else if(datas[2] == REQ_OTA_FAILED_REBOOT_SYSTEM){
						// nothing to do
					}
				}
			}else if(datas.length == 10){
				if ((datas[4] & 0xff) == 0x00) {
					if(datas[2] == REQ_SET_BOOT_MODE){
						otaHandler.obtainMessage(OTA_REBOOT_FLASH ).sendToTarget();
						return;
					}
				}
			}
			otaHandler.obtainMessage(OTA_OTHER_ERROR, getRebootErrorMsg(datas[4])).sendToTarget();
		}

	}


	private int initFile(String firmwareFileUrl){
		if(firmwareFileUrl == null || !firmwareFileUrl.contains("bin")){
		   return DOWNLOAD_FILE_NOT_FOUND ;
		}
		if(!FileUtils.isFileExist(firmwareFileUrl)){
			return DOWNLOAD_FILE_NOT_FOUND ;
		}
		String otafileVersion = readOtafileVersion(firmwareFileUrl);
		LOG("otafileVersion = "+otafileVersion+" mTypeCVersion = "+mTypeCVersion);
		if(otafileVersion != null){
			return DOWNLOAD_READY;
		}
		return DOWNLOAD_READ_FILE_VERSION_FIALED ;
	}

	/**
	 * 读取升级文件的版本信息
	 * @param firmwareFileUrl
	 * @return
	 */
	private String readOtafileVersion(String firmwareFileUrl){
		if(!FileUtils.isFileExist(firmwareFileUrl)){
			return  null ;
		}
		if(!firmwareFileUrl.endsWith("bin")){
			return null;
		}
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(firmwareFileUrl);
			int LEN = inputStream.available() ;
			byte[] totalDatas = new byte[LEN];
			int ret = inputStream.read(totalDatas ,0 ,LEN);
			byte[] buildInfoAddr = new byte[4];
			System.arraycopy(totalDatas , 0x0c , buildInfoAddr , 0 , 4); // OXOC为buildinfo的值的对应首地址
			byte[] bootStartAddr = new byte[4];
			System.arraycopy(totalDatas , LEN - 4 , bootStartAddr , 0 , 4);
			LOG("buildInfoAddr = "+ArrayUtil.toHex(buildInfoAddr)+" bootStartAddrInt "+ ArrayUtil.toHex(bootStartAddr));
			int bootStartAddrInt = (bootStartAddr[0]&0xff)+((bootStartAddr[1]&0xff)<<8)+((bootStartAddr[2]&0xff)<<16)+((bootStartAddr[3]&0xff)<<24);
			int buildInfoAddrInt = (buildInfoAddr[0]&0xff)+((buildInfoAddr[1]&0xff)<<8)+((buildInfoAddr[2]&0xff)<<16)+((buildInfoAddr[3]&0xff)<<24);
			if(buildInfoAddrInt > bootStartAddrInt) {
				int buildInfoInBootFileAddr = buildInfoAddrInt - bootStartAddrInt;
				LOG("8 buildInfoAddrInt = " + buildInfoAddrInt + " -  " + bootStartAddrInt + " = " + buildInfoInBootFileAddr);
				byte[] buildInfoVersion = new byte[64];
				System.arraycopy(totalDatas, buildInfoInBootFileAddr, buildInfoVersion, 0, 64);
				String versionMsg = ArrayUtil.toASCII(buildInfoVersion);
				LOG(versionMsg + "");
				if (versionMsg.startsWith("SW_VER") && versionMsg.contains("\n")) {
					versionMsg = versionMsg.split("\n")[0];
					LOG("versionMsg = "+versionMsg);
					if(versionMsg != null && versionMsg.contains("SW_VER=")){
						versionMsg = versionMsg.replace("SW_VER=","").trim();
					}
					if(versionMsg != null && versionMsg.endsWith("a")){
						LOG("check result is a boot ");
						isAboot = true ;
					}else{
						LOG("check result is b boot ");
						isAboot = false ;
					}
					return versionMsg;
				} else {
					LOG(" NOT versionMsg.startsWith(\"SW_VER\") && versionMsg.contains(\"\\n\")");
				}
			}
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
		return  null ;
	}

	public boolean isIdle(){
		return mOtaStatus == OTA_IDLE;
	}


	private void sendMessage(UsbMessage msg) {
		LOG("ready to sendMessage(byte[] msg) "+ ArrayUtil.toHex(msg.getBytes())) ;
		if(mBesCdcDriver != null){
			mBesCdcDriver.sendMeesage(msg);
		}
	}

	private String getProgramInfoErrorMsg(byte response){
		if(response == 0x01){
			return "Length error";
		}else if(response == 0x02){
			return "Checksum error";
		}else if(response == 0x03){
			return "not Sync yet";
		}
		return "unkown error";
	}

	private String getProgramBinErrorMsg(byte response){
		if(response == 0x01){
			return "Length error";
		}else if(response == 0x02){
			return "Checksum error";
		}else if(response == 0x24){
			return "Code information message (type 0x53) missing";
		}else if(response == 0x25){
			return "Code CRC error";
		}
		return "unkown error";
	}

	private String getRunProgrammerErrorMsg(byte response){
		if(response == 0x03){
			return "not sync yet";
		}else if(response == 0x31){
			return "Code missing";
		}
		return  "unkown error";
	}

	private String getReadErrorMsg(byte response){
		if(response == 0x01){
			return  "length error";
		}else if(response == 0x02){
			return  "Checksum error";
		}else if(response == 0x03){
			return  "Not sync yet";
		}else if(response == 0x07){
			return  "Invalid address";
		}else if(response == 0x08){
			return  "Invalid data length";
		}else if(response == 0x09){
			return  "Access right error";
		}
		return  "unkown error";
	}

	private String getBurnInfoErrorMsg(byte response){
		if(response == 0x01){
			return  "length error";
		}else if(response == 0x02){
			return  "Checksum error";
		}else if(response == 0x07){
			return  "Invalid address";
		}else if(response == 0x61){
			return  "Unsupported sector size";
		}else if(response == 0x62){
			return  "sector sequence overflow";
		}
		return  "unknow error";
	}

	private String getBurnBinErrorMsg(byte response){
		if(response == 0x01){
			return "length error";
		}else if(response == 0x02){
			return  "checksum error";
		}else if(response == 0x63){
			return "no burn info";
		}else if(response == 0x64){
			return  "incorrect sector data length";
		}else if(response == 0x65){
			return  "sector data crc error";
		}else if(response == 0x66){
			return  "sector sequence error";
		}else if(response == 0x67){
			return  "erase error";
		}else if(response == 0x68){
			return  "burn error";
		}
		return  "unknow error";
	}

	private String getWriteOrEraseErrorMsg(byte response){
		if(response == 0x01){
			return "length error";
		}else if(response == 0x02){
			return  "checksum error";
		}else if(response == 0x07){
			return "Invalid address";
		}else if(response == 0x08){
			return  "Invalid data length";
		}else if(response == 0x61){
			return  "Invalid sector size";
		}else if(response == 0x67){
			return  "erase error";
		}else if(response == 0x68){
			return  "burn error";
		}else if(response == 0x69){
			return  "Invalid burn cmd";
		}
		return  "unknow error";
	}

	private String getRebootErrorMsg(byte response){
		if(response == 0x01){
			return "length error";
		}else if(response == 0x02){
			return  "checksum error";
		}else if(response == 0x03){
			return  "Not sync yet";
		}else if(response == 0x06){
			return "Invalid command";
		}
		return  "unknow error";
	}

	private void LOG(String msg){
		if(msg != null){
			LogUtils.writeComm(TAG , FileUtils.USB_OTA_FILE , msg);
		}
	}

}
