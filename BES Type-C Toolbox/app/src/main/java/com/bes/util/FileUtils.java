package com.bes.util;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class FileUtils
{
	private static String mPath	= Environment.getExternalStorageDirectory() + "/";
	public static String USB_OTA_FILE = "usb_ota.txt";

	
	/**
	 * 判断指定路径文件是否存在，如果不存在直接生成一个新的文件
	 * @param path ： 文件指定路径
	 */
	public static void isExist(String path)
	{
		File file = new File(path);
		// 判断文件夹是否存在,如果不存在则创建文件夹
		if (!file.exists())
		{
			synchronized (FileUtils.class)
			{
				file.mkdirs();
			}
		}
	}

	public static boolean isFileExist(String path)
	{
		File file = new File(path);
		return file.exists();
	}
	
	/**
	 * 获取本应用的数据存储路径
	 * @return
	 */
	public static String getFolderPath()
	{
		String pathString = mPath;
		isExist(pathString);
		pathString += "/";
		return pathString;
	}


	/**
	 * 删除程序本地文件夹中的文件，只需指定文件名称。
	 * @param fileName 如对应apk名称，对应软件包名称等等
	 */
	public static void deleteFile(String fileName)
	{
		File file = new File(getFolderPath() + fileName);
		if (file.exists())
		{
			file.delete();
		}
	}


	public static void writeTOfileAndActiveClear(String filename, String context)
	{
		String path = getFolderPath()+"BES/";
		isExist(path);
		path = path+"LogData/";
		isExist(path);
		File file = new File(path + filename);
		try
		{
			if (!file.exists())
			{
				file.createNewFile();
			}
			FileInputStream fis = new FileInputStream(file);
			long size = fis.available();
			fis.close();
			/**
			 * 当文件大小大于80MByte时，主动删除
			 */
			if (size >= 80000000)
			{
				file.delete();
				return;
			}

			FileOutputStream stream = new FileOutputStream(file, true);
			String temp = context + "\n";
			byte[] buf = temp.getBytes();
			stream.write(buf);
			stream.close();

		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
