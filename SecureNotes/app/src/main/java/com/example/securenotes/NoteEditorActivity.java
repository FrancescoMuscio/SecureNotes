package com.example.securenotes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText etNote;
    private String noteId;
    private boolean isNewNote;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        etNote = findViewById(R.id.et_note_content);
        Button btnSave = findViewById(R.id.btn_save_note);

        noteId = getIntent().getStringExtra("note_id");
        isNewNote = (noteId == null);

        if (isNewNote) {
            noteId = UUID.randomUUID().toString(); // genera nuovo ID
        } else {
            loadNoteContent();
        }

        btnSave.setOnClickListener(v -> saveNote());
    }

    private void loadNoteContent() {
        try {
            String content = getEncryptedPrefs().getString(noteId, "");
            etNote.setText(content);
        } catch (Exception e) {
            Toast.makeText(this, "Errore durante il caricamento", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNote() {
        String content = etNote.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Nota vuota", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            getEncryptedPrefs().edit().putString(noteId, content).apply();
            Toast.makeText(this, "Nota salvata", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Errore durante il salvataggio", Toast.LENGTH_SHORT).show();
        }
    }

    private SharedPreferences getEncryptedPrefs() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                this,
                "notes_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}
