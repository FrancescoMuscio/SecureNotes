package com.example.securenotes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final List<String> noteIds;
    private final OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(String noteId);
        void onNoteLongClick(String noteId);
    }

    public NoteAdapter(List<String> noteIds, OnNoteClickListener listener) {
        this.noteIds = noteIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        String id = noteIds.get(position);
        holder.tv.setText("Nota #" + id);
        holder.itemView.setOnClickListener(v -> listener.onNoteClick(id));
        holder.itemView.setOnLongClickListener(v -> {listener.onNoteLongClick(id);return true;});
    }

    @Override
    public int getItemCount() {
        return noteIds.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        NoteViewHolder(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tv_note_preview);
        }
    }


}
