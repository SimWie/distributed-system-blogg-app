# Distributed Systems Blog App

Ein Web-API-Backend für ein Blog-System, entwickelt im Rahmen des Kurses zu verteilten Systemen an der HFTM. Das Projekt basiert auf [Quarkus](https://quarkus.io/) und demonstriert typische Konzepte verteilter Systeme wie synchrone HTTP-Kommunikation, Authentifizierung und Cloud Deployment.

---

## Was die Applikation kann / noch nicht kann

### Aktuell implementiert
- Projekt-Setup mit Quarkus und REST JSON-B Extension
- Beispiel-Endpunkt (`/hello`)

### Geplant / in Arbeit
- Blog-Posts erstellen, lesen, bearbeiten und löschen (CRUD-API)
- Benutzer-Authentifizierung (JWT)
- Kommentarfunktion
- Cloud Deployment

---

## Projekt im Entwicklungsmodus starten

**Voraussetzungen:** Java 17+, Maven

```bash
# Repository klonen
git clone https://github.com/simwie/distributed-system-blogg-app.git
cd distributed-system-blogg-app

# Im Dev-Modus starten (mit Live Reload)
./mvnw quarkus:dev
```

Die Applikation läuft danach unter `http://localhost:8080`.  
Das Quarkus Dev UI ist erreichbar unter `http://localhost:8080/q/dev/`.

---

## Anwendung testen / nutzen

### Aktuell verfügbare Endpunkte

| Methode | Endpunkt  | Beschreibung        |
|---------|-----------|---------------------|
| GET     | `/hello`  | Test-Endpunkt       |

### Geplante Endpunkte

| Methode | Endpunkt         | Beschreibung              |
|---------|------------------|---------------------------|
| GET     | `/posts`         | Alle Blog-Posts abrufen   |
| POST    | `/posts`         | Neuen Blog-Post erstellen |
| GET     | `/posts/{id}`    | Einzelnen Post abrufen    |
| PUT     | `/posts/{id}`    | Post bearbeiten           |
| DELETE  | `/posts/{id}`    | Post löschen              |
| POST    | `/auth/register` | Benutzer registrieren     |
| POST    | `/auth/login`    | Benutzer einloggen        |

Endpunkte können direkt im Browser, mit `curl` oder einem Tool wie Postman getestet werden.

---

## Verwendete Technologien

- [Quarkus](https://quarkus.io/) – Java Framework
- [Quarkus REST JSON-B](https://quarkus.io/guides/rest#json-serialisation) – REST-Endpunkte mit JSON-Serialisierung
