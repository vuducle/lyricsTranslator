# User-Profil API Endpoints

## Übersicht

Die folgenden PUT-Methoden wurden zum User-Controller hinzugefügt:

### 1. Passwort ändern

**Endpoint:** `PUT /api/user/change-password`

**Authentifizierung:** Bearer Token erforderlich

**Request Body:**

```json
{
  "oldPassword": "aktuellesPasswort123",
  "newPassword": "neuesPasswort456"
}
```

**Response (Erfolg):**

```
Status: 200 OK
Body: "Passwort erfolgreich geändert"
```

**Response (Fehler):**

```
Status: 400 Bad Request
Body: "Altes Passwort ist nicht korrekt"
```

**Validierung:**

- `oldPassword`: Pflichtfeld
- `newPassword`: Pflichtfeld, mindestens 6 Zeichen

---

### 2. Profilbild hochladen

**Endpoint:** `PUT /api/user/profile-image`

**Authentifizierung:** Bearer Token erforderlich

**Content-Type:** `multipart/form-data`

**Request Parameter:**

- `file`: Bilddatei (MultipartFile)

**Response (Erfolg):**

```json
{
  "id": "309a03f1-2711-4515-a557-9a429816583f",
  "username": "johndoe",
  "name": "John Doe",
  "email": "john@example.com",
  "profileImageUrl": "/uploads/profile-images/309a03f1-2711-4515-a557-9a429816583f_abc123.jpg"
}
```

**Validierung:**

- Nur Bilddateien erlaubt (Content-Type muss `image/*` sein)
- Datei darf nicht leer sein
- Das alte Profilbild wird automatisch aus dem Storage gelöscht
- Vor dem Speichern wird das Bild als JPEG mit ~75% Qualität komprimiert (Datei wird ggf. konvertiert)
- Vor dem Speichern wird das Bild auf maximal `1024x1024` skaliert (proportional) und als **WebP** (fallback JPEG) mit ~75% Qualität gespeichert

---

### 3. Profilbild löschen

**Endpoint:** `DELETE /api/user/profile-image`

**Authentifizierung:** Bearer Token erforderlich

**Response (Erfolg):**

```json
{
  "id": "309a03f1-2711-4515-a557-9a429816583f",
  "username": "johndoe",
  "name": "John Doe",
  "email": "john@example.com",
  "profileImageUrl": null
}
```

**Funktionalität:**

- Löscht das Profilbild vollständig aus dem Storage (Dateisystem)
- Setzt die `profileImageUrl` des Users auf `null`
- Gibt eine Fehlermeldung zurück, wenn das Löschen fehlschlägt

---

### 4. User-Profil abrufen

**Endpoint:** `GET /api/user/profile`

**Authentifizierung:** Bearer Token erforderlich

**Response:**

```json
{
  "id": "309a03f1-2711-4515-a557-9a429816583f",
  "username": "johndoe",
  "name": "John Doe",
  "email": "john@example.com",
  "profileImageUrl": "/uploads/profile-images/309a03f1-2711-4515-a557-9a429816583f_abc123.jpg"
}
```

---

## Beispiele mit cURL

### Passwort ändern:

```bash
curl -X PUT http://localhost:8080/api/user/change-password \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "altesPasswort",
    "newPassword": "neuesPasswort123"
  }'
```

### Profilbild hochladen:

```bash
curl -X PUT http://localhost:8080/api/user/profile-image \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/image.jpg"
```

### Profilbild löschen:

```bash
curl -X DELETE http://localhost:8080/api/user/profile-image \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Profil abrufen:

```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Implementierungsdetails

### Dateistruktur:

- **Controller:** `/src/main/java/org/example/javamusicapp/controller/userController/UserController.java`
- **DTOs:**
  - `/src/main/java/org/example/javamusicapp/controller/userController/dto/ChangePasswordRequest.java`
  - `/src/main/java/org/example/javamusicapp/controller/userController/dto/UserResponse.java`
- **Service:** `/src/main/java/org/example/javamusicapp/service/auth/UserService.java` (erweitert)
- **Model:** `/src/main/java/org/example/javamusicapp/model/User.java` (Feld `profileImageUrl` hinzugefügt)
- **Config:** `/src/main/java/org/example/javamusicapp/config/WebConfig.java` (für statische Ressourcen)

### Upload-Verzeichnis:

Profilbilder werden gespeichert in: `uploads/profile-images/`

Format: `{userId}_{randomUUID}.{extension}`

### Sicherheit:

- Alle Endpoints sind durch JWT-Authentifizierung geschützt
- User kann nur sein eigenes Passwort ändern und Profilbild hochladen/löschen
- Passwortänderung erfordert Validierung des alten Passworts
- Nur Bilddateien sind für den Upload erlaubt

### Storage-Management:

- Beim Hochladen eines neuen Profilbilds wird das alte automatisch aus dem Storage gelöscht
- Beim expliziten Löschen des Profilbilds (`DELETE /api/user/profile-image`) wird die Datei vollständig aus dem Dateisystem entfernt
- Die `profileImageUrl` wird in beiden Fällen entsprechend aktualisiert (neue URL oder `null`)
