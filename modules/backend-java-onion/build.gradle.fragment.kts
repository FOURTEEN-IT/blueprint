// build.gradle.fragment.kts — Modul backend-java-onion
//
// Kein eigenständiges Buildfile: Die drei Abschnitte (Plugins, Dependencies,
// Tasks) werden in das Ziel-build.gradle.kts an den passenden Stellen
// zusammengeführt (Generator-Skill). Reihenfolge/Gruppierung folgt der
// Referenzimplementierung, damit ein Vergleich der beiden
// Buildfiles nicht durch reine Umsortierung erschwert wird.
//
// Platzhalter: {{PACKAGE_BASE}} (z. B. de.example.projectname),
// {{PROJECT_NAME}}, {{JAVA_VERSION}}. jacoco/pitest/Gate-Verdrahtung an
// `check` liegen bewusst NICHT hier, sondern im Modul quality-gates — dieses
// Fragment liefert nur, was die Onion-Struktur selbst braucht (NullAway,
// jMolecules, ArchUnit, die drei Testebenen-Tasks als Task-Definitionen).

import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import net.ltgt.gradle.errorprone.CheckSeverity

plugins {
    java
    id("org.springframework.boot") version "{{SPRING_BOOT_VERSION}}"
    id("io.spring.dependency-management") version "{{SPRING_DEPENDENCY_MANAGEMENT_VERSION}}"
    // Setzt JSpecify durch: ein @Nullable an der falschen Stelle ist ein
    // Compile-Fehler, keine Doku (ADR-026-Vorlage, docs/).
    id("net.ltgt.errorprone") version "{{ERRORPRONE_PLUGIN_VERSION}}"
    id("net.ltgt.nullaway") version "{{NULLAWAY_PLUGIN_VERSION}}"
}

group = "{{PROJECT_NAME}}"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of({{JAVA_VERSION}})
    }
}

dependencies {
    // Die Annotationen selbst (ADR-026-Vorlage): @NullMarked, @Nullable.
    // Reine Deklarationen ohne Laufzeitverhalten — die Durchsetzung macht
    // NullAway (siehe unten).
    implementation("org.jspecify:jspecify:{{JSPECIFY_VERSION}}")

    // DDD-Stereotypen (@AggregateRoot, @Entity, @ValueObject, @Identity,
    // @Service) und die Onion-Ring-Annotationen (ADR-027-Vorlage). Reine
    // Marker ohne Laufzeitverhalten wie JSpecify — die Durchsetzung macht
    // ArchUnit über selbstgeschriebene Regeln, nicht über die
    // vorgefertigten jmolecules-archunit-Regeln (siehe
    // ArchitectureTest.java.template für den Grund).
    implementation("org.jmolecules:jmolecules-ddd:{{JMOLECULES_VERSION}}")
    implementation("org.jmolecules:jmolecules-onion-architecture:{{JMOLECULES_VERSION}}")

    // Ohne Mockito: Test Doubles werden von Hand geschrieben (Konvention der
    // Referenzimplementierung). Der Ausschluss macht daraus eine Regel statt
    // einer Absprache — ein versehentliches mock(...) kompiliert nicht.
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }

    // Haelt die Ringregel aus ADR-024-Vorlage als Test fest. Version an
    // ArchUnit gebunden, nicht an jmolecules-archunit — siehe
    // ArchitectureTest.java.template.
    testImplementation("com.tngtech.archunit:archunit-junit5:{{ARCHUNIT_VERSION}}")

    // Ab Gradle 9 liegt der Launcher nicht mehr automatisch auf dem
    // Test-Classpath; ohne ihn startet der Test-Executor gar nicht erst.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone("com.google.errorprone:error_prone_core:{{ERRORPRONE_VERSION}}")
    errorprone("com.uber.nullaway:nullaway:{{NULLAWAY_VERSION}}")
}

