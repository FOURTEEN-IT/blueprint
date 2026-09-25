# deployment-fly

Liefert den generischen Deployment-Baustein: Fly.io als Hosting (ein
Container, eine Instanz, optionales Fly-Volume für persistenten Zustand),
GitHub Actions für Build/Release/Deploy. Kein Fachcode — nur das
Gerüst.

## Umfang

| Teil | Zweck |
|---|---|
| `template/fly.toml.template` | Fly.io-Konfiguration, inklusive Kommentaren zu den Architekturentscheidungen Ein-Instanz-Betrieb und Verzicht auf einen vorgeschalteten Proxy |
| `template/Dockerfile.template` | Multi-Stage-Dockerfile für Frontend- und Backend-Build |
| `template/.github/workflows/build.yml.template` | CI-Workflow; E2E- und Commit-Format-Schritte sind als auskommentierte Beispiele hinterlegt, weil sie projekteigene Skripte/Ebenen voraussetzen, die dieses Modul nicht mitliefert |
| `template/.github/workflows/release.yml.template` | CD-Workflow; ein optionaler GitHub-Pages-Job für Testberichte ist **nicht** enthalten, weil er eine bestimmte Testebene voraussetzt (siehe unten) |
| `template/.releaserc.json` | semantic-release-Konfiguration, bereits generisch gehalten (nur `branches` ist ein Platzhalter) |

Bewusst nicht Teil dieses Moduls: ein täglicher GitHub-Actions-Relay-Job
für einen externen, fachspezifischen Datenfeed. Das zugrunde liegende
Muster ist generalisierbar und wird unten als Hinweis mitgegeben, ohne
den fachspezifischen Code selbst.

## Warum Fly.io (Kontext für diese Wahl)

Dieses Modul passt zu Anwendungen mit derselben Architekturentscheidung:
**genau eine Server-Instanz, kein Autoscaling, kein Sharding** (harte
Invariante 6, CLAUDE.md). Der Grund ist nicht
Sparsamkeit, sondern Korrektheit: Sobald Zustand im Arbeitsspeicher lebt
(WebSocket-Sitzungen, ein In-Memory-Aggregat, ein Actor-Loop) und nicht
über mehrere Instanzen hinweg konsistent gehalten wird, wären zwei
Instanzen zwei getrennte Zustände mit Sitzungen, die zufällig auf der
falschen landen. Fly.io passt dazu, weil:

- **Neustart-Politik zählt mehr als Preis.** Scale-to-Zero, tägliches
  Dyno-Cycling (Heroku) oder CPU-Throttling außerhalb von Requests (Cloud
  Run, kollidiert mit einem eigenen Event-Loop/Timer) würden bei
  In-Memory-Zustand Daten kosten. Fly erlaubt `auto_stop_machines = false`
  bei fester Kostenkontrolle über die VM-Größe.
- **Ein Fly-Volume** deckt den Fall ab, dass zumindest ein Snapshot des
  Zustands einen Neustart *innerhalb* einer laufenden Sitzung überleben
  soll — kein Ersatz für eine echte Datenbank
  (siehe Modul `persistence-postgres-flyway`, falls dauerhafte Persistenz
  gebraucht wird), nur ein Abzug für den Fall eines Absturzes oder
  Deploys am selben Abend/derselben Sitzung.
- **`fly deploy --ha=false` ist Pflicht, nicht optional.** Fly legt beim
  ersten Deploy von sich aus eine zweite Maschine für High Availability an
  — auch bei `min_machines_running = 1`. Das widerspricht der
  Ein-Instanz-Entscheidung strukturell, deshalb prüft `release.yml` das
  nach jedem Deploy automatisiert (Platzhalter für ein Prüfskript, siehe
  unten) statt sich auf einen manuellen `fly machines list`-Blick zu
  verlassen.

Ohne diese Architekturentscheidung (also bei einer zustandslosen Anwendung,
die horizontal skalieren soll) ist Fly.io trotzdem nutzbar, aber die
Ein-Instanz-Absicherungen in `fly.toml.template` und `release.yml.template`
sind dann nicht nötig und sollten entfernt werden.

## Was hier drinsteckt

| Datei | Zweck |
|---|---|
| `template/fly.toml.template` | Fly.io-Konfiguration: Region, optionales Volume-Mount, Health-Check, Concurrency, VM-Größe |
| `template/Dockerfile.template` | Multi-Stage-Build: Frontend (falls vorhanden) → Backend-Build → schlankes JRE-Runtime-Image |
| `template/.github/workflows/build.yml.template` | CI: Checkout, Build, Tests, Testbericht als Artifact |
| `template/.github/workflows/release.yml.template` | CD: ruft `build.yml` auf, optional Semantic Release, danach Fly-Deploy mit `--ha=false` |
| `template/.releaserc.json` | semantic-release-Konfiguration (Conventional-Commits-basiert) |

