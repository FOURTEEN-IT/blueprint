// Modul dependabot-maintenance -- OpenRewrite fuer Major-Versionsspruenge,
// die die taegliche Dependabot-Routine (docs/dependabot-routine.md) ausloest.
//
// Herkunft: aus Watchparty (build.gradle.kts, ADR-042) uebernommen. Setzt
// kein anderes Modul dieses Blueprints zwingend voraus -- OpenRewrite laeuft
// auf jedem Gradle/Java-Projekt -- ist aber nur sinnvoll mit
// backend-java-onion (oder einem gleichwertigen Gradle-Projekt).
//
// Einspielen: An das Ende von build.gradle.kts des Zielprojekts haengen.
//
// Platzhalter: keine in diesem Fragment selbst -- die Rezeptauswahl steht in
// template/ci/openrewrite-anwenden.sh.template, nicht hier (siehe README).

plugins {
    // Fuehrt Major-Versionsupdates als Rezept aus statt von Hand.
    // Haengt bewusst an KEINER Stelle an `check` -- rewriteRun/rewriteDryRun
    // sind Werkzeuge fuer die Dependabot-Routine, keine Pruefung.
    id("org.openrewrite.rewrite") version "7.39.0"
}

dependencies {
    // Die Rezeptsammlungen fuer Major-Updates liegen auf einer eigenen
    // Konfiguration (`rewrite`) und damit weder auf dem Compile- noch auf
    // dem Test-Classpath -- ein Rezept kann nichts kompilieren, was sonst
    // nicht kompiliert.
    //
    // Bewusst nur diese drei: Sie beschreiben, was ein Versionssprung
    // *erzwingt*. rewrite-static-analysis waere die vierte naheliegende, ist
    // aber Geschmacksverbesserung -- und die Routine darf laut ihren eigenen
    // Grenzen nichts anfassen, was der Sprung nicht verlangt (siehe
    // docs/dependabot-routine.md, "Harte Grenzen").
    "rewrite"(platform("org.openrewrite.recipe:rewrite-recipe-bom:3.37.0"))
    "rewrite"("org.openrewrite.recipe:rewrite-spring")
    "rewrite"("org.openrewrite.recipe:rewrite-migrate-java")
    "rewrite"("org.openrewrite.recipe:rewrite-testing-frameworks")
}

// --- Major-Versionsupdates als Rezept ---------------------------------------
//
// Ein Major-Sprung bringt brechende Aenderungen mit; ohne dieses Fragment
// zieht die taegliche Dependabot-Routine sie von Hand nach -- aus den
// Release Notes gelesen und interpretiert. OpenRewrite macht daraus eine
// Programmausfuehrung: Der Hersteller der Bibliothek beschreibt den Umstieg
// einmal als Rezept, die Routine wendet es an. Was das Rezept nicht abdeckt,
// bleibt Handarbeit -- aber es ist danach sichtbar weniger.
//
// Die Rezepte kommen je Lauf von aussen und stehen bewusst nicht fest im
// Build: Welches Rezept gilt, haengt am konkreten Sprung der jeweiligen
// Dependabot-PR. Die Zuordnung Sprung -> Rezept steht in
// ci/openrewrite-anwenden.sh, nicht hier.
//
//   ./gradlew rewriteDryRun -PrewriteRezepte=org.openrewrite.java.testing.junit6.JUnit5to6Migration
//   ./gradlew rewriteRun    -PrewriteRezepte=rezept.eins,rezept.zwei
//
// Ohne -PrewriteRezepte ist kein Rezept aktiv; rewriteRun aendert dann
// nichts. Das ist Absicht: ein versehentlicher Aufruf schreibt nicht um.
rewrite {
    val rezepte = (project.findProperty("rewriteRezepte") as String?)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()
    rezepte.forEach { activeRecipe(it) }

    // Ein Rezeptname, den es nicht gibt, bricht den Lauf ab statt ihn
    // stillschweigend auszulassen -- sonst meldete rewriteRun Erfolg, haette
    // aber nichts getan, und ein Tippfehler im Katalog von
    // ci/openrewrite-anwenden.sh saehe genauso aus wie "dieser Sprung
    // braucht kein Rezept".
    failOnInvalidActiveRecipes = true
}
