package br.com.devready.infrastructure.career.seed;

import br.com.devready.application.career.dto.CareerSeed;
import br.com.devready.application.career.dto.CompanySeed;
import br.com.devready.application.career.dto.CompetencySeed;
import br.com.devready.application.career.dto.MaterialSeed;
import br.com.devready.application.career.dto.StackSeed;
import br.com.devready.application.career.dto.TrackSeed;
import br.com.devready.application.career.dto.TrackUnitSeed;
import br.com.devready.application.career.exception.InvalidCareerSeedException;
import br.com.devready.application.career.port.CareerSeedSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class YamlCareerSeedSource implements CareerSeedSource {

    private final String location;
    private final YAMLMapper yaml =
            YAMLMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    public YamlCareerSeedSource(String location) {
        this.location =
                location.endsWith("/") ? location.substring(0, location.length() - 1) : location;
    }

    @Override
    public CareerSeed load() {
        List<YamlCareerFiles.CompanyFile> companies =
                read(
                        location + "/companies.yml",
                        new TypeReference<List<YamlCareerFiles.CompanyFile>>() {});
        List<YamlCareerFiles.StackFile> stacks =
                read(
                        location + "/stacks.yml",
                        new TypeReference<List<YamlCareerFiles.StackFile>>() {});
        List<YamlCareerFiles.CompetencyFile> competencies =
                read(
                        location + "/competencies.yml",
                        new TypeReference<List<YamlCareerFiles.CompetencyFile>>() {});
        List<YamlCareerFiles.MaterialFile> materials =
                read(
                        location + "/materials.yml",
                        new TypeReference<List<YamlCareerFiles.MaterialFile>>() {});
        return new CareerSeed(
                companies.stream()
                        .map(
                                c ->
                                        new CompanySeed(
                                                c.slug(),
                                                c.name(),
                                                c.group(),
                                                c.about() == null ? "" : c.about(),
                                                c.careers() == null ? List.of() : c.careers()))
                        .toList(),
                stacks.stream().map(s -> new StackSeed(s.slug(), s.name(), s.area())).toList(),
                competencies.stream()
                        .map(
                                c ->
                                        new CompetencySeed(
                                                c.slug(),
                                                c.name(),
                                                c.category(),
                                                c.summary() == null ? "" : c.summary(),
                                                c.difficulty(),
                                                c.tags()))
                        .toList(),
                materials.stream()
                        .map(
                                m ->
                                        new MaterialSeed(
                                                m.competency(),
                                                m.title(),
                                                m.url(),
                                                m.type(),
                                                m.language(),
                                                Boolean.TRUE.equals(m.free())))
                        .toList(),
                readTracks());
    }

    private List<TrackSeed> readTracks() {
        List<TrackSeed> tracks = new ArrayList<>();
        for (Resource resource : sorted(location + "/tracks/*.yml")) {
            YamlCareerFiles.TrackFile file = read(resource);
            if (file == null || file.tracks() == null) {
                continue;
            }
            for (YamlCareerFiles.TrackItem item : file.tracks()) {
                List<TrackUnitSeed> units =
                        item.units() == null
                                ? List.of()
                                : item.units().stream()
                                        .map(
                                                u ->
                                                        new TrackUnitSeed(
                                                                u.competency(),
                                                                u.weight(),
                                                                u.requirement(),
                                                                u.jobs()))
                                        .toList();
                tracks.add(
                        new TrackSeed(
                                file.company(),
                                item.area(),
                                item.stack(),
                                item.seniority(),
                                item.jobs(),
                                units));
            }
        }
        return tracks;
    }

    private List<Resource> sorted(String pattern) {
        try {
            return Arrays.stream(resolver.getResources(pattern))
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .toList();
        } catch (FileNotFoundException e) {
            return List.of();
        } catch (IOException e) {
            throw new InvalidCareerSeedException(
                    "Não foi possível listar " + pattern + ": " + e.getMessage(), e);
        }
    }

    private <T> List<T> read(String path, TypeReference<List<T>> type) {
        Resource resource = resolver.getResource(path);
        try (InputStream in = resource.getInputStream()) {
            List<T> items = yaml.readValue(in, type);
            return items == null ? List.of() : items;
        } catch (FileNotFoundException e) {
            return List.of();
        } catch (JacksonException e) {
            throw new InvalidCareerSeedException(
                    "Não foi possível ler " + path + ": " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new InvalidCareerSeedException(
                    "Não foi possível ler " + path + ": " + e.getMessage(), e);
        }
    }

    private YamlCareerFiles.TrackFile read(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return yaml.readValue(in, YamlCareerFiles.TrackFile.class);
        } catch (JacksonException e) {
            throw new InvalidCareerSeedException(
                    "Não foi possível ler tracks/"
                            + resource.getFilename()
                            + ": "
                            + e.getOriginalMessage(),
                    e);
        } catch (IOException e) {
            throw new InvalidCareerSeedException(
                    "Não foi possível ler tracks/" + resource.getFilename() + ": " + e.getMessage(),
                    e);
        }
    }
}
