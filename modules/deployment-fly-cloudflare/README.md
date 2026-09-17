# deployment-fly-cloudflare

Liefert den generischen Deployment-Baustein: Fly.io als Hosting (ein
Container, eine Instanz, optionales Fly-Volume für persistenten Zustand),
GitHub Actions für Build/Release/Deploy, und eine eigenständige
Cloudflare-Vorlage als vorgeschaltete Schicht. Kein Fachcode aus
Watchparty — nur das Gerüst.

## Herkunft, ehrlich getrennt

| Teil | Herkunft |
|---|---|
| `template/fly.toml.template` | Generalisiert aus Watchparty (`fly.toml`), inklusive der Kommentare zu ADR-005/ADR-018 |
| `template/Dockerfile.template` | Generalisiert aus Watchparty (`Dockerfile`) |
| `template/.github/workflows/build.yml.template` | Generalisiert aus Watchparty (`.github/workflows/build.yml`), E2E- und Commit-Format-Schritte als auskommentierte Beispiele, weil sie projekteigene Skripte/Ebenen voraussetzen, die dieses Modul nicht mitliefert |
| `template/.github/workflows/release.yml.template` | Generalisiert aus Watchparty (`.github/workflows/release.yml`); der GitHub-Pages-Job (JGiven-Bericht) ist **nicht** übernommen, weil er an Watchpartys eigene Testebene hängt |
| `template/.releaserc.json` | Unverändert aus Watchparty übernommen (semantic-release-Konfiguration ist bereits generisch, nur `branches` ist ein Platzhalter) |
| `docs/cloudflare-setup.md` | **Keine Watchparty-Herkunft.** Watchparty setzt bewusst **kein** Cloudflare ein (ADR-018 verwirft einen vorgeschalteten Proxy ausdrücklich, DNS liegt dort direkt bei IONOS). Das Dokument ist eine eigenständig verfasste Vorlage für Projekte, die einen Grund für Cloudflare haben, den Watchparty nicht hat — im Dokument selbst noch einmal so gekennzeichnet |

Nicht übernommen: `schedule-relay.yml` (Watchparty). Der tägliche
GitHub-Actions-Relay dort ist fachspezifisch (ESPN-Feed für das
Tippspiel), zeigt aber ein generalisierbares **Muster**, das dieses Modul
nicht als Code, sondern nur als Hinweis mitgibt (siehe unten).

## Warum Fly.io (Kontext für diese Wahl)

Dieses Modul passt zu Anwendungen, die dieselbe Architekturentscheidung wie
Watchparty treffen: **genau eine Server-Instanz, kein Autoscaling, kein
Sharding** (Watchparty: harte Invariante 6, CLAUDE.md). Der Grund ist nicht
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
  soll (Watchparty-Vorbild: ADR-023) — kein Ersatz für eine echte Datenbank
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
| `docs/cloudflare-setup.md` | Eigenständige Anleitung: Cloudflare als DNS/Proxy vor Fly.io, oder Cloudflare Pages für ein getrennt deploytes Frontend |

## Platzhalter

Einheitlich über alle Fragmente (`{{...}}`):

- `{{APP_NAME}}` — Fly-App-Name.
- `{{FLY_REGION}}` — Fly-Region (Watchparty: `fra`).
- `{{FLY_VM_SIZE}}`, `{{FLY_VM_MEMORY}}` — VM-Größe/Speicher.
- `{{APP_PORT}}` — interner Port der Anwendung (Watchparty: `8080`).
- `{{HEALTHCHECK_PATH}}` — Pfad für den HTTP-Health-Check.
- `{{CONCURRENCY_SOFT_LIMIT}}`, `{{CONCURRENCY_HARD_LIMIT}}` — Grenzwerte
  für gleichzeitige Verbindungen; bei dauerhaft offenen WebSockets und
  genau einer Instanz bewusst höher setzen als die Fly-Defaults.
- `{{VOLUME_NAME}}`, `{{VOLUME_MOUNT_PATH}}` — nur falls die Anwendung
  einen Snapshot/State auf Platte sichert; sonst den `[mounts]`-Abschnitt
  entfernen.
