package br.com.devready.domain.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackFusionTest {

    private static final Requirement O = Requirement.OBRIGATORIA;
    private static final Requirement D = Requirement.DESEJAVEL;

    private static final Map<String, Competency> COMPETENCIES =
            Map.of(
                    "eda", competency("eda", "Estruturas de dados", CompetencyCategory.FUNDAMENTO),
                    "sql", competency("sql", "SQL", CompetencyCategory.FUNDAMENTO),
                    "java", competency("java", "Java", CompetencyCategory.LINGUAGEM),
                    "spring", competency("spring", "Spring", CompetencyCategory.FRAMEWORK),
                    "testes", competency("testes", "Testes", CompetencyCategory.PRATICA),
                    "ingles", competency("ingles", "Inglês", CompetencyCategory.COMPORTAMENTAL));

    private static Competency competency(String slug, String name, CompetencyCategory category) {
        return new Competency(
                UUID.randomUUID(),
                slug,
                name,
                category,
                "",
                Difficulty.MEDIUM,
                Set.of(),
                List.of());
    }

    private static TrackUnit unit(String slug, double weight, Requirement requirement, int jobs) {
        return new TrackUnit(slug, weight, requirement, jobs);
    }

    private static TrackUnit unit(String slug) {
        return unit(slug, 0.5, O, 1);
    }

    private static Track track(
            String company, String stack, Level seniority, int jobs, TrackUnit... units) {
        return new Track(
                UUID.randomUUID(), company, "backend", stack, seniority, jobs, List.of(units));
    }

    private static TrackGoal goal(String... companies) {
        return new TrackGoal(List.of(companies), "backend", "java-spring", Level.JUNIOR);
    }

    private static FusedTrack fuse(TrackGoal goal, Track... tracks) {
        return TrackFusion.fuse(goal, List.of(tracks), COMPETENCIES);
    }

    private static List<String> slugs(FusedTrack track) {
        return track.units().stream().map(FusedUnit::competencySlug).toList();
    }

    private static FusedUnit unitOf(FusedTrack track, String slug) {
        return track.units().stream()
                .filter(u -> u.competencySlug().equals(slug))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void choosesTheExactTrackFirst() {
        FusedTrack fused =
                fuse(
                        goal("amazon"),
                        track("amazon", null, null, 205, unit("eda")),
                        track("amazon", "java-spring", null, 96, unit("eda")),
                        track("amazon", null, Level.JUNIOR, 8, unit("eda")),
                        track("amazon", "java-spring", Level.JUNIOR, 5, unit("eda")));

        assertThat(fused.jobsAnalyzed()).isEqualTo(5);
    }

    @Test
    void seniorityComesBeforeTheStack() {
        FusedTrack fused =
                fuse(
                        goal("amazon"),
                        track("amazon", null, null, 205, unit("eda")),
                        track("amazon", "java-spring", null, 96, unit("eda")),
                        track("amazon", null, Level.JUNIOR, 8, unit("eda")));

        assertThat(fused.jobsAnalyzed()).isEqualTo(8);
    }

    @Test
    void thenTheStackInAllSeniorities() {
        FusedTrack fused =
                fuse(
                        goal("google"),
                        track("google", null, null, 24, unit("eda")),
                        track("google", null, Level.PLENO, 3, unit("eda")),
                        track("google", "java-spring", null, 9, unit("eda")));

        assertThat(fused.jobsAnalyzed()).isEqualTo(9);
    }

    @Test
    void thenTheWholeArea() {
        FusedTrack fused =
                fuse(
                        goal("nubank"),
                        track("nubank", "node", null, 6, unit("eda")),
                        track("nubank", null, Level.SENIOR, 16, unit("eda")),
                        track("nubank", null, null, 17, unit("eda")));

        assertThat(fused.jobsAnalyzed()).isEqualTo(17);
    }

    @Test
    void anotherStackAtTheGoalSeniorityIsNotTheAreaTrackAtThatSeniority() {
        FusedTrack fused =
                fuse(
                        goal("nubank"),
                        track("nubank", "node", Level.JUNIOR, 4, unit("eda")),
                        track("nubank", null, null, 17, unit("eda")));

        assertThat(fused.jobsAnalyzed()).isEqualTo(17);
    }

    @Test
    void companyWithoutTrackInTheAreaIsLeftOutInTheGoalOrder() {
        Track frontend =
                new Track(
                        UUID.randomUUID(),
                        "picpay",
                        "frontend",
                        null,
                        null,
                        2,
                        List.of(unit("eda")));

        FusedTrack fused =
                fuse(
                        goal("meta", "nubank", "picpay"),
                        track("nubank", null, null, 17, unit("eda")),
                        frontend);

        assertThat(fused.missingCompanies()).containsExactly("meta", "picpay");
        assertThat(fused.jobsAnalyzed()).isEqualTo(17);
        assertThat(unitOf(fused, "eda").askedBy()).containsExactly("nubank");
    }

    @Test
    void withoutAnyTrackTheResultIsEmpty() {
        FusedTrack fused = fuse(goal("meta", "stone"));

        assertThat(fused.units()).isEmpty();
        assertThat(fused.jobsAnalyzed()).isZero();
        assertThat(fused.missingCompanies()).containsExactly("meta", "stone");
    }

    @Test
    void weightIsTheMeanTimesTheShareOfCompaniesThatAskForIt() {
        FusedTrack fused =
                fuse(
                        goal("a", "b", "c"),
                        track("a", null, null, 10, unit("eda", 0.8, O, 8), unit("java", 0.6, O, 6)),
                        track("b", null, null, 10, unit("eda", 0.5, O, 5)),
                        track("c", null, null, 10, unit("sql", 0.9, O, 9)));

        assertThat(unitOf(fused, "eda").weight()).isEqualTo(0.433);
        assertThat(unitOf(fused, "java").weight()).isEqualTo(0.2);
        assertThat(unitOf(fused, "sql").weight()).isEqualTo(0.3);
    }

    @Test
    void weightIsTheSumOverTheUsedCompaniesRoundedHalfUp() {
        FusedTrack fused =
                fuse(
                        goal("a", "b"),
                        track("a", null, null, 10, unit("eda", 0.1, O, 1)),
                        track("b", null, null, 10, unit("eda", 0.345, O, 3)));

        assertThat(unitOf(fused, "eda").weight()).isEqualTo(0.223);
    }

    @Test
    void weightDoesNotDependOnTheOrderOfTheCompanies() {
        Track a = track("a", null, null, 10, unit("eda", 0.74, O, 7));
        Track b = track("b", null, null, 10, unit("eda", 0.15, O, 2));
        Track c = track("c", null, null, 10, unit("eda", 0.64, O, 6));
        Track d = track("d", null, null, 10, unit("sql", 0.5, O, 5));

        double abc = unitOf(fuse(goal("a", "b", "c", "d"), a, b, c, d), "eda").weight();
        double acb = unitOf(fuse(goal("a", "c", "b", "d"), a, b, c, d), "eda").weight();

        assertThat(abc).isEqualTo(0.383);
        assertThat(acb).isEqualTo(0.383);
    }

    @Test
    void obligatoryWhenAnyUsedTrackRequiresIt() {
        FusedTrack fused =
                fuse(
                        goal("a", "b"),
                        track(
                                "a",
                                null,
                                null,
                                10,
                                unit("eda", 0.5, D, 5),
                                unit("ingles", 0.4, D, 4)),
                        track(
                                "b",
                                null,
                                null,
                                10,
                                unit("eda", 0.7, O, 7),
                                unit("ingles", 0.2, D, 2)));

        assertThat(unitOf(fused, "eda").requirement()).isEqualTo(O);
        assertThat(unitOf(fused, "ingles").requirement()).isEqualTo(D);
    }

    @Test
    void askedByFollowsTheGoalOrderAndJobsAreSummed() {
        FusedTrack fused =
                fuse(
                        goal("nubank", "google"),
                        track("google", "java-spring", null, 9, unit("eda", 1, O, 9)),
                        track(
                                "nubank",
                                null,
                                null,
                                17,
                                unit("eda", 0.53, O, 9),
                                unit("java", 0.29, D, 5)));

        assertThat(unitOf(fused, "eda").askedBy()).containsExactly("nubank", "google");
        assertThat(unitOf(fused, "eda").jobs()).isEqualTo(18);
        assertThat(unitOf(fused, "java").askedBy()).containsExactly("nubank");
        assertThat(fused.jobsAnalyzed()).isEqualTo(26);
    }

    @Test
    void ordersByCategoryThenByMeanPositionCountingAbsenceAsAfterTheLast() {
        FusedTrack fused =
                fuse(
                        goal("a", "b"),
                        track(
                                "a",
                                null,
                                null,
                                10,
                                unit("sql", 0.9, O, 9),
                                unit("eda", 0.5, O, 5),
                                unit("java", 0.5, O, 5)),
                        track(
                                "b",
                                null,
                                null,
                                10,
                                unit("eda", 0.5, O, 5),
                                unit("testes", 0.5, O, 5)));

        assertThat(slugs(fused)).containsExactly("eda", "sql", "java", "testes");
        assertThat(unitOf(fused, "eda").meanPosition()).isEqualTo(0.5);
        assertThat(unitOf(fused, "sql").meanPosition()).isEqualTo(1.0);
    }

    @Test
    void obligatoryDoesNotDependOnWhichTrackComesLast() {
        FusedTrack fused =
                fuse(
                        goal("a", "b"),
                        track("a", null, null, 10, unit("eda", 0.7, O, 7)),
                        track("b", null, null, 10, unit("eda", 0.5, D, 5)));

        assertThat(unitOf(fused, "eda").requirement()).isEqualTo(O);
    }

    @Test
    void positionComesBeforeWeightAndCategoryComesBeforePosition() {
        FusedTrack byPosition =
                fuse(
                        goal("a", "b"),
                        track("a", null, null, 10, unit("eda", 0.3, O, 3), unit("sql", 0.9, O, 9)),
                        track("b", null, null, 10, unit("eda", 0.3, O, 3), unit("sql", 0.9, O, 9)));
        assertThat(slugs(byPosition)).containsExactly("eda", "sql");

        FusedTrack byCategory =
                fuse(
                        goal("a", "b"),
                        track("a", null, null, 10, unit("java", 0.9, O, 9), unit("eda", 0.3, O, 3)),
                        track(
                                "b",
                                null,
                                null,
                                10,
                                unit("java", 0.9, O, 9),
                                unit("eda", 0.3, O, 3)));
        assertThat(slugs(byCategory)).containsExactly("eda", "java");
    }

    @Test
    void tiesGoToTheHigherWeightThenToTheName() {
        FusedTrack byWeight =
                fuse(
                        goal("a", "b"),
                        track("a", null, null, 10, unit("eda", 0.3, O, 3), unit("sql", 0.9, O, 9)),
                        track("b", null, null, 10, unit("sql", 0.9, O, 9), unit("eda", 0.3, O, 3)));
        assertThat(slugs(byWeight)).containsExactly("sql", "eda");

        FusedTrack byName =
                fuse(
                        goal("a", "b"),
                        track("a", null, null, 10, unit("sql", 0.6, O, 6), unit("eda", 0.6, O, 6)),
                        track("b", null, null, 10, unit("eda", 0.6, O, 6), unit("sql", 0.6, O, 6)));
        assertThat(slugs(byName)).containsExactly("eda", "sql");
    }

    @Test
    void aFullTieGoesToTheSlug() {
        Map<String, Competency> sameName =
                Map.of(
                        "git-b", competency("git-b", "Git", CompetencyCategory.PRATICA),
                        "git-a", competency("git-a", "Git", CompetencyCategory.PRATICA));
        FusedTrack fused =
                TrackFusion.fuse(
                        goal("a", "b"),
                        List.of(
                                track(
                                        "a",
                                        null,
                                        null,
                                        10,
                                        unit("git-b", 0.5, O, 5),
                                        unit("git-a", 0.5, O, 5)),
                                track(
                                        "b",
                                        null,
                                        null,
                                        10,
                                        unit("git-a", 0.5, O, 5),
                                        unit("git-b", 0.5, O, 5))),
                        sameName);

        assertThat(slugs(fused)).containsExactly("git-a", "git-b");
    }

    @Test
    void unknownCompetencyIsAnError() {
        assertThatThrownBy(() -> fuse(goal("a"), track("a", null, null, 10, unit("kotlin"))))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("kotlin");
    }

    @Test
    void goalNeedsAreaStackAndSeniority() {
        assertThat(new TrackGoal(null, "backend", "java-spring", Level.JUNIOR).companySlugs())
                .isEmpty();
        assertThatThrownBy(() -> new TrackGoal(List.of("a"), "backend", "java-spring", null))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(
                        () -> new TrackGoal(List.of("a"), "Back End", "java-spring", Level.JUNIOR))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new TrackGoal(List.of("a"), "backend", null, Level.JUNIOR))
                .isInstanceOf(CareerException.class);
    }
}
