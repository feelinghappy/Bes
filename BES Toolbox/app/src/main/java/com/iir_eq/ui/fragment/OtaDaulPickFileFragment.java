package com.iir_eq.ui.fragment;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.iir_eq.R;
import com.iir_eq.contants.Constants;
import com.iir_eq.util.SPHelper;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import static android.app.Activity.RESULT_OK;

public class OtaDaulPickFileFragment extends DialogFragment implements View.OnClickListener{

    private  OtaPickFileCallback mOtaPickFileCallback;
    private int apply_type;
    private TextView dual_pick_file_left_title;
    private TextView ota_file_left;
    private Button pick_ota_file_left;
    private TextView dual_pick_file_right_title;
    private TextView ota_file_right;
    private Button pick_ota_file_right;
    private static final int REQUEST_OTA_DAUL_LEFT_FILE= 0X03;//only 左
    private static final int REQUEST_OTA_DAUL_RIGHT_FILE = 0X04;//only 右

    protected static final String KEY_OTA_DAUL_BOTH_ONE = "ota_file_daul_both_one";
    protected static final String KEY_OTA_DAUL_BOTH_TWO_LEFT_FILE = "ota_file_daul_both_two_left_file";
    protected static final String KEY_OTA_DAUL_BOTH_TWO_RIGHT_FILE = "ota_file_daul_both_two_right_file";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.ota_daul_pick_file, container, false);
        Bundle bundle = getArguments();
        apply_type = bundle.getInt("apply_type",0);
        initView(view);
        return view;
    }

    private void initView(View view) {
        dual_pick_file_left_title = (TextView)view.findViewById(R.id.dual_pick_file_left_title);
        dual_pick_file_right_title = (TextView)view.findViewById(R.id.dual_pick_file_right_title);
        ota_file_left = (TextView)view.findViewById(R.id.ota_file_left);
        ota_file_right = (TextView)view.findViewById(R.id.ota_file_right);
        pick_ota_file_left = (Button)view.findViewById(R.id.pick_ota_file_left);
        pick_ota_file_right = (Button)view.findViewById(R.id.pick_ota_file_right);
        pick_ota_file_left.setOnClickListener(this);
        pick_ota_file_right.setOnClickListener(this);
        if(apply_type == 0)
        {
            dual_pick_file_left_title.setVisibility(View.VISIBLE);
            ota_file_left.setVisibility(View.VISIBLE);
            pick_ota_file_left.setVisibility(View.VISIBLE);
            dual_pick_file_right_title.setVisibility(View.GONE);
            ota_file_right.setVisibility(View.GONE);
            pick_ota_file_right.setVisibility(View.GONE);

        }
        else if(apply_type == 1)
        {
            dual_pick_file_left_title.setVisibility(View.GONE);
            ota_file_left.setVisibility(View.GONE);
            pick_ota_file_left.setVisibility(View.GONE);
            dual_pick_file_right_title.setVisibility(View.VISIBLE);
            ota_file_right.setVisibility(View.VISIBLE);
            pick_ota_file_right.setVisibility(View.VISIBLE);
        }
        else if(apply_type == 2)
        {
            dual_pick_file_left_title.setVisibility(View.VISIBLE);
            ota_file_left.setVisibility(View.VISIBLE);
            pick_ota_file_left.setVisibility(View.VISIBLE);
            dual_pick_file_right_title.setVisibility(View.GONE);
            ota_file_right.setVisibility(View.GONE);
            pick_ota_file_right.setVisibility(View.GONE);
            dual_pick_file_left_title.setText(getString(R.string.pick_ota_file));

        }
        else if(apply_type == 3)
        {
            dual_pick_file_left_title.setVisibility(View.VISIBLE);
            ota_file_left.setVisibility(View.VISIBLE);
            pick_ota_file_left.setVisibility(View.VISIBLE);
            dual_pick_file_right_title.setVisibility(View.VISIBLE);
            ota_file_right.setVisibility(View.VISIBLE);
            pick_ota_file_right.setVisibility(View.VISIBLE);
            dual_pick_file_left_title.setText(getString(R.string.pick_ota_file));
            dual_pick_file_left_title.setText(getString(R.string.pick_ota_file_left));
            dual_pick_file_right_title.setText(getString(R.string.pick_ota_file_right));

        }

        view.findViewById(R.id.ok_dual_pick).setOnClickListener(mOkListener);
        view.findViewById(R.id.cancel_dual_pick).setOnClickListener(mCancelListener);


    }

    private boolean savePickFile() {
        if (apply_type == 0) {
            if (TextUtils.isEmpty(ota_file_left.getText())||ota_file_left.getText().toString().equals("--")) {

                Toast.makeText(getActivity(), getString(R.string.pick_File_tips), Toast.LENGTH_SHORT).show();
                return false;
            } else {
                SPHelper.putPreference(getActivity(), Constants.KEY_OTA_DAUL_LEFT_FILE, ota_file_left.getText());
                return true;
            }

        } else if (apply_type == 1) {
            if (TextUtils.isEmpty(ota_file_right.getText())||ota_file_right.getText().toString().equals("--")) {

                Toast.makeText(getActivity(), getString(R.string.pick_File_tips), Toast.LENGTH_SHORT).show();
                return false;
            } else {
                SPHelper.putPreference(getActivity(), Constants.KEY_OTA_DAUL_RIGHT_FILE, ota_file_right.getText());
                return true;
            }
        } else if (apply_type == 2) {
            if (TextUtils.isEmpty(ota_file_left.getText())||ota_file_left.getText().toString().equals("--")) {
                Toast.makeText(getActivity(), getString(R.string.pick_File_tips), Toast.LENGTH_SHORT).show();
                return false;
            } else {
                SPHelper.putPreference(getActivity(), Constants.KEY_OTA_DAUL_LEFT_FILE, ota_file_left.getText());
                return true;
            }
        } else if (apply_type == 3) {
            if (TextUtils.isEmpty(ota_file_right.getText()) || TextUtils.isEmpty(ota_file_left.getText())||ota_file_right.getText().toString().equals("--")||ota_file_left.getText().toString().equals("--")) {

                Toast.makeText(getActivity(), getString(R.string.pick_File_tips), Toast.LENGTH_SHORT).show();
                return false;
            } else {
                SPHelper.putPreference(getActivity(), Constants.KEY_OTA_DAUL_LEFT_FILE, ota_file_left.getText());
                SPHelper.putPreference(getActivity(), Constants.KEY_OTA_DAUL_RIGHT_FILE, ota_file_right.getText());
                return  true;
            }
        }
        return  false;

    }

    public void setPickFileCallback(OtaPickFileCallback otaPickFileCallback) {
        mOtaPickFileCallback = otaPickFileCallback;
    }

    private final View.OnClickListener mOkListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (savePickFile()) {
                dismissAllowingStateLoss();
                if (mOtaPickFileCallback != null) {
                    mOtaPickFileCallback.onOtaPickFileOk();
                }
            }
            }

    };



    private final View.OnClickListener mCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismissAllowingStateLoss();
            if (mOtaPickFileCallback != null) {
                mOtaPickFileCallback.onOtaPickFileCancel();
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.pick_ota_file_left:
                    pickFile(REQUEST_OTA_DAUL_LEFT_FILE);
                break;
            case R.id.pick_ota_file_right:
                    pickFile(REQUEST_OTA_DAUL_RIGHT_FILE);
                break;
        }
    }


    public interface OtaPickFileCallback {
        void onOtaPickFileOk();

        void onOtaPickFileCancel();
    }
    protected void pickFile(int request) {
        startActivityForResult(new Intent(getContext(), FilePickerActivity.class), request);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("OtaDaulPickFileFragment" , "onActivityResult");
        if (requestCode == REQUEST_OTA_DAUL_LEFT_FILE) {
            onPickFileLeft(resultCode, data);
        }
        else if(requestCode == REQUEST_OTA_DAUL_RIGHT_FILE) {
            onPickFileRight(resultCode, data);
        }

    }

    private void onPickFileLeft(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String file = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            ota_file_left.setText(file);
            //sendCmdDelayed(CMD_CONNECT, 0);
        }
    }

    private void onPickFileRight(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String file = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);;
            ota_file_right.setText(file);
            //sendCmdDelayed(CMD_CONNECT, 0);
        }
    }



}