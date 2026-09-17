package {{PACKAGE_BASE}}.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Root: die Konsistenzgrenze. Aeussere Aufrufer aendern
 * {@link ExampleEntity} niemals direkt, sondern ausschliesslich ueber
 * benannte Uebergaenge hier — nie ueber einen oeffentlichen Setter
 * ({@code ArchitectureTest.keineOeffentlichenSetterAufDemAggregateRoot}).
 *
 * Fachfrei benanntes Beispiel-Trio, das nur die Bauweise demonstriert. Als
 * Referenzbeispiel fuer ein echtes Aggregat mit Entities, Value Objects und
 * benannten Uebergaengen (statt Setter) siehe {@code Room} im
 * Watchparty-Projekt ({@code closeCurrentRound}, {@code addPick}, ...) —
 * nicht kopiert, weil {@code Room} Fachlogik traegt, die in diesem
 * Blueprint-Modul nichts zu suchen hat.
 *
 * Traegt bewusst kein {@code @Identity}: Ob ein Aggregat selbst eine
 * Identitaet braucht, haengt vom Projekt ab (in der
 * Referenzimplementierung hat der Aggregate Root {@code Room} seit einer
 * spaeteren Erweiterung eine {@code RoomCode}-Identitaet, weil ein Prozess
 * mehrere Instanzen davon haelt — anfangs, mit genau einer Instanz pro
 * Prozess, hatte er keine). Wird eine gebraucht, ein Feld nach demselben
 * Muster wie {@link ExampleEntity#id} ergaenzen.
 */
@AggregateRoot
public final class ExampleAggregate {

    private final List<ExampleEntity> entities = new ArrayList<>();

    public static ExampleAggregate empty() {
        return new ExampleAggregate();
    }

    private ExampleAggregate() {
    }

    public List<ExampleEntity> entities() {
        return List.copyOf(entities);
    }

    /** Benannter Uebergang: fuegt eine neue Entity hinzu. */
    public void addEntity(ExampleId id, String description) {
        entities.add(new ExampleEntity(Objects.requireNonNull(id, "id"), description));
    }

    /** Benannter Uebergang: aendert eine bestehende Entity ueber ihre Identitaet. */
    public void renameEntity(ExampleId id, String newDescription) {
        entities.stream()
                .filter(entity -> entity.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("keine Entity mit " + id))
                .changeDescription(newDescription);
    }
}
