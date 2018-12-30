package com.iir_eq.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.iir_eq.R;
import com.iir_eq.bluetooth.LeManager;
import com.iir_eq.bluetooth.callback.ConnectCallback;
import com.iir_eq.model.Music;
import com.iir_eq.ui.adapter.MusicAdapter;
import com.iir_eq.util.ArrayUtil;
import com.iir_eq.util.Logger;

import java.util.Stack;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by zhaowanxing on 2017/7/18.
 */

/**
 * Todo: Loading Tip And Preventing for too fast operation
 */
public class PlayerActivity extends BaseActivity implements MusicAdapter.OnMusicClickListener, ConnectCallback, SeekBar.OnSeekBarChangeListener {

    @BindView(R.id.music_list)
    ListView mMusicList;
    @BindView(R.id.volume_bar)
    SeekBar mVolumeBar;
    @BindView(R.id.volume_value)
    TextView mVolumeValue;

    public static final long TIME_OUT = 5000;

    public static final int MSG_RESPONSE = 0x00;
    public static final int MSG_TIME_OUT = 0x10;
    public static final int MSG_TIME_OUT_PLAY = 0x11;
    public static final int MSG_TIME_OUT_REPLAY = 0x12;
    public static final int MSG_TIME_OUT_PAUSE = 0x13;
    public static final int MSG_TIME_OUT_PREVIOUS = 0x14;
    public static final int MSG_TIME_OUT_NEXT = 0x15;
    public static final int MSG_TIME_OUT_DELETE = 0x16;
    public static final int MSG_TIME_OUT_VOLUME = 0x17;
    public static final int MSG_TIME_OUT_GET_MUSIC_COUNT_AND_VOLUME = 0x18;
    public static final int MSG_TIME_OUT_GET_MUSIC_NAME = 0x19;

    private MusicAdapter mMusicAdapter;
    private LeManager mLeManager;

    private int mDeleteIndex = -1;
    private int mPlayIndex = -1;
    private Stack<Runnable> mLoadMusicTask = new Stack<>();

    private int mLastVolume = 0;

    private HandlerThread mCmdThread;
    private Handler mCmdHandler;

