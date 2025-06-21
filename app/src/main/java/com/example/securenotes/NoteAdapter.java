package com.example.securenotes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(String noteId);
        void onNoteLongClick(String noteId);
    }

    private final List<DashboardActivity.NotePreview> notes;
    private final OnNoteClickListener listener;

    public NoteAdapter(List<DashboardActivity.NotePreview> notes, OnNoteClickListener listener) {
        this.notes = notes;
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
        DashboardActivity.NotePreview note = notes.get(position);
        holder.tv.setText(note.title);
        holder.itemView.setOnClickListener(v -> listener.onNoteClick(note.fileName));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onNoteLongClick(note.fileName);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        NoteViewHolder(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tv_note_preview);
        }
    }

    public void updateNotes(List<DashboardActivity.NotePreview> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        notifyDataSetChanged();
    }

}



