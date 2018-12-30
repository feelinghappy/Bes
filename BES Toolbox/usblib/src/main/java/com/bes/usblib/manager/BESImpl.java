package com.bes.usblib.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;

import com.bes.usblib.callback.BaseUsbDriver;
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class BESImpl implements IUsbCallback {

	String TAG = "BESCdcOtaImpl";

	//连接状态回调
	public static final int CONNECTION_CONNECTED = 0 ;
	public static final int CONNECTION_FAILED_WITH_NO_DEVICE = 1 ;
	public static final int CONNECTION_FAILED_WITH_NO_BES_DEVICE=  2 ;
	public static final int CONNECTION_FAILED_IN_CDC_MODE = 3 ;
	public static final int CONNECTION_FAILED_NO_PREMISSION = 4 ;
	public static final int CONNECTION_CDC_DEVICE_GOING = 5 ;
	public static final int CONNECTION_AUDIO_DEVICE_GOING = 6 ;
	public static final int CONNECTION_DISCONNECTED = 7 ;
	public static final int CONNECTION_ATTACHED = 8 ;
	public static final int CONNECTION_DETACHED = 9 ;
	public static final int CONNECTION_IDLE = 10 ;
	public int mConnectionSatus = CONNECTION_IDLE ;


	//执行下载返回码
	public static final int DOWNLAOD_READY = 0 ;
	public static final int DOWNLOAD_FILE_NOT_FOUND = 1 ;
	public static final int DOWNLOAD_VERSION_IS_SAME = 2;
	public static final int DOWNLOAD_NO_DEVICE_CONNECTED = 3 ;
	public static final int DOWNLOAD_IN_CDC_DEVICE = 4;
	public static final int DOWNLOAD_ONLY_SUPPORT_AUTO_OTA = 5 ;
	public static final int DOWNLOAD_READ_FILE_VERSION_FIALED  = 6 ;
	public static final int DOWNLOAD_HANDSHAKE_FAILED = 7 ;
	public static final int DOWNLOAD_PROGRAMMER_INFO_FAILED = 8 ;
	public static final int DOWNLOAD_RUN_PROGRAMMER_FAILED = 9 ;
	public static final int DOWNLOAD_BURN_INFO_FAILED = 10 ;
	public static final int DOWNLOAD_OTHER_ERROR = 11 ;
	public static final int DOWNLOAD_NO_PERMISSION = 12 ;
	public static final int DOWNLOAD_WRONG_BOOT_TYPE_FILE = 13 ;


	//由于以下擦除跟写入都用到了统一指令类型-x65,故使用固定req值进行区分 begin
	public static final byte REQ_ERASE_BAT_BOOT_FLAG = 0X10 ;
	public static final byte REQ_WRITE_BOOT_FLAG_TO_BAT_ADDR = 0X11 ;
	public static final byte REQ_WRITE_MAGIC_NUMBER = 0X12 ;
	public static final byte REQ_ERASE_BOOT_FLAG = 0X13 ;
	public static final byte REQ_WRITE_BOOT_FALG = 0X14 ;
	//由于以下擦除跟写入都用到了统一指令类型-x65,故使用固定req值进行区分 end

	//由于设置boot mode 与 reboot 用到了同一条指令 ，故使用固定req值进行区分 begin
	public static final byte REQ_SET_BOOT_MODE = 0X15 ;
	public static final byte REQ_REBOOT_SYSTEM = 0X16 ;
	//由于设置boot mode 与 reboot 用到了同一条指令 ，故使用固定req值进行区分 end


	// ota整套逻辑进度 begin
	public static final int OTA_START = 0;
	public static final int OTA_FIRST_HANDSHAKE_START = 1;
	public static final int OTA_FIRST_HANDSHAKE_FAILED = 2;
	public static final int OTA_FIRST_HANDSHAKE_SUCCESSFUL = 3;
	public static final int OTA_START_DOWNLOAD_PROGRAMMER = 4;
	public static final int OTA_START_DOWNLOAD_PROGRAMMER_BIN = 5;
	public static final int OTA_DOWNLOAD_PROGRAMMER_FAILED = 6;
	public static final int OTA_DOWNLOAD_PROGRAMMER_SUCCESSFUL = 7;
	public static final int OTA_RUN_PROGRAMMER = 8 ;
	public static final int OTA_RUN_PROGRAMMER_SUCCESSFUL = 9 ;
	public static final int OTA_RUN_PROGRAMMER_FAILED = 10 ;
	public static final int OTA_WAIT_RAM_RUN = 11;
	public static final int OTA_READ_BOOT_FLAG = 12 ;
	public static final int OTA_ERASE_BAT_BOOT_FLAG = 13 ;
	public static final int OTA_COPY_TO_BAT_BOOT_FLAG = 14 ;
	public static final int OTA_START_DOWNLOAD_BURN = 15;
	public static final int OTA_START_DOWNLOAD_BURN_NEXT = 16;
	public static final int OTA_START_DOWNLOAD_BURN_FAILED = 17;
	public static final int OTA_START_DOWNLOAD_BURN_SUCCESFUL = 18;
	public static final int OTA_BURN_WIRTE_MAGIC_NUMBER = 19 ;
	public static final int OTA_BURN_ERASE_BOOT_FLAG = 20 ;
	public static final int OTA_BURN_WRITE_BOOT_FLAG = 21 ;
	public static final int OTA_SET_BOOT_MODE = 22 ;
	public static final int OTA_REBOOT_FLASH = 23;
	public static final int OTA_DONE = 24 ;
	public static final int OTA_OTHER_ERROR = 25 ; //代表OTA_BURN_WIRTE_MAGIC_NUMBER往后的所有指令交互错误
	public static final int OTA_IDLE = 26;
	int mOtaStatus = OTA_IDLE;
	// ota整套逻辑进度 end

	Context mContext;
	private BaseUsbDriver mUsbDriver;
	PermissionBroadcastReceiver mPermissionBroadcastReceiver;
	/**
	 * programmer 烧录工具
	 */
	ProgrammerUtils programmerUtils;
	/**
	 * burn 烧录工具
	 */
	BurnFlashUtils burnFlashUtils ;
	/**
	 * 最后一个USB设备
	 */
	UsbDevice mUsbDevice ;
	UsbManager mUsbManager ;
	String mSerialNumber ;
	String mTypeCVersion ;
	String mFirmwareFileUrl ;
	int  mProcess ;
	byte[] copyBootFlag ;
	int mSectorSize = 0 ;
	boolean isAboot = true ;//默认为aboot，根据读取work boot  build info 值之后做正确判断

	/**
	 * 旧模式下，由于需要手动让设备进入cdc模式，故成为手动ota模式。不支持在普通模式下让设备进入ota
	 */
	boolean humanOtaFlag = false ;

	/**
	 * 只能在本工具调用此函数接口
	 * @param humanOtaFlag
	 */
	public void setHumanOta(boolean humanOtaFlag , String mFirmwareFileUrl){
		this.humanOtaFlag = humanOtaFlag ;
		this.mFirmwareFileUrl = mFirmwareFileUrl ;
	}

	public BESImpl(Context context) {
		mContext = context;
		mPermissionBroadcastReceiver = new PermissionBroadcastReceiver();
		mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
	}

	public void init(){
		registerReceiver();
	}

	public void finalize(){
		unregisterReceiver();
		if(mUsbDriver != null){
			mUsbDriver.unregisterReceiver();
			mUsbDriver.disconnect();
		}
	}

	public void disconnect() {
		LOG( "disconnect()") ;
		if(mUsbDriver != null){
			mUsbDriver.disconnect();
			mUsbDriver.unregisterReceiver();
			mUsbDriver = null ;
		}
		resetParm();
	}

	/**
	 * 发起USB设备连接操作，外部连接是不给连接CDC设备操作的。OTA流程始终走正常模式触发后进入CDC设备模式再升级
	 * 若处于CDC模式将返回错误码给到调用层；
	 */
	public int connect(int fd){
		LOG("connect()");
		if(mUsbDevice == null || mUsbManager == null){
			LOG("connect STATUS_NO_DEVICE");
			if(onOtaCallBack != null){
				onOtaCallBack.onConnectCallback(CONNECTION_FAILED_WITH_NO_DEVICE);
			}
			return  CONNECTION_FAILED_WITH_NO_DEVICE ;
		}
		if(mUsbDevice.getDeviceClass() == USBContants.USB_TYPE_CDC){
			LOG("connect STATUS_IN_CEC_MODE");
			if(onOtaCallBack != null){
				onOtaCallBack.onConnectCallback(CONNECTION_FAILED_IN_CDC_MODE);
			}
			return  CONNECTION_FAILED_IN_CDC_MODE ;
		}
		return  connectInside() ;
	}

	/**
	 * 执行烧录动作
	 * @param firmwareFileUrl
	 * @param option
	 * @return
	 */
	public int download(String firmwareFileUrl , int option){
		if(mUsbManager == null || mUsbDevice == null || mUsbDriver == null){
			LOG("download DOWNLOAD_NO_DEVICE_CONNECTED");
			return DOWNLOAD_NO_DEVICE_CONNECTED ;
		}
		if(mUsbDevice.getDeviceClass() == USBContants.USB_TYPE_CDC){
			LOG("download DOWN_IN_CDC_DEVICE");
			return DOWNLOAD_IN_CDC_DEVICE ;
		}
		int initFileRet = initFile(firmwareFileUrl) ;
		LOG("download initFileRet "+ initFileRet);
		if(initFileRet != DOWNLAOD_READY){
			return initFileRet ;
		}
		mOtaStatus = OTA_START ;
		boolean ret =  mUsbDriver.startOta();
		LOG("download  mUsbDriver.startOta() ret "+ ret);
		if(!ret){
			otaHandler.obtainMessage(OTA_OTHER_ERROR , "DOWN_ONLY_SUPPORT_AUTO_OTA").sendToTarget();
		}
		mFirmwareFileUrl = firmwareFileUrl ;
		return  ret ? DOWNLAOD_READY : DOWNLOAD_ONLY_SUPPORT_AUTO_OTA ;
	}

	/**
	 * 获取设备VENDOR ID
	 * @return
	 */
	public int getVendorID(){
		if(mUsbDevice != null)
			return  mUsbDevice.getVendorId() ;
		return  0 ;
	}

	/**
	 * 获取设备PRODUCT ID
	 * @return
	 */
	public int getProductID(){
		if(mUsbDevice != null)
			return  mUsbDevice.getProductId() ;
		return 0 ;
	}

	/**
	 * 获取厂商名称
	 * @return
	 */
	public String getManufacturerString(){
		if(mUsbDevice != null)
			return  mUsbDevice.getManufacturerName() ;
		return "" ;
	}

	/**
	 * 获取产品名称
	 * @return
	 */
	public String getProductString(){
		if(mUsbDevice != null)
			return  mUsbDevice.getProductName() ;
		return  "" ;
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
			if(otafileVersion.equals(mTypeCVersion)||otafileVersion.contains(mTypeCVersion)){
				return false ;
			}else if(mTypeCVersion != null && mTypeCVersion.length() > 1){
				String endVersionWithDevice = mTypeCVersion.substring(mTypeCVersion.length() -1 ,mTypeCVersion.length());
				String endVersionWithUpdateFile = otafileVersion.substring(otafileVersion.length() -1 , otafileVersion.length());
				if(endVersionWithDevice != null && endVersionWithUpdateFile != null
						&& endVersionWithDevice.equals(endVersionWithUpdateFile)){
					return  false;
				}
			}
			return true;
		}
		return  false ;
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

	public int getUsbType(){
		if(mUsbDevice != null){
			return  mUsbDevice.getDeviceClass() ;
		}
		return -1 ;
	}

	public void openLog(boolean isNeedOpen){
		if(isNeedOpen){
			LogUtils.InitLogUtils(true);
		}else{
			LogUtils.InitLogUtils(false);
		}
	}

	private void resetParm(){
		mSerialNumber = null ;
		mTypeCVersion = null ;
		copyBootFlag = null ;
		mProcess = 0 ;
	}

	void progress(int progress){
		mProcess = progress ;
		if(onOtaCallBack != null){
			onOtaCallBack.onOtaProgress(progress);
		}
	}

	Handler otaHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			mOtaStatus = msg.what ;
			switch (msg.what) {
				case OTA_START:
					LOG("OTA_START");
					progress(0);
					break;
				case OTA_FIRST_HANDSHAKE_START:
					progress(2);
					LOG("OTA_FIRST_HANDSHAKE_START");
					byte req = (byte) msg.obj;
					UsbMessage handShark = MessageFactory.getHandShakeCmdResponse(req);
					mUsbDriver.sendMeesage(handShark);
					if(onOtaCallBack != null){
						onOtaCallBack.onOtaStart();
					}
					break;
				case OTA_FIRST_HANDSHAKE_FAILED:
					LOG("OTA_FIRST_HANDSHAKE_FAILED");
					otaHandler.sendEmptyMessage(OTA_IDLE);
					if(onOtaCallBack != null){
						onOtaCallBack.onOtaError(DOWNLOAD_HANDSHAKE_FAILED , "");
					}
					break;
				case OTA_FIRST_HANDSHAKE_SUCCESSFUL:
					progress(5);
					LOG("OTA_FIRST_HANDSHAKE_SUCCESSFUL");
					programmerUtils = new ProgrammerUtils();
					programmerUtils.initProgrammer(mContext);
					otaHandler.sendEmptyMessage(OTA_START_DOWNLOAD_PROGRAMMER);
					break;
				case OTA_START_DOWNLOAD_PROGRAMMER:
					progress(8);
					LOG("OTA_START_DOWNLOAD_PROGRAMMER");
					UsbMessage programmerInfo = programmerUtils.prepareProgrammer();
					if (programmerInfo != null) {
						mUsbDriver.sendMeesage(programmerInfo);
					} else {
						otaHandler.obtainMessage(OTA_DOWNLOAD_PROGRAMMER_FAILED , "info message is null").sendToTarget();
					}
					break;
				case OTA_START_DOWNLOAD_PROGRAMMER_BIN:
					progress(10);
					LOG("OTA_START_DOWNLOAD_PROGRAMMER_BIN");
					UsbMessage programmerBin = programmerUtils.sendProgrammerBin();
					if (programmerBin != null) {
						mUsbDriver.sendMeesage(programmerBin);
					} else {
						otaHandler.obtainMessage(OTA_DOWNLOAD_PROGRAMMER_FAILED , "bin message is null").sendToTarget();
					}
					break;
				case OTA_DOWNLOAD_PROGRAMMER_FAILED:
					String errorMsg = (String) msg.obj;
					LOG("OTA_DOWNLOAD_PROGRAMMER_FAILED errorMsg = "+errorMsg);
					otaHandler.sendEmptyMessage(OTA_IDLE);
					if(onOtaCallBack != null){
						onOtaCallBack.onOtaError(DOWNLOAD_PROGRAMMER_INFO_FAILED , errorMsg);
					}
					break;
				case OTA_DOWNLOAD_PROGRAMMER_SUCCESSFUL:
					progress(15);
					LOG("OTA_DOWNLOAD_PROGRAMMER_SUCCESSFUL");
					otaHandler.sendEmptyMessage(OTA_RUN_PROGRAMMER);
					break;
				case OTA_RUN_PROGRAMMER:
					LOG("OTA_RUN_PROGRAMMER");
					UsbMessage runUseMessage = programmerUtils.sendRunProgrammer();
					mUsbDriver.sendMeesage(runUseMessage);
					break;
				case OTA_RUN_PROGRAMMER_SUCCESSFUL:
					progress(20);
					LOG("OTA_RUN_PROGRAMMER_SUCCESSFUL");
					otaHandler.sendEmptyMessage(OTA_WAIT_RAM_RUN);
					break;
				case OTA_RUN_PROGRAMMER_FAILED:
					String runProgrammerErrorMsg = (String) msg.obj;
					LOG("OTA_RUN_PROGRAMMER_FAILED errorMsg = "+runProgrammerErrorMsg);
					otaHandler.sendEmptyMessage(OTA_IDLE);
					if(onOtaCallBack != null){
						onOtaCallBack.onOtaError(DOWNLOAD_RUN_PROGRAMMER_FAILED , runProgrammerErrorMsg);
					}
					break;
				case OTA_WAIT_RAM_RUN:
					LOG("OTA_WAIT_RAM_RUN");
					break;
				case OTA_READ_BOOT_FLAG:
					progress(25);
					UsbMessage readBootMsg = MessageFactory.getReadBootFlagMsg();
					mUsbDriver.sendMeesage(readBootMsg);
					break;
				case OTA_ERASE_BAT_BOOT_FLAG:
					progress(30);
					UsbMessage eraseBatBootFlag = MessageFactory.getEraseBatBootFlagMsg( REQ_ERASE_BAT_BOOT_FLAG);
					mUsbDriver.sendMeesage(eraseBatBootFlag);
					break;
				case OTA_COPY_TO_BAT_BOOT_FLAG:
					progress(35);
					UsbMessage writeBatBootFlag = MessageFactory.getWriteBatBootFlagMsg(copyBootFlag , REQ_WRITE_BOOT_FLAG_TO_BAT_ADDR);
					if(writeBatBootFlag != null){
						mUsbDriver.sendMeesage(writeBatBootFlag);
					}else{
						otaHandler.obtainMessage(OTA_OTHER_ERROR, "writeBatBootFlag is null").sendToTarget();
					}
					break;
				case OTA_START_DOWNLOAD_BURN:
					progress(40);
					LOG("OTA_START_DOWNLOAD_BURN");
					burnFlashUtils = new BurnFlashUtils();
					burnFlashUtils.initProgrammer(mFirmwareFileUrl , mSectorSize);
					UsbMessage burnInfo = burnFlashUtils.prepareBurn();
					if (burnInfo != null) {
						mUsbDriver.sendMeesage(burnInfo);
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
							mUsbDriver.sendMeesage(burnBinInfo);
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
					otaHandler.sendEmptyMessage(OTA_IDLE);
					if(onOtaCallBack != null){
						onOtaCallBack.onOtaError(DOWNLOAD_BURN_INFO_FAILED , burnErrorMsg);
					}
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
					mUsbDriver.sendMeesage(writeMagic);
					break;
				case OTA_BURN_ERASE_BOOT_FLAG:
					progress(90);
					LOG("OTA_BURN_ERASE_FLAG_SECTOR ");
					UsbMessage eraseBootFlag = MessageFactory.getEraseBootFlagMsg(REQ_ERASE_BOOT_FLAG);
					mUsbDriver.sendMeesage(eraseBootFlag);
					break;
				case OTA_BURN_WRITE_BOOT_FLAG:
					progress(95);
					LOG("OTA_BURN_WRITE_FLAG_SECTOR ");
					UsbMessage writeSectorFlag = MessageFactory.getWriteBootFlagMsg(isAboot , REQ_WRITE_BOOT_FALG);
					mUsbDriver.sendMeesage(writeSectorFlag);
					break;
				case OTA_SET_BOOT_MODE:
					progress(98);
					LOG("OTA_SET_BOOT_MODE ");
					UsbMessage setBootModeMsg = MessageFactory.getSetModeMsg(REQ_SET_BOOT_MODE);
					mUsbDriver.sendMeesage(setBootModeMsg);
					break;
				case OTA_REBOOT_FLASH:
					progress(99);
					LOG("OTA_REBOOT_FLASH ");
					UsbMessage rebootMsg = MessageFactory.getSystemReBootMsg(REQ_REBOOT_SYSTEM);
					mUsbDriver.sendMeesage(rebootMsg);
					break;
				case OTA_DONE:
					progress(100);
					LOG("OTA_DONE ");
					otaHandler.sendEmptyMessage(OTA_IDLE);
					if(onOtaCallBack != null){
						onOtaCallBack.onOtaDone();
					}
					break;
				case OTA_OTHER_ERROR:
					LOG("OTA_OTHER_ERROR ");
					String otherError = (String) msg.obj;
					LOG("OTA_OTHER_ERROR errorMsg = "+otherError);
					otaHandler.sendEmptyMessage(OTA_IDLE);
					if(onOtaCallBack != null){
						onOtaCallBack.onOtaError(DOWNLOAD_OTHER_ERROR , otherError);
					}
					break;
				case OTA_IDLE:
					LOG("OTA_IDLE ");
					break;
				default:
			}
		}
	};

	@Override
	public void onConnectionStateChanged(int state) {
		if(state == IUsbCallback.USB_CONNECTED){
			if(onOtaCallBack != null){
				mConnectionSatus = CONNECTION_CONNECTED ;
				onOtaCallBack.onConnectCallback(CONNECTION_CONNECTED);
			}
		}else if(state == IUsbCallback.USB_NO_PERMISSION){
			mConnectionSatus = CONNECTION_IDLE;
			if(onOtaCallBack != null){
				onOtaCallBack.onConnectCallback(CONNECTION_FAILED_NO_PREMISSION);
			}
			if(mUsbDriver instanceof BesCdcDriver){
				otaHandler.sendEmptyMessage(OTA_IDLE);
				if(onOtaCallBack != null){
					onOtaCallBack.onOtaError(DOWNLOAD_NO_PERMISSION , "no permission");
				}
			}
		}else {
			mConnectionSatus = CONNECTION_IDLE;
			if(onOtaCallBack != null){
				onOtaCallBack.onConnectCallback(CONNECTION_DISCONNECTED);
			}
		}
	}

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

	@Override
	public void onVersionReceive(String version) {
		mTypeCVersion = version ;
	}

	@Override
	public void onSerialNumberReceive(String serialNumber) {
		mSerialNumber = serialNumber ;
	}

	/**
	 * 自动检测
	 * @return
	 */
	private int connectInside() {
		LOG("connectInside()") ;
		if(null == mUsbManager || mUsbDevice == null) {
			LOG("Get UsbManager fail! null == usbManager") ;
			if(onOtaCallBack != null){
				onOtaCallBack.onConnectCallback(CONNECTION_FAILED_WITH_NO_DEVICE);
			}
			return CONNECTION_FAILED_WITH_NO_DEVICE;
		} else {
			int vendorId = mUsbDevice.getVendorId();
			int productId = mUsbDevice.getProductId();
			LOG("DeviceInfo - vendorId:" + vendorId + ", productId:" + productId) ;
			if(vendorId == USBContants.BES_CDC_VENDER_ID && productId == USBContants.BES_CDC_PRODUCT_ID) {
				LOG("Found matching device - vendorId:" + vendorId + ", productId:" + productId) ;
				if(onOtaCallBack != null){
					onOtaCallBack.onConnectCallback(CONNECTION_CDC_DEVICE_GOING);
				}
				if(mUsbDriver != null){
					mUsbDriver.unregisterReceiver();
					mUsbDriver = null ;
				}
				mUsbDriver = new BesCdcDriver(mContext, mUsbManager, this);
				mUsbDriver.registerReceiver();
				mUsbDriver.connect(mUsbDevice);
				return CONNECTION_CDC_DEVICE_GOING;
			}else if(vendorId == USBContants.BES_USB_C_HEADSET_VENDER_ID && productId == USBContants.BES_USB_C_HEADSET_PRODUCT_ID){
				LOG("Found matching device - vendorId:" + vendorId + ", productId:" + productId) ;
				if(onOtaCallBack != null){
					onOtaCallBack.onConnectCallback(CONNECTION_AUDIO_DEVICE_GOING);
				}
				if(mUsbDriver != null){
					mUsbDriver.unregisterReceiver();
					mUsbDriver = null ;
				}
				mUsbDriver = new BesAudioDriver(mContext, mUsbManager, this);
				mUsbDriver.registerReceiver();
				mUsbDriver.connect(mUsbDevice);
				return CONNECTION_AUDIO_DEVICE_GOING ;
			}else {
				LOG("NO Found matching device - vendorId:" + vendorId + ", productId:" + productId);
				if (onOtaCallBack != null) {
					onOtaCallBack.onConnectCallback(CONNECTION_FAILED_WITH_NO_BES_DEVICE);
				}
				return CONNECTION_FAILED_WITH_NO_BES_DEVICE;
			}
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
		if(otafileVersion != null && otafileVersion.length() > 1){
			if(otafileVersion.equals(mTypeCVersion)||otafileVersion.contains(mTypeCVersion)){
				return DOWNLOAD_VERSION_IS_SAME ;
			}else if(mTypeCVersion != null && mTypeCVersion.length() > 1){
				String endVersionWithDevice = mTypeCVersion.substring(mTypeCVersion.length() -1 ,mTypeCVersion.length());
				String endVersionWithUpdateFile = otafileVersion.substring(otafileVersion.length() -1 , otafileVersion.length());
				if(endVersionWithDevice != null && endVersionWithUpdateFile != null
						&& endVersionWithDevice.equals(endVersionWithUpdateFile)){
					return  DOWNLOAD_WRONG_BOOT_TYPE_FILE;
				}
			}
			return DOWNLAOD_READY;
		}
		return DOWNLOAD_READ_FILE_VERSION_FIALED ;
	}

	private String readOtafileVersion(String firmwareFileUrl){
		if(!FileUtils.isFileExist(firmwareFileUrl)){
			return  null ;
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

	public boolean isHadUsbDevice(){
		UsbManager mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
		if(null == mUsbManager) {
			LOG("Get UsbManager fail! null == usbManager") ;
		} else {
			HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
			LOG("deviceMap.size(): "+ deviceMap.size()) ;
			if(!deviceMap.isEmpty()) {
				Iterator<UsbDevice> iterator = deviceMap.values().iterator();
				while(iterator.hasNext()) {
					mUsbDevice = iterator.next();
					LOG(" find usb device name =  "+mUsbDevice.getProductName()) ;
				}
				return true;
			} else {
				return false ;
			}
		}
		return  false ;
	}

 	public boolean isConnected(){
		return  mConnectionSatus == CONNECTION_CONNECTED ;
	}

	public boolean isIdle(){
		return mOtaStatus == OTA_IDLE;
	}

	@Deprecated
	private void sendMessage(byte[] msg) {
		LOG("ready to sendMessage(byte[] msg) "+ ArrayUtil.toHex(msg)) ;
		if(mUsbDriver != null){
			mUsbDriver.sendMessage(msg);
		}
	}

	private void sendMessage(UsbMessage msg) {
		LOG("ready to sendMessage(byte[] msg) "+ ArrayUtil.toHex(msg.getBytes())) ;
		if(mUsbDriver != null){
			mUsbDriver.sendMeesage(msg);
		}
	}

	/**
	 * 注册usb广播，在进入ota页面时后进行注册
	 */
	private void registerReceiver() {
		if(mContext != null && mPermissionBroadcastReceiver != null){
			try{
				unregisterReceiver();
			}catch (Exception e){

			}finally {
				IntentFilter filter = new IntentFilter();
				filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
				filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
				mContext.registerReceiver(mPermissionBroadcastReceiver, filter);
			}
		}
	}

	/**
	 *  注销usb广播，在推出ota页面后进行注销,此部分集成在 destroy
	 */
	private void unregisterReceiver() {
		mContext.unregisterReceiver(mPermissionBroadcastReceiver);
	}

	class PermissionBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent != null && intent.getAction() != null){
				String action = intent.getAction();
				if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
					LOG("UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)");
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					mUsbDevice = device;
					if(mUsbDevice != null){
						LOG("ACTION_USB_DEVICE_ATTACHED "+mUsbDevice.getProductName());
					}
					if(onOtaCallBack != null){
						onOtaCallBack.onConnectCallback(CONNECTION_ATTACHED);
					}
					if(mOtaStatus == OTA_START || (humanOtaFlag && device.getDeviceClass() == USBContants.USB_TYPE_CDC)){
						if(humanOtaFlag && device.getDeviceClass() == USBContants.USB_TYPE_CDC){
							if(mFirmwareFileUrl != null && mFirmwareFileUrl.contains("bin") && FileUtils.isFileExist(mFirmwareFileUrl)){
								LOG("mFirmwareFileUrl != null && mFirmwareFileUrl.contains(\"bin\") && FileUtils.isFileExist(mFirmwareFileUrl) IS TRUE");
								connectInside();
							}else{
								otaHandler.sendEmptyMessage(OTA_IDLE);
								if(onOtaCallBack != null){
									onOtaCallBack.onOtaError(DOWNLOAD_FILE_NOT_FOUND ,"can not find file");
								}
							}
						}else{
							LOG("mOtaStatus == OTA_START IS TRUE");
							connectInside();
						}
					}
				}else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
					disconnect();
					LOG("UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)");
					if(onOtaCallBack != null){
						onOtaCallBack.onConnectCallback(CONNECTION_DETACHED);
					}
				}
			}
		}
	}

	public interface OnOtaCallBack{
		void onOtaProgress(int progress);
		void onConnectCallback(int status);
		void onOtaStart();
		void onOtaDone();
		void onOtaError(int msgCode, String errorMsg);
	}

	OnOtaCallBack onOtaCallBack ;
	public void setOnDataCallBack(OnOtaCallBack callBack){
		onOtaCallBack = callBack ;
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
