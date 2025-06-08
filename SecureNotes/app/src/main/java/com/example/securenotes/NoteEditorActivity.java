package com.example.securenotes;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText etNoteContent;
    private String currentNoteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        etNoteContent = findViewById(R.id.et_note_content);
        Button btnSave = findViewById(R.id.btn_save_note);

        // Ottieni il note_id se passato da Dashboard
        currentNoteId = getIntent().getStringExtra("note_id");

        if (currentNoteId != null) {
            loadNote(currentNoteId);
        }

        btnSave.setOnClickListener(v -> {
            saveNote();
        });
    }

    private void loadNote(String noteId) {
        try {
            File notesDir = new File(getFilesDir(), "notes");
            File noteFile = new File(notesDir, noteId);

            if (!noteFile.exists()) {
                Toast.makeText(this, "Nota non trovata", Toast.LENGTH_SHORT).show();
                return;
            }

            FileInputStream fis = new FileInputStream(noteFile);
            byte[] content = new byte[(int) noteFile.length()];
            fis.read(content);
            fis.close();

            etNoteContent.setText(new String(content));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore nel caricamento", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNote() {
        try {
            String content = etNoteContent.getText().toString().trim();

            if (content.isEmpty()) {
                Toast.makeText(this, "La nota è vuota", Toast.LENGTH_SHORT).show();
                return;
            }

            File notesDir = new File(getFilesDir(), "notes");
            if (!notesDir.exists()) notesDir.mkdir();

            // Se è una nuova nota, genera un nuovo ID
            if (currentNoteId == null) {
                currentNoteId = "note_" + System.currentTimeMillis() + ".txt";
            }

            File noteFile = new File(notesDir, currentNoteId);
            FileOutputStream fos = new FileOutputStream(noteFile);
            fos.write(content.getBytes());
            fos.close();

            Toast.makeText(this, "Nota salvata", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore nel salvataggio", Toast.LENGTH_SHORT).show();
        }
    }
}
