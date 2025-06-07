package com.example.securenotes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileVaultActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;

    private ListView listView;
    private List<String> filenames;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_vault);

        Button btnAddFile = findViewById(R.id.btn_add_file);
        listView = findViewById(R.id.list_files);
        filenames = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filenames);
        listView.setAdapter(adapter);

        btnAddFile.setOnClickListener(v -> pickFile());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String fileName = filenames.get(position);
            EncryptedFileHelper.openEncryptedFile(this, fileName);
        });

        refreshFileList();
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                String fileName = getFileName(uri);
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    EncryptedFileHelper.saveEncryptedFile(this, fileName, inputStream);
                    Toast.makeText(this, "File salvato", Toast.LENGTH_SHORT).show();
                    refreshFileList();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Errore durante il salvataggio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getFileName(Uri uri) {
        String name = "allegato_" + System.currentTimeMillis();
        try (var cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex);
                }
            }
        }
        return name;
    }

    private void refreshFileList() {
        filenames.clear();
        filenames.addAll(EncryptedFileHelper.listEncryptedFiles(this));
        adapter.notifyDataSetChanged();
    }
}
