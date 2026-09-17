#!/usr/bin/env bash
# Prueft Commit-Betreffzeilen gegen Conventional Commits -- und gegen eine
# Zusatzregel: Ein releasender Typ ohne jede Anwendungsdatei im Commit ist
# mit hoher Sicherheit falsch getippt oder falsch gewaehlt.
#
# Der Grund ist kein Stil, sondern das Deployment: Ein Release-Werkzeug wie
# Semantic Release leitet aus dem Commit-Typ ab, ob ueberhaupt ein Release
# entsteht -- und damit, ob deployed wird. Ein "fixed:" statt "fix:" wird
# stillschweigend ignoriert: kein Release, kein Deploy, keine Fehlermeldung.
# Ein "feat:"/"fix:"/"perf:" auf eine reine Doku-/Tooling-Aenderung ist der
# umgekehrte Fehler: ein Release und ein Deploy, die inhaltlich nichts an der
# laufenden Anwendung aendern. Beides faengt dieses Skript ab.
#
# Die erlaubten Typen sind die des Angular-Presets, das
# @semantic-release/commit-analyzer per Default verwendet -- die Liste hier
# ist also keine eigene Konvention, sondern die Menge, die das
# Release-Werkzeug tatsaechlich versteht. Von denen loesen nur feat/fix/perf
# ueberhaupt einen Release aus; das ist die "releasende" Teilmenge unten.
#
# Aufruf: ci/commit-format-pruefen.sh <basis-ref> <kopf-ref>
#         ci/commit-format-pruefen.sh              (ohne Argumente: HEAD allein)
#         ci/commit-format-pruefen.sh <nachrichtendatei>   (commit-msg-Hook)
#
# Der dritte Aufruf ist fuer .githooks/commit-msg.
#
# Herkunft: aus Watchparty (ci/commit-format-pruefen.sh) uebernommen, von
# projektspezifischen Strings befreit. Die einzige Stelle, die ein
# einspielendes Projekt anpassen MUSS, ist ANWENDUNGSPFADE unten -- siehe
# Platzhalter-Kommentar dort.

set -euo pipefail

typen="build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test"
muster="^(${typen})(\([a-z0-9./-]+\))?!?: .+"
releasende_typen="feat|fix|perf"

# {{ANWENDUNGSPFADE}}: Regex der Dateipfade, deren Aenderung sich auf das
# tatsaechlich deployte Artefakt auswirken kann -- bewusst eng gehalten.
# Watchparty-Beispiel (Java/Spring-Boot + React/Vite, ein Gradle-Build):
#   '^(src/main/|frontend/src/|frontend/package(-lock)?\.json$|build\.gradle\.kts$|settings\.gradle\.kts$|Dockerfile$|fly\.toml$)'
# Ein anderer Stack (anderer Build, anderes Deploy-Ziel) passt diese Liste
# an -- CI-Workflows selbst gehoeren bewusst NICHT dazu: Eine Aenderung dort,
# die einen Release verdient, heisst treffender "ci:" (nicht-releasend) oder
# traegt zusaetzlich eine echte Anwendungsdatei.
anwendungspfade='{{ANWENDUNGSPFADE}}'

# Wahr (Exit 0), wenn der Typ releasend ist, aber keine der uebergebenen
# Dateien eine Anwendungsdatei ist -- der Fall, den eine Freigabe-Pruefung
# von Hand abfangen sollte und leicht durchrutscht.
releasender_typ_ohne_anwendungsaenderung() {
    local typ="$1"
    shift
    echo "$typ" | grep -Eq "^(${releasende_typen})\$" || return 1
    local datei
    for datei in "$@"; do
        echo "$datei" | grep -Eq "$anwendungspfade" && return 1
    done
    return 0
}