- `{{SNAPSHOT_ENV_VAR_NAME}}`, `{{SNAPSHOT_SUBDIR}}` — optional, analog.
- `{{MAIN_BRANCH}}` — Hauptzweig (Watchparty: `main`).
- `{{JAVA_VERSION}}`, `{{NODE_VERSION}}` — Toolchain-Versionen.
- `{{FRONTEND_DIR}}`, `{{E2E_DIR}}` — Verzeichnisse, falls vorhanden.
- `{{GRADLE_IMAGE_TAG}}`, `{{JRE_IMAGE_TAG}}` — Docker-Image-Tags fürs
  Backend-Build bzw. die Runtime (Watchparty: `9.6-jdk25` / `25-jre-alpine`).
- `{{SKIP_FRONTEND_GRADLE_PROPERTY}}` — Name der Gradle-Property, mit der
  ein reiner Backend-Build ohne Frontend läuft (Watchparty: `skipFrontend`).
- `{{COMMIT_FORMAT_CHECK_SCRIPT}}`, `{{SINGLE_MACHINE_CHECK_SCRIPT}}`,
  `{{SMOKE_TEST_SCRIPT}}`, `{{SMOKE_TEST_URL}}` — Pfade zu optionalen,
  projekteigenen Prüfskripten; die zugehörigen Schritte in
  `release.yml.template`/`build.yml.template` sind deshalb auskommentiert
  statt aktiv, weil dieses Modul die Skripte selbst nicht mitliefert (kein
  Fachcode). Wer sie braucht, schreibt sie analog zu Watchparty
  (`ci/eine-maschine-pruefen.sh`, `ci/rauchtest.mjs`,
  `ci/commit-format-pruefen.sh`) für das eigene Projekt.
- `docs/cloudflare-setup.md`: `{{SUBDOMAIN}}`, `{{DOMAIN}}`,
  `{{PAGES_PROJECT}}`.

## Was ein Nutzer noch braucht

- **Secrets:** `FLY_API_TOKEN` (App-gescoped, z. B. über
  `fly tokens create deploy -a {{APP_NAME}}`); bei Semantic Release reicht
  `GITHUB_TOKEN` (von GitHub Actions automatisch bereitgestellt).
- **Ein Fly-Postgres oder eine andere Datenbank** ist nicht Teil dieses
  Moduls — siehe `persistence-postgres-flyway`.
- **Der GitHub-Pages-Job aus Watchparty** (Veröffentlichung eines
  Testberichts) ist nicht übernommen, weil er an eine spezifische
  Testebene (JGiven) hängt, die dieses Modul nicht voraussetzt. Wer einen
  Testbericht veröffentlichen will, kann das Muster (eigener Job im selben
  Workflow, `needs: build`, `actions/deploy-pages`) übertragen.

## Ein Muster aus Watchparty, nicht übernommen als Code

Watchpartys `schedule-relay.yml` löst ein Problem, das über Fly.io
hinausgeht: Ein externer Dienst (dort: der ESPN-Feed) blockiert Zugriffe
aus dem Fly.io-IP-Bereich (Akamai-Bot-Abwehr gegen Rechenzentrums-Adressen,
ADR-037-Nachtrag). Statt eines internen, selbst geplanten Jobs auf der
Fly-Instanz ruft dort ein täglicher GitHub-Actions-Workflow den externen
Dienst von einem GitHub-Runner ab (anderes Netz) und reicht die Antwort an
einen Relay-Endpunkt der Anwendung weiter.

Das ist als **Muster** generell brauchbar, sobald eine Zielumgebung
(Fly.io oder sonst ein Rechenzentrums-IP-Bereich) von einem externen
Dienst geblockt wird: ein täglicher/periodischer GitHub-Actions-Cronjob
als Ersatz für einen internen Scheduler, der Daten von außerhalb abruft
und über einen eigenen, tokengeschützten Endpunkt an die Anwendung
weiterreicht. Der konkrete Fachcode (ESPN, Spieltage, Saison) ist bewusst
nicht Teil dieses Moduls — wer das Muster braucht, schreibt den Relay für
den eigenen externen Dienst analog zu Watchparty
(`.github/workflows/schedule-relay.yml`) selbst.
