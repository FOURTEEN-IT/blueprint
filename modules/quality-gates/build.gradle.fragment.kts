// Modul quality-gates -- Mutationstests und JGiven-Report ueber alle
// Ebenen, verbindlich an `check` gehaengt.
//
// Herkunft: aus Watchparty (build.gradle.kts) uebernommen und von
// Fachbegriffen (Kritikalitaetsstufen, Klassennamen, Anforderungsdokument-
// Format) befreit. Setzt voraus, dass das Modul backend-java-onion bereits
// eingespielt ist -- die Test-Tasks `test`/`adapterTest`/`apiTest`/`archTest`
// werden hier referenziert, nicht definiert.
//
// Einspielen: An das Ende von build.gradle.kts des Zielprojekts haengen,
// NACH dem Fragment von backend-java-onion.
//
// Platzhalter in diesem Fragment:
//   {{PACKAGE_BASE}}              Basispaket, z. B. de.example.projectname
//   {{PITEST_MUTATION_THRESHOLD}} Mindest-Mutation-Score in Prozent (0-100),
//                                 am echten Projekt kalibriert -- keine
//                                 Empfehlung dieses Moduls

import com.tngtech.jgiven.gradle.JGivenTaskExtension
import com.tngtech.jgiven.gradle.JGivenReportTask
import info.solidsoft.gradle.pitest.PitestPluginExtension
import java.net.URLClassLoader

plugins {
    // Erzeugt den JGiven-HTML-Report aus den JSON-Ergebnissen, die
    // jgiven-junit5 beim Testlauf schreibt.
    id("com.tngtech.jgiven.gradle-plugin") version "2.0.3"
    // Mutationstests statt reiner Zeilen-/Zweigabdeckung.
    id("info.solidsoft.pitest") version "1.19.0"
}

dependencies {
    // jgiven-junit5 bringt die JUnit5-Erweiterung fuer ScenarioTest mit;
    // muss auf demselben Test-Classpath liegen wie in backend-java-onion.
    "testImplementation"("com.tngtech.jgiven:jgiven-junit5:2.0.3")
}

// --- Ein JGiven-Report ueber alle Ebenen hinweg -----------------------------
//
// Das JGiven-Gradle-Plugin verdrahtet jeden Test-Task automatisch mit einem
// eigenen Ergebnisordner (build/<Taskname>/jgiven-results), haengt aber nur
// fuer den vorgefundenen Standard-Task `test` einen Report-Task ein -- die
// in backend-java-onion definierten `adapterTest`/`apiTest` kommen dabei zu
// spaet. Alle drei schreiben deshalb in denselben Ordner (Dateinamen sind je
// Testklasse eindeutig, Kollisionen also ausgeschlossen), und der
// vorhandene `jgivenTestReport`-Task liest von dort -- ein einziger Report
// mit allen Szenarien aus allen Ebenen.
//
// `archTest` bewusst aussen vor: ArchUnit-Regeln sind keine JGiven-Szenarien
// und schreiben nie in diesen Ordner -- eine Abhaengigkeit von `archTest`
// waere hier ein Validierungsfehler ohne Gegenwert (Gradle bemaengelt sonst
// eine "implicit dependency" auf ein Verzeichnis, das der Task nie befuellt).
val jgivenResultsDir = layout.buildDirectory.dir("jgiven-results/alle-ebenen")

val adapterTest = tasks.named("adapterTest")
val apiTest = tasks.named("apiTest")

listOf(tasks.named("test"), adapterTest, apiTest).forEach { test ->
    test.configure {
        extensions.configure<JGivenTaskExtension> {
            resultsDir.set(jgivenResultsDir)
        }
    }
}

tasks.named<JGivenReportTask>("jgivenTestReport") {
    dependsOn(tasks.named("test"), adapterTest, apiTest)
    results.set(jgivenResultsDir)
}

tasks.named("check") {
    dependsOn("jgivenTestReport")
}

