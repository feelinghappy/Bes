package com.iir_eq.util;

import java.util.zip.CRC32;

/**
 * Created by zhaowanxing on 2017/4/23.
 */

public class ArrayUtil {
    public static byte[] extractBytes(byte[] data, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(data, start, bytes, 0, length);
        return bytes;
    }

    public static boolean isEqual(byte[] array_1, byte[] array_2) {
        if (array_1 == null) {
            return array_2 == null;
        }
        if (array_2 == null) {
            return false;
        }
        if (array_1 == array_2) {
            return true;
        }
        if (array_1.length != array_2.length) {
            return false;
        }
        for (int i = 0; i < array_1.length; i++) {
            if (array_1[i] != array_2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean contains(byte[] parent, byte[] child) {
        if (parent == null) {
            return child == null;
        }
        if (child == null || child.length == 0) {
            return true;
        }
        if (parent == child) {
            return true;
        }
        return new String(parent).contains(new String(child));
    }

    public static long crc32(byte[] data, int offset, int length) {
        CRC32 crc32 = new CRC32();
        crc32.update(data, offset, length);
        return crc32.getValue();
    }

    public static byte checkSum(byte[] data, int len) {
        byte sum = (byte) 0;
        for (int i = 0; i < len; i++) {
            sum ^= data[i];
        }
        return sum;
    }

    public static String toHex(byte[] data) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buffer.append(String.format("%02x", data[i])).append(",");
        }
        return buffer.toString();
    }

    public static String toASCII(byte[] data) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buffer.append((char)data[i]);
        }
        return buffer.toString();
    }


    public static boolean startsWith(byte[] data, byte[] param) {
        if (data == null) {
            return param == null;
        }
        if (param == null) {
            return true;
        }
        if (data.length < param.length) {
            return false;
        }
        for (int i = 0; i < param.length; i++) {
            if (data[i] != param[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将toHex()16进制字符串转换为byte[]
     *
     * @param str
     * @return
     * add by fanxiaoli 20190102
     */
    public static byte[] toBytes(String str) {
        if(str == null || str.trim().equals("")) {
            return new byte[0];
        }
        str = str.replace(",","");//与toHex函数对应
        if(str == null || str.trim().equals("")) {
            return new byte[0];
        }
        byte[] bytes = new byte[str.length() / 2];
        for(int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }



    /**
     * 以小端模式将byte[]转成int
     * add by fanxiaoli 20190102
     */
    public static int bytesToIntLittle(byte[] src) {
        int value = 0;
        value = (int) ((src[0] & 0xFF)
                | ((src[1] << 8) & 0xFF00)
                | ((src[2] << 16) & 0xFF0000)
                | ((src[3] << 24) & 0xFF000000));
        return value;


    }

    /*获取如1.1.1.1格式版本号
    * add by fanxiaoli 20190114
    */

    public static String  bytesToVersion(byte[] src) {
        String version = "";
        int[] version_int = new int[4];
        if(src.length==4) {
            for (int i = 0;i<src.length;i++) {
                version_int[i] = (int) (src[i] & 0xff);
                version = version + (String.valueOf(version_int[i]));
                if (i < 3) {
                    version = version + ".";
                }
            }
        }
        return version;
    }


}
