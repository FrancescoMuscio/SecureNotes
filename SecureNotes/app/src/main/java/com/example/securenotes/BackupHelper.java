package com.example.securenotes;

import android.content.Context;
import android.os.Environment;

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
import java.util.zip.ZipOutputStream;

public class BackupHelper {

    public static File createEncryptedBackup(Context context, String password) throws Exception {
        // 1. Crea zip temporaneo
        File filesDir = new File(context.getFilesDir(), "secure_attachments");
        File zipFile = new File(context.getCacheDir(), "backup_temp.zip");

        boolean zipCreated = false;

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            if (filesDir.exists()) {
                File[] files = filesDir.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            zos.putNextEntry(new ZipEntry(file.getName()));
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = fis.read(buffer)) != -1) {
                                zos.write(buffer, 0, len);
                            }
                            zos.closeEntry();
                            zipCreated = true;
                        }
                    }
                }
            }
        }

        if (!zipCreated) {
            throw new IOException("Nessun file da includere nel backup.");
        }

        // 2. Cripta zip
        File encryptedFile = new File(context.getCacheDir(), "backup_temp_encrypted.aes");
        encryptFile(zipFile, encryptedFile, password);
        zipFile.delete();

        return encryptedFile;
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

            // salva salt + iv all’inizio del file
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

        // 1. Leggi salt e iv
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

        // 2. Estrai ZIP
        File targetDir = new File(context.getFilesDir(), "secure_attachments");
        if (!targetDir.exists()) targetDir.mkdir();

        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(tempZip))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(targetDir, entry.getName());
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
