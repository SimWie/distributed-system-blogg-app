# Distributed Systems Blog App

Ein Web-API-Backend für ein Blog-System, entwickelt im Rahmen des Kurses zu verteilten Systemen an der HFTM. Das Projekt basiert auf [Quarkus](https://quarkus.io/) und demonstriert typische Konzepte verteilter Systeme wie synchrone HTTP-Kommunikation, Authentifizierung und Cloud Deployment.

**Schnellstart:** Das Backend gibt es als Container-Image auf GitHub Packages (`ghcr.io/simwie/blog-backend:latest`). Wie man es zusammen mit Keycloak startet und mit HTTPie testet, steht unter [System als Container starten](#system-als-container-starten-ghcr).

---

## REST-Design

Die Applikation bietet drei Ressourcen an: `users`, `blogs` und (verschachtelt) `blogs/{blogId}/comments`. Alle Endpunkte tauschen JSON aus.

### Users

| Methode | Pfad         | Beschreibung                  | Erwartetes Verhalten |
|---------|--------------|--------------------------------|-----------------------|
| GET     | `/users`     | Alle Benutzer abrufen          | `200 OK` mit JSON-Array (auch leer) |
| GET     | `/users/{id}`| Einzelnen Benutzer abrufen     | `200 OK` mit Benutzer, oder `404 Not Found` mit Fehlermeldung, falls `id` unbekannt |
| POST    | `/users`     | Neuen Benutzer anlegen         | `201 Created` mit dem angelegten Benutzer (inkl. generierter `id`), oder `400 Bad Request`, falls `username` leer/fehlt |
| PUT     | `/users/{id}`| Benutzernamen aktualisieren    | `200 OK` mit aktualisiertem Benutzer, oder `404 Not Found`, falls `id` unbekannt, oder `400 Bad Request` bei ungültigen Daten |
| DELETE  | `/users/{id}`| Benutzer löschen               | `204 No Content`, oder `404 Not Found`, falls `id` unbekannt |

### Blogs

| Methode | Pfad          | Beschreibung                              | Erwartetes Verhalten |
|---------|---------------|--------------------------------------------|-----------------------|
| GET     | `/blogs`      | Alle Blog-Posts abrufen                    | `200 OK` mit JSON-Array. Optionaler Query-Parameter `search` filtert Posts, deren Titel den Suchbegriff enthält (Gross-/Kleinschreibung wird ignoriert), z. B. `GET /blogs?search=quarkus` |
| GET     | `/blogs/{id}` | Einzelnen Blog-Post abrufen                | `200 OK` mit Post, oder `404 Not Found` mit Fehlermeldung, falls `id` unbekannt |
| POST    | `/blogs`      | Neuen Blog-Post erstellen (Request-Body: `NewBlogDTO`). Autor ist der angemeldete Benutzer | `201 Created` mit dem angelegten Post als `BlogDTO` (inkl. generierter `id`), oder `400 Bad Request`, falls `title`/`content` leer ist |
| PUT     | `/blogs/{id}` | Titel/Inhalt eines Posts aktualisieren (Request-Body: `UpdateBlogDTO`). Nur Autor oder Admin | `200 OK` mit aktualisiertem Post als `BlogDTO`, oder `403 Forbidden`, falls nicht Autor/Admin, oder `404 Not Found`, falls `id` unbekannt, oder `400 Bad Request` bei ungültigen Daten |
| DELETE  | `/blogs/{id}` | Post löschen (inkl. zugehöriger Kommentare). Nur Autor oder Admin | `204 No Content`, oder `403 Forbidden`, falls nicht Autor/Admin, oder `404 Not Found`, falls `id` unbekannt |

### Comments (verschachtelt unter einem Blog)

| Methode | Pfad                                   | Beschreibung                | Erwartetes Verhalten |
|---------|-----------------------------------------|------------------------------|-----------------------|
| GET     | `/blogs/{blogId}/comments`              | Alle Kommentare eines Posts abrufen | `200 OK` mit JSON-Array, oder `404 Not Found`, falls `blogId` unbekannt |
| POST    | `/blogs/{blogId}/comments?userId={id}`  | Neuen Kommentar zu einem Post erstellen (Request-Body: `NewCommentDTO`) | `201 Created` mit dem angelegten Kommentar als `CommentDTO`. Der Query-Parameter `userId` bestimmt den Autor. `404 Not Found`, falls `blogId` oder `userId` unbekannt sind; `400 Bad Request`, falls `userId` fehlt oder `content` leer/fehlt |
| DELETE  | `/blogs/{blogId}/comments/{commentId}`  | Kommentar löschen            | `204 No Content`, oder `404 Not Found`, falls `commentId` unbekannt |

### Fehler-Responses

Alle `404`- und `400`-Antworten liefern zusätzlich einen JSON-Body mit einer aussagekräftigen Meldung im Format:

```json
{ "message": "Blog with id 42 not found" }
```

Bei ungültigen Eingaben (Bean Validation, z. B. leerer Titel) enthält `message` die konkret verletzte Regel, z. B. `"title must not be blank"`.

---

## DTOs und Mapping

Die Endpunkte für `blogs` und `comments` geben die JPA-Entities nicht mehr direkt nach aussen. Stattdessen gibt es für Requests und Responses eigene DTOs im Package `ch.hftm.ds.blog.boundry.dto`. Ein DTO legt fest, welche Felder der Client senden darf bzw. zu sehen bekommt.

### Welche Daten geben die DTOs frei?

| DTO | Richtung / Verwendung | Felder | Validierung |
|-----|------------------------|--------|-------------|
| `NewBlogDTO` | Request, `POST /blogs` | `title`, `content` (der Autor kommt aus dem Access-Token) | `title`/`content`: `@NotBlank` |
| `UpdateBlogDTO` | Request, `PUT /blogs/{id}` | `title`, `content` | `title`/`content`: `@NotBlank` |
| `BlogDTO` | Response aller `/blogs`-Endpunkte | `id`, `title`, `content`, `authorId`, `authorUsername` | – |
| `NewCommentDTO` | Request, `POST /blogs/{blogId}/comments` | `content` (Autor via Query-Parameter `userId`) | `content`: `@NotBlank` |
| `CommentDTO` | Response aller Kommentar-Endpunkte | `id`, `content`, `blogId`, `authorId`, `authorUsername` | – |

Was dadurch **nicht** nach aussen gelangt bzw. vom Client **nicht** gesetzt werden kann:

- Die `id` ist in keinem Request-DTO enthalten. Sie wird immer vom Server vergeben, eine mitgeschickte `id` wird ignoriert.
- Beim Update (`UpdateBlogDTO`) kann nur Titel und Inhalt geändert werden, nicht der Autor.
- Die Kommentar-Liste eines Blogs (`Blog.comments`) und das vollständige User-Objekt (inkl. dessen Blog-Liste) sind nicht Teil der Responses. Vom Autor werden nur `authorId` und `authorUsername` ausgeliefert, vom Kommentar nur die `blogId`.

Beispiel `POST /blogs`:

```json
// Request (NewBlogDTO)
{ "title": "Mein Post", "content": "Inhalt" }

// Response 201 (BlogDTO)
{ "id": 4, "title": "Mein Post", "content": "Inhalt", "authorId": 1, "authorUsername": "alice" }
```

Beispiel `POST /blogs/1/comments?userId=2`:

```json
// Request (NewCommentDTO)
{ "content": "Guter Post!" }

// Response 201 (CommentDTO)
{ "id": 7, "content": "Guter Post!", "blogId": 1, "authorId": 2, "authorUsername": "bob" }
```

Die Endpunkte `/users` verwenden aktuell weiterhin die Entity `User` (nur `id` und `username`), siehe [Bewusst noch ausgeklammert](#bewusst-noch-ausgeklammert).

### Wie funktioniert das Mapping?

Das Mapping ist bewusst von Hand geschrieben (kein MapStruct o. Ä.) und findet ausschliesslich in der Boundary-Schicht (`*Resource`-Klassen und DTOs) statt. Die Service-Schicht kennt keine DTOs.

- **Entity -> Response-DTO:** statische Factory-Methoden `BlogDTO.from(Blog)` und `CommentDTO.from(Comment)`. Sie kopieren die freigegebenen Felder und lösen die Beziehungen zu flachen Feldern auf (`blog.getAuthor().getId()` -> `authorId`).
- **Request-DTO -> Entity:** `NewBlogDTO.toEntity(User author)` und `NewCommentDTO.toEntity(Blog blog, User author)`. Referenzen können nicht direkt aus dem DTO erzeugt werden. Beim Blog bestimmt die Resource den Autor über den Benutzernamen im Access-Token (`UserService.getOrCreateUser`, legt den lokalen `User` bei Bedarf an). Beim Kommentar lädt sie Blog und Autor über den jeweiligen Service und gibt `404 Not Found` zurück, wenn einer davon nicht existiert.
- **Update:** Die Resource übergibt Titel und Inhalt aus dem `UpdateBlogDTO` als einfache Werte an `BlogService.updateBlog(id, title, content)`. Die bestehende Entity wird geändert, der Autor bleibt unverändert.
- **Validierung:** Die Bean-Validation-Annotationen liegen auf den Request-DTOs (`@Valid` an den Resource-Parametern). Ein leerer Body führt über `@NotNull` ebenfalls zu `400 Bad Request`. Die Fehlermeldung wird wie bisher vom `ValidationExceptionMapper` im Format `{ "message": "..." }` geliefert.

Die DTOs sind einfache Klassen mit Gettern/Settern (analog zu den Entities), damit JSON-B und Bean Validation ohne Sonderbehandlung funktionieren.

---

## CORS

Damit ein Browser-Frontend (z. B. Angular mit `ng serve` auf `http://localhost:4200`) das API von einem anderen Origin aus aufrufen darf, ist CORS in `src/main/resources/application.properties` konfiguriert:

```properties
quarkus.http.cors.enabled=true
quarkus.http.cors.origins=${CORS_ALLOWED_ORIGINS:http://localhost:4200}
quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS
quarkus.http.cors.headers=Content-Type,Accept,Authorization
quarkus.http.cors.access-control-max-age=1H
```

- **Erlaubter Origin:** Standardmässig nur `http://localhost:4200`. Für andere Umgebungen (z. B. das Cloud Deployment) wird die Umgebungsvariable `CORS_ALLOWED_ORIGINS` gesetzt (mehrere Origins kommagetrennt), ohne den Code zu ändern.
- **Kein Wildcard (`*`):** Das API ist noch nicht durch Authentifizierung geschützt. Mit `*` könnte jede beliebige Webseite im Browser eines Benutzers Anfragen an das API stellen. Deshalb ist die Origin-Liste explizit.
- **Methoden/Header:** Nur die vom API verwendeten HTTP-Methoden und die Header `Content-Type`, `Accept` und `Authorization` (für die geplante JWT-Authentifizierung) sind freigegeben.
- **Test:** `CorsTest` prüft den Preflight-Request (`OPTIONS`) für den erlaubten Origin sowie, dass ein fremder Origin keine CORS-Header erhält.
- **Grenze von CORS:** CORS schützt nur Browser-Zugriffe. Tools wie `curl` oder Postman senden keinen `Origin`-Header und sind davon nicht betroffen. CORS ersetzt deshalb keine Authentifizierung.

---

## Sicherheit: OWASP API Security Top 10 (2023)

Einschätzung der Risiken aus der [OWASP API Security Top 10 (2023)](https://owasp.org/API-Security/editions/2023/en/0x11-t10/) für dieses API. Stand: aktueller Projektstand, mit Rollenprüfung über Keycloak (`quarkus-oidc`) und Besitzprüfung für Blogs.

| Risiko | Relevanz | Status | Begründung |
|--------|----------|--------|------------|
| API1: Broken Object Level Authorization | hoch | **Teilweise** | Blogs: Der Autor kommt aus dem Token, und nur der Autor oder ein Admin darf einen Blog ändern oder löschen, sonst `403` (getestet in `SecurityTest`). Offen bei Kommentaren: Der Autor kommt noch aus dem Query-Parameter `userId`, jeder `user` kann jeden Kommentar löschen, und `DELETE /blogs/{blogId}/comments/{commentId}` prüft nicht, ob der Kommentar zum angegebenen Blog gehört. |
| API2: Broken Authentication | hoch | **Abgedeckt** | Schreibende Endpunkte und `/users` verlangen ein gültiges Access-Token von Keycloak (`quarkus-oidc`), sonst `401`. Nur die lesenden Blog- und Kommentar-Endpunkte sind bewusst öffentlich. |
| API3: Broken Object Property Level Authorization (Excessive Data Exposure / Mass Assignment) | mittel | **Teilweise** | Für Blogs und Kommentare durch die DTOs abgedeckt: Es werden nur whitelisted Felder gelesen und ausgeliefert, die `id` ist nicht setzbar (getestet in `creatingBlog_ignoresClientSuppliedId`). Offen: `/users` verwendet noch die Entity. Aktuell ohne sensible Felder, aber `POST /users` akzeptiert z. B. eine `id` vom Client. |
| API4: Unrestricted Resource Consumption | mittel | **Teilweise / offen** | Quarkus begrenzt die Grösse von Request-Bodies standardmässig (konfigurierbar über `quarkus.http.limits.max-body-size`). Offen: keine Pagination bei `GET /blogs` und `/users`, keine Längenbeschränkung für `title`/`content`, kein Rate Limiting. |
| API5: Broken Function Level Authorization | hoch | **Abgedeckt** | Rollenprüfung mit `@RolesAllowed`: Die Benutzerverwaltung (`/users`) ist nur für `admin`, ein `user` erhält `403` (getestet in `SecurityTest`). |
| API6: Unrestricted Access to Sensitive Business Flows | gering | **Offen** | Blogs und Kommentare können automatisiert in grosser Zahl angelegt werden (Spam). Ein Login ist zwar nötig, es gibt aber kein Rate Limiting. Das Risiko ist bei einem Blog ohne Geschäftsprozesse (Zahlung o. Ä.) gering. |
| API7: Server Side Request Forgery | nicht relevant | **Nicht relevant** | Das API ruft keine URLs auf, die vom Client geliefert werden. |
| API8: Security Misconfiguration | mittel | **Teilweise** | Abgedeckt: CORS ist auf konkrete Origins/Methoden/Header beschränkt (kein `*`), Fehler (`400`/`404`) werden einheitlich ohne Stacktrace geliefert. Offen: nur HTTP (kein TLS), keine Security-Header, die OpenAPI-Beschreibung unter `/q/openapi` sollte für Produktion geprüft werden, Produktions-Datenbank und deren Zugangsdaten sind noch nicht konfiguriert (Dev Services gibt es nur im Dev-/Test-Modus). |
| API9: Improper Inventory Management | gering | **Teilweise** | Die Endpunkte sind in diesem README und über `quarkus-smallrye-openapi` dokumentiert. Es gibt aber keine API-Versionierung (z. B. `/v1`) und nur eine Umgebung. |
| API10: Unsafe Consumption of APIs | nicht relevant | **Nicht relevant** | Das API konsumiert keine fremden APIs. |

Weitere Beobachtung: Die Suche (`GET /blogs?search=...`) verwendet eine parametrisierte Query (`?1`), eine Injection über den Suchbegriff ist damit nicht möglich.

---

## Berechtigungskonzept

Grundsatz: **Lesen ist öffentlich, Schreiben braucht ein Login, die Benutzerverwaltung ist Admin-Sache.** Wer welche Rolle hat, legt Keycloak fest. Das Backend (`quarkus-oidc`) prüft das Access-Token, liest die Rollen aus `realm_access.roles` und prüft sie mit `@RolesAllowed`.

### Benutzer-Rollen

| Rolle | Wer | Darf zusätzlich zu „öffentlich" |
|-------|-----|----------------------------------|
| *(keine / anonym)* | Besucher ohne Login | Blogs und Kommentare lesen |
| `user` | Angemeldeter Benutzer | Blogs erstellen, ändern und löschen, Kommentare schreiben und löschen |
| `admin` | Administrator | Wie `user`, plus Benutzer verwalten (`/users`) |

Die Rollen sind in Keycloak nicht hierarchisch. Deshalb werden in `@RolesAllowed` beide Rollen aufgezählt (`@RolesAllowed({"user", "admin"})`), damit auch ein Admin ohne zusätzliche `user`-Rolle schreiben darf.

Der Realm `blog` ist in [`keycloak/blog-realm.json`](keycloak/blog-realm.json) definiert. Dieselbe Datei verwenden die Quarkus Dev Services (Dev- und Test-Modus, `quarkus.keycloak.devservices.realm-path`) und der eigene Keycloak-Container aus `docker-compose.yml`. Sie enthält die Rollen `user`/`admin`, den Client `backend-service` (Secret `secret`) und diese Benutzer:

| Benutzer | Passwort | Rollen |
|----------|----------|--------|
| `alice` | `alice` | `admin`, `user` |
| `bob` | `bob` | `user` |

Zwei Einstellungen in `application.properties` sind dabei wichtig, sonst startet Keycloak zwar, aber der Realm fehlt bzw. ist über HTTP nicht erreichbar (Fehler `HTTPS required`, im Backend dann `500 OIDC Server is not available`):

- `quarkus.keycloak.devservices.disable-https=true`: schaltet im `master`-Realm die HTTPS-Pflicht ab, damit Dev Services sich als Admin anmelden und den Realm anlegen kann.
- `"sslRequired": "none"` in `keycloak/blog-realm.json`: dasselbe für den Realm `blog`, damit Backend und Clients Tokens per HTTP holen können.

Keycloak verlangt HTTPS für Anfragen, die es nicht als lokal einstuft. Je nach Docker-Desktop-Netzwerk gelten Anfragen vom Mac in den Container nicht als lokal. Beide Einstellungen sind nur für die lokale Entwicklung gedacht.

### Umsetzung im Code

| Resource | Annotation |
|----------|------------|
| `BlogResource`, `CommentResource` | `@RolesAllowed({"user", "admin"})` auf der Klasse, die GET-Methoden zusätzlich mit `@PermitAll` |
| `UserResource` | `@RolesAllowed("admin")` auf der Klasse |

### Zugriffsregeln

| Methode | Pfad | Erlaubte Rollen |
|---------|------|-----------------|
| GET | `/blogs`, `/blogs?search=...`, `/blogs/{id}` | öffentlich |
| POST | `/blogs` | `user`, `admin` |
| PUT | `/blogs/{id}` | `user` (nur eigene Blogs), `admin` (alle) |
| DELETE | `/blogs/{id}` | `user` (nur eigene Blogs), `admin` (alle) |
| GET | `/blogs/{blogId}/comments` | öffentlich |
| POST | `/blogs/{blogId}/comments` | `user`, `admin` |
| DELETE | `/blogs/{blogId}/comments/{commentId}` | `user`, `admin` |
| GET / POST / PUT / DELETE | `/users`, `/users/{id}` | `admin` |

Es gibt (noch) kein Feld „veröffentlicht/Entwurf“, daher sind alle vorhandenen Blogs öffentlich lesbar.

Antworten bei fehlender Berechtigung:

- **`401 Unauthorized`**: kein oder ungültiges/abgelaufenes Token bei einem geschützten Endpunkt
- **`403 Forbidden`**: gültiges Token, aber falsche Rolle (z. B. `bob` ruft `/users` auf) oder fremder Blog (z. B. `bob` ändert einen Blog von `alice`)

Damit werden die OWASP-Risiken API1 (Besitzprüfung, für Blogs), API2 (Authentifizierung) und API5 (Rollenprüfung) adressiert.

### Autor und Besitzprüfung bei Blogs

- **Autor automatisch:** `POST /blogs` nimmt keine `authorId` mehr entgegen. Der Autor ist immer der angemeldete Benutzer: Die Resource liest den Benutzernamen aus dem Token (`SecurityIdentity`, Claim `preferred_username`) und sucht den passenden lokalen `User`. Gibt es ihn noch nicht, wird er angelegt. Eine trotzdem mitgeschickte `authorId` wird ignoriert.
- **Nur Autor oder Admin:** `PUT` und `DELETE` auf `/blogs/{id}` prüfen, ob der angemeldete Benutzer der Autor des Blogs ist. Andernfalls gibt es `403 Forbidden`. Ein `admin` darf jeden Blog ändern und löschen.
- **Abgleich mit Keycloak:** Die lokalen Benutzer werden nicht mit Keycloak synchronisiert. Sie werden nur über den Benutzernamen zugeordnet (bewusste Vereinfachung).

Noch offen bei **Kommentaren**: Der Autor kommt weiterhin aus dem Query-Parameter `userId`, und jeder `user` darf jeden Kommentar löschen.

---

## Auth-Setup austesten

Das Backend prüft Access-Tokens von Keycloak (`quarkus-oidc`). Es gibt zwei Varianten, Keycloak bereitzustellen:

| | Variante A: Dev Services (Standard) | Variante B: eigener Keycloak-Container |
|---|---|---|
| Start | `./mvnw quarkus:dev` | `docker compose up -d keycloak`, dann Quarkus mit `auth-server-url` starten (oder als [Container](#system-als-container-starten-ghcr)) |
| Keycloak-URL | http://localhost:8088 | http://localhost:8180 |
| Realm-Definition | [`keycloak/blog-realm.json`](keycloak/blog-realm.json) | dieselbe Datei |
| Client | `backend-service` (Secret `secret`) | dito |
| Manuelle Konfiguration | keine | keine (Realm wird beim Start importiert) |

**Voraussetzungen:** Java 21+, Docker, [HTTPie](https://httpie.io/) (`brew install httpie`)

### Variante A: Quarkus Dev Services

Quarkus startet beim Dev-Modus automatisch einen Keycloak-Container und legt den Realm `blog` aus der Realm-Datei an:

```bash
./mvnw quarkus:dev
```

Benutzer:

| Benutzer | Passwort | Rollen | Darf |
|----------|----------|--------|------|
| `alice` | `alice` | `admin`, `user` | alles, auch fremde Blogs ändern/löschen und `/users` verwalten |
| `bob` | `bob` | `user` | Blogs und Kommentare schreiben, nur eigene Blogs ändern/löschen |

Access-Token holen (hier für `bob`, für `alice` Benutzername und Passwort austauschen):

```bash
TOKEN=$(http --form POST :8088/realms/blog/protocol/openid-connect/token \
  client_id=backend-service client_secret=secret grant_type=password \
  username=bob password=bob | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')
```

Beispielaufrufe mit HTTPie (Startdaten: Blogs 1 und 2 gehören `alice`, Blog 3 gehört `bob`):

```bash
# Öffentlich, ohne Token -> 200
http :8080/blogs

# Ohne Token schreiben -> 401
http POST :8080/blogs title="Ohne Login" content="..."

# Blog anlegen -> 201, authorUsername ist der angemeldete Benutzer
http POST :8080/blogs "Authorization: Bearer $TOKEN" title="Mein Post" content="Inhalt"

# Eigenen Blog ändern (bob, Blog 3) -> 200
http PUT :8080/blogs/3 "Authorization: Bearer $TOKEN" title="Neuer Titel" content="Neuer Inhalt"

# Fremden Blog ändern oder löschen (bob, Blog 1 von alice) -> 403
http PUT :8080/blogs/1 "Authorization: Bearer $TOKEN" title="Fremd" content="..."
http DELETE :8080/blogs/1 "Authorization: Bearer $TOKEN"

# Benutzerverwaltung als bob -> 403, als alice -> 200
http :8080/users "Authorization: Bearer $TOKEN"

# Kommentar schreiben -> 201
http POST ":8080/blogs/1/comments?userId=2" "Authorization: Bearer $TOKEN" content="Guter Post!"
```

Erwartete Ergebnisse:

| Aufruf | ohne Token | `bob` | `alice` |
|--------|-----------|-------|---------|
| `GET /blogs` | `200` | `200` | `200` |
| `POST /blogs` | `401` | `201` | `201` |
| `PUT /blogs/3` (Blog von `bob`) | `401` | `200` | `200` |
| `PUT /blogs/1` (Blog von `alice`) | `401` | `403` | `200` |
| `DELETE /blogs/1` (Blog von `alice`) | `401` | `403` | `204` |
| `GET /users` | `401` | `403` | `200` |

### Variante B: eigener Keycloak-Container

Keycloak aus [`docker-compose.yml`](docker-compose.yml) starten (Details siehe [Keycloak lokal starten](#keycloak-lokal-starten-identity-provider)):

```bash
docker compose up -d keycloak
```

Quarkus im Dev-Modus gegen diesen Keycloak starten. Sobald `quarkus.oidc.auth-server-url` gesetzt ist, startet Quarkus keinen Dev-Services-Keycloak. Client-ID und Secret kommen aus `application.properties`:

```bash
./mvnw quarkus:dev -Dquarkus.oidc.auth-server-url=http://localhost:8180/realms/blog
```

Im Prod-Modus (JAR oder Container) ist dieser Keycloak der Standard, siehe [System als Container starten](#system-als-container-starten-ghcr).

Access-Token holen:

```bash
TOKEN=$(http --form POST :8180/realms/blog/protocol/openid-connect/token \
  client_id=backend-service client_secret=secret grant_type=password \
  username=bob password=bob | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')
```

Danach funktionieren dieselben Beispielaufrufe und Benutzer wie bei Variante A.

### Swagger UI

1. App starten (Variante A oder B) und http://localhost:8080/q/swagger-ui öffnen.
2. Ein Access-Token wie oben holen und ausgeben: `echo $TOKEN`
3. In Swagger UI auf **Authorize** klicken, das Token (ohne `Bearer `) einfügen und bestätigen.
4. Geschützte Endpunkte ausführen: Swagger UI sendet das Token als `Authorization: Bearer ...` mit.

Der Button kommt von `quarkus.smallrye-openapi.security-scheme=jwt` in `application.properties`. Swagger UI zeigt das Schloss-Symbol auch bei den öffentlichen `GET`-Endpunkten, weil die Rollen auf Klassenebene definiert sind. Diese funktionieren trotzdem ohne Token (`@PermitAll`).

---

## Keycloak lokal starten (Identity Provider)

Als Identity Provider wird [Keycloak](https://www.keycloak.org/) verwendet. Die Konfiguration ist vollständig im Repository abgelegt, es ist **keine manuelle Konfiguration** in der Admin-Konsole nötig.

- [`docker-compose.yml`](docker-compose.yml): Keycloak `26.8.1` im Dev-Modus auf Port `8180` (8080 ist vom Quarkus-Backend belegt)
- [`keycloak/blog-realm.json`](keycloak/blog-realm.json): Realm-Export, wird beim Start automatisch importiert (`--import-realm`)

**Voraussetzungen:** Docker, [HTTPie](https://httpie.io/) (`brew install httpie`)

### Starten / Stoppen

```bash
docker compose up -d keycloak
```

Nach ca. 20–30 Sekunden ist Keycloak bereit. Prüfen:

```bash
http :8180/realms/blog/.well-known/openid-configuration
```

```bash
docker compose down
```

Da kein Volume für die Datenbank verwendet wird, startet Keycloak nach `docker compose down` wieder mit dem Stand aus `blog-realm.json`. Änderungen in der Admin-Konsole gehen dabei verloren und müssen bei Bedarf in die JSON-Datei übernommen werden.

Nach einer Änderung an `blog-realm.json` reicht `docker compose restart` nicht: Der Realm existiert dann schon im Container und wird nicht erneut importiert. Den Container neu erstellen:

```bash
docker compose up -d --force-recreate keycloak
```

### Was importiert wird

| Element | Wert |
|---------|------|
| Admin-Konsole | http://localhost:8180, Login `admin` / `admin` (nur lokal) |
| Realm | `blog`, `sslRequired: none` (HTTP erlaubt, nur lokal) |
| Realm-Rollen | `user`, `admin` |
| Client | `backend-service`: Confidential Client mit Secret `secret`, Direct Access Grants (Password Grant) aktiviert |
| Benutzer | `alice` / `alice` (`admin`, `user`), `bob` / `bob` (`user`) |

### Hostname und Docker-Netzwerk

In `docker-compose.yml` sind zwei Hostname-Einstellungen gesetzt:

- `KC_HOSTNAME: http://localhost:8180`: Der Issuer (`iss`) im Token ist immer `http://localhost:8180/realms/blog`, egal über welche URL das Token geholt wurde.
- `KC_HOSTNAME_BACKCHANNEL_DYNAMIC: "true"`: Interne Endpunkte wie die Schlüssel (JWKS) bleiben unter der angefragten URL erreichbar, z. B. `http://keycloak:8080/...` aus dem Docker-Netzwerk.

Ohne diese Einstellungen hätte ein Token vom Host den Issuer `localhost:8180`, das Backend im Container würde Keycloak aber unter `keycloak:8080` kennen. Das Backend lehnt das Token dann wegen des falschen Issuers mit `401` ab.

### Rolle im Token prüfen (jwt.io)

Token wie unter [Variante B](#variante-b-eigener-keycloak-container) holen und im Debugger auf [jwt.io](https://jwt.io/) einfügen. Im Payload stehen die Rollen unter `realm_access.roles`. Auszug für `bob`:

```json
{
  "iss": "http://localhost:8180/realms/blog",
  "azp": "backend-service",
  "realm_access": {
    "roles": ["user"]
  },
  "preferred_username": "bob"
}
```

| Benutzer | `realm_access.roles` | `iss` | Gültigkeit (`exp - iat`) |
|----------|----------------------|-------|--------------------------|
| `alice` | `["admin", "user"]` | `http://localhost:8180/realms/blog` | 300 s |
| `bob` | `["user"]` | `http://localhost:8180/realms/blog` | 300 s |

### Stolpersteine

- Keycloak ist erst nach einigen Sekunden bereit. Vorher liefert der Token-Endpunkt `Connection refused` oder `404`.
- `invalid_grant` / `Invalid user credentials`: falsches Passwort. Beim Refresh bedeutet `invalid_grant`, dass das Refresh-Token abgelaufen ist (nach 30 Minuten).
- `invalid_client`: Client-ID oder Secret falsch, oder der Container läuft noch mit einem älteren Realm-Stand (dann neu erstellen, siehe oben).
- Platzhalter wie `<token>` nicht wörtlich übernehmen: zsh interpretiert `<` als Eingabeumleitung.

---

## System als Container starten (GHCR)

Das Backend ist als Container-Image auf GitHub Packages veröffentlicht:

```
ghcr.io/simwie/blog-backend:latest
```

### Umsysteme

| System | Wie es läuft | Konfiguration |
|--------|--------------|---------------|
| Backend | Container `ghcr.io/simwie/blog-backend:latest`, Port `8080` | über Umgebungsvariablen (siehe unten) |
| Keycloak | Container `quay.io/keycloak/keycloak:26.8.1`, Port `8180` | Realm `blog` wird beim Start aus [`keycloak/blog-realm.json`](keycloak/blog-realm.json) importiert, keine manuelle Konfiguration |
| Datenbank | H2 In-Memory **im Backend-Container**, kein eigener Container | Schema wird beim Start angelegt, Startdaten (2 Benutzer, 3 Blogs, 3 Kommentare) werden geladen. Daten gehen beim Neustart verloren |

### Starten

Voraussetzungen: Docker, [HTTPie](https://httpie.io/) (`brew install httpie`), dieses Repository (für `docker-compose.yml` und die Realm-Datei).

```bash
git clone https://github.com/SimWie/distributed-system-blogg-app.git
cd distributed-system-blogg-app
docker compose up -d
```

Damit starten Keycloak und das Backend im selben Docker-Netzwerk. Keycloak braucht ca. 20–30 Sekunden. Prüfen:

```bash
http :8180/realms/blog/.well-known/openid-configuration
http :8080/blogs
```

Logs ansehen und stoppen:

```bash
docker compose logs -f backend
```

```bash
docker compose down
```

Ist Port 8080 schon belegt, kann der Host-Port angepasst werden: `BACKEND_PORT=8081 docker compose up -d`.

### Konfiguration über Umgebungsvariablen

Jede Quarkus-Eigenschaft lässt sich beim Start überschreiben: Grossbuchstaben, `.` und `-` werden zu `_`, z. B. `quarkus.oidc.auth-server-url` → `QUARKUS_OIDC_AUTH_SERVER_URL`. In `docker-compose.yml` gesetzt:

| Variable | Wert | Zweck |
|----------|------|-------|
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `http://keycloak:8080/realms/blog` | Keycloak im Docker-Netzwerk über den Servicenamen (Container erreichen sich nicht über `localhost`) |
| `QUARKUS_OIDC_CLIENT_ID` | `backend-service` | Client im Realm `blog` |
| `QUARKUS_OIDC_CREDENTIALS_SECRET` | `${OIDC_CLIENT_SECRET:-secret}` | Client-Secret, aus der Umgebung oder einer lokalen `.env`-Datei (`OIDC_CLIENT_SECRET=...`), Default `secret` |

Tokens werden vom Host über `localhost:8180` geholt, das Backend spricht Keycloak über `keycloak:8080` an. Damit der Issuer trotzdem passt, setzt `docker-compose.yml` `KC_HOSTNAME` (siehe [Hostname und Docker-Netzwerk](#hostname-und-docker-netzwerk)).

Ohne Docker Compose, mit `docker run` im Netzwerk, das Compose für Keycloak anlegt:

```bash
docker compose up -d keycloak
docker run -i --rm -p 8080:8080 --network distributed-system-blogg-app_default \
  -e QUARKUS_OIDC_AUTH_SERVER_URL=http://keycloak:8080/realms/blog \
  -e QUARKUS_OIDC_CLIENT_ID=backend-service \
  -e QUARKUS_OIDC_CREDENTIALS_SECRET=secret \
  ghcr.io/simwie/blog-backend:latest
```

### Beispielaufrufe mit HTTPie

Benutzer: `alice` / `alice` (Rollen `admin`, `user`) und `bob` / `bob` (Rolle `user`). Startdaten: Blogs 1 und 2 gehören `alice`, Blog 3 gehört `bob`.

Access-Tokens bei Keycloak holen:

```bash
ALICE=$(http --form -b POST :8180/realms/blog/protocol/openid-connect/token \
  client_id=backend-service client_secret=secret grant_type=password username=alice password=alice \
  | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')

BOB=$(http --form -b POST :8180/realms/blog/protocol/openid-connect/token \
  client_id=backend-service client_secret=secret grant_type=password username=bob password=bob \
  | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')
```

Token übergeben als Header `Authorization: Bearer <token>`:

```bash
# Öffentlich, ohne Token -> 200
http :8080/blogs

# Schreiben ohne Token -> 401
http POST :8080/blogs title="Ohne Login" content="..."

# bob legt einen Blog an -> 201, Autor ist bob (aus dem Token)
http POST :8080/blogs "Authorization: Bearer $BOB" title="Mein Post" content="Inhalt"

# bob ändert den Blog von alice -> 403
http PUT :8080/blogs/1 "Authorization: Bearer $BOB" title="Fremd" content="..."

# bob ändert seinen eigenen Blog -> 200
http PUT :8080/blogs/3 "Authorization: Bearer $BOB" title="Neuer Titel" content="Neuer Inhalt"

# Benutzerverwaltung: bob -> 403, alice -> 200
http :8080/users "Authorization: Bearer $BOB"
http :8080/users "Authorization: Bearer $ALICE"

# alice (admin) löscht den Blog von bob -> 204
http DELETE :8080/blogs/3 "Authorization: Bearer $ALICE"

# Kommentar schreiben -> 201
http POST ":8080/blogs/1/comments?userId=2" "Authorization: Bearer $BOB" content="Guter Post!"
```

Access-Tokens sind 5 Minuten gültig. Danach liefert das Backend `401`, und das Token muss neu geholt werden.

Swagger UI ist auch im Container aktiv: http://localhost:8080/q/swagger-ui (Token über **Authorize** einfügen, siehe [Swagger UI](#swagger-ui)).

### Image bauen und veröffentlichen

Das Image wird über Maven gebaut (Extension `quarkus-container-image-docker`). Registry, Name und Tag stehen in `application.properties` (`ghcr.io/simwie/blog-backend:latest`), das Label `org.opencontainers.image.source` verknüpft das Package mit diesem Repository.

Das Image wird für **`linux/amd64` und `linux/arm64`** gebaut (`quarkus.docker.buildx.platform`), damit es auf Intel/AMD-Rechnern (z. B. Windows) und auf Apple-Silicon-Macs läuft. Docker wählt beim Download automatisch die passende Variante. Ein Multi-Platform-Image wird per `docker buildx` gebaut und direkt in die Registry gepusht (vorher `docker login ghcr.io`, siehe unten):

```bash
./mvnw package -DskipTests -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true
```

Enthaltene Plattformen prüfen:

```bash
docker buildx imagetools inspect ghcr.io/simwie/blog-backend:latest
```

Alternativ manuell mit dem von Quarkus generierten Dockerfile:

```bash
./mvnw package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t ghcr.io/simwie/blog-backend:latest .
```

Anmelden bei GitHub Packages: Es braucht ein Personal Access Token (classic) mit Scope `write:packages` (https://github.com/settings/tokens/new?scopes=write:packages), nicht das GitHub-Passwort. Token kopieren, dann:

```bash
pbpaste | docker login ghcr.io -u SimWie --password-stdin
```

Ein nur lokal gebautes Image (manueller `docker build`, nur für die eigene Architektur) wird mit `docker push ghcr.io/simwie/blog-backend:latest` hochgeladen.

Danach ist das Package unter https://github.com/SimWie?tab=packages sichtbar. Neue Packages sind zuerst privat. Unter *Package settings → Change visibility* auf **Public** stellen, damit `docker compose up` ohne Login funktioniert.

### Ohne Container: JAR im Prod-Modus

`./mvnw package` erzeugt ein Fast-JAR in `target/quarkus-app/`: `quarkus-run.jar` ist nur die Startdatei, `lib/`, `app/` und `quarkus/` gehören dazu und müssen mitkopiert werden. Ein einzelnes JAR mit allem drin (Uber-JAR) gibt es mit `-Dquarkus.package.jar.type=uber-jar`. Für Container ist das Fast-JAR besser, weil Docker die Abhängigkeiten in einem eigenen Layer cachen kann.

```bash
docker compose up -d keycloak
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

Im Prod-Profil (JAR und Container) gilt:

| Einstellung | Wert im Prod-Profil |
|-------------|---------------------|
| Datenbank | H2 In-Memory (`jdbc:h2:mem:blogdb`), Schema `drop-and-create` |
| Keycloak | Default `http://localhost:8180/realms/blog`, Client `backend-service` / `secret` |
| Swagger UI | aktiv (`quarkus.swagger-ui.always-include=true`) |
| Logging | Level `INFO` für `ch.hftm.ds.blog` (siehe [Logging](#logging)) |

---

## Logging

Logging erfolgt durchgängig mit `io.quarkus.logging.Log`. Die Log-Level sind nach Bedeutung eingeteilt:

| Level | Wofür | Beispiel |
|-------|-------|----------|
| `INFO` | Systemzustand und Geschäftsereignisse, inkl. ausführendem Benutzer | `Startup data loaded: 2 users, 3 blogs, 3 comments`, `Blog 4 'Mein Post' created by bob`, `Blog 3 deleted by alice` |
| `WARN` | Verweigerte Zugriffe und ungültige Requests | `Access denied: bob tried to modify blog 1 of another author`, `Rejected invalid request: addBlog.newBlog.title title must not be blank` |
| `ERROR` | Unerwartete Fehler, mit Stacktrace | `Unexpected error: ...` |
| `DEBUG` | Technische Details: Lesezugriffe, Persistenz, 404 | `Returning 3 blogs`, `Blog 999 not found` |

- **Wo:** Geschäftsereignisse loggen die Resources, weil nur dort der angemeldete Benutzer (`SecurityIdentity`) bekannt ist. Die Services loggen technische Details auf `DEBUG`.
- **Fehler:** `UnexpectedExceptionMapper` fängt unerwartete Exceptions ab, loggt sie mit Stacktrace als `ERROR` und liefert dem Client nur `500` mit `{"message": "Internal server error"}`, ohne interne Details. Bewusste HTTP-Fehler (z. B. `404` für unbekannte Pfade) werden unverändert weitergegeben.
- **Konfiguration** (`application.properties`): `INFO` für `ch.hftm.ds.blog`, im Dev-Modus `DEBUG`. Ein anderes Level im Container: `-e QUARKUS_LOG_CATEGORY__CH_HFTM_DS_BLOG__LEVEL=WARN`.
- **Datenschutz:** Es werden Benutzernamen und IDs geloggt, aber keine Tokens, Passwörter oder Blog-Inhalte (nur Titel).

---

## Was die Applikation kann / noch nicht kann

### Aktuell implementiert
- Vollständiges CRUD für `users` und `blogs` (GET, POST, PUT, DELETE)
- Verschachtelte Kommentar-Ressource unter `blogs/{blogId}/comments` (GET, POST, DELETE)
- Persistenz über Hibernate ORM mit Panache (H2, im Dev-/Test-Modus via Quarkus Dev Services, im Prod-Modus In-Memory)
- Prod-Modus als JAR oder Container im Docker-Netzwerk mit eigenem Keycloak, Image auf GitHub Packages, Start des ganzen Systems mit `docker compose up -d` (siehe [System als Container starten](#system-als-container-starten-ghcr))
- Durchgängiges Logging mit `io.quarkus.logging.Log` nach Level (Geschäftsereignisse, verweigerte Zugriffe, Fehler) und zentrales Abfangen unerwarteter Fehler (siehe [Logging](#logging))
- Such-Filter über Query-Parameter (`GET /blogs?search=...`)
- Bean Validation auf je einem Pflichtfeld pro Request (`User.username` sowie die Felder von `NewBlogDTO`, `UpdateBlogDTO`, `NewCommentDTO`) mit `400 Bad Request` und aussagekräftiger Fehlermeldung bei Verletzung
- Einheitliche, aussagekräftige Fehler-Responses (`404`/`400` mit `{"message": "..."}`) für alle Ressourcen
- Testdaten-Seeding beim Start (`StartupData`)
- Eigene Request-/Response-DTOs für `blogs` und `comments` inkl. manuellem Mapping (siehe [DTOs und Mapping](#dtos-und-mapping))
- CORS-Konfiguration, beschränkt auf den Angular-Dev-Server (siehe [CORS](#cors))
- REST-Assured-Tests für Blogs, Kommentare, Berechtigungen und CORS (siehe [Tests](#tests))
- Einschätzung zu den OWASP API Security Risks (siehe [Sicherheit](#sicherheit-owasp-api-security-top-10-2023))
- Geschützte Endpunkte mit `quarkus-oidc` und Keycloak: Lesen öffentlich, Schreiben für `user`/`admin`, Benutzerverwaltung nur für `admin` (siehe [Berechtigungskonzept](#berechtigungskonzept))
- Blog-Autor wird aus dem Access-Token gesetzt, Blogs dürfen nur vom Autor oder einem Admin geändert/gelöscht werden
- Anleitung zum Austesten mit Dev Services oder eigenem Keycloak-Container, HTTPie und Swagger UI (siehe [Auth-Setup austesten](#auth-setup-austesten))
- Reproduzierbares Keycloak-Setup via Docker Compose mit Realm-Import und Test-Benutzern; Access-Tokens werden per HTTPie abgeholt (siehe [Keycloak lokal starten](#keycloak-lokal-starten-identity-provider))

### Bewusst noch ausgeklammert
Gemäss Aufgabenstellung sind folgende Themen erst für kommende Einheiten vorgesehen und daher aktuell noch nicht umgesetzt:
- **DTOs für `/users`**: Die User-Endpunkte nehmen und liefern weiterhin direkt die Entity (`id`, `username`). Das ist vertretbar, solange die Entity keine internen Felder besitzt, sollte aber mit Passwort/Rollen (JWT-Einheit) auf DTOs umgestellt werden.
- **Vertiefte Bean Validation**: Es ist bewusst nur je ein Pflichtfeld pro Entity validiert; weitere Regeln (z. B. Längenbeschränkungen, Formatvalidierung) folgen im nächsten REST-Teil.
- **Besitzprüfung bei Kommentaren**: Bei Blogs umgesetzt, bei Kommentaren kommt der Autor noch aus `userId`, und jeder `user` darf jeden Kommentar löschen (siehe [Autor und Besitzprüfung](#autor-und-besitzprüfung-bei-blogs)).
- **Cloud Deployment**

---

## Projekt im Entwicklungsmodus starten

**Voraussetzungen:** Java 21+, Maven, Docker (für den Keycloak-Container der Quarkus Dev Services)

```bash
# Repository klonen
git clone https://github.com/simwie/distributed-system-blogg-app.git
cd distributed-system-blogg-app

# Im Dev-Modus starten (mit Live Reload)
./mvnw quarkus:dev
```

Die Applikation läuft danach unter `http://localhost:8080`.
Das Quarkus Dev UI ist erreichbar unter `http://localhost:8080/q/dev/`.
Da `application.properties` für Dev und Test keine Datenbankverbindung konfiguriert, startet Quarkus Dev Services automatisch eine H2-Datenbank (In-Memory, ohne Container) sowie einen Keycloak-Container.

---

## Anwendung testen / nutzen

Endpunkte können direkt im Browser, mit `curl` oder einem Tool wie Postman getestet werden. Schreibende Requests brauchen ein Access-Token. Im Dev-Modus holt man es beim Dev-Services-Keycloak (Port `8088`), z. B. für `bob`:

```bash
TOKEN=$(curl -s -X POST http://localhost:8088/realms/blog/protocol/openid-connect/token \
  -d client_id=backend-service -d client_secret=secret \
  -d grant_type=password -d username=bob -d password=bob \
  | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])')
```

Beispiele:

```bash
# Alle Blogs abrufen
curl http://localhost:8080/blogs

# Blogs suchen, deren Titel "quarkus" enthält
curl "http://localhost:8080/blogs?search=quarkus"

# Neuen Blog anlegen (Autor = angemeldeter Benutzer aus dem Token)
curl -X POST http://localhost:8080/blogs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Mein Post","content":"Inhalt"}'

# Eigenen Blog aktualisieren (fremder Blog -> 403, ausser als admin)
curl -X PUT http://localhost:8080/blogs/3 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Neuer Titel","content":"Neuer Inhalt"}'

# Ungültigen Blog anlegen -> 400 mit Fehlermeldung
curl -i -X POST http://localhost:8080/blogs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"","content":"Inhalt"}'

# Kommentar zu Blog 1 von Benutzer 2 hinzufügen
curl -X POST "http://localhost:8080/blogs/1/comments?userId=2" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"Guter Post!"}'
```

Siehe [REST-Design](#rest-design) oben für die vollständige Übersicht aller Methoden, Pfade und Statuscodes.

---

## Tests

Das Backend wird automatisiert mit [REST Assured](https://rest-assured.io/) und `@QuarkusTest` getestet. Die Tests starten die Applikation inkl. H2-Testdatenbank und Keycloak (Dev Services, Docker muss laufen) und senden echte HTTP-Requests.

- `BlogServiceTest` – Unit-/Integrationstests der Service-Klassen (`BlogService`, `UserService`, `CommentService`) direkt gegen die Datenbank.
- `BlogResourceTest` – REST-Assured-Tests gegen `/blogs`. Deckt die Mindestanforderungen des Auftrags ab:
  - `creatingAndUpdatingABlog_returnsUpdatedFields` – **PUT-Request** zur Aktualisierung eines Blogs
  - `searchQueryParam_filtersBlogsByTitle` – **Query-Parameter** `search` zum Filtern von Blogs
  - `gettingUnknownBlog_returnsNotFoundWithMessage` / `updatingUnknownBlog_returnsNotFoundWithMessage` – **aussagekräftige Fehler-Responses** (`404` mit Meldung)
  - `creatingBlogWithBlankTitle_returnsBadRequest` – **Bean Validation** eines ungültigen Requests (leerer Titel -> `400` mit Meldung)
  - `creatingBlog_returnsOnlyDtoFields` / `creatingBlog_ignoresClientSuppliedId` – **DTO-Vertrag**: nur die dokumentierten Felder werden ausgeliefert, eine vom Client mitgeschickte `id` wird ignoriert
- `CommentResourceTest` – REST-Assured-Tests gegen `/blogs/{blogId}/comments` (Anlegen, Auflisten, Löschen, Fehlerfälle `400`/`404`, DTO-Felder).
- `SecurityTest` – prüft das [Berechtigungskonzept](#berechtigungskonzept): anonymes Lesen ist erlaubt, anonymes Schreiben gibt `401`, ein `user` erhält auf `/users` `403`, ein `admin` darf `/users` verwalten. Dazu die Besitzprüfung: Ein `user` wird beim Anlegen automatisch Autor, darf eigene Blogs ändern, erhält bei fremden Blogs `403`, ein `admin` darf fremde Blogs ändern und löschen.
- `CorsTest` – prüft, dass der konfigurierte Origin (`http://localhost:4200`) eine CORS-Freigabe erhält und fremde Origins nicht.
- `TestData` – Hilfsklasse, die Testdaten (User, Blogs) über die REST-API anlegt. Jeder Test verwendet eigene, eindeutig benannte Daten und ist damit unabhängig von der Reihenfolge.

Da die Endpunkte geschützt sind, simulieren die Tests einen eingeloggten Benutzer mit `@TestSecurity` (Extension `quarkus-test-security`), statt echte Tokens bei Keycloak zu holen. `BlogResourceTest` und `CommentResourceTest` laufen als `alice` mit den Rollen `admin` und `user`, `SecurityTest` setzt die Identität pro Testmethode.

Tests ausführen:

```bash
./mvnw test
```

---

## Verwendete Technologien

- [Quarkus](https://quarkus.io/) – Java Framework
- [Quarkus REST JSON-B](https://quarkus.io/guides/rest#json-serialisation) – REST-Endpunkte mit JSON-Serialisierung
- [Hibernate ORM mit Panache](https://quarkus.io/guides/hibernate-orm-panache) – Persistenz
- [Hibernate Validator (Bean Validation)](https://quarkus.io/guides/validation) – Validierung von Eingaben
- [REST Assured](https://rest-assured.io/) – REST-Tests

