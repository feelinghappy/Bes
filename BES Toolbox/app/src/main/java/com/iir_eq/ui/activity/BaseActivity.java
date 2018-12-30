package com.iir_eq.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.iir_eq.R;
import com.iir_eq.util.Logger;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by zhaowanxing on 2017/4/15.
 */

public class BaseActivity extends AppCompatActivity {
    protected final String TAG = getClass().getSimpleName();

    private Unbinder mUnBinder;

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        mUnBinder = ButterKnife.bind(this);
        keepScreenOn(getApplicationContext(),true);
    }

    private PowerManager.WakeLock wakeLock;
    void keepScreenOn(Context context, boolean on) {
        if (on) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "==KeepScreenOn==");
            wakeLock.acquire();
        } else {
            if (wakeLock != null) {
                wakeLock.release();
                wakeLock = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        Logger.e(TAG, "onBackPressed");
        exit();
    }

    @Override
    protected void onDestroy() {
        Logger.e(TAG, "onDestroy");
        if (mUnBinder != null)
            mUnBinder.unbind();
        super.onDestroy();
        keepScreenOn(getApplicationContext(),false);

    }

    protected void showToast(int msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected void exit() {
        finish();
    }

    protected void showConfirmDialog(int message, final DialogInterface.OnClickListener positiveClickListener) {
        AlertDialog dialog = new AlertDialog.Builder(this).setMessage(message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (positiveClickListener != null) {
                            positiveClickListener.onClick(dialog, which);
                        }
                    }
                })
                .setNegativeButton(R.string.no, null).create();
        dialog.show();
    }
}
