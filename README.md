# lyricsTranslator (monorepo)

Dieses Repository enthält das Backend `JavaMusicApp` (Spring Boot) und zukünftig ein Frontend (wird noch erstellt).

## Inhalt

- `javaMusicApp/` — Spring Boot Backend (siehe `javaMusicApp/README.md` für Details)
- `frontend/` — (noch nicht implementiert)

## Konfiguration

Bevor Sie die Anwendung zum ersten Mal starten, müssen Sie eine `.env`-Datei für die Umgebungsvariablen erstellen.

1.  Stellen Sie sicher, dass Sie sich im Hauptverzeichnis des Projekts befinden (dem Ordner, der `docker-compose.yml` enthält).

2.  Kopieren Sie die Vorlagedatei aus dem `javaMusicApp`-Verzeichnis in das **Hauptverzeichnis** und benennen Sie sie in `.env` um:
    ```sh
    cp javaMusicApp/.env.template .env
    ```

3.  Öffnen Sie die neu erstellte `.env`-Datei im Hauptverzeichnis und ersetzen Sie die Platzhalter (z. B. `<your_postgres_password>`) durch Ihre tatsächlichen Anmeldeinformationen und Geheimnisse.

Diese `.env`-Datei wird von `docker-compose` automatisch erkannt und zur Konfiguration der Anwendung verwendet. Sie sollte **niemals** in Git eingecheckt werden.

## Schnellstart Backend

Siehe `javaMusicApp/README.md` für ausführliche Anweisungen, Beispiele und API-Dokumentation.

## Hinweise

- Das Frontend wird in einem separaten `frontend/`-Verzeichnis angelegt, wenn es umgesetzt wird.
- Hochgeladene Dateien werden lokal in `uploads/profile-images/` gespeichert; um zu verhindern, dass diese ins Repo gelangen, ist `uploads/` in `.gitignore` eingetragen.

## Kontakt

Bei Fragen wende dich an den Projektverantwortlichen.
