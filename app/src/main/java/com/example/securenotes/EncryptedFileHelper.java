package com.example.securenotes;

import android.content.Context;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;


public class EncryptedFileHelper {

    public static void saveEncryptedTextFile(Context context, File file, String text) throws IOException, GeneralSecurityException {
        if (file.exists()) file.delete();

        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                getMasterKey(context),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (FileOutputStream fos = encryptedFile.openFileOutput()) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static String readEncryptedTextFile(Context context, File file) throws IOException, GeneralSecurityException {
        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                getMasterKey(context),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(encryptedFile.openFileInput()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    public static void saveEncryptedBinaryFile(Context context, File file, InputStream in) throws IOException, GeneralSecurityException {
        if (file.exists()) file.delete();

        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                getMasterKey(context),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (OutputStream out = encryptedFile.openFileOutput()) {
            byte[] buffer = new byte[8192]; // Buffer più grande per file lunghi
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    public static File decryptToTempFile(Context context, File encryptedFile, String outputName) throws IOException, GeneralSecurityException {
        EncryptedFile ef = new EncryptedFile.Builder(
                context,
                encryptedFile,
                getMasterKey(context),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        File temp = new File(context.getCacheDir(), outputName);

        try (InputStream in = ef.openFileInput();
             OutputStream out = new FileOutputStream(temp)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        return temp;
    }

    public static InputStream openDecryptedInputStream(Context context, File file) throws IOException, GeneralSecurityException {
        //Alternativa più robusta: scrivi il contenuto temporaneamente su file per letture lunghe
        File temp = decryptToTempFile(context, file, file.getName() + ".tmp");
        return new FileInputStream(temp);
    }

    public static MasterKey getMasterKey(Context context) throws GeneralSecurityException, IOException {
        return new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setUserAuthenticationRequired(false) //Fondamentale per backup automatici
                .build();
    }
}

