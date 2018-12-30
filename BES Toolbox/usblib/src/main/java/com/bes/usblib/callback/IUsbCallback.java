package com.bes.usblib.callback;


public interface IUsbCallback {

	public final static int USB_CONNECTED = 1 ;
	public final static int USB_DISCONNECTED = 2 ;
	public final static int USB_NO_PERMISSION = 3 ;

	/**
	 * USB连接回调
	 * @param state
	 */
	void onConnectionStateChanged(int state) ;

	/**
	 * 接收到的数据，仅支持端点数据，不支持usb控制通讯
	 * @param datas
	 */
	void onDataReceive(byte[] datas) ;
	
	/**
	 * 其中包含三部分信息：版本号、厂家标识以及厂家自定义信息。具体格式定义如下，各部分信息之间用下划线“_”连接。
	 [version]_[vendor-id][project-id][manufacture-id]_[vendor-specific-info]
	 	[version]：长度不固定，通常不会超过5字节；数字字符串，如”0.12”、”1.1”等；
	 	[vendor-id]：2字节；BES对不同耳机厂商的编号；
	 	[project-id]：2字节；BES对同一耳机厂家不同项目的编号；
	 	[manufacturer-id]：2字节；BES对同一耳机厂家不同代工厂的编号；
	 	[vendor-specific-info]：长度不固定，具体格式根据具体产品定义；
	 目前基于BES方案的Type-C耳机信息如表3-1。
	 表3-1 BES Type-C耳机信息
	 VID*	PID*	MID*	UVID*	 UPID*		 	   UPN*					VSI*
	 -----------------------------------------------------------------------------------
	 01		01		01		0x12d1	 0x3a07·	BBIITT USB-C HEADSET*	[e/w/a][a/b]*
	 				02
	 				03
	 -----------------------------------------------------------------------------------
	 02		01		01		0x2717	 0x3801				em006				 /
	 -----------------------------------------------------------------------------------
	 *说明：
	 	VID表示[vendor-id]，PID表示[product-id]，MID表示[manufacturer-id]，UVID表示USB Vendor-ID，UPID表示USB Product ID，UPN表示USB Product Name，VSI表示[vendor-specific-info]；
	 	BBIITT USB-C HEADSET，注意包含空格。
	 	[e/w/a][a/b]，表示2个字节的vendor-specific-info
	 第一个字节为’e’、’w’或’a’，分别表示欧洲版、世界版和无区分版；
	 第二个字节为’a’或’b’，对应于AB boot架构的work boot a和work boot b；
	 * @param version 版本格式
	 */
	void onVersionReceive(String version) ;

	/**
	 * 序列号返回 由各厂家自行定义。
	 * @param serialNumber
	 */
	void onSerialNumberReceive(String serialNumber);
}
