package com.example.securenotes;

import android.content.Context;
import android.net.Uri;

import java.io.*;
import java.security.GeneralSecurityException;
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

    public static void createEncryptedBackup(Context context, Uri destination, String password) throws Exception {
        File tempZip = new File(context.getCacheDir(), "temp_backup.zip");

        // Crea zip contenente note e allegati decriptati
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {
            addDirToZip(context, new File(context.getFilesDir(), "notes"), "notes/", zos, true);
            addDirToZip(context, new File(context.getFilesDir(), "secure_attachments"), "secure_attachments/", zos, false);
        }

        // Cifra il file zip in output finale
        try (InputStream in = new FileInputStream(tempZip);
             OutputStream out = context.getContentResolver().openOutputStream(destination)) {

            if (out == null) throw new IOException("Output stream nullo");

            encryptStream(in, out, password);
        }

        tempZip.delete();
    }

    public static void restoreEncryptedBackup(Context context, Uri source, String password) throws Exception {
        File tempZip = new File(context.getCacheDir(), "restored_temp.zip");

        // Decifra il file in uno zip temporaneo
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(tempZip)) {

            if (in == null) throw new IOException("Input stream nullo");

            decryptStream(in, out, password);
        }

        // Estrai e salva nei file criptati
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                File outputFile;
                if (entry.getName().startsWith("notes/")) {
                    File notesDir = new File(context.getFilesDir(), "notes");
                    if (!notesDir.exists()) notesDir.mkdirs();

                    outputFile = new File(notesDir, entry.getName().substring("notes/".length()));
                    String content = readTextFromZip(zis);
                    EncryptedFileHelper.saveEncryptedTextFile(context, outputFile, content);

                } else if (entry.getName().startsWith("secure_attachments/")) {
                    File attachmentsDir = new File(context.getFilesDir(), "secure_attachments");
                    if (!attachmentsDir.exists()) attachmentsDir.mkdirs();

                    outputFile = new File(attachmentsDir, entry.getName().substring("secure_attachments/".length()));
                    EncryptedFileHelper.saveEncryptedBinaryFile(context, outputFile, zis);
                }

                zis.closeEntry();
            }
        }

        tempZip.delete();
    }

    private static void addDirToZip(Context context, File dir, String pathInZip, ZipOutputStream zos, boolean isText) throws Exception {
        if (!dir.exists()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            zos.putNextEntry(new ZipEntry(pathInZip + file.getName()));

            if (isText) {
                String text = EncryptedFileHelper.readEncryptedTextFile(context, file);
                zos.write(text.getBytes());
            } else {
                try (InputStream in = EncryptedFileHelper.openDecryptedInputStream(context, file)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
            }

            zos.closeEntry();
        }
    }

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
            byte[] buffer = new byte[8192]; // buffer più grande
            int len;
            while ((len = in.read(buffer)) != -1) {
                cos.write(buffer, 0, len);
            }
        }
    }

    private static void decryptStream(InputStream in, OutputStream out, String password) throws Exception {
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        if (in.read(salt) != salt.length || in.read(iv) != iv.length)
            throw new IOException("Salt o IV non validi");

        SecretKeySpec key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        try (CipherInputStream cis = new CipherInputStream(in, cipher)) {
            byte[] buffer = new byte[8192]; // buffer più grande
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

    private static String readTextFromZip(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        return sb.toString().trim();
    }
}