    private Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RESPONSE:
                    onResponse((byte[]) msg.obj);
                    break;
                case MSG_TIME_OUT:
                    break;
                case MSG_TIME_OUT_PLAY:
                case MSG_TIME_OUT_REPLAY:
                case MSG_TIME_OUT_PAUSE:
                case MSG_TIME_OUT_PREVIOUS:
                case MSG_TIME_OUT_NEXT:
                case MSG_TIME_OUT_DELETE:
                case MSG_TIME_OUT_GET_MUSIC_COUNT_AND_VOLUME:
                case MSG_TIME_OUT_GET_MUSIC_NAME:
                    showToast(msg.arg1);
                    break;
                case MSG_TIME_OUT_VOLUME:
                    showToast(msg.arg1);
                    mVolumeBar.setProgress(mLastVolume);
                    break;
            }
        }
    };

    @OnClick({
            R.id.previous,
            R.id.next
    })
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.previous:
                previous();
                break;
            case R.id.next:
                next();
                break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_player);
        initView();
        initConfig();
    }

    private void initView() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mMusicAdapter = new MusicAdapter(this, this);
        mMusicList.setAdapter(mMusicAdapter);
        mVolumeBar.setOnSeekBarChangeListener(this);
    }

    private void initConfig() {
        mCmdThread = new HandlerThread(TAG);
        mCmdThread.start();
        mCmdHandler = new Handler(mCmdThread.getLooper());
        mLeManager = LeManager.getLeManager();
        mLeManager.addConnectCallback(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getMusicCountAndVolume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            if (mCmdThread != null && mCmdThread.isAlive()) {
                mCmdThread.quit();
                mCmdThread = null;
            }
            if (mLeManager != null) {
                mLeManager.close();
                mLeManager.removeConnectCallback(this);
            }
        }
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

    private void sendMessage(int msg, Object obj) {
        Message message = mMsgHandler.obtainMessage(msg);
        message.obj = obj;
        mMsgHandler.sendMessage(message);
    }

    private void sendTimeOut(int timeOut, int info, long delayMillis) {
        Message message = mMsgHandler.obtainMessage(timeOut);
        message.arg1 = info;
        mMsgHandler.sendMessageDelayed(message, delayMillis);
    }

    private void removeTimeOut(int timeOut) {
        mMsgHandler.removeMessages(timeOut);
    }

    private void onResponse(byte[] data) {

        switch (data[0] & 0xFF) {
            case 0x00:
                onGetMusicCountAndVolume((data[2] & 0xFF) | ((data[3] & 0xFF) << 8), data[1] & 0xFF);
                break;
            case 0x01:
                onGetMusicName(new String(ArrayUtil.extractBytes(data, 1, data.length - 1)));
                break;
            case 0x02:
                onPlayChanged((data[1] & 0xFF) | ((data[2] & 0xFF) << 8));
                break;
            case 0x03:
                onDelete(data[1] & 0xFF);
                break;
            case 0x0E:
                onSetVolume(data[1] & 0xFF);
                break;
            case 0x0F:
                onPause(data[1] & 0xFF);
                break;
            case 0x10:
                onReplay(data[1] & 0xFF);
                break;
            case 0x12:
                onPlay(data[1] & 0xFF);
                break;
            case 0x13:
                onNext(data[1] & 0xFF);
                break;
            case 0x14:
                onPrevious(data[1] & 0xFF);
                break;
        }
    }

    private void loadNextMusic() {
        if (!mLoadMusicTask.isEmpty()) {
            mCmdHandler.post(mLoadMusicTask.pop());
        }
    }

    private void getMusicCountAndVolume() {
        mLeManager.write(new byte[]{0x0C, 0x00, 0x00, 0x00});
        sendTimeOut(MSG_TIME_OUT_GET_MUSIC_COUNT_AND_VOLUME, R.string.get_music_count_and_volume_time_out, TIME_OUT);
    }

    private void onGetMusicCountAndVolume(int count, final int volume) {
        removeTimeOut(MSG_TIME_OUT_GET_MUSIC_COUNT_AND_VOLUME);
        mLastVolume = volume;
        mVolumeBar.setProgress(volume);
        for (int i = count - 1; i >= 0; i--) {
            mLoadMusicTask.push(new LoadMusic(i));
        }
        loadNextMusic();
    }

    private void getMusicName(int index) {
        mLeManager.write(new byte[]{0x0D, 0x00, 0x02, 0x00, (byte) index, (byte) (index >> 8)});
        sendTimeOut(MSG_TIME_OUT_GET_MUSIC_NAME, R.string.get_music_name_time_out, TIME_OUT);
    }

    private void onGetMusicName(String name) {
        removeTimeOut(MSG_TIME_OUT_GET_MUSIC_NAME);
        mMusicAdapter.add(new Music(name));
        loadNextMusic();
    }

    private void setVolume(int volume) {
        mLeManager.write(new byte[]{0x0e, 0x00, 0x01, 0x00, (byte) volume});
        sendTimeOut(MSG_TIME_OUT_VOLUME, R.string.set_volume_time_out, TIME_OUT);
    }

    private void onSetVolume(int result) {
        removeTimeOut(MSG_TIME_OUT_VOLUME);
        if (result == 0x01) {
            mLastVolume = mVolumeBar.getProgress();
        } else {
            mVolumeBar.setProgress(mLastVolume);
            showToast(R.string.set_volume_failed);
        }
    }

    private void pause() {
        mLeManager.write(new byte[]{0x0F, 0x00, 0x00, 0x00});
        sendTimeOut(MSG_TIME_OUT_PAUSE, R.string.pause_time_out, TIME_OUT);
    }

    private void onPause(int result) {
        removeTimeOut(MSG_TIME_OUT_PAUSE);
        if (result == 0x01) {
            mMusicAdapter.setPlaying(false);
        } else {
            showToast(R.string.pause_failed);
        }
    }

    private void replay() {
        mLeManager.write(new byte[]{0x10, 0x00, 0x00, 0x00});
        sendTimeOut(MSG_TIME_OUT_REPLAY, R.string.replay_time_out, TIME_OUT);
    }

    private void onReplay(int result) {
        removeTimeOut(MSG_TIME_OUT_REPLAY);
        if (result == 0x01) {
            mMusicAdapter.setPlaying(true);
        } else {
            showToast(R.string.replay_failed);
        }
    }

    private void delete(int index) {
        mDeleteIndex = index;
        mLeManager.write(new byte[]{0x11, 0x00, 0x02, 0x00, (byte) index, (byte) (index >> 8)});
        sendTimeOut(MSG_TIME_OUT_DELETE, R.string.delete_time_out, TIME_OUT);
    }

    private void onDelete(int result) {
        removeTimeOut(MSG_TIME_OUT_DELETE);
        if (result == 0x01) {
            mMusicAdapter.remove(mDeleteIndex);
        } else if (result == 0x00) {
            showToast(R.string.delete_failed);
        }
    }

    private void play(int index) {
        mPlayIndex = index;
        mLeManager.write(new byte[]{0x12, 0x00, 0x02, 0x00, (byte) index, (byte) (index >> 8)});
        sendTimeOut(MSG_TIME_OUT_PLAY, R.string.play_time_out, TIME_OUT);
    }

    private void onPlay(int result) {
        removeTimeOut(MSG_TIME_OUT_PLAY);
        if (result == 0x01) {
            mMusicAdapter.setPlaying(true);
            mMusicAdapter.setPlayIndex(mPlayIndex);
        } else {
            showToast(R.string.play_failed);
        }
    }

    private void previous() {
        mLeManager.write(new byte[]{0x14, 0x00, 0x00, 0x00});
        sendTimeOut(MSG_TIME_OUT_PREVIOUS, R.string.previous_time_out, TIME_OUT);
    }

    void onPrevious(int result) {
        removeTimeOut(MSG_TIME_OUT_PREVIOUS);
        if (result == 0x01) {
            mMusicAdapter.previous();
        } else if (result == 0x00) {
            showToast(R.string.previous_failed);
        }
    }

    private void next() {
        mLeManager.write(new byte[]{0x14, 0x00, 0x00, 0x00});
        sendTimeOut(MSG_TIME_OUT_NEXT, R.string.next_time_out, TIME_OUT);
    }

    private void onNext(int result) {
        removeTimeOut(MSG_TIME_OUT_NEXT);
        if (result == 0x01) {
            mMusicAdapter.next();
        } else {
            showToast(R.string.next_failed);
        }
    }

    private void onPlayChanged(int index) {
        mMusicAdapter.onPlayChanged(index);
    }

    @Override
    public void onClickPlay(int position) {
        Logger.e(TAG, "onClickPlay " + position);
        if (position == mMusicAdapter.getPlayIndex()) {
            replay();
        } else {
            play(position);
        }
    }

    @Override
    public void onClickPause(int position) {
        pause();
    }

    @Override
    public void onClickDelete(final int position) {
        Logger.e(TAG, "onClickDelete " + position);
        new AlertDialog.Builder(this).
                setMessage(getString(R.string.delete_music_tips, mMusicAdapter.getItem(position).getName()))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        delete(position);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .setCancelable(true)
                .create()
                .show();
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        if (!connected) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(getString(R.string.disconnected));
                    finish();
                }
            });
        }
    }

    @Override
    public void onReceive(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        Logger.e(TAG, "onReceive " + ArrayUtil.toHex(data));
        sendMessage(MSG_RESPONSE, data);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mVolumeValue.setText(String.valueOf(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        setVolume(mVolumeBar.getProgress());
    }

    private class LoadMusic implements Runnable {

        private int mIndex;

        public LoadMusic(int index) {
            mIndex = index;
        }

        @Override
        public void run() {
            getMusicName(mIndex);
        }
    }
}
