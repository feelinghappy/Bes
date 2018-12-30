package com.iir_eq.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by zhaowanxing on 2017/4/23.
 */

public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "eq_config.db";
    private static final int VERSION = 1;

    public static final String TABLE_EQ_CONFIG = "table_eq_config";

    public static final String COLUMN_NAME = "_name";

    public static final String COLUMN_LEFT_GAIN = "_left_gain";
    public static final String COLUMN_RIGHT_GAIN = "_right_gain";

    public static final String COLUMN_CHECK_1 = "_check_1";
    public static final String COLUMN_CHECK_2 = "_check_2";
    public static final String COLUMN_CHECK_3 = "_check_3";
    public static final String COLUMN_CHECK_4 = "_check_4";
    public static final String COLUMN_CHECK_5 = "_check_5";

    public static final String COLUMN_GAIN_1 = "_gain_1";
    public static final String COLUMN_GAIN_2 = "_gain_2";
    public static final String COLUMN_GAIN_3 = "_gain_3";
    public static final String COLUMN_GAIN_4 = "_gain_4";
    public static final String COLUMN_GAIN_5 = "_gain_5";

    public static final String COLUMN_FREQ_1 = "_freq_1";
    public static final String COLUMN_FREQ_2 = "_freq_2";
    public static final String COLUMN_FREQ_3 = "_freq_3";
    public static final String COLUMN_FREQ_4 = "_freq_4";
    public static final String COLUMN_FREQ_5 = "_freq_5";

    public static final String COLUMN_Q_1 = "_q_1";
    public static final String COLUMN_Q_2 = "_q_2";
    public static final String COLUMN_Q_3 = "_q_3";
    public static final String COLUMN_Q_4 = "_q_4";
    public static final String COLUMN_Q_5 = "_q_5";

    public DbHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EQ_CONFIG + " ("
                + COLUMN_NAME + " VARCHAR(20) PRIMARY KEY, "
                + COLUMN_LEFT_GAIN + " FLOAT, "
                + COLUMN_RIGHT_GAIN + " FLOAT, "
                + COLUMN_CHECK_1 + " INTEGER, "
                + COLUMN_GAIN_1 + " FLOAT, "
                + COLUMN_FREQ_1 + " FLOAT, "
                + COLUMN_Q_1 + " FLOAT, "
                + COLUMN_CHECK_2 + " INTEGER, "
                + COLUMN_GAIN_2 + " FLOAT, "
                + COLUMN_FREQ_2 + " FLOAT, "
                + COLUMN_Q_2 + " FLOAT, "
                + COLUMN_CHECK_3 + " INTEGER, "
                + COLUMN_GAIN_3 + " FLOAT, "
                + COLUMN_FREQ_3 + " FLOAT, "
                + COLUMN_Q_3 + " FLOAT, "
                + COLUMN_CHECK_4 + " INTEGER, "
                + COLUMN_GAIN_4 + " FLOAT, "
                + COLUMN_FREQ_4 + " FLOAT, "
                + COLUMN_Q_4 + " FLOAT, "
                + COLUMN_CHECK_5 + " INTEGER, "
                + COLUMN_GAIN_5 + " FLOAT, "
                + COLUMN_FREQ_5 + " FLOAT, "
                + COLUMN_Q_5 + " FLOAT"
                + ");"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
