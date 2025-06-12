package com.example.securenotes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
        String noteId = noteIds.get(position);
        Context context = holder.itemView.getContext();

        String title = noteId; // fallback

        try {
            File notesDir = new File(context.getFilesDir(), "notes");
            File noteFile = new File(notesDir, noteId);

            if (noteFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(noteFile));
                String firstLine = reader.readLine();
                if (firstLine != null && !firstLine.trim().isEmpty()) {
                    title = firstLine.trim();
                }
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace(); // in produzione puoi loggare o ignorare
        }

        holder.tv.setText(title);
        holder.itemView.setOnClickListener(v -> listener.onNoteClick(noteId));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onNoteLongClick(noteId);
            return true;
        });
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

