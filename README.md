# SecureNotes – App Android per Note e File Sensibili

##  Descrizione
SecureNotes è un'app Android progettata per consentire agli utenti di scrivere note personali e archiviare file sensibili (PDF, immagini, documenti), garantendo sicurezza avanzata tramite:

-  Autenticazione biometrica o PIN
-  Criptazione locale end-to-end
-  Timeout di sessione automatico
-  Backup locale criptato
-  Accesso ai file solo previa autenticazione

##  Specifiche Tecniche

- **Target Android:** API 26+ (Android 8.0 Oreo o superiore)
- **Linguaggio:** Java (preferito) o Kotlin
- **IDE:** Android Studio

##  Funzionalità Principali

### 1. Autenticazione Sicura
- Sblocco tramite **biometria** (impronta digitale, riconoscimento facciale) o **PIN fallback**
- Autenticazione richiesta:
  - All'avvio dell'app
  - Dopo inattività
  - Prima di accedere a contenuti sensibili

### 2. Crittografia Locale
- Dati criptati con **Android Keystore API**
- Cifratura simmetrica con **AES/GCM**
- Nessun salvataggio in chiaro su disco

### 3. Archivio File Sicuro
- Caricamento e visualizzazione di file (PDF, immagini, ecc.)
- Memorizzazione protetta con **EncryptedFile API**

### 4. Timeout Automatico
- Sessione configurabile (default: 3 minuti)
- Blocca automaticamente l'app e richiede nuova autenticazione

### 5. Backup Criptato
- Esportazione backup cifrato in `.zip`
- Protezione con password e cifratura AES
- Backup **solo locale**, nessun salvataggio automatico in cloud

##  Architettura e Tecnologie

- **Architettura:** MVVM + Repository
- **Sicurezza:** Jetpack Security (EncryptedSharedPreferences, EncryptedFile)
- **Biometria:** Jetpack Biometric API
- **Database:** Room + SQLCipher (opzionale)
- **Backup:** WorkManager per backup programmati

##  Interfaccia Utente

- **Login:** Biometria o PIN
- **Dashboard:** Lista note criptate, accesso all’archivio file
- **Editor Note:** Editor di testo con salvataggio automatico sicuro
- **Archivio File:** Caricamento protetto, accesso autenticato
- **Impostazioni:** Timeout sessione, backup, cambio PIN

##  Test e Sicurezza

- Nessun dato accessibile nel filesystem o SQLite
- Timeout automatico verificato
- Protezione contro reverse engineering attiva (offuscamento abilitato)
- Test effettuati con strumenti come `apktool`, `jadx`

##  Offuscamento del Codice

L'app viene offuscata nella **build di release** per proteggere le logiche sensibili e rendere più difficile l'analisi del codice tramite reverse engineering.  
Sono stati utilizzati strumenti come **R8/ProGuard**, con configurazioni ottimizzate per mantenere funzionalità critiche e allo stesso tempo oscurare metodi e classi non pubblici.

##  Sicurezza Avanzata

- Protezione delle chiavi con **Android Keystore**
- Firma APK con **chiave privata release**
- Protezioni runtime suggerite:
  - **Rilevamento root**
  - **Tamper detection**
  - **Offuscamento stringhe** tramite JNI o decodifica a runtime

##  Output Richiesto

-  Codice sorgente su GitHub/GitLab
-  APK firmato (release)
-  Documento tecnico (5-6 pagine):
  - Architettura
  - Meccanismi di sicurezza
  - Tecnologie utilizzate
  - Limiti e miglioramenti futuri

##  Tecnologie Consigliate

- `androidx.security.crypto`
- `androidx.biometric`
- `androidx.room` (+ SQLCipher)
- `WorkManager`
- `EncryptedFile`, `EncryptedSharedPreferences`

##  Suggerimenti Extra

- Supporta **modalità scura**
- **Note autodistruttive** (a tempo)
- **Tag e filtri** per organizzazione delle note
- Conferma utente per avvio **backup manuale**
