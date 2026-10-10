package br.com.devready.domain.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackTest {

    private static TrackUnit unit(String slug, double weight, int jobs) {
        return new TrackUnit(slug, weight, Requirement.OBRIGATORIA, jobs);
    }

    @Test
    void stackAndSeniorityAreOptionalAndTheLabelSaysTodas() {
        Track area =
                new Track(
                        UUID.randomUUID(),
                        "nubank",
                        "backend",
                        null,
                        null,
                        17,
                        List.of(unit("sql", 0.41, 7)));
        Track junior =
                new Track(
                        UUID.randomUUID(),
                        "amazon",
                        "backend",
                        "java-spring",
                        Level.JUNIOR,
                        8,
                        List.of(unit("java", 0.75, 6)));

        assertThat(area.label()).isEqualTo("nubank/backend/todas/todas");
        assertThat(junior.label()).isEqualTo("amazon/backend/junior/java-spring");
        assertThat(Track.label("amazon", "backend", null, Level.PLENO))
                .isEqualTo("amazon/backend/pleno/todas");
    }

    @Test
    void positionIsTheIndexInTheListAndUnitsAreCopied() {
        List<TrackUnit> units =
                new ArrayList<>(
                        List.of(unit("design-de-sistemas", 0.65, 11), unit("sql", 0.41, 7)));
        Track track = new Track(UUID.randomUUID(), "nubank", "backend", null, null, 17, units);

        units.add(unit("java", 0.29, 5));

        assertThat(track.units()).hasSize(2);
        assertThat(track.positionOf("design-de-sistemas")).isZero();
        assertThat(track.positionOf("sql")).isEqualTo(1);
        assertThat(track.positionOf("java")).isEqualTo(-1);
    }

    @Test
    void rejectsInvalidTracks() {
        UUID id = UUID.randomUUID();
        List<TrackUnit> units = List.of(unit("sql", 0.5, 5));
        assertThatThrownBy(() -> new Track(null, "nubank", "backend", null, null, 10, units))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new Track(id, "Nu Bank", "backend", null, null, 10, units))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new Track(id, "nubank", "backend", "Java Spring", null, 10, units))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new Track(id, "nubank", "backend", null, null, 0, units))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("ao menos uma vaga");
        assertThatThrownBy(() -> new Track(id, "nubank", "backend", null, null, 10, List.of()))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("sem competências");
        assertThatThrownBy(
                        () ->
                                new Track(
                                        id,
                                        "nubank",
                                        "backend",
                                        null,
                                        null,
                                        10,
                                        List.of(unit("sql", 0.5, 5), unit("sql", 0.4, 4))))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("sql")
                .hasMessageContaining("repetida");
        assertThatThrownBy(
                        () ->
                                new Track(
                                        id,
                                        "nubank",
                                        "backend",
                                        null,
                                        null,
                                        10,
                                        List.of(unit("sql", 0.5, 11))))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("mais vagas");
    }

    @Test
    void unitWeightStaysBetweenZeroAndOne() {
        assertThat(new TrackUnit("sql", 0, Requirement.DESEJAVEL, 0).weight()).isZero();
        assertThat(new TrackUnit("sql", 1, Requirement.OBRIGATORIA, 3).weight()).isEqualTo(1);
        assertThatThrownBy(() -> new TrackUnit("sql", 1.4, Requirement.OBRIGATORIA, 3))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("Peso fora de 0 a 1");
        assertThatThrownBy(() -> new TrackUnit("sql", -0.1, Requirement.OBRIGATORIA, 3))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new TrackUnit("sql", Double.NaN, Requirement.OBRIGATORIA, 3))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new TrackUnit("sql", 0.5, null, 3))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new TrackUnit("sql", 0.5, Requirement.OBRIGATORIA, -1))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("negativas");
        assertThatThrownBy(() -> new TrackUnit("SQL", 0.5, Requirement.OBRIGATORIA, 1))
                .isInstanceOf(CareerException.class);
    }
}
