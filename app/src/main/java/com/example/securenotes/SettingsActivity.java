package com.example.securenotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public class SettingsActivity extends AppCompatActivity {

    private static final int CREATE_BACKUP_REQUEST = 1001;
    private String backupPasswordTemp;

    private EditText etTimeout, etOldPin, etNewPin;

    private static final int RESTORE_BACKUP_REQUEST = 1002;
    private String restorePasswordTemp;


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

            new android.app.AlertDialog.Builder(this)
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
                        startActivityForResult(intent, CREATE_BACKUP_REQUEST);

                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        });

        Button btnRestore = findViewById(R.id.btn_restore);
        btnRestore.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setHint("Password del backup");

            new android.app.AlertDialog.Builder(this)
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
                        startActivityForResult(intent, RESTORE_BACKUP_REQUEST);

                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        });


        findViewById(R.id.btn_choose_theme).setOnClickListener(v -> {
            String[] options = {"Chiaro", "Scuro", "Segui sistema"};
            String[] values = {"light", "dark", "system"};

            // Prendi il tema attuale
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
                        recreate(); // ricarica activity
                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_BACKUP_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                File encryptedBackup = BackupHelper.createEncryptedBackup(this, backupPasswordTemp);

                try (FileInputStream in = new FileInputStream(encryptedBackup);
                     OutputStream out = getContentResolver().openOutputStream(uri, "rwt")) {

                    if (out == null) {
                        toast("Errore: impossibile aprire destinazione");
                        return;
                    }

                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }

                    out.flush();
                    toast("Backup salvato con successo");

                }

            } catch (IOException e) {
                toast("Nessun file da salvare nel backup");
            } catch (Exception e) {
                e.printStackTrace();
                toast("Errore durante il backup");
            }
        }else if (requestCode == RESTORE_BACKUP_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();

                File inputEncrypted = new File(getCacheDir(), "restore_input.aes");

                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(inputEncrypted)) {

                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }

                BackupHelper.restoreEncryptedBackup(this, inputEncrypted, restorePasswordTemp);
                toast("Backup ripristinato con successo");

            } catch (IOException e) {
                toast("Errore: file non leggibile");
            } catch (Exception e) {
                e.printStackTrace();
                toast("Errore durante il ripristino");
            }
        }

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
}
