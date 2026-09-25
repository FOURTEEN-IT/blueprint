# Cloudflare vor Fly.io einrichten

**Standardmäßig kein Cloudflare:** Die Fly.io-Vorlage dieses Moduls setzt
bewusst **kein** Cloudflare ein — die DNS-Subdomain liegt direkt beim
Registrar (CNAME auf `*.fly.dev`). Der Grund: Eine zusätzliche Proxy-Schicht
bringt ein weiteres Idle-Timeout und eine zweite Zertifikatskette mit, ohne
Nutzen für eine Anwendung mit wenigen, bekannten Nutzern ohne öffentliche
Reichweite.

Für eine solche Anwendung ist das eine vernünftige Entscheidung. Dieses
Dokument liefert trotzdem eine eigenständige
Cloudflare-Vorlage für Projekte, die *einen* der folgenden Gründe haben:

- Öffentliche Reichweite statt eines geschlossenen Freundeskreises (DDoS-
  Schutz, Bot-Abwehr, Caching).
- Ein getrennt deploytes Frontend (z. B. über Cloudflare Pages), waehrend
  nur das Backend auf Fly.io läuft.
- Eine vorhandene Cloudflare-Organisation, in die weitere Subdomains ohnehin
  einsortiert werden.

Wer keinen dieser Gründe hat, sollte beim Standard bleiben: DNS direkt
beim Registrar, kein zusätzlicher Proxy. Jede zusätzliche Schicht bringt ein
eigenes Idle-Timeout und eine eigene Zertifikatskette mit — bei WebSockets
(siehe Invarianten-Hinweis im Modul-README) ein zusätzliches Risiko, keine
zusätzliche Sicherheit.

## Variante A: Cloudflare als DNS + Proxy vor Fly.io

Für ein Backend, das komplett auf Fly.io läuft und nur zusätzlich hinter
Cloudflare liegen soll (WAF, Caching von statischen Assets, DDoS-Schutz).

1. **Domain zu Cloudflare hinzufügen** (Free-Plan reicht für DNS + Proxy).
   Cloudflare übernimmt danach die Nameserver — das ist beim Registrar
   umzustellen.
2. **DNS-Eintrag anlegen:**
   - Typ `CNAME`
   - Name: `{{SUBDOMAIN}}` (z. B. `app`)
   - Ziel: `{{APP_NAME}}.fly.dev`
   - Proxy-Status: **Proxied** (orange Wolke) — nur dann greifen WAF/Caching
     überhaupt; bei **DNS only** (graue Wolke) verhält es sich wie ein
     normaler CNAME beim Registrar, ohne Cloudflare-Funktionen.
3. **TLS-Modus:** `Full (strict)` einstellen (Cloudflare → SSL/TLS →
   Overview). Fly.io terminiert TLS selbst mit einem gültigen,
   von Let's Encrypt ausgestellten Zertifikat (siehe `force_https = true`
   in `fly.toml`) — `Full (strict)` prüft dieses Zertifikat, `Flexible`
   würde die Strecke Cloudflare→Fly unverschlüsselt lassen und ist deshalb
   **nicht** geeignet.
4. **Fly-Zertifikat für die eigene Domain anlegen**, nicht nur für
   `*.fly.dev`:
   ```bash
   fly certs add {{SUBDOMAIN}}.{{DOMAIN}} -a {{APP_NAME}}
   fly certs show {{SUBDOMAIN}}.{{DOMAIN}} -a {{APP_NAME}}
   ```
   Solange Cloudflare proxied, sieht Fly die Anfragen mit Cloudflares
   IP-Adressen, nicht der echten Client-IP — für Rate-Limits oder
   IP-basiertes Logging braucht es zusätzlich den Header `CF-Connecting-IP`
   (Cloudflare setzt ihn automatisch bei proxierten Anfragen).
5. **WebSocket-Support prüfen:** Cloudflare unterstützt WebSockets im
   Proxy-Modus standardmäßig (Network → WebSockets → On, ist bei den meisten
   Plänen bereits aktiv). Ohne das würde jede Live-Verbindung sofort
   abbrechen — für eine Anwendung mit dauerhaften Verbindungen (WebSocket,
   SSE) ist das keine Kann-, sondern eine Muss-Prüfung vor dem Go-Live.
6. **Idle-Timeout beachten:** Cloudflares Proxy trennt eine Verbindung nach
   100 Sekunden Inaktivität (Free/Pro-Plan, nicht konfigurierbar). Eine
   Anwendung mit eigenem Heartbeat/Ping unterhalb dieser Grenze bleibt davon
   unberührt; eine, die das nicht hat, braucht eins, sobald Cloudflare
   davorsteht — ein Unterschied zum direkten Weg ohne Proxy.

## Variante B: Getrenntes Frontend über Cloudflare Pages

Nur relevant, wenn Frontend und Backend getrennt deployt werden (anders als
im Standardfall dieses Blueprints, wo das Frontend-Build ins Backend-Jar
gepackt wird, siehe Modul `frontend-react-vite`). Für ein statisches
React/Vite-Build, während
nur die API/das WebSocket-Backend auf Fly.io läuft:

1. Repository (oder Unterverzeichnis `{{FRONTEND_DIR}}`) in Cloudflare Pages
   als Projekt anlegen, Build-Befehl `npm run build`, Ausgabeverzeichnis
   `dist`.
2. Umgebungsvariable für die Backend-URL setzen (z. B.
   `VITE_API_BASE_URL=https://{{SUBDOMAIN}}.{{DOMAIN}}`), damit das
   Frontend weiß, wohin es WebSocket-/API-Anfragen schickt — bei getrennten
   Deployments gibt es keinen gemeinsamen Origin mehr wie im Standardfall
   (Frontend-Build im Backend-Jar).
3. CORS am Backend für die Pages-Domain freigeben (`https://{{PAGES_PROJECT}}.pages.dev`
   und die eigene Domain, falls per Custom Domain verbunden).
4. Custom Domain für Pages einrichten (Cloudflare Pages → Custom domains) —
   unabhängig von Variante A, kann parallel für eine andere Subdomain
   laufen (z. B. `app.{{DOMAIN}}` für Pages, `api.{{DOMAIN}}` für Fly.io).

## Was hier bewusst fehlt

Kein Cloudflare Workers, kein Cache-Rules-Feintuning, kein Argo Smart
Routing — das sind Erweiterungen für einen Bedarf, den die meisten aus
diesem Blueprint entstehenden Projekte nicht haben. Wer sie
braucht, hat vermutlich auch die Erfahrung, sie selbst einzurichten; diese
Vorlage deckt nur den Einstieg.
