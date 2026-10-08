# Lumina Visuals

Moderne Visual-Overhaul-Mod für **Minecraft Java Edition 26.1.2** (Fabric, clientseitig).
Ein dunkles In-Game-Menü mit Suche, Kategorien, Themes und Presets, dazu Crosshair, Info-HUD und Bildschirm-Effekte.

> **Wichtiger Hinweis:** Dieses Projekt wurde in einer Umgebung ohne Internetzugang geschrieben und
> **konnte dort nicht kompiliert oder im Spiel getestet werden**. Der Code wurde gegen die offizielle
> Fabric-Dokumentation für 26.1.2 geschrieben. Falls der Build trotzdem Fehler meldet, kopiere die
> Fehlermeldung (siehe „Fehlersuche“) – dann lässt sie sich gezielt beheben.

## Versionen

| Komponente | Version |
|---|---|
| Minecraft | 26.1.2 (unobfuskiert, keine Mappings nötig) |
| Modloader | **Fabric Loader** 0.19.3 |
| Fabric API | 0.155.2+26.1.2 |
| Fabric Loom | 1.15.5 (`net.fabricmc.fabric-loom`) |
| Gradle | 9.4.1 |
| Java | **25** |

**Warum Fabric?** Fabric ist für reine Client-Mods der leichteste und am schnellsten aktualisierte Loader,
hat eine offizielle Anleitung für 26.1 und bringt für Keybinds und HUD-Rendering fertige APIs mit.

## Mod installieren

