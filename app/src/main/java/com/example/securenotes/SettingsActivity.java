package com.example.securenotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SettingsActivity extends AppCompatActivity {

    private EditText etTimeout, etOldPin, etNewPin;
    private String backupPasswordTemp;
    private String restorePasswordTemp;

    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri destinationUri = result.getData().getData();
                    String password = backupPasswordTemp;

                    if (destinationUri != null && password != null) {
                        getContentResolver().takePersistableUriPermission(
                                destinationUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

                        new Thread(() -> {
                            try {
                                AESBackupHelper.createEncryptedBackup(this, destinationUri, password);
                                runOnUiThread(() -> toast("Backup completato con successo"));
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() -> toast("Errore durante il backup"));
                            }
                        }).start();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> importLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    String password = restorePasswordTemp;

                    if (uri != null && password != null) {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

                        new Thread(() -> {
                            try {
                                AESBackupHelper.restoreEncryptedBackup(this, uri, password);
                                runOnUiThread(() -> toast("Backup ripristinato con successo"));
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() -> toast("Errore durante il ripristino"));
                            }
                        }).start();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etTimeout = findViewById(R.id.et_timeout);
        etOldPin = findViewById(R.id.et_old_pin);
        etNewPin = findViewById(R.id.et_new_pin);

        Button btnSaveTimeout = findViewById(R.id.btn_save_timeout);
        Button btnChangePin = findViewById(R.id.btn_change_pin);
        Button btnBackup = findViewById(R.id.btn_backup);
        Button btnRestore = findViewById(R.id.btn_restore);
        Button btnChooseTheme = findViewById(R.id.btn_choose_theme);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int currentTimeout = prefs.getInt("timeout_minutes", 3);
        etTimeout.setText(String.valueOf(currentTimeout));

        btnSaveTimeout.setOnClickListener(v -> {
            String value = etTimeout.getText().toString().trim();
            if (value.isEmpty()) {
                toast("Inserisci un numero");
                return;
            }

            int timeout = Integer.parseInt(value);
            if (timeout < 1) {
                toast("Il timeout deve essere almeno 1 minuto");
                return;
            }

            prefs.edit().putInt("timeout_minutes", timeout).apply();
            toast("Timeout aggiornato");
        });

        btnChangePin.setOnClickListener(v -> {
            try {
                SharedPreferences securePrefs = getEncryptedPrefs();
                String savedPin = securePrefs.getString("user_pin", null);
                String oldPin = etOldPin.getText().toString().trim();
                String newPin = etNewPin.getText().toString().trim();

                if (savedPin == null || !oldPin.equals(savedPin)) {
                    toast("PIN attuale errato");
                    return;
                }

                if (newPin.length() < 4) {
                    toast("Nuovo PIN troppo corto");
                    return;
                }

                securePrefs.edit().putString("user_pin", newPin).apply();
                toast("PIN aggiornato");
                etOldPin.setText("");
                etNewPin.setText("");
            } catch (Exception e) {
                toast("Errore durante il cambio PIN");
            }
        });

        btnBackup.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setHint("Password per il backup");

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Proteggi backup")
                    .setMessage("Inserisci una password per criptare il backup:")
                    .setView(input)
                    .setPositiveButton("Continua", (dialog, which) -> {
                        String password = input.getText().toString().trim();
                        if (password.length() < 4) {
                            toast("Password troppo corta");
                            return;
                        }

                        backupPasswordTemp = password;

                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.setType("application/octet-stream");
                        intent.putExtra(Intent.EXTRA_TITLE, "secure_notes_backup.aes");
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        exportLauncher.launch(intent);
                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        });

        btnRestore.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setHint("Password del backup");

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Ripristina backup")
                    .setMessage("Inserisci la password del file di backup:")
                    .setView(input)
                    .setPositiveButton("Continua", (dialog, which) -> {
                        String password = input.getText().toString().trim();
                        if (password.length() < 4) {
                            toast("Password troppo corta");
                            return;
                        }

                        restorePasswordTemp = password;

                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        importLauncher.launch(intent);
                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        });

        btnChooseTheme.setOnClickListener(v -> {
            String[] options = {"Chiaro", "Scuro", "Segui sistema"};
            String[] values = {"light", "dark", "system"};

            String current = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("theme_mode", "light");

            int checkedItem = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(current)) {
                    checkedItem = i;
                    break;
                }
            }

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Scegli tema")
                    .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                        ThemeUtils.setTheme(this, values[which]);
                        dialog.dismiss();
                        recreate();
                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        });
    }

    private SharedPreferences getEncryptedPrefs() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                this,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppLifecycleTracker.needsReauth()) {
            AppLifecycleTracker.clearReauthFlag();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}


