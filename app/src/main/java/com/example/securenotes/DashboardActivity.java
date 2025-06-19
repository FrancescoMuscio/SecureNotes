package com.example.securenotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;
    private NoteAdapter adapter;
    private boolean wasInBackground = false;
    private List<NotePreview> notes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        RecyclerView recyclerView = findViewById(R.id.recycler_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(notes, new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(String noteId) {
                openNote(noteId);
            }

            @Override
            public void onNoteLongClick(String noteId) {
                confirmDeleteNote(noteId);
            }
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_note);
        fab.setOnClickListener(v -> openNote(null));

        timeoutRunnable = () -> {
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AppLifecycleTracker.needsReauth()) {
            AppLifecycleTracker.clearReauthFlag();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        resetTimeout();
        loadNoteList();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetTimeout();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timeoutHandler.removeCallbacks(timeoutRunnable);
    }

    private void resetTimeout() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, getTimeoutDurationMillis());
    }

    private long getTimeoutDurationMillis() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int timeoutMinutes = prefs.getInt("timeout_minutes", 3);
        return timeoutMinutes * 60 * 1000L;
    }

    private void loadNoteList() {
        notes.clear();
        File notesDir = new File(getFilesDir(), "notes");
        if (!notesDir.exists()) notesDir.mkdirs();

        File[] files = notesDir.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    String text = EncryptedFileHelper.readEncryptedTextFile(this, f);
                    String title = text.split("\n", 2)[0];
                    notes.add(new NotePreview(f.getName(), title));
                } catch (Exception e) {
                    // ignora file corrotti o accesso negato
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void openNote(String noteId) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        if (noteId != null) {
            intent.putExtra("note_id", noteId);
        }
        startActivity(intent);
    }

    private void confirmDeleteNote(String noteId) {
        new AlertDialog.Builder(this)
                .setTitle("Elimina nota")
                .setMessage("Vuoi eliminare definitivamente questa nota?")
                .setPositiveButton("Sì", (dialog, which) -> deleteNote(noteId))
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void deleteNote(String noteId) {
        try {
            File notesDir = new File(getFilesDir(), "notes");
            File noteFile = new File(notesDir, noteId);
            if (noteFile.exists()) {
                noteFile.delete();
            }
            loadNoteList();
            Toast.makeText(this, "Nota eliminata", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore durante l'eliminazione", Toast.LENGTH_SHORT).show();
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

    // Menu impostazioni e allegati
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_file_vault) {
            startActivity(new Intent(this, FileVaultActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        wasInBackground = true;
    }

    // Preview oggetto nota con titolo + nome file
    public static class NotePreview {
        public final String fileName;
        public final String title;

        public NotePreview(String fileName, String title) {
            this.fileName = fileName;
            this.title = title;
        }
    }
}


