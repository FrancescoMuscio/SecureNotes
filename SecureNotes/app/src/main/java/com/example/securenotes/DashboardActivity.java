package com.example.securenotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;
    private List<String> noteIds = new ArrayList<>();
    private NoteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        RecyclerView recyclerView = findViewById(R.id.recycler_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(noteIds, new NoteAdapter.OnNoteClickListener() {
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
        try {
            SharedPreferences prefs = getEncryptedPrefs();
            noteIds.clear();
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                noteIds.add(entry.getKey());
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            SharedPreferences prefs = getEncryptedPrefs();
            prefs.edit().remove(noteId).apply();
            loadNoteList();
        } catch (Exception e) {
            e.printStackTrace();
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

    // Menu: impostazioni e cambio PIN
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_change_pin) {
            startActivity(new Intent(this, ChangePinActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

