package br.com.devready.application.practice.service;

import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.practice.exception.ChallengeNotFoundException;
import br.com.devready.application.practice.exception.QuestionNotFoundException;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import java.util.Set;
import java.util.UUID;

public class VisibleLevels {

    private final CareerProfileRepository profiles;

    public VisibleLevels(CareerProfileRepository profiles) {
        this.profiles = profiles;
    }

    public Set<Level> of(UUID userId) {
        return Level.upTo(
                profiles.findByUserId(userId).map(CareerProfile::seniority).orElse(Level.JUNIOR));
    }

    public Question question(UUID userId, Question question) {
        if (!of(userId).contains(question.level())) {
            throw new QuestionNotFoundException(question.slug());
        }
        return question;
    }

    public Challenge challenge(UUID userId, Challenge challenge) {
        if (!of(userId).contains(challenge.level())) {
            throw new ChallengeNotFoundException(challenge.slug());
        }
        return challenge;
    }
}
