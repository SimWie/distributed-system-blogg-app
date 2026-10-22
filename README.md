# Distributed Systems Blog App

Ein Web-API-Backend für ein Blog-System, entwickelt im Rahmen des Kurses zu verteilten Systemen an der HFTM. Das Projekt basiert auf [Quarkus](https://quarkus.io/) und demonstriert typische Konzepte verteilter Systeme wie synchrone HTTP-Kommunikation, Authentifizierung und Cloud Deployment.

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
| POST    | `/blogs`      | Neuen Blog-Post erstellen (Request-Body: `NewBlogDTO`) | `201 Created` mit dem angelegten Post als `BlogDTO` (inkl. generierter `id`), oder `400 Bad Request`, falls `title`/`content` leer bzw. `authorId` fehlt, oder `404 Not Found`, falls der Autor (`authorId`) unbekannt ist |
| PUT     | `/blogs/{id}` | Titel/Inhalt eines Posts aktualisieren (Request-Body: `UpdateBlogDTO`) | `200 OK` mit aktualisiertem Post als `BlogDTO`, oder `404 Not Found`, falls `id` unbekannt, oder `400 Bad Request` bei ungültigen Daten |
| DELETE  | `/blogs/{id}` | Post löschen (inkl. zugehöriger Kommentare)| `204 No Content`, oder `404 Not Found`, falls `id` unbekannt |

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
| `NewBlogDTO` | Request, `POST /blogs` | `title`, `content`, `authorId` | `title`/`content`: `@NotBlank`, `authorId`: `@NotNull` |
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
{ "title": "Mein Post", "content": "Inhalt", "authorId": 1 }

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
- **Request-DTO -> Entity:** `NewBlogDTO.toEntity(User author)` und `NewCommentDTO.toEntity(Blog blog, User author)`. Referenzen können nicht direkt aus dem DTO erzeugt werden (das DTO enthält nur eine `authorId`). Die Resource lädt daher zuerst den Autor bzw. Blog über den jeweiligen Service und gibt `404 Not Found` zurück, wenn er nicht existiert.
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

Einschätzung der Risiken aus der [OWASP API Security Top 10 (2023)](https://owasp.org/API-Security/editions/2023/en/0x11-t10/) für dieses API. Stand: aktueller Projektstand, ohne Authentifizierung.

| Risiko | Relevanz | Status | Begründung |
|--------|----------|--------|------------|
| API1: Broken Object Level Authorization | hoch | **Offen** | Es gibt keine Benutzer-Authentifizierung und keine Besitzprüfung. Jeder Client kann jeden Blog, Kommentar und User per `id` lesen, ändern und löschen. Zusätzlich prüft `DELETE /blogs/{blogId}/comments/{commentId}` nicht, ob der Kommentar zum angegebenen Blog gehört. Wird mit der JWT-Einheit angegangen (Autor-Prüfung). |
| API2: Broken Authentication | hoch | **Offen** | Alle Endpunkte sind ungeschützt. JWT-Authentifizierung ist für eine kommende Einheit vorgesehen. |
| API3: Broken Object Property Level Authorization (Excessive Data Exposure / Mass Assignment) | mittel | **Teilweise** | Für Blogs und Kommentare durch die DTOs abgedeckt: Es werden nur whitelisted Felder gelesen und ausgeliefert, die `id` ist nicht setzbar (getestet in `creatingBlog_ignoresClientSuppliedId`). Offen: `/users` verwendet noch die Entity. Aktuell ohne sensible Felder, aber `POST /users` akzeptiert z. B. eine `id` vom Client. |
| API4: Unrestricted Resource Consumption | mittel | **Teilweise / offen** | Quarkus begrenzt die Grösse von Request-Bodies standardmässig (konfigurierbar über `quarkus.http.limits.max-body-size`). Offen: keine Pagination bei `GET /blogs` und `/users`, keine Längenbeschränkung für `title`/`content`, kein Rate Limiting. |
| API5: Broken Function Level Authorization | hoch | **Offen** | Es gibt keine Rollen. Auch administrative Aktionen wie `DELETE` stehen jedem Client offen. |
| API6: Unrestricted Access to Sensitive Business Flows | gering | **Offen** | Blogs und Kommentare können automatisiert in grosser Zahl angelegt werden (Spam), da es weder Authentifizierung noch Rate Limiting gibt. Das Risiko ist bei einem Blog ohne Geschäftsprozesse (Zahlung o. Ä.) gering. |
| API7: Server Side Request Forgery | nicht relevant | **Nicht relevant** | Das API ruft keine URLs auf, die vom Client geliefert werden. |
| API8: Security Misconfiguration | mittel | **Teilweise** | Abgedeckt: CORS ist auf konkrete Origins/Methoden/Header beschränkt (kein `*`), Fehler (`400`/`404`) werden einheitlich ohne Stacktrace geliefert. Offen: nur HTTP (kein TLS), keine Security-Header, die OpenAPI-Beschreibung unter `/q/openapi` sollte für Produktion geprüft werden, Produktions-Datenbank und deren Zugangsdaten sind noch nicht konfiguriert (Dev Services gibt es nur im Dev-/Test-Modus). |
| API9: Improper Inventory Management | gering | **Teilweise** | Die Endpunkte sind in diesem README und über `quarkus-smallrye-openapi` dokumentiert. Es gibt aber keine API-Versionierung (z. B. `/v1`) und nur eine Umgebung. |
| API10: Unsafe Consumption of APIs | nicht relevant | **Nicht relevant** | Das API konsumiert keine fremden APIs. |

Weitere Beobachtung: Die Suche (`GET /blogs?search=...`) verwendet eine parametrisierte Query (`?1`), eine Injection über den Suchbegriff ist damit nicht möglich.

---

## Authentifizierung mit Keycloak: Tokens holen

Als Identity Provider wird [Keycloak](https://www.keycloak.org/) eingesetzt. In diesem Schritt wird noch nicht das Backend geschützt, sondern gezeigt, wie ein Client direkt bei Keycloak ein Access-Token (JWT) abholt, es analysiert und über das Refresh-Token erneuert. Mit diesem Token werden später die geschützten API-Methoden aufgerufen (`Authorization: Bearer <token>`).

### Setup

- Keycloak läuft als Docker-Container auf Port `8180`
- Realm: `blog`
- Client: `backend-service` (Client authentication: ON, Direct access grants: aktiviert)
- Benutzer: `alice` / `alice` mit den Realm-Rollen `author` und `admin`
- CLI-Tool [HTTPie](https://httpie.io/) (`brew install httpie`)

Das Client Secret steht in der Keycloak-Admin-Konsole unter **Clients → backend-service → Credentials**. Für die folgenden Befehle wird es als Umgebungsvariable gesetzt:

```bash
export CLIENT_SECRET=<client-secret-aus-keycloak>
```

### 1. Access-Token abholen (Password Grant)

```bash
http -v --form --auth backend-service:$CLIENT_SECRET \
  POST http://localhost:8180/realms/blog/protocol/openid-connect/token \
  username=alice password=alice grant_type=password
```

Antwort von Keycloak (`200 OK`, Tokens gekürzt):

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "scope": "profile email"
}
```

- `access_token`: Base64Url-codiertes JWT zur Autorisierung von API-Aufrufen
- `expires_in`: Gültigkeit des Access-Tokens, 300 s (5 Minuten)
- `refresh_token`: länger gültiges Token (1800 s), um ein neues Access-Token zu beziehen
- `token_type`: `Bearer`

### 2. JWT-Analyse mit jwt.io

Das `access_token` wurde im Debugger auf [jwt.io](https://jwt.io/) dekodiert. Payload (Auszug):

```json
{
  "exp": 1792667757,
  "iat": 1792667457,
  "iss": "http://keycloak:8180/realms/blog",
  "aud": "account",
  "sub": "6339c150-7d7c-4466-ba0f-15cbd70f6423",
  "typ": "Bearer",
  "azp": "backend-service",
  "realm_access": {
    "roles": ["author", "offline_access", "admin", "uma_authorization", "default-roles-bllog"]
  },
  "scope": "profile email",
  "preferred_username": "alice"
}
```

| Claim | Wert | Bedeutung |
|---|---|---|
| `preferred_username` | `alice` | Angemeldeter Benutzer |
| `realm_access.roles` | `author`, `admin`, … | Dem Benutzer zugewiesene Realm-Rollen. Darauf kann das Backend später mit `@RolesAllowed` prüfen |
| `iat` / `exp` | `1792667457` / `1792667757` | Ausstellungs- und Ablaufzeitpunkt (Unix-Timestamp), Differenz = 300 s |
| `iss` | `http://keycloak:8180/realms/blog` | Aussteller des Tokens (Issuer) |
| `azp` | `backend-service` | Client, für den das Token ausgestellt wurde |
| `sub` | `6339c150-…` | Eindeutige ID des Benutzers in Keycloak |

**Beobachtung:** Obwohl die Anfrage über `localhost:8180` lief, lautet der Issuer `http://keycloak:8180/...`, weil Keycloak mit einem festen Hostnamen konfiguriert ist. Ein Backend, das die Tokens validiert, muss den Hostnamen `keycloak` deshalb auflösen können (im Docker-Netzwerk automatisch, auf dem Host über einen `/etc/hosts`-Eintrag `127.0.0.1 keycloak`).

### 3. Neues Access-Token über das Refresh-Token beziehen

```bash
http -v --form --auth backend-service:$CLIENT_SECRET \
  POST http://localhost:8180/realms/blog/protocol/openid-connect/token \
  client_id=backend-service grant_type=refresh_token refresh_token=<refresh_token>
```

Antwort: `200 OK` mit einem neuen `access_token` **und** einem neuen `refresh_token`.

Vergleich mit dem ersten Token:

- `iat` und `exp` sind neu, die Gültigkeit beginnt wieder bei 300 s
- `jti` (Token-ID) ist neu
- `session_state` / `sid` bleiben gleich: Es wurde keine neue Anmeldung erzeugt, sondern die bestehende Keycloak-Session verlängert
- Das Passwort des Benutzers musste nicht erneut angegeben werden
- Für den nächsten Refresh muss das **neue** Refresh-Token verwendet werden

### Stolpersteine

- Die Platzhalter `<...>` aus der Anleitung dürfen nicht wörtlich übernommen werden: zsh interpretiert `<` als Eingabeumleitung (`no such file or directory`).
- `unauthorized_client` / `Invalid client credentials`: falsches oder fehlendes Client Secret.
- `invalid_grant` beim Refresh: Das Refresh-Token ist abgelaufen (nach 30 Minuten). Dann zuerst wieder mit dem Password Grant ein neues Token-Paar holen.

---

## Was die Applikation kann / noch nicht kann

### Aktuell implementiert
- Vollständiges CRUD für `users` und `blogs` (GET, POST, PUT, DELETE)
- Verschachtelte Kommentar-Ressource unter `blogs/{blogId}/comments` (GET, POST, DELETE)
- Persistenz über Hibernate ORM mit Panache (MySQL, via Quarkus Dev Services automatisch bereitgestellt)
- Such-Filter über Query-Parameter (`GET /blogs?search=...`)
- Bean Validation auf je einem Pflichtfeld pro Request (`User.username` sowie die Felder von `NewBlogDTO`, `UpdateBlogDTO`, `NewCommentDTO`) mit `400 Bad Request` und aussagekräftiger Fehlermeldung bei Verletzung
- Einheitliche, aussagekräftige Fehler-Responses (`404`/`400` mit `{"message": "..."}`) für alle Ressourcen
- Testdaten-Seeding beim Start (`StartupData`)
- Eigene Request-/Response-DTOs für `blogs` und `comments` inkl. manuellem Mapping (siehe [DTOs und Mapping](#dtos-und-mapping))
- CORS-Konfiguration, beschränkt auf den Angular-Dev-Server (siehe [CORS](#cors))
- REST-Assured-Tests für Blogs, Kommentare und CORS (siehe [Tests](#tests))
- Einschätzung zu den OWASP API Security Risks (siehe [Sicherheit](#sicherheit-owasp-api-security-top-10-2023))
- Keycloak als Identity Provider eingerichtet (Realm `blog`, Client `backend-service`, Benutzer `alice`); Access- und Refresh-Tokens werden per HTTPie abgeholt und analysiert (siehe [Authentifizierung mit Keycloak](#authentifizierung-mit-keycloak-tokens-holen))

### Bewusst noch ausgeklammert
Gemäss Aufgabenstellung sind folgende Themen erst für kommende Einheiten vorgesehen und daher aktuell noch nicht umgesetzt:
- **DTOs für `/users`**: Die User-Endpunkte nehmen und liefern weiterhin direkt die Entity (`id`, `username`). Das ist vertretbar, solange die Entity keine internen Felder besitzt, sollte aber mit Passwort/Rollen (JWT-Einheit) auf DTOs umgestellt werden.
- **Vertiefte Bean Validation**: Es ist bewusst nur je ein Pflichtfeld pro Entity validiert; weitere Regeln (z. B. Längenbeschränkungen, Formatvalidierung) folgen im nächsten REST-Teil.
- **Benutzer-Authentifizierung (JWT) im Backend**: Keycloak stellt bereits Tokens aus, das Backend validiert sie aber noch nicht (keine `quarkus-oidc`-Extension). Alle Endpunkte sind aktuell ungeschützt.
- **Cloud Deployment**

---

## Projekt im Entwicklungsmodus starten

**Voraussetzungen:** Java 21+, Maven, Docker (für Quarkus Dev Services / die automatisch gestartete MySQL-Testdatenbank)

```bash
# Repository klonen
git clone https://github.com/simwie/distributed-system-blogg-app.git
cd distributed-system-blogg-app

# Im Dev-Modus starten (mit Live Reload)
./mvnw quarkus:dev
```

Die Applikation läuft danach unter `http://localhost:8080`.
Das Quarkus Dev UI ist erreichbar unter `http://localhost:8080/q/dev/`.
Da `application.properties` keine Datenbankverbindung konfiguriert, startet Quarkus Dev Services automatisch einen passenden MySQL-Container.

---

## Anwendung testen / nutzen

Endpunkte können direkt im Browser, mit `curl` oder einem Tool wie Postman getestet werden. Beispiele:

```bash
# Alle Blogs abrufen
curl http://localhost:8080/blogs

# Blogs suchen, deren Titel "quarkus" enthält
curl "http://localhost:8080/blogs?search=quarkus"

# Neuen Blog anlegen (authorId muss ein existierender User sein)
curl -X POST http://localhost:8080/blogs \
  -H "Content-Type: application/json" \
  -d '{"title":"Mein Post","content":"Inhalt","authorId":1}'

# Blog aktualisieren
curl -X PUT http://localhost:8080/blogs/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Neuer Titel","content":"Neuer Inhalt"}'

# Ungültigen Blog anlegen -> 400 mit Fehlermeldung
curl -i -X POST http://localhost:8080/blogs \
  -H "Content-Type: application/json" \
  -d '{"title":"","content":"Inhalt"}'

# Kommentar zu Blog 1 von Benutzer 2 hinzufügen
curl -X POST "http://localhost:8080/blogs/1/comments?userId=2" \
  -H "Content-Type: application/json" \
  -d '{"content":"Guter Post!"}'
```

Siehe [REST-Design](#rest-design) oben für die vollständige Übersicht aller Methoden, Pfade und Statuscodes.

---

## Tests

Das Backend wird automatisiert mit [REST Assured](https://rest-assured.io/) und `@QuarkusTest` getestet. Die Tests starten die Applikation inkl. MySQL-Testdatenbank (Dev Services, Docker muss laufen) und senden echte HTTP-Requests.

- `BlogServiceTest` – Unit-/Integrationstests der Service-Klassen (`BlogService`, `UserService`, `CommentService`) direkt gegen die Datenbank.
- `BlogResourceTest` – REST-Assured-Tests gegen `/blogs`. Deckt die Mindestanforderungen des Auftrags ab:
  - `creatingAndUpdatingABlog_returnsUpdatedFields` – **PUT-Request** zur Aktualisierung eines Blogs
  - `searchQueryParam_filtersBlogsByTitle` – **Query-Parameter** `search` zum Filtern von Blogs
  - `gettingUnknownBlog_returnsNotFoundWithMessage` / `updatingUnknownBlog_returnsNotFoundWithMessage` – **aussagekräftige Fehler-Responses** (`404` mit Meldung)
  - `creatingBlogWithBlankTitle_returnsBadRequest` – **Bean Validation** eines ungültigen Requests (leerer Titel -> `400` mit Meldung)
  - `creatingBlog_returnsOnlyDtoFields` / `creatingBlog_ignoresClientSuppliedId` – **DTO-Vertrag**: nur die dokumentierten Felder werden ausgeliefert, eine vom Client mitgeschickte `id` wird ignoriert
- `CommentResourceTest` – REST-Assured-Tests gegen `/blogs/{blogId}/comments` (Anlegen, Auflisten, Löschen, Fehlerfälle `400`/`404`, DTO-Felder).
- `CorsTest` – prüft, dass der konfigurierte Origin (`http://localhost:4200`) eine CORS-Freigabe erhält und fremde Origins nicht.
- `TestData` – Hilfsklasse, die Testdaten (User, Blogs) über die REST-API anlegt. Jeder Test verwendet eigene, eindeutig benannte Daten und ist damit unabhängig von der Reihenfolge.

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


## Sercret für Backend
I0HSD6787xJyZ2pLDreG0Xg7JtSALeqz