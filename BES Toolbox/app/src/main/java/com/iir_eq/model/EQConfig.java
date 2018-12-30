package com.iir_eq.model;

import com.iir_eq.model.IIRParam;

/**
 * Created by zhaowanxing on 2017/4/23.
 */

public class EQConfig {

    public static final byte[] DEFAULT_CONFIG = new byte[]{
            0x00, 0x00, (byte) 0x80, (byte) 0xc0,
            0x00, 0x00, (byte) 0x80, (byte) 0xc0,
            0x05, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x48, 0x42,
            0x33, 0x33, 0x33, 0x3f,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x7a, 0x43,
            0x33, 0x33, 0x33, 0x3f,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x7a, 0x44,
            0x33, 0x33, 0x33, 0x3f,
            0x00, 0x00, 0x40, 0x41,
            0x00, 0x00, 0x7a, 0x45,
            0x33, 0x33, 0x33, 0x3f,
            0x00, 0x00, 0x40, 0x41,
            0x00, 0x00, (byte) 0xfa, 0x45,
            0x33, 0x33, 0x33, 0x3f
    };

    public static final int PARAM_NUM = 5;
    private boolean[] mChks = new boolean[PARAM_NUM];

    private float mLeftGain;
    private float mRightGain;

    private IIRParam[] mParams = new IIRParam[PARAM_NUM];

    public EQConfig() {
        reset();
    }

    private void reset() {
        mLeftGain = -4.0f;
        mRightGain = -4.0f;
        for (int i = 0; i < PARAM_NUM; i++) {
            mChks[i] = true;
        }
        mParams[0] = new IIRParam(0.0f, 50.0f, 0.7f);
        mParams[1] = new IIRParam(0.0f, 250.0f, 0.7f);
        mParams[2] = new IIRParam(0.0f, 1000.0f, 0.7f);
        mParams[3] = new IIRParam(12.0f, 4000.0f, 0.7f);
        mParams[4] = new IIRParam(12.0f, 8000.0f, 0.7f);
    }

    public void setLeftGain(float leftGain) {
        mLeftGain = leftGain;
    }

    public void setRightGain(float rightGain) {
        mRightGain = rightGain;
    }

    public boolean[] getChks() {
        return mChks;
    }

    public float getLeftGain() {
        return mLeftGain;
    }

    public float getRightGain() {
        return mRightGain;
    }

    public IIRParam[] getParams() {
        return mParams;
    }

    public int getNumOfValidParam() {
        int num = 0;
        for (boolean chk : mChks)
            if (chk)
                num++;
        return num;
    }

    public byte[] parse() {
        int num = getNumOfValidParam();
        int count = 0;

        byte[] data = new byte[(num + 1) * 12];
        int leftGain = Float.floatToIntBits(mLeftGain);
        data[count++] = (byte) leftGain;
        data[count++] = (byte) (leftGain >> 8);
        data[count++] = (byte) (leftGain >> 16);
        data[count++] = (byte) (leftGain >> 24);
        int rightGain = Float.floatToIntBits(mRightGain);
        data[count++] = (byte) rightGain;
        data[count++] = (byte) (rightGain >> 8);
        data[count++] = (byte) (rightGain >> 16);
        data[count++] = (byte) (rightGain >> 24);
        data[count++] = (byte) num;
        data[count++] = (byte) (num >> 8);
        data[count++] = (byte) (num >> 16);
        data[count++] = (byte) (num >> 24);

        for (int i = 0; i < mChks.length; i++) {
            if (mChks[i]) {
                int gain = Float.floatToIntBits(mParams[i].getGain());
                data[count++] = (byte) gain;
                data[count++] = (byte) (gain >> 8);
                data[count++] = (byte) (gain >> 16);
                data[count++] = (byte) (gain >> 24);
                int freq = Float.floatToIntBits(mParams[i].getFrequency());
                data[count++] = (byte) freq;
                data[count++] = (byte) (freq >> 8);
                data[count++] = (byte) (freq >> 16);
                data[count++] = (byte) (freq >> 24);
                int q = Float.floatToIntBits(mParams[i].getQ());
                data[count++] = (byte) q;
                data[count++] = (byte) (q >> 8);
                data[count++] = (byte) (q >> 16);
                data[count++] = (byte) (q >> 24);
            }
        }
        return data;
    }
}
