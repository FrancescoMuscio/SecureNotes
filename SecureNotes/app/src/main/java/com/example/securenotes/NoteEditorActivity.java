package com.example.securenotes;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.*;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private String currentNoteId;
    private File notesDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        etNoteTitle = findViewById(R.id.et_note_title);
        etNoteContent = findViewById(R.id.et_note_content);
        Button btnSave = findViewById(R.id.btn_save_note);

        notesDir = new File(getFilesDir(), "notes");
        if (!notesDir.exists()) notesDir.mkdir();

        currentNoteId = getIntent().getStringExtra("note_id");

        if (currentNoteId != null) {
            loadNote(currentNoteId);
        }

        btnSave.setOnClickListener(v -> saveNote());
    }

    private void loadNote(String noteId) {
        File noteFile = new File(notesDir, noteId);

        if (!noteFile.exists()) {
            Toast.makeText(this, "Nota non trovata", Toast.LENGTH_SHORT).show();
            finish(); // evita di lasciare l’utente in un editor vuoto
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(noteFile))) {
            String title = reader.readLine();
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            etNoteTitle.setText(title != null ? title : "");
            etNoteContent.setText(content.toString().trim());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore nel caricamento della nota", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Inserisci un titolo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "Il contenuto è vuoto", Toast.LENGTH_SHORT).show();
            return;
        }

        // Se nuova nota, genera nuovo nome file
        if (currentNoteId == null) {
            currentNoteId = "note_" + System.currentTimeMillis() + ".txt";
        }

        File noteFile = new File(notesDir, currentNoteId);

        try (FileOutputStream fos = new FileOutputStream(noteFile)) {
            fos.write((title + "\n" + content).getBytes());
            Toast.makeText(this, "Nota salvata", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore nel salvataggio", Toast.LENGTH_SHORT).show();
        }
    }
}