## Platzhalter

Einheitlich über alle Fragmente (`{{...}}`):

- `{{APP_NAME}}` — Fly-App-Name.
- `{{FLY_REGION}}` — Fly-Region (Beispielwert: `fra`).
- `{{FLY_VM_SIZE}}`, `{{FLY_VM_MEMORY}}` — VM-Größe/Speicher.
- `{{APP_PORT}}` — interner Port der Anwendung (Beispielwert: `8080`).
- `{{HEALTHCHECK_PATH}}` — Pfad für den HTTP-Health-Check.
- `{{CONCURRENCY_SOFT_LIMIT}}`, `{{CONCURRENCY_HARD_LIMIT}}` — Grenzwerte
  für gleichzeitige Verbindungen; bei dauerhaft offenen WebSockets und
  genau einer Instanz bewusst höher setzen als die Fly-Defaults.
- `{{VOLUME_NAME}}`, `{{VOLUME_MOUNT_PATH}}` — nur falls die Anwendung
  einen Snapshot/State auf Platte sichert; sonst den `[mounts]`-Abschnitt
  entfernen.
- `{{SNAPSHOT_ENV_VAR_NAME}}`, `{{SNAPSHOT_SUBDIR}}` — optional, analog.
- `{{MAIN_BRANCH}}` — Hauptzweig (Beispielwert: `main`).
- `{{JAVA_VERSION}}`, `{{NODE_VERSION}}` — Toolchain-Versionen.
- `{{FRONTEND_DIR}}`, `{{E2E_DIR}}` — Verzeichnisse, falls vorhanden.
- `{{GRADLE_IMAGE_TAG}}`, `{{JRE_IMAGE_TAG}}` — Docker-Image-Tags fürs
  Backend-Build bzw. die Runtime (Beispielwert: `9.6-jdk25` / `25-jre-alpine`).
- `{{SKIP_FRONTEND_GRADLE_PROPERTY}}` — Name der Gradle-Property, mit der
  ein reiner Backend-Build ohne Frontend läuft (Beispielwert: `skipFrontend`).
- `{{COMMIT_FORMAT_CHECK_SCRIPT}}`, `{{SINGLE_MACHINE_CHECK_SCRIPT}}`,
  `{{SMOKE_TEST_SCRIPT}}`, `{{SMOKE_TEST_URL}}` — Pfade zu optionalen,
  projekteigenen Prüfskripten; die zugehörigen Schritte in
  `release.yml.template`/`build.yml.template` sind deshalb auskommentiert
  statt aktiv, weil dieses Modul die Skripte selbst nicht mitliefert (kein
  Fachcode). Wer sie braucht, schreibt sie für das eigene Projekt, z. B. als
  `ci/eine-maschine-pruefen.sh`, `ci/rauchtest.mjs` und
  `ci/commit-format-pruefen.sh`.

## Was ein Nutzer noch braucht

- **Secrets:** `FLY_API_TOKEN` (App-gescoped, z. B. über
  `fly tokens create deploy -a {{APP_NAME}}`); bei Semantic Release reicht
  `GITHUB_TOKEN` (von GitHub Actions automatisch bereitgestellt).
- **Ein Fly-Postgres oder eine andere Datenbank** ist nicht Teil dieses
  Moduls — siehe `persistence-postgres-flyway`.
- **Ein GitHub-Pages-Job zur Veröffentlichung eines Testberichts** ist
  bewusst nicht Teil dieses Moduls, weil er eine spezifische Testebene
  voraussetzt, die nicht jedes Projekt hat. Wer einen Testbericht
  veröffentlichen will, kann das Muster (eigener Job im selben Workflow,
  `needs: build`, `actions/deploy-pages`) übertragen.

## Ergänzendes Muster: externer Dienst blockiert Rechenzentrums-IPs

Manche externen Dienste blockieren Zugriffe aus Rechenzentrums-IP-Bereichen
wie dem von Fly.io (z. B. Bot-Abwehr gegen Rechenzentrums-Adressen). Statt
eines internen, selbst geplanten Jobs auf der Fly-Instanz kann dann ein
täglicher/periodischer GitHub-Actions-Cronjob die Daten von einem
GitHub-Runner (anderes Netz) abrufen und über einen eigenen,
tokengeschützten Endpunkt an die Anwendung weiterreichen.

Das ist als **Muster** generell brauchbar, sobald eine Zielumgebung
(Fly.io oder sonst ein Rechenzentrums-IP-Bereich) von einem externen
Dienst geblockt wird. Der konkrete Fachcode für den jeweiligen externen
Dienst ist bewusst nicht Teil dieses Moduls — wer das Muster braucht,
schreibt den Relay als eigenen GitHub-Actions-Workflow für den eigenen
externen Dienst selbst.
