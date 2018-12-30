package com.iir_eq.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.iir_eq.model.EQConfig;
import com.iir_eq.model.IIRParam;
import com.iir_eq.util.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhaowanxing on 2017/4/23.
 */

public class DataHelper {

    public static final String FILE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "eq_config";
    public static final String FILE_NAME = "eq_config.dat";

    private DbHelper mDbHelper;

    private static volatile DataHelper sDataHelper;

    private DataHelper(Context context) {
        mDbHelper = new DbHelper(context);
    }

    public static DataHelper getDataHelper(Context context) {
        if (sDataHelper == null) {
            synchronized (DataHelper.class) {
                if (sDataHelper == null) {
                    sDataHelper = new DataHelper(context);
                }
            }
        }
        return sDataHelper;
    }

    public boolean saveFile(EQConfig config) {
        File dir = new File(FILE_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(dir, FILE_NAME);
        Logger.e("Helper", "saveFile " + file.getAbsolutePath());
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(file, true));
            writer.println("IIR_CFG_T audio_eq_iir_cfg = {");
            writer.println("    .leftGain = " + config.getLeftGain() + ",");
            writer.println("    .rightGain = " + config.getRightGain() + ",");
            writer.println("    .num = " + config.getNumOfValidParam() + ",");
            writer.println("    .param = {");
            for (int i = 0; i < EQConfig.PARAM_NUM; i++) {
                if (config.getChks()[i]) {
                    IIRParam param = config.getParams()[i];
                    writer.println("        {" + param.getGain() + ",   " + param.getFrequency() + ",   " + param.getQ() + "},");
                }
            }
            writer.println("    }");
            writer.println("};");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (writer != null)
                writer.close();
        }
        return true;
    }

    public boolean saveConfig(String name, EQConfig config) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_NAME, name);

        values.put(DbHelper.COLUMN_LEFT_GAIN, config.getLeftGain());
        values.put(DbHelper.COLUMN_RIGHT_GAIN, config.getRightGain());

        values.put(DbHelper.COLUMN_CHECK_1, config.getChks()[0] ? 1 : 0);
        values.put(DbHelper.COLUMN_CHECK_2, config.getChks()[1] ? 1 : 0);
        values.put(DbHelper.COLUMN_CHECK_3, config.getChks()[2] ? 1 : 0);
        values.put(DbHelper.COLUMN_CHECK_4, config.getChks()[3] ? 1 : 0);
        values.put(DbHelper.COLUMN_CHECK_5, config.getChks()[4] ? 1 : 0);

        values.put(DbHelper.COLUMN_GAIN_1, config.getParams()[0].getGain());
        values.put(DbHelper.COLUMN_GAIN_2, config.getParams()[1].getGain());
        values.put(DbHelper.COLUMN_GAIN_3, config.getParams()[2].getGain());
        values.put(DbHelper.COLUMN_GAIN_4, config.getParams()[3].getGain());
        values.put(DbHelper.COLUMN_GAIN_5, config.getParams()[4].getGain());

        values.put(DbHelper.COLUMN_FREQ_1, config.getParams()[0].getFrequency());
        values.put(DbHelper.COLUMN_FREQ_2, config.getParams()[1].getFrequency());
        values.put(DbHelper.COLUMN_FREQ_3, config.getParams()[2].getFrequency());
        values.put(DbHelper.COLUMN_FREQ_4, config.getParams()[3].getFrequency());
        values.put(DbHelper.COLUMN_FREQ_5, config.getParams()[4].getFrequency());

        values.put(DbHelper.COLUMN_Q_1, config.getParams()[0].getQ());
        values.put(DbHelper.COLUMN_Q_2, config.getParams()[1].getQ());
        values.put(DbHelper.COLUMN_Q_3, config.getParams()[2].getQ());
        values.put(DbHelper.COLUMN_Q_4, config.getParams()[3].getQ());
        values.put(DbHelper.COLUMN_Q_5, config.getParams()[4].getQ());

        return database.insert(DbHelper.TABLE_EQ_CONFIG, null, values) > 0;
    }

    public boolean delete(String name) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        return database.delete(DbHelper.TABLE_EQ_CONFIG, DbHelper.COLUMN_NAME + "=?", new String[]{name}) > 0;
    }

    public void clear() {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        database.execSQL("DELETE FROM " + DbHelper.TABLE_EQ_CONFIG + ";");
    }

    public boolean isExist(String name) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = database.query(DbHelper.TABLE_EQ_CONFIG, new String[]{DbHelper.COLUMN_NAME}, DbHelper.COLUMN_NAME + "=?", new String[]{name}, null, null, null);
        boolean result = false;
        if (cursor != null) {
            result = cursor.moveToNext();
            cursor.close();
        }
        return result;
    }

    public EQConfig getConfigByName(String name) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = database.query(DbHelper.TABLE_EQ_CONFIG, null, DbHelper.COLUMN_NAME + "=?", new String[]{name}, null, null, null);
        EQConfig config = null;

        if (cursor != null) {
            if (cursor.moveToNext()) {
                config = new EQConfig();
                config.setLeftGain(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_LEFT_GAIN)));
                config.setRightGain(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_RIGHT_GAIN)));

                config.getChks()[0] = cursor.getInt(cursor.getColumnIndex(DbHelper.COLUMN_CHECK_1)) == 1;
                config.getChks()[1] = cursor.getInt(cursor.getColumnIndex(DbHelper.COLUMN_CHECK_2)) == 1;
                config.getChks()[2] = cursor.getInt(cursor.getColumnIndex(DbHelper.COLUMN_CHECK_3)) == 1;
                config.getChks()[3] = cursor.getInt(cursor.getColumnIndex(DbHelper.COLUMN_CHECK_4)) == 1;
                config.getChks()[4] = cursor.getInt(cursor.getColumnIndex(DbHelper.COLUMN_CHECK_5)) == 1;

                config.getParams()[0].setGain(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_GAIN_1)));
                config.getParams()[1].setGain(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_GAIN_2)));
                config.getParams()[2].setGain(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_GAIN_3)));
                config.getParams()[3].setGain(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_GAIN_4)));
                config.getParams()[4].setGain(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_GAIN_5)));

                config.getParams()[0].setFrequency(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_FREQ_1)));
                config.getParams()[1].setFrequency(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_FREQ_2)));
                config.getParams()[2].setFrequency(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_FREQ_3)));
                config.getParams()[3].setFrequency(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_FREQ_4)));
                config.getParams()[4].setFrequency(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_FREQ_5)));

                config.getParams()[0].setQ(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_Q_1)));
                config.getParams()[1].setQ(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_Q_2)));
                config.getParams()[2].setQ(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_Q_3)));
                config.getParams()[3].setQ(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_Q_4)));
                config.getParams()[4].setQ(cursor.getFloat(cursor.getColumnIndex(DbHelper.COLUMN_Q_5)));
            }

            cursor.close();
        }
        return config;
    }

    public List<String> getConfigs() {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = database.query(DbHelper.TABLE_EQ_CONFIG, new String[]{DbHelper.COLUMN_NAME}, null, null, null, null, null);
        List<String> configs = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                configs.add(cursor.getString(cursor.getColumnIndex(DbHelper.COLUMN_NAME)));
            }
            cursor.close();
        }
        return configs;
    }
}
