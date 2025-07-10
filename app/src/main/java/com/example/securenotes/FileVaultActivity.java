package com.example.securenotes;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class FileVaultActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 2001;
    private ListView listView;
    private List<String> fileNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private File attachmentsDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_vault);

        // Risolve i problemi di interfaccia per le versioni di android > 14
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final View rootView = findViewById(android.R.id.content);
            rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets sysBars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom);
                return insets;
            });
        }

        listView = findViewById(R.id.list_view_files);
        attachmentsDir = new File(getFilesDir(), "secure_attachments");
        if (!attachmentsDir.exists()) attachmentsDir.mkdirs();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            File file = new File(attachmentsDir, fileNames.get(position));
            openEncryptedFile(file, fileNames.get(position));
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String fileName = fileNames.get(position);
            confirmDelete(fileName);
            return true;
        });

        findViewById(R.id.btn_add_file).setOnClickListener(v -> pickFile());

        loadFiles();
    }

    private void loadFiles() {
        fileNames.clear();
        File[] files = attachmentsDir.listFiles();
        if (files != null) {
            for (File file : files) fileNames.add(file.getName());
        }
        adapter.notifyDataSetChanged();
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) {
                Toast.makeText(this, "File non valido", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = getFileNameFromUri(uri);
            if (fileName == null) {
                Toast.makeText(this, "Impossibile ottenere il nome del file", Toast.LENGTH_SHORT).show();
                return;
            }

            File newFile = getUniqueFileName(attachmentsDir, fileName);

            try (InputStream in = getContentResolver().openInputStream(uri)) {
                EncryptedFileHelper.saveEncryptedBinaryFile(this, newFile, in);
                Toast.makeText(this, "File aggiunto", Toast.LENGTH_SHORT).show();
                loadFiles();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Errore import file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openEncryptedFile(File encryptedFile, String originalName) {
        AuthHelper.authenticate(this, new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                try {
                    File decryptedTemp = EncryptedFileHelper.decryptToTempFile(FileVaultActivity.this, encryptedFile, originalName);

                    Uri uri = FileProvider.getUriForFile(FileVaultActivity.this, "com.example.securenotes.fileprovider", decryptedTemp);

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, getMimeType(originalName));
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FileVaultActivity.this, "Errore apertura file", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure() {
                Toast.makeText(FileVaultActivity.this, "Autenticazione fallita", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete(String fileName) {
        new AlertDialog.Builder(this)
                .setTitle("Elimina file")
                .setMessage("Vuoi eliminare " + fileName + "?")
                .setPositiveButton("Sì", (dialog, which) -> deleteAttachment(fileName))
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void deleteAttachment(String fileName) {
        File file = new File(attachmentsDir, fileName);
        if (file.exists() && file.delete()) {
            Toast.makeText(this, "File eliminato", Toast.LENGTH_SHORT).show();
            loadFiles();
        } else {
            Toast.makeText(this, "Errore durante l'eliminazione", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (result == null && uri.getPath() != null) {
            int cut = uri.getPath().lastIndexOf('/');
            if (cut != -1) {
                result = uri.getPath().substring(cut + 1);
            }
        }
        return result;
    }

    private File getUniqueFileName(File dir, String originalName) {
        File file = new File(dir, originalName);
        if (!file.exists()) return file;

        String base = originalName;
        String ext = "";
        int dot = base.lastIndexOf('.');
        if (dot != -1) {
            ext = base.substring(dot);
            base = base.substring(0, dot);
        }

        int i = 1;
        while (file.exists()) {
            file = new File(dir, base + "(" + i + ")" + ext);
            i++;
        }
        return file;
    }

    private String getMimeType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "pdf": return "application/pdf";
            case "txt": return "text/plain";
            default: return "*/*";
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}

