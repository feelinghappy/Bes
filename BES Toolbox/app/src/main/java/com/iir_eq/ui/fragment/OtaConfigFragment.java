package com.iir_eq.ui.fragment;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.iir_eq.R;
import com.iir_eq.contants.Constants;
import com.iir_eq.util.SPHelper;

/**
 * Created by zhaowanxing on 2017/9/1.
 */

public class OtaConfigFragment extends DialogFragment {

    private boolean mClearUserData;
    private boolean mUpdateBtAddress;
    private boolean mUpdateBtName;
    private boolean mUpdateBleAddress;
    private boolean mUpdateBleName;

    private EditText mBtAddress;
    private EditText mBtName;
    private EditText mBleAddress;
    private EditText mBleName;

    private OtaConfigCallback mOtaConfigCallback;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.ota_config, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        RadioGroup clearUserData = (RadioGroup) view.findViewById(R.id.clear_user_data);
        mClearUserData = (boolean) SPHelper.getPreference(getActivity(), Constants.KEY_OTA_CONFIG_CLEAR_USER_DATA, Constants.DEFAULT_CLEAR_USER_DATA);
        clearUserData.check(mClearUserData ? R.id.clear_user_data_yes : R.id.clear_user_data_no);
        clearUserData.setOnCheckedChangeListener(mClearUserDataCheckedChangedListener);

