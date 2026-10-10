package br.com.devready.infrastructure.catalog.seed;

import br.com.devready.application.catalog.dto.AreaSeed;
import br.com.devready.application.catalog.dto.CatalogSeed;
import br.com.devready.application.catalog.dto.ChallengeSeed;
import br.com.devready.application.catalog.dto.QuestionSeed;
import br.com.devready.application.catalog.dto.TagSeed;
import br.com.devready.application.catalog.exception.InvalidSeedException;
import br.com.devready.application.catalog.port.CatalogSeedSource;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class YamlCatalogSeedSource implements CatalogSeedSource {

    private final String location;
    private final YAMLMapper yaml = YAMLMapper.builder().build();
    private final JsonMapper json = JsonMapper.builder().build();
    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    public YamlCatalogSeedSource(String location) {
        this.location =
                location.endsWith("/") ? location.substring(0, location.length() - 1) : location;
    }

    @Override
    public CatalogSeed load() {
        List<YamlSeedFiles.AreaFile> areas =
                read(location + "/areas.yml", new TypeReference<List<YamlSeedFiles.AreaFile>>() {});
        List<YamlSeedFiles.TagFile> tags =
                read(location + "/tags.yml", new TypeReference<List<YamlSeedFiles.TagFile>>() {});
        List<QuestionSeed> questions = new ArrayList<>();
        for (Resource resource : sorted(location + "/questions/*.yml")) {
            YamlSeedFiles.QuestionsFile file = read(resource, YamlSeedFiles.QuestionsFile.class);
            Level level = level(file.level(), resource);
            for (YamlSeedFiles.QuestionItem item : file.questions()) {
                List<QuestionSeed.OptionSeed> options =
                        item.options().stream()
                                .map(
                                        o ->
                                                new QuestionSeed.OptionSeed(
                                                        o.text(), Boolean.TRUE.equals(o.correct())))
                                .toList();
                questions.add(
                        new QuestionSeed(
                                item.slug(),
                                item.title(),
                                file.area(),
                                level,
                                enumValue(
                                        Difficulty.class,
                                        item.difficulty(),
                                        "dificuldade da questão " + item.slug(),
                                        resource),
                                item.statement(),
                                item.explanation(),
                                options,
                                item.tags() == null ? List.of() : item.tags()));
            }
        }
        List<ChallengeSeed> challenges = new ArrayList<>();
        for (Resource resource : sorted(location + "/challenges/*.yml")) {
            YamlSeedFiles.ChallengeFile file = read(resource, YamlSeedFiles.ChallengeFile.class);
            Map<Language, ChallengeSeed.LanguageSeed> languages = new LinkedHashMap<>();
            file.languages()
                    .forEach(
                            (key, value) ->
                                    languages.put(
                                            enumValue(Language.class, key, "linguagem", resource),
                                            new ChallengeSeed.LanguageSeed(
                                                    value.functionName(),
                                                    value.starterCode(),
                                                    value.referenceSolution())));
            List<ChallengeSeed.TestCaseSeed> cases =
                    file.testCases().stream()
                            .map(
                                    tc ->
                                            new ChallengeSeed.TestCaseSeed(
                                                    toJson(tc.args()),
                                                    toJson(tc.expected()),
                                                    Boolean.TRUE.equals(tc.visible())))
                            .toList();
            challenges.add(
                    new ChallengeSeed(
                            file.slug(),
                            file.title(),
                            file.area(),
                            level(file.level(), resource),
                            enumValue(Difficulty.class, file.difficulty(), "dificuldade", resource),
                            file.statement(),
                            file.approachSummary(),
                            file.timeLimitMs() == null ? 5000 : file.timeLimitMs(),
                            file.memoryLimitKb() == null ? 262144 : file.memoryLimitKb(),
                            enumValue(
                                    ComparisonMode.class,
                                    file.comparison(),
                                    "modo de comparação",
                                    resource),
                            file.maxSubmissions() == null ? 5 : file.maxSubmissions(),
                            languages,
                            cases,
                            file.tags() == null ? List.of() : file.tags()));
        }
        return new CatalogSeed(
                areas.stream()
                        .map(
                                a ->
                                        new AreaSeed(
                                                a.slug(),
                                                a.name(),
                                                a.questionCount(),
                                                a.challengeCount(),
                                                a.durationMinutes(),
                                                a.objectiveWeight()))
                        .toList(),
                tags.stream().map(t -> new TagSeed(t.slug(), t.name())).toList(),
                questions,
                challenges);
    }

    private List<Resource> sorted(String pattern) {
        try {
            Resource[] resources = resolver.getResources(pattern);
            return Arrays.stream(resources)
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .toList();
        } catch (FileNotFoundException e) {
            return List.of();
        } catch (IOException e) {
            throw new InvalidSeedException(
                    "Não foi possível listar " + pattern + ": " + e.getMessage(), e);
        }
    }

    private <T> T read(String path, TypeReference<T> type) {
        Resource resource = resolver.getResource(path);
        try (InputStream in = resource.getInputStream()) {
            return yaml.readValue(in, type);
        } catch (JacksonException e) {
            throw new InvalidSeedException(
                    "Não foi possível ler " + path + ": " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new InvalidSeedException(
                    "Não foi possível ler " + path + ": " + e.getMessage(), e);
        }
    }

    private <T> T read(Resource resource, Class<T> type) {
        try (InputStream in = resource.getInputStream()) {
            return yaml.readValue(in, type);
        } catch (JacksonException e) {
            throw new InvalidSeedException(
                    "Não foi possível ler " + resource + ": " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new InvalidSeedException(
                    "Não foi possível ler " + resource + ": " + e.getMessage(), e);
        }
    }

    private static Level level(String value, Resource resource) {
        return enumValue(Level.class, value, "nível", resource);
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type, String value, String field, Resource resource) {
        if (value == null || value.isBlank()) {
            throw new InvalidSeedException(
                    "Arquivo sem " + field + ": " + resource.getFilename() + ".");
        }
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidSeedException(
                    "Valor de "
                            + field
                            + " desconhecido em "
                            + resource.getFilename()
                            + ": \""
                            + value
                            + "\".");
        }
    }

    private String toJson(Object value) {
        return json.writeValueAsString(value);
    }
}
