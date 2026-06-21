# Distributed System Blog App

Ein Web-API-Backend für ein Blog-System, entwickelt im Rahmen des Kurses zu verteilten Systemen. Das Projekt demonstriert typische Konzepte verteilter Systeme wie synchrone HTTP-Kommunikation, Authentifizierung und Cloud Deployment.

---

## Was die Applikation kann / noch nicht kann

### Aktuell implementiert
- Projekt-Setup und Repository-Struktur

### Geplant / in Arbeit
- Blog-Posts erstellen, lesen, bearbeiten und löschen (CRUD-API)
- Benutzer-Authentifizierung (z.B. JWT)
- Kommentarfunktion
- Cloud Deployment

---

## Projekt im Entwicklungsmodus starten

> Wird laufend aktualisiert, sobald Abhängigkeiten und Setup definiert sind.

```bash
# Repository klonen
git clone https://github.com/simonwiedmer/distributed-system-blogg-app.git
cd distributed-system-blogg-app

# Abhängigkeiten installieren (folgt)

# Entwicklungsserver starten (folgt)
```

---

## Anwendung testen / nutzen

> API-Endpunkte und Beispiel-Requests werden hier dokumentiert, sobald sie implementiert sind.

Geplante Endpunkte (Beispiele):

| Methode | Endpunkt         | Beschreibung              |
|---------|------------------|---------------------------|
| GET     | `/posts`         | Alle Blog-Posts abrufen   |
| POST    | `/posts`         | Neuen Blog-Post erstellen |
| GET     | `/posts/:id`     | Einzelnen Post abrufen    |
| PUT     | `/posts/:id`     | Post bearbeiten           |
| DELETE  | `/posts/:id`     | Post löschen              |
| POST    | `/auth/register` | Benutzer registrieren     |
| POST    | `/auth/login`    | Benutzer einloggen        |
