package com.example.securenotes;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class BackupWorker extends Worker {

    public static final String KEY_FOLDER = "backup_folder_uri";
    public static final String KEY_PASSWORD = "backup_password";

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        String folderUriStr = getInputData().getString(KEY_FOLDER);
        String password = getInputData().getString(KEY_PASSWORD);

        if (folderUriStr == null || password == null) {
            Log.e("BackupWorker", "Dati input mancanti");
            return Result.failure();
        }

        try {
            Uri folderUri = Uri.parse(folderUriStr);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "auto_backup_" + timestamp + ".aes";

            // Crea il backup nella cartella scelta
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(
                    folderUri,
                    DocumentsContract.getTreeDocumentId(folderUri)
            );

            Uri outputUri = DocumentsContract.createDocument(
                    ctx.getContentResolver(),
                    docUri,
                    "application/octet-stream",
                    filename
            );

            if (outputUri == null) throw new Exception("Impossibile creare il file di destinazione");

            OutputStream out = ctx.getContentResolver().openOutputStream(outputUri);
            if (out == null) throw new Exception("OutputStream nullo");

            // Crea il backup temporaneo in formnato ZIP
            File tempZip = new File(ctx.getCacheDir(), "temp_backup.zip");
            AESBackupHelper.createZipOfData(ctx, tempZip);

            // Cripta il backup
            AESBackupHelper.encryptToStream(tempZip, out, password);

            tempZip.delete();
            return Result.success();

        } catch (Exception e) {
            Log.e("BackupWorker", "Errore durante il backup automatico", e);
            return Result.failure();
        }
    }
}



