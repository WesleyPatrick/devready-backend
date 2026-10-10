package br.com.devready.domain.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FeedbackTest {

    private static List<FeedbackItem> items(String slug, String name, int hits, int total) {
        List<FeedbackItem> list = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            list.add(new FeedbackItem(slug, name, i < hits));
        }
        return list;
    }

    @Test
    void sevenTenthsIsStrongAndBelowHalfIsAGap() {
        assertThat(FeedbackVerdict.of(7, 10)).isEqualTo(FeedbackVerdict.STRONG);
        assertThat(FeedbackVerdict.of(6, 10)).isEqualTo(FeedbackVerdict.OK);
        assertThat(FeedbackVerdict.of(1, 2)).isEqualTo(FeedbackVerdict.OK);
        assertThat(FeedbackVerdict.of(4, 10)).isEqualTo(FeedbackVerdict.GAP);
        assertThat(FeedbackVerdict.of(0, 1)).isEqualTo(FeedbackVerdict.GAP);
        assertThatThrownBy(() -> FeedbackVerdict.of(0, 0)).isInstanceOf(SessionException.class);
    }

    @Test
    void groupsByCompetencyInTheOrderTheyAppearWithOthersLast() {
        List<FeedbackItem> all = new ArrayList<>();
        all.add(new FeedbackItem(null, null, true));
        all.addAll(items("sql", "SQL e modelagem de dados", 3, 3));
        all.addAll(items("estruturas", "Estruturas de dados e algoritmos", 1, 3));
        all.add(new FeedbackItem("sql", "SQL e modelagem de dados", false));

        List<CompetencyFeedback> feedback = Feedback.of(all);

        assertThat(feedback)
                .extracting(CompetencyFeedback::competencySlug)
                .containsExactly("sql", "estruturas", null);
        assertThat(feedback.get(0).hits()).isEqualTo(3);
        assertThat(feedback.get(0).total()).isEqualTo(4);
        assertThat(feedback.get(2).competencyName()).isEqualTo("Outros");
        assertThat(feedback.get(2).verdict()).isEqualTo(FeedbackVerdict.STRONG);
    }

    @Test
    void theTextsFollowTheTemplates() {
        List<CompetencyFeedback> feedback =
                Feedback.of(
                        new ArrayList<>() {
                            {
                                addAll(items("sql", "SQL e modelagem de dados", 3, 3));
                                addAll(
                                        items(
                                                "estruturas",
                                                "Estruturas de dados e algoritmos",
                                                1,
                                                3));
                                addAll(items("java", "Java", 1, 2));
                                addAll(items(null, null, 0, 2));
                            }
                        });

        assertThat(feedback)
                .extracting(CompetencyFeedback::text)
                .containsExactly(
                        "Você acertou 3 de 3 em SQL e modelagem de dados: ponto forte.",
                        "1 de 3 em Estruturas de dados e algoritmos: vale revisar os materiais da competência.",
                        "Você acertou 1 de 2 em Java: falta pouco para virar ponto forte.",
                        "0 de 2 em outros temas: vale revisar esses temas.");
    }

    @Test
    void anItemWithACompetencyNeedsItsName() {
        assertThatThrownBy(() -> new FeedbackItem("sql", " ", true))
                .isInstanceOf(SessionException.class);
    }
}
