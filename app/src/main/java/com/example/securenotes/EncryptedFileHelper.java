package com.example.securenotes;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EncryptedFileHelper {

    private static final String DIR_NAME = "secure_attachments";

    public static void saveEncryptedFile(Context context, String fileName, InputStream inputStream)
            throws Exception {

        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists()) dir.mkdir();

        File file = new File(dir, fileName);

        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (FileOutputStream fos = encryptedFile.openFileOutput()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    public static List<String> listEncryptedFiles(Context context) {
        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists()) return new ArrayList<>();

        String[] files = dir.list();
        return files != null ? Arrays.asList(files) : new ArrayList<>();
    }

    public static void openEncryptedFile(Context context, String fileName) {
        try {
            File dir = new File(context.getFilesDir(), DIR_NAME);
            File encryptedFile = new File(dir, fileName);

            String extension = getFileExtension(fileName);
            File tempFile = new File(context.getCacheDir(), "temp_" + System.currentTimeMillis() + extension);

            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            EncryptedFile ef = new EncryptedFile.Builder(
                    context,
                    encryptedFile,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            try (InputStream is = ef.openFileInput(); FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    tempFile
            );

            String mimeType = getMimeType(extension);
            if (mimeType == null) mimeType = "*/*";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "Apri con"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Impossibile aprire il file", Toast.LENGTH_SHORT).show();
        }
    }


    private static String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot != -1) ? fileName.substring(dot) : "";
    }

    private static String getMimeType(String extension) {
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
    }

    public static boolean deleteEncryptedFile(Context context, String fileName) {
        File dir = new File(context.getFilesDir(), DIR_NAME);
        File file = new File(dir, fileName);
        return file.exists() && file.delete();
    }


}