# commit-msg-Hook-Modus: einziges Argument ist eine vorhandene Datei -- die
# noch nicht erstellte Commit-Nachricht, kein Git-Ref, den git log verstehen
# wuerde. git-log-Modus (unten) bekommt nie ein Argument, das eine Datei ist.
if [ $# -eq 1 ] && [ -f "$1" ]; then
    betreff="$(head -n1 "$1")"
    if ! echo "$betreff" | grep -Eq "$muster"; then
        echo "  ✗ ${betreff}"
        echo
        echo "Commit-Betreff folgt nicht Conventional Commits."
        echo "Erlaubte Typen: ${typen//|/, }"
        echo "Beispiel: fix: kurze Beschreibung der Aenderung"
        echo
        echo "Ein nicht erkannter Typ bedeutet: kein Release und kein Deploy."
        exit 1
    fi

    typ="$(echo "$betreff" | grep -Eo '^[a-z]+')"
    dateien=()
    while IFS= read -r datei; do
        dateien+=("$datei")
    done < <(git diff --cached --name-only)

    if releasender_typ_ohne_anwendungsaenderung "$typ" "${dateien[@]}"; then
        echo "  ✗ ${betreff}"
        echo
        echo "Typ \"${typ}:\" loest laut Release-Werkzeug einen Release und ein"
        echo "Deploy aus -- keine der geaenderten Dateien wirkt sich aber auf die"
        echo "Anwendung aus (siehe ANWENDUNGSPFADE in diesem Skript). Vermutlich"
        echo "der falsche Typ: docs:/chore:/test:/refactor: pruefen."
        exit 1
    fi

    echo "Commit-Format: gueltig."
    exit 0
fi

# Loest sich die Basis nicht auf (flacher Klon, erster Push eines Branches,
# ein Force-Push, der den alten Stand entfernt hat), wird nur HEAD geprueft --
# lieber weniger pruefen als den Build an einer Referenz scheitern lassen, die
# mit dem Commit-Format nichts zu tun hat.
if [ $# -eq 2 ] && git rev-parse --verify --quiet "$1^{commit}" >/dev/null && [ "$1" != "0000000000000000000000000000000000000000" ]; then
    bereich="$1..$2"
else
    bereich="-1 HEAD"
fi

# %s ist die Betreffzeile, %P die Eltern-Commits. Merge-Commits (mehr als ein
# Elternteil) bleiben aussen vor: Ihre Betreffzeile erzeugt "Merge branch ..."
# und wird von Semantic Release ohnehin nicht als Release-ausloesend gewertet.
fehler=0
while IFS=$'\t' read -r hash eltern betreff; do
    [ -z "$hash" ] && continue
    if [ "$(echo "$eltern" | wc -w)" -gt 1 ]; then
        continue
    fi
    if ! echo "$betreff" | grep -Eq "$muster"; then
        echo "  ✗ ${hash:0:8}  ${betreff}"
        fehler=$((fehler + 1))
        continue
    fi

    typ="$(echo "$betreff" | grep -Eo '^[a-z]+')"
    dateien=()
    while IFS= read -r datei; do
        dateien+=("$datei")
    done < <(git diff-tree --no-commit-id --name-only -r "$hash")

    if releasender_typ_ohne_anwendungsaenderung "$typ" "${dateien[@]}"; then
        echo "  ✗ ${hash:0:8}  ${betreff}  (releasender Typ, keine Anwendungsdatei geaendert)"
        fehler=$((fehler + 1))
    fi
done < <(git log --format='%H%x09%P%x09%s' $bereich)

if [ "$fehler" -gt 0 ]; then
    echo
    echo "${fehler} Commit(s) verletzen die Commit-Konventionen dieses Projekts."
    echo "Erlaubte Typen: ${typen//|/, }"
    echo "Beispiel: fix: kurze Beschreibung der Aenderung"
    echo
    echo "Ein nicht erkannter Typ, oder ein releasender Typ ohne Anwendungsdatei,"
    echo "bedeutet: falsches Release-/Deploy-Verhalten."
    exit 1
fi

echo "Commit-Format: alle geprueften Betreffzeilen sind gueltig."
