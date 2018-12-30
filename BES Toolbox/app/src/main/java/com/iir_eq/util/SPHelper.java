package com.iir_eq.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by zhaowanxing on 2017/6/13.
 */

public class SPHelper {


    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("app_config", Context.MODE_PRIVATE);
    }

    public static void putPreference(Context context, String key, Object value) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Short) {
            int data = ((Short) value).shortValue();
            editor.putInt(key, data);
        } else if (value instanceof Byte) {
            int data = ((Byte) value).byteValue();
            editor.putInt(key, data);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Double) {
            float data = (float) ((Double) value).doubleValue();
            editor.putFloat(key, data);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }
        editor.commit();
    }

    public static Object getPreference(Context context, String key, Object defaultValue) {
        SharedPreferences sp = getSharedPreferences(context);
        if ((defaultValue instanceof Integer) || (defaultValue instanceof Short) || (defaultValue instanceof Byte)) {
            return sp.getInt(key, (Integer) defaultValue);
        }
        if ((defaultValue instanceof Float) || (defaultValue instanceof Double)) {
            return sp.getFloat(key, (Float) defaultValue);
        }
        if (defaultValue instanceof Long) {
            return sp.getLong(key, (Long) defaultValue);
        }
        if (defaultValue instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultValue);
        }
        if (defaultValue instanceof String) {
            return sp.getString(key, (String) defaultValue);
        }
        return null;
    }

    public static void removePreference(Context context, String key) {
        getSharedPreferences(context).edit().remove(key).commit();
    }
}
