-- Beispielmigration nach Flyway-Namenskonvention (V<n>__<beschreibung>.sql).
-- Fachfrei: `example_entity` steht stellvertretend fuer das erste Aggregat,
-- das beim Einspielen dieses Moduls tatsaechlich persistiert werden soll --
-- Name und Spalten sind beim Einspielen zu ersetzen, nicht zu uebernehmen.
--
-- Vorbild: src/main/resources/db/league/migration/V1__create_account.sql
-- (Referenzimplementierung).
CREATE TABLE example_entity (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
