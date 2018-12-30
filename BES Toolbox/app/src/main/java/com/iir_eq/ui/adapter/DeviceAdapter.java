package com.iir_eq.ui.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.UiThread;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.iir_eq.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhaowanxing on 2017/4/16.
 */

public class DeviceAdapter extends BaseAdapter {

    private List<BluetoothDevice> mDevices;
    private Map<String, Integer> mRssis;

    private Context mContext;

    public DeviceAdapter(Context context) {
        mContext = context;
        mDevices = new ArrayList<>();
        mRssis = new HashMap<>();
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return mDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.device_item, parent, false);
            holder = new ViewHolder();
            holder.mName = (TextView) convertView.findViewById(R.id.name);
            holder.mAddress = (TextView) convertView.findViewById(R.id.address);
            holder.mRssiIcon = (ImageView) convertView.findViewById(R.id.rssi_icon);
            holder.mRssiValue = (TextView) convertView.findViewById(R.id.rssi_value);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice device = mDevices.get(position);
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = device.getAddress();
        }
        holder.mName.setText(name);
        holder.mAddress.setText(device.getAddress());
        int rssi = mRssis.get(device.getAddress());
        holder.mRssiIcon.setImageResource(getRssiLevel(rssi));
        holder.mRssiValue.setText(String.valueOf(rssi));
        return convertView;
    }

    @UiThread
    public void add(BluetoothDevice device, int rssi) {
        synchronized (mDevices) {
            int index = mDevices.indexOf(device);
            if (index < 0) {
                mDevices.add(device);
                mRssis.put(device.getAddress(), rssi);
                notifyDataSetChanged();
            } else {
                if (!TextUtils.isEmpty(device.getName()) && TextUtils.isEmpty(mDevices.get(index).getName())) {
                    mDevices.set(index, device);
                    notifyDataSetChanged();
                }
                if (mRssis.get(device.getAddress()) != rssi) {
                    mRssis.put(device.getAddress(), rssi);
                    notifyDataSetChanged();
                }
            }
        }
    }

    public void clear() {
        synchronized (mDevices) {
            mDevices.clear();
            mRssis.clear();
            notifyDataSetChanged();
        }
    }

    private int getRssiLevel(int rssi) {
        if (rssi < -90) {
            return R.mipmap.signal_level0;
        }
        if (rssi < -80) {
            return R.mipmap.signal_level1;
        }
        if (rssi < -70) {
            return R.mipmap.signal_level2;
        }
        if (rssi < -60) {
            return R.mipmap.signal_level3;
        }
        if (rssi < -50) {
            return R.mipmap.signal_level4;
        }
        return R.mipmap.signal_level5;
    }

    private class ViewHolder {
        TextView mName;
        TextView mAddress;
        ImageView mRssiIcon;
        TextView mRssiValue;
    }
}
