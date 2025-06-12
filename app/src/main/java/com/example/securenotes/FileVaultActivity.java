package com.example.securenotes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileVaultActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 2001;
    private static final int AUTH_REQUEST_CODE = 999;

    private ListView listView;
    private List<String> fileNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private File attachmentsDir;
    private File fileToOpenAfterAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_vault);

        listView = findViewById(R.id.list_view_files);
        attachmentsDir = new File(getFilesDir(), "secure_attachments");
        if (!attachmentsDir.exists()) attachmentsDir.mkdirs();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            fileToOpenAfterAuth = new File(attachmentsDir, fileNames.get(position));
            AuthHelper.authenticate(this, new AuthHelper.AuthCallback() {
                @Override
                public void onSuccess() {
                    apriFileFisicamente(fileToOpenAfterAuth);
                }

                @Override
                public void onFailure() {
                    Toast.makeText(FileVaultActivity.this, "Autenticazione fallita", Toast.LENGTH_SHORT).show();
                }
            });
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

    private void apriFileFisicamente(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    "com.example.securenotes.fileprovider",
                    file
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(file.getName()));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Errore apertura file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String fn) {
        String ext = fn.substring(fn.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "pdf": return "application/pdf";
            case "txt": return "text/plain";
            default: return "*/*";
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
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1 && result != null) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private File getUniqueFileName(File dir, String originalName) {
        File file = new File(dir, originalName);
        if (!file.exists()) return file;

        String baseName = originalName;
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        }

        int index = 1;
        while (file.exists()) {
            file = new File(dir, baseName + "(" + index + ")" + extension);
            index++;
        }
        return file;
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

            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(newFile)) {

                if (in == null) throw new IOException("InputStream nullo");

                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }

                Toast.makeText(this, "File aggiunto con successo", Toast.LENGTH_SHORT).show();
                loadFiles();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Errore import file", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == AUTH_REQUEST_CODE && resultCode == RESULT_OK && fileToOpenAfterAuth != null) {
            apriFileFisicamente(fileToOpenAfterAuth);
        }
    }
}

