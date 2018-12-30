package com.bes.usblib.callback;

import android.hardware.usb.UsbDevice;

import com.bes.usblib.message.UsbMessage;

public abstract class BaseUsbDriver {

	public abstract boolean connect(UsbDevice usbDevice);
	
	public abstract void disconnect();

	public abstract void registerReceiver();

	public abstract void unregisterReceiver();

	public abstract int getUsbType();

	@Deprecated
    public boolean sendMessage(byte[] msg){
		return  false ;
	};

	public abstract boolean sendMeesage(UsbMessage msg);

	public abstract boolean startOta();

}
