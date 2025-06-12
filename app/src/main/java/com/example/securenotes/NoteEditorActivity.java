package com.example.securenotes;

import android.content.Intent;
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
    private boolean authenticated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthHelper.authenticate(this, new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                authenticated = true;
                setupUI();
            }

            @Override
            public void onFailure() {
                finish(); // blocca accesso
            }
        });
    }

    private void setupUI() {
        setContentView(R.layout.activity_note_editor);

        etNoteTitle = findViewById(R.id.et_note_title);
        etNoteContent = findViewById(R.id.et_note_content);
        notesDir = new File(getFilesDir(), "notes");
        if (!notesDir.exists()) notesDir.mkdir();

        currentNoteId = getIntent().getStringExtra("note_id");
        if (currentNoteId != null) loadNote(currentNoteId);

        Button btnSave = findViewById(R.id.btn_save_note);
        btnSave.setOnClickListener(v -> saveNote());
    }

    private void loadNote(String noteId) {
        File noteFile = new File(notesDir, noteId);
        if (!noteFile.exists()) {
            Toast.makeText(this, "Nota non trovata", Toast.LENGTH_SHORT).show();
            finish();
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
            Toast.makeText(this, "Errore nel caricamento", Toast.LENGTH_SHORT).show();
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

        try (FileOutputStream fos = new FileOutputStream(noteFile)) {
            fos.write((title + "\n" + content).getBytes());
            Toast.makeText(this, "Nota salvata", Toast.LENGTH_SHORT).show();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore nel salvataggio", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 999) {
            if (resultCode == RESULT_OK && !authenticated) {
                authenticated = true;
                setupUI();
            } else {
                finish(); // autenticazione fallita
            }
        }
    }
}
