package {{PACKAGE_BASE}}.domain.model;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;

/**
 * Entity: hat eine Identitaet ({@link ExampleId}), die ueber die Zeit
 * gleichbleibt, waehrend sich ihr Zustand aendert.
 *
 * Fachfrei benanntes Beispiel — siehe {@link ExampleId} fuer den Verweis auf
 * die echten Entities der Referenzimplementierung ({@code Round},
 * {@code Player}).
 *
 * Mutatoren sind paket-privat: Das ist die Aggregatgrenze (nur der
 * {@link ExampleAggregate}, im selben Paket, darf den Zustand aendern), kein
 * oeffentlicher Setter — {@code ArchitectureTest.keineOeffentlichenSetterAufEntities}
 * haelt das nach.
 */
@Entity
public final class ExampleEntity {

    @Identity
    private final ExampleId id;

    private String description;

    ExampleEntity(ExampleId id, String description) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
    }

    public ExampleId id() {
        return id;
    }

    public String description() {
        return description;
    }

    /** Benannter Uebergang statt Setter — nur vom Aggregate Root aufgerufen. */
    void changeDescription(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }
}
