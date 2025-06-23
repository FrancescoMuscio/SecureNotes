package com.example.securenotes;

import android.content.Context;
import android.net.Uri;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESBackupHelper {

    // ==== Backup manuale ====

    public static void createEncryptedBackup(Context context, Uri destination, String password) throws Exception {
        File tempZip = new File(context.getCacheDir(), "temp_backup.zip");

        createZipOfData(context, tempZip);

        try (InputStream in = new FileInputStream(tempZip);
             OutputStream out = context.getContentResolver().openOutputStream(destination)) {

            if (out == null) throw new IOException("Output stream nullo");

            encryptToStream(tempZip, out, password);
        }

        tempZip.delete();
    }

    public static void restoreEncryptedBackup(Context context, Uri source, String password) throws Exception {
        File tempZip = new File(context.getCacheDir(), "restored_temp.zip");

        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(tempZip)) {

            if (in == null) throw new IOException("Input stream nullo");

            decryptStream(in, out, password);
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile;
                if (entry.getName().startsWith("notes/")) {
                    outFile = new File(context.getFilesDir(), entry.getName());
                    String text = readTextFromZip(zis);
                    EncryptedFileHelper.saveEncryptedTextFile(context, outFile, text);
                } else if (entry.getName().startsWith("secure_attachments/")) {
                    outFile = new File(context.getFilesDir(), entry.getName());
                    EncryptedFileHelper.saveEncryptedBinaryFile(context, outFile, zis);
                }
                zis.closeEntry();
            }
        }

        tempZip.delete();
    }

    // ==== Backup automatico ====

    public static void createZipOfData(Context context, File outputZip) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZip))) {
            zipDirectory(context, new File(context.getFilesDir(), "notes"), "notes/", zos, true);
            zipDirectory(context, new File(context.getFilesDir(), "secure_attachments"), "secure_attachments/", zos, false);
        }
    }

    public static void encryptToStream(File inputFile, OutputStream out, String password) throws Exception {
        try (InputStream in = new FileInputStream(inputFile)) {
            encryptStream(in, out, password);
        }
    }

    // ==== Utilità ZIP ====

    private static void zipDirectory(Context ctx, File dir, String zipPathPrefix, ZipOutputStream zos, boolean isNote) throws IOException, GeneralSecurityException {
        if (!dir.exists()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            zos.putNextEntry(new ZipEntry(zipPathPrefix + file.getName()));
            if (isNote) {
                String text = EncryptedFileHelper.readEncryptedTextFile(ctx, file);
                zos.write(text.getBytes());
            } else {
                try (InputStream in = EncryptedFileHelper.openDecryptedInputStream(ctx, file)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        zos.write(buf, 0, len);
                    }
                }
            }
            zos.closeEntry();
        }
    }

    private static String readTextFromZip(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        return sb.toString().trim();
    }

    // ==== Criptazione ====

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
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}