        RadioGroup updateBtAddress = (RadioGroup) view.findViewById(R.id.update_bt_addr);
        mUpdateBtAddress = (boolean) SPHelper.getPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BT_ADDRESS, Constants.DEFAULT_UPDATE_BT_ADDRESS);
        updateBtAddress.check(mUpdateBtAddress ? R.id.update_bt_addr_yes : R.id.update_bt_addr_no);
        mBtAddress = (EditText) view.findViewById(R.id.update_bt_addr_input);
        mBtAddress.setEnabled(mUpdateBtAddress);
        mBtAddress.setText(SPHelper.getPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BT_ADDRESS_VALUE, "").toString());
        updateBtAddress.setOnCheckedChangeListener(mUpdateBtAddressCheckedChangedListener);

        RadioGroup updateBtName = (RadioGroup) view.findViewById(R.id.update_bt_name);
        mUpdateBtName = (boolean) SPHelper.getPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BT_NAME, Constants.DEFAULT_UPDATE_BT_NAME);
        updateBtName.check(mUpdateBtName ? R.id.update_bt_name_yes : R.id.update_bt_name_no);
        mBtName = (EditText) view.findViewById(R.id.update_bt_name_input);
        mBtName.setEnabled(mUpdateBtName);
        mBtName.setText(SPHelper.getPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BT_NAME_VALUE, "").toString());
        updateBtName.setOnCheckedChangeListener(mUpdateBtNameCheckedChangedListener);

        RadioGroup updateBleAddress = (RadioGroup) view.findViewById(R.id.update_ble_addr);
        mUpdateBleAddress = (boolean) SPHelper.getPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BLE_ADDRESS, Constants.DEFAULT_UPDATE_BLE_ADDRESS);
        updateBleAddress.check(mUpdateBleAddress ? R.id.update_ble_addr_yes : R.id.update_ble_addr_no);
        mBleAddress = (EditText) view.findViewById(R.id.update_ble_addr_input);
        mBleAddress.setEnabled(mUpdateBleAddress);
        mBleAddress.setText(SPHelper.getPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BLE_ADDRESS_VALUE, "").toString());
        updateBleAddress.setOnCheckedChangeListener(mUpdateBleAddressCheckedChangedListener);

        RadioGroup updateBleName = (RadioGroup) view.findViewById(R.id.update_ble_name);
        mUpdateBleName = (boolean) SPHelper.getPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BLE_NAME, Constants.DEFAULT_UPDATE_BLE_NAME);
        updateBleName.check(mUpdateBleName ? R.id.update_ble_name_yes : R.id.update_ble_name_no);
        mBleName = (EditText) view.findViewById(R.id.update_ble_name_input);
        mBleName.setEnabled(mUpdateBleName);
        mBleName.setText(SPHelper.getPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BLE_NAME_VALUE, "").toString());
        updateBleName.setOnCheckedChangeListener(mUpdateBleNameChangedListener);

        view.findViewById(R.id.ok).setOnClickListener(mOkListener);
        view.findViewById(R.id.cancel).setOnClickListener(mCancelListener);
    }

    private boolean saveConfig() {
        if (mUpdateBtAddress) {
            String btAddress = mBtAddress.getText().toString();
            if (!btAddress.matches("[0-9a-fA-F]+") || btAddress.length() != 12) {
                Toast.makeText(getActivity(), R.string.invalid_bt_address, Toast.LENGTH_SHORT).show();
                return false;
            }
            SPHelper.putPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BT_ADDRESS_VALUE, btAddress);
        }
        if (mUpdateBtName) {
            String btName = mBtName.getText().toString();
            if (TextUtils.isEmpty(btName)) {
                Toast.makeText(getActivity(), R.string.invalid_bt_name, Toast.LENGTH_SHORT).show();
                return false;
            }
            SPHelper.putPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BT_NAME_VALUE, btName);
        }
        if (mUpdateBleAddress) {
            String bleAddress = mBleAddress.getText().toString();
            if (!bleAddress.matches("[0-9a-fA-F]+") || bleAddress.length() != 12) {
                Toast.makeText(getActivity(), R.string.invalid_ble_address, Toast.LENGTH_SHORT).show();
                return false;
            }
            SPHelper.putPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BLE_ADDRESS_VALUE, bleAddress);
        }
        if (mUpdateBleName) {
            String bleName = mBleName.getText().toString();
            if (TextUtils.isEmpty(bleName)) {
                Toast.makeText(getActivity(), R.string.invalid_ble_name, Toast.LENGTH_SHORT).show();
                return false;
            }
            SPHelper.putPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BLE_NAME_VALUE, bleName);
        }
        SPHelper.putPreference(getActivity(), Constants.KEY_OTA_CONFIG_CLEAR_USER_DATA, mClearUserData);
        SPHelper.putPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BT_ADDRESS, mUpdateBtAddress);
        SPHelper.putPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BT_NAME, mUpdateBtName);
        SPHelper.putPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BLE_ADDRESS, mUpdateBleAddress);
        SPHelper.putPreference(getActivity(), Constants.KEY_OTA_CONFIG_UPDATE_BLE_NAME, mUpdateBleName);
        return true;
    }

    public void setOtaConfigCallback(OtaConfigCallback otaConfigCallback) {
        mOtaConfigCallback = otaConfigCallback;
    }

    private final View.OnClickListener mOkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (saveConfig()) {
                dismissAllowingStateLoss();
                if (mOtaConfigCallback != null) {
                    mOtaConfigCallback.onOtaConfigOk();
                }
            }
        }
    };

    private final View.OnClickListener mCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismissAllowingStateLoss();
            if (mOtaConfigCallback != null) {
                mOtaConfigCallback.onOtaConfigCancel();
            }
        }
    };

    private final RadioGroup.OnCheckedChangeListener mClearUserDataCheckedChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
            switch (checkedId) {
                case R.id.clear_user_data_yes:
                    mClearUserData = true;
                    break;
                case R.id.clear_user_data_no:
                    mClearUserData = false;
                    break;
            }
        }
    };

    private final RadioGroup.OnCheckedChangeListener mUpdateBtAddressCheckedChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
            switch (checkedId) {
                case R.id.update_bt_addr_yes:
                    mUpdateBtAddress = true;
                    mBtAddress.setEnabled(mUpdateBtAddress);
                    break;
                case R.id.update_bt_addr_no:
                    mUpdateBtAddress = false;
                    mBtAddress.setEnabled(mUpdateBtAddress);
                    break;
            }
        }
    };

    private final RadioGroup.OnCheckedChangeListener mUpdateBtNameCheckedChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
            switch (checkedId) {
                case R.id.update_bt_name_yes:
                    mUpdateBtName = true;
                    mBtName.setEnabled(mUpdateBtName);
                    break;
                case R.id.update_bt_name_no:
                    mUpdateBtName = false;
                    mBtName.setEnabled(mUpdateBtName);
                    break;
            }
        }
    };

    private final RadioGroup.OnCheckedChangeListener mUpdateBleAddressCheckedChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
            switch (checkedId) {
                case R.id.update_ble_addr_yes:
                    mUpdateBleAddress = true;
                    mBleAddress.setEnabled(mUpdateBleAddress);
                    break;
                case R.id.update_ble_addr_no:
                    mUpdateBleAddress = false;
                    mBleAddress.setEnabled(mUpdateBleAddress);
                    break;
            }
        }
    };

    private final RadioGroup.OnCheckedChangeListener mUpdateBleNameChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
            switch (checkedId) {
                case R.id.update_ble_name_yes:
                    mUpdateBleName = true;
                    mBleName.setEnabled(mUpdateBleName);
                    break;
                case R.id.update_ble_name_no:
                    mUpdateBleName = false;
                    mBleName.setEnabled(mUpdateBleName);
                    break;
            }
        }
    };

    public interface OtaConfigCallback {
        void onOtaConfigOk();

        void onOtaConfigCancel();
    }
}
