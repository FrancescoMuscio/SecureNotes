package com.example.securenotes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private String currentNoteId;
    private File notesDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthHelper.authenticate(this, new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                try {
                    EncryptedFileHelper.getMasterKey(getApplicationContext()); // forza Keystore
                    setupUI();
                } catch (Exception e) {
                    Toast.makeText(NoteEditorActivity.this, "Errore Keystore", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure() {
                finish();
            }
        });
    }

    private void setupUI() {
        setContentView(R.layout.activity_note_editor);
        etNoteTitle = findViewById(R.id.et_note_title);
        etNoteContent = findViewById(R.id.et_note_content);
        notesDir = new File(getFilesDir(), "notes");
        if (!notesDir.exists()) notesDir.mkdirs();

        currentNoteId = getIntent().getStringExtra("note_id");
        if (currentNoteId != null) loadNote(currentNoteId);

        Button btnSave = findViewById(R.id.btn_save_note);
        btnSave.setOnClickListener(v -> saveNote());
    }

    private void loadNote(String noteId) {
        File noteFile = new File(notesDir, noteId);
        try {
            String fullText = EncryptedFileHelper.readEncryptedTextFile(this, noteFile);
            String[] lines = fullText.split("\n", 2);
            etNoteTitle.setText(lines.length > 0 ? lines[0] : "");
            etNoteContent.setText(lines.length > 1 ? lines[1] : "");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore caricamento nota", Toast.LENGTH_SHORT).show();
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

        if (currentNoteId == null) {
            currentNoteId = "note_" + System.currentTimeMillis() + ".txt";
        }

        File noteFile = new File(notesDir, currentNoteId);
        String fullText = title + "\n" + content;

        try {
            EncryptedFileHelper.saveEncryptedTextFile(this, noteFile, fullText);
            Toast.makeText(this, "Nota salvata", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore salvataggio", Toast.LENGTH_SHORT).show();
        }
    }
}




