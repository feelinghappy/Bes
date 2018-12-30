package com.iir_eq.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.iir_eq.R;
import com.iir_eq.model.Music;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhaowanxing on 2017/7/18.
 */

public class MusicAdapter extends BaseAdapter {

    private Context mContext;
    private OnMusicClickListener mMusicClickListener;

    private List<Music> mMusic;
    private int mPlayIndex;
    private boolean mPlaying;

    public MusicAdapter(Context context, OnMusicClickListener musicClickListener) {
        mContext = context;
        mMusicClickListener = musicClickListener;
        mMusic = new ArrayList<>();
        mPlayIndex = -1;
    }

    public void add(Music music) {
        mMusic.add(music);
        notifyDataSetChanged();
    }

    public void remove(int position) {
        if (position >= 0) {
            mMusic.remove(position);
            notifyDataSetChanged();
        }
    }

    public void clear() {
        mMusic.clear();
        notifyDataSetChanged();
    }

    public void setPlayIndex(int playIndex) {
        if (playIndex == mPlayIndex) {
            return;
        }
        mPlayIndex = playIndex;
        notifyDataSetChanged();
    }

    public int getPlayIndex() {
        return mPlayIndex;
    }

    public void setPlaying(boolean playing) {
        if (playing == mPlaying) {
            return;
        }
        mPlaying = playing;
        notifyDataSetChanged();
    }

    public void onPlayChanged(int index) {
        mPlayIndex = index;
        mPlaying = true;
        notifyDataSetChanged();
    }

    public void next() {
        int size = mMusic.size();
        mPlayIndex = (mPlayIndex + 1) % size;
        mPlaying = true;
        notifyDataSetChanged();
    }

    public void previous() {
        int size = mMusic.size();
        mPlayIndex = (mPlayIndex - 1 + size) % size;
        mPlaying = true;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mMusic.size();
    }

    @Override
    public Music getItem(int position) {
        return mMusic.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            holder = new Holder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.music_item, parent, false);
            holder.mName = (TextView) convertView.findViewById(R.id.name);
            holder.mPlay = convertView.findViewById(R.id.play);
            holder.mPause = convertView.findViewById(R.id.pause);
            holder.mDelete = convertView.findViewById(R.id.delete);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        Music music = mMusic.get(position);
        holder.mName.setText(music.getName());
        boolean playing = (mPlayIndex == position) && mPlaying;
        holder.mPlay.setVisibility(playing ? View.GONE : View.VISIBLE);
        holder.mPause.setVisibility(playing ? View.VISIBLE : View.GONE);
        holder.mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMusicClickListener != null) {
                    mMusicClickListener.onClickPlay(position);
                }
            }
        });
        holder.mPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMusicClickListener != null) {
                    mMusicClickListener.onClickPause(position);
                }
            }
        });
        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMusicClickListener != null) {
                    mMusicClickListener.onClickDelete(position);
                }
            }
        });
        return convertView;
    }

    private class Holder {
        TextView mName;
        View mPlay;
        View mPause;
        View mDelete;
    }

    public interface OnMusicClickListener {
        void onClickPlay(int position);

        void onClickPause(int position);

        void onClickDelete(int position);
    }
}
