// Modul frontend-react-vite -- Gradle<->npm-Verdrahtung.
//
// Herkunft: eins zu eins aus der Referenzimplementierung (build.gradle.kts) uebernommen und
// von Fachbegriffen befreit. Vor dem Einspielen in ein Zielprojekt:
//   - {{PROJECT_NAME}} durch den tatsaechlichen Projektnamen ersetzen
//   - Pruefen, ob "processResources" der richtige Anknuepfungspunkt ist
//     (bei einem reinen Spring-Boot-Jar ja; bei einem anderen Build-Tool
//     entsprechend uebertragen)
//
// Einspielen: Diesen Block an das Ende von build.gradle.kts des Zielprojekts
// haengen (oder per `apply(from = ...)` einbinden).

// --- Frontend-Build -------------------------------------------------------
// Baut die React-App und legt das Ergebnis in build/frontend ab, von wo es
// als statische Ressource in das Jar wandert. Braucht npm auf dem PATH.
// In einem Docker-Build, der das Frontend separat baut, laesst sich dieser
// Schritt mit -PskipFrontend ueberspringen -- der Sinn der Property ist eine
// schnelle Backend-Only-Dev-Schleife (Backend mit `-PskipFrontend`, Frontend
// separat per `npm run dev`), nicht nur der Docker-Fall.

val frontendDir = layout.projectDirectory.dir("frontend")
val frontendOut = layout.buildDirectory.dir("frontend/static")
val skipFrontend = providers.gradleProperty("skipFrontend").isPresent
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val npm = if (isWindows) "npm.cmd" else "npm"

val npmInstall = tasks.register<Exec>("npmInstall") {
    workingDir = frontendDir.asFile
    commandLine(npm, "install")
    inputs.file(frontendDir.file("package.json"))
    outputs.dir(frontendDir.dir("node_modules"))
}

val npmBuild = tasks.register<Exec>("npmBuild") {
    dependsOn(npmInstall)
    workingDir = frontendDir.asFile
    commandLine(npm, "run", "build", "--", "--outDir", frontendOut.get().asFile.absolutePath, "--emptyOutDir")
    inputs.dir(frontendDir.dir("src"))
    inputs.file(frontendDir.file("index.html"))
    outputs.dir(frontendOut)
}

tasks.named<ProcessResources>("processResources") {
    if (!skipFrontend) {
        dependsOn(npmBuild)
        from(frontendOut) {
            into("static")
        }
    }
}

// --- Frontend-Ebene: Testlauf ---------------------------------------------
//
// Eigene Testebene neben Unit-/Integrationstests im Backend: Vitest prueft
// die Projektion Serverdaten -> sichtbare Ausgabe, keine fachliche Regel ein
// zweites Mal (Details: docs/frontend-testebene.md).
val npmTest = tasks.register<Exec>("npmTest") {
    group = "verification"
    description = "Frontend-Ebene: Vitest ueber frontend/tests."
    dependsOn(npmInstall)
    workingDir = frontendDir.asFile
    commandLine(npm, "run", "test")
    inputs.dir(frontendDir.dir("src"))
    inputs.dir(frontendDir.dir("tests"))
    inputs.file(frontendDir.file("package.json"))
    inputs.file(frontendDir.file("vite.config.js"))
    outputs.file(layout.buildDirectory.file("reports/frontend-tests.json"))
}

tasks.named("check") {
    dependsOn(npmTest)
}

// --- Abdeckungs-Gate (optional, projektspezifisch) ------------------------
//
// In der Referenzimplementierung gibt es zusaetzlich einen Task `abdeckungFrontend`, der
// jede mit der Marke "frontend" versehene Anforderungs-ID aus einem
// Anforderungsdokument gegen die requirement(...)-Aufrufe in frontend/tests
// (und einer moeglichen E2E-Ebene) abgleicht -- Regeln ohne Szenario und
// Test-IDs ohne bekannte Anforderung fallen beide auf. Das Konzept setzt ein
// eigenes Anforderungsdokument mit fester Tabellenform voraus (siehe
// requirement.js), die dieses generische Geruest nicht mitbringt.
//
// Ob und wie ein Zielprojekt das nachbaut, ist eine offene Erweiterung
// dieses Moduls -- hier absichtlich nicht erfunden, weil die Form des
// Anforderungsdokuments je nach Projekt unterschiedlich sein kann. Wer es
// nachbauen will: build.gradle.kts der Referenzimplementierung, Task `abdeckungFrontend`
// (sucht `## Anhang A`, parst eine Markdown-Tabelle, gleicht sie mit
// `requirement\(...\)`-Aufrufen ab) als Vorlage nehmen.
