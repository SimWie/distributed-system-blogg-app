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
| POST    | `/blogs`      | Neuen Blog-Post erstellen                  | `201 Created` mit dem angelegten Post (inkl. generierter `id`), oder `400 Bad Request`, falls `title` oder `content` leer/fehlen |
| PUT     | `/blogs/{id}` | Titel/Inhalt eines Posts aktualisieren     | `200 OK` mit aktualisiertem Post, oder `404 Not Found`, falls `id` unbekannt, oder `400 Bad Request` bei ungültigen Daten |
| DELETE  | `/blogs/{id}` | Post löschen (inkl. zugehöriger Kommentare)| `204 No Content`, oder `404 Not Found`, falls `id` unbekannt |

### Comments (verschachtelt unter einem Blog)

| Methode | Pfad                                   | Beschreibung                | Erwartetes Verhalten |
|---------|-----------------------------------------|------------------------------|-----------------------|
| GET     | `/blogs/{blogId}/comments`              | Alle Kommentare eines Posts abrufen | `200 OK` mit JSON-Array, oder `404 Not Found`, falls `blogId` unbekannt |
| POST    | `/blogs/{blogId}/comments?userId={id}`  | Neuen Kommentar zu einem Post erstellen | `201 Created` mit dem angelegten Kommentar. Der Query-Parameter `userId` bestimmt den Autor. `404 Not Found`, falls `blogId` oder `userId` unbekannt sind; `400 Bad Request`, falls `userId` fehlt oder `content` leer/fehlt |
| DELETE  | `/blogs/{blogId}/comments/{commentId}`  | Kommentar löschen            | `204 No Content`, oder `404 Not Found`, falls `commentId` unbekannt |

### Fehler-Responses

Alle `404`- und `400`-Antworten liefern zusätzlich einen JSON-Body mit einer aussagekräftigen Meldung im Format:

```json
{ "message": "Blog with id 42 not found" }
```

Bei ungültigen Eingaben (Bean Validation, z. B. leerer Titel) enthält `message` die konkret verletzte Regel, z. B. `"title must not be blank"`.

---

## Was die Applikation kann / noch nicht kann

### Aktuell implementiert
- Vollständiges CRUD für `users` und `blogs` (GET, POST, PUT, DELETE)
- Verschachtelte Kommentar-Ressource unter `blogs/{blogId}/comments` (GET, POST, DELETE)
- Persistenz über Hibernate ORM mit Panache (MySQL, via Quarkus Dev Services automatisch bereitgestellt)
- Such-Filter über Query-Parameter (`GET /blogs?search=...`)
- Bean Validation auf je einem Pflichtfeld pro Entity (`User.username`, `Blog.title`/`Blog.content`, `Comment.content`) mit `400 Bad Request` und aussagekräftiger Fehlermeldung bei Verletzung
- Einheitliche, aussagekräftige Fehler-Responses (`404`/`400` mit `{"message": "..."}`) für alle Ressourcen
- Testdaten-Seeding beim Start (`StartupData`)

### Bewusst noch ausgeklammert
Gemäss Aufgabenstellung sind folgende Themen erst für kommende Einheiten vorgesehen und daher aktuell noch nicht umgesetzt:
- **DTO-Mapping**: Die Endpunkte nehmen und liefern aktuell direkt die JPA-Entities entgegen (kein separates Request-/Response-DTO). Das führt z. B. dazu, dass beim Anlegen eines Blogs der Autor aktuell nur per `{"author": {"id": ...}}` referenziert werden kann.
- **Vertiefte Bean Validation**: Es ist bewusst nur je ein Pflichtfeld pro Entity validiert; weitere Regeln (z. B. Längenbeschränkungen, Formatvalidierung) folgen im nächsten REST-Teil.
- **Benutzer-Authentifizierung (JWT)**: Alle Endpunkte sind aktuell ungeschützt.
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

# Neuen Blog anlegen (author.id muss ein existierender User sein)
curl -X POST http://localhost:8080/blogs \
  -H "Content-Type: application/json" \
  -d '{"title":"Mein Post","content":"Inhalt","author":{"id":1}}'

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

Die Tests decken sowohl die Service- als auch die REST-Schicht ab:

- `BlogServiceTest` – Unit-/Integrationstests der Service-Klassen (`BlogService`, `UserService`, `CommentService`) direkt gegen die Datenbank.
- `BlogResourceTest` – REST-Assured-Tests gegen den `/blogs`-Endpunkt. Deckt insbesondere die Mindestanforderungen des Auftrags ab:
  - `creatingAndUpdatingABlog_returnsUpdatedFields` – **PUT-Request** zur Aktualisierung eines Blogs
  - `searchQueryParam_filtersBlogsByTitle` – **Query-Parameter** `search` zum Filtern von Blogs
  - `gettingUnknownBlog_returnsNotFoundWithMessage` / `updatingUnknownBlog_returnsNotFoundWithMessage` – **aussagekräftige Fehler-Responses** (`404` mit Meldung)
  - `creatingBlogWithBlankTitle_returnsBadRequest` – **Bean Validation** eines ungültigen Requests (leerer Titel -> `400` mit Meldung)

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
