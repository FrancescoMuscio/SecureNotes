package com.example.securenotes;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.scottyab.rootbeer.RootBeer;


public class DashboardActivity extends AppCompatActivity {

    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;
    private NoteAdapter adapter;
    private boolean wasInBackground = false;
    private List<NotePreview> notes = new ArrayList<>();
    private List<NotePreview> allNotes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Controllo root
        RootBeer rootBeer = new RootBeer(this);
        if (rootBeer.isRooted()) {
            new AlertDialog.Builder(this)
                    .setTitle("Dispositivo non sicuro")
                    .setMessage("Il dispositivo è rootato. L'applicazione non può essere eseguita per motivi di sicurezza.")
                    .setCancelable(false)
                    .setPositiveButton("Esci", (dialog, which) -> finishAffinity())
                    .show();
            return;
        }

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

        EditText etSearch = findViewById(R.id.et_search);

        // Filtro in tempo reale
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().toLowerCase();
                List<NotePreview> filtered = new ArrayList<>();

                if (query.isEmpty()) {
                    filtered.addAll(allNotes);
                } else {
                    for (NotePreview note : allNotes) {
                        if (note.title.toLowerCase().contains(query)) {
                            filtered.add(note);
                        }
                    }
                }

                adapter.updateNotes(filtered);
            }
        });

        // Nascondi tastiera e rimuovi focus solo alla pressione di "Fine"
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                etSearch.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

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

        EditText etSearch = findViewById(R.id.et_search);
        String query = etSearch.getText().toString().trim().toLowerCase();

        if (!query.isEmpty()) {
            List<NotePreview> filtered = new ArrayList<>();
            for (NotePreview note : allNotes) {
                if (note.title.toLowerCase().contains(query)) {
                    filtered.add(note);
                }
            }
            adapter.updateNotes(filtered);
        }
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
        allNotes.clear();

        File notesDir = new File(getFilesDir(), "notes");
        if (!notesDir.exists()) notesDir.mkdirs();

        File[] files = notesDir.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    String text = EncryptedFileHelper.readEncryptedTextFile(this, f);
                    String title = text.split("\n", 2)[0];
                    NotePreview preview = new NotePreview(f.getName(), title);
                    allNotes.add(preview);
                    notes.add(preview);
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
        } else if (id == R.id.action_attachments) {
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

    public static class NotePreview {
        public final String fileName;
        public final String title;

        public NotePreview(String fileName, String title) {
            this.fileName = fileName;
            this.title = title;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}

