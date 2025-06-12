package com.example.securenotes;



import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // qui puoi anche fare controlli su sessione, autenticazione, ecc.
        startActivity(new Intent(this, LoginActivity.class));
        finish(); // chiude MainActivity, non torna indietro
    }
}

