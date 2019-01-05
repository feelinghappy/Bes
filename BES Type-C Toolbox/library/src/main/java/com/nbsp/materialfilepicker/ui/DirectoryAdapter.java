package com.nbsp.materialfilepicker.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.nbsp.materialfilepicker.R;
import com.nbsp.materialfilepicker.utils.FileTypeUtils;

import java.io.File;
import java.util.List;

/**
 * Created by Dimorinny on 24.10.15.
 */

public class DirectoryAdapter extends RecyclerView.Adapter<DirectoryAdapter.DirectoryViewHolder> {

    CheckBox lastCheckBox ;

    public interface OnItemClickListener {
        void onItemClick(View view, int position );
        void onItemClickCancel();
        void onItemSelect(View view , int position);
    }

    public class DirectoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView mFileImage;
        private TextView mFileTitle;
        private TextView mFileSubtitle;
        private CheckBox mCheckBox ;

        public DirectoryViewHolder(final View itemView, final OnItemClickListener clickListener) {
            super(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onItemClick(v, getAdapterPosition());
                }
            });

            mFileImage = (ImageView) itemView.findViewById(R.id.item_file_image);
            mFileTitle = (TextView) itemView.findViewById(R.id.item_file_title);
            mFileSubtitle = (TextView) itemView.findViewById(R.id.item_file_subtitle);
            mCheckBox = (CheckBox) itemView.findViewById(R.id.item_checkBox);
            mCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mCheckBox.isChecked()){
                        if(clickListener != null){
                            clickListener.onItemSelect(view, getAdapterPosition());
                        }
                        if(lastCheckBox != null){
                            lastCheckBox.setChecked(false);
                        }
                        lastCheckBox = mCheckBox ;
                    }else{
                        if(clickListener != null){
                            clickListener.onItemClickCancel();
                        }
                    }
                }
            });
//            mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                    if(b){
//
//
//                    }else{
//
//                    }
//                }
//            });
        }
    }

    private List<File> mFiles;
    private Context mContext;
    private OnItemClickListener mOnItemClickListener;
    private  boolean isChooseFile = true;

    public DirectoryAdapter(Context context, List<File> files , boolean isChooseFile) {
        mContext = context;
        mFiles = files;
        this.isChooseFile = isChooseFile ;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @Override
    public DirectoryViewHolder onCreateViewHolder(ViewGroup parent,
                                                int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);

        return new DirectoryViewHolder(view, mOnItemClickListener);
    }

    @Override
    public void onBindViewHolder(DirectoryViewHolder holder, int position) {
        File currentFile = mFiles.get(position);

        FileTypeUtils.FileType fileType = FileTypeUtils.getFileType(currentFile);
        holder.mFileImage.setImageResource(fileType.getIcon());
        holder.mFileSubtitle.setText(fileType.getDescription());
        holder.mFileTitle.setText(currentFile.getName());
        switch (fileType) {
            case DIRECTORY:
                if (isChooseFile) {
                    holder.mCheckBox.setVisibility(View.GONE);
                } else {
                    holder.mCheckBox.setVisibility(View.VISIBLE);
                }
                break;
            case DOCUMENT:
                if (isChooseFile) {
                    holder.mCheckBox.setVisibility(View.VISIBLE);
                } else {
                    holder.mCheckBox.setVisibility(View.GONE);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public File getModel(int index) {
        return mFiles.get(index);
    }
}