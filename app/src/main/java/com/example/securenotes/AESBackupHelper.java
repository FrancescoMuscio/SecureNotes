package com.example.securenotes;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.*;
import java.security.SecureRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESBackupHelper {

    private static final String TAG = "AESBackupHelper";

    public static void createEncryptedBackup(Context context, Uri destination, String password) throws Exception {
        File tempZip = new File(context.getCacheDir(), "temp_backup.zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {
            File notesDir = new File(context.getFilesDir(), "notes");
            File attachmentsDir = new File(context.getFilesDir(), "secure_attachments");

            Log.d(TAG, "Backup: note dir = " + notesDir.getAbsolutePath());
            Log.d(TAG, "Backup: attachments dir = " + attachmentsDir.getAbsolutePath());

            int count = 0;

            if (notesDir.exists()) {
                File[] noteFiles = notesDir.listFiles();
                if (noteFiles != null) {
                    for (File file : noteFiles) {
                        String content = EncryptedFileHelper.readEncryptedTextFile(context, file);
                        ZipEntry entry = new ZipEntry("notes/" + file.getName());
                        zos.putNextEntry(entry);
                        zos.write(content.getBytes());
                        zos.closeEntry();
                        count++;
                    }
                }
            }

            if (attachmentsDir.exists()) {
                File[] files = attachmentsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        ZipEntry entry = new ZipEntry("secure_attachments/" + file.getName());
                        zos.putNextEntry(entry);
                        try (InputStream in = EncryptedFileHelper.openDecryptedInputStream(context, file)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = in.read(buffer)) != -1) {
                                zos.write(buffer, 0, len);
                            }
                        }
                        zos.closeEntry();
                        count++;
                    }
                }
            }

            Log.d(TAG, "Totale file nel backup: " + count);

            if (count == 0) {
                throw new IOException("Nessun file da includere nel backup");
            }
        }

        // Cifra e salva
        try (InputStream in = new FileInputStream(tempZip);
             OutputStream out = context.getContentResolver().openOutputStream(destination)) {

            if (out == null) throw new IOException("OutputStream nullo");

            encryptStream(in, out, password);
            Log.d(TAG, "Backup cifrato e salvato");
        }

        tempZip.delete();
    }

    public static void restoreEncryptedBackup(Context context, Uri source, String password) throws Exception {
        File tempZip = new File(context.getCacheDir(), "restored_backup.zip");

        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(tempZip)) {

            if (in == null) throw new IOException("InputStream nullo");

            decryptStream(in, out, password);
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile;
                if (entry.getName().startsWith("notes/")) {
                    File dir = new File(context.getFilesDir(), "notes");
                    if (!dir.exists()) dir.mkdirs();
                    outFile = new File(dir, entry.getName().substring("notes/".length()));
                    String content = readTextFromZip(zis);
                    EncryptedFileHelper.saveEncryptedTextFile(context, outFile, content);
                } else if (entry.getName().startsWith("secure_attachments/")) {
                    File dir = new File(context.getFilesDir(), "secure_attachments");
                    if (!dir.exists()) dir.mkdirs();
                    outFile = new File(dir, entry.getName().substring("secure_attachments/".length()));
                    EncryptedFileHelper.saveEncryptedBinaryFile(context, outFile, zis);
                }
                zis.closeEntry();
            }
        }

        tempZip.delete();
    }

    // Cifratura AES con password
    private static void encryptStream(InputStream in, OutputStream out, String password) throws Exception {
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKeySpec key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        out.write(salt);
        out.write(iv);

        try (CipherOutputStream cos = new CipherOutputStream(out, cipher)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                cos.write(buffer, 0, len);
            }
        }
    }

    private static void decryptStream(InputStream in, OutputStream out, String password) throws Exception {
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        in.read(salt);
        in.read(iv);

        SecretKeySpec key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        try (CipherInputStream cis = new CipherInputStream(in, cipher)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = cis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    private static SecretKeySpec deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static String readTextFromZip(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        return sb.toString().trim();
    }
}

