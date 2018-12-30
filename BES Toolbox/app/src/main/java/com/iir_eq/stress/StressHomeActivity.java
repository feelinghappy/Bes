package com.iir_eq.stress;

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
import com.iir_eq.ui.activity.BaseActivity;
import com.iir_eq.ui.activity.EQActivity;
import com.iir_eq.ui.activity.LeOtaActivity;
import com.iir_eq.ui.activity.LeScanActivity;
import com.iir_eq.ui.activity.PlayerActivity;
import com.iir_eq.ui.activity.SppOtaActivity;
import com.iir_eq.util.CommUtils;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by zhaowanxing on 2017/4/23.
 */

public class StressHomeActivity extends BaseActivity {

    private PopupWindow mInfo;

    @BindView(R.id.container)
    View mContainer;

    @OnClick({
            R.id.stress_spp_ota,
            R.id.stress_ble_ota,
    })

    void onClick(View view) {
        switch (view.getId()) {
            case R.id.stress_ble_ota:
                startActivity(new Intent(this, StressLeOtaActivity.class));
                break;
            case R.id.stress_spp_ota:
                startActivity(new Intent(this, StressSppOtaActivity.class));
                break;
            default:
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_stress_home);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
