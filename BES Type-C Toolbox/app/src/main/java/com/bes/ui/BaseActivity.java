package com.bes.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.iir_eq.R;

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
        exit();
    }

    @Override
    protected void onDestroy() {
        if (mUnBinder != null)
            mUnBinder.unbind();
        super.onDestroy();
        keepScreenOn(getApplicationContext(),false);

    }

    protected void exit() {
        finish();
    }

}
