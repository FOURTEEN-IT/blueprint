# Modul: frontend-react-vite

Liefert das React/Vite-Frontend-Grundgeruest, dessen Build in ein
Spring-Boot-Jar eingebettet wird, plus die Vitest-Testebene.

## Abhaengigkeit

Braucht das Modul `backend-java-onion` (oder ein gleichwertiges
Gradle-Projekt) -- das Jar-Embedding ueber `processResources` setzt einen
Gradle-Build mit einem Task dieses Namens voraus. Ohne Gradle-Projekt liefert
dieses Modul nur `template/`, der Rest (`build.gradle.fragment.kts`) entfaellt.

## Inhalt

- `template/` -- fachfreies React/Vite-Grundgeruest zum Kopieren in das
  Zielprojekt (z. B. nach `frontend/`): `package.json`, `vite.config.js`,
  `index.html`, `src/main.jsx`, `src/App.jsx`, `tests/setup.js`,
  `tests/requirement.js`, `tests/App.test.jsx`.
- `build.gradle.fragment.kts` -- die Gradle<->npm-Wiring-Tasks
  (`npmInstall`, `npmBuild`, `npmTest`), `-PskipFrontend`, Einbettung des
  Frontend-Builds in `processResources`, Anbindung an `check`.
- `docs/frontend-testebene.md` -- Beschreibung der Frontend-Testebene
  (Vitest + Testing Library, "Serverdaten rein, sichtbare Ausgabe raus").

## Checkliste

- [ ] `{{PROJECT_NAME}}` gesetzt -- in `template/package.json`
  (Paketname) und `template/index.html`/`template/src/App.jsx`
  (Titel/Ueberschrift).
- [ ] `{{BACKEND_DEV_PORT}}` gesetzt -- in `template/vite.config.js`,
  Dev-Proxy-Ziel.
- [ ] Proxy-Block in `template/vite.config.js` an die tatsaechlichen
  Endpunkte angepasst -- er ist nur ein Beispiel (`/ws` fuer WebSocket,
  `/api` fuer REST); ein Projekt ohne WebSocket laesst den `ws`-Block
  einfach weg.
- [ ] `template/` nach `frontend/` im Zielprojekt kopiert und
  `build.gradle.fragment.kts` an dessen `build.gradle.kts` angehaengt
  (oder per `apply(from = ...)` eingebunden), bevor die Platzhalter
  ersetzt werden.

## Lokal entwickeln

Zwei Terminals, wie im generierten Projekt auch:

```bash
# Terminal 1: Backend allein, ohne Frontend-Build
gradle bootRun -PskipFrontend

# Terminal 2: Frontend mit Live-Reload, proxyt /ws bzw. /api ans Backend
cd frontend && npm run dev
```

Produktionsnah (Frontend-Build eingebettet):

```bash
gradle bootJar && java -jar build/libs/{{PROJECT_NAME}}-0.1.0.jar
```

## Offene Erweiterung

Ein Abdeckungs-Gate wie ein `abdeckungFrontend`-Task (gleicht
frontend-markierte Anforderungs-IDs gegen `requirement(...)`-Aufrufe ab)
ist nicht Teil dieses Moduls, weil es ein eigenes, projektspezifisches
Anforderungsdokument mit fester Tabellenform voraussetzt. Siehe
`docs/frontend-testebene.md` und `build.gradle.fragment.kts` fuer die
Begruendung.
