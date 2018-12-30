package com.iir_eq.model;

/**
 * Created by zhaowanxing on 2017/4/17.
 */

public class IIRParam {
    private float mGain;
    private float mFrequency;
    private float mQ;

    public IIRParam(float gain, float frequency, float q) {
        mGain = gain;
        mFrequency = frequency;
        mQ = q;
    }

    public float getGain() {
        return mGain;
    }

    public void setGain(float gain) {
        mGain = gain;
    }

    public float getFrequency() {
        return mFrequency;
    }

    public void setFrequency(float frequency) {
        mFrequency = frequency;
    }

    public float getQ() {
        return mQ;
    }

    public void setQ(float q) {
        mQ = q;
    }
}
