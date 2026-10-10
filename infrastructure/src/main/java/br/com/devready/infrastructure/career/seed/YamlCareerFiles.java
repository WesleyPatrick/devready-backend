package br.com.devready.infrastructure.career.seed;

import java.util.List;

final class YamlCareerFiles {

    private YamlCareerFiles() {}

    record CompanyFile(
            String slug, String name, String group, String about, List<String> careers) {}

    record StackFile(String slug, String name, String area) {}

    record CompetencyFile(
            String slug,
            String name,
            String category,
            String summary,
            String difficulty,
            List<String> tags) {}

    record MaterialFile(
            String competency,
            String title,
            String url,
            String type,
            String language,
            Boolean free) {}

    record TrackFile(String company, List<TrackItem> tracks) {}

    record TrackItem(
            String area, String stack, String seniority, Integer jobs, List<UnitItem> units) {}

    record UnitItem(String competency, Double weight, String requirement, Integer jobs) {}
}
