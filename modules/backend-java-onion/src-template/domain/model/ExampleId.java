package {{PACKAGE_BASE}}.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: die Identitaet eines {@link ExampleAggregate}.
 *
 * Fachfrei benanntes Beispiel-Trio ({@code ExampleAggregate}/
 * {@code ExampleEntity}/{@code ExampleId}) — es zeigt nur die Bauweise, kein
 * Fachkonzept. Im echten Fachmodell traegt jede Identitaet ihren eigenen
 * Typ, gegeneinander nicht austauschbar — ein Vertauschen zweier IDs ist
 * dann ein Kompilierfehler, nicht ein Laufzeitfehler.
 *
 * Ein Value Object ist ueber seine Werte gleich, nie ueber eine separate
 * Identitaet — deshalb {@code equals}/{@code hashCode} auf {@code value},
 * kein {@code @Identity}-Feld wie bei einer Entity.
 */
@ValueObject
public final class ExampleId {

    private final UUID value;

    private ExampleId(UUID value) {
        this.value = value;
    }

    public static ExampleId newId() {
        return new ExampleId(UUID.randomUUID());
    }

    public static ExampleId of(UUID value) {
        return new ExampleId(Objects.requireNonNull(value, "value"));
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ExampleId that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
