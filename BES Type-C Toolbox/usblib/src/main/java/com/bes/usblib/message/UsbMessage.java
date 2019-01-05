/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bes.usblib.message;


import java.nio.ByteBuffer;

/**
 * <PREFIX> <TYPE> <SEQ> <LEN> <DATA> <CHKSUM> <EXTDATA>
 •	PREFIX——1字节，固定为0xBE
 •	TYPE——1字节，为消息的类型
 •	SEQ——1字节，为消息序列号；如果是接收方回复消息，此序列号与原消息相同
 •	LEN——1字节，为DATA字段数据的长度
 •	DATA——0到255字节数据
 •	CHKSUM——1字节，为消息CHKSUM字段前所有字节的累加和取反
 •	EXTDATA——0到任意字节，为扩展数据，只有个别类型的消息会带有扩展数据，通常本消息或者上一个消息的DATA字段会指明扩展数据的长度以及校验码
 */
public class UsbMessage {
	
	private static final String TAG = UsbMessage.class.getSimpleName();
	private final byte HEAD_BYTE = (byte)0xBE ;
	private final byte HEAD_COMM_LEN = 4 ; //include <PREFIX> <TYPE> <SEQ> <LEN>

    private ByteBuffer mReceiveBuffer;
    private  byte[] extDatas;
	private  byte cmdType = 0 ;
	private  byte len = 0 ;
	private byte seqNumber = 0 ;
	private byte[] datas ;
	private byte chkSum ;
    private ByteBuffer dataBuffer ; //不包含拓展字段

	public UsbMessage(){}

	private byte[] command ;
	public void setCommand(byte[] datas){
		this.command = datas ;
	}

	public byte[] getCommand(){
		return command ;
	}


	public byte getSeq(){
		return  seqNumber ;
	}

    public UsbMessage(byte cmd , byte seq , byte[] datas) {
        cmdType = cmd ;
		seqNumber = seq ;
		this.datas = datas ;
		if(datas != null){
			len = (byte)datas.length ;
		}
		dataBuffer = ByteBuffer.allocate(HEAD_COMM_LEN+len);
		dataBuffer.put(HEAD_BYTE);
		dataBuffer.put(cmdType);
		dataBuffer.put(seqNumber);
		dataBuffer.put(len);
		if(this.datas !=  null && this.datas.length > 0){
			dataBuffer.put(this.datas);
		}
    }

    public void setExtData(byte[] datas){
		this.extDatas = datas ;
	}

	public byte[] getBytes(){
		int extLen = 0 ;
		byte[] datas = null;
		if(extDatas != null){
			extLen = extDatas.length ;
		}
		if(dataBuffer != null){
			datas = dataBuffer.array();
		}
		chkSum = calcuChkSum();
		ByteBuffer msgBuffer = ByteBuffer.allocate(datas.length + 1 + extLen);
		msgBuffer.put(datas);
		msgBuffer.put(chkSum);
		if(extDatas != null){
			msgBuffer.put(extDatas);
		}
		return msgBuffer.array() ;
	}

	public  byte calcuChkSum(){
		if(dataBuffer != null){
			byte[] temp = dataBuffer.array() ;
			byte sum = 0 ;
			for (int i = 0 ; i < temp.length ; i++){
				sum += temp[i];
			}
			sum = (byte) ~sum;
			return  sum ;
		}
		return  0 ;
	}

    
}