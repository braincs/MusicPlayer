package com.braincs.attrsc.musicplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

/**
 * Created by Shuai
 * 21/12/2019.
 */
public class MusicPlayerModelAdapter extends RecyclerView.Adapter<MusicPlayerModelAdapter.MusicViewHolder> {
    private List<String> names;
    private int currentIndex;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public MusicPlayerModelAdapter(MusicPlayerModel model) {
        this.names = model.getMusicList();
        this.currentIndex = model.getCurrentIndex();
    }

    public MusicPlayerModelAdapter(MusicPlayerModel model, OnItemClickListener listener) {
        this.names = model.getMusicList();
        this.currentIndex = model.getCurrentIndex();
        this.onItemClickListener = listener;
    }

    public void updateModel(MusicPlayerModel model){
        this.names = model.getMusicList();
        this.currentIndex = model.getCurrentIndex();
    }
    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        LinearLayout v = (LinearLayout) LayoutInflater.from(this.context)
                .inflate(R.layout.layout_music_list, parent, false);
        return new MusicViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MusicViewHolder musicViewHolder, int position) {
        File file = new File(names.get(position));
        String name = file.getName();
        String path = file.getParent();
//        String path = file.getParentFile().getName();

        musicViewHolder.tvName.setText(name);
        if (position == currentIndex) {
            musicViewHolder.tvName.setTextColor(context.getResources().getColor(R.color.CRIMSON));
        }else {
            musicViewHolder.tvName.setTextColor(context.getResources().getColor(R.color.STEELBLUE));
        }
        musicViewHolder.tvPath.setText(path);
        musicViewHolder.ll_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    int layoutPosition = musicViewHolder.getLayoutPosition();
                    onItemClickListener.onItemClick(musicViewHolder.ll_item, layoutPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
//        void onItemLongClick(View view, int position);
    }

    /**
     * 设置回调监听
     *
     * @param listener OnItemClickListener 监听
     */
    public void setOnItemClickListener(MusicPlayerModelAdapter.OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {

        LinearLayout ll_item;
        TextView tvName;
        TextView tvPath;

        MusicViewHolder(LinearLayout ll_item) {
            super(ll_item);
            this.ll_item = ll_item;
            this.tvName = ll_item.findViewById(R.id.tv_list_name);
            this.tvPath = ll_item.findViewById(R.id.tv_list_path);

        }
    }
}
