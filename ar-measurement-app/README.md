# AR Mätning (AR Measurement App)

En Android-app som använder ARCore för att mäta verkliga avstånd och exportera mätningar till DXF-format.

## Funktioner

- **AR-baserad mätning**: Använd telefonens kamera och ARCore för att sätta mätpunkter i verkligheten
- **3D-positionsspårning**: Exakt spårning av punkter i 3D-rummet
- **Visuell feedback**: Se markerade punkter direkt i AR-vyn
- **DXF-export**: Exportera alla mätpunkter till DXF-filformat för användning i CAD-program

## Krav

### Enhetskompatibilitet
- Android-enhet som stöder ARCore
- Android 7.0 (API level 24) eller senare
- Kamera med autofokus

Lista över ARCore-kompatibla enheter: https://developers.google.com/ar/devices

### Utvecklingsmiljö
- Android Studio Arctic Fox eller senare
- Kotlin 1.9.0+
- Gradle 8.1+
- JDK 17

## Installation

### Öppna i Android Studio

1. Öppna Android Studio
2. Välj "Open an Existing Project"
3. Navigera till `ar-measurement-app`-mappen
4. Klicka "OK"

### Bygga projektet

```bash
./gradlew build
```

### Installera på enhet

1. Anslut din Android-enhet via USB
2. Aktivera "USB Debugging" på enheten
3. I Android Studio, klicka "Run" (grön play-knapp)
4. Välj din enhet från listan

eller via kommandorad:

```bash
./gradlew installDebug
```

## Användning

### Starta mätning

1. Öppna appen
2. Ge kamerabehörighet när den efterfrågas
3. Klicka på "Starta Mätning"

### Markera punkter

1. Vänta tills ARCore har detekterat ytor (plan surfaces)
2. Tryck på skärmen där du vill placera en mätpunkt
3. Första punkten blir startpunkt (P0) - markerad i rött
4. Efterföljande punkter markeras i blått
5. Fortsätt trycka för att lägga till fler punkter

### Exportera till DXF

1. När du är klar med mätningen, klicka "Exportera DXF"
2. Filen sparas i Downloads-mappen med tidsstämpel
3. Filnamn: `measurement_YYYYMMDD_HHMMSS.dxf`

### Återställa mätning

- Klicka "Återställ" för att rensa alla punkter och börja om

## Projektstruktur

```
ar-measurement-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/fatec/armeasure/
│   │   │   ├── MainActivity.kt           # Huvudaktivitet
│   │   │   ├── ArMeasurementActivity.kt  # AR-mätaktivitet
│   │   │   └── utils/
│   │   │       └── DxfGenerator.kt       # DXF-filgenerering
│   │   ├── res/                          # Resurser (layouts, strings)
│   │   └── AndroidManifest.xml           # App-manifest
│   └── build.gradle                      # App-nivå byggkonfiguration
├── build.gradle                          # Projekt-nivå byggkonfiguration
└── settings.gradle                       # Gradle-inställningar
```

## Teknisk implementation

### ARCore Integration

Appen använder ARCore för:
- **Plane Detection**: Upptäcka horisontella och vertikala ytor
- **Pose Tracking**: Spåra telefonens position i 3D-rummet
- **Anchor Creation**: Skapa ankare vid varje mätpunkt för beständig positionering

### Koordinatsystem

- Första punkten (P0) sätts som origo (0, 0, 0)
- Alla efterföljande punkter är relativa till P0
- Koordinater: X (höger/vänster), Y (upp/ner), Z (fram/bak)
- Enhet: Meter

### DXF-filformat

Genererade DXF-filer innehåller:
- **POINT entities**: Varje mätpunkt som en DXF-punkt
- **TEXT entities**: Etiketter för varje punkt (P0, P1, P2...)
- **POLYLINE entity**: 3D-polylinje som kopplar ihop alla punkter
- **Layer**: Alla entiteter placeras i lagret "MEASUREMENTS"

## Beroenden

- **ARCore**: `com.google.ar:core:1.41.0`
- **SceneView**: `io.github.sceneview:arsceneview:0.10.0`
- **AndroidX**: Core, AppCompat, Material Design
- **Kotlin Coroutines**: För asynkron fil-I/O

## Felsökning

### ARCore stöds inte

**Problem**: Meddelande om att ARCore inte stöds
**Lösning**: Kontrollera att din enhet finns på ARCore-kompatibilitetslistan

### Inga ytor detekteras

**Problem**: Kan inte placera punkter
**Lösning**:
- Se till att det finns tillräckligt med ljus
- Rikta kameran mot texturerade ytor (inte blanka eller mörka)
- Rör telefonen långsamt för att hjälpa ARCore kartlägga miljön

### DXF-export misslyckas

**Problem**: Kan inte spara DXF-fil
**Lösning**:
- Kontrollera filbehörigheter
- Se till att det finns utrymme på enheten
- Titta i loggen (Logcat) för felmeddelanden

## Framtida förbättringar

- [ ] Mät och visa avstånd mellan punkter
- [ ] Stöd för olika exportformat (CSV, JSON)
- [ ] Möjlighet att redigera/ta bort enskilda punkter
- [ ] Spara och ladda mätprojekt
- [ ] Kalibrering för ökad noggrannhet
- [ ] Stöd för områdes- och volymberäkningar

## Licens

Detta projekt är skapat för FA-TEC.

## Support

För frågor eller problem, kontakta utvecklingsteamet.
