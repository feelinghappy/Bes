package com.bes.usblib.utils;

import android.text.format.DateFormat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Log工具类，打印并保存本地文件等操作
 */
public class LogUtils
{
	static boolean E_DEBUG = true;

	private static void checkBetaOrLine(){
		E_DEBUG = true;
	}
	
	/**
	 * 初始化程序log打印规则
	 * @param e_log : true 使能 e 等级log ; false 关闭 e 等级log
	 */
	public static void InitLogUtils( boolean e_log)
	{
		E_DEBUG = e_log ;
	}

	
	public static void e(String tag, String msg)
	{
		checkBetaOrLine();
		if (E_DEBUG && msg != null)
		{
			android.util.Log.e(tag, msg);
		}
	}

	/**
	 *
	 * @param tag
	 * @param logMsg
	 */
	public static void writeComm(String tag , String fileName ,String logMsg)
	{
		checkBetaOrLine();
		if (true)
		{
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())).append("\t")
					.append("< ").append(tag).append(" >").append("\t")
					.append(logMsg).append("\t").append("\n");
			e(tag, stringBuilder.toString());
			FileUtils.writeTOfileAndActiveClear(fileName,stringBuilder.toString());
		}
	}

}
