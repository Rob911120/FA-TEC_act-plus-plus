# GitHub Actions - AR Mätning

Detta repo använder GitHub Actions för att automatiskt bygga Android APK-filer.

## Workflows

### 1. Build APK (`build-apk.yml`)

**Triggers:**
- Push till branch som börjar med `claude/ar-measurement-`
- Pull requests som ändrar filer i `ar-measurement-app/`
- Manuell körning via "Actions" > "Build AR Measurement APK" > "Run workflow"

**Vad den gör:**
- Bygger debug APK
- Laddar upp APK som en artifact (nedladdningsbar i 30 dagar)
- Visar APK-storlek och build-information

**Ladda ner APK:**
1. Gå till "Actions" tab
2. Klicka på den senaste workflow-körningen
3. Scrolla ner till "Artifacts"
4. Klicka på "ar-measurement-debug-apk" för att ladda ner

### 2. Release APK (`release-apk.yml`)

**Triggers:**
- Push av git tag som matchar `ar-v*.*.*` (t.ex. `ar-v1.0.0`)

**Vad den gör:**
- Bygger release APK
- Skapar en GitHub Release
- Bifogar APK till releasen
- Genererar release notes

**Skapa en release:**
```bash
git tag ar-v1.0.0
git push origin ar-v1.0.0
```

Eller via GitHub UI:
1. Gå till "Releases"
2. "Draft a new release"
3. Skapa tag: `ar-v1.0.0`
4. Publish release

APK:n byggs och bifogas automatiskt!

## Manuell körning

Du kan köra workflows manuellt:

1. Gå till "Actions" tab
2. Välj workflow i vänstermenyn
3. Klicka "Run workflow"
4. Välj branch och kör

## Felsökning

### Build misslyckas
- Kontrollera build logs under "Actions" > klicka på misslyckad körning
- Vanliga problem:
  - Gradle-konfigurationsfel
  - Felaktiga dependencies
  - Android SDK-versionsfel

### Artifact hittas inte
- Artifacts finns bara i 30 dagar
- Kontrollera att workflow kördes utan fel
- För permanent distribution, använd release-workflow istället

## Tips

**För utveckling:**
- Använd build-workflow för att testa ändringar
- APK:n från artifacts är för testning

**För distribution:**
- Skapa en release med tag för produktion
- Release APK finns permanent i "Releases"-sektionen
- Användare kan enkelt ladda ner senaste versionen

## Status Badges

Lägg till dessa i README för att visa build-status:

```markdown
![Build APK](https://github.com/Rob911120/FA-TEC_act-plus-plus/workflows/Build%20AR%20Measurement%20APK/badge.svg)
```

## Mer information

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android CI/CD with GitHub Actions](https://github.com/android/android-test/tree/main/.github/workflows)
