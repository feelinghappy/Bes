package com.iir_eq.util;

import android.text.format.DateFormat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Log工具类，具备打印规则控制，打印并保存本地文件等操作
 */
public class LogUtils
{
	static String PROJECT_OTA = "ota.txt" ;
	static String PROJECT_LOG = "Log.txt" ;
	static String PROJECT_HW_CLOUD_LOG = "HwCloudLog.txt" ;
	static boolean V_DEBUG = true;
	static boolean I_DEBUG = true;
	static boolean E_DEBUG = true;
	static boolean W_DEBUG = true;

	private static void checkBetaOrLine(){
		V_DEBUG = true;
		I_DEBUG = true;
		E_DEBUG = true;
		W_DEBUG = true;

	}
	
	/**
	 * 初始化程序log打印规则
	 * @param v_log : true 使能 v 等级log ; false 关闭 v 等级log
	 * @param i_log : true 使能 i 等级log ; false 关闭 i 等级log
	 * @param e_log : true 使能 e 等级log ; false 关闭 e 等级log
	 */
	public static void InitLogUtils(boolean w_log ,boolean v_log, boolean i_log, boolean e_log)
	{
		V_DEBUG = v_log ;
		I_DEBUG = i_log ;
		E_DEBUG = e_log ;
		W_DEBUG = w_log ; 
	}
	
	public static void v(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.v(tag, msg);
		}
			
	}
	
	public static void v_write(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.v(tag, msg);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
						DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "V<" + tag + ">---" + msg);
        }	
	}

	public static void v(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.v(tag, msg, t);
		}
			
    }
	
	public static void v_write(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.v(tag, msg, t);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
					DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "Vt<" + tag + ">---" + msg);
		}
			
	}
	public static void w(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.w(tag, msg);
		}
			
	}
	
	public static void w_write(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.w(tag, msg);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
						DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "V<" + tag + ">---" + msg);
        }	
	}

	public static void w(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.w(tag, msg, t);
		}
			
    }
	
	public static void w_write(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.w(tag, msg, t);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
					DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "Vt<" + tag + ">---" + msg);
		}
			
	}
	
	public static void i(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.i(tag, msg);
		}
			
	}
	
	public static void i_write(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.v(tag, msg);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
						DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "V<" + tag + ">---" + msg);
        }	
	}

	public static void i(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.i(tag, msg, t);
		}
			
    }
	
	public static void i_write(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.i(tag, msg, t);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
					DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "Vt<" + tag + ">---" + msg);
		}
			
	}
	
	public static void e(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.e(tag, msg);
		}
			
	}
	
	public static void e_write(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.e(tag, msg);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
						DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "V<" + tag + ">---" + msg);
        }	
	}
	

	/**
	 * 
	 * @param tag
	 * @param logMsg
	 */
	public static void writeForBle(String tag , String logMsg)
	{
		checkBetaOrLine();
		if (V_DEBUG)
		{
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())).append("\t")
			             .append("< ").append(tag).append(" >").append("\t")
			             .append(logMsg).append("\t").append("\n");
			android.util.Log.e(tag, stringBuilder.toString());
			FileUtils.writeTOfileAndActiveClear(FileUtils.BLE_FILE_NAME,stringBuilder.toString());
        }	
	}

	/**
	 *
	 * @param tag
	 * @param logMsg
	 */
	public static void writeForClassicBt(String tag , String logMsg)
	{
		checkBetaOrLine();
		if (V_DEBUG)
		{
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())).append("\t")
					.append("< ").append(tag).append(" >").append("\t")
					.append(logMsg).append("\t").append("\n");
			android.util.Log.e(tag, stringBuilder.toString());
			FileUtils.writeTOfileAndActiveClear(FileUtils.BLE_FILE_NAME,stringBuilder.toString());
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
			android.util.Log.e(tag, stringBuilder.toString());
			FileUtils.writeTOfileAndActiveClear(fileName,stringBuilder.toString());
		}
	}

	/**
	 *
	 * @param tag
	 * @param logMsg
	 */
	public static void writeForOTAStatic(String tag , String logMsg)
	{
		checkBetaOrLine();
		if (V_DEBUG)
		{
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis())).append("\t")
					.append("< ").append(tag).append(" >").append("\t")
					.append(logMsg).append("\t").append("\n");
			android.util.Log.e(tag, stringBuilder.toString());
			FileUtils.writeTOfileAndActiveClear(FileUtils.OTA_STATIC,stringBuilder.toString());
		}
	}

	public static void e(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.e(tag, msg, t);
		}
			
    }
	
	public static void e_write(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.e(tag, msg, t);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
					DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "Vt<" + tag + ">---" + msg);
		}
			
	}
	
	public static void d(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.d(tag, msg);
		}
			
	}
	
	public static void d_write(String tag, String msg)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.d(tag, msg);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
						DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "V<" + tag + ">---" + msg);
        }	
	}

	public static void d(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.d(tag, msg, t);
		}
			
    }
	
	public static void d_write(String tag, String msg, Throwable t)
	{
		checkBetaOrLine();
		if (V_DEBUG && msg != null)
		{
			android.util.Log.d(tag, msg, t);
			FileUtils.writeTOfileAndActiveClear(PROJECT_LOG,
					DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()) + "Vt<" + tag + ">---" + msg);
		}
			
	}
	/**
	 * e 转 String
	 * @param ex
	 * @return
	 */
	public static String exToString(Exception ex){
		Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);      
        Throwable cause = ex.getCause();
        while (cause != null) {      
            cause.printStackTrace(printWriter);      
            cause = cause.getCause();      
        }      
        printWriter.close();      
        String result = writer.toString();
        return result;
	}
}