1. Fabric Loader für 26.1.2 installieren (https://fabricmc.net/use/).
2. Die **Fabric API** (0.155.2+26.1.2 oder neuer für 26.1.2) in den `mods`-Ordner legen.
3. `lumina-visuals-1.0.0.jar` ebenfalls in den `mods`-Ordner legen.
4. Minecraft mit dem Fabric-Profil starten (Java 25).

## Bedienung

* Menü öffnen/schließen: Taste **Ü** (auf deutscher Tastatur). Im Spiel muss kein Bildschirm offen sein.
* Maus: Klicken, Slider ziehen, Mausrad scrollt (über einem Slider ändert es den Wert). Rechtsklick auf eine Zeile setzt sie zurück.
* Tastatur: `↑/↓` Zeile wählen, `←/→` Wert ändern (mit Shift größere Schritte), `Enter`/`Leertaste` umschalten, `Entf` zurücksetzen,
  `Tab` nächste Seite, `Strg+F` Suche, irgendeine Buchstabentaste startet ebenfalls die Suche, `Esc` schließen.

## Keybind ändern

Das Menü-Kürzel ist ein normaler Minecraft-Keybind mit dem Namen **„Open Visual Settings“**:

`Optionen → Steuerung → Tastenbelegung → Lumina Visuals → Open Visual Settings`

(Der Standard „Ü“ ist technisch die Taste links neben „+“; intern heißt sie `GLFW_KEY_LEFT_BRACKET`.)

## Konfiguration

* Alle Einstellungen: `.minecraft/config/lumina/config.json` (werden automatisch gespeichert).
* Eigene Presets: `.minecraft/config/lumina/presets/<Name>.json`
* Eigene Themes: `.minecraft/config/lumina/themes/<Name>.json`

## Was die Mod enthält

* **Menü:** Dark-UI, Seiten + Unterkategorien, Suche, Tastaturbedienung, Fade/Scale/Slide beim Öffnen, weiches Scrollen,
  Hover-/Toggle-/Dropdown-Animationen, Farbwähler (Hue/Sat/Bri + Palette), Benachrichtigungen.
* **HUD:** Info-Panel (FPS, Koordinaten, Blickrichtung, Geschwindigkeit, Uhrzeit) mit Position, Skalierung, Transparenz,
  Hintergrund, Rundung, Abstand und eigenen Farben.
* **Crosshair:** 7 Formen, Größe, Dicke, Lücke, Farbe, Transparenz, Outline, Rainbow, dynamisches Spreizen, Hitmarker.
* **Effekte:** Farb-Tint, Vignette, Cinematic-Balken, eigener Schadens-Flash.
* **Player:** View Bobbing, Damage Tilt, FOV-/Distortion-Effekte (spiegeln die Vanilla-Optionen).
* **GUI/Colors:** UI-Scale, Deckkraft, Hintergrund-Abdunklung, Eckenradius, Schatten, Akzent-, Hintergrund-, Sekundär- und Textfarben.
* **Themes:** Dark, Midnight, Minimal, Glass, AMOLED, Light + eigene Themes speichern/überschreiben/löschen.
* **Presets:** Minimal, Clean, Cinematic, Competitive, Vanilla+, Ultra + eigene Presets („Custom“) speichern/laden/überschreiben/löschen.
* **Animationen:** globaler Regler *Animation Speed* (0 = aus), „Reduce Motion“ in Accessibility.
* **Performance:** Lite Mode, einstellbares Aktualisierungsintervall des Panels, abschaltbare Schatten/Rundungen, Kreis-Qualität.
  Es gibt nur einen Tick-Handler (Tastenabfrage); gezeichnet wird nur, was aktiv ist.

### Bewusst noch nicht enthalten

Für diese Wünsche wären Mixins in interne Render-Klassen von 26.1 nötig, die ich ohne Compiler nicht
verlässlich prüfen konnte. Sie sind **nicht** als Platzhalter im Menü, sondern schlicht nicht vorhanden:
Custom Sky/Fog/Wolken, Wetter-Visuals, Block-Break-/Highlight-Animationen, Item-/Hand-Animationen,
Partikel-Anpassungen, echter Hintergrund-Blur. Die Architektur ist darauf vorbereitet (siehe „Erweitern“).

## Bauen

### Variante A – GitHub (empfohlen, nichts zu installieren)

1. Neues Repository auf GitHub anlegen und den **Inhalt** dieses Ordners hochladen
   (nicht den ZIP selbst, sondern die entpackten Dateien; Ordner `.github` muss mit hoch).
2. Im Repository auf **Actions** → Workflow **Build** wählen. Er startet automatisch bei jedem Upload.
3. Wenn der Lauf grün ist: auf den Lauf klicken → unten bei **Artifacts** `lumina-visuals-jar` herunterladen.
   In der ZIP liegt `lumina-visuals-1.0.0.jar`.

### Variante B – lokal

Voraussetzungen: **JDK 25** und **Gradle 9.4+** (nur einmalig, um den Wrapper zu erzeugen).

```bash
gradle wrapper --gradle-version 9.4.1   # erzeugt gradle/wrapper/gradle-wrapper.jar
./gradlew build                          # Windows: gradlew.bat build
```

Die fertige Mod liegt danach in `build/libs/lumina-visuals-1.0.0.jar`
(die Datei mit `-sources` im Namen wird nicht benötigt).

Zum Testen im Entwicklungs-Client: `./gradlew runClient`.

## Fehlersuche

* **Build-Fehler `cannot find symbol` / `does not override`:** In der Datei steht, welche Zeile betroffen ist.
  Kopiere die ersten ~30 Zeilen der Fehlerausgabe (GitHub: Actions → Lauf → Schritt „Build“).
* **„Unsupported class file major version“ / Java-Fehler:** Es wird Java 25 benötigt.
* **Spiel startet nicht, Fabric meldet fehlende Abhängigkeit:** Fabric API für 26.1.2 in den `mods`-Ordner legen.

## Projektstruktur

```
src/main/java/dev/lumina/
  client/   LuminaClient (Einstieg), LuminaKeys (Keybind), VanillaBridge (Vanilla-Optionen)
  config/   ConfigManager (JSON), LibraryService (Presets/Themes), BuiltIns (eingebaute Werte)
  setting/  Settings (ALLE Optionen), Bool/Number/Enum/ColorSetting, Category
  anim/     Anim (weiche Werte), Motion (globaler Animationsschalter)
  gui/      LuminaScreen (Menü), Gfx (Zeichenhilfen), UiTheme, Colors, Toasts
  hud/      Crosshair, InfoHud, Effects
```

## Erweitern

* **Neue Option:** in `Settings.java` eine Zeile ergänzen (`bool(...)`, `num(...)`, `en(...)`, `col(...)`) und den Wert dort lesen,
  wo er gebraucht wird. Menü, Suche, Config-Datei, Zurücksetzen und Presets funktionieren automatisch.
* **Neues Preset/Theme:** in `BuiltIns.java` einen Eintrag hinzufügen.
* **Neue Seite:** Eintrag in `Category.java`.

## Lizenz

MIT – siehe `LICENSE`.
