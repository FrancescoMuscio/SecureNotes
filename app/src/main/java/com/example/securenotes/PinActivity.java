package com.example.securenotes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;


public class PinActivity extends AppCompatActivity {

    private EditText etPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        etPin = findViewById(R.id.et_pin);
        Button btnConfirm = findViewById(R.id.btn_confirm_pin);

        btnConfirm.setOnClickListener(v -> checkPin());
    }

    private void checkPin() {
        try {
            SharedPreferences prefs = getEncryptedPrefs();
            String savedPin = prefs.getString("user_pin", null);
            String input = etPin.getText().toString().trim();

            if (savedPin == null) {
                Toast.makeText(this, "Nessun PIN salvato", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            if (input.equals(savedPin)) {
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "PIN errato", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore lettura PIN", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private SharedPreferences getEncryptedPrefs() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                this,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
