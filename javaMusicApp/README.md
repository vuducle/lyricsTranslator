# JavaMusicApp

Kleines Spring Boot Projekt (Gradle) zur Verwaltung von Benutzern, ToDos und Nachweisen.

Diese README fasst die wichtigsten Run-/Entwickler-Hinweise zusammen und dokumentiert die kürzlich hinzugefügten User‑Profil Endpoints (Passwort ändern, Profilbild hochladen/löschen, Profil abrufen).

## Voraussetzungen

- Java 17+ (oder die in `build.gradle` konfigurierte Java-Version)
- Gradle Wrapper (`./gradlew`) — empfohlen: Verwende den mitgelieferten Wrapper
- (Optional) IntelliJ IDEA zum Entwickeln und Starten

## Anwendung starten

Im Projekt-Root `javaMusicApp`:

```bash
# Build (während Entwicklung ohne Tests)
./gradlew build -x test

# Starten
./gradlew bootRun
```

Standard-Port: `8088` (siehe `src/main/resources/application.properties`).

Wenn `./gradlew` nicht ausführbar ist:

```bash
chmod +x gradlew
```

Oder starte die Hauptklasse `JavaMusicAppApplication` direkt aus IntelliJ.

## Swagger / OpenAPI

- Swagger UI: `http://localhost:8088/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8088/v3/api-docs`

Die Swagger UI unterstützt das Setzen des Bearer-JWT über den `Authorize`-Button.

## Authentifizierung

Registrierung / Login laufen über die Auth-Controller Endpoints (`/api/auth`). Nach Login erhältst du ein `accessToken` (JWT) und `refreshToken`.
Geschützte Endpoints benötigen den Header:

```
Authorization: Bearer <ACCESS_TOKEN>
```

## User‑Profil Endpoints (neu)

- PUT `/api/user/change-password`

  - Body (JSON): `{ "oldPassword": "...", "newPassword": "..." }`
  - Validierung: `newPassword` mindestens 6 Zeichen

- PUT `/api/user/profile-image` (multipart)

  - Feld: `file` (image/\*)
  - Beim Hochladen wird das alte Bild aus dem Storage gelöscht und die URL aktualisiert
  - Vor dem Speichern wird das Bild auf maximal `1024x1024` skaliert (proportional) und als **WebP** (fallback JPEG) mit ~75% Qualität gespeichert

- DELETE `/api/user/profile-image`

  - Löscht das Profilbild vollständig aus dem Dateisystem
  - Setzt `profileImageUrl` auf `null`

- GET `/api/user/profile`
  - Liefert User-Daten inkl. `profileImageUrl`

Alle genannten Endpoints benötigen ein gültiges Bearer‑Token.

## Upload / Storage

Profilbilder werden im Ordner `uploads/profile-images/` im Projekt-Root gespeichert. Die Anwendung stellt diese Dateien unter `/uploads/profile-images/**` bereit (siehe `WebConfig`).

Erstelle das Verzeichnis lokal, falls nicht vorhanden:

```bash
mkdir -p uploads/profile-images
```

Empfehlung: Füge `uploads/` zu `.gitignore` hinzu, damit hochgeladene Dateien nicht ins Repo gelangen.

## cURL Beispiele

Passwort ändern:

```bash
curl -X PUT http://localhost:8088/api/user/change-password \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "oldPassword": "altesPasswort", "newPassword": "neuesPasswort123" }'
```

Profilbild hochladen:

```bash
curl -X PUT http://localhost:8088/api/user/profile-image \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/image.jpg"
```

Profilbild löschen:

```bash
curl -X DELETE http://localhost:8088/api/user/profile-image \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Profil abrufen:

```bash
curl -X GET http://localhost:8088/api/user/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Troubleshooting

- Wenn beim Start zirkuläre Bean-Referenzen auftreten: Es wurde bereits ein eigener Bean für `PasswordEncoder` ausgelagert (`PasswordEncoderConfig`) um Zyklussituationen zu vermeiden.
- Falls Swagger/Auth-Probleme auftreten: prüfe `application.properties` (DB/Redis/JWT Konfiguration) und Log-Level `DEBUG` für `org.example.javamusicapp`.

## Weiteres

- Optional: S3/Cloud-Storage für Profilbilder integrieren
- Optional: Upload-Validierung erweitern (Max-Size, Image-Resizing)

---

Dieses README wurde aktualisiert und dokumentiert die neuen User-API Endpoints sowie Hinweise zum Betrieb.
