package br.com.devready.application.career.fake;

import br.com.devready.application.career.dto.CareerSeed;
import br.com.devready.application.career.dto.CompanySeed;
import br.com.devready.application.career.dto.CompetencySeed;
import br.com.devready.application.career.dto.MaterialSeed;
import br.com.devready.application.career.dto.StackSeed;
import br.com.devready.application.career.dto.TrackSeed;
import br.com.devready.application.career.dto.TrackUnitSeed;
import java.util.List;

public final class CareerSeedFixtures {

    private CareerSeedFixtures() {}

    public static CompanySeed company(String slug) {
        return new CompanySeed(slug, slug, "tech-brasil", "", List.of());
    }

    public static StackSeed stack(String slug, String area) {
        return new StackSeed(slug, slug, area);
    }

    public static CompetencySeed competency(
            String slug, String category, String difficulty, String... tags) {
        return new CompetencySeed(
                slug,
                "Nome de " + slug,
                category,
                "Resumo de " + slug + ".",
                difficulty,
                List.of(tags));
    }

    public static MaterialSeed material(String competency, String title) {
        return new MaterialSeed(
                competency, title, "https://exemplo.com/" + competency, "documentacao", "pt", true);
    }

    public static TrackUnitSeed unit(
            String competency, double weight, String requirement, int jobs) {
        return new TrackUnitSeed(competency, weight, requirement, jobs);
    }

    public static TrackSeed track(
            String company,
            String area,
            String stack,
            String seniority,
            int jobs,
            TrackUnitSeed... units) {
        return new TrackSeed(company, area, stack, seniority, jobs, List.of(units));
    }

    public static CareerSeed seed() {
        return new CareerSeed(
                List.of(company("nubank"), company("google")),
                List.of(stack("java-spring", "backend"), stack("react", "frontend")),
                List.of(
                        competency("sql", "fundamento", "medium", "sql"),
                        competency("java", "linguagem", "medium", "java")),
                List.of(
                        material("sql", "Use The Index, Luke"),
                        material("sql", "Tutorial do PostgreSQL")),
                List.of(
                        track(
                                "nubank",
                                "backend",
                                "todas",
                                "todas",
                                17,
                                unit("sql", 0.41, "obrigatoria", 7),
                                unit("java", 0.29, "desejavel", 5)),
                        track(
                                "google",
                                "backend",
                                "java-spring",
                                "junior",
                                9,
                                unit("java", 0.89, "obrigatoria", 8))));
    }
}
