package br.com.devready.domain.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CareerProfileTest {

    private final UUID user = UUID.randomUUID();

    @Test
    void keepsTheOrderTheUserChose() {
        CareerProfile profile =
                new CareerProfile(
                        user, List.of("nubank", "google", "ifood"), "java-spring", Level.JUNIOR);
        assertThat(profile.companySlugs()).containsExactly("nubank", "google", "ifood");
    }

    @Test
    void acceptsFromOneToFiveCompanies() {
        assertThat(new CareerProfile(user, List.of("nubank"), "react", Level.PLENO).companySlugs())
                .hasSize(1);
        assertThat(
                        new CareerProfile(
                                        user,
                                        List.of("a", "b", "c", "d", "e"),
                                        "react",
                                        Level.PLENO)
                                .companySlugs())
                .hasSize(5);
    }

    @Test
    void rejectsEmptyTooManyAndRepeatedCompanies() {
        assertThatThrownBy(() -> new CareerProfile(user, List.of(), "react", Level.PLENO))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("pelo menos uma empresa");
        assertThatThrownBy(
                        () ->
                                new CareerProfile(
                                        user,
                                        List.of("a", "b", "c", "d", "e", "f"),
                                        "react",
                                        Level.PLENO))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("até 5 empresas");
        assertThatThrownBy(() -> new CareerProfile(user, List.of("a", "a"), "react", Level.PLENO))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("repetida");
    }

    @Test
    void rejectsMalformedSlugsNamingThem() {
        assertThatThrownBy(() -> new CareerProfile(user, List.of("Nubank"), "react", Level.PLENO))
                .isInstanceOf(CareerException.class)
                .hasMessage("Slug inválido para empresa: Nubank");
        assertThatThrownBy(() -> new CareerProfile(user, List.of("nubank"), "Java", Level.PLENO))
                .isInstanceOf(CareerException.class)
                .hasMessage("Slug inválido para stack: Java");
    }

    @Test
    void copiesTheCompaniesDefensively() {
        List<String> companies = new ArrayList<>(List.of("nubank", "google"));
        CareerProfile profile = new CareerProfile(user, companies, "react", Level.PLENO);

        companies.add("ifood");
        companies.set(0, "amazon");

        assertThat(profile.companySlugs()).containsExactly("nubank", "google");
    }

    @Test
    void explainsWhatIsMissingInStackAndSeniority() {
        assertThatThrownBy(() -> new CareerProfile(user, List.of("a"), null, Level.PLENO))
                .isInstanceOf(CareerException.class)
                .hasMessage("Escolha uma stack.");
        assertThatThrownBy(() -> new CareerProfile(user, List.of("a"), "react", null))
                .isInstanceOf(CareerException.class)
                .hasMessage("Escolha a senioridade.");
    }

    @Test
    void requiresUserStackAndSeniority() {
        assertThatThrownBy(() -> new CareerProfile(null, List.of("a"), "react", Level.PLENO))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new CareerProfile(user, List.of("a"), " ", Level.PLENO))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new CareerProfile(user, List.of("a"), "react", null))
                .isInstanceOf(CareerException.class);
    }
}
