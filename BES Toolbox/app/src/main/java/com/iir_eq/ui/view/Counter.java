package com.iir_eq.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.iir_eq.R;

/**
 * Created by zhaowanxing on 2017/4/21.
 */

public class Counter extends android.support.v7.widget.AppCompatImageView {
    private final String TAG = getClass().getSimpleName();

    private float mCountUnit = 1;

    private Handler mHandler = new Handler();

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.removeCallbacks(mRunnable);
            if (mCounterTouchListener != null)
                mCounterTouchListener.onTouch(Counter.this);
            mHandler.postDelayed(this, 100);
        }
    };

    public Counter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Counter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.Counter);
        mCountUnit = array.getFloat(R.styleable.Counter_countUnit, 1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getX() < 0 || event.getY() < 0 || event.getX() > getWidth() || event.getY() > getHeight()) {
            mHandler.removeCallbacks(mRunnable);
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mHandler.post(mRunnable);
                return true;
            case MotionEvent.ACTION_UP:
                mHandler.removeCallbacks(mRunnable);
                return true;
        }
        return super.onTouchEvent(event);
    }

    public float getCountUnit() {
        return mCountUnit;
    }

    private OnCounterTouchListener mCounterTouchListener;

    public void setCounterTouchListener(OnCounterTouchListener counterTouchListener) {
        mCounterTouchListener = counterTouchListener;
    }

    public interface OnCounterTouchListener {
        void onTouch(View view);
    }
}
