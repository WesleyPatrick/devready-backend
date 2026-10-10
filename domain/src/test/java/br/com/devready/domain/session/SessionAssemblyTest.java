package br.com.devready.domain.session;

import static br.com.devready.domain.session.SessionFixtures.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionAssemblyTest {

    private static RandomSource seeded() {
        return new Random(7)::nextInt;
    }

    private final List<UUID> pool = List.of(id(1), id(2), id(3), id(4), id(5), id(6), id(7));

    @Test
    void drawsDistinctItemsOfThePoolAndTheSameSeedGivesTheSameDraw() {
        List<UUID> drawn = SessionAssembly.draw(pool, 4, seeded());

        assertThat(drawn).hasSize(4).doesNotHaveDuplicates();
        assertThat(pool).containsAll(drawn);
        assertThat(SessionAssembly.draw(pool, 4, seeded())).isEqualTo(drawn);
        assertThat(SessionAssembly.draw(pool.reversed(), 4, seeded())).isEqualTo(drawn);
        assertThat(SessionAssembly.draw(pool, 7, seeded()))
                .containsExactlyInAnyOrderElementsOf(pool);
    }

    @Test
    void differentSeedsShuffleDifferently() {
        assertThat(SessionAssembly.draw(pool, 7, new Random(1)::nextInt))
                .isNotEqualTo(SessionAssembly.draw(pool, 7, new Random(2)::nextInt));
    }

    @Test
    void aDrawLargerThanThePoolIsABug() {
        assertThatThrownBy(() -> SessionAssembly.draw(pool, 8, seeded()))
                .isInstanceOf(SessionException.class);
    }

    @Test
    void theLessonPutsTheUnsolvedFirstAndStopsAtEight() {
        List<UUID> nine = List.of(id(1), id(2), id(3), id(4), id(5), id(6), id(7), id(8), id(9));
        Set<UUID> solved = Set.of(id(1), id(2), id(3));

        List<UUID> lesson = SessionAssembly.lesson(nine, solved, seeded());

        assertThat(lesson).hasSize(8).doesNotHaveDuplicates();
        assertThat(lesson.subList(0, 6)).doesNotContainAnyElementsOf(solved);
        assertThat(lesson.subList(6, 8)).allMatch(solved::contains);
    }

    @Test
    void withFewerThanThreeThereIsNoLesson() {
        assertThat(SessionAssembly.lesson(List.of(id(1), id(2)), Set.of(), seeded())).isEmpty();
        assertThat(SessionAssembly.lesson(List.of(id(1), id(2), id(3)), Set.of(id(1)), seeded()))
                .hasSize(3)
                .endsWith(id(1));
    }
}
