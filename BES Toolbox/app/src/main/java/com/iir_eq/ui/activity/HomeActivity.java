package com.iir_eq.ui.activity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.iir_eq.R;
import com.iir_eq.bluetooth.BtHelper;
import com.iir_eq.bluetooth.LeManager;
import com.iir_eq.stress.StressHomeActivity;
import com.iir_eq.usb.USBOtaActivity;
import com.iir_eq.usb.USBOtaDemoActivity;
import com.iir_eq.util.CommUtils;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by zhaowanxing on 2017/4/23.
 */

public class HomeActivity extends BaseActivity {

    private PopupWindow mInfo;

    @BindView(R.id.container)
    View mContainer;

    @OnClick({
            R.id.player,
            R.id.spp_ota,
            R.id.ble_ota,
            R.id.eq,
            R.id.stress_test,
            R.id.usb_ota_tools_test,
            R.id.usb_ota_demo_test
    })
    void onClick(View view) {
        switch (view.getId()) {
            case R.id.spp_ota:
                startActivity(new Intent(this, SppOtaActivity.class));
                break;
            case R.id.ble_ota:
                startActivity(new Intent(this, LeOtaActivity.class));
                break;
            case R.id.eq:
                gotoEQ();
                break;
            case R.id.player:
                gotoPlayer();
                break;
            case R.id.stress_test:
                startActivity(new Intent(this , StressHomeActivity.class));
                break;
            case R.id.usb_ota_tools_test:
                startActivity(new Intent(this , USBOtaActivity.class));
                break;
            case R.id.usb_ota_demo_test:
                startActivity(new Intent(this , USBOtaDemoActivity.class));
                break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.act_home);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:
                moreInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void gotoEQ() {
        if (BtHelper.getBluetoothAdapter(this).isEnabled() && LeManager.getLeManager().isConnected()) {
            startActivity(new Intent(this, EQActivity.class));
        } else {
            Intent intent = new Intent(this, LeScanActivity.class);
            intent.putExtra(LeScanActivity.EXTRA_MODE, LeScanActivity.MODE_EQ);
            startActivity(intent);
        }
    }

    private void gotoPlayer() {
        if (BtHelper.getBluetoothAdapter(this).isEnabled() && LeManager.getLeManager().isConnected()) {
            startActivity(new Intent(this, PlayerActivity.class));
        } else {
            Intent intent = new Intent(this, LeScanActivity.class);
            intent.putExtra(LeScanActivity.EXTRA_MODE, LeScanActivity.MODE_PLAYER);
            startActivity(intent);
        }
    }

    private void moreInfo() {
        if (mInfo != null && mInfo.isShowing()) {
            mInfo.dismiss();
            return;
        }
        if (mInfo == null) {
            View view = getLayoutInflater().inflate(R.layout.wnd_info, null);
            mInfo = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            mInfo.setOutsideTouchable(true);
            mInfo.setBackgroundDrawable(new ColorDrawable(0x00000000));
            final TextView version = (TextView) view.findViewById(R.id.version);
            String versionName = CommUtils.getVersion(HomeActivity.this);
            version.setText(versionName);

         }
        mInfo.showAsDropDown(mContainer, mContainer.getWidth(), -mContainer.getHeight());
    }
}
