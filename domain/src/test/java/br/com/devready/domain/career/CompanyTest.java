package br.com.devready.domain.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompanyTest {

    @Test
    void normalisesOptionalFieldsAndCopiesCareers() {
        List<String> careers = new ArrayList<>(List.of("https://nu.com/careers"));
        Company company =
                new Company(
                        UUID.randomUUID(),
                        "nubank",
                        "Nubank",
                        CompanyGroup.TECH_BRASIL,
                        null,
                        careers,
                        true);

        careers.add("https://mudou.depois");

        assertThat(company.about()).isEmpty();
        assertThat(company.careers()).containsExactly("https://nu.com/careers");
    }

    @Test
    void rejectsNullOrBlankCareerPagesNamingTheCompany() {
        UUID id = UUID.randomUUID();
        List<String> withNull = new ArrayList<>();
        withNull.add("https://nu.com/careers");
        withNull.add(null);
        assertThatThrownBy(
                        () ->
                                new Company(
                                        id,
                                        "nubank",
                                        "Nubank",
                                        CompanyGroup.TECH_BRASIL,
                                        "",
                                        withNull,
                                        true))
                .isInstanceOf(CareerException.class)
                .hasMessage("Página de carreira vazia na empresa nubank.");
        assertThatThrownBy(
                        () ->
                                new Company(
                                        id,
                                        "nubank",
                                        "Nubank",
                                        CompanyGroup.TECH_BRASIL,
                                        "",
                                        List.of("  "),
                                        true))
                .isInstanceOf(CareerException.class)
                .hasMessage("Página de carreira vazia na empresa nubank.");
    }

    @Test
    void validatesSlugNameAndGroup() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(
                        () ->
                                new Company(
                                        id,
                                        "Nu Bank",
                                        "Nubank",
                                        CompanyGroup.TECH_BRASIL,
                                        "",
                                        List.of(),
                                        true))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(
                        () ->
                                new Company(
                                        id,
                                        "nubank",
                                        " ",
                                        CompanyGroup.TECH_BRASIL,
                                        "",
                                        List.of(),
                                        true))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new Company(id, "nubank", "Nubank", null, "", List.of(), true))
                .isInstanceOf(CareerException.class);
    }
}
