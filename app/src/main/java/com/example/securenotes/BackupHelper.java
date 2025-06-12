package com.example.securenotes;

import android.content.Context;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import java.io.*;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupHelper {

    public static File createEncryptedBackup(Context context, String password) throws Exception {
        File zipFile = new File(context.getCacheDir(), "backup_temp.zip");

        boolean zipCreated;
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipCreated = false;

            // Aggiungi allegati
            File attachmentsDir = new File(context.getFilesDir(), "secure_attachments");
            zipCreated |= zipDirectoryToZip(zos, attachmentsDir, "secure_attachments/");

            // Aggiungi note
            File notesDir = new File(context.getFilesDir(), "notes");
            zipCreated |= zipDirectoryToZip(zos, notesDir, "notes/");
        }

        if (!zipCreated) {
            throw new IOException("Nessun file da includere nel backup.");
        }

        // Cripta lo zip
        File encryptedFile = new File(context.getCacheDir(), "backup_temp_encrypted.aes");
        encryptFile(zipFile, encryptedFile, password);
        zipFile.delete();

        return encryptedFile;
    }

    private static boolean zipDirectoryToZip(ZipOutputStream zos, File dir, String zipPathPrefix) throws IOException {
        if (!dir.exists()) return false;

        File[] files = dir.listFiles();
        boolean added = false;

        if (files != null) {
            for (File file : files) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry entry = new ZipEntry(zipPathPrefix + file.getName());
                    zos.putNextEntry(entry);
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                    added = true;
                }
            }
        }

        return added;
    }

    private static void encryptFile(File inputFile, File outputFile, String password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        byte[] key = factory.generateSecret(spec).getEncoded();
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher);
             FileInputStream fis = new FileInputStream(inputFile)) {

            fos.write(salt);
            fos.write(iv);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, len);
            }
        }
    }

    public static void restoreEncryptedBackup(Context context, File encryptedFile, String password) throws Exception {
        File tempZip = new File(context.getCacheDir(), "restored_temp.zip");

        // Decrittografia
        try (FileInputStream fis = new FileInputStream(encryptedFile)) {
            byte[] salt = new byte[16];
            byte[] iv = new byte[16];
            fis.read(salt);
            fis.read(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            byte[] key = factory.generateSecret(spec).getEncoded();

            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            try (CipherInputStream cis = new CipherInputStream(fis, cipher);
                 FileOutputStream fos = new FileOutputStream(tempZip)) {

                byte[] buffer = new byte[4096];
                int len;
                while ((len = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
        }

        // Estrazione
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile;
                if (entry.getName().startsWith("secure_attachments/")) {
                    File dir = new File(context.getFilesDir(), "secure_attachments");
                    if (!dir.exists()) dir.mkdirs();
                    outFile = new File(dir, entry.getName().substring("secure_attachments/".length()));
                } else if (entry.getName().startsWith("notes/")) {
                    File dir = new File(context.getFilesDir(), "notes");
                    if (!dir.exists()) dir.mkdirs();
                    outFile = new File(dir, entry.getName().substring("notes/".length()));
                } else {
                    continue;
                }

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }

                zis.closeEntry();
            }
        }

        tempZip.delete();
    }
}

