// persistence-postgres-flyway — Dependency-Zeilen zum Einspielen in das
// build.gradle.kts des Zielprojekts. Kein eigenständiges Build-Skript,
// sondern ein Fragment: die Zeilen gehören in den bestehenden
// dependencies { ... }-Block.
//
// Standard-JDBC-Weg von Spring Boot, kein Spring Data -- Repository-Adapter
// sprechen JdbcTemplate direkt, Migrationen laufen ueber Flyway von Hand
// (DataSource-/Flyway-Autoconfiguration ist im Zielprojekt auszuschalten,
// siehe README.md dieses Moduls).

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Datenbanken in Tests kommen ausschliesslich aus Testcontainers -- auf
    // jeder Ebene, auch in E2E. Kein selbst abgesetztes `docker run` in
    // einem Start- oder CI-Skript.
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
}
