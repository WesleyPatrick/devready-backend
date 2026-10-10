package br.com.devready.application.practice.fake;

import static br.com.devready.application.career.fake.CareerFixtures.company;
import static br.com.devready.application.career.fake.CareerFixtures.competency;
import static br.com.devready.application.career.fake.CareerFixtures.stack;
import static br.com.devready.application.career.fake.CareerFixtures.track;
import static br.com.devready.application.career.fake.CareerFixtures.unit;

import br.com.devready.application.career.fake.InMemoryCareerProfileRepository;
import br.com.devready.application.career.fake.InMemoryCompanyRepository;
import br.com.devready.application.career.fake.InMemoryCompetencyRepository;
import br.com.devready.application.career.fake.InMemoryQuestionProgressReader;
import br.com.devready.application.career.fake.InMemoryStackRepository;
import br.com.devready.application.career.fake.InMemoryTrackRepository;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.catalog.fake.InMemoryAreaRepository;
import br.com.devready.application.catalog.fake.InMemoryChallengeRepository;
import br.com.devready.application.catalog.fake.InMemoryPracticeCatalog;
import br.com.devready.application.catalog.fake.InMemoryQuestionRepository;
import br.com.devready.application.evaluation.fake.InMemorySubmissionRepository;
import br.com.devready.application.evaluation.fake.SubmissionFixtures;
import br.com.devready.application.practice.service.ProblemBoard;
import br.com.devready.application.practice.service.VisibleLevels;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.CompetencyCategory;
import br.com.devready.domain.career.Requirement;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.practice.Answer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PracticeWorld {

    public final InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
    public final InMemoryStackRepository stacks = new InMemoryStackRepository();
    public final InMemoryCareerProfileRepository profiles = new InMemoryCareerProfileRepository();
    public final InMemoryCompetencyRepository competencies = new InMemoryCompetencyRepository();
    public final InMemoryTrackRepository tracks = new InMemoryTrackRepository();
    public final InMemoryAreaRepository areas = new InMemoryAreaRepository();
    public final InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
    public final InMemoryChallengeRepository challenges = new InMemoryChallengeRepository();
    public final InMemoryAnswerRepository answers = new InMemoryAnswerRepository();
    public final InMemorySubmissionRepository submissions = new InMemorySubmissionRepository();
    public final InMemoryChallengeAttemptReader attempts =
            new InMemoryChallengeAttemptReader(submissions);
    public final InMemorySimulationRecordReader simulations = new InMemorySimulationRecordReader();
    public final InMemoryExamLockReader examLocks = new InMemoryExamLockReader();
    public final InMemoryQuestionProgressReader progress = new InMemoryQuestionProgressReader();
    public final InMemoryPracticeCatalog catalog =
            new InMemoryPracticeCatalog(questions, challenges);
    public final TrackAssembler assembler =
            new TrackAssembler(
                    profiles, companies, stacks, competencies, tracks, areas, catalog, progress);
    public final ProblemBoard board = new ProblemBoard(assembler, areas, catalog, competencies);
    public final VisibleLevels levels = new VisibleLevels(profiles);
    public final UUID user = UUID.randomUUID();

    public PracticeWorld() {
        Area backend = areas.save(CatalogFixtures.area("backend"));
        Area frontend = areas.save(CatalogFixtures.area("frontend"));
        companies.save(company("nubank", "Nubank", CompanyGroup.TECH_BRASIL));
        companies.save(company("google", "Google", CompanyGroup.BIG_TECH));
        stacks.save(stack("java-spring", "Java e Spring", "backend"));
        stacks.save(stack("react", "React", "frontend"));
        competencies.save(
                competency(
                        "sql",
                        "SQL",
                        CompetencyCategory.FUNDAMENTO,
                        Difficulty.MEDIUM,
                        "sql",
                        "banco-de-dados"));
        competencies.save(
                competency(
                        "java", "Java", CompetencyCategory.LINGUAGEM, Difficulty.MEDIUM, "java"));
        competencies.save(
                competency(
                        "apis-rest",
                        "APIs REST",
                        CompetencyCategory.PRATICA,
                        Difficulty.EASY,
                        "http"));
        competencies.save(
                competency(
                        "seguranca",
                        "Segurança",
                        CompetencyCategory.PRATICA,
                        Difficulty.MEDIUM,
                        "seguranca"));
        competencies.save(
                competency(
                        "autenticacao",
                        "Autenticação",
                        CompetencyCategory.PRATICA,
                        Difficulty.MEDIUM,
                        "seguranca"));
        competencies.save(
                competency(
                        "criptografia",
                        "Criptografia",
                        CompetencyCategory.FUNDAMENTO,
                        Difficulty.HARD,
                        "cripto"));
        tracks.save(
                track(
                        "nubank",
                        "backend",
                        null,
                        null,
                        17,
                        unit("sql", 0.5, Requirement.OBRIGATORIA, 9),
                        unit("java", 0.6, Requirement.OBRIGATORIA, 10)));
        tracks.save(
                track(
                        "google",
                        "backend",
                        "java-spring",
                        null,
                        9,
                        unit("apis-rest", 0.8, Requirement.OBRIGATORIA, 7),
                        unit("sql", 0.4, Requirement.DESEJAVEL, 4)));
        questions.save(
                CatalogFixtures.question(
                        "q-sql-easy", "Chave estrangeira", backend, Difficulty.EASY, true, "sql"));
        questions.save(
                CatalogFixtures.question(
                        "q-sql-hard",
                        "Cem consultas",
                        backend,
                        Difficulty.HARD,
                        true,
                        "banco-de-dados"));
        questions.save(
                CatalogFixtures.question(
                        "q-java-sql",
                        "Equals e hashCode",
                        backend,
                        Difficulty.MEDIUM,
                        true,
                        "java",
                        "sql"));
        questions.save(
                CatalogFixtures.question(
                        "q-java", "Exceção verificada", backend, Difficulty.EASY, true, "java"));
        questions.save(
                CatalogFixtures.question(
                        "q-http", "Idempotência", backend, Difficulty.EASY, true, "http"));
        questions.save(
                CatalogFixtures.question(
                        "q-seg", "Senha com hash", backend, Difficulty.MEDIUM, true, "seguranca"));
        questions.save(
                CatalogFixtures.question(
                        "q-cripto",
                        "Sal na senha",
                        backend,
                        Difficulty.HARD,
                        true,
                        "seguranca",
                        "cripto"));
        questions.save(
                CatalogFixtures.question(
                        "q-none", "Sem competência", backend, Difficulty.EASY, true, "outra"));
        questions.save(
                CatalogFixtures.question(
                        "q-off", "Inativa", backend, Difficulty.EASY, false, "sql"));
        questions.save(
                CatalogFixtures.question(
                        "q-front", "Outra área", frontend, Difficulty.EASY, true, "sql"));
        challenges.save(
                CatalogFixtures.challenge("c-sql", "Mesclar", backend, Difficulty.EASY, "sql"));
        challenges.save(
                CatalogFixtures.challenge(
                        "c-front", "Outra área", frontend, Difficulty.EASY, "sql"));
    }

    public void withGoal() {
        withGoal(Level.JUNIOR);
    }

    public void withGoal(Level seniority) {
        profiles.save(
                new CareerProfile(user, List.of("nubank", "google"), "java-spring", seniority));
    }

    public void withLevels() {
        Area backend = areas.findBySlug("backend").orElseThrow();
        questions.save(
                CatalogFixtures.atLevel(
                        CatalogFixtures.question(
                                "q-sql-pleno",
                                "Leitura repetível",
                                backend,
                                Difficulty.MEDIUM,
                                true,
                                "sql"),
                        Level.PLENO));
        questions.save(
                CatalogFixtures.atLevel(
                        CatalogFixtures.question(
                                "q-java-pleno",
                                "Mapa concorrente",
                                backend,
                                Difficulty.EASY,
                                true,
                                "java"),
                        Level.PLENO));
        questions.save(
                CatalogFixtures.atLevel(
                        CatalogFixtures.question(
                                "q-sql-senior",
                                "Preencher coluna",
                                backend,
                                Difficulty.HARD,
                                true,
                                "banco-de-dados"),
                        Level.SENIOR));
        challenges.save(
                CatalogFixtures.atLevel(
                        CatalogFixtures.challenge(
                                "c-sql-senior",
                                "Janela deslizante",
                                backend,
                                Difficulty.HARD,
                                "sql"),
                        Level.SENIOR));
    }

    public Question question(String slug) {
        return questions.get(slug);
    }

    public Answer answered(UUID who, String slug, boolean correct, int xp, Instant at) {
        Question question = questions.get(slug);
        QuestionOption option =
                question.options().stream()
                        .filter(candidate -> candidate.correct() == correct)
                        .findFirst()
                        .orElseThrow();
        return answers.save(
                new Answer(UUID.randomUUID(), who, question.id(), option.id(), correct, xp, at));
    }

    public CodeSubmission submitted(
            UUID who, String challengeSlug, SubmissionStatus status, int xp, Instant at) {
        return submissions.save(
                SubmissionFixtures.finished(
                        who, challenges.get(challengeSlug).id(), status, xp, at));
    }

    public CodeSubmission submittedInSession(
            UUID who,
            String challengeSlug,
            SubmissionStatus status,
            int passed,
            UUID sessionId,
            Instant at) {
        return submissions.save(
                SubmissionFixtures.finishedInSession(
                        who, challenges.get(challengeSlug).id(), status, passed, sessionId, at));
    }
}
