# Installation Guide - AR Mätning

## Steg 1: Bygg APK-filen

Det finns flera sätt att bygga APK-filen från detta projekt:

### Metod A: Android Studio (Rekommenderas)

1. **Öppna Android Studio**
2. **File > Open** och välj mappen `ar-measurement-app`
3. Vänta medan Gradle synkroniserar (första gången tar det 2-5 minuter)
4. **Build > Build Bundle(s) / APK(s) > Build APK(s)**
5. När bygget är klart, klicka på "locate" i notifikationen
6. APK-filen finns i: `app/build/outputs/apk/debug/app-debug.apk`

### Metod B: Kommandorad med build-script

Om du har Android SDK installerat:

```bash
cd ar-measurement-app
./build-apk.sh
```

Detta bygger APK:n automatiskt och visar var den finns.

### Metod C: Manuell Gradle-build

```bash
cd ar-measurement-app
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Steg 2: Installera på Android-enhet

### Från dator (rekommenderas)

1. **Aktivera USB Debugging på telefonen:**
   - Gå till Inställningar
   - Om "Utvecklaralternativ" > Aktivera "USB-felsökning"
   - (För att aktivera Utvecklaralternativ: Gå till Om telefon > Tryck 7 gånger på "Build-nummer")

2. **Anslut telefonen via USB**

3. **Installera APK:**

   **Via Android Studio:**
   - Klicka på den gröna "Run"-knappen
   - Välj din enhet

   **Via kommandorad:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

   **Via Gradle:**
   ```bash
   ./gradlew installDebug
   ```

### Direkt på telefonen

1. **Överför APK-filen till telefonen**
   - Via USB-kabel
   - Via e-post
   - Via molntjänst (Google Drive, Dropbox, etc.)

2. **Aktivera installation från okända källor:**
   - Android 8+: Inställningar > Appar > Särskild åtkomst > Installera okända appar
   - Tillåt filhanteraren/webbläsaren att installera appar

3. **Öppna APK-filen** i filhanteraren och tryck "Installera"

---

## Steg 3: Första körningen

1. **Öppna "AR Mätning"-appen**
2. **Tillåt kameraåtkomst** när den frågar
3. **Tryck "Starta Mätning"**
4. **Vänta på ytdetektering** - rikta kameran mot golv/bord
5. **Börja mäta!**

---

## Krav för att köra appen

### Enhetskrav
- **Android 7.0** (Nougat) eller senare
- **ARCore-stöd** - kontrollera på: https://developers.google.com/ar/devices
- **Kamera** med autofokus
- **Minst 2GB RAM** (rekommenderat)

### Populära ARCore-kompatibla enheter
- Samsung Galaxy S8 och senare
- Google Pixel och senare
- OnePlus 5 och senare
- Xiaomi Mi 8 och senare
- Huawei P20 och senare

---

## Felsökning

### "Appen kan inte installeras"
- Kontrollera att du tillåtit installation från okända källor
- Försök avinstallera tidigare version först
- Kontrollera att enheten har minst 100MB ledigt utrymme

### "ARCore stöds inte"
- Din enhet kanske inte stöder ARCore
- Försök uppdatera Google Play Services
- Kontrollera enhetskompatibilitet på Google's lista

### "Hittar inga ytor"
- Se till att det finns god belysning
- Rikta kameran mot texturerade ytor (inte mörka/blanka)
- Rör telefonen långsamt för att hjälpa ARCore kartlägga

### Bygget misslyckas
- Kontrollera att Android SDK är installerat
- Kontrollera att `ANDROID_HOME` är satt korrekt
- Försök `./gradlew clean` och bygg igen
- Öppna i Android Studio för automatisk konfiguration

---

## Support

För frågor eller problem:
- Läs README.md för mer information om appen
- Kontrollera att alla systemkrav är uppfyllda
- Kontakta utvecklingsteamet

---

**Lycka till med mätningen!** 📐📱
