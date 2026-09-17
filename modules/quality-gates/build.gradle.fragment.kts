// Modul quality-gates -- Mutationstests, JGiven-Report ueber alle Ebenen,
// Ausnahmenregister, alles verbindlich an `check` gehaengt.
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
//   {{ANWENDUNGSPFADE}}           steht NICHT hier, sondern in
//                                 ci-template/commit-format-pruefen.sh

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
    // Ausnahmenregister (siehe Task ausnahmenregister unten): das eingebaute
    // FANN-Plugin (an, per Default schon auf Generated/DoNotMutate/
    // CoverageIgnore) schliesst annotierte Klassen/Methoden von der Mutation
    // aus -- die eigene Annotation ergaenzt die drei Standardnamen, statt sie
    // zu ersetzen (ein konfigurierter Wert ueberschreibt sonst die
    // eingebaute Liste komplett).
    features.set(listOf(
            "+FANN(annotation[Generated]annotation[DoNotMutate]annotation[CoverageIgnore]annotation[EquivalentMutant])"))
}

// Der Schwellwert oben gilt erst dann fuer jeden Build, wenn `check` davon
// abhaengt -- sonst ist die Metrik zwar konfiguriert, aber in keinem
// CI-Lauf wirksam. Genau das ist die Kernaussage dieses Moduls.
tasks.named("check") {
    dependsOn("pitest")
}

// --- Ausnahmenregister ------------------------------------------------------
//
// Optionales, aber generalisierbares Muster (siehe README): jede bewusste
// Unterdrueckung einer Pruefung im Code -- hier: @EquivalentMutant gegen
// einen Pitest-Mutanten, @Disabled gegen einen JUnit-Test -- muss in einem
// versionierten Register mit Begruendung und Datum stehen, und umgekehrt
// darf das Register keine Karteileichen enthalten. Ein Gate gleicht beide
// Seiten per Reflection ab.
//
// Ein Projekt, das dieses Muster nicht will, entfernt einfach diesen Task
// und die zugehoerige dependsOn-Zeile unten -- es ist bewusst als
// eigenstaendiger Block gehalten, nicht mit dem Pitest-Block oben verwoben.
tasks.register("ausnahmenregister") {
    group = "verification"
    description = "Prueft, dass jede @EquivalentMutant- und @Disabled-Unterdrueckung in docs/test-ausnahmen.md steht."
    dependsOn(tasks.named("classes"), tasks.named("testClasses"))

    val registerFile = layout.projectDirectory.file("docs/test-ausnahmen.md")
    val mainClasses = sourceSets.getByName("main").output.classesDirs
    val testClassesDirs = sourceSets.getByName("test").output.classesDirs
    val classpath = sourceSets.getByName("test").runtimeClasspath
    val reportFile = layout.buildDirectory.file("reports/ausnahmenregister.txt")

    inputs.file(registerFile)
    inputs.files(mainClasses)
    inputs.files(testClassesDirs)
    outputs.file(reportFile)

    doLast {
        val urls = classpath.files.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, javaClass.classLoader)

        fun annotationClassOrNull(name: String): Class<out Annotation>? {
            @Suppress("UNCHECKED_CAST")
            return try {
                classLoader.loadClass(name) as Class<out Annotation>
            } catch (e: Throwable) {
                null
            }
        }

        // {{PACKAGE_BASE}}.mutationtest.EquivalentMutant ist Projektsache,
        // analog zu Watchpartys AequivalenterMutant -- eine eigene, leichte
        // Annotation, kein Bestandteil dieses Moduls.
        val equivalentMutant = annotationClassOrNull("{{PACKAGE_BASE}}.mutationtest.EquivalentMutant")
        val disabled = annotationClassOrNull("org.junit.jupiter.api.Disabled")

        fun collect(dirs: FileCollection, annotation: Class<out Annotation>?): Set<String> {
            if (annotation == null) return emptySet()
            val found = sortedSetOf<String>()
            dirs.forEach { rootDir ->
                rootDir.walkTopDown()
                    .filter { it.isFile && it.extension == "class" }
                    .forEach { classFile ->
                        val className = classFile.relativeTo(rootDir).path
                            .removeSuffix(".class").replace(File.separatorChar, '.')
                        val klass = try {
                            classLoader.loadClass(className)
                        } catch (e: Throwable) {
                            return@forEach
                        }
                        val simpleName = klass.simpleName
                        if (klass.getAnnotation(annotation) != null) {
                            found += simpleName
                        }
                        for (method in klass.declaredMethods) {
                            if (method.getAnnotation(annotation) != null) {
                                found += "$simpleName.${method.name}"
                            }
                        }
                    }
            }
            return found
        }

        val suppressions = collect(mainClasses, equivalentMutant) + collect(testClassesDirs, disabled)

        // Erste Spalte jeder Tabellenzeile, ohne Backticks und ohne
        // Platzhalterzeilen.
        val entries = sortedSetOf<String>()
        registerFile.asFile.forEachLine { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("|")) return@forEachLine
            val firstColumn = trimmed.trim('|').split("|").firstOrNull()?.trim()?.trim('`') ?: return@forEachLine
            if (firstColumn.isEmpty()) return@forEachLine
            if (firstColumn.startsWith("---")) return@forEachLine
            if (firstColumn == "Klasse/Methode" || firstColumn == "Test") return@forEachLine
            if (firstColumn.startsWith("_(")) return@forEachLine
            entries += firstColumn
        }

        val missingEntry = (suppressions - entries).sorted()
        val missingSuppression = (entries - suppressions).sorted()

        val report = buildString {
            appendLine("Ausnahmenregister: ${suppressions.size} Unterdrueckung(en) im Code, ${entries.size} Eintrag/Eintraege in docs/test-ausnahmen.md.")
            if (missingEntry.isEmpty() && missingSuppression.isEmpty()) {
                appendLine("Code und Register stimmen ueberein.")
            }
            if (missingEntry.isNotEmpty()) {
                appendLine("Ohne Eintrag im Register (${missingEntry.size}):")
                missingEntry.forEach { appendLine("  - $it") }
            }
            if (missingSuppression.isNotEmpty()) {
                appendLine("Eintrag ohne Entsprechung im Code (${missingSuppression.size}):")
                missingSuppression.forEach { appendLine("  - $it") }
            }
        }
        println(report)
        val file = reportFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(report)

        if (missingEntry.isNotEmpty()) {
            throw GradleException(
                "Unterdrueckung ohne Eintrag in docs/test-ausnahmen.md: ${missingEntry.joinToString(", ")} " +
                    "-- jede Ausnahme wird dort mit Begruendung und Datum benannt.")
        }
        if (missingSuppression.isNotEmpty()) {
            throw GradleException(
                "Karteileiche in docs/test-ausnahmen.md: ${missingSuppression.joinToString(", ")} " +
                    "-- im Code gibt es dazu keine Unterdrueckung mehr, der Eintrag gehoert entfernt.")
        }
    }
}

tasks.named("check") {
    dependsOn("ausnahmenregister")
}
