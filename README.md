# SecureNotes
App Android per note e file sensibili con sicurezza avanzata

Obiettivo dell'app
Creare un'applicazione Android che permetta agli utenti di scrivere note personali e archiviare file sensibili (es. PDF, immagini, documenti), garantendo la massima sicurezza dei dati grazie a:
• Autenticazione biometrica o a PIN
• Criptazione locale end-to-end (note e file)
• Timeout di sessione automatica
• Backup locale criptato
• Accesso ai file solo dopo verifica d'identità
Funzionalità principali
1. Autenticazione Sicura
• Sblocco tramite biometria (impronta, volto) oppure PIN fallback
• Autenticazione richiesta:
o All'avvio dell'app
o Dopo timeout di inattività
o Prima di visualizzare contenuti sensibili
2. Crittografia Dati Locali
• Tutti i dati (note e file) vengono criptati con Android Keystore API
• Utilizzo di AES/GCM per la cifratura simmetrica
• Nessun dato in chiaro salvato nel filesystem/app
3. Archivio Sicuro di File
• Possibilità di caricare e visualizzare documenti (PDF, immagini, ecc.)
• I file sono memorizzati criptati internamente (es. encryptedFile API)
4. Timeout automatico
• Timeout configurabile (default: 3 minuti)
• Alla scadenza, l’app si blocca e richiede nuova autenticazione
5. Backup Criptato
• Backup cifrato esportabile in formato .zip
Protezione del backup con password e criptazione AES
• Nessun salvataggio automatico su cloud (salvataggio locale o manuale)
Architettura suggerita
• MVVM + Repository pattern
• Jetpack Security (EncryptedSharedPreferences, EncryptedFile)
• Jetpack Biometric API
• Room Database con crittografia (SQLCipher se necessario)
• WorkManager per backup pianificati
Interfaccia Utente (UI)
1. Login screen
o Autenticazione biometrica o PIN
2. Dashboard
o Lista note criptate con anteprima
o Accesso a archivio file
3. Editor Note
o Editor testo
o Salvataggio automatico criptato
4. Archivio File
o Caricamento file
o Accesso solo dopo autenticazione
5. Impostazioni
o Timeout sessione
o Esporta backup criptato
o Cambia PIN