// --- Mutationstests auf gezielt kritischen Klassen --------------------------
//
// Nur als kritisch eingestufte Klassen und nur die Tags unit/port als
// Testmenge -- kein Spring, kein Socket, kein Reportschreiben, sonst wird
// der Lauf unbenutzbar (PIT wiederholt Tests je Mutant). includedGroups
// reicht bis zur JUnit5Configuration des PIT-Plugins durch und filtert dort
// per Tag, genau wie der `test`-Task aus backend-java-onion.
//
// Welche Klassen kritisch sind, steht als Annotation am Code selbst
// (Konvention, keine Bibliotheksklasse dieses Moduls -- ein Projekt
// definiert {{PACKAGE_BASE}}.criticality.Critical selbst, analog zu
// Watchpartys @Criticality). Diese Liste wird deshalb daraus *abgeleitet*
// und nicht daneben gefuehrt: Eine zweite, handgepflegte Aufzaehlung waere
// genau die zweite Wahrheit, die still veraltet -- eine neu als kritisch
// eingestufte Klasse bliebe unmutiert, und der Mutation Score bliebe gruen,
// obwohl er sie nie angefasst hat. Reflection ueber die kompilierten
// Klassen, keine Textsuche im Quelltext.
//
// Property `pitestLevel` waehlt die Stufe (Default: HIGH) -- ein Projekt mit
// mehr als einer Kritikalitaetsstufe (wie Watchparty: LOW/MEDIUM/HIGH) kann
// hier einen anderen Wert setzen; der Annotationstyp selbst entscheidet, ob
// diese Stufe ueberhaupt existiert (die Annotation ist Projektsache).
val criticalClasses = provider {
    val level = (project.findProperty("pitestLevel") as String?) ?: "HIGH"
    val classesDirs = sourceSets.getByName("main").output.classesDirs
    val urls = sourceSets.getByName("main").runtimeClasspath.files.map { it.toURI().toURL() }.toTypedArray()
    val classLoader = URLClassLoader(urls, javaClass.classLoader)
    val criticalityClass = classLoader.loadClass("{{PACKAGE_BASE}}.criticality.Critical")
    @Suppress("UNCHECKED_CAST")
    val annotationClass = criticalityClass as Class<out Annotation>
    val levelMethod = criticalityClass.getMethod("level")

    val found = sortedSetOf<String>()
    classesDirs.forEach { rootDir ->
        rootDir.walkTopDown()
            .filter { it.isFile && it.extension == "class" && !it.name.contains("$") }
            .forEach { classFile ->
                val className = classFile.relativeTo(rootDir).path
                    .removeSuffix(".class").replace(File.separatorChar, '.')
                val klass = try {
                    classLoader.loadClass(className)
                } catch (e: Throwable) {
                    return@forEach
                }
                val annotation = klass.getAnnotation(annotationClass) ?: return@forEach
                if (levelMethod.invoke(annotation).toString() == level) {
                    found += className
                }
            }
    }

    // Eine leere Zielmenge waere der gefaehrlichste Ausgang: PIT liefe durch,
    // mutierte nichts und meldete Erfolg -- ununterscheidbar von einem echten
    // Lauf.
    if (found.isEmpty()) {
        throw GradleException(
            "Keine @Critical($level)-Klasse gefunden -- die Mutationstests haetten kein Ziel. " +
                "Entweder ist die Einstufung verschwunden, oder das Einsammeln ist kaputt.")
    }
    logger.lifecycle("Mutationstests auf ${found.size} $level-Klasse(n): ${found.joinToString(", ")}")
    found.toSet()
}

configure<PitestPluginExtension> {
    targetClasses.set(criticalClasses)
    targetTests.set(setOf("{{PACKAGE_BASE}}.*"))
    includedGroups.set(setOf("unit", "port"))
    testPlugin.set("junit5")
    junit5PluginVersion.set("1.2.3")
    mutationThreshold.set({{PITEST_MUTATION_THRESHOLD}})
    outputFormats.set(setOf("HTML", "XML"))
    timestampedReports.set(false)
}

// Der Schwellwert oben gilt erst dann fuer jeden Build, wenn `check` davon
// abhaengt -- sonst ist die Metrik zwar konfiguriert, aber in keinem
// CI-Lauf wirksam. Genau das ist die Kernaussage dieses Moduls.
tasks.named("check") {
    dependsOn("pitest")
}