// Wie in der Referenzimplementierung: jgiven-junit5/jqwik-engine (falls das Projekt sie
// einbindet) haengen eigene JUnit-Versionen an, die mit Spring Boots
// Dependency-Management fuer junit-platform-launcher auseinanderlaufen
// koennen (NoSuchMethodError auf NamespacedHierarchicalStore$CloseAction).
// eachDependency erzwingt vorab denselben Stand ueberall. {{JUNIT_VERSION}}
// ist der Stand, den die verwendete Spring-Boot-Version selbst managt.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.junit.jupiter") {
            useVersion("{{JUNIT_VERSION}}")
        }
        if (requested.group == "org.junit.platform") {
            useVersion("{{JUNIT_VERSION}}")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// --- Ebenen als Gradle-Tasks --------------------------------------------
//
// Getrennt wird ueber JUnit-Tags, nicht ueber eigene Source Sets: Test
// Doubles bleiben in einem gemeinsamen Quellbaum (src/test/java), erreichbar
// von jeder Ebene. `test` ist der schnelle Lauf (unit, port); `adapterTest`
// und `apiTest` kommen extra dazu, weil sie Spring bzw. einen echten Socket
// brauchen. `archTest` laeuft NICHT ueber Tags, sondern ueber die Engine
// (`includeEngines("archunit")`) — Fund aus der Referenzimplementierung:
// archunit-junit5-engine implementiert getTags() auf keinem seiner
// TestDescriptor-Knoten, jeder JUnit-Platform-TagFilter sortiert deshalb
// ALLE ArchUnit-Tests aus, unabhaengig von den gesetzten Tags. Ein
// Tag-Filter auf ArchitectureTest wuerde den Test dadurch unbemerkt leer
// laufen lassen — gruen, aber ohne dass er je etwas geprueft hat.
tasks.named<Test>("test") {
    useJUnitPlatform {
        includeTags("unit", "port")
    }
}

val adapterTest = tasks.register<Test>("adapterTest") {
    description = "Adapter-Ebene: kann der Adapter alles uebertragen, was der Port ausdrueckt?"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("adapter")
    }
    shouldRunAfter(tasks.test)
}

val apiTest = tasks.register<Test>("apiTest") {
    description = "API-Ebene: echter Server, echter Socket/Client."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("api")
    }
    shouldRunAfter(adapterTest)
}

// Struktur (`arch`): ueber die JUnit-Platform-Engine ausgewaehlt, nicht ueber
// einen Tag (Begruendung oben). Ohne Tag-Filter funktioniert die Engine wie
// dokumentiert; die Auswahl nach Engine statt nach Layer ist inhaltlich
// treffender, weil Architekturregeln nicht zu einer einzelnen Ebene
// gehoeren, sondern fuer alle gelten.
val archTest = tasks.register<Test>("archTest") {
    description = "Struktur: haelt der Bau die Ringe und Stereotypen ein?"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeEngines("archunit")
    }
    shouldRunAfter(tasks.test)
}

// Absichtlich KEIN `tasks.named("check") { dependsOn(...) }` hier: Ob diese
// drei Tasks den Build verbindlich brechen, entscheidet das Modul
// quality-gates. Dieses Fragment definiert nur die Tasks selbst.

// --- Null-Sicherheit -----------------------------------------------------
//
// NullAway prueft im JSpecify-Modus nur Code, der explizit @NullMarked ist
// (package-info.java in domain, application, adapter, config) -- alles
// andere (Spring/Jackson/JDK) bleibt "legacy" und wird nicht mitgeprueft.
// Innerhalb der markierten Pakete ist jeder Verweistyp nicht-null, ausser er
// traegt @Nullable; ein Verstoss ist ein Compile-Fehler.
//
// Nur NullAway laeuft, keine der uebrigen Error-Prone-Pruefungen -- diese
// Einrichtung soll Null-Sicherheit durchsetzen, keinen Stilkatalog.
tasks.withType<JavaCompile>().configureEach {
    // Von Error Prone selbst verlangt, sobald der Compiler Typannotationen
    // (wie @Nullable auf Feldern/Parametern) an Symbole binden soll.
    options.compilerArgs.add("-XDaddTypeAnnotationsToSymbol=true")
    options.errorprone {
        disableAllChecks.set(true)
        // Die getypte NullAway-DSL statt roher -Xep-Optionen: Wer die
        // Severity per option("NullAway:...") von Hand setzt, konkurriert
        // mit der Konfiguration, die dieses Plugin selbst schon einhaengt,
        // und die zuletzt geschriebene Xep-Flagge gewinnt.
        nullaway {
            severity.set(CheckSeverity.ERROR)
            onlyNullMarked.set(true)
            jspecifyMode.set(true)
        }
    }
}

// Testcode ist bewusst nicht @NullMarked -- NullAway liesse ihn ohnehin
// durch, aber ohne Error Prone ueberhaupt erst anzuwerfen spart das Zeit und
// macht die Abgrenzung im Build sichtbar.
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.enabled.set(false)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
