package com.example.securenotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.work.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {

    private EditText etTimeout, etOldPin, etNewPin;
    private Switch switchAutoBackup;
    private SharedPreferences prefs;
    private String backupPasswordTemp, restorePasswordTemp;

    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri destinationUri = result.getData().getData();
                    String password = backupPasswordTemp;

                    if (destinationUri != null && password != null) {
                        getContentResolver().takePersistableUriPermission(destinationUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

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
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);

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

    private final ActivityResultLauncher<Intent> folderPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        getContentResolver().takePersistableUriPermission(treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        prefs.edit().putString("auto_backup_folder_uri", treeUri.toString()).apply();
                        toast("Cartella backup impostata");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_force_backup).setOnClickListener(v -> forceBackupNow());


        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        etTimeout = findViewById(R.id.et_timeout);
        etOldPin = findViewById(R.id.et_old_pin);
        etNewPin = findViewById(R.id.et_new_pin);
        switchAutoBackup = findViewById(R.id.switch_auto_backup);

        EditText etBackupInterval = findViewById(R.id.et_backup_interval);
        Button btnSetSchedule = findViewById(R.id.btn_set_backup_schedule);
        Button btnPickFolder = findViewById(R.id.btn_pick_folder);

        etTimeout.setText(String.valueOf(prefs.getInt("timeout_minutes", 3)));

        findViewById(R.id.btn_save_timeout).setOnClickListener(v -> {
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

        findViewById(R.id.btn_change_pin).setOnClickListener(v -> {
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

        findViewById(R.id.btn_backup).setOnClickListener(v -> {
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

        findViewById(R.id.btn_restore).setOnClickListener(v -> {
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

        btnPickFolder.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            folderPickerLauncher.launch(intent);
        });

        btnSetSchedule.setOnClickListener(v -> {
            String intervalStr = etBackupInterval.getText().toString().trim();
            if (intervalStr.isEmpty()) {
                toast("Inserisci un intervallo");
                return;
            }
            int interval = Integer.parseInt(intervalStr);
            if (interval < 15) {
                toast("Intervallo minimo: 15 minuti");
                return;
            }

            String folderUriStr = prefs.getString("auto_backup_folder_uri", null);
            if (folderUriStr == null) {
                toast("Scegli prima una cartella di destinazione");
                return;
            }

            EditText input = new EditText(this);
            input.setHint("Password per backup automatico");

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Proteggi backup automatici")
                    .setMessage("Inserisci la password per i backup automatici:")
                    .setView(input)
                    .setPositiveButton("Conferma", (dialog, which) -> {
                        String password = input.getText().toString().trim();
                        if (password.length() < 4) {
                            toast("Password troppo corta");
                            return;
                        }

                        prefs.edit()
                                .putString("auto_backup_password", password)
                                .putBoolean("auto_backup_enabled", true)
                                .apply();

                        scheduleBackup(Uri.parse(folderUriStr), password, interval);
                    })
                    .setNegativeButton("Annulla", null)
                    .show();
        });
        switchAutoBackup.setChecked(prefs.getBoolean("auto_backup_enabled", false));
        boolean isEnabled = switchAutoBackup.isChecked();
        etBackupInterval.setEnabled(isEnabled);
        btnSetSchedule.setEnabled(isEnabled);
        btnPickFolder.setEnabled(isEnabled);

        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_backup_enabled", isChecked).apply();

            etBackupInterval.setEnabled(isChecked);
            btnSetSchedule.setEnabled(isChecked);
            btnPickFolder.setEnabled(isChecked);

            if (!isChecked) {
                WorkManager.getInstance(this).cancelUniqueWork("auto_backup_work");
                toast("Backup automatico disattivato");
            }
        });



        findViewById(R.id.btn_choose_theme).setOnClickListener(v -> {
            String[] options = {"Chiaro", "Scuro", "Segui sistema"};
            String[] values = {"light", "dark", "system"};
            String current = prefs.getString("theme_mode", "light");

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

    private void scheduleBackup(Uri folderUri, String password, int intervalMinutes) {
        Data inputData = new Data.Builder()
                .putString(BackupWorker.KEY_FOLDER, folderUri.toString())
                .putString(BackupWorker.KEY_PASSWORD, password)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true) // non esegue con batteria bassa
                .setRequiresCharging(false) // opzionale: esegue anche se non in carica
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                BackupWorker.class,
                intervalMinutes, TimeUnit.MINUTES)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "auto_backup_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        );

        toast("Backup automatico pianificato ogni " + intervalMinutes + " minuti");
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

    private void forceBackupNow() {
        String folderUriStr = prefs.getString("auto_backup_folder_uri", null);
        String password = prefs.getString("auto_backup_password", null);

        if (folderUriStr == null || password == null) {
            toast("Cartella o password non impostata");
            return;
        }

        Data inputData = new Data.Builder()
                .putString(BackupWorker.KEY_FOLDER, folderUriStr)
                .putString(BackupWorker.KEY_PASSWORD, password)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(request);

        toast("Backup forzato in esecuzione");
    }

}