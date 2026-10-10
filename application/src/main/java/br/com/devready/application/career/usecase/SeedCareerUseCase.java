package br.com.devready.application.career.usecase;

import br.com.devready.application.career.dto.CareerSeed;
import br.com.devready.application.career.dto.CareerSeedReport;
import br.com.devready.application.career.dto.CompanySeed;
import br.com.devready.application.career.dto.CompetencySeed;
import br.com.devready.application.career.dto.MaterialSeed;
import br.com.devready.application.career.dto.StackSeed;
import br.com.devready.application.career.dto.TrackSeed;
import br.com.devready.application.career.dto.TrackUnitSeed;
import br.com.devready.application.career.exception.InvalidCareerSeedException;
import br.com.devready.application.career.port.CareerSeedSource;
import br.com.devready.application.career.port.CompanyRepository;
import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.application.career.port.StackRepository;
import br.com.devready.application.career.port.TrackRepository;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.TagRepository;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.domain.career.CareerException;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.career.CompetencyCategory;
import br.com.devready.domain.career.Material;
import br.com.devready.domain.career.MaterialType;
import br.com.devready.domain.career.Requirement;
import br.com.devready.domain.career.Stack;
import br.com.devready.domain.career.Track;
import br.com.devready.domain.career.TrackUnit;
import br.com.devready.domain.catalog.CatalogException;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SeedCareerUseCase {

    private static final String ALL = "todas";

    private final CareerSeedSource source;
    private final CompanyRepository companies;
    private final StackRepository stacks;
    private final AreaRepository areas;
    private final TagRepository tags;
    private final CompetencyRepository competencies;
    private final TrackRepository tracks;
    private final IdGenerator ids;

    public SeedCareerUseCase(
            CareerSeedSource source,
            CompanyRepository companies,
            StackRepository stacks,
            AreaRepository areas,
            TagRepository tags,
            CompetencyRepository competencies,
            TrackRepository tracks,
            IdGenerator ids) {
        this.source = source;
        this.companies = companies;
        this.stacks = stacks;
        this.areas = areas;
        this.tags = tags;
        this.competencies = competencies;
        this.tracks = tracks;
        this.ids = ids;
    }

    public CareerSeedReport execute() {
        CareerSeed seed = source.load();

        Changes<Company> companyChanges = planCompanies(seed.companies());
        Changes<Stack> stackChanges = planStacks(seed.stacks());
        Changes<Competency> competencyChanges =
                planCompetencies(seed.competencies(), seed.materials());
        Set<String> seedCompanies = new HashSet<>();
        seed.companies().forEach(company -> seedCompanies.add(company.slug()));
        Set<String> seedCompetencies = new HashSet<>();
        seed.competencies().forEach(competency -> seedCompetencies.add(competency.slug()));
        Changes<Track> trackChanges =
                planTracks(
                        seed.tracks(), seedCompanies, knownStacks(stackChanges), seedCompetencies);

        companyChanges.all().forEach(companies::save);
        stackChanges.all().forEach(stacks::save);
        competencyChanges.all().forEach(competencies::save);
        trackChanges.all().forEach(tracks::save);
        trackChanges.removed().forEach(track -> tracks.delete(track.id()));
        competencyChanges.removed().forEach(competency -> competencies.delete(competency.slug()));
        return new CareerSeedReport(
                companyChanges.inserted().size(),
                companyChanges.updated().size(),
                stackChanges.inserted().size(),
                stackChanges.updated().size(),
                competencyChanges.inserted().size(),
                competencyChanges.updated().size(),
                competencyChanges.removed().size(),
                trackChanges.inserted().size(),
                trackChanges.updated().size(),
                trackChanges.removed().size());
    }

    private Changes<Company> planCompanies(List<CompanySeed> items) {
        Changes<Company> changes = new Changes<>();
        Set<String> seen = new HashSet<>();
        for (CompanySeed item : items) {
            if (!seen.add(item.slug())) {
                throw new InvalidCareerSeedException(
                        "Empresa " + item.slug() + " repetida no seed.");
            }
            Optional<Company> existing = companies.findBySlug(item.slug());
            UUID id = existing.map(Company::id).orElseGet(ids::next);
            boolean active = existing.map(Company::active).orElse(true);
            Company wanted = toCompany(item, id, active);
            if (existing.isEmpty()) {
                changes.inserted().add(wanted);
            } else if (!existing.get().equals(wanted)) {
                changes.updated().add(wanted);
            }
        }
        return changes;
    }

    private Changes<Stack> planStacks(List<StackSeed> items) {
        Changes<Stack> changes = new Changes<>();
        Set<String> seen = new HashSet<>();
        for (StackSeed item : items) {
            if (!seen.add(item.slug())) {
                throw new InvalidCareerSeedException("Stack " + item.slug() + " repetida no seed.");
            }
            if (areas.findBySlug(item.area()).isEmpty()) {
                throw new InvalidCareerSeedException(
                        "Stack "
                                + item.slug()
                                + " referencia a área desconhecida "
                                + item.area()
                                + ".");
            }
            Optional<Stack> existing = stacks.findBySlug(item.slug());
            Stack wanted = toStack(item, existing.map(Stack::id).orElseGet(ids::next));
            if (existing.isEmpty()) {
                changes.inserted().add(wanted);
            } else if (!existing.get().equals(wanted)) {
                changes.updated().add(wanted);
            }
        }
        return changes;
    }

    private Changes<Competency> planCompetencies(
            List<CompetencySeed> items, List<MaterialSeed> materialItems) {
        Set<String> slugs = new HashSet<>();
        for (CompetencySeed item : items) {
            if (!slugs.add(item.slug())) {
                throw new InvalidCareerSeedException(
                        "Competência " + item.slug() + " repetida no seed.");
            }
        }
        Map<String, List<Material>> materials = new HashMap<>();
        for (MaterialSeed item : materialItems) {
            if (!slugs.contains(item.competency())) {
                throw new InvalidCareerSeedException(
                        "Material \""
                                + item.title()
                                + "\" aponta para a competência desconhecida "
                                + item.competency()
                                + ".");
            }
            materials
                    .computeIfAbsent(item.competency(), slug -> new ArrayList<>())
                    .add(toMaterial(item));
        }
        Map<String, Competency> existing = new LinkedHashMap<>();
        competencies.findAll().forEach(competency -> existing.put(competency.slug(), competency));
        Changes<Competency> changes = new Changes<>();
        for (CompetencySeed item : items) {
            for (String tag : item.tags()) {
                if (tags.findBySlug(tag).isEmpty()) {
                    throw new InvalidCareerSeedException(
                            "Competência " + item.slug() + " usa a tag desconhecida " + tag + ".");
                }
            }
            Competency current = existing.remove(item.slug());
            Competency wanted =
                    toCompetency(
                            item,
                            current == null ? ids.next() : current.id(),
                            materials.getOrDefault(item.slug(), List.of()));
            if (current == null) {
                changes.inserted().add(wanted);
            } else if (!current.equals(wanted)) {
                changes.updated().add(wanted);
            }
        }
        changes.removed().addAll(existing.values());
        return changes;
    }

    private Changes<Track> planTracks(
            List<TrackSeed> items,
            Set<String> seedCompanies,
            Map<String, Stack> knownStacks,
            Set<String> knownCompetencies) {
        Map<String, Track> existing = new LinkedHashMap<>();
        tracks.findAll().forEach(track -> existing.put(track.label(), track));
        Set<String> seen = new HashSet<>();
        Changes<Track> changes = new Changes<>();
        for (TrackSeed item : items) {
            String written =
                    item.company()
                            + "/"
                            + item.area()
                            + "/"
                            + item.seniority()
                            + "/"
                            + item.stack();
            if (!seedCompanies.contains(item.company())
                    && companies.findBySlug(item.company()).isEmpty()) {
                throw new InvalidCareerSeedException(
                        "Trilha "
                                + written
                                + " usa a empresa desconhecida "
                                + item.company()
                                + ".");
            }
            if (areas.findBySlug(item.area()).isEmpty()) {
                throw new InvalidCareerSeedException(
                        "Trilha "
                                + written
                                + " referencia a área desconhecida "
                                + item.area()
                                + ".");
            }
            String stackSlug = stackOf(item, written, knownStacks);
            Level seniority = seniorityOf(item.seniority(), written);
            String key = Track.label(item.company(), item.area(), stackSlug, seniority);
            if (!seen.add(key)) {
                throw new InvalidCareerSeedException("Trilha " + key + " repetida no seed.");
            }
            List<TrackUnit> units = new ArrayList<>();
            for (TrackUnitSeed unit : item.units()) {
                if (!knownCompetencies.contains(unit.competency())) {
                    throw new InvalidCareerSeedException(
                            "Trilha "
                                    + key
                                    + " usa a competência desconhecida "
                                    + unit.competency()
                                    + "; competência que sai de competencies.yml não pode"
                                    + " continuar numa trilha.");
                }
                units.add(toUnit(unit, key));
            }
            if (item.jobs() == null) {
                throw new InvalidCareerSeedException("Trilha " + key + " sem o número de vagas.");
            }
            Track current = existing.remove(key);
            Track wanted =
                    toTrack(
                            item,
                            current == null ? ids.next() : current.id(),
                            stackSlug,
                            seniority,
                            units,
                            key);
            if (current == null) {
                changes.inserted().add(wanted);
            } else if (!current.equals(wanted)) {
                changes.updated().add(wanted);
            }
        }
        changes.removed().addAll(existing.values());
        return changes;
    }

    private Map<String, Stack> knownStacks(Changes<Stack> stackChanges) {
        Map<String, Stack> known = new HashMap<>();
        stacks.findAll().forEach(stack -> known.put(stack.slug(), stack));
        stackChanges.all().forEach(stack -> known.put(stack.slug(), stack));
        return known;
    }

    private static String stackOf(TrackSeed item, String written, Map<String, Stack> knownStacks) {
        if (item.stack() == null) {
            throw new InvalidCareerSeedException(
                    "Trilha " + written + " sem stack; use todas para a área inteira.");
        }
        if (ALL.equals(item.stack())) {
            return null;
        }
        Stack stack = knownStacks.get(item.stack());
        if (stack == null) {
            throw new InvalidCareerSeedException(
                    "Trilha " + written + " usa a stack desconhecida " + item.stack() + ".");
        }
        if (!stack.areaSlug().equals(item.area())) {
            throw new InvalidCareerSeedException(
                    "Trilha "
                            + written
                            + " usa a stack "
                            + item.stack()
                            + ", que é da área "
                            + stack.areaSlug()
                            + ".");
        }
        return item.stack();
    }

    private static Level seniorityOf(String value, String written) {
        if (ALL.equals(value)) {
            return null;
        }
        for (Level level : Level.values()) {
            if (level.name().toLowerCase(Locale.ROOT).equals(value)) {
                return level;
            }
        }
        throw new InvalidCareerSeedException(
                "Trilha "
                        + written
                        + " com senioridade inválida: "
                        + value
                        + " (use junior, pleno, senior ou todas).");
    }

    private static Company toCompany(CompanySeed item, UUID id, boolean active) {
        try {
            return new Company(
                    id,
                    item.slug(),
                    item.name(),
                    CompanyGroup.fromSlug(item.group()),
                    item.about(),
                    item.careers(),
                    active);
        } catch (CareerException error) {
            throw new InvalidCareerSeedException(
                    "Empresa " + item.slug() + " inválida no seed: " + error.getMessage());
        }
    }

    private static Stack toStack(StackSeed item, UUID id) {
        try {
            return new Stack(id, item.slug(), item.name(), item.area());
        } catch (CareerException error) {
            throw new InvalidCareerSeedException(
                    "Stack " + item.slug() + " inválida no seed: " + error.getMessage());
        }
    }

    private static Competency toCompetency(CompetencySeed item, UUID id, List<Material> materials) {
        try {
            return new Competency(
                    id,
                    item.slug(),
                    item.name(),
                    CompetencyCategory.fromSlug(item.category()),
                    item.summary(),
                    Difficulty.fromSlug(item.difficulty()),
                    Set.copyOf(item.tags()),
                    materials);
        } catch (CareerException | CatalogException error) {
            throw new InvalidCareerSeedException(
                    "Competência " + item.slug() + " inválida no seed: " + error.getMessage());
        }
    }

    private static Material toMaterial(MaterialSeed item) {
        try {
            return new Material(
                    item.title(),
                    item.url(),
                    MaterialType.fromSlug(item.type()),
                    item.language(),
                    item.free());
        } catch (CareerException error) {
            throw new InvalidCareerSeedException(
                    "Material \"" + item.title() + "\" inválido no seed: " + error.getMessage());
        }
    }

    private static TrackUnit toUnit(TrackUnitSeed unit, String key) {
        if (unit.weight() == null || unit.jobs() == null) {
            throw new InvalidCareerSeedException(
                    "Trilha "
                            + key
                            + " sem peso ou sem vagas na competência "
                            + unit.competency()
                            + ".");
        }
        try {
            return new TrackUnit(
                    unit.competency(),
                    unit.weight(),
                    Requirement.fromSlug(unit.requirement()),
                    unit.jobs());
        } catch (CareerException error) {
            throw new InvalidCareerSeedException(
                    "Trilha " + key + " inválida no seed: " + error.getMessage());
        }
    }

    private static Track toTrack(
            TrackSeed item,
            UUID id,
            String stackSlug,
            Level seniority,
            List<TrackUnit> units,
            String key) {
        try {
            return new Track(
                    id, item.company(), item.area(), stackSlug, seniority, item.jobs(), units);
        } catch (CareerException error) {
            throw new InvalidCareerSeedException(
                    "Trilha " + key + " inválida no seed: " + error.getMessage());
        }
    }

    private record Changes<T>(List<T> inserted, List<T> updated, List<T> removed) {
        Changes() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        List<T> all() {
            List<T> all = new ArrayList<>(inserted);
            all.addAll(updated);
            return all;
        }
    }
}
