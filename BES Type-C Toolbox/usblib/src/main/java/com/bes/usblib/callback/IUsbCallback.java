package com.bes.usblib.callback;


public interface IUsbCallback {

	/**
	 * 接收到的数据，仅支持端点数据，不支持usb控制通讯
	 *
	 * @param datas
	 */
	void onDataReceive(byte[] datas);

}
